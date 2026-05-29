package websocket

import (
	"encoding/json"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

type WSClient struct {
	ID          string
	Name        string
	Conn        *websocket.Conn
	Send        chan []byte
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
	s.server = &http.Server{Addr: fmt.Sprintf(":%d", s.port), Handler: mux}
	s.running = true
	go func() {
		if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			utils.LogError("WebSocket server error", "error", err)
		}
	}()
	return nil
}

func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	clientID := conn.RemoteAddr().String()
	client := &WSClient{
		ID:          clientID,
		Name:        "Unknown",
		Conn:        conn,
		Send:        make(chan []byte, 256),
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}
	s.mu.Lock()
	s.clients[clientID] = client
	s.mu.Unlock()

	utils.LogInfo("WebSocket client connected", "id", clientID)
	s.deviceMgr.RegisterDevice(clientID, device.TypeWebSocket, client.Name)

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

		var msg map[string]interface{}
		if err := json.Unmarshal(message, &msg); err != nil {
			continue
		}
		s.processMessage(client, msg)
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
			if !ok {
				client.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
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

func (s *Server) processMessage(client *WSClient, msg map[string]interface{}) {
	t, _ := msg["type"].(string)
	switch t {
	case "move":
		if payload, ok := msg["payload"].(map[string]interface{}); ok {
			dx, _ := payload["dx"].(float64)
			dy, _ := payload["dy"].(float64)
			s.mouse.Move(dx, dy)
		}
	case "click":
		if payload, ok := msg["payload"].(map[string]interface{}); ok {
			btn, _ := payload["button"].(string)
			s.mouse.Click(btn)
		}
	case "doubleclick":
		s.mouse.DoubleClick()
	case "rightclick":
		s.mouse.Click("right")
	case "scroll":
		if payload, ok := msg["payload"].(map[string]interface{}); ok {
			delta, _ := payload["delta"].(float64)
			s.mouse.Scroll(int(delta))
		}
	case "hello":
		if payload, ok := msg["payload"].(map[string]interface{}); ok {
			name, _ := payload["name"].(string)
			client.Name = name
			s.deviceMgr.UpdateDeviceName(client.ID, name)
			utils.LogInfo("WebSocket device identified", "id", client.ID, "name", name)
			client.Send <- []byte(`{"type":"welcome","payload":{"server":"AirMouse"}}`)
		}
	case "ping":
		client.Send <- []byte(`{"type":"pong"}`)
	}
}

func (s *Server) Stop() {
	s.running = false
	if s.server != nil {
		s.server.Close()
	}
	s.mu.Lock()
	for _, c := range s.clients {
		c.Conn.Close()
	}
	s.clients = make(map[string]*WSClient)
	s.mu.Unlock()
	utils.LogInfo("WebSocket server stopped")
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return map[string]interface{}{"clients": len(s.clients)}
}


case "control":
    var payload struct {
        Command string `json:"command"`
    }
    if err := json.Unmarshal(msg.Payload, &payload); err == nil {
        switch payload.Command {
        case "pause_movement":
            control.SetMovementPaused(true)
            logger.LogInfo("Movement paused by client (orientation monitor)")
        case "resume_movement":
            control.SetMovementPaused(false)
            logger.LogInfo("Movement resumed by client")
        }
    }