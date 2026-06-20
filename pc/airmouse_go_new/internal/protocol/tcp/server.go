package tcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"sync"
	"time"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

type Client struct {
	ID          string
	Name        string
	Conn        net.Conn
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	IP          string
}

type Server struct {
	host      string
	port      int
	listener  net.Listener
	clients   map[string]*Client
	mouse     control.MouseController
	deviceMgr *device.Manager
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

func NewServer(host string, port int, mouse control.MouseController, deviceMgr *device.Manager) *Server {
	return &Server{
		host:      host,
		port:      port,
		clients:   make(map[string]*Client),
		mouse:     mouse,
		deviceMgr: deviceMgr,
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
	s.deviceMgr.UnregisterDevice(clientID)
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

	switch msgType {
	case "move":
		dx := number(payload["dx"])
		dy := number(payload["dy"])
		s.mouse.Move(dx, dy)

	case "click":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		s.mouse.Click(button)
		s.writeAck(client, id)

	case "doubleclick":
		s.mouse.DoubleClick()
		s.writeAck(client, id)

	case "rightclick":
		s.mouse.Click("right")
		s.writeAck(client, id)

	case "scroll":
		delta := int(number(payload["delta"]))
		s.mouse.Scroll(delta)
		s.writeAck(client, id)

	case "hello":
		name, _ := payload["name"].(string)
		if name != "" {
			client.Name = name
			s.deviceMgr.UpdateDeviceName(client.ID, client.Name)
		}

		welcome := fmt.Sprintf(
			`{"type":"welcome","payload":{"server":"%s","version":"%s","client_id":"%s"}}`+"\n",
			cfg.ServerName,
			cfg.Version,
			client.ID,
		)
		client.Conn.Write([]byte(welcome))
		client.BytesSent += int64(len(welcome))

		utils.LogInfo("TCP client identified: id=%s name=%s", client.ID, client.Name)

	case "ping":
		client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))

	case "gesture":
		gesture, _ := payload["gesture"].(string)
		confidence := number(payload["confidence"])
		utils.LogInfo("TCP gesture received: client=%s gesture=%s confidence=%.2f", client.ID, gesture, confidence)

	case "proximity":
		isNear, _ := payload["is_near"].(bool)
		distance := number(payload["distance"])
		utils.LogInfo("TCP proximity update: client=%s near=%v distance=%.2f", client.ID, isNear, distance)

	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "pause_movement":
			control.SetMovementPaused(true)
			utils.LogInfo("Movement paused by TCP client: client=%s", client.ID)
		case "resume_movement":
			control.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by TCP client: client=%s", client.ID)
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
