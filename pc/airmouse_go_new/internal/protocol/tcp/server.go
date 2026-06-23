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
}

type Server struct {
	host      string
	port      int
	listener  net.Listener
	clients   map[string]*Client
	mouse     control.MouseController
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

func (s *Server) Start() error {
	addr := fmt.Sprintf("%s:%d", s.host, s.port)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return fmt.Errorf("failed to listen on %s: %w", addr, err)
	}

	s.listener = listener
	s.running = true
	go s.acceptLoop()

	utils.LogInfo("TCP server started: address=%s", addr)
	utils.LogDebug("TCP listen socket ready on %s", addr)
	return nil
}

func (s *Server) acceptLoop() {
	for s.running {
		conn, err := s.listener.Accept()
		if err != nil {
			if s.running {
				utils.LogError("TCP accept error: %v", err)
			}
			continue
		}
		go s.handleClient(conn)
	}
}

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
	}

	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()

	utils.LogInfo("TCP client connected: id=%s ip=%s", clientID, clientIP)
	utils.LogInfo("TCP approval pending: id=%s", clientID)
	utils.LogDebug("TCP initial client record created: id=%s name=%s", clientID, client.Name)
	s.deviceMgr.RegisterDevice(clientID, device.TypeTCP, client.Name)
	s.triggerEvent(TCPEvent{
		Type:      "connected",
		ClientID:  clientID,
		ClientIP:  clientIP,
		Timestamp: time.Now(),
	})

	reader := bufio.NewReader(conn)
	heartbeat := time.NewTicker(10 * time.Second)
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
			if time.Since(c.LastActive) > 30*time.Second {
				utils.LogInfo("TCP client timeout: id=%s", clientID)
				conn.Close()
				return
			}
			conn.Write([]byte(`{"type":"ping"}` + "\n"))
		}
	}()

	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			utils.LogDebug("TCP read loop ending for client=%s err=%v", clientID, err)
			break
		}

		client.LastActive = time.Now()
		client.BytesRecv += int64(len(line))
		s.processLine(client, []byte(line))
	}

	conn.Close()
	s.mu.Lock()
	delete(s.clients, clientID)
	s.mu.Unlock()

	utils.LogInfo("TCP client disconnected: id=%s", clientID)
	deviceID := client.DeviceID
	if deviceID == "" {
		deviceID = clientID
	}
	_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusDisconnected)
	s.triggerEvent(TCPEvent{
		Type:      "disconnected",
		ClientID:  clientID,
		ClientIP:  clientIP,
		Timestamp: time.Now(),
	})
}

func (s *Server) processLine(client *Client, line []byte) {
	msgType, payload, id, err := decodeWireMessage(line)
	if err != nil {
		utils.LogDebug("Invalid TCP message: %v", err)
		return
	}
	cfg := config.Get()
	utils.LogDebug("TCP message parsed: client=%s type=%s payload_keys=%d", client.ID, msgType, len(payload))

	switch msgType {
	case "move":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP move while waiting for approval: client=%s", client.ID)
			return
		}
		dx := firstNumber(payload, "dx", "DeltaX", "deltaX")
		dy := firstNumber(payload, "dy", "DeltaY", "deltaY")
		s.mouse.Move(dx, dy)
		utils.LogDebug("TCP move forwarded: client=%s dx=%.2f dy=%.2f", client.ID, dx, dy)

	case "click":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP click while waiting for approval: client=%s", client.ID)
			return
		}
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		s.mouse.Click(button)
		utils.LogDebug("TCP click forwarded: client=%s button=%s", client.ID, button)
		s.writeAck(client, id)

	case "doubleclick":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP doubleclick while waiting for approval: client=%s", client.ID)
			return
		}
		s.mouse.DoubleClick()
		s.writeAck(client, id)

	case "rightclick":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP rightclick while waiting for approval: client=%s", client.ID)
			return
		}
		s.mouse.Click("right")
		s.writeAck(client, id)

	case "scroll":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP scroll while waiting for approval: client=%s", client.ID)
			return
		}
		delta := int(firstNumber(payload, "delta", "Scroll", "scroll"))
		s.mouse.Scroll(delta)
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
		if config.Get().AuthEnabled {
			if token == "" || s.authMgr == nil || !s.authMgr.ValidateToken(token) {
				errMsg := `{"type":"error","payload":{"message":"connection rejected: invalid pairing token"}}` + "\n"
				_, _ = client.Conn.Write([]byte(errMsg))
				_ = client.Conn.Close()
				utils.LogInfo("TCP client rejected: id=%s reason=invalid token", client.ID)
				return
			}
		}
		if name != "" {
			client.Name = name
		}
		if s.deviceMgr != nil {
			_ = s.deviceMgr.RenameDeviceID(client.ID, fingerprint)
			client.DeviceID = fingerprint
			client.Name = name
			s.deviceMgr.UpsertDevice(fingerprint, device.TypeTCP, client.Name, map[string]string{
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
		}
		client.Approved = true

		welcome := fmt.Sprintf(
			`{"type":"welcome","payload":{"server":"%s","version":"%s","client_id":"%s"}}`+"\n",
			cfg.ServerName,
			cfg.Version,
			client.ID,
		)
		client.Conn.Write([]byte(welcome))
		client.BytesSent += int64(len(welcome))
		utils.LogInfo("TCP approval accepted: id=%s name=%s", client.ID, client.Name)
		utils.LogInfo("TCP client connected: id=%s name=%s", client.ID, client.Name)

	case "ping":
		client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))

	case "gesture":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP gesture while waiting for approval: client=%s", client.ID)
			return
		}
		gesture, _ := payload["gesture"].(string)
		confidence := number(payload["confidence"])
		utils.LogInfo("TCP gesture received: client=%s gesture=%s confidence=%.2f", client.ID, gesture, confidence)

	case "proximity":
		if !client.Approved {
			utils.LogDebug("Ignoring TCP proximity while waiting for approval: client=%s", client.ID)
			return
		}
		isNear, _ := payload["is_near"].(bool)
		distance := number(payload["distance"])
		utils.LogInfo("TCP proximity update: client=%s near=%v distance=%.2f", client.ID, isNear, distance)

	case "control":
		if !client.Approved {
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
		}

	default:
		utils.LogDebug("Unknown TCP message type: type=%s client=%s", msgType, client.ID)
	}
}

func (s *Server) writeAck(client *Client, id *string) {
	if ack := ackMessage(id); len(ack) > 0 {
		client.Conn.Write(ack)
		client.BytesSent += int64(len(ack))
	}
}

func (s *Server) Stop() {
	s.running = false
	if s.listener != nil {
		s.listener.Close()
	}

	s.mu.Lock()
	for _, c := range s.clients {
		c.Conn.Close()
	}
	s.clients = make(map[string]*Client)
	s.mu.Unlock()

	utils.LogInfo("TCP server stopped")
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()

	var totalSent, totalRecv int64
	var activeCount int
	now := time.Now()

	for _, c := range s.clients {
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
		if now.Sub(c.LastActive) < 10*time.Second {
			activeCount++
		}
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

	_, err = client.Conn.Write(append(data, '\n'))
	if err == nil {
		client.BytesSent += int64(len(data) + 1)
	}
	return err
}

func firstNumber(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return number(v)
		}
	}
	return 0
}
