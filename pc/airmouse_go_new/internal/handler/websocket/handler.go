package websocket

import (
	"encoding/json"
	"net/http"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/pkg/utils"
	"airmouse-go/internal/domain/entity"
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
	hub *Hub
}

// The handler uses the services present in Hub via h.hub (mouseService, gestureService, connService).

func NewHandler(hub *Hub) *Handler {
	return &Handler{hub: hub}
}

func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		return
	}
	id := utils.GenerateID()
	client := NewClient(id, conn, h.hub)
	h.hub.register <- client

	go client.writePump()
	go client.readPump()
}
// Note: read/write pumps are handled by Client methods (client.readPump/writePump).

func (h *Handler) processMessage(client *Client, data []byte) {
	var msg Message
	if err := json.Unmarshal(data, &msg); err != nil {
		return
	}
	switch msg.Type {
	case "move":
		var p MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
				    // call hub mouse service
				    _ = h.hub.mouseService.Move(p.DX, p.DY)
			// Actually move the mouse (platform‑specific) – not part of domain service.
			// We'll leave the actual mouse movement to the outer layer.
			// For simplicity, we just call the service (which should contain the actual movement logic).
			// In a real implementation, the service would call the infra mouse controller.
		}
	case "click":
		var p ClickPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = h.hub.mouseService.Click(entity.MouseButton(p.Button))
		}
	case "doubleclick":
		_ = h.hub.mouseService.DoubleClick()
	case "rightclick":
		_ = h.hub.mouseService.RightClick()
	case "scroll":
		var p ScrollPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = h.hub.mouseService.Scroll(p.Delta)
		}
	case "hello":
		var p HelloPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			client.entity.Name = p.Name
			_ = h.hub.connService.RegisterClient(client.entity)
		}
	}
}
