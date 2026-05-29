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
	devices    map[string]*DeviceInfo
	bleDevices map[string]*BLEDevice
	mu         sync.RWMutex
	callbacks  []func(*DeviceInfo)
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
	if dev, ok := m.devices[id]; ok {
		dev.Status = "disconnected"
		m.notifyCallbacks(dev)
		delete(m.devices, id)
	}
}

func (m *Manager) UpdateDeviceName(id, name string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if dev, ok := m.devices[id]; ok {
		dev.Name = name
		m.notifyCallbacks(dev)
	}
}

func (m *Manager) UpdateBLEDevice(addr, name string, rssi int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if d, ok := m.bleDevices[addr]; ok {
		d.RSSI = rssi
	} else {
		m.bleDevices[addr] = &BLEDevice{Addr: addr, Name: name, RSSI: rssi}
	}
}

func (m *Manager) GetAllDevices() []*DeviceInfo {
	m.mu.RLock()
	defer m.mu.RUnlock()
	devs := make([]*DeviceInfo, 0, len(m.devices))
	for _, d := range m.devices {
		devs = append(devs, d)
	}
	return devs
}

func (m *Manager) GetBLEDevices() []*BLEDevice {
	m.mu.RLock()
	defer m.mu.RUnlock()
	devs := make([]*BLEDevice, 0, len(m.bleDevices))
	for _, d := range m.bleDevices {
		devs = append(devs, d)
	}
	return devs
}

func (m *Manager) OnDeviceChange(cb func(*DeviceInfo)) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, cb)
}

func (m *Manager) notifyCallbacks(dev *DeviceInfo) {
	for _, cb := range m.callbacks {
		go cb(dev)
	}
}