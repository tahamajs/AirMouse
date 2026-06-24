package bluetooth

import (
    "bufio"
    "fmt"
    "io"
    "sync"
    "time"

	"airmouse-go/internal/logger"
)

// SerialConnection represents a Bluetooth serial (SPP) connection
type SerialConnection struct {
    Port        string
    Connected   bool
    ConnectedAt time.Time
    LastActive  time.Time
    BytesSent   int64
    BytesRecv   int64
    reader      *bufio.Reader
    writer      io.Writer
    stopChan    chan struct{}
    mu          sync.RWMutex
}

// SerialManager manages Bluetooth serial connections
type SerialManager struct {
    connections map[string]*SerialConnection
    mu          sync.RWMutex
    running     bool
    callbacks   []func(event SerialEvent)
}

// SerialEvent represents a serial port event
type SerialEvent struct {
    Type      string // "connected", "disconnected", "data"
    Port      string
    Data      []byte
    Timestamp time.Time
}

// NewSerialManager creates a new serial manager
func NewSerialManager() *SerialManager {
    return &SerialManager{
        connections: make(map[string]*SerialConnection),
        callbacks:   make([]func(SerialEvent), 0),
        running:     true,
    }
}

// NewSerialConnection creates a new serial connection
func NewSerialConnection(port string) *SerialConnection {
    return &SerialConnection{
        Port:        port,
        Connected:   false,
        ConnectedAt: time.Now(),
        LastActive:  time.Now(),
        stopChan:    make(chan struct{}),
    }
}

// Connect establishes a serial connection
func (s *SerialConnection) Connect() error {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if s.Connected {
        return fmt.Errorf("already connected")
    }
    
    // In production, open actual serial port
    // For now, simulate connection
    s.Connected = true
    s.ConnectedAt = time.Now()
    s.LastActive = time.Now()
    
    logger.Info("Serial connection opened: port=%s", s.Port)
    return nil
}

// Disconnect closes the serial connection
func (s *SerialConnection) Disconnect() {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if !s.Connected {
        return
    }
    
    close(s.stopChan)
    s.Connected = false
    logger.Info("Serial connection closed: port=%s", s.Port)
}

// Write writes data to the serial port
func (s *SerialConnection) Write(data []byte) (int, error) {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if !s.Connected {
        return 0, fmt.Errorf("not connected")
    }
    
    s.BytesSent += int64(len(data))
    s.LastActive = time.Now()
    
    // In production, write to actual serial port
    logger.Debug("Serial write: port=%s bytes=%d", s.Port, len(data))
    return len(data), nil
}

// Read reads data from the serial port
func (s *SerialConnection) Read(p []byte) (int, error) {
    s.mu.Lock()
    defer s.mu.Unlock()
    
    if !s.Connected {
        return 0, fmt.Errorf("not connected")
    }
    
    // In production, read from actual serial port
    // For now, return empty
    return 0, io.EOF
}

// IsConnected returns connection status
func (s *SerialConnection) IsConnected() bool {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return s.Connected
}

// GetStats returns connection statistics
func (s *SerialConnection) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    return map[string]interface{}{
        "port":         s.Port,
        "connected":    s.Connected,
        "connected_at": s.ConnectedAt,
        "last_active":  s.LastActive,
        "bytes_sent":   s.BytesSent,
        "bytes_recv":   s.BytesRecv,
    }
}

// Start starts the serial manager
func (sm *SerialManager) Start() error {
    sm.running = true
    logger.Info("Bluetooth serial manager started")
    return nil
}

// Stop stops the serial manager
func (sm *SerialManager) Stop() {
    sm.running = false
    
    sm.mu.Lock()
    defer sm.mu.Unlock()
    
    for _, conn := range sm.connections {
        conn.Disconnect()
    }
    sm.connections = make(map[string]*SerialConnection)
    
    logger.Info("Bluetooth serial manager stopped")
}

// AddConnection adds a serial connection
func (sm *SerialManager) AddConnection(port string) error {
    sm.mu.Lock()
    defer sm.mu.Unlock()
    
    if _, exists := sm.connections[port]; exists {
        return fmt.Errorf("connection already exists: %s", port)
    }
    
    conn := NewSerialConnection(port)
    if err := conn.Connect(); err != nil {
        return err
    }
    
    sm.connections[port] = conn
    sm.triggerEvent(SerialEvent{
        Type:      "connected",
        Port:      port,
        Timestamp: time.Now(),
    })
    
    return nil
}

// RemoveConnection removes a serial connection
func (sm *SerialManager) RemoveConnection(port string) error {
    sm.mu.Lock()
    defer sm.mu.Unlock()
    
    conn, exists := sm.connections[port]
    if !exists {
        return fmt.Errorf("connection not found: %s", port)
    }
    
    conn.Disconnect()
    delete(sm.connections, port)
    
    sm.triggerEvent(SerialEvent{
        Type:      "disconnected",
        Port:      port,
        Timestamp: time.Now(),
    })
    
    return nil
}

// GetConnection returns a serial connection
func (sm *SerialManager) GetConnection(port string) *SerialConnection {
    sm.mu.RLock()
    defer sm.mu.RUnlock()
    return sm.connections[port]
}

// GetAllConnections returns all connections
func (sm *SerialManager) GetAllConnections() []*SerialConnection {
    sm.mu.RLock()
    defer sm.mu.RUnlock()
    
    conns := make([]*SerialConnection, 0, len(sm.connections))
    for _, conn := range sm.connections {
        conns = append(conns, conn)
    }
    return conns
}

// WriteTo writes data to a specific port
func (sm *SerialManager) WriteTo(port string, data []byte) (int, error) {
    sm.mu.RLock()
    conn, exists := sm.connections[port]
    sm.mu.RUnlock()
    
    if !exists {
        return 0, fmt.Errorf("connection not found: %s", port)
    }
    
    return conn.Write(data)
}

// Broadcast writes data to all connections
func (sm *SerialManager) Broadcast(data []byte) {
    sm.mu.RLock()
    defer sm.mu.RUnlock()
    
    for _, conn := range sm.connections {
        go conn.Write(data)
    }
}

// AddEventListener adds a callback for serial events
func (sm *SerialManager) AddEventListener(callback func(event SerialEvent)) {
    sm.mu.Lock()
    defer sm.mu.Unlock()
    sm.callbacks = append(sm.callbacks, callback)
}

func (sm *SerialManager) triggerEvent(event SerialEvent) {
    sm.mu.RLock()
    callbacks := make([]func(SerialEvent), len(sm.callbacks))
    copy(callbacks, sm.callbacks)
    sm.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

// GetStats returns manager statistics
func (sm *SerialManager) GetStats() map[string]interface{} {
    sm.mu.RLock()
    defer sm.mu.RUnlock()
    
    var totalSent, totalRecv int64
    for _, conn := range sm.connections {
        stats := conn.GetStats()
        totalSent += stats["bytes_sent"].(int64)
        totalRecv += stats["bytes_recv"].(int64)
    }
    
    return map[string]interface{}{
        "connections":       len(sm.connections),
        "running":           sm.running,
        "total_bytes_sent":  totalSent,
        "total_bytes_recv":  totalRecv,
    }
}