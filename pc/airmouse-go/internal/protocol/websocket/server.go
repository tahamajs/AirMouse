package websocket

import (
	"encoding/json"
	"net/http"
	"sync"
	"time"
	
	"github.com/gorilla/websocket"
	
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type WSClient struct {
	ID         string
	Name       string
	Conn       *websocket.Conn
	Send       chan []byte
	ConnectedAt time.Time
	LastActive  time.Time
}

type Server struct {
	port      int
	clients   map[string]*WSClient
	mouse     control.MouseController
	deviceMgr *device.Manager
	mu        sync.RWMutex
	server    *http.Server
	running   bool
}

type WMessage struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
	ID      string          `json:"id,omitempty"`
	Device  string          `json:"device,omitempty"`
}

func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager) *Server {
	return &Server{
		port:      port,
		clients:   make(map[string]*WSClient),
		mouse:     mouse,
		deviceMgr: deviceMgr,
	}
}

func (s *Server) Start() error {
	mux := http.NewServeMux()
	mux.HandleFunc("/ws", s.handleWebSocket)
	
	s.server = &http.Server{
		Addr:    fmt.Sprintf(":%d", s.port),
		Handler: mux,
	}
	
	s.running = true
	
	go func() {
		logger.Info("WebSocket server started", "port", s.port)
		if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("WebSocket server error", "error", err)
		}
	}()
	
	return nil
}

func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		logger.Error("WebSocket upgrade failed", "error", err)
		return
	}
	
	clientID := conn.RemoteAddr().String()
	client := &WSClient{
		ID:         clientID,
		Name:       "Unknown",
		Conn:       conn,
		Send:       make(chan []byte, 256),
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}
	
	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()
	
	logger.Info("WebSocket client connected", "id", clientID)
	s.deviceMgr.RegisterDevice(clientID, "websocket", client.Name)
	
	go s.writePump(client)
	go s.readPump(client)
}

func (s *Server) readPump(client *WSClient) {
	defer func() {
		s.mu.Lock()
		delete(s.clients, client.ID)
		s.mu.Unlock()
		client.Conn.Close()
		s.deviceMgr.UnregisterDevice(client.ID)
	}()
	
	client.Conn.SetReadLimit(512)
	client.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	client.Conn.SetPongHandler(func(string) error {
		client.Conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})
	
	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			break
		}
		
		client.LastActive = time.Now()
		
		var msg WMessage
		if err := json.Unmarshal(message, &msg); err != nil {
			logger.Error("Invalid WebSocket message", "error", err)
			continue
		}
		
		s.processMessage(client, &msg)
	}
}

func (s *Server) writePump(client *WSClient) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		ticker.Stop()
		client.Conn.Close()
	}()
	
	for {
		select {
		case message, ok := <-client.Send:
			client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			
			if err := client.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
			
		case <-ticker.C:
			client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (s *Server) processMessage(client *WSClient, msg *WMessage) {
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
			logger.Info("Device identified via WebSocket", "id", client.ID, "name", client.Name)
			
			welcome := WMessage{Type: "welcome", Payload: json.RawMessage(`{"server":"AirMouse","version":"2.0"}`)}
			if data, err := json.Marshal(welcome); err == nil {
				client.Send <- data
			}
		}
		
	case "ping":
		pong := WMessage{Type: "pong"}
		if data, err := json.Marshal(pong); err == nil {
			client.Send <- data
		}
	}
}

func (s *Server) Stop() {
	s.running = false
	if s.server != nil {
		s.server.Close()
	}
	
	s.mu.Lock()
	for _, client := range s.clients {
		client.Conn.Close()
	}
	s.clients = make(map[string]*WSClient)
	s.mu.Unlock()
	
	logger.Info("WebSocket server stopped")
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	
	return map[string]interface{}{
		"clients": len(s.clients),
	}
}