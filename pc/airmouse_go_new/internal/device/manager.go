package device

import (
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
)

type DeviceInfo struct {
    ID          string
    Name        string
    Type        DeviceType
    ConnectedAt time.Time
    LastActive  time.Time
    BytesSent   int64
    BytesRecv   int64
}

type Manager struct {
    mu      sync.RWMutex
    devices map[string]*DeviceInfo
}

func NewManager() *Manager {
    return &Manager{
        devices: make(map[string]*DeviceInfo),
    }
}

func (m *Manager) RegisterDevice(id string, deviceType DeviceType, name string) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    m.devices[id] = &DeviceInfo{
        ID:          id,
        Name:        name,
        Type:        deviceType,
        ConnectedAt: time.Now(),
        LastActive:  time.Now(),
    }
}

func (m *Manager) UnregisterDevice(id string) {
    m.mu.Lock()
    defer m.mu.Unlock()
    delete(m.devices, id)
}

func (m *Manager) UpdateDeviceName(id string, name string) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if device, exists := m.devices[id]; exists {
        if name != "" {
            device.Name = name
        }
        device.LastActive = time.Now()
    }
}

func (m *Manager) UpdateDeviceActivity(id string, sent, recv int64) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if device, exists := m.devices[id]; exists {
        device.LastActive = time.Now()
        device.BytesSent += sent
        device.BytesRecv += recv
    }
}

func (m *Manager) GetAllDevices() []*DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()
    
    devices := make([]*DeviceInfo, 0, len(m.devices))
    for _, d := range m.devices {
        devices = append(devices, d)
    }
    return devices
}

func (m *Manager) GetDevice(id string) *DeviceInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()
    return m.devices[id]
}