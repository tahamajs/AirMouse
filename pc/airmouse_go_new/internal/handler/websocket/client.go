package websocket

import (
	"time"

	"github.com/gorilla/websocket"
)

type Client struct {
	hub        *Hub
	conn       *websocket.Conn
	send       chan []byte
	id         string
	name       string
	lastActive time.Time
}

func NewClient(hub *Hub, conn *websocket.Conn) *Client {
	return &Client{
		hub:        hub,
		conn:       conn,
		send:       make(chan []byte, 256),
		id:         conn.RemoteAddr().String(),
		lastActive: time.Now(),
	}
}

func (c *Client) ID() string    { return c.id }
func (c *Client) Name() string  { return c.name }
func (c *Client) SetName(name string) { c.name = name }
func (c *Client) UpdateActivity() { c.lastActive = time.Now() }
func (c *Client) IsIdle(timeout time.Duration) bool {
	return time.Since(c.lastActive) > timeout
}