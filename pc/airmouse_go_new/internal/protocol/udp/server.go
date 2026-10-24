package udp

import (
	"encoding/json"
	"fmt"
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
	"airmouse-go/internal/utils"
)

type Server struct {
	port          int
	conn          *net.UDPConn
	deviceMgr     *device.Manager
	mouse         mouse.Controller
	authMgr       *auth.Manager
	running       bool
	mu            sync.RWMutex
	clients       map[string]*UDPClient
	callbacks     []func(event UDPEvent)
	OnScreenFrame func(data []byte)
	seenNonces    map[string]time.Time
	nonceMu       sync.Mutex
}

type UDPClient struct {
	Address    *net.UDPAddr
	LastSeen   time.Time
	DeviceID   string
	DeviceName string
	Approved   bool
	BytesSent  int64
	BytesRecv  int64
	mu         sync.RWMutex
}

type UDPEvent struct {
	Type      string
	ClientIP  string
	Timestamp time.Time
}

func NewServer(port int, mouseCtrl mouse.Controller, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		port:       port,
		mouse:      mouseCtrl,
		deviceMgr:  deviceMgr,
		authMgr:    authMgr,
		clients:    make(map[string]*UDPClient),
		callbacks:  make([]func(UDPEvent), 0),
		seenNonces: make(map[string]time.Time),
	}
}

func (s *Server) Start() error {
	addr := net.UDPAddr{Port: s.port, IP: net.ParseIP("0.0.0.0")}
	conn, err := net.ListenUDP("udp4", &addr)
	if err != nil {
		return fmt.Errorf("failed to listen on UDP port %d: %w", s.port, err)
	}
	s.mu.Lock()
	s.conn = conn
	s.running = true
	s.mu.Unlock()
	go s.listenLoop()
	utils.LogInfo("UDP discovery server started on port %d", s.port)
	utils.LogDebug("UDP discovery bound to %s", addr.String())
	return nil
}

func (s *Server) listenLoop() {
	buf := make([]byte, 65536)
	for {
		n, clientAddr, err := s.conn.ReadFromUDP(buf)
		if err != nil {
			s.mu.RLock()
			running := s.running
			s.mu.RUnlock()
			if !running {
				break
			}
			utils.LogDebug("UDP read error: %v", err)
			continue
		}

		// Check for JPEG binary screen mirroring frame (SOI: 0xFF, 0xD8)
		if n >= 2 && buf[0] == 0xFF && buf[1] == 0xD8 {
			if s.OnScreenFrame != nil {
				// Copy the slice so we don't hold the read buffer
				dataCopy := make([]byte, n)
				copy(dataCopy, buf[:n])
				s.OnScreenFrame(dataCopy)
			}
			continue
		}

		msg := strings.TrimSpace(string(buf[:n]))
		go s.handleMessage(msg, clientAddr)
	}
}

func (s *Server) handleMessage(msg string, clientAddr *net.UDPAddr) {
	clientIP := clientAddr.IP.String()
	utils.LogDebug("UDP inbound packet from %s payload=%q", clientIP, msg)

	clientKey := clientAddr.String()
	s.mu.Lock()
	client, exists := s.clients[clientKey]
	if !exists {
		client = &UDPClient{Address: clientAddr, LastSeen: time.Now()}
		s.clients[clientKey] = client
	}
	client.mu.Lock()
	client.LastSeen = time.Now()
	client.BytesRecv += int64(len(msg))
	devID := client.DeviceID
	client.mu.Unlock()
	s.mu.Unlock()

	if devID != "" && s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceActivity(devID, 0, int64(len(msg)))
	}

	var isDiscovery bool
	var nonce string
	var isJSONDiscovery bool

	trimmedMsg := strings.TrimSpace(msg)
	if trimmedMsg == "AIRMOUSE_DISCOVER" || trimmedMsg == "AIRMOUSE_DISCOVERY" {
		isDiscovery = true
	} else if strings.HasPrefix(trimmedMsg, "AIRMOUSE_DISCOVER:") {
		isDiscovery = true
		parts := strings.SplitN(trimmedMsg, ":", 2)
		if len(parts) == 2 {
			nonce = strings.TrimSpace(parts[1])
		}
	} else if strings.HasPrefix(trimmedMsg, "AIRMOUSE_DISCOVERY:") {
		isDiscovery = true
		parts := strings.SplitN(trimmedMsg, ":", 2)
		if len(parts) == 2 {
			nonce = strings.TrimSpace(parts[1])
		}
	} else if strings.HasPrefix(trimmedMsg, "{") {
		var discReq struct {
			Type  string `json:"type"`
			Nonce string `json:"nonce"`
		}
		if err := json.Unmarshal([]byte(trimmedMsg), &discReq); err == nil {
			if discReq.Type == "discover" || discReq.Type == "discovery" {
				isDiscovery = true
				nonce = discReq.Nonce
				isJSONDiscovery = true
			}
		}
	}

	if isDiscovery {
		isDup := s.isDuplicateNonce(clientIP, nonce)
		if isDup {
			utils.LogDebug("UDP discovery deduplicated for nonce %q from %s", nonce, clientIP)
			// Resend cached response without re-triggering discovery event
			s.sendDiscoveryResponseWithNonce(clientAddr, nonce, isJSONDiscovery)
			return
		}
		utils.LogDebug("UDP discovery request from %s (nonce: %s)", clientIP, nonce)
		s.sendDiscoveryResponseWithNonce(clientAddr, nonce, isJSONDiscovery)
		s.triggerEvent(UDPEvent{Type: "discovery", ClientIP: clientIP, Timestamp: time.Now()})
		return
	}

	msgType, payload, id, err := websocketpkg.DecodeWireMessage([]byte(msg))
	if err != nil {
		if msg == "AIRMOUSE_HELLO" {
			s.triggerEvent(UDPEvent{Type: "hello", ClientIP: clientIP, Timestamp: time.Now()})
		} else {
			utils.LogDebug("UDP unknown message from %s: %s", clientIP, msg)
		}
		return
	}

	if id != nil {
		ack := websocketpkg.AckMessage(id)
		if len(ack) > 0 {
			s.writeToClient(clientKey, clientAddr, ack)
			utils.LogDebug("UDP ack sent: client=%s type=%s id=%s", clientIP, msgType, *id)
		}
	}

	client.mu.RLock()
	approved := client.Approved
	client.mu.RUnlock()
	if !approved && msgType != "hello" && msgType != "ping" {
		utils.LogDebug("Ignoring UDP %s while waiting for approval: client=%s", msgType, clientIP)
		return
	}

	switch msgType {
	case "move":
		dx := firstNumber(payload, "dx", "DeltaX", "deltaX")
		dy := firstNumber(payload, "dy", "DeltaY", "deltaY")
		if s.mouse != nil {
			s.mouse.Move(dx, dy)
		}
	case "click":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
		utils.LogDebug("UDP click received: client=%s button=%s", clientIP, button)
	case "mouse_down":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseDown(button)
		}
		utils.LogDebug("UDP mouse_down received: client=%s button=%s", clientIP, button)
	case "mouse_up":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseUp(button)
		}
		utils.LogDebug("UDP mouse_up received: client=%s button=%s", clientIP, button)
	case "doubleclick":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
	case "rightclick":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
	case "scroll":
		delta := int(firstNumber(payload, "delta", "Scroll", "scroll"))
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
	case "hello":
		s.handleHello(clientKey, clientAddr, client, payload)
	case "gesture":
		gesture, _ := payload["gesture"].(string)
		confidence := firstNumber(payload, "confidence")
		utils.LogInfo("UDP gesture received: client=%s gesture=%s confidence=%.2f", clientIP, gesture, confidence)
	case "proximity":
		isNear, _ := payload["is_near"].(bool)
		distance := firstNumber(payload, "distance")
		utils.LogInfo("UDP proximity update: client=%s near=%v distance=%.2f", clientIP, isNear, distance)
	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "start", "touchpad_start", "resume", "resume_movement":
			common.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by UDP client: %s command=%s", clientIP, command)
		case "touchpad_stop", "pause", "pause_movement":
			common.SetMovementPaused(true)
			utils.LogInfo("Movement paused by UDP client: %s command=%s", clientIP, command)
		case "show_desktop", "task_view", "switch_window", "lock_screen", "window_close", "zoom_in", "zoom_out", "zoom_reset",
			"volume_up", "volume_down", "mute", "play_pause", "next_track", "prev_track", "stop",
			"window_maximize", "window_minimize", "window_fullscreen",
			"browser_back", "browser_forward", "browser_refresh", "browser_home":
			go func(cmd string) {
				if err := syscmd.ExecuteSystemCommand(cmd); err != nil {
					utils.LogError("UDP system command failed: %s command=%s err=%v", clientIP, cmd, err)
				} else {
					utils.LogInfo("UDP system command executed: %s command=%s", clientIP, cmd)
				}
			}(command)
		case "calibrate":
			utils.LogInfo("UDP calibration control command received: %s", clientIP)
		default:
			utils.LogDebug("Unknown UDP control command: %s", command)
		}

	case "presentation":
		if !approved {
			return
		}
		action, _ := payload["action"].(string)
		utils.LogInfo("UDP presentation action received: client=%s action=%s", clientIP, action)
		switch action {
		case "prev":
			_ = syscmd.ExecuteSystemCommand("prev_track")
		case "next":
			_ = syscmd.ExecuteSystemCommand("next_track")
		case "fullscreen":
			_ = syscmd.ExecuteSystemCommand("window_fullscreen")
		}

	case "media":
		if !approved {
			return
		}
		action, _ := payload["action"].(string)
		utils.LogInfo("UDP media action received: client=%s action=%s", clientIP, action)
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
		utils.LogInfo("Received %s data from client %s over UDP", msgType, clientIP)

	case "steering":
		angle := firstNumber(payload, "angle")
		utils.LogInfo("Gaming Mode Steering received over UDP: client=%s, angle=%.2f", clientIP, angle)
		if angle > 15 {
			go sysaction.KeyTap("d") // Steer Right
		} else if angle < -15 {
			go sysaction.KeyTap("a") // Steer Left
		}

	case "shooter":
		action, _ := payload["action"].(string)
		utils.LogInfo("Gaming Mode Shooter received over UDP: client=%s, action=%s", clientIP, action)
		if action == "shoot" {
			go sysaction.KeyTap("space")
		} else if action == "reload" {
			go sysaction.KeyTap("r")
		}

	case "keypress", "keydown", "keyup", "key":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("UDP key event %s: key=%s client=%s", msgType, keyVal, clientIP)
			go sysaction.KeyTap(keyVal)
		}

	case "type", "text":
		textVal, _ := payload["text"].(string)
		if textVal == "" {
			textVal, _ = payload["value"].(string)
		}
		if textVal != "" {
			utils.LogInfo("UDP type event %s: client=%s", msgType, clientIP)
			go sysaction.TypeText(textVal)
		}

	case "smarthome":
		utils.LogInfo("Received smarthome command from client %s over UDP", clientIP)

	case "calibration_data":
		if !approved {
			utils.LogDebug("Ignoring UDP calibration_data while waiting for approval: %s", clientIP)
			return
		}
		cp, err := websocketpkg.DecodeCalibrationPayload(payload)
		if err != nil {
			utils.LogError("Failed to decode UDP calibration_data: %v", err)
			return
		}
		utils.LogInfo("UDP calibration_data received: client=%s, quality=%s, gyro_bias=(%.4f,%.4f,%.4f), accel_bias=(%.4f,%.4f,%.4f)",
			clientIP, cp.Quality, cp.Gyro.X, cp.Gyro.Y, cp.Gyro.Z, cp.Accel.X, cp.Accel.Y, cp.Accel.Z)
	case "ping":
		s.writeToClient(clientKey, clientAddr, websocketpkg.PongMessage())
	case "pong":
		utils.LogDebug("UDP pong received from %s", clientIP)
	default:
		utils.LogDebug("UDP message type=%s from %s", msgType, clientIP)
	}
}

func (s *Server) isDuplicateNonce(clientIP, nonce string) bool {
	if nonce == "" {
		return false
	}
	s.nonceMu.Lock()
	defer s.nonceMu.Unlock()

	now := time.Now()
	// Evict entries older than 10 seconds
	for k, t := range s.seenNonces {
		if now.Sub(t) > 10*time.Second {
			delete(s.seenNonces, k)
		}
	}

	key := clientIP + ":" + nonce
	if lastSeen, found := s.seenNonces[key]; found {
		if now.Sub(lastSeen) < 5*time.Second {
			return true
		}
	}
	s.seenNonces[key] = now
	return false
}

func (s *Server) sendDiscoveryResponse(clientAddr *net.UDPAddr) {
	s.sendDiscoveryResponseWithNonce(clientAddr, "", false)
}

func (s *Server) sendDiscoveryResponseWithNonce(clientAddr *net.UDPAddr, nonce string, jsonFormat bool) {
	cfg := config.Get()
	var responseBytes []byte

	if jsonFormat {
		respMap := map[string]any{
			"type":    "discovery_response",
			"ip":      getLocalIP(),
			"port":    cfg.UDPPort,
			"name":    cfg.ServerName,
			"version": cfg.Version,
		}
		if nonce != "" {
			respMap["nonce"] = nonce
		}
		responseBytes, _ = json.Marshal(respMap)
	} else {
		var response string
		if nonce != "" {
			response = fmt.Sprintf("AIRMOUSE_SERVER:%d:%s:%s:%s", cfg.UDPPort, cfg.ServerName, cfg.Version, nonce)
		} else {
			response = fmt.Sprintf("AIRMOUSE_SERVER:%d:%s:%s", cfg.UDPPort, cfg.ServerName, cfg.Version)
		}
		responseBytes = []byte(response)
	}

	s.mu.RLock()
	conn := s.conn
	running := s.running
	s.mu.RUnlock()
	if !running || conn == nil {
		return
	}
	_, err := conn.WriteToUDP(responseBytes, clientAddr)
	if err != nil {
		utils.LogDebug("Failed to send discovery response: %v", err)
	}
	utils.LogInfo("UDP discovery response sent to %s on port %d (nonce: %s)",
		clientAddr.IP.String(), cfg.UDPPort, nonce)
}

func (s *Server) sendDiscoveryResponseWithRetry(clientAddr *net.UDPAddr, nonce string, jsonFormat bool, retries int) {
	if retries <= 1 {
		s.sendDiscoveryResponseWithNonce(clientAddr, nonce, jsonFormat)
		return
	}
	for i := 0; i < retries; i++ {
		s.sendDiscoveryResponseWithNonce(clientAddr, nonce, jsonFormat)
		if i < retries-1 {
			time.Sleep(15 * time.Millisecond)
		}
	}
}

// BroadcastDiscovery broadcasts discovery presence with 3 retries across network interfaces.
func (s *Server) BroadcastDiscovery() error {
	return s.BroadcastDiscoveryWithRetries(3)
}

// BroadcastDiscoveryWithRetries broadcasts discovery presence with configurable retry count.
func (s *Server) BroadcastDiscoveryWithRetries(retries int) error {
	if retries <= 0 {
		retries = 3
	}
	cfg := config.Get()
	nonce := fmt.Sprintf("%x", time.Now().UnixNano())
	payload := fmt.Sprintf("AIRMOUSE_SERVER:%d:%s:%s:%s", cfg.UDPPort, cfg.ServerName, cfg.Version, nonce)
	data := []byte(payload)

	var bcastAddrs []*net.UDPAddr
	bcastAddrs = append(bcastAddrs, &net.UDPAddr{IP: net.IPv4bcast, Port: s.port})

	if ifaces, err := net.Interfaces(); err == nil {
		for _, iface := range ifaces {
			if iface.Flags&net.FlagUp == 0 || iface.Flags&net.FlagLoopback != 0 {
				continue
			}
			addrs, err := iface.Addrs()
			if err != nil {
				continue
			}
			for _, addr := range addrs {
				if ipnet, ok := addr.(*net.IPNet); ok && ipnet.IP.To4() != nil {
					ip := ipnet.IP.To4()
					mask := ipnet.Mask
					if len(mask) == 4 {
						bcast := net.IPv4(
							ip[0]|^mask[0],
							ip[1]|^mask[1],
							ip[2]|^mask[2],
							ip[3]|^mask[3],
						)
						bcastAddrs = append(bcastAddrs, &net.UDPAddr{IP: bcast, Port: s.port})
					}
				}
			}
		}
	}

	for r := 0; r < retries; r++ {
		s.mu.RLock()
		conn := s.conn
		running := s.running
		s.mu.RUnlock()
		if !running || conn == nil {
			break
		}
		for _, bcast := range bcastAddrs {
			_, _ = conn.WriteToUDP(data, bcast)
		}
		if r < retries-1 {
			time.Sleep(25 * time.Millisecond)
		}
	}
	utils.LogInfo("UDP discovery broadcast completed (%d retries, nonce: %s)", retries, nonce)
	return nil
}

func (s *Server) handleHello(clientKey string, clientAddr *net.UDPAddr, client *UDPClient, payload map[string]any) {
	cfg := config.Get()
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
	token, _ := payload["token"].(string)
	fingerprint := deviceIDValue
	if fingerprint == "" {
		fingerprint = device.StableDeviceID(name, deviceName, model, manufacturer, brand)
	}

	if name == "" {
		name = "Unknown"
	}

	if cfg.AuthEnabled {
		if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
			_ = s.writeToClient(clientKey, clientAddr, []byte(`{"type":"error","payload":{"message":"connection rejected: invalid pairing token"}}`+"\n"))
			utils.LogInfo("UDP client rejected: addr=%s reason=invalid token", clientAddr.String())
			return
		}
	}

	// Auto‑approval check
	if s.autoApproveUDPClient(clientKey, client, fingerprint) {
		return
	}

	client.mu.Lock()
	client.DeviceName = name
	client.DeviceID = fingerprint
	client.mu.Unlock()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeUDP, name, map[string]string{
			"fingerprint":     fingerprint,
			"ip_address":      clientAddr.IP.String(),
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
	utils.LogInfo("UDP client awaiting approval: addr=%s name=%s", clientAddr.String(), name)
}

func (s *Server) writeToClient(clientKey string, clientAddr *net.UDPAddr, data []byte) error {
	if len(data) == 0 {
		return nil
	}
	if _, err := s.conn.WriteToUDP(data, clientAddr); err != nil {
		utils.LogDebug("Failed to send UDP message to %s: %v", clientAddr.String(), err)
		return err
	}
	s.mu.Lock()
	var devID string
	if client, ok := s.clients[clientKey]; ok {
		client.mu.Lock()
		client.BytesSent += int64(len(data))
		devID = client.DeviceID
		client.mu.Unlock()
	}
	s.mu.Unlock()

	if devID != "" && s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceActivity(devID, int64(len(data)), 0)
	}
	return nil
}

func (s *Server) Stop() {
	s.mu.Lock()
	s.running = false
	if s.conn != nil {
		_ = s.conn.Close()
	}
	s.clients = make(map[string]*UDPClient)
	s.mu.Unlock()
	utils.LogInfo("UDP discovery server stopped")
}

func (s *Server) ApproveDevice(deviceID string) error {
	s.mu.RLock()
	var clientKey string
	var client *UDPClient
	for key, c := range s.clients {
		c.mu.RLock()
		devID := c.DeviceID
		addr := c.Address.String()
		c.mu.RUnlock()
		if devID == deviceID || addr == deviceID {
			clientKey = key
			client = c
			break
		}
	}
	s.mu.RUnlock()
	if client == nil || client.Address == nil {
		return fmt.Errorf("udp client not found: %s", deviceID)
	}
	client.mu.Lock()
	if client.Approved {
		client.mu.Unlock()
		return nil
	}
	client.mu.Unlock()

	cfg := config.Get()
	welcome := websocketpkg.WelcomeMessage(cfg.ServerName, cfg.Version)
	if err := s.writeToClient(clientKey, client.Address, welcome); err != nil {
		return fmt.Errorf("failed to send welcome: %w", err)
	}
	client.mu.Lock()
	client.Approved = true
	client.mu.Unlock()

	common.SetMovementPaused(false)
	common.ClearPause()

	if s.deviceMgr != nil {
		client.mu.RLock()
		updateID := client.DeviceID
		client.mu.RUnlock()
		if updateID == "" {
			updateID = deviceID
		}
		_ = s.deviceMgr.UpdateDeviceStatus(updateID, device.StatusConnected)
		cfg.AddTrustedDevice(updateID)
	} else {
		cfg.AddTrustedDevice(deviceID)
	}
	utils.LogInfo("UDP approval accepted: device=%s", deviceID)
	return nil
}

// autoApproveUDPClient checks if the fingerprint is trusted and auto‑approves.
func (s *Server) autoApproveUDPClient(clientKey string, client *UDPClient, fingerprint string) bool {
	if s.deviceMgr == nil || fingerprint == "" {
		return false
	}
	if !config.Get().IsTrustedDevice(fingerprint) {
		return false
	}
	_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeUDP, client.DeviceName, map[string]string{
		"fingerprint": fingerprint,
		"ip_address":  client.Address.IP.String(),
	})
	_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusConnected)
	cfg := config.Get()
	welcome := websocketpkg.WelcomeMessage(cfg.ServerName, cfg.Version)
	if err := s.writeToClient(clientKey, client.Address, welcome); err == nil {
		client.mu.Lock()
		client.Approved = true
		client.DeviceID = fingerprint
		client.mu.Unlock()
		utils.LogInfo("UDP auto-approved trusted device: %s (fingerprint: %s)", client.Address.String(), fingerprint)
		common.SetMovementPaused(false)
		common.ClearPause()
		return true
	}
	return false
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	activeClients := 0
	now := time.Now()
	for _, c := range s.clients {
		c.mu.RLock()
		if now.Sub(c.LastSeen) < 30*time.Second {
			activeClients++
		}
		c.mu.RUnlock()
	}
	return map[string]interface{}{
		"running":        s.running,
		"port":           s.port,
		"total_clients":  len(s.clients),
		"active_clients": activeClients,
	}
}

func (s *Server) AddEventListener(callback func(event UDPEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if callback != nil {
		s.callbacks = append(s.callbacks, callback)
	}
}

func (s *Server) triggerEvent(event UDPEvent) {
	s.mu.RLock()
	callbacks := make([]func(UDPEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()
	for _, cb := range callbacks {
		go cb(event)
	}
}

func (s *Server) BroadcastMessage(msg interface{}) error {
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	s.mu.RLock()
	defer s.mu.RUnlock()
	for _, client := range s.clients {
		_, _ = s.conn.WriteToUDP(data, client.Address)
	}
	return nil
}

func (s *Server) GetConnectedClients() []*UDPClient {
	s.mu.RLock()
	defer s.mu.RUnlock()
	clients := make([]*UDPClient, 0, len(s.clients))
	for _, c := range s.clients {
		clients = append(clients, c)
	}
	return clients
}

func firstNumber(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			switch t := v.(type) {
			case float64:
				return t
			case float32:
				return float64(t)
			case int:
				return float64(t)
			case int64:
				return float64(t)
			case json.Number:
				f, _ := t.Float64()
				return f
			}
		}
	}
	return 0
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			return ipnet.IP.String()
		}
	}
	return "127.0.0.1"
}
