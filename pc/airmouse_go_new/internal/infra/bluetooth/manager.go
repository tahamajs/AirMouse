package bluetooth

import (
	"sync"
	"time"

	"airmouse-go/internal/infra/logger"
)

type Manager struct {
	adapter     string
	connections map[string]*Connection
	mu          sync.RWMutex
	running     bool
}

type Connection struct {
	Addr        string
	Name        string
	ConnectedAt time.Time
}

func NewManager(adapter string) *Manager {
	return &Manager{
		adapter:     adapter,
		connections: make(map[string]*Connection),
	}
}

func (m *Manager) Start() error {
	m.running = true
	go m.simulateScan()
	logger.Info("Bluetooth manager started (simulation mode)", "adapter", m.adapter)
	return nil
}

func (m *Manager) simulateScan() {
	for m.running {
		// Simulate discovering a device
		m.mu.Lock()
		m.connections["AA:BB:CC:DD:EE:FF"] = &Connection{
			Addr:        "AA:BB:CC:DD:EE:FF",
			Name:        "AirMouse Simulated",
			ConnectedAt: time.Now(),
		}
		m.mu.Unlock()
		logger.Debug("Discovered BLE device", "addr", "AA:BB:CC:DD:EE:FF")
		time.Sleep(10 * time.Second)
	}
}

func (m *Manager) Stop() {
	m.running = false
	logger.Info("Bluetooth manager stopped")
}

func (m *Manager) GetConnections() []*Connection {
	m.mu.RLock()
	defer m.mu.RUnlock()
	conns := make([]*Connection, 0, len(m.connections))
	for _, c := range m.connections {
		conns = append(conns, c)
	}
	return conns
}