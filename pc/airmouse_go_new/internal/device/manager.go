package device

import (
    "crypto/sha256"
    "encoding/hex"
    "fmt"
    "sync"
    "time"
)

type DeviceType string

const (
    TypeTCP       DeviceType = "TCP"
    TypeWebSocket DeviceType = "WebSocket"
    TypeUDP       DeviceType = "UDP"
    TypeBluetooth DeviceType = "Bluetooth"
    TypeUSB       DeviceType = "USB"
    TypeSerial    DeviceType = "Serial"
)

type DeviceStatus string

const (
    StatusConnected    DeviceStatus = "connected"
    StatusDisconnected DeviceStatus = "disconnected"
    StatusIdle         DeviceStatus = "idle"
    StatusBlocked      DeviceStatus = "blocked"
)

type DeviceInfo struct {
    ID           string       `json:"id"`
    Name         string       `json:"name"`
    Type         DeviceType   `json:"type"`
    Status       DeviceStatus `json:"status"`
    ConnectedAt  time.Time    `json:"connected_at"`
    LastActive   time.Time    `json:"last_active"`
    BytesSent    int64        `json:"bytes_sent"`
    BytesRecv    int64        `json:"bytes_recv"`
    MessagesSent int64        `json:"messages_sent"`
    MessagesRecv int64        `json:"messages_recv"`
    RSSI         int32        `json:"rssi,omitempty"`
    MACAddress   string       `json:"mac_address,omitempty"`
    IPAddress    string       `json:"ip_address,omitempty"`
    UserAgent    string       `json:"user_agent,omitempty"`
    Version      string       `json:"version,omitempty"`
}

type DeviceEvent struct {
    Type       string // "registered", "unregistered", "updated", "blocked"
    DeviceID   string
    DeviceName string
    Timestamp  time.Time
}

type DeviceManager struct {
    mu         sync.RWMutex
    devices    map[string]*DeviceInfo
    blockedIDs map[string]bool
    callbacks  []func(event DeviceEvent)
    maxDevices int
}

// Simple logger functions to avoid external dependency
func logInfo(msg string, args ...interface{}) {
    fmt.Printf("[INFO] "+msg+"\n", args...)
}

func logDebug(msg string, args ...interface{}) {
    fmt.Printf("[DEBUG] "+msg+"\n", args...)
}

func NewManager() *DeviceManager {
    return &DeviceManager{
        devices:    make(map[string]*DeviceInfo),
        blockedIDs: make(map[string]bool),
        callbacks:  make([]func(DeviceEvent), 0),
        maxDevices: 100,
    }
}

func (m *DeviceManager) SetMaxDevices(max int) {
    m.maxDevices = max
}

func (m *DeviceManager) RegisterDevice(id string, deviceType DeviceType, name string) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    if m.blockedIDs[id] {
        return fmt.Errorf("device is blocked: %s", id)
    }

    if len(m.devices) >= m.maxDevices {
        return fmt.Errorf("max devices reached (%d)", m.maxDevices)
    }

    now := time.Now()
    m.devices[id] = &DeviceInfo{
        ID:          id,
        Name:        name,
        Type:        deviceType,
        Status:      StatusConnected,
        ConnectedAt: now,
        LastActive:  now,
    }

    m.triggerEvent(DeviceEvent{
        Type:       "registered",
        DeviceID:   id,
        DeviceName: name,
        Timestamp:  now,
    })

    logInfo("Device registered: %s (%s)", name, deviceType)
    return nil
}

func (m *DeviceManager) UnregisterDevice(id string) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    device, exists := m.devices[id]
    if !exists {
        return fmt.Errorf("device not found: %s", id)
    }

    delete(m.devices, id)

    m.triggerEvent(DeviceEvent{
        Type:       "unregistered",
        DeviceID:   id,
        DeviceName: device.Name,
        Timestamp:  time.Now(),
    })

    logInfo("Device unregistered: %s (%s)", device.Name, device.Type)
    return nil
}

func (m *DeviceManager) UpdateDeviceName(id string, name string) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    device, exists := m.devices[id]
    if !exists {
        return fmt.Errorf("device not found: %s", id)
    }

    if name != "" {
        device.Name = name
        device.LastActive = time.Now()

        m.triggerEvent(DeviceEvent{
            Type:       "updated",
            DeviceID:   id,
            DeviceName: name,
            Timestamp:  time.Now(),
        })
    }
    return nil
}

func (m *DeviceManager) UpdateDeviceActivity(id string, sent, recv int64) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    device, exists := m.devices[id]
    if !exists {
        return fmt.Errorf("device not found: %s", id)
    }

    device.LastActive = time.Now()
    device.BytesSent += sent
    device.BytesRecv += recv
    device.MessagesSent++
    device.MessagesRecv++

    return nil
}

func (m *DeviceManager) UpdateDeviceStatus(id string, status DeviceStatus) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    device, exists := m.devices[id]
    if !exists {
        return fmt.Errorf("device not found: %s", id)
    }

    device.Status = status
    return nil
}

func (m *DeviceManager) UpdateBLEDevice(addr, name string, rssi int32) {
    m.mu.Lock()
    defer m.mu.Unlock()

    if device, exists := m.devices[addr]; exists {
        device.RSSI = rssi
        device.LastActive = time.Now()
        if name != "" && device.Name == "" {
            device.Name = name
        }
    } else if !m.blockedIDs[addr] && len(m.devices) < m.maxDevices {
        m.devices[addr] = &DeviceInfo{
            ID:          addr,
            Name:        name,
            Type:        TypeBluetooth,
            Status:      StatusConnected,
            ConnectedAt: time.Now(),
            LastActive:  time.Now(),
            RSSI:        rssi,
            MACAddress:  addr,
        }
        logDebug("BLE device discovered: %s (%s)", name, addr)
    }
}

func (m *DeviceManager) BlockDevice(id string) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    m.blockedIDs[id] = true

    if device, exists := m.devices[id]; exists {
        device.Status = StatusBlocked
        m.triggerEvent(DeviceEvent{
            Type:       "blocked",
            DeviceID:   id,
            DeviceName: device.Name,
            Timestamp:  time.Now(),
        })
        logInfo("Device blocked: %s (%s)", device.Name, id)
    }

    return nil
}

func (m *DeviceManager) UnblockDevice(id string) error {
    m.mu.Lock()
    defer m.mu.Unlock()

    delete(m.blockedIDs, id)

    if device, exists := m.devices[id]; exists {
        device.Status = StatusConnected
        logInfo("Device unblocked: %s (%s)", device.Name, id)
    }

    return nil
}

func (m *DeviceManager) IsBlocked(id string) bool {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.blockedIDs[id]
}

func (m *DeviceManager) GetAllDevices() []*DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()

    devices := make([]*DeviceInfo, 0, len(m.devices))
    for _, d := range m.devices {
        devices = append(devices, d)
    }
    return devices
}

func (m *DeviceManager) GetDevice(id string) *DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()

    if device, exists := m.devices[id]; exists {
        // Return a copy to prevent external modification
        copy := *device
        return &copy
    }
    return nil
}

func (m *DeviceManager) GetDeviceByMAC(mac string) *DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()

    for _, device := range m.devices {
        if device.MACAddress == mac {
            copy := *device
            return &copy
        }
    }
    return nil
}

func (m *DeviceManager) GetActiveDevices() []*DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()

    active := make([]*DeviceInfo, 0)
    threshold := 30 * time.Second

    for _, d := range m.devices {
        if time.Since(d.LastActive) < threshold {
            active = append(active, d)
        }
    }
    return active
}

func (m *DeviceManager) GetDeviceCount() int {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return len(m.devices)
}

func (m *DeviceManager) GetBlockedCount() int {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return len(m.blockedIDs)
}

func (m *DeviceManager) GetStatistics() map[string]interface{} {
    m.mu.RLock()
    defer m.mu.RUnlock()

    var totalSent, totalRecv int64
    typeCount := make(map[DeviceType]int)
    statusCount := make(map[DeviceStatus]int)

    for _, d := range m.devices {
        totalSent += d.BytesSent
        totalRecv += d.BytesRecv
        typeCount[d.Type]++
        statusCount[d.Status]++
    }

    return map[string]interface{}{
        "total_devices":     len(m.devices),
        "blocked_devices":   len(m.blockedIDs),
        "by_type":           typeCount,
        "by_status":         statusCount,
        "total_bytes_sent":  totalSent,
        "total_bytes_recv":  totalRecv,
        "max_devices":       m.maxDevices,
    }
}

func (m *DeviceManager) PruneInactive(maxIdle time.Duration) int {
    m.mu.Lock()
    defer m.mu.Unlock()

    removed := 0
    for id, device := range m.devices {
        if time.Since(device.LastActive) > maxIdle {
            delete(m.devices, id)
            removed++
            logDebug("Pruned inactive device: %s", id)
        }
    }
    return removed
}

func (m *DeviceManager) AddEventListener(callback func(event DeviceEvent)) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.callbacks = append(m.callbacks, callback)
}

func (m *DeviceManager) triggerEvent(event DeviceEvent) {
    callbacks := make([]func(DeviceEvent), len(m.callbacks))
    copy(callbacks, m.callbacks)

    for _, cb := range callbacks {
        go cb(event)
    }
}

func (m *DeviceManager) GetDeviceHash(id string) string {
    hash := sha256.Sum256([]byte(id))
    return hex.EncodeToString(hash[:8])
}