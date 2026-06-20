package websocket

import (
	"encoding/json"
	"net/http"
	"strings"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/handler/dto"
	"airmouse-go/internal/infra/logger"
	"airmouse-go/internal/pkg/utils"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		origin := r.Header.Get("Origin")
		if origin == "" {
			return true
		}
		// Allow localhost and any origin (configurable)
		return strings.Contains(origin, "localhost") ||
			strings.Contains(origin, "127.0.0.1") ||
			strings.HasPrefix(origin, "http://") ||
			strings.HasPrefix(origin, "https://")
	},
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
	Subprotocols:    []string{"json"},
}

type Handler struct {
	hub *Hub
}

func NewHandler(hub *Hub) *Handler {
	return &Handler{hub: hub}
}

func WebSocketHandler(hub *Hub) http.HandlerFunc {
	h := NewHandler(hub)
	return h.ServeHTTP
}

func (h *Handler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	// Optional authentication
	token := r.URL.Query().Get("token")
	if token != "" {
		// Validate token (implementation depends on auth service)
		// if !h.hub.authService.ValidateToken(token) {
		//     w.WriteHeader(http.StatusUnauthorized)
		//     return
		// }
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		logger.Error("WebSocket upgrade failed: %v", err)
		return
	}

	id := utils.GenerateID()
	client := NewClient(id, conn, h.hub)

	// Send welcome message
	welcome := dto.NewWelcome()
	if data, err := json.Marshal(welcome); err == nil {
		client.send <- data
	}

	h.hub.register <- client
	logger.Info("WebSocket client registered: %s from %s", id, conn.RemoteAddr())

	// Start client goroutines
	go client.WritePump()
	go client.ReadPump(h)
}

// ProcessMessage processes incoming WebSocket messages
func (h *Handler) ProcessMessage(client *Client, data []byte) {
	// Increment receive stats
	atomic.AddInt64(&h.hub.stats.TotalBytesRecv, int64(len(data)))
	atomic.AddInt64(&h.hub.stats.TotalMessages, 1)

	// Parse message
	var root map[string]json.RawMessage
	if err := json.Unmarshal(data, &root); err != nil {
		logger.Debug("Failed to parse WebSocket message: %v", err)
		return
	}

	// Extract type
	var typ string
	if tRaw, ok := root["type"]; ok {
		if err := json.Unmarshal(tRaw, &typ); err != nil {
			return
		}
	}

	// Extract ID for ACK
	var msgID string
	if idRaw, ok := root["id"]; ok {
		_ = json.Unmarshal(idRaw, &msgID)
	}

	// Log incoming message (debug)
	logger.Debug("WebSocket message: type=%s, client=%s", typ, client.id)

	// Process by type
	switch typ {
	case "move":
		h.handleMove(client, root)
	case "click":
		h.handleClick(client, root)
	case "doubleclick":
		h.handleDoubleClick(client)
	case "rightclick":
		h.handleRightClick(client)
	case "scroll":
		h.handleScroll(client, root)
	case "hello":
		h.handleHello(client, root)
	case "gesture":
		h.handleGesture(client, root)
	case "proximity":
		h.handleProximity(client, root)
	case "control":
		h.handleControl(client, root)
	case "ping":
		client.send <- []byte(`{"type":"pong"}`)
	default:
		logger.Debug("Unknown message type: %s", typ)
	}

	// Send ACK if ID was provided
	if msgID != "" {
		ack := map[string]interface{}{
			"type":      "ack",
			"id":        msgID,
			"timestamp": time.Now().Unix(),
		}
		if ackData, err := json.Marshal(ack); err == nil {
			select {
			case client.send <- ackData:
			default:
			}
		}
	}
}

func (h *Handler) handleMove(client *Client, root map[string]json.RawMessage) {
	var dx, dy float64

	// Try payload format first
	if pRaw, ok := root["payload"]; ok {
		var p dto.MovePayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			dx, dy = p.DX, p.DY
		}
	} else {
		// Flat format
		if dRaw, ok := root["dx"]; ok {
			_ = json.Unmarshal(dRaw, &dx)
		}
		if dRaw, ok := root["dy"]; ok {
			_ = json.Unmarshal(dRaw, &dy)
		}
	}

	// Log movement
	logger.Debug("Move: dx=%.2f, dy=%.2f, client=%s", dx, dy, client.id)

	if h.hub.mouseService != nil {
		if err := h.hub.mouseService.Move(dx, dy); err != nil {
			logger.Error("Move failed: %v", err)
		}
	} else {
		logger.Warn("Mouse service not available")
	}
}

func (h *Handler) handleClick(client *Client, root map[string]json.RawMessage) {
	var button string

	if pRaw, ok := root["payload"]; ok {
		var p dto.ClickPayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			button = p.Button
		}
	} else if bRaw, ok := root["button"]; ok {
		_ = json.Unmarshal(bRaw, &button)
	}

	if button == "" {
		button = "left"
	}

	logger.Debug("Click: %s, client=%s", button, client.id)

	if h.hub.mouseService != nil {
		if err := h.hub.mouseService.Click(entity.MouseButton(button)); err != nil {
			logger.Error("Click failed: %v", err)
		}
	}
}

func (h *Handler) handleDoubleClick(client *Client) {
	logger.Debug("DoubleClick, client=%s", client.id)
	if h.hub.mouseService != nil {
		if err := h.hub.mouseService.DoubleClick(); err != nil {
			logger.Error("DoubleClick failed: %v", err)
		}
	}
}

func (h *Handler) handleRightClick(client *Client) {
	logger.Debug("RightClick, client=%s", client.id)
	if h.hub.mouseService != nil {
		if err := h.hub.mouseService.RightClick(); err != nil {
			logger.Error("RightClick failed: %v", err)
		}
	}
}

func (h *Handler) handleScroll(client *Client, root map[string]json.RawMessage) {
	var delta int

	if pRaw, ok := root["payload"]; ok {
		var p dto.ScrollPayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			delta = p.Delta
		}
	} else if dRaw, ok := root["delta"]; ok {
		_ = json.Unmarshal(dRaw, &delta)
	}

	logger.Debug("Scroll: delta=%d, client=%s", delta, client.id)

	if h.hub.mouseService != nil {
		if err := h.hub.mouseService.Scroll(delta); err != nil {
			logger.Error("Scroll failed: %v", err)
		}
	}
}

func (h *Handler) handleHello(client *Client, root map[string]json.RawMessage) {
	var name string
	var version string

	if pRaw, ok := root["payload"]; ok {
		var p dto.HelloPayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			name = p.Name
			version = p.Version
		}
	} else {
		if nRaw, ok := root["name"]; ok {
			_ = json.Unmarshal(nRaw, &name)
		}
		if vRaw, ok := root["version"]; ok {
			_ = json.Unmarshal(vRaw, &version)
		}
	}

	if name != "" {
		client.entity.Name = name
		client.entity.Version = version
		if h.hub.connService != nil {
			if err := h.hub.connService.RegisterClient(client.entity); err != nil {
				logger.Error("Failed to register client: %v", err)
			}
		}
		logger.Info("Client identified: %s (version %s)", name, version)
	}
}

func (h *Handler) handleGesture(client *Client, root map[string]json.RawMessage) {
	var gesture string
	var confidence float64

	if pRaw, ok := root["payload"]; ok {
		var p dto.GesturePayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			gesture = p.Gesture
			confidence = p.Confidence
		}
	}

	if gesture != "" && confidence > 0.7 {
		if h.hub.gestureService != nil {
			if err := h.hub.gestureService.ExecuteGesture(entity.NewGesture(entity.GestureType(gesture), confidence)); err != nil {
				logger.Error("Gesture processing failed: %v", err)
			}
		}
		logger.Info("Gesture detected: %s (confidence: %.2f)", gesture, confidence)
	}
}

func (h *Handler) handleProximity(client *Client, root map[string]json.RawMessage) {
	var distance float32
	var isNear bool
	var deviceID string

	if pRaw, ok := root["payload"]; ok {
		var p dto.ProximityPayload
		if err := json.Unmarshal(pRaw, &p); err == nil {
			distance = p.Distance
			isNear = p.IsNear
			deviceID = p.DeviceID
		}
	}

	logger.Debug("Proximity update: device=%s, distance=%.2f, near=%v", deviceID, distance, isNear)
}

func (h *Handler) handleControl(client *Client, root map[string]json.RawMessage) {
	var command string

	if pRaw, ok := root["payload"]; ok {
		var cmdMap map[string]string
		if err := json.Unmarshal(pRaw, &cmdMap); err == nil {
			command = cmdMap["command"]
		}
	} else if cRaw, ok := root["command"]; ok {
		_ = json.Unmarshal(cRaw, &command)
	}

	switch command {
	case "pause_movement":
		logger.Info("Movement paused by client: %s", client.id)
	case "resume_movement":
		logger.Info("Movement resumed by client: %s", client.id)
	default:
		logger.Debug("Unknown control command: %s", command)
	}
}