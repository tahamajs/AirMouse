package websocket

import (
	"encoding/json"
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/handler/dto"
)

type Client struct {
	id       string
	conn     *websocket.Conn
	send     chan []byte
	hub      *Hub
	lastPing int64
	entity   *entity.Client
}

func NewClient(id string, conn *websocket.Conn, hub *Hub) *Client {
	return &Client{
		id:       id,
		conn:     conn,
		send:     make(chan []byte, 256),
		hub:      hub,
		lastPing: time.Now().Unix(),
		entity: &entity.Client{
			ID:          id,
			Name:        "unknown",
			ConnectedAt: time.Now(),
			LastActive:  time.Now(),
			Transport:   "websocket",
			RemoteAddr:  conn.RemoteAddr().String(),
		},
	}
}

func (c *Client) readPump() {
	defer func() {
		c.hub.unregister <- c
		c.conn.Close()
	}()
	c.conn.SetReadLimit(512)
	c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	c.conn.SetPongHandler(func(string) error {
		atomic.StoreInt64(&c.lastPing, time.Now().Unix())
		c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		_ = c.hub.connService.Heartbeat(c.id, 0)
		return nil
	})
	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			break
		}
		c.processMessage(message)
	}
}

func (c *Client) writePump() {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		ticker.Stop()
		c.conn.Close()
	}()
	for {
		select {
		case message, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}
		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

func (c *Client) processMessage(data []byte) {
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(data, &raw); err != nil {
		return
	}
	var msgType string
	if tRaw, ok := raw["type"]; ok {
		_ = json.Unmarshal(tRaw, &msgType)
	}
	var id string
	if idRaw, ok := raw["id"]; ok {
		_ = json.Unmarshal(idRaw, &id)
	}
	payload := raw["payload"]
	if len(payload) == 0 {
		if msgType == "" {
			return
		}
		payload = data
	}
	switch msgType {
	case "move":
		var p dto.MovePayload
		if err := json.Unmarshal(payload, &p); err != nil {
			var flat struct {
				DX float64 `json:"dx"`
				DY float64 `json:"dy"`
			}
			if err := json.Unmarshal(data, &flat); err == nil {
				p.DX, p.DY = flat.DX, flat.DY
			}
		}
		_ = c.hub.mouseService.Move(p.DX, p.DY)
	case "click":
		var p dto.ClickPayload
		if err := json.Unmarshal(payload, &p); err != nil {
			var flat struct {
				Button string `json:"button"`
			}
			if err := json.Unmarshal(data, &flat); err == nil {
				p.Button = flat.Button
			}
		}
		if p.Button == "" {
			p.Button = "left"
		}
		_ = c.hub.mouseService.Click(entity.MouseButton(p.Button))
	case "doubleclick":
		_ = c.hub.mouseService.DoubleClick()
	case "rightclick":
		_ = c.hub.mouseService.RightClick()
	case "scroll":
		var p dto.ScrollPayload
		if err := json.Unmarshal(payload, &p); err != nil {
			var flat struct {
				Delta int `json:"delta"`
			}
			if err := json.Unmarshal(data, &flat); err == nil {
				p.Delta = flat.Delta
			}
		}
		_ = c.hub.mouseService.Scroll(p.Delta)
	case "hello":
		var p dto.HelloPayload
		if err := json.Unmarshal(payload, &p); err != nil {
			var flat struct {
				Name    string `json:"name"`
				Version string `json:"version"`
			}
			if err := json.Unmarshal(data, &flat); err == nil {
				p.Name = flat.Name
				p.Version = flat.Version
			}
		}
		if p.Name != "" {
			c.entity.Name = p.Name
			c.entity.Version = p.Version
			_ = c.hub.connService.RegisterClient(c.entity)
		}
	case "ping":
		c.send <- []byte(`{"type":"pong"}`)
	case "control":
		var payloadMap map[string]any
		if err := json.Unmarshal(payload, &payloadMap); err != nil {
			_ = json.Unmarshal(data, &payloadMap)
		}
		if cmd, _ := payloadMap["command"].(string); cmd != "" {
			switch cmd {
			case "pause_movement":
				_ = c.hub.mouseService.Pause(5)
			case "resume_movement":
				_ = c.hub.mouseService.Pause(0)
			}
		}
	}
}
