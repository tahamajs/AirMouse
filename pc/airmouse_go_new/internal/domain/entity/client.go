package entity

import (
	"time"
)

// Client represents a connected device.
type Client struct {
	ID          string
	Name        string
	Type        string // tcp, websocket, udp, bluetooth
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	IsPaired    bool
}

// NewClient creates a new client entity.
func NewClient(id, name, clientType string) *Client {
	now := time.Now()
	return &Client{
		ID:          id,
		Name:        name,
		Type:        clientType,
		ConnectedAt: now,
		LastActive:  now,
		IsPaired:    false,
	}
}

// UpdateActivity updates the last active timestamp.
func (c *Client) UpdateActivity() {
	c.LastActive = time.Now()
}

// IsIdle checks if the client has been idle for longer than the given duration.
func (c *Client) IsIdle(timeout time.Duration) bool {
	return time.Since(c.LastActive) > timeout
}