package tcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"sync"
	"time"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

// Client represents a TCP connected client.
type Client struct {
	ID          string
	DeviceID    string
	Name        string
	Conn        net.Conn
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	IP          string
	Approved    bool
	mu          sync.RWMutex // protects all fields except Conn (which is closed under server lock)
}

// Server is the TCP server.
type Server struct {
	host      string
	port      int
	listener  net.Listener
	clients   map[string]*Client
	mouse     control.MouseController
	deviceMgr *device.Manager
	authMgr   *auth.Manager
	mu        sync.RWMutex // protects clients map and running flag
	running   bool
	callbacks []func(event TCPEvent)
}

// TCPEvent represents a server event.
type TCPEvent struct {
	Type      string
	ClientID  string
	ClientIP  string
	Timestamp time.Time
}

// NewServer creates a new TCP server.
func NewServer(host string, port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		host:      host,
		port:      port,
		clients:   make(map[string]*Client),
		mouse:     mouse,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
		callbacks: make([]func(TCPEvent), 0),
	}
}

// Start binds and starts the TCP server.
func (s *Server) Start() error {
	addr := fmt.Sprintf("%s:%d", s.host, s.port)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return fmt.Errorf("failed to listen on %s: %w", addr, err)
	}

	s.mu.Lock()
	s.listener = listener
	s.running = true
	s.mu.Unlock()

	go s.acceptLoop()

	utils.LogInfo("TCP server started: address=%s", addr)
	utils.LogDebug("TCP listen socket ready on %s", addr)
	return nil
}

// acceptLoop accepts incoming connections.
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

// handleClient handles a single client connection.
func (s *Server) handleClient(conn net.Conn) {
	clientID := utils.GenerateID()
	clientIP := conn.RemoteAddr().String()

	client := &Client{
		ID:          clientID,
		Name:        "Unknown",
		Conn:        conn,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
		IP:          clientIP,
		Approved:    false,
	}

	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()

	utils.LogInfo("TCP client connected: id=%s ip=%s", clientID, clientIP)
	utils.LogInfo("TCP approval pending: id=%s", clientID)
	utils.LogDebug("TCP initial client record created: id=%s name=%s", clientID, client.Name)

	// Register device as pending
	if s.deviceMgr != nil {
		_ = s.deviceMgr.RegisterDevice(clientID, device.TypeTCP, client.Name)
		_ = s.deviceMgr.UpdateDeviceStatus(clientID, device.StatusPendingApproval)
	}
	s.triggerEvent(TCPEvent{
		Type:      "connected",
		ClientID:  clientID,
		ClientIP:  clientIP,
		Timestamp: time.Now(),
	})

	reader := bufio.NewReader(conn)
	heartbeat := time.NewTicker(30 * time.Second)
	defer heartbeat.Stop()

	// Heartbeat goroutine
	go func() {
		for range heartbeat.C {
			s.mu.RLock()
			c, exists := s.clients[clientID]
			s.mu.RUnlock()
			if !exists {
				return
			}
			// Check idle timeout
			c.mu.RLock()
			idle := time.Since(c.LastActive)
			c.mu.RUnlock()
			if idle > 120*time.Second {
				utils.LogInfo("TCP client timeout: id=%s", clientID)
				_ = conn.Close()
				return
			}
			// Send ping with write deadline
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

	// Read loop
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			utils.LogDebug("TCP read loop ending for client=%s err=%v", clientID, err)
			break
		}
		utils.LogDebug("TCP raw line from %s: %q", clientID, line)

		// Update last active and bytes received (with mutex)
		client.mu.Lock()
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(line))
		client.mu.Unlock()

		s.processLine(client, []byte(line))
	}

	// Disconnect
	_ = conn.Close()

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
	s.triggerEvent(TCPEvent{
		Type:      "disconnected",
		ClientID:  clientID,
		ClientIP:  clientIP,
		Timestamp: time.Now(),
	})
}

// processLine processes a single line of input.
func (s *Server) processLine(client *Client, line []byte) {
	msgType, payload, id, err := decodeWireMessage(line)
	if err != nil {
		utils.LogDebug("Invalid TCP message: %v", err)
		return
	}
	utils.LogDebug("TCP message parsed: client=%s type=%s payload_keys=%d", client.ID, msgType, len(payload))

	// Check approval status (with mutex)
	client.mu.RLock()
	approved := client.Approved
	client.mu.RUnlock()

	switch msgType {
	case "move":
		if !approved {
			utils.LogDebug("Ignoring TCP move while waiting for approval: client=%s", client.ID)
			return
		}
		dx := firstNumber(payload, "dx", "DeltaX", "deltaX")
		dy := firstNumber(payload, "dy", "DeltaY", "deltaY")
		utils.LogInfo("TCP move: client=%s dx=%.2f dy=%.2f", client.ID, dx, dy)
		if s.mouse != nil {
			s.mouse.Move(dx, dy)
		}
		utils.LogDebug("TCP move forwarded: client=%s dx=%.2f dy=%.2f", client.ID, dx, dy)

	case "click":
		if !approved {
			utils.LogDebug("Ignoring TCP click while waiting for approval: client=%s", client.ID)
			return
		}
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
		utils.LogDebug("TCP click forwarded: client=%s button=%s", client.ID, button)
		s.writeAck(client, id)

	case "doubleclick":
		if !approved {
			utils.LogDebug("Ignoring TCP doubleclick while waiting for approval: client=%s", client.ID)
			return
		}
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
		s.writeAck(client, id)

	case "rightclick":
		if !approved {
			utils.LogDebug("Ignoring TCP rightclick while waiting for approval: client=%s", client.ID)
			return
		}
		if s.mouse != nil {
			s.mouse.Click("right")
		}
		s.writeAck(client, id)

	case "scroll":
		if !approved {
			utils.LogDebug("Ignoring TCP scroll while waiting for approval: client=%s", client.ID)
			return
		}
		delta := int(firstNumber(payload, "delta", "Scroll", "scroll"))
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
		utils.LogDebug("TCP scroll forwarded: client=%s delta=%d", client.ID, delta)
		s.writeAck(client, id)

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
		token, _ := payload["token"].(string)

		utils.LogInfo("Handshake received from Android (TCP): id=%s name=%s", client.ID, name)
		utils.LogDebug("TCP hello payload: id=%s version=%s device=%s model=%s android=%s protocol=%s transport=%s", client.ID, version, deviceName, model, androidVersion, protocolName, transport)

		// Authentication check
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

		// Update client fields (with mutex)
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

		utils.LogInfo("TCP client awaiting approval: id=%s name=%s", client.ID, client.Name)
		// Note: Approved flag is still false; will be set by ApproveDevice

	case "ping":
		utils.LogDebug("TCP ping received from client=%s, sending pong", client.ID)
		_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
		_, _ = client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))

	case "pong":
		utils.LogDebug("TCP pong received from client=%s", client.ID)

	case "gesture":
		if !approved {
			utils.LogDebug("Ignoring TCP gesture while waiting for approval: client=%s", client.ID)
			return
		}
		gesture, _ := payload["gesture"].(string)
		confidence := number(payload["confidence"])
		utils.LogInfo("TCP gesture received: client=%s gesture=%s confidence=%.2f", client.ID, gesture, confidence)

	case "proximity":
		if !approved {
			utils.LogDebug("Ignoring TCP proximity while waiting for approval: client=%s", client.ID)
			return
		}
		isNear, _ := payload["is_near"].(bool)
		distance := number(payload["distance"])
		utils.LogInfo("TCP proximity update: client=%s near=%v distance=%.2f", client.ID, isNear, distance)

	case "control":
		if !approved {
			utils.LogDebug("Ignoring TCP control while waiting for approval: client=%s", client.ID)
			return
		}
		command, _ := payload["command"].(string)
		switch command {
		case "start", "touchpad_start", "resume", "resume_movement":
			control.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by TCP client: client=%s command=%s", client.ID, command)
		case "stop", "touchpad_stop", "pause", "pause_movement":
			control.SetMovementPaused(true)
			utils.LogInfo("Movement paused by TCP client: client=%s command=%s", client.ID, command)
		default:
			utils.LogDebug("Unknown TCP control command: %s", command)
		}

	default:
		utils.LogDebug("Unknown TCP message type: type=%s client=%s", msgType, client.ID)
	}
}

// writeAck sends an ACK message for the given ID.
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
	client.mu.Lock()
	client.BytesSent += int64(len(ack))
	client.mu.Unlock()
}

// ApproveDevice approves a pending TCP client and sends the welcome message.
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
		cfg.ServerName,
		cfg.Version,
		client.ID,
	)

	// Send welcome with deadline
	_ = client.Conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
	if _, err := client.Conn.Write([]byte(welcome)); err != nil {
		return fmt.Errorf("failed to send welcome: %w", err)
	}

	// Update state (under mutex)
	client.mu.Lock()
	client.Approved = true
	client.BytesSent += int64(len(welcome))
	client.mu.Unlock()

	control.SetMovementPaused(false)
	control.ClearPause()

	if s.deviceMgr != nil {
		deviceIDToUpdate := client.DeviceID
		if deviceIDToUpdate == "" {
			deviceIDToUpdate = client.ID
		}
		_ = s.deviceMgr.UpdateDeviceStatus(deviceIDToUpdate, device.StatusConnected)
	}
	utils.LogInfo("TCP approval accepted: device=%s", deviceID)
	return nil
}

// Stop gracefully stops the TCP server.
func (s *Server) Stop() {
	s.mu.Lock()
	s.running = false
	if s.listener != nil {
		_ = s.listener.Close()
	}
	// Close all client connections
	for _, c := range s.clients {
		_ = c.Conn.Close()
	}
	s.clients = make(map[string]*Client)
	s.mu.Unlock()
	utils.LogInfo("TCP server stopped")
}

// GetStats returns server statistics.
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

// AddEventListener registers a callback for server events.
func (s *Server) AddEventListener(callback func(event TCPEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.callbacks = append(s.callbacks, callback)
}

// triggerEvent fires a server event.
func (s *Server) triggerEvent(event TCPEvent) {
	s.mu.RLock()
	callbacks := make([]func(TCPEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()
	for _, cb := range callbacks {
		go cb(event)
	}
}

// SendToClient sends a message to a specific client.
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
	client.mu.Unlock()
	return nil
}

// Helper functions (unchanged)
func firstNumber(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return number(v)
		}
	}
	return 0
}
