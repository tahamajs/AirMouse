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

func WebSocketHandler(hub *Hub) http.HandlerFunc {
	h := NewHandler(hub)
	return h.ServeHTTP
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
	// Support both {"type":"move","payload":{...}} and flat {"type":"move","dx":...,"dy":...}
	var root map[string]json.RawMessage
	if err := json.Unmarshal(data, &root); err != nil {
		return
	}
	var typ string
	if tRaw, ok := root["type"]; ok {
		_ = json.Unmarshal(tRaw, &typ)
	}
	// optional id for acking critical messages
	var msgID int64 = -1
	if idRaw, ok := root["id"]; ok {
		_ = json.Unmarshal(idRaw, &msgID)
	}

	switch typ {
	case "move":
		var dx, dy float64
		// try payload first
		if pRaw, ok := root["payload"]; ok {
			var p MovePayload
			if err := json.Unmarshal(pRaw, &p); err == nil {
				dx, dy = p.DX, p.DY
			}
		} else {
			// flat fields
			if dRaw, ok := root["dx"]; ok {
				_ = json.Unmarshal(dRaw, &dx)
			}
			if dRaw, ok := root["dy"]; ok {
				_ = json.Unmarshal(dRaw, &dy)
			}
		}
		_ = h.hub.mouseService.Move(dx, dy)
	case "click":
		var btn string
		if pRaw, ok := root["payload"]; ok {
			var p ClickPayload
			if err := json.Unmarshal(pRaw, &p); err == nil {
				btn = p.Button
			}
		} else if bRaw, ok := root["button"]; ok {
			_ = json.Unmarshal(bRaw, &btn)
		}
		_ = h.hub.mouseService.Click(entity.MouseButton(btn))
	case "doubleclick":
		_ = h.hub.mouseService.DoubleClick()
	case "rightclick":
		_ = h.hub.mouseService.RightClick()
	case "scroll":
		var delta int
		if pRaw, ok := root["payload"]; ok {
			var p ScrollPayload
			if err := json.Unmarshal(pRaw, &p); err == nil {
				delta = p.Delta
			}
		} else if dRaw, ok := root["delta"]; ok {
			_ = json.Unmarshal(dRaw, &delta)
		}
		_ = h.hub.mouseService.Scroll(delta)
	case "hello":
		var name string
		if pRaw, ok := root["payload"]; ok {
			var p HelloPayload
			if err := json.Unmarshal(pRaw, &p); err == nil {
				name = p.Name
			}
		} else if nRaw, ok := root["name"]; ok {
			_ = json.Unmarshal(nRaw, &name)
		}
		if name != "" {
			client.entity.Name = name
			_ = h.hub.connService.RegisterClient(client.entity)
		}
	case "gesture":
		// gesture messages are forwarded to higher layers (gesture recognition may be async)
		// for now we accept the payload and let the Hub/Service decide; no-op here.
	case "proximity":
		// client proximity updates
		// read payload fields if needed and forward
	case "control":
		var cmd string
		if pRaw, ok := root["payload"]; ok {
			var obj map[string]json.RawMessage
			if err := json.Unmarshal(pRaw, &obj); err == nil {
				if cRaw, ok := obj["command"]; ok {
					_ = json.Unmarshal(cRaw, &cmd)
				}
			}
		} else if cRaw, ok := root["command"]; ok {
			_ = json.Unmarshal(cRaw, &cmd)
		}
		switch cmd {
		case "pause_movement":
			_ = h.hub.mouseService.Pause(5)
		case "resume_movement":
			_ = h.hub.mouseService.Pause(0)
		}
	}

	// send ack if requested
	if msgID >= 0 {
		ack := map[string]interface{}{"type": "ack", "id": msgID}
		if b, err := json.Marshal(ack); err == nil {
			select {
			case client.send <- b:
			default:
				// drop if client buffer full
			}
		}
	}
}
