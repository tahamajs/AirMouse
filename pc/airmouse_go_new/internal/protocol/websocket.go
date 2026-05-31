package websocket

import (
	"encoding/json"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/utils"
)

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

func NewServer(port int, mouse control.MouseController, deviceMgr *device.Manager, authMgr *auth.Manager, proximityMgr *proximity.Manager) *Server {
	return &Server{
		port:         port,
		clients:      make(map[string]*WSClient),
		mouse:        mouse,
		deviceMgr:    deviceMgr,
		authMgr:      authMgr,
		proximityMgr: proximityMgr,
		jitterBuffer: jitter.NewJitterBuffer(jitter.DefaultJitterBufferConfig()),
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
	// token validation omitted for brevity
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
	s.deviceMgr.RegisterDevice(clientID, "websocket", client.Name)
	go s.writePump(client)
	go s.readPump(client)
}

func (s *Server) processMessage(client *WSClient, msg *WMessage) {
	switch msg.Type {
	case "move":
		var p MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			if control.IsMovementPaused() {
				return
			}
			now := time.Now()
			smDx, smDy := s.jitterBuffer.AddMovement(p.DX, p.DY, now)
			s.mouse.Move(smDx, smDy)
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
		}
	case "gesture":
		var p GesturePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			// map to sysaction
			var action sysaction.Action
			switch p.Gesture {
			case "ThumbsUp":
				action = sysaction.ActionPlayPause
			case "LeftSwipe":
				action = sysaction.ActionNextTrack
			case "RightSwipe":
				action = sysaction.ActionPrevTrack
			case "CircleCW":
				action = sysaction.ActionVolumeUp
			case "CircleCCW":
				action = sysaction.ActionVolumeDown
			}
			sysaction.Execute(action)
		}
	case "proximity":
		var update ProximityUpdate
		if err := json.Unmarshal(msg.Payload, &update); err == nil {
			if s.proximityMgr != nil {
				s.proximityMgr.ProcessUpdate(update)
			}
		}
	case "control":
		var p ControlPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			if p.Command == "pause_movement" {
				control.SetMovementPaused(true)
			} else if p.Command == "resume_movement" {
				control.SetMovementPaused(false)
			}
		}
	case "ping":
		client.Send <- []byte(`{"type":"pong"}`)
	}
}