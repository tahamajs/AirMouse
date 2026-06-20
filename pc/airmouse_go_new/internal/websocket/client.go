package websocket

import (
	"sync/atomic"
	"time"

	"github.com/gorilla/websocket"

	"airmouse-go/internal/domain/entity"
	"airmouse-go/internal/infra/logger"
)

// ------------------------------------------------------------
//  Client
// ------------------------------------------------------------

type Client struct {
	id         string
	conn       *websocket.Conn
	send       chan []byte
	hub        *Hub
	lastPing   int64
	lastActive int64
	entity     *entity.Client
	handler    *Handler // Reference to handler for processing messages
}

// NewClient creates a new WebSocket client
func NewClient(id string, conn *websocket.Conn, hub *Hub) *Client {
	now := time.Now()
	return &Client{
		id:         id,
		conn:       conn,
		send:       make(chan []byte, 256),
		hub:        hub,
		lastPing:   now.Unix(),
		lastActive: now.Unix(),
		entity: &entity.Client{
			ID:          id,
			Name:        "unknown",
			ConnectedAt: now,
			LastActive:  now,
			Transport:   "websocket",
			RemoteAddr:  conn.RemoteAddr().String(),
			Status:      entity.StatusConnected,
		},
	}
}

// SetHandler sets the handler for message processing
func (c *Client) SetHandler(handler *Handler) {
	c.handler = handler
}

// ReadPump pumps messages from the WebSocket connection to the hub
func (c *Client) ReadPump(handler *Handler) {
	c.handler = handler
	defer func() {
		if c.hub != nil {
			c.hub.unregister <- c
		}
		_ = c.conn.Close()
	}()

	c.conn.SetReadLimit(4096)
	_ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	c.conn.SetPongHandler(func(string) error {
		atomic.StoreInt64(&c.lastPing, time.Now().Unix())
		c.updateLastActive()
		_ = c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		if c.hub != nil && c.hub.connService != nil {
			if err := c.hub.connService.Heartbeat(c.id, 0); err != nil {
				logger.Error("Heartbeat failed: %v", err)
			}
		}
		return nil
	})

	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				logger.Error("WebSocket read error: %v", err)
			}
			break
		}
		c.updateLastActive()

		// Process message through handler
		if c.handler != nil {
			c.handler.ProcessMessage(c, message)
		} else if c.hub != nil && c.hub.handler != nil {
			c.hub.handler.ProcessMessage(c, message)
		} else {
			logger.Warn("No handler available to process message")
		}
	}
}

// WritePump pumps messages from the hub to the WebSocket connection
func (c *Client) WritePump() {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		ticker.Stop()
		_ = c.conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.send:
			_ = c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				_ = c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			if err := c.conn.WriteMessage(websocket.TextMessage, message); err != nil {
				logger.Error("WebSocket write error: %v", err)
				return
			}

		case <-ticker.C:
			_ = c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				logger.Error("WebSocket ping error: %v", err)
				return
			}
		}
	}
}

// processMessage processes incoming messages (legacy - use handler instead)
func (c *Client) processMessage(data []byte) {
	// Delegate to hub's handler
	if c.handler != nil {
		c.handler.ProcessMessage(c, data)
	} else if c.hub != nil && c.hub.handler != nil {
		c.hub.handler.ProcessMessage(c, data)
	} else {
		logger.Warn("No handler available to process message from client: %s", c.id)
	}
}

// updateLastActive updates the last active timestamp
func (c *Client) updateLastActive() {
	atomic.StoreInt64(&c.lastActive, time.Now().Unix())
	if c.entity != nil {
		c.entity.LastActive = time.Now()
	}
}

// GetLastActive returns the last active time
func (c *Client) GetLastActive() time.Time {
	return time.Unix(atomic.LoadInt64(&c.lastActive), 0)
}

// SendMessage sends a message to the client
func (c *Client) SendMessage(message []byte) {
	if c.send == nil {
		return
	}
	select {
	case c.send <- message:
	default:
		logger.Debug("Client send buffer full, dropping message: %s", c.id)
	}
}

// GetID returns the client ID
func (c *Client) GetID() string {
	return c.id
}

// GetName returns the client name
func (c *Client) GetName() string {
	if c.entity != nil {
		return c.entity.Name
	}
	return "unknown"
}

// SetName sets the client name
func (c *Client) SetName(name string) {
	if c.entity != nil && name != "" {
		c.entity.Name = name
	}
}

// GetEntity returns the client entity
func (c *Client) GetEntity() *entity.Client {
	return c.entity
}

// Close closes the client connection
func (c *Client) Close() {
	if c.conn != nil {
		_ = c.conn.Close()
	}
	if c.send != nil {
		close(c.send)
	}
}

// IsConnected returns true if the client is connected
func (c *Client) IsConnected() bool {
	if c.conn == nil {
		return false
	}
	// Check if connection is still alive
	err := c.conn.WriteMessage(websocket.PingMessage, nil)
	return err == nil
}

// GetStatistics returns client statistics
func (c *Client) GetStatistics() map[string]interface{} {
	return map[string]interface{}{
		"id":          c.id,
		"name":        c.GetName(),
		"connected":   c.entity != nil && c.entity.Status == entity.StatusConnected,
		"last_active": c.GetLastActive().Unix(),
		"send_buffer": len(c.send),
	}
}
