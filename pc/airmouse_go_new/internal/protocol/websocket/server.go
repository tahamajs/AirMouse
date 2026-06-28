package websocket

import (
	"crypto/md5"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"hash"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	gorilla "github.com/gorilla/websocket"

	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/utils"
)

var upgrader = gorilla.Upgrader{
	CheckOrigin:       func(r *http.Request) bool { return true },
	ReadBufferSize:    4096,
	WriteBufferSize:   4096,
	EnableCompression: true,
}

// ------------------------------------------------------------
// WSClient - WebSocket client
// ------------------------------------------------------------

type WSClient struct {
	ID          string
	DeviceID    string
	Name        string
	Conn        *gorilla.Conn
	Send        chan []byte
	BinarySend  chan []byte
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	UserAgent   string
	IP          string
	Approved    atomic.Bool
}

// ------------------------------------------------------------
// Server - WebSocket server
// ------------------------------------------------------------

type Server struct {
	port         int
	listener     net.Listener
	clients      map[string]*WSClient
	mouse        mouse.Controller
	deviceMgr    *device.Manager
	authMgr      *auth.Manager
	proxMgr      *proximity.Manager
	mu           sync.RWMutex
	server       *http.Server
	running      bool
	totalClients int64
	startTime    time.Time
	fileSessions map[string]*fileSession
}

type fileSession struct {
	id        string
	name      string
	path      string
	tempPath  string
	size      int64
	received  int64
	md5Hex    string
	direction string
	hasher    hash.Hash
	file      *os.File
}

// NewServer creates a new WebSocket server.
func NewServer(port int, mouseCtrl mouse.Controller, deviceMgr *device.Manager, authMgr *auth.Manager, proxMgr *proximity.Manager) *Server {
	return &Server{
		port:         port,
		clients:      make(map[string]*WSClient),
		mouse:        mouseCtrl,
		deviceMgr:    deviceMgr,
		authMgr:      authMgr,
		proxMgr:      proxMgr,
		fileSessions: make(map[string]*fileSession),
		startTime:    time.Now(),
	}
}

// Start starts the WebSocket server.
func (s *Server) Start() error {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", s.port))
	if err != nil {
		return fmt.Errorf("failed to bind WebSocket port %d: %w", s.port, err)
	}
	s.listener = listener

	mux := http.NewServeMux()
	mux.HandleFunc("/ws", s.handleWebSocket)
	mux.HandleFunc("/health", s.handleHealth)
	mux.HandleFunc("/stats", s.handleStats)

	s.server = &http.Server{
		Handler:      mux,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	s.mu.Lock()
	s.running = true
	s.startTime = time.Now()
	s.mu.Unlock()

	go func() {
		utils.LogInfo("WebSocket server starting on port %d", s.port)
		if err := s.server.Serve(s.listener); err != nil && err != http.ErrServerClosed {
			utils.LogError("WebSocket server error: %v", err)
		}
	}()
	return nil
}

// Stop stops the WebSocket server.
func (s *Server) Stop() error {
	s.mu.Lock()
	if !s.running {
		s.mu.Unlock()
		return nil
	}
	s.running = false
	s.mu.Unlock()

	if s.listener != nil {
		_ = s.listener.Close()
	}
	if s.server != nil {
		_ = s.server.Close()
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	for _, c := range s.clients {
		close(c.Send)
		close(c.BinarySend)
		_ = c.Conn.Close()
	}
	s.clients = make(map[string]*WSClient)

	utils.LogInfo("WebSocket server stopped on port %d", s.port)
	return nil
}

// handleWebSocket handles WebSocket connections.
func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	cfg := config.Get()

	if cfg.AuthEnabled {
		token := r.URL.Query().Get("token")
		if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte(`{"error":"unauthorized"}`))
			return
		}
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		utils.LogError("WebSocket upgrade failed: %v", err)
		return
	}

	id := utils.GenerateID()
	client := &WSClient{
		ID:          id,
		Name:        "Unknown",
		Conn:        conn,
		Send:        make(chan []byte, 256),
		BinarySend:  make(chan []byte, 128),
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
		UserAgent:   r.UserAgent(),
		IP:          r.RemoteAddr,
	}
	client.Approved.Store(false)

	s.mu.Lock()
	s.clients[id] = client
	s.totalClients++
	s.mu.Unlock()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.RegisterDevice(id, device.TypeWebSocket, "Android")
		_ = s.deviceMgr.UpdateDeviceStatus(id, device.StatusPendingApproval)
	}

	utils.LogInfo("WebSocket client connected: id=%s, ip=%s", id, client.IP)
	utils.LogInfo("WebSocket approval pending: id=%s", id)
	utils.LogDebug("WebSocket initial client state: id=%s approved=%v device=%s", id, client.Approved.Load(), client.DeviceID)

	go s.readLoop(client)
	go s.writeLoop(client)
}

// handleHealth handles health checks.
func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_, _ = w.Write([]byte(`{"status":"ok","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}

// handleStats handles stats requests.
func (s *Server) handleStats(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	clientCount := len(s.clients)
	s.mu.RUnlock()

	w.Header().Set("Content-Type", "application/json")
	_, _ = fmt.Fprintf(w, `{"clients":%d,"running":%v,"uptime":%.0f}`, clientCount, s.running, time.Since(s.startTime).Seconds())
}

// readLoop reads messages from the client.
func (s *Server) readLoop(client *WSClient) {
	defer func() {
		_ = client.Conn.Close()
		close(client.Send)
		close(client.BinarySend)

		s.mu.Lock()
		delete(s.clients, client.ID)
		delete(s.fileSessions, client.ID)
		s.mu.Unlock()

		if s.deviceMgr != nil {
			deviceID := client.DeviceID
			if deviceID == "" {
				deviceID = client.ID
			}
			_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusDisconnected)
		}
		utils.LogInfo("WebSocket client disconnected: id=%s", client.ID)
	}()

	for {
		messageType, data, err := client.Conn.ReadMessage()
		if err != nil {
			if !gorilla.IsUnexpectedCloseError(err, gorilla.CloseGoingAway, gorilla.CloseAbnormalClosure) {
				utils.LogDebug("WebSocket read error: %v", err)
			}
			return
		}

		s.mu.Lock()
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(data))
		s.mu.Unlock()

		if messageType == gorilla.BinaryMessage {
			if s.handleBinaryFileChunk(client, data) {
				continue
			}
			utils.LogDebug("Unexpected binary frame from device: %s", client.ID)
			continue
		}

		msgType, payload, id, err := DecodeWireMessage(data)
		if err != nil {
			utils.LogDebug("Invalid WebSocket message: %v", err)
			continue
		}

		s.processMessage(client, msgType, payload, id)
	}
}

// writeLoop writes messages to the client.
func (s *Server) writeLoop(client *WSClient) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case msg, ok := <-client.Send:
			if !ok {
				return
			}
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.TextMessage, msg); err != nil {
				utils.LogDebug("WebSocket write error: %v", err)
				return
			}
			s.mu.Lock()
			client.BytesSent += int64(len(msg))
			s.mu.Unlock()

		case data, ok := <-client.BinarySend:
			if !ok {
				return
			}
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.BinaryMessage, data); err != nil {
				utils.LogDebug("WebSocket binary write error: %v", err)
				return
			}
			s.mu.Lock()
			client.BytesSent += int64(len(data))
			s.mu.Unlock()

		case <-ticker.C:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// processMessage processes incoming messages.
func (s *Server) processMessage(client *WSClient, msgType string, payload map[string]any, id *string) {
	if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "rightclick" || msgType == "scroll") {
		if ack := AckMessage(id); len(ack) > 0 {
			select {
			case client.Send <- ack:
			default:
				utils.LogDebug("WebSocket ack dropped (client send buffer full): %s", client.ID)
			}
		}
	}

	approved := client.Approved.Load()
	if !approved && msgType != "hello" && msgType != "ping" {
		utils.LogDebug("Ignoring WebSocket %s while waiting for approval: device=%s", msgType, client.ID)
		return
	}

	switch msgType {
	case "file":
		s.processFileMessage(client, payload)

	case "move":
		dx := firstFloat64(payload, "dx", "DeltaX", "deltaX")
		dy := firstFloat64(payload, "dy", "DeltaY", "deltaY")
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
		utils.LogDebug("Click received: device=%s, button=%s", client.ID, button)

	case "doubleclick":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
		utils.LogDebug("Double click received: device=%s", client.ID)

	case "rightclick":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
		utils.LogDebug("Right click received: device=%s", client.ID)

	case "scroll":
		delta := firstInt(payload, "delta", "Scroll", "scroll")
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
		utils.LogDebug("Scroll received: device=%s, delta=%d", client.ID, delta)

	case "hello":
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
		fingerprint := device.StableDeviceID(deviceIDValue, name, version, deviceName, model, manufacturer, brand, androidVersion, sdkInt, protocolName, transport)
		if name == "" {
			name = "Unknown"
		}
		utils.LogInfo("Handshake received from Android (WebSocket): id=%s name=%s", client.ID, name)
		utils.LogDebug("WebSocket hello payload: id=%s version=%s device=%s model=%s android=%s protocol=%s transport=%s", client.ID, version, deviceName, model, androidVersion, protocolName, transport)

		// Auto-approve if trusted
		if s.autoApproveWSClient(client, fingerprint) {
			break
		}

		// Otherwise mark pending
		s.mu.Lock()
		client.Name = name
		if s.deviceMgr != nil {
			_ = s.deviceMgr.RenameDeviceID(client.ID, fingerprint)
			client.DeviceID = fingerprint
			_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeWebSocket, name, map[string]string{
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
				"user_agent":      client.UserAgent,
			})
			_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusPendingApproval)
		}
		s.mu.Unlock()

		utils.LogInfo("WebSocket client awaiting approval: id=%s, name=%s", client.ID, name)
		utils.LogDebug("WebSocket approval state pending: id=%s approved=%v device_id=%s", client.ID, client.Approved.Load(), client.DeviceID)

	case "gesture":
		gesture, _ := payload["gesture"].(string)
		confidence := toFloat64(payload["confidence"])
		utils.LogInfo("Gesture received: device=%s, gesture=%s, confidence=%.2f", client.ID, gesture, confidence)

	case "proximity":
		if s.proxMgr == nil {
			utils.LogDebug("Proximity manager not available, ignoring proximity update")
			break
		}
		deviceID, _ := payload["device_id"].(string)
		isNear, _ := payload["is_near"].(bool)
		distance := firstFloat64(payload, "distance")
		rssi := int32(firstFloat64(payload, "rssi"))

		update := proximity.ProximityUpdate{
			DeviceID:  deviceID,
			IsNear:    isNear,
			Distance:  float32(distance),
			RSSI:      rssi,
			Timestamp: time.Now().Unix(),
		}
		s.proxMgr.ProcessUpdate(update)
		utils.LogDebug("Proximity update: device=%s near=%v dist=%.2f", deviceID, isNear, distance)

	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "start", "touchpad_start", "resume", "resume_movement":
			common.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by device: %s command=%s", client.ID, command)
		case "touchpad_stop", "pause", "pause_movement":
			common.SetMovementPaused(true)
			utils.LogInfo("Movement paused by device: %s command=%s", client.ID, command)
		case "show_desktop", "task_view", "switch_window", "lock_screen", "window_close", "zoom_in", "zoom_out", "zoom_reset",
			"volume_up", "volume_down", "mute", "play_pause", "next_track", "prev_track", "stop",
			"window_maximize", "window_minimize", "window_fullscreen",
			"browser_back", "browser_forward", "browser_refresh", "browser_home":
			go func(cmd string) {
				if err := syscmd.ExecuteSystemCommand(cmd); err != nil {
					utils.LogError("WebSocket system command failed: device=%s command=%s err=%v", client.ID, cmd, err)
				} else {
					utils.LogInfo("WebSocket system command executed: device=%s command=%s", client.ID, cmd)
				}
			}(command)
		case "calibrate":
			utils.LogInfo("WebSocket calibration control command received: device=%s", client.ID)
		default:
			utils.LogDebug("Unknown control command: %s", command)
		}

	case "presentation":
		action, _ := payload["action"].(string)
		utils.LogInfo("Presentation action received: device=%s action=%s", client.ID, action)
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
		utils.LogInfo("Media action received: device=%s action=%s", client.ID, action)
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
		utils.LogInfo("Received %s data from client %s", msgType, client.ID)

	case "keypress", "keydown", "keyup", "type":
		utils.LogInfo("Received keyboard event %s from client %s", msgType, client.ID)

	case "smarthome":
		utils.LogInfo("Received smarthome command from client %s", client.ID)


	case "calibration_data":
		if !client.Approved.Load() {
			utils.LogDebug("Ignoring WebSocket calibration_data while waiting for approval: %s", client.ID)
			return
		}
		utils.LogInfo("WebSocket calibration_data received: device=%s", client.ID)

	case "ping":
		select {
		case client.Send <- PongMessage():
		default:
		}

	case "pong":
		utils.LogDebug("Pong received from: %s", client.ID)

	default:
		utils.LogDebug("Unknown message type: %s from device: %s", msgType, client.ID)
	}
}

// autoApproveWSClient checks if the device is trusted and auto-approves.
func (s *Server) autoApproveWSClient(client *WSClient, fingerprint string) bool {
	if s.deviceMgr == nil || fingerprint == "" {
		return false
	}
	// Use config trusted list
	if !config.Get().IsTrustedDevice(fingerprint) {
		return false
	}
	// Update device manager to connected state
	_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeWebSocket, client.Name, map[string]string{
		"fingerprint": fingerprint,
		"ip_address":  client.IP,
		"user_agent":  client.UserAgent,
	})
	_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusConnected)

	cfg := config.Get()
	welcomeMsg := WelcomeMessage(cfg.ServerName, cfg.Version)
	select {
	case client.Send <- welcomeMsg:
		client.Approved.Store(true)
		client.DeviceID = fingerprint
		common.SetMovementPaused(false)
		common.ClearPause()
		utils.LogInfo("WebSocket auto-approved trusted device: %s (fingerprint: %s)", client.ID, fingerprint)
		return true
	default:
		utils.LogWarn("WebSocket auto-approve welcome send blocked: %s", client.ID)
		return false
	}
}

// ApproveDevice approves a pending WebSocket client and sends the welcome message.
func (s *Server) ApproveDevice(deviceID string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	var client *WSClient
	for _, c := range s.clients {
		if c.DeviceID == deviceID || c.ID == deviceID {
			client = c
			break
		}
	}
	if client == nil {
		return fmt.Errorf("websocket client not found: %s", deviceID)
	}

	if client.Approved.Load() {
		return nil
	}

	client.Approved.Store(true)
	common.SetMovementPaused(false)
	common.ClearPause()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusConnected)
	}

	cfg := config.Get()
	cfg.AddTrustedDevice(deviceID)

	welcomeMsg := WelcomeMessage(cfg.ServerName, cfg.Version)
	select {
	case client.Send <- welcomeMsg:
		utils.LogInfo("WebSocket approval accepted and welcome sent: device=%s", deviceID)
	default:
		utils.LogWarn("WebSocket approval accepted but welcome send blocked (channel full): device=%s", deviceID)
	}
	return nil
}

// ---------------------------------------------------------------------
// File transfer helpers
// ---------------------------------------------------------------------

func (s *Server) processFileMessage(client *WSClient, payload map[string]any) {
	action, _ := payload["action"].(string)
	id, _ := payload["id"].(string)
	name, _ := payload["name"].(string)

	switch action {
	case "start":
		if id == "" || name == "" {
			select {
			case client.Send <- []byte(`{"type":"file","action":"error","message":"missing file metadata"}` + "\n"):
			default:
			}
			return
		}
		s.mu.Lock()
		dir := s.fileTransferDir()
		_ = os.MkdirAll(dir, 0755)
		tempPath := filepath.Join(dir, fmt.Sprintf(".upload_%s_%s.part", id, sanitizeFileName(name)))
		file, err := os.OpenFile(tempPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
		if err != nil {
			s.mu.Unlock()
			select {
			case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, err.Error())):
			default:
			}
			return
		}
		session := &fileSession{
			id:        id,
			name:      name,
			path:      filepath.Join(dir, sanitizeFileName(name)),
			tempPath:  tempPath,
			size:      toInt64(payload["size"]),
			md5Hex:    strings.ToLower(toString(payload["md5"])),
			direction: "upload",
			hasher:    md5.New(),
			file:      file,
		}
		s.fileSessions[client.ID] = session
		s.mu.Unlock()
		select {
		case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"ack","id":"%s"}`+"\n", id)):
		default:
		}

	case "complete":
		s.mu.Lock()
		session := s.fileSessions[client.ID]
		delete(s.fileSessions, client.ID)
		s.mu.Unlock()
		if session == nil || session.id != id {
			return
		}
		if session.file != nil {
			_ = session.file.Close()
		}
		actualMD5 := ""
		data, readErr := os.ReadFile(session.tempPath)
		if readErr == nil {
			sum := md5.Sum(data)
			actualMD5 = hex.EncodeToString(sum[:])
		}
		if session.md5Hex != "" && actualMD5 != session.md5Hex {
			_ = os.Remove(session.tempPath)
			select {
			case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"md5 mismatch"}`+"\n", id)):
			default:
			}
			return
		}
		if readErr == nil {
			_ = os.WriteFile(session.path, data, 0644)
			_ = os.Remove(session.tempPath)
		}
		select {
		case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"complete","id":"%s","md5":"%s","bytes":%d}`+"\n", id, actualMD5, session.received)):
		default:
		}

	case "download":
		if name == "" {
			select {
			case client.Send <- []byte(`{"type":"file","action":"error","message":"missing file name"}` + "\n"):
			default:
			}
			return
		}
		path := filepath.Join(s.fileTransferDir(), sanitizeFileName(name))
		data, err := os.ReadFile(path)
		if err != nil {
			select {
			case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, err.Error())):
			default:
			}
			return
		}
		sum := md5.Sum(data)
		md5Hex := hex.EncodeToString(sum[:])
		start := map[string]any{
			"type":      "file",
			"action":    "start",
			"id":        id,
			"name":      name,
			"size":      len(data),
			"md5":       md5Hex,
			"version":   1,
			"direction": "download",
		}
		if raw, err := json.Marshal(start); err == nil {
			select {
			case client.Send <- append(raw, '\n'):
			default:
			}
		}
		chunkSize := 64 * 1024
		for offset := 0; offset < len(data); offset += chunkSize {
			end := offset + chunkSize
			if end > len(data) {
				end = len(data)
			}
			select {
			case client.BinarySend <- append([]byte(nil), data[offset:end]...):
			default:
				utils.LogWarn("Binary send channel full for client %s, dropping chunk", client.ID)
			}
		}
		select {
		case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"complete","id":"%s","md5":"%s","bytes":%d}`+"\n", id, md5Hex, len(data))):
		default:
		}
	}
}

func (s *Server) fileTransferDir() string {
	if cfg := config.Get(); cfg != nil {
		if cfg.LogFile != "" {
			base := filepath.Dir(cfg.LogFile)
			if base != "." {
				return filepath.Join(base, "airmouse-files")
			}
		}
	}
	return filepath.Join(os.TempDir(), "airmouse-files")
}

func (s *Server) handleBinaryFileChunk(client *WSClient, data []byte) bool {
	s.mu.Lock()
	session := s.fileSessions[client.ID]
	s.mu.Unlock()
	if session == nil || session.direction != "upload" || session.file == nil {
		return false
	}
	if _, err := session.file.Write(data); err != nil {
		select {
		case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", session.id, err.Error())):
		default:
		}
		return true
	}
	session.received += int64(len(data))
	if session.hasher != nil {
		_, _ = session.hasher.Write(data)
	}
	return true
}

func sanitizeFileName(name string) string {
	name = filepath.Base(name)
	name = strings.TrimSpace(name)
	if name == "." || name == string(filepath.Separator) || name == "" {
		return "file.bin"
	}
	return name
}

func toString(v any) string {
	switch t := v.(type) {
	case string:
		return t
	case fmt.Stringer:
		return t.String()
	default:
		return fmt.Sprintf("%v", v)
	}
}

func toInt64(v any) int64 {
	switch t := v.(type) {
	case int64:
		return t
	case int:
		return int64(t)
	case float64:
		return int64(t)
	case json.Number:
		i, _ := t.Int64()
		return i
	default:
		return 0
	}
}

func firstFloat64(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return toFloat64(v)
		}
	}
	return 0
}

func firstInt(payload map[string]any, keys ...string) int {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return toInt(v)
		}
	}
	return 0
}

// GetStats returns server statistics.
func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()

	var totalSent, totalRecv int64
	for _, c := range s.clients {
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
	}

	return map[string]interface{}{
		"clients":        len(s.clients),
		"total_clients":  s.totalClients,
		"bytes_sent":     totalSent,
		"bytes_recv":     totalRecv,
		"running":        s.running,
		"port":           s.port,
		"uptime_seconds": time.Since(s.startTime).Seconds(),
	}
}

// GetClientCount returns the number of connected clients.
func (s *Server) GetClientCount() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.clients)
}

// IsRunning returns true if the server is running.
func (s *Server) IsRunning() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.running
}