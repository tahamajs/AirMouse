package bluetooth

import (
	"sync"
	"time"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

type Manager struct {
	adapter     string
	mouse       control.MouseController
	deviceMgr   *device.Manager
	connections map[string]*BLEConnection
	mu          sync.RWMutex
	running     bool
}

type BLEConnection struct {
	Addr        string
	Name        string
	ConnectedAt time.Time
	LastActive  time.Time
}

func NewManager(adapter string, mouse control.MouseController, deviceMgr *device.Manager) *Manager {
	return &Manager{
		adapter:     adapter,
		mouse:       mouse,
		deviceMgr:   deviceMgr,
		connections: make(map[string]*BLEConnection),
	}
}

func (m *Manager) Start() error {
	m.running = true
	go m.simulateScan()
	utils.LogInfo("Bluetooth manager started (simulation)", "adapter", m.adapter)
	return nil
}

func (m *Manager) simulateScan() {
	for m.running {
		m.deviceMgr.UpdateBLEDevice("AA:BB:CC:DD:EE:FF", "AirMouse Simulated", -55)
		time.Sleep(10 * time.Second)
	}
}

func (m *Manager) Stop() {
	m.running = false
	utils.LogInfo("Bluetooth manager stopped")
}

func (m *Manager) GetStats() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return map[string]interface{}{"connections": len(m.connections)}
}
