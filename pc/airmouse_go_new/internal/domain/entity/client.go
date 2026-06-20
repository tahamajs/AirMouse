package entity

import (
	"time"
)

// ClientStatus represents the current connection state.
type ClientStatus string

const (
	StatusConnected    ClientStatus = "connected"
	StatusDisconnected ClientStatus = "disconnected"
	StatusIdle         ClientStatus = "idle"
	StatusBusy         ClientStatus = "busy"
)

// ClientCapabilities describes what the client supports.
type ClientCapabilities struct {
	SupportsTouchpad bool `json:"supports_touchpad"`
	SupportsGestures bool `json:"supports_gestures"`
	SupportsVoice    bool `json:"supports_voice"`
	SupportsHID      bool `json:"supports_hid"`
	ProtocolVersion  int  `json:"protocol_version"` // e.g., 3
}

// Client represents a connected device (phone, tablet, etc.).
type Client struct {
    ID           string             `json:"id"`   // Unique identifier
    Name         string             `json:"name"` // Friendly name
    Status       ClientStatus       `json:"status"`
    Capabilities ClientCapabilities `json:"capabilities"`
    Version      string             `json:"version,omitempty"`
    Type         string             `json:"type,omitempty"`

	// Connection details
	Transport   string    `json:"transport"`   // "websocket", "tcp", "udp", "bluetooth", "usb"
	RemoteAddr  string    `json:"remote_addr"` // IP:port or MAC
	ConnectedAt time.Time `json:"connected_at"`
	LastActive  time.Time `json:"last_active"`

	// Statistics
	BytesSent    int64 `json:"bytes_sent"`
	BytesRecv    int64 `json:"bytes_recv"`
	MessagesSent int64 `json:"messages_sent"`
	MessagesRecv int64 `json:"messages_recv"`

	// Runtime
    PingLatency   time.Duration `json:"ping_latency"` // current round‑trip
    Jitter        float64       `json:"jitter"`       // network jitter in ms
    PacketLoss    float64       `json:"packet_loss,omitempty"`
    Errors        int64         `json:"errors,omitempty"`
    LastHeartbeat time.Time     `json:"last_heartbeat"`
}

// NewClient creates a new client with defaults.
func NewClient(id, transport, remoteAddr string) *Client {
	now := time.Now()
	return &Client{
		ID:            id,
		Name:          id[:min(8, len(id))],
		Status:        StatusConnected,
		Transport:     transport,
		RemoteAddr:    remoteAddr,
		ConnectedAt:   now,
		LastActive:    now,
		LastHeartbeat: now,
	}
}

// UpdateActivity refreshes LastActive.
func (c *Client) UpdateActivity() {
	c.LastActive = time.Now()
}

// UpdateHeartbeat refreshes the heartbeat timestamp and optionally updates latency.
func (c *Client) UpdateHeartbeat(latency time.Duration) {
	c.LastHeartbeat = time.Now()
	if latency > 0 {
		// Exponential moving average for jitter
		newLatency := float64(latency.Milliseconds())
		c.PingLatency = time.Duration((float64(c.PingLatency)*0.8 + newLatency*0.2)) * time.Millisecond
	}
}

// AddTraffic increments byte counters and message counts.
func (c *Client) AddTraffic(sent, recv int64) {
	c.BytesSent += sent
	c.BytesRecv += recv
	c.MessagesSent++
	c.MessagesRecv++
}

// IsActive returns true if the client has been active within the last 30 seconds.
func (c *Client) IsActive() bool {
    return time.Since(c.LastActive) < 30*time.Second
}

// IsHealthy reports whether the client is considered healthy by basic heuristics.
func (c *Client) IsHealthy() bool {
    if c.Status != StatusConnected {
        return false
    }
    if c.PacketLoss > 0.2 {
        return false
    }
    if c.Errors > 10 {
        return false
    }
    return true
}

// SetType stores the transport/device category for legacy callers.
func (c *Client) SetType(typ string) {
	c.Type = typ
}

// SetCapabilities updates the client’s feature set.
func (c *Client) SetCapabilities(caps ClientCapabilities) {
	c.Capabilities = caps
}

// SetStatus changes the client’s state.
func (c *Client) SetStatus(status ClientStatus) {
	c.Status = status
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
