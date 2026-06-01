package websocket

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	CheckOrigin:     func(r *http.Request) bool { return true },
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type Message struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
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

type Handler struct {
	hub            *Hub
	mouseService   MouseService
	gestureService GestureService
	connService    ConnectionService
}

type MouseService interface {
	Move(dx, dy float64, dt float64) (float64, float64, error)
	Click(button string) error
	DoubleClick() error
	RightClick() error
	Scroll(delta int) error
}

type GestureService interface {
	DetectGesture(gyroY, accelY float64, dt float64) interface{}
}

type ConnectionService interface {
	AddClient(id, name, clientType string) (interface{}, error)
	UpdateClientActivity(id string) error
}

func NewHandler(hub *Hub, mouseSvc MouseService, gestureSvc GestureService, connSvc ConnectionService) *Handler {
	return &Handler{
		hub:            hub,
		mouseService:   mouseSvc,
		gestureService: gestureSvc,
		connService:    connSvc,
	}
}

func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	client := NewClient(h.hub, conn)
	h.hub.register <- client

	go h.writePump(client)
	go h.readPump(client)
}

func (h *Handler) readPump(client *Client) {
	defer func() {
		h.hub.unregister <- client
		client.conn.Close()
		h.connService.UpdateClientActivity(client.id) // will fail if not registered; safe
	}()

	client.conn.SetReadLimit(512)
	client.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	client.conn.SetPongHandler(func(string) error {
		client.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})

	for {
		_, message, err := client.conn.ReadMessage()
		if err != nil {
			break
		}
		client.UpdateActivity()
		h.processMessage(client, message)
	}
}

func (h *Handler) writePump(client *Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		ticker.Stop()
		client.conn.Close()
	}()

	for {
		select {
		case message, ok := <-client.send:
			client.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				client.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := client.conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
		case <-ticker.C:
			client.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (h *Handler) processMessage(client *Client, data []byte) {
	var msg Message
	if err := json.Unmarshal(data, &msg); err != nil {
		return
	}
	switch msg.Type {
	case "move":
		var p MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			// dt is not available; use fixed 0.02s assumption
			dx, dy, _ := h.mouseService.Move(p.DX, p.DY, 0.02)
			// Actually move the mouse (platform‑specific) – not part of domain service.
			// We'll leave the actual mouse movement to the outer layer.
			// For simplicity, we just call the service (which should contain the actual movement logic).
			// In a real implementation, the service would call the infra mouse controller.
		}
	case "click":
		var p ClickPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = h.mouseService.Click(p.Button)
		}
	case "doubleclick":
		_ = h.mouseService.DoubleClick()
	case "rightclick":
		_ = h.mouseService.RightClick()
	case "scroll":
		var p ScrollPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = h.mouseService.Scroll(p.Delta)
		}
	case "hello":
		var p HelloPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			client.SetName(p.Name)
			_, _ = h.connService.AddClient(client.ID(), p.Name, "websocket")
		}
	}
}
