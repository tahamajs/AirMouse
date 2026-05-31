package websocket

import (
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

// WSClient represents a connected WebSocket client.
type WSClient struct {
	ID          string
	Name        string
	Conn        *websocket.Conn
	Send        chan []byte
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
}

// Server manages WebSocket connections.
type Server struct {
	port      int
	clients   map[string]*WSClient
	mouse     control.MouseController
	deviceMgr *device.Manager
	authMgr   *auth.Manager
	mu        sync.RWMutex
	server    *http.Server
	running   bool
}

// WMessage is the JSON structure exchanged over WebSocket.
type WMessage struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
	ID      string          `json:"id,omitempty"`
	Device  string          `json:"device,omitempty"`
}

// Payload structures
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

// NewServer creates a new WebSocket server.
func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		port:      port,
		clients:   make(map[string]*WSClient),
		mouse:     mouse,
		deviceMgr: deviceMgr,
		authMgr:   authMgr,
	}
}

// Start launches the WebSocket server.
func (s *Server) Start() error {
	mux := http.NewServeMux()
	mux.HandleFunc("/ws", s.handleWebSocket)

	s.server = &http.Server{
		Addr:    fmt.Sprintf(":%d", s.port),
		Handler: mux,
	}
	s.running = true

	go func() {
		utils.LogInfo("WebSocket server started", "port", s.port)
		if err := s.server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			utils.LogError("WebSocket server error", "error", err)
		}
	}()
	return nil
}

func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	// (Optional) Extract pairing token if authentication is enabled.
	// If authMgr is not nil and config.AuthEnabled == true, validate token.
	// For now we skip token validation to keep compatibility.

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		utils.LogError("WebSocket upgrade failed", "error", err)
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
		client.BytesRecv += int64(len(message))

		var msg WMessage
		if err := json.Unmarshal(message, &msg); err != nil {
			utils.LogError("Invalid WebSocket message", "error", err)
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
			client.BytesSent += int64(len(message))
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
	case "proximity":
		var update ProximityUpdate
		if err := json.Unmarshal(msg.Payload, &update); err == nil {
			proximityMgr.ProcessUpdate(update)
		}
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
			utils.LogInfo("Device identified via WebSocket", "id", client.ID, "name", client.Name)

			welcome := WMessage{Type: "welcome", Payload: json.RawMessage(`{"server":"AirMouse","version":"3.0"}`)}
			if data, err := json.Marshal(welcome); err == nil {
				client.Send <- data
			}
		}
	case "ping":
		pong := WMessage{Type: "pong"}
		if data, err := json.Marshal(pong); err == nil {
			client.Send <- data
		}
	default:
		utils.LogWarn("Unknown WebSocket message type", "type", msg.Type, "id", client.ID)
	}
}

// Stop shuts down the WebSocket server.
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