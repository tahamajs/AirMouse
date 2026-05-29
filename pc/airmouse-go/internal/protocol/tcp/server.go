package tcp

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"sync"
	"time"
	
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

type Client struct {
	ID         string
	Name       string
	Conn       net.Conn
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
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
}

type Message struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
	ID      string          `json:"id,omitempty"`
	Device  string          `json:"device,omitempty"`
}

type MovePayload struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
}

type ClickPayload struct {
	Button string `json:"button"`
}

type ScrollPayload struct {
	Delta int `json:"delta"`
}

type HelloPayload struct {
	Name    string `json:"name"`
	Version string `json:"version"`
}

func NewServer(host string, port int, mouse control.MouseController, deviceMgr *device.Manager) *Server {
	return &Server{
		host:      host,
		port:      port,
		clients:   make(map[string]*Client),
		mouse:     mouse,
		deviceMgr: deviceMgr,
	}
}

func (s *Server) Start() error {
	addr := fmt.Sprintf("%s:%d", s.host, s.port)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}
	
	s.listener = listener
	s.running = true
	
	logger.Info("TCP server started", "address", addr)
	
	go s.acceptLoop()
	return nil
}

func (s *Server) acceptLoop() {
	for s.running {
		conn, err := s.listener.Accept()
		if err != nil {
			if s.running {
				logger.Error("Accept error", "error", err)
			}
			continue
		}
		
		go s.handleClient(conn)
	}
}

func (s *Server) handleClient(conn net.Conn) {
	clientID := conn.RemoteAddr().String()
	client := &Client{
		ID:         clientID,
		Name:       "Unknown",
		Conn:       conn,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}
	
	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()
	
	logger.Info("Client connected", "id", clientID)
	s.deviceMgr.RegisterDevice(clientID, "tcp", client.Name)
	
	reader := bufio.NewReader(conn)
	heartbeat := time.NewTicker(10 * time.Second)
	
	go func() {
		for range heartbeat.C {
			s.mu.RLock()
			c, exists := s.clients[clientID]
			s.mu.RUnlock()
			
			if !exists {
				return
			}
			
			if time.Since(c.LastActive) > 30*time.Second {
				logger.Info("Client timeout", "id", clientID)
				conn.Close()
				return
			}
			
			// Send heartbeat ping
			ping := `{"type":"ping"}`
			conn.Write([]byte(ping + "\n"))
		}
	}()
	
	for {
		line, err := reader.ReadString('\n')
		if err != nil {
			break
		}
		
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(line))
		
		var msg Message
		if err := json.Unmarshal([]byte(line), &msg); err != nil {
			logger.Error("Invalid message", "error", err)
			continue
		}
		
		s.processMessage(client, &msg)
	}
	
	heartbeat.Stop()
	conn.Close()
	
	s.mu.Lock()
	delete(s.clients, clientID)
	s.mu.Unlock()
	
	logger.Info("Client disconnected", "id", clientID)
	s.deviceMgr.UnregisterDevice(clientID)
}

func (s *Server) processMessage(client *Client, msg *Message) {
	switch msg.Type {
	case "move":
		var payload MovePayload
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			s.mouse.Move(payload.DX, payload.DY)
		}
		
	case "click":
		var payload ClickPayload
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			s.mouse.Click(payload.Button)
		}
		
	case "doubleclick":
		s.mouse.DoubleClick()
		
	case "rightclick":
		s.mouse.Click("right")
		
	case "scroll":
		var payload ScrollPayload
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			s.mouse.Scroll(payload.Delta)
		}
		
	case "hello":
		var payload HelloPayload
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			client.Name = payload.Name
			s.deviceMgr.UpdateDeviceName(client.ID, client.Name)
			logger.Info("Device identified", "id", client.ID, "name", client.Name)
			
			// Send welcome message
			welcome := fmt.Sprintf(`{"type":"welcome","payload":{"server":"AirMouse","version":"2.0"}}`)
			client.Conn.Write([]byte(welcome + "\n"))
		}
		
	case "ping":
		pong := `{"type":"pong"}`
		client.Conn.Write([]byte(pong + "\n"))
		
	case "gesture":
		logger.Debug("Gesture received", "type", string(msg.Payload), "device", client.Name)
	}
}

func (s *Server) Stop() {
	s.running = false
	if s.listener != nil {
		s.listener.Close()
	}
	
	s.mu.Lock()
	for _, client := range s.clients {
		client.Conn.Close()
	}
	s.clients = make(map[string]*Client)
	s.mu.Unlock()
	
	logger.Info("TCP server stopped")
}

func (s *Server) GetClients() []*Client {
	s.mu.RLock()
	defer s.mu.RUnlock()
	
	clients := make([]*Client, 0, len(s.clients))
	for _, client := range s.clients {
		clients = append(clients, client)
	}
	return clients
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	
	stats := make(map[string]interface{})
	stats["clients"] = len(s.clients)
	
	var totalBytesSent, totalBytesRecv int64
	for _, client := range s.clients {
		totalBytesSent += client.BytesSent
		totalBytesRecv += client.BytesRecv
	}
	stats["bytes_sent"] = totalBytesSent
	stats["bytes_recv"] = totalBytesRecv
	
	return stats
}