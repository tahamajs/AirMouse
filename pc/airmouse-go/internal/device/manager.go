package device

import (
	"sync"
	"time"
)

// DeviceType enumerates the possible connection types.
type DeviceType string

const (
	TypeTCP       DeviceType = "tcp"
	TypeWebSocket DeviceType = "websocket"
	TypeUDP       DeviceType = "udp"
	TypeBluetooth DeviceType = "bluetooth"
)

// DeviceInfo holds information about a connected device.
type DeviceInfo struct {
	ID          string                 `json:"id"`
	Name        string                 `json:"name"`
	Type        DeviceType             `json:"type"`
	ConnectedAt time.Time              `json:"connected_at"`
	LastActive  time.Time              `json:"last_active"`
	BytesSent   int64                  `json:"bytes_sent"`
	BytesRecv   int64                  `json:"bytes_recv"`
	Status      string                 `json:"status"` // "connected", "disconnected", etc.
	Metadata    map[string]interface{} `json:"metadata"`
}

// BLEDevice represents a discovered Bluetooth Low Energy device.
type BLEDevice struct {
	Addr      string `json:"addr"`
	Name      string `json:"name"`
	RSSI      int    `json:"rssi"`
	Connected bool   `json:"connected"`
}

// Manager maintains the registry of all connected devices and discovered BLE devices.
type Manager struct {
	devices    map[string]*DeviceInfo
	bleDevices map[string]*BLEDevice
	mu         sync.RWMutex
	callbacks  []func(*DeviceInfo)
}

// NewManager creates a new device manager.
func NewManager() *Manager {
	return &Manager{
		devices:    make(map[string]*DeviceInfo),
		bleDevices: make(map[string]*BLEDevice),
	}
}

// RegisterDevice adds a new device to the registry.
func (m *Manager) RegisterDevice(id string, devType DeviceType, name string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.devices[id] = &DeviceInfo{
		ID:          id,
		Name:        name,
		Type:        devType,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
		Status:      "connected",
		Metadata:    make(map[string]interface{}),
	}
	m.notifyCallbacks(m.devices[id])
}

// UnregisterDevice removes a device from the registry.
func (m *Manager) UnregisterDevice(id string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if dev, ok := m.devices[id]; ok {
		dev.Status = "disconnected"
		m.notifyCallbacks(dev)
		delete(m.devices, id)
	}
}

// UpdateDeviceName changes the display name of a registered device.
func (m *Manager) UpdateDeviceName(id, name string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if dev, ok := m.devices[id]; ok {
		dev.Name = name
		m.notifyCallbacks(dev)
	}
}

// UpdateBLEDevice adds or updates a discovered BLE device.
func (m *Manager) UpdateBLEDevice(addr, name string, rssi int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if d, ok := m.bleDevices[addr]; ok {
		d.RSSI = rssi
	} else {
		m.bleDevices[addr] = &BLEDevice{
			Addr:      addr,
			Name:      name,
			RSSI:      rssi,
			Connected: false,
		}
	}
}

// GetAllDevices returns a snapshot of all registered devices.
func (m *Manager) GetAllDevices() []*DeviceInfo {
	m.mu.RLock()
	defer m.mu.RUnlock()
	devs := make([]*DeviceInfo, 0, len(m.devices))
	for _, d := range m.devices {
		devs = append(devs, d)
	}
	return devs
}

// GetBLEDevices returns a snapshot of all discovered BLE devices.
func (m *Manager) GetBLEDevices() []*BLEDevice {
	m.mu.RLock()
	defer m.mu.RUnlock()
	devs := make([]*BLEDevice, 0, len(m.bleDevices))
	for _, d := range m.bleDevices {
		devs = append(devs, d)
	}
	return devs
}

// OnDeviceChange registers a callback that is invoked whenever a device changes state.
func (m *Manager) OnDeviceChange(cb func(*DeviceInfo)) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, cb)
}

// notifyCallbacks calls all registered callbacks with the given device.
func (m *Manager) notifyCallbacks(dev *DeviceInfo) {
	for _, cb := range m.callbacks {
		go cb(dev)
	}
}