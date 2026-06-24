package bluetooth

import (
	"fmt"
	"sync"
	"time"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/infra/logger"
)

// Manager manages Bluetooth connections and HID reports.
type Manager struct {
	adapter     string
	mouse       control.MouseController
	deviceMgr   *device.Manager
	connections map[string]*Connection
	mu          sync.RWMutex
	running     bool
	stopChan    chan struct{}
}

// Connection represents a connected Bluetooth device.
type Connection struct {
	Addr        string
	Name        string
	ConnectedAt time.Time
	LastActive  time.Time
	HIDEnabled  bool
}

// HIDReport represents a mouse HID report.
type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

// NewManager creates a new Bluetooth manager.
func NewManager(adapter string, mouse control.MouseController, deviceMgr *device.Manager) *Manager {
	return &Manager{
		adapter:     adapter,
		mouse:       mouse,
		deviceMgr:   deviceMgr,
		connections: make(map[string]*Connection),
		stopChan:    make(chan struct{}),
	}
}

// Start initialises and starts the Bluetooth manager.
func (m *Manager) Start() error {
	m.mu.Lock()
	if m.running {
		m.mu.Unlock()
		return fmt.Errorf("bluetooth manager already running")
	}
	m.running = true
	m.mu.Unlock()

	logger.Info("Bluetooth manager started: adapter=%s", m.adapter)
	go m.discoveryLoop()
	return nil
}

// discoveryLoop simulates device discovery and HID events.
func (m *Manager) discoveryLoop() {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	// Simulate a discovered device after 2 seconds.
	time.Sleep(2 * time.Second)
	m.addConnection("AA:BB:CC:DD:EE:FF", "AirMouse Device")

	for {
		select {
		case <-ticker.C:
			// Simulate RSSI updates for connected devices.
			m.mu.RLock()
			for addr, conn := range m.connections {
				// Update device manager RSSI.
				if m.deviceMgr != nil {
					rssi := int32(-60 - time.Now().Unix()%30) // pretend RSSI varies
					m.deviceMgr.UpdateBLEDevice(addr, conn.Name, rssi)
				}
			}
			m.mu.RUnlock()

		case <-m.stopChan:
			return
		}
	}
}

// addConnection adds a new device to the manager and device manager.
func (m *Manager) addConnection(addr, name string) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if _, exists := m.connections[addr]; exists {
		return
	}
	conn := &Connection{
		Addr:        addr,
		Name:        name,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
		HIDEnabled:  true,
	}
	m.connections[addr] = conn

	if m.deviceMgr != nil {
		m.deviceMgr.UpdateBLEDevice(addr, name, -60)
	}
	logger.Info("Bluetooth device discovered: %s (%s)", name, addr)
}

// Stop shuts down the Bluetooth manager.
func (m *Manager) Stop() {
	m.mu.Lock()
	if !m.running {
		m.mu.Unlock()
		return
	}
	m.running = false
	m.mu.Unlock()

	select {
	case <-m.stopChan:
	default:
		close(m.stopChan)
	}

	m.mu.Lock()
	m.connections = make(map[string]*Connection)
	m.mu.Unlock()
	logger.Info("Bluetooth manager stopped")
}

// GetStats returns statistics about the manager.
func (m *Manager) GetStats() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()
	conns := make([]string, 0, len(m.connections))
	for addr := range m.connections {
		conns = append(conns, addr)
	}
	return map[string]interface{}{
		"running":        m.running,
		"adapter":        m.adapter,
		"connections":    len(m.connections),
		"device_list":    conns,
	}
}

// SendHIDReport sends a mouse HID report to all connected devices.
func (m *Manager) SendHIDReport(report HIDReport) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	if len(m.connections) == 0 {
		logger.Debug("No Bluetooth connections to send HID report")
		return
	}
	// In a real implementation, this would send the report to each connected device.
	// For simulation, we log and also pass to the mouse controller.
	logger.Debug("HID report sent: buttons=%d x=%d y=%d wheel=%d", report.Buttons, report.X, report.Y, report.Wheel)

	// Simulate mouse movement (optional)
	if m.mouse != nil {
		if report.X != 0 || report.Y != 0 {
			m.mouse.Move(float64(report.X), float64(report.Y))
		}
		if report.Buttons&0x01 != 0 {
			m.mouse.Click("left")
		}
		if report.Buttons&0x02 != 0 {
			m.mouse.Click("right")
		}
		if report.Wheel != 0 {
			m.mouse.Scroll(int(report.Wheel))
		}
	}
}

// GetConnections returns a copy of the connection list.
func (m *Manager) GetConnections() []*Connection {
	m.mu.RLock()
	defer m.mu.RUnlock()
	conns := make([]*Connection, 0, len(m.connections))
	for _, c := range m.connections {
		conns = append(conns, c)
	}
	return conns
}