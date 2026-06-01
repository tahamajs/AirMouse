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
	var msg dto.Message
	if err := json.Unmarshal(data, &msg); err != nil {
		return
	}
	switch msg.Type {
	case "move":
		var p dto.MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = c.hub.mouseService.Move(p.DX, p.DY)
		}
	case "click":
		var p dto.ClickPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = c.hub.mouseService.Click(entity.MouseButton(p.Button))
		}
	case "doubleclick":
		_ = c.hub.mouseService.DoubleClick()
	case "rightclick":
		_ = c.hub.mouseService.RightClick()
	case "scroll":
		var p dto.ScrollPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			_ = c.hub.mouseService.Scroll(p.Delta)
		}
	case "hello":
		var p dto.HelloPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			c.entity.Name = p.Name
			_ = c.hub.connService.RegisterClient(c.entity)
		}
	case "ping":
		c.send <- []byte(`{"type":"pong"}`)
	}
}
