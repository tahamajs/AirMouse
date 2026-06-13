package entity

import (
    "crypto/sha256"
    "encoding/hex"
    "sync"
    "time"
)

// ClientStatus represents the current connection state
type ClientStatus string

const (
    StatusConnected    ClientStatus = "connected"
    StatusDisconnected ClientStatus = "disconnected"
    StatusIdle         ClientStatus = "idle"
    StatusBusy         ClientStatus = "busy"
    StatusAuthenticating ClientStatus = "authenticating"
    StatusBlocked      ClientStatus = "blocked"
)

// ClientCapabilities describes what the client supports
type ClientCapabilities struct {
    SupportsTouchpad   bool   `json:"supports_touchpad"`
    SupportsGestures   bool   `json:"supports_gestures"`
    SupportsVoice      bool   `json:"supports_voice"`
    SupportsHID        bool   `json:"supports_hid"`
    SupportsProximity  bool   `json:"supports_proximity"`
    SupportsML         bool   `json:"supports_ml"`
    ProtocolVersion    int    `json:"protocol_version"`
    DeviceModel        string `json:"device_model,omitempty"`
    OSVersion          string `json:"os_version,omitempty"`
    AppVersion         string `json:"app_version,omitempty"`
}

// Client represents a connected device
type Client struct {
    ID           string             `json:"id"`
    Name         string             `json:"name"`
    Status       ClientStatus       `json:"status"`
    Capabilities ClientCapabilities `json:"capabilities"`

    // Connection details
    Transport   string    `json:"transport"`
    RemoteAddr  string    `json:"remote_addr"`
    ConnectedAt time.Time `json:"connected_at"`
    LastActive  time.Time `json:"last_active"`

    // Statistics
    BytesSent    int64 `json:"bytes_sent"`
    BytesRecv    int64 `json:"bytes_recv"`
    MessagesSent int64 `json:"messages_sent"`
    MessagesRecv int64 `json:"messages_recv"`
    Errors       int64 `json:"errors"`

    // Runtime metrics
    PingLatency   time.Duration `json:"ping_latency"`
    Jitter        float64       `json:"jitter"`
    PacketLoss    float64       `json:"packet_loss"`
    LastHeartbeat time.Time     `json:"last_heartbeat"`
    LastError     string        `json:"last_error,omitempty"`

    // Internal
    mu             sync.RWMutex `json:"-"`
    heartbeatHistory []time.Duration
    maxHistorySize   int
}

// NewClient creates a new client with defaults
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
        heartbeatHistory: make([]time.Duration, 0, 10),
        maxHistorySize: 10,
    }
}

// UpdateActivity refreshes LastActive
func (c *Client) UpdateActivity() {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.LastActive = time.Now()
}

// UpdateHeartbeat updates heartbeat and calculates jitter
func (c *Client) UpdateHeartbeat(latency time.Duration) {
    c.mu.Lock()
    defer c.mu.Unlock()
    
    c.LastHeartbeat = time.Now()
    if latency > 0 {
        // Store history
        c.heartbeatHistory = append(c.heartbeatHistory, latency)
        if len(c.heartbeatHistory) > c.maxHistorySize {
            c.heartbeatHistory = c.heartbeatHistory[1:]
        }
        
        // Calculate jitter (standard deviation of latencies)
        if len(c.heartbeatHistory) > 1 {
            var sum, sumSq float64
            for _, l := range c.heartbeatHistory {
                ms := float64(l.Milliseconds())
                sum += ms
                sumSq += ms * ms
            }
            mean := sum / float64(len(c.heartbeatHistory))
            variance := sumSq/float64(len(c.heartbeatHistory)) - mean*mean
            c.Jitter = variance
        }
        
        // Update average latency (EMA)
        if c.PingLatency == 0 {
            c.PingLatency = latency
        } else {
            c.PingLatency = time.Duration(float64(c.PingLatency)*0.8 + float64(latency)*0.2)
        }
    }
}

// AddTraffic increments byte counters and message counts
func (c *Client) AddTraffic(sent, recv int64) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.BytesSent += sent
    c.BytesRecv += recv
    c.MessagesSent++
    c.MessagesRecv++
}

// AddError records an error
func (c *Client) AddError(errMsg string) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.Errors++
    c.LastError = errMsg
}

// IsActive returns true if client has been active recently
func (c *Client) IsActive() bool {
    c.mu.RLock()
    defer c.mu.RUnlock()
    return time.Since(c.LastActive) < 30*time.Second
}

// IsHealthy returns true if client is responding
func (c *Client) IsHealthy() bool {
    c.mu.RLock()
    defer c.mu.RUnlock()
    return time.Since(c.LastHeartbeat) < 60*time.Second
}

// GetAverageLatency returns average latency
func (c *Client) GetAverageLatency() time.Duration {
    c.mu.RLock()
    defer c.mu.RUnlock()
    if len(c.heartbeatHistory) == 0 {
        return 0
    }
    var sum time.Duration
    for _, l := range c.heartbeatHistory {
        sum += l
    }
    return sum / time.Duration(len(c.heartbeatHistory))
}

// SetCapabilities updates the client's feature set
func (c *Client) SetCapabilities(caps ClientCapabilities) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.Capabilities = caps
}

// SetStatus changes the client's state
func (c *Client) SetStatus(status ClientStatus) {
    c.mu.Lock()
    defer c.mu.Unlock()
    c.Status = status
}

// SetName sets client name
func (c *Client) SetName(name string) {
    c.mu.Lock()
    defer c.mu.Unlock()
    if name != "" {
        c.Name = name
    }
}

// GetIDHash returns a hash of the client ID for security
func (c *Client) GetIDHash() string {
    hash := sha256.Sum256([]byte(c.ID))
    return hex.EncodeToString(hash[:8])
}

// ToMap converts client to map for API responses
func (c *Client) ToMap() map[string]interface{} {
    c.mu.RLock()
    defer c.mu.RUnlock()
    
    return map[string]interface{}{
        "id":            c.ID,
        "name":          c.Name,
        "status":        c.Status,
        "transport":     c.Transport,
        "remote_addr":   c.RemoteAddr,
        "connected_at":  c.ConnectedAt.Unix(),
        "last_active":   c.LastActive.Unix(),
        "ping_latency":  c.PingLatency.Milliseconds(),
        "jitter":        c.Jitter,
        "bytes_sent":    c.BytesSent,
        "bytes_recv":    c.BytesRecv,
        "messages_sent": c.MessagesSent,
        "messages_recv": c.MessagesRecv,
        "capabilities":  c.Capabilities,
    }
}

func min(a, b int) int {
    if a < b {
        return a
    }
    return b
}