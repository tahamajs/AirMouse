package websocket

import (
    "encoding/json"
    "net/http"
    "strings"
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
}

func (h *Handler) processMessage(client *Client, data []byte) {
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
        json.Unmarshal(idRaw, &msgID)
    }

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
            "type": "ack",
            "id":   msgID,
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
            json.Unmarshal(dRaw, &dx)
        }
        if dRaw, ok := root["dy"]; ok {
            json.Unmarshal(dRaw, &dy)
        }
    }
    
    if err := h.hub.mouseService.Move(dx, dy); err != nil {
        logger.Error("Move failed: %v", err)
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
        json.Unmarshal(bRaw, &button)
    }
    
    if button == "" {
        button = "left"
    }
    
    if err := h.hub.mouseService.Click(entity.MouseButton(button)); err != nil {
        logger.Error("Click failed: %v", err)
    }
}

func (h *Handler) handleDoubleClick(client *Client) {
    if err := h.hub.mouseService.DoubleClick(); err != nil {
        logger.Error("DoubleClick failed: %v", err)
    }
}

func (h *Handler) handleRightClick(client *Client) {
    if err := h.hub.mouseService.RightClick(); err != nil {
        logger.Error("RightClick failed: %v", err)
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
        json.Unmarshal(dRaw, &delta)
    }
    
    if err := h.hub.mouseService.Scroll(delta); err != nil {
        logger.Error("Scroll failed: %v", err)
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
            json.Unmarshal(nRaw, &name)
        }
        if vRaw, ok := root["version"]; ok {
            json.Unmarshal(vRaw, &version)
        }
    }
    
    if name != "" {
        client.entity.Name = name
        client.entity.Version = version
        if err := h.hub.connService.RegisterClient(client.entity); err != nil {
            logger.Error("Failed to register client: %v", err)
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
        if err := h.hub.gestureService.ProcessGesture(gesture, confidence); err != nil {
            logger.Error("Gesture processing failed: %v", err)
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
        json.Unmarshal(cRaw, &command)
    }
    
    switch command {
    case "pause_movement":
        // Implement pause
        logger.Info("Movement paused by client: %s", client.id)
    case "resume_movement":
        // Implement resume
        logger.Info("Movement resumed by client: %s", client.id)
    default:
        logger.Debug("Unknown control command: %s", command)
    }
}