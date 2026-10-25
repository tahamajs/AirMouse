package tcp

import (
	"bufio"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"math"
	"net"
	"strings"
	"sync"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	websocketpkg "airmouse-go/internal/protocol/websocket"
	"airmouse-go/internal/sysaction"
	"airmouse-go/internal/tlsutil"
	"airmouse-go/internal/utils"
)

const (
	maxCoordLimit  = 500.0
	maxScrollLimit = 10.0
	replayWindowMs = 5000
)

type replayDetector struct {
	mu          sync.Mutex
	lastSeq     uint64
	initialized bool
}

func newReplayDetector() *replayDetector {
	return &replayDetector{}
}

func (rd *replayDetector) checkAndAdvanceSeq(seq uint64) error {
	rd.mu.Lock()
	defer rd.mu.Unlock()
	if !rd.initialized {
		rd.lastSeq = seq
		rd.initialized = true
		return nil
	}
	if seq <= rd.lastSeq {
		return fmt.Errorf("replay_detected: seq %d <= last_seq %d", seq, rd.lastSeq)
	}
	rd.lastSeq = seq
	return nil
}

func validateTimestamp(ts, nowMs int64) error {
	if ts <= 0 {
		return fmt.Errorf("replay_rejected: invalid non-positive timestamp %d", ts)
	}
	diff := nowMs - ts
	if diff < 0 {
		diff = -diff
	}
	if diff < 0 || diff > replayWindowMs {
		return fmt.Errorf("replay_rejected: delta %d ms > %d ms", diff, replayWindowMs)
	}
	return nil
}

type Client struct {
	ID             string
	DeviceID       string
	Name           string
	Conn           net.Conn
	ConnectedAt    time.Time
	LastActive     time.Time
	BytesSent      int64
	BytesRecv      int64
	IP             string
	Approved       bool
	ReplayDetector *replayDetector
	mu             sync.RWMutex
}

type Server struct {
	host      string
	port      int
	listener  net.Listener
	tlsConfig *tls.Config
	clients   map[string]*Client
	mouse     mouse.Controller
	deviceMgr *device.Manager
	authMgr   *auth.Manager
	mu        sync.RWMutex
	running   bool
	callbacks []func(event TCPEvent)
}

type TCPEvent struct {
	Type      string
	ClientID  string
	ClientIP  string
	Timestamp time.Time
}

func NewServer(host string, port int, mouseCtrl mouse.Controller, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		host:      host,
		port:      port,
		clients:   make(map[string]*Client),
		mouse:     mouseCtrl,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
		callbacks: make([]func(TCPEvent), 0),
	}
}

// SetTLSConfig explicitly sets or overrides the TLS configuration for the server.
func (s *Server) SetTLSConfig(tlsConfig *tls.Config) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.tlsConfig = tlsConfig
}

func (s *Server) Start() error {
	addr := fmt.Sprintf("%s:%d", s.host, s.port)
	cfg := config.Get()

	var listener net.Listener
	var err error

	useTLS := (s.tlsConfig != nil) || (cfg != nil && cfg.TLSEnabled)
	if useTLS {
		tlsCfg := s.tlsConfig
		if tlsCfg == nil {
			var tlsErr error
			tlsCfg, tlsErr = tlsutil.GetServerTLSConfig(cfg.TLSCertPath, cfg.TLSKeyPath, []string{s.host, "localhost", "127.0.0.1"})
			if tlsErr != nil {
				return fmt.Errorf("failed to init TLS for TCP server: %w", tlsErr)
			}
			s.tlsConfig = tlsCfg
		}
		listener, err = tls.Listen("tcp", addr, tlsCfg)
		if err != nil {
			return fmt.Errorf("failed to listen on %s with TLS: %w", addr, err)
		}
		utils.LogInfo("TCP server started with TLS: address=%s", addr)
	} else {
		listener, err = net.Listen("tcp", addr)
		if err != nil {
			return fmt.Errorf("failed to listen on %s: %w", addr, err)
		}
		utils.LogInfo("TCP server started (cleartext): address=%s", addr)
	}

	s.mu.Lock()
	s.listener = listener
	s.running = true
	s.mu.Unlock()
	go s.acceptLoop()
	utils.LogDebug("TCP listen socket ready on %s", addr)
	return nil
}

func (s *Server) acceptLoop() {
	for {
		s.mu.RLock()
		running := s.running
		s.mu.RUnlock()
		if !running {
			break
		}
		conn, err := s.listener.Accept()
		if err != nil {
			s.mu.RLock()
			running = s.running
			s.mu.RUnlock()
			if running {
				utils.LogError("TCP accept error: %v", err)
			}
			continue
		}
		go s.handleClient(conn)
	}
}

func (s *Server) handleClient(conn net.Conn) {
	// If TLS connection, enforce handshake and reject cleartext
	if tlsConn, ok := conn.(*tls.Conn); ok {
		_ = tlsConn.SetDeadline(time.Now().Add(5 * time.Second))
		if err := tlsConn.Handshake(); err != nil {
			utils.LogInfo("TCP handshake rejected (cleartext or invalid TLS): %v", err)
			_ = conn.Close()
			return
		}
		_ = tlsConn.SetDeadline(time.Time{})
	}

	if tcpConn, ok := conn.(*net.TCPConn); ok {
		_ = tcpConn.SetNoDelay(true)
		_ = tcpConn.SetKeepAlive(true)
		_ = tcpConn.SetKeepAlivePeriod(3 * time.Second)
	}
	clientID := utils.GenerateID()
	clientIP := conn.RemoteAddr().String()
	client := &Client{
		ID:             clientID,
		Name:           "Unknown",
		Conn:           conn,
		ConnectedAt:    time.Now(),
		LastActive:     time.Now(),
		IP:             clientIP,
		Approved:       false,
		ReplayDetector: newReplayDetector(),
	}
	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()

	utils.LogInfo("TCP client connected: id=%s ip=%s", clientID, clientIP)
	utils.LogInfo("TCP approval pending: id=%s", clientID)
	utils.LogDebug("TCP initial client record created: id=%s name=%s", clientID, client.Name)

	if s.deviceMgr != nil {
		_ = s.deviceMgr.RegisterDevice(clientID, device.TypeTCP, client.Name)
		_ = s.deviceMgr.UpdateDeviceStatus(clientID, device.StatusPendingApproval)
	}
	s.triggerEvent(TCPEvent{Type: "connected", ClientID: clientID, ClientIP: clientIP, Timestamp: time.Now()})

	reader := bufio.NewReader(conn)
	heartbeat := time.NewTicker(30 * time.Second)
	defer heartbeat.Stop()

	go func() {
		for range heartbeat.C {
			s.mu.RLock()
			c, exists := s.clients[clientID]
			s.mu.RUnlock()
			if !exists {
				return
			}
			c.mu.RLock()
			idle := time.Since(c.LastActive)
			c.mu.RUnlock()
			if idle > 120*time.Second {
				utils.LogInfo("TCP client timeout: id=%s", clientID)
				_ = conn.Close()
				return
			}
			_ = conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
			_, err := conn.Write([]byte(`{"type":"ping"}` + "\n"))
			if err != nil {
				utils.LogDebug("TCP ping write error: %v", err)
				_ = conn.Close()
				return
			}
			utils.LogDebug("TCP ping sent to client=%s", clientID)
		}
	}()

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			utils.LogDebug("TCP read loop ending for client=%s err=%v", clientID, err)
			break
		}
		utils.LogDebug("TCP raw line from %s: %q", clientID, line)

		client.mu.Lock()
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(line))
		devID := client.DeviceID
		if devID == "" {
			devID = client.ID
		}
		client.mu.Unlock()
		utils.RecordPacketIn(int64(len(line)))
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceActivity(devID, 0, int64(len(line)))
		}
		s.processLine(client, []byte(line))
	}

	_ = conn.Close()
	if s.mouse != nil {
		s.mouse.MouseUp("left")
		s.mouse.MouseUp("right")
		s.mouse.MouseUp("middle")
	}
	sysaction.ReleaseAllKeys()
	s.mu.Lock()
	delete(s.clients, clientID)
	s.mu.Unlock()
	utils.LogInfo("TCP client disconnected: id=%s", clientID)

	client.mu.RLock()
	deviceID := client.DeviceID
	client.mu.RUnlock()
	if deviceID == "" {
		deviceID = clientID
	}
	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusDisconnected)
	}
	s.triggerEvent(TCPEvent{Type: "disconnected", ClientID: clientID, ClientIP: clientIP, Timestamp: time.Now()})
}

// autoApproveTCPClient checks if the fingerprint is trusted and auto‑approves.
func (s *Server) autoApproveTCPClient(client *Client, fingerprint string) bool {
	if s.deviceMgr == nil || fingerprint == "" {
		return false
	}
	// Use the config trusted list
	if !config.Get().IsTrustedDevice(fingerprint) {
		return false
	}
	// Update device manager to connected state
	_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeTCP, client.Name, map[string]string{
		"fingerprint": fingerprint,
		"ip_address":  client.IP,
	})
	_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusConnected)
	cfg := config.Get()
	welcome := fmt.Sprintf(`{"type":"welcome","payload":{"server":"%s","version":"%s","client_id":"%s"}}`+"\n",
		cfg.ServerName, cfg.Version, client.ID)
	_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	if _, err := client.Conn.Write([]byte(welcome)); err == nil {
		client.mu.Lock()
		client.Approved = true
		client.DeviceID = fingerprint
		client.BytesSent += int64(len(welcome))
		client.mu.Unlock()
		common.SetMovementPaused(false)
		common.ClearPause()
		utils.LogInfo("TCP auto-approved trusted device: %s (fingerprint: %s)", client.ID, fingerprint)
		return true
	}
	return false
}

func (s *Server) processLine(client *Client, line []byte) {
	msgType, payload, id, err := decodeWireMessage(line)
	if err != nil {
		utils.LogDebug("Invalid TCP message: %v", err)
		return
	}
	utils.LogDebug("TCP message parsed: client=%s type=%s payload_keys=%d", client.ID, msgType, len(payload))

	client.mu.RLock()
	approved := client.Approved
	client.mu.RUnlock()

	if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "rightclick" || msgType == "scroll" ||
		msgType == "left_click" || msgType == "right_click" || msgType == "double_click" || msgType == "middle_click" || msgType == "middleclick" ||
		msgType == "hscroll" || msgType == "key" || msgType == "keypress" || msgType == "key_press" || msgType == "keydown" ||
		msgType == "key_down" || msgType == "keyup" || msgType == "key_up" || msgType == "hotkey" || msgType == "type" || msgType == "text" ||
		msgType == "slide_next" || msgType == "slide_prev" || msgType == "volume_up" || msgType == "volume_down" || msgType == "mute" ||
		msgType == "play_pause" || msgType == "lock_screen" || msgType == "power_sleep" || msgType == "brightness" || msgType == "voice_command") {
		s.writeAck(client, id)
	}

	if !approved && msgType != "hello" && msgType != "device_info" && msgType != "ping" && msgType != "heartbeat" {
		utils.LogDebug("Ignoring TCP %s while waiting for approval: client=%s", msgType, client.ID)
		return
	}

	// Replay and timestamp checks if seq/ts are present
	if client.ReplayDetector != nil {
		nowMs := time.Now().UnixMilli()
		if tsVal, ok := payload["ts"]; ok {
			ts := int64(number(tsVal))
			if ts > 0 {
				if err := validateTimestamp(ts, nowMs); err != nil {
					utils.RecordDroppedPacket()
					utils.LogInfo("TCP replay rejection (timestamp): client=%s err=%v", client.ID, err)
					return
				}
			}
		}
		if seqVal, ok := payload["seq"]; ok {
			seq := uint64(number(seqVal))
			if seq > 0 {
				if err := client.ReplayDetector.checkAndAdvanceSeq(seq); err != nil {
					utils.RecordDroppedPacket()
					utils.LogInfo("TCP replay rejection (sequence): client=%s err=%v", client.ID, err)
					return
				}
			}
		}
	}

	switch msgType {
	case "move", "move_rel":
		dx := firstNumber(payload, "dx", "DeltaX", "deltaX")
		dy := firstNumber(payload, "dy", "DeltaY", "deltaY")
		if math.IsNaN(dx) || math.IsInf(dx, 0) || math.IsNaN(dy) || math.IsInf(dy, 0) ||
			math.Abs(dx) > maxCoordLimit || math.Abs(dy) > maxCoordLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("TCP move dropped (out of bounds or NaN/Inf): dx=%.2f dy=%.2f", dx, dy)
			return
		}
		utils.LogInfo("TCP move: client=%s dx=%.2f dy=%.2f", client.ID, dx, dy)
		if s.mouse != nil {
			s.mouse.Move(dx, dy)
		}
		utils.LogDebug("TCP move forwarded: client=%s dx=%.2f dy=%.2f", client.ID, dx, dy)

	case "move_abs":
		targetX := firstNumber(payload, "x", "target_x", "X")
		targetY := firstNumber(payload, "y", "target_y", "Y")
		if math.IsNaN(targetX) || math.IsInf(targetX, 0) || math.IsNaN(targetY) || math.IsInf(targetY, 0) {
			utils.RecordDroppedPacket()
			utils.LogInfo("TCP move_abs dropped (NaN/Inf): x=%.2f y=%.2f", targetX, targetY)
			return
		}
		if s.mouse != nil {
			curX, curY := s.mouse.GetPosition()
			dx := targetX - curX
			dy := targetY - curY
			if math.Abs(dx) > maxCoordLimit {
				if dx > 0 {
					dx = maxCoordLimit
				} else {
					dx = -maxCoordLimit
				}
			}
			if math.Abs(dy) > maxCoordLimit {
				if dy > 0 {
					dy = maxCoordLimit
				} else {
					dy = -maxCoordLimit
				}
			}
			s.mouse.Move(dx, dy)
		}
		utils.LogDebug("TCP move_abs forwarded: client=%s target=(%.2f, %.2f)", client.ID, targetX, targetY)

	case "click", "left_click":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
		utils.LogDebug("TCP click forwarded: client=%s button=%s", client.ID, button)

	case "mouse_down":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseDown(button)
		}
		utils.LogDebug("TCP mouse_down forwarded: client=%s button=%s", client.ID, button)

	case "mouse_up":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseUp(button)
		}
		utils.LogDebug("TCP mouse_up forwarded: client=%s button=%s", client.ID, button)

	case "doubleclick", "double_click":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
		utils.LogDebug("TCP double_click forwarded: client=%s", client.ID)

	case "rightclick", "right_click":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
		utils.LogDebug("TCP right_click forwarded: client=%s", client.ID)

	case "middle_click", "middleclick":
		if s.mouse != nil {
			s.mouse.Click("middle")
		}
		utils.LogDebug("TCP middle_click forwarded: client=%s", client.ID)

	case "scroll":
		delta := firstInt(payload, "delta", "Scroll", "scroll", "amount", "dy")
		if math.Abs(float64(delta)) > maxScrollLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("TCP scroll dropped (|delta| <= %.1f): delta=%d", maxScrollLimit, delta)
			return
		}
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
		utils.LogDebug("TCP scroll forwarded: client=%s delta=%d", client.ID, delta)

	case "hscroll":
		delta := firstInt(payload, "delta", "hscroll", "amount", "dx")
		if math.Abs(float64(delta)) > maxScrollLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("TCP hscroll dropped (|delta| <= %.1f): delta=%d", maxScrollLimit, delta)
			return
		}
		sysaction.MouseHScroll(delta)
		utils.LogDebug("TCP hscroll forwarded: client=%s delta=%d", client.ID, delta)

	case "slide_next":
		utils.LogInfo("TCP slide_next received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("slide_next")

	case "slide_prev":
		utils.LogInfo("TCP slide_prev received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("slide_prev")

	case "volume_up":
		utils.LogInfo("TCP volume_up received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("volume_up")

	case "volume_down":
		utils.LogInfo("TCP volume_down received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("volume_down")

	case "mute":
		utils.LogInfo("TCP mute received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("mute")

	case "play_pause":
		utils.LogInfo("TCP play_pause received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("play_pause")

	case "lock_screen":
		utils.LogInfo("TCP lock_screen received: client=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("lock_screen")

	case "power_sleep":
		utils.LogInfo("TCP power_sleep received: client=%s", client.ID)
		sysaction.Execute(sysaction.ActionSleep)

	case "brightness":
		dir, _ := payload["direction"].(string)
		if dir == "" {
			dir, _ = payload["action"].(string)
		}
		delta := firstNumber(payload, "delta", "amount", "value")
		if delta < 0 {
			dir = "down"
		} else if dir == "" {
			dir = "up"
		}
		if delta == 0 {
			delta = 10.0
		}
		utils.LogInfo("TCP brightness received: dir=%s delta=%.1f client=%s", dir, delta, client.ID)
		sysaction.AdjustBrightness(dir, math.Abs(delta))

	case "hello", "device_info":
		name, _ := payload["name"].(string)
		version, _ := payload["version"].(string)
		deviceName, _ := payload["device_name"].(string)
		model, _ := payload["model"].(string)
		manufacturer, _ := payload["manufacturer"].(string)
		brand, _ := payload["brand"].(string)
		androidVersion, _ := payload["android_version"].(string)
		sdkInt, _ := payload["sdk_int"].(string)
		deviceIDValue, _ := payload["device_id"].(string)
		protocolName, _ := payload["protocol"].(string)
		transport, _ := payload["transport"].(string)
		fingerprint := deviceIDValue
		if fingerprint == "" {
			fingerprint = device.StableDeviceID(name, deviceName, model, manufacturer, brand)
		}
		token, _ := payload["token"].(string)

		utils.LogInfo("Handshake/Device info received from Android (TCP): id=%s name=%s", client.ID, name)
		utils.LogDebug("TCP hello payload: id=%s version=%s device=%s model=%s android=%s protocol=%s transport=%s", client.ID, version, deviceName, model, androidVersion, protocolName, transport)

		if config.Get().AuthEnabled {
			if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
				errMsg := `{"type":"error","payload":{"message":"connection rejected: invalid pairing token"}}` + "\n"
				_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
				_, _ = client.Conn.Write([]byte(errMsg))
				_ = client.Conn.Close()
				utils.LogInfo("TCP client rejected: id=%s reason=invalid token", client.ID)
				return
			}
		}

		isDeviceInfo := (msgType == "device_info")

		client.mu.Lock()
		if name != "" {
			client.Name = name
		}
		if s.deviceMgr != nil {
			_ = s.deviceMgr.RenameDeviceID(client.ID, fingerprint)
			client.DeviceID = fingerprint
			_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeTCP, name, map[string]string{
				"fingerprint":     fingerprint,
				"ip_address":      client.IP,
				"version":         version,
				"device_name":     deviceName,
				"device_model":    model,
				"manufacturer":    manufacturer,
				"brand":           brand,
				"android_version": androidVersion,
				"device_id":       deviceIDValue,
				"sdk_int":         sdkInt,
				"protocol":        protocolName,
				"transport":       transport,
			})
			_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusPendingApproval)
		}
		client.mu.Unlock()

		if isDeviceInfo || s.autoApproveTCPClient(client, fingerprint) {
			_ = s.ApproveDevice(fingerprint)
			break
		}
		utils.LogInfo("TCP client awaiting approval: id=%s name=%s", client.ID, client.Name)

	case "ping", "heartbeat":
		utils.LogDebug("TCP ping/heartbeat received from client=%s, sending pong", client.ID)
		_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
		_, _ = client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))
		utils.RecordPacketOut(16)

	case "pong":
		utils.LogDebug("TCP pong received from client=%s", client.ID)
		if tsVal, ok := payload["ts"]; ok {
			ts := int64(number(tsVal))
			if ts > 0 {
				rtt := float64(time.Now().UnixMilli() - ts)
				if rtt >= 0 && rtt < 10000 {
					utils.RecordPingLatency(rtt)
				}
			}
		}

	case "stats_req":
		snap := utils.GetTelemetrySnapshot()
		var clicks, scrolls int64
		if s.mouse != nil {
			c, _, _, scr := s.mouse.Stats()
			clicks, scrolls = c, scr
		}
		resp := statsRespMessage(map[string]any{
			"clicks":          clicks,
			"scrolls":         scrolls,
			"uptime_seconds":  snap.UptimeSeconds,
			"packets_per_sec": snap.PacketsPerSec,
			"bytes_in_kbps":   snap.BytesInKBps,
			"bytes_out_kbps":  snap.BytesOutKBps,
			"dropped_packets": snap.DroppedPackets,
			"avg_ping_ms":     snap.AveragePingMs,
		}, id)
		_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
		_, _ = client.Conn.Write(resp)
		utils.RecordPacketOut(int64(len(resp)))

	case "stats_resp", "stats":
		utils.LogDebug("TCP stats update received from client=%s", client.ID)

	case "battery", "battery_report":
		level := firstInt(payload, "level", "battery")
		charging, _ := payload["charging"].(bool)
		devID := client.DeviceID
		if devID == "" {
			devID = client.ID
		}
		utils.LogInfo("TCP battery report received from client=%s: level=%d%% charging=%v", client.ID, level, charging)
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceBattery(devID, level, charging)
		}

	case "gesture":
		gesture, _ := payload["gesture"].(string)
		if gesture == "" {
			gesture, _ = payload["name"].(string)
		}
		confidence := number(payload["confidence"])
		if confidence == 0 {
			confidence = 1.0
		}
		utils.LogInfo("TCP gesture received: client=%s gesture=%s confidence=%.2f", client.ID, gesture, confidence)
		go sysaction.ExecuteGesture(gesture, confidence)

	case "proximity":
		isNear, _ := payload["is_near"].(bool)
		distance := number(payload["distance"])
		utils.LogInfo("TCP proximity update: client=%s near=%v distance=%.2f", client.ID, isNear, distance)

	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "start", "touchpad_start", "resume", "resume_movement":
			common.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by TCP client: client=%s command=%s", client.ID, command)
		case "touchpad_stop", "pause", "pause_movement":
			common.SetMovementPaused(true)
			utils.LogInfo("Movement paused by TCP client: client=%s command=%s", client.ID, command)
		case "show_desktop", "task_view", "switch_window", "lock_screen", "window_close", "zoom_in", "zoom_out", "zoom_reset",
			"volume_up", "volume_down", "mute", "play_pause", "next_track", "prev_track", "stop",
			"window_maximize", "window_minimize", "window_fullscreen",
			"browser_back", "browser_forward", "browser_refresh", "browser_home":
			go func(cmd string) {
				if err := syscmd.ExecuteSystemCommand(cmd); err != nil {
					utils.LogError("TCP system command failed: client=%s command=%s err=%v", client.ID, cmd, err)
				} else {
					utils.LogInfo("TCP system command executed: client=%s command=%s", client.ID, cmd)
				}
			}(command)
		case "calibrate":
			utils.LogInfo("TCP calibration control command received: client=%s", client.ID)
		default:
			utils.LogDebug("Unknown TCP control command: %s", command)
		}

	case "presentation":
		action, _ := payload["action"].(string)
		utils.LogInfo("TCP presentation action received: client=%s action=%s", client.ID, action)
		switch action {
		case "prev":
			_ = syscmd.ExecuteSystemCommand("prev_track")
		case "next":
			_ = syscmd.ExecuteSystemCommand("next_track")
		case "fullscreen":
			_ = syscmd.ExecuteSystemCommand("window_fullscreen")
		}

	case "media":
		action, _ := payload["action"].(string)
		utils.LogInfo("TCP media action received: client=%s action=%s", client.ID, action)
		switch action {
		case "playpause":
			_ = syscmd.ExecuteSystemCommand("play_pause")
		case "prev":
			_ = syscmd.ExecuteSystemCommand("prev_track")
		case "next":
			_ = syscmd.ExecuteSystemCommand("next_track")
		case "volumeup":
			_ = syscmd.ExecuteSystemCommand("volume_up")
		case "volumedown":
			_ = syscmd.ExecuteSystemCommand("volume_down")
		}

	case "laser", "draw", "annotation":
		utils.LogInfo("Received %s data from client %s over TCP", msgType, client.ID)

	case "keypress", "key_press", "key":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("TCP key event %s: key=%s client=%s", msgType, keyVal, client.ID)
			go sysaction.KeyTap(keyVal)
		}

	case "keydown", "key_down":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("TCP key down event: key=%s client=%s", keyVal, client.ID)
			go sysaction.KeyDown(keyVal)
		}

	case "keyup", "key_up":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("TCP key up event: key=%s client=%s", keyVal, client.ID)
			go sysaction.KeyUp(keyVal)
		}

	case "hotkey":
		keyVal, _ := payload["key"].(string)
		var modStr string
		if rawMods, ok := payload["modifiers"].([]any); ok {
			for _, m := range rawMods {
				if ms, ok := m.(string); ok {
					modStr += ms + "+"
				}
			}
		}
		if keyVal != "" {
			utils.LogInfo("TCP hotkey event: mod=%s key=%s client=%s", modStr, keyVal, client.ID)
			go sysaction.KeyTap(modStr + keyVal)
		}

	case "type", "text":
		textVal, _ := payload["text"].(string)
		if textVal == "" {
			textVal, _ = payload["value"].(string)
		}
		if textVal != "" {
			utils.LogInfo("TCP type event %s: client=%s", msgType, client.ID)
			go sysaction.TypeText(textVal)
		}

	case "smarthome":
		utils.LogInfo("Received smarthome command from client %s over TCP", client.ID)

	case "steering":
		angle := firstNumber(payload, "angle")
		utils.LogInfo("Gaming Mode Steering received over TCP: client=%s, angle=%.2f", client.ID, angle)
		if angle > 15 {
			go sysaction.KeyTap("d") // Steer Right
		} else if angle < -15 {
			go sysaction.KeyTap("a") // Steer Left
		}

	case "shooter":
		action, _ := payload["action"].(string)
		utils.LogInfo("Gaming Mode Shooter received over TCP: client=%s, action=%s", client.ID, action)
		if action == "shoot" {
			go sysaction.KeyTap("space")
		} else if action == "reload" {
			go sysaction.KeyTap("r")
		}

	case "calibration_data":
		cp, err := websocketpkg.DecodeCalibrationPayload(payload)
		if err != nil {
			utils.LogError("Failed to decode TCP calibration_data: %v", err)
			return
		}
		utils.LogInfo("TCP calibration_data received: client=%s, quality=%s, gyro_bias=(%.4f,%.4f,%.4f), accel_bias=(%.4f,%.4f,%.4f)",
			client.ID, cp.Quality, cp.Gyro.X, cp.Gyro.Y, cp.Gyro.Z, cp.Accel.X, cp.Accel.Y, cp.Accel.Z)

	case "voice_command":
		command, _ := payload["command"].(string)
		utils.LogInfo("TCP voice command received: command=%s client=%s", command, client.ID)
		cmdLower := strings.ToLower(strings.TrimSpace(command))
		var act sysaction.Action
		switch {
		case strings.Contains(cmdLower, "play") || strings.Contains(cmdLower, "pause") || strings.Contains(cmdLower, "resume"):
			act = sysaction.ActionPlayPause
		case strings.Contains(cmdLower, "next"):
			act = sysaction.ActionNextTrack
		case strings.Contains(cmdLower, "previous") || strings.Contains(cmdLower, "prev"):
			act = sysaction.ActionPrevTrack
		case strings.Contains(cmdLower, "stop"):
			act = sysaction.ActionStop
		case strings.Contains(cmdLower, "volume up") || strings.Contains(cmdLower, "louder"):
			act = sysaction.ActionVolumeUp
		case strings.Contains(cmdLower, "volume down") || strings.Contains(cmdLower, "quieter"):
			act = sysaction.ActionVolumeDown
		case strings.Contains(cmdLower, "mute"):
			act = sysaction.ActionMute
		case strings.Contains(cmdLower, "lock"):
			act = sysaction.ActionLockScreen
		case strings.Contains(cmdLower, "desktop") || strings.Contains(cmdLower, "home"):
			act = sysaction.ActionShowDesktop
		case strings.Contains(cmdLower, "close"):
			act = sysaction.ActionCloseWindow
		case strings.Contains(cmdLower, "copy"):
			act = sysaction.ActionCopy
		case strings.Contains(cmdLower, "paste"):
			act = sysaction.ActionPaste
		case strings.Contains(cmdLower, "undo"):
			act = sysaction.ActionUndo
		case strings.Contains(cmdLower, "redo"):
			act = sysaction.ActionRedo
		}
		if act != "" {
			go sysaction.Execute(act)
		}

	default:
		utils.LogDebug("Unknown TCP message type: type=%s client=%s", msgType, client.ID)
	}
}

func (s *Server) writeAck(client *Client, id *string) {
	ack := ackMessage(id)
	if len(ack) == 0 {
		return
	}
	_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	if _, err := client.Conn.Write(ack); err != nil {
		utils.LogDebug("TCP ack write error: %v", err)
		return
	}
	utils.RecordPacketOut(int64(len(ack)))
	client.mu.Lock()
	client.BytesSent += int64(len(ack))
	devID := client.DeviceID
	if devID == "" {
		devID = client.ID
	}
	client.mu.Unlock()
	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceActivity(devID, int64(len(ack)), 0)
	}
}

func (s *Server) ApproveDevice(deviceID string) error {
	s.mu.RLock()
	var client *Client
	for _, c := range s.clients {
		if c.DeviceID == deviceID || c.ID == deviceID {
			client = c
			break
		}
	}
	s.mu.RUnlock()
	if client == nil {
		return fmt.Errorf("tcp client not found: %s", deviceID)
	}
	client.mu.Lock()
	if client.Approved {
		client.mu.Unlock()
		return nil
	}
	client.mu.Unlock()

	cfg := config.Get()
	welcome := fmt.Sprintf(
		`{"type":"welcome","payload":{"server":"%s","version":"%s","client_id":"%s"}}`+"\n",
		cfg.ServerName, cfg.Version, client.ID)
	_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	if _, err := client.Conn.Write([]byte(welcome)); err != nil {
		return fmt.Errorf("failed to send welcome: %w", err)
	}
	client.mu.Lock()
	client.Approved = true
	client.BytesSent += int64(len(welcome))
	client.mu.Unlock()
	common.SetMovementPaused(false)
	common.ClearPause()

	if s.deviceMgr != nil {
		deviceIDToUpdate := client.DeviceID
		if deviceIDToUpdate == "" {
			deviceIDToUpdate = client.ID
		}
		_ = s.deviceMgr.UpdateDeviceStatus(deviceIDToUpdate, device.StatusConnected)
		cfg.AddTrustedDevice(deviceIDToUpdate)
	} else {
		cfg.AddTrustedDevice(deviceID)
	}
	utils.LogInfo("TCP approval accepted: device=%s", deviceID)
	return nil
}

// ClientSessionSnapshot represents connected TCP client details for live metrics.
type ClientSessionSnapshot struct {
	ID          string    `json:"id"`
	DeviceID    string    `json:"device_id"`
	Name        string    `json:"name"`
	IP          string    `json:"ip"`
	Protocol    string    `json:"protocol"`
	ConnectedAt time.Time `json:"connected_at"`
	LastActive  time.Time `json:"last_active"`
	BytesSent   int64     `json:"bytes_sent"`
	BytesRecv   int64     `json:"bytes_recv"`
	Approved    bool      `json:"approved"`
}

func (s *Server) GetClientSessions() []ClientSessionSnapshot {
	s.mu.RLock()
	defer s.mu.RUnlock()
	sessions := make([]ClientSessionSnapshot, 0, len(s.clients))
	for _, c := range s.clients {
		c.mu.RLock()
		sessions = append(sessions, ClientSessionSnapshot{
			ID:          c.ID,
			DeviceID:    c.DeviceID,
			Name:        c.Name,
			IP:          c.IP,
			Protocol:    "TCP",
			ConnectedAt: c.ConnectedAt,
			LastActive:  c.LastActive,
			BytesSent:   c.BytesSent,
			BytesRecv:   c.BytesRecv,
			Approved:    c.Approved,
		})
		c.mu.RUnlock()
	}
	return sessions
}

func (s *Server) Stop() {
	s.mu.Lock()
	if !s.running {
		s.mu.Unlock()
		return
	}
	s.running = false
	listener := s.listener
	s.listener = nil

	clients := make([]*Client, 0, len(s.clients))
	for _, c := range s.clients {
		clients = append(clients, c)
	}
	s.clients = make(map[string]*Client)
	s.mu.Unlock()

	// 1. Broadcast server_closing packet to all connected clients
	closingMsg := serverClosingMessage()
	for _, c := range clients {
		_ = c.Conn.SetWriteDeadline(time.Now().Add(500 * time.Millisecond))
		_, _ = c.Conn.Write(closingMsg)
	}
	time.Sleep(50 * time.Millisecond) // Short buffer flush window

	// 2. Close listener socket
	if listener != nil {
		_ = listener.Close()
	}

	// 3. Close all client connections cleanly
	for _, c := range clients {
		_ = c.Conn.Close()
	}

	// 4. Reset mouse buttons and keys so nothing remains stuck
	if s.mouse != nil {
		s.mouse.MouseUp("left")
		s.mouse.MouseUp("right")
		s.mouse.MouseUp("middle")
	}
	sysaction.ReleaseAllKeys()

	utils.LogInfo("TCP server stopped cleanly with graceful teardown")
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	var totalSent, totalRecv int64
	var activeCount int
	now := time.Now()
	for _, c := range s.clients {
		c.mu.RLock()
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
		if now.Sub(c.LastActive) < 10*time.Second {
			activeCount++
		}
		c.mu.RUnlock()
	}
	return map[string]interface{}{
		"clients":    len(s.clients),
		"active":     activeCount,
		"bytes_sent": totalSent,
		"bytes_recv": totalRecv,
		"running":    s.running,
		"port":       s.port,
		"host":       s.host,
	}
}

func (s *Server) AddEventListener(callback func(event TCPEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.callbacks = append(s.callbacks, callback)
}

func (s *Server) triggerEvent(event TCPEvent) {
	s.mu.RLock()
	callbacks := make([]func(TCPEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()
	for _, cb := range callbacks {
		go cb(event)
	}
}

func (s *Server) SendToClient(clientID string, msg interface{}) error {
	s.mu.RLock()
	client, exists := s.clients[clientID]
	s.mu.RUnlock()
	if !exists {
		return fmt.Errorf("client not found: %s", clientID)
	}
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	if _, err = client.Conn.Write(append(data, '\n')); err != nil {
		return err
	}
	client.mu.Lock()
	client.BytesSent += int64(len(data) + 1)
	devID := client.DeviceID
	if devID == "" {
		devID = client.ID
	}
	client.mu.Unlock()
	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceActivity(devID, int64(len(data)+1), 0)
	}
	return nil
}

func firstNumber(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return number(v)
		}
	}
	return 0
}

func firstInt(payload map[string]any, keys ...string) int {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return int(number(v))
		}
	}
	return 0
}
