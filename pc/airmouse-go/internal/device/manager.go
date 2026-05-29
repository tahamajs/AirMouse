package device

import (
	"sync"
	"time"
)

type DeviceType string

const (
	TypeTCP       DeviceType = "tcp"
	TypeWebSocket DeviceType = "websocket"
	TypeUDP       DeviceType = "udp"
	TypeBluetooth DeviceType = "bluetooth"
	TypeSerial    DeviceType = "serial"
)

type DeviceInfo struct {
	ID          string
	Name        string
	Type        DeviceType
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	Status      string
	Metadata    map[string]interface{}
}

type BLEDevice struct {
	Addr      string
	Name      string
	RSSI      int
	Connected bool
}

type Manager struct {
	devices      map[string]*DeviceInfo
	bleDevices   map[string]*BLEDevice
	mu           sync.RWMutex
	callbacks    []func(*DeviceInfo)
}

func NewManager() *Manager {
	return &Manager{
		devices:    make(map[string]*DeviceInfo),
		bleDevices: make(map[string]*BLEDevice),
	}
}

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

func (m *Manager) UnregisterDevice(id string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	if device, exists := m.devices[id]; exists {
		device.Status = "disconnected"
		m.notifyCallbacks(device)
		delete(m.devices, id)
	}
}

func (m *Manager) UpdateDeviceActivity(id string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	if device, exists := m.devices[id]; exists {
		device.LastActive = time.Now()
	}
}

func (m *Manager) UpdateDeviceName(id string, name string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	if device, exists := m.devices[id]; exists {
		device.Name = name
		m.notifyCallbacks(device)
	}
}

func (m *Manager) UpdateBLEDevice(addr, name string, rssi int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	if device, exists := m.bleDevices[addr]; exists {
		device.RSSI = rssi
	} else {
		m.bleDevices[addr] = &BLEDevice{
			Addr: addr,
			Name: name,
			RSSI: rssi,
		}
	}
}

func (m *Manager) GetDevice(id string) *DeviceInfo {
	m.mu.RLock()
	defer m.mu.RUnlock()
	
	return m.devices[id]
}

func (m *Manager) GetAllDevices() []*DeviceInfo {
	m.mu.RLock()
	defer m.mu.RUnlock()
	
	devices := make([]*DeviceInfo, 0, len(m.devices))
	for _, device := range m.devices {
		devices = append(devices, device)
	}
	return devices
}

func (m *Manager) GetBLEDevices() []*BLEDevice {
	m.mu.RLock()
	defer m.mu.RUnlock()
	
	devices := make([]*BLEDevice, 0, len(m.bleDevices))
	for _, device := range m.bleDevices {
		devices = append(devices, device)
	}
	return devices
}

func (m *Manager) OnDeviceChange(callback func(*DeviceInfo)) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, callback)
}

func (m *Manager) notifyCallbacks(device *DeviceInfo) {
	for _, cb := range m.callbacks {
		go cb(device)
	}
}