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
	"airmouse-go/internal/utils"
)

// Client represents a TCP client connection.
type Client struct {
	ID          string
	Name        string
	Conn        net.Conn
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
}

// Server is the TCP server.
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

// Message is the JSON structure sent by clients.
type Message struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
	ID      string          `json:"id,omitempty"`
	Device  string          `json:"device,omitempty"`
}

// Payload structures.
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

// NewServer creates a new TCP server.
func NewServer(host string, port int, mouse control.MouseController, deviceMgr *device.Manager) *Server {
	return &Server{
		host:      host,
		port:      port,
		clients:   make(map[string]*Client),
		mouse:     mouse,
		deviceMgr: deviceMgr,
	}
}

// Start launches the TCP server.
func (s *Server) Start() error {
	addr := fmt.Sprintf("%s:%d", s.host, s.port)
	listener, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}
	s.listener = listener
	s.running = true

	utils.LogInfo("TCP server started", "address", addr)
	go s.acceptLoop()
	return nil
}

func (s *Server) acceptLoop() {
	for s.running {
		conn, err := s.listener.Accept()
		if err != nil {
			if s.running {
				utils.LogError("TCP accept error", "error", err)
			}
			continue
		}
		go s.handleClient(conn)
	}
}

func (s *Server) handleClient(conn net.Conn) {
	clientID := conn.RemoteAddr().String()
	client := &Client{
		ID:          clientID,
		Name:        "Unknown",
		Conn:        conn,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}

	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()

	utils.LogInfo("TCP client connected", "id", clientID)
	s.deviceMgr.RegisterDevice(clientID, device.TypeTCP, client.Name)

	reader := bufio.NewReader(conn)
	heartbeat := time.NewTicker(10 * time.Second)
	defer heartbeat.Stop()

	go func() {
		for range heartbeat.C {
			s.mu.RLock()
			c, exists := s.clients[clientID]
			s.mu.RUnlock()
			if !exists {
				return
			}
			if time.Since(c.LastActive) > 30*time.Second {
				utils.LogInfo("TCP client timeout", "id", clientID)
				conn.Close()
				return
			}
			// Send ping
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

		var msg Message
		if err := json.Unmarshal([]byte(line), &msg); err != nil {
			utils.LogError("Invalid TCP message", "error", err)
			continue
		}
		s.processMessage(client, &msg)
	}

	conn.Close()
	s.mu.Lock()
	delete(s.clients, clientID)
	s.mu.Unlock()
	utils.LogInfo("TCP client disconnected", "id", clientID)
	s.deviceMgr.UnregisterDevice(clientID)
}

func (s *Server) processMessage(client *Client, msg *Message) {
	switch msg.Type {
	case "move":
		var p MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			s.mouse.Move(p.DX, p.DY)
		}
	case "click":
		var p ClickPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			s.mouse.Click(p.Button)
		}
	case "doubleclick":
		s.mouse.DoubleClick()
	case "rightclick":
		s.mouse.Click("right")
	case "scroll":
		var p ScrollPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			s.mouse.Scroll(p.Delta)
		}
	case "hello":
		var p HelloPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			client.Name = p.Name
			s.deviceMgr.UpdateDeviceName(client.ID, client.Name)
			utils.LogInfo("Device identified via TCP", "id", client.ID, "name", client.Name)
			welcome := `{"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}`
			client.Conn.Write([]byte(welcome + "\n"))
		}
	case "ping":
		client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))
	default:
		utils.LogWarn("Unknown TCP message type", "type", msg.Type, "id", client.ID)
	}
}

// Stop shuts down the TCP server.
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

// GetStats returns server statistics.
func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	stats := map[string]interface{}{
		"clients": len(s.clients),
	}
	var totalSent, totalRecv int64
	for _, c := range s.clients {
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
	}
	stats["bytes_sent"] = totalSent
	stats["bytes_recv"] = totalRecv
	return stats
}