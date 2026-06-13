package tcp

import (
	"bufio"
	"fmt"
	"net"
	"sync"
	"time"

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
	go s.acceptLoop()
	utils.LogInfo("TCP server started", "address", addr)
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
	utils.LogInfo("TCP client disconnected", "id", clientID)
	s.deviceMgr.UnregisterDevice(clientID)
}

func (s *Server) processLine(client *Client, line []byte) {
	msgType, payload, id, err := decodeWireMessage(line)
	if err != nil {
		return
	}
	switch msgType {
	case "move":
		s.mouse.Move(number(payload["dx"]), number(payload["dy"]))
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
		s.mouse.Scroll(int(number(payload["delta"])))
		s.writeAck(client, id)
	case "hello":
		name, _ := payload["name"].(string)
		client.Name = name
		s.deviceMgr.UpdateDeviceName(client.ID, client.Name)
		client.Conn.Write([]byte(`{"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}` + "\n"))
	case "ping":
		client.Conn.Write([]byte(`{"type":"pong"}` + "\n"))
	case "proximity":
		utils.LogInfo("TCP proximity update received", "id", client.ID)
	case "gesture":
		utils.LogInfo("TCP gesture received", "id", client.ID)
	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "pause_movement":
			control.SetMovementPaused(true)
		case "resume_movement":
			control.SetMovementPaused(false)
		}
	default:
		utils.LogDebug("TCP message ignored", "type", msgType)
	}
}

func (s *Server) writeAck(client *Client, id *string) {
	if ack := ackMessage(id); len(ack) > 0 {
		_, _ = client.Conn.Write(ack)
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
	stats := map[string]interface{}{"clients": len(s.clients)}
	var totalSent, totalRecv int64
	for _, c := range s.clients {
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
	}
	stats["bytes_sent"] = totalSent
	stats["bytes_recv"] = totalRecv
	return stats
}
