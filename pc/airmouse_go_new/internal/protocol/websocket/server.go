package websocket

import (
	"fmt"
	"net/http"
	"sync"
	"time"

	gorilla "github.com/gorilla/websocket"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/jitter"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/utils"
)

var upgrader = gorilla.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type WSClient struct {
	ID         string
	Name       string
	Conn       *gorilla.Conn
	Send       chan []byte
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
}

type Server struct {
	port         int
	clients      map[string]*WSClient
	mouse        control.MouseController
	deviceMgr    *device.Manager
	authMgr      *auth.Manager
	proximityMgr *proximity.Manager
	mu           sync.RWMutex
	server       *http.Server
	running      bool
	jitterBuffer *jitter.JitterBuffer
}

func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager) *Server {
	return &Server{
		port:         port,
		clients:      make(map[string]*WSClient),
		mouse:        mouse,
		deviceMgr:    deviceMgr,
		authMgr:      authMgr,
		jitterBuffer: jitter.NewJitterBuffer(jitter.DefaultJitterBufferConfig()),
	}
}

func (s *Server) SetProximityManager(pm *proximity.Manager) {
	s.proximityMgr = pm
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
	id := utils.GenerateID()
	client := &WSClient{
		ID:         id,
		Conn:       conn,
		Send:       make(chan []byte, 256),
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}

	s.mu.Lock()
	s.clients[id] = client
	s.mu.Unlock()
	s.deviceMgr.RegisterDevice(id, device.TypeWebSocket, "Android")
	utils.LogInfo("WebSocket client connected", "id", id)

	go s.readLoop(client)
	go s.writeLoop(client)
}

func (s *Server) readLoop(client *WSClient) {
	defer func() {
		client.Conn.Close()
		s.mu.Lock()
		delete(s.clients, client.ID)
		s.mu.Unlock()
		s.deviceMgr.UnregisterDevice(client.ID)
		utils.LogInfo("WebSocket client disconnected", "id", client.ID)
	}()

	for {
		_, data, err := client.Conn.ReadMessage()
		if err != nil {
			return
		}
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(data))
		msgType, payload, id, err := decodeWireMessage(data)
		if err != nil {
			continue
		}
		s.processMessage(client, msgType, payload, id)
	}
}

func (s *Server) writeLoop(client *WSClient) {
	for msg := range client.Send {
		if err := client.Conn.WriteMessage(gorilla.TextMessage, msg); err != nil {
			return
		}
		client.BytesSent += int64(len(msg))
	}
}

func (s *Server) processMessage(client *WSClient, msgType string, payload map[string]any, id *string) {
	if ack := ackMessage(id); len(ack) > 0 {
		client.Send <- ack
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
	case "doubleclick":
		s.mouse.DoubleClick()
	case "rightclick":
		s.mouse.Click("right")
	case "scroll":
		s.mouse.Scroll(int(number(payload["delta"])))
	case "hello":
		name, _ := payload["name"].(string)
		client.Name = name
		if client.Name == "" {
			client.Name = "Android"
		}
		s.deviceMgr.UpdateDeviceName(client.ID, client.Name)
		client.Send <- []byte(`{"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}`)
	case "gesture":
		gesture, _ := payload["gesture"].(string)
		confidence := number(payload["confidence"])
		utils.LogInfo("Gesture received", "device", client.ID, "gesture", gesture, "confidence", confidence)
	case "proximity":
		isNear, _ := payload["is_near"].(bool)
		distance := number(payload["distance"])
		utils.LogInfo("Proximity received", "device", client.ID, "near", isNear, "distance", distance)
		if s.proximityMgr != nil {
			s.proximityMgr.ProcessUpdate(proximity.ProximityUpdate{
				DeviceID:  client.ID,
				IsNear:    isNear,
				Distance:  float32(distance),
				Timestamp: time.Now().UnixMilli(),
			})
		}
	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "pause_movement":
			control.SetMovementPaused(true)
		case "resume_movement":
			control.SetMovementPaused(false)
		}
	case "ping":
		client.Send <- []byte(`{"type":"pong"}`)
	case "pong":
		// no-op
	default:
		utils.LogDebug("WebSocket message ignored", "type", msgType)
	}
}

func (s *Server) Stop() error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if !s.running {
		return nil
	}
	s.running = false
	if s.server != nil {
		_ = s.server.Close()
	}
	for _, c := range s.clients {
		close(c.Send)
		c.Conn.Close()
	}
	s.clients = make(map[string]*WSClient)
	return nil
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return map[string]interface{}{"clients": len(s.clients)}
}
