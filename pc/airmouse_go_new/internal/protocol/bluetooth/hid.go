package bluetooth

import (
    "encoding/binary"
    "fmt"
    "sync"
    "time"

    "airmouse-go/internal/infra/logger"
)

// HIDService represents a Bluetooth HID service
type HIDService struct {
    manager      *Manager
    connections  map[string]*HIDConnection
    mu           sync.RWMutex
    reportMap    []byte
    running      bool
}

// HIDConnection represents an active HID connection
type HIDConnection struct {
    DeviceAddr   string
    ConnectedAt  time.Time
    LastActivity time.Time
    ReportChannel chan HIDReport
    StopChan     chan struct{}
}

// HIDReportDescriptor for mouse
var HIDReportDescriptor = []byte{
    0x05, 0x01, // Usage Page (Generic Desktop)
    0x09, 0x02, // Usage (Mouse)
    0xA1, 0x01, // Collection (Application)
    0x09, 0x01, //   Usage (Pointer)
    0xA1, 0x00, //   Collection (Physical)
    0x05, 0x09, //     Usage Page (Button)
    0x19, 0x01, //     Usage Minimum (1)
    0x29, 0x03, //     Usage Maximum (3)
    0x15, 0x00, //     Logical Minimum (0)
    0x25, 0x01, //     Logical Maximum (1)
    0x95, 0x03, //     Report Count (3)
    0x75, 0x01, //     Report Size (1)
    0x81, 0x02, //     Input (Data, Var, Abs)
    0x95, 0x01, //     Report Count (1)
    0x75, 0x05, //     Report Size (5)
    0x81, 0x01, //     Input (Const)
    0x05, 0x01, //     Usage Page (Generic Desktop)
    0x09, 0x30, //     Usage (X)
    0x09, 0x31, //     Usage (Y)
    0x16, 0x01, 0x80, // Logical Minimum (-32767)
    0x26, 0xFF, 0x7F, // Logical Maximum (32767)
    0x75, 0x10, //     Report Size (16)
    0x95, 0x02, //     Report Count (2)
    0x81, 0x06, //     Input (Data, Var, Rel)
    0x09, 0x38, //     Usage (Wheel)
    0x15, 0x81, //     Logical Minimum (-127)
    0x25, 0x7F, //     Logical Maximum (127)
    0x75, 0x08, //     Report Size (8)
    0x95, 0x01, //     Report Count (1)
    0x81, 0x06, //     Input (Data, Var, Rel)
    0xC0,       //   End Collection
    0xC0,       // End Collection
}

// NewHIDService creates a new HID service
func NewHIDService(manager *Manager) *HIDService {
    return &HIDService{
        manager:     manager,
        connections: make(map[string]*HIDConnection),
        reportMap:   HIDReportDescriptor,
        running:     true,
    }
}

// Start starts the HID service
func (h *HIDService) Start() error {
    logger.Info("HID over GATT service started")
    return nil
}

// Stop stops the HID service
func (h *HIDService) Stop() {
    h.running = false
    h.mu.Lock()
    defer h.mu.Unlock()
    
    for addr, conn := range h.connections {
        close(conn.StopChan)
        delete(h.connections, addr)
    }
    logger.Info("HID over GATT service stopped")
}

// RegisterConnection registers a new HID connection
func (h *HIDService) RegisterConnection(addr string) error {
    h.mu.Lock()
    defer h.mu.Unlock()
    
    if _, exists := h.connections[addr]; exists {
        return fmt.Errorf("connection already exists: %s", addr)
    }
    
    conn := &HIDConnection{
        DeviceAddr:    addr,
        ConnectedAt:   time.Now(),
        LastActivity:  time.Now(),
        ReportChannel: make(chan HIDReport, 50),
        StopChan:      make(chan struct{}),
    }
    
    h.connections[addr] = conn
    go h.processReports(conn)
    
    logger.Info("HID connection registered: addr=%s", addr)
    return nil
}

// UnregisterConnection unregisters a HID connection
func (h *HIDService) UnregisterConnection(addr string) error {
    h.mu.Lock()
    defer h.mu.Unlock()
    
    conn, exists := h.connections[addr]
    if !exists {
        return fmt.Errorf("connection not found: %s", addr)
    }
    
    close(conn.StopChan)
    delete(h.connections, addr)
    
    logger.Info("HID connection unregistered: addr=%s", addr)
    return nil
}

// SendReport sends an HID report to a device
func (h *HIDService) SendReport(addr string, report HIDReport) error {
    h.mu.RLock()
    conn, exists := h.connections[addr]
    h.mu.RUnlock()
    
    if !exists {
        return fmt.Errorf("connection not found: %s", addr)
    }
    
    select {
    case conn.ReportChannel <- report:
        conn.LastActivity = time.Now()
        return nil
    default:
        return fmt.Errorf("report channel full")
    }
}

func (h *HIDService) processReports(conn *HIDConnection) {
    for {
        select {
        case report := <-conn.ReportChannel:
            h.handleReport(conn.DeviceAddr, report)
        case <-conn.StopChan:
            return
        }
    }
}

func (h *HIDService) handleReport(addr string, report HIDReport) {
    // Encode and send via BLE
    data := h.encodeReport(report)
    logger.Debug("HID report sent: addr=%s data=%v", addr, data)
}

func (h *HIDService) encodeReport(report HIDReport) []byte {
    buf := make([]byte, 8)
    buf[0] = report.Buttons
    binary.LittleEndian.PutUint16(buf[1:3], uint16(report.X))
    binary.LittleEndian.PutUint16(buf[3:5], uint16(report.Y))
    buf[5] = byte(report.Wheel)
    return buf
}

// GetReportMap returns the HID report descriptor
func (h *HIDService) GetReportMap() []byte {
    return h.reportMap
}

// GetConnections returns all active connections
func (h *HIDService) GetConnections() []*HIDConnection {
    h.mu.RLock()
    defer h.mu.RUnlock()
    
    conns := make([]*HIDConnection, 0, len(h.connections))
    for _, conn := range h.connections {
        conns = append(conns, conn)
    }
    return conns
}