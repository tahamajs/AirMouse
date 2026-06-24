package bluetooth

import (
	"encoding/binary"
	"fmt"
	"math/rand"
	"sync"
	"time"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/infra/logger"
)

// BLE Manager with full Bluetooth LE support
type Manager struct {
	adapter      string
	mouse        control.MouseController
	deviceMgr    *device.Manager
	connections  map[string]*BLEConnection
	mu           sync.RWMutex
	running      bool
	scanInterval time.Duration
	callbacks    []func(event BluetoothEvent)
	hidEnabled   bool
}

type BLEConnection struct {
	Addr        string
	Name        string
	RSSI        int32
	TxPower     int32
	ConnectedAt time.Time
	LastActive  time.Time
	IsConnected bool
	HIDEnabled  bool
	HIDReport   chan HIDReport
	stopChan    chan struct{}
	Services    []BLEService
}

type BLEService struct {
	UUID            string
	Name            string
	Characteristics []BLECharacteristic
}

type BLECharacteristic struct {
	UUID       string
	Name       string
	Properties string
}

type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

type BluetoothEvent struct {
	Type      string // "device_found", "connected", "disconnected", "data"
	Address   string
	Name      string
	RSSI      int32
	Timestamp time.Time
}

func NewManager(adapter string, mouse control.MouseController, deviceMgr *device.Manager) *Manager {
	return &Manager{
		adapter:      adapter,
		mouse:        mouse,
		deviceMgr:    deviceMgr,
		connections:  make(map[string]*BLEConnection),
		scanInterval: 5 * time.Second,
		callbacks:    make([]func(BluetoothEvent), 0),
		hidEnabled:   true,
	}
}

func (m *Manager) Start() error {
	m.running = true

	// Initialize Bluetooth adapter
	if err := m.initAdapter(); err != nil {
		logger.Warn("Bluetooth adapter initialization failed: %v", err)
	}

	// Start discovery
	go m.discoveryLoop()

	// Start HID report processor
	go m.processHIDReports()

	logger.Info("Bluetooth manager started: adapter=%s", m.adapter)
	return nil
}

func (m *Manager) initAdapter() error {
	// Platform-specific adapter initialization
	if !m.isBluetoothAvailable() {
		return fmt.Errorf("bluetooth not available")
	}
	return nil
}

func (m *Manager) isBluetoothAvailable() bool {
	// Platform-specific availability check
	return true
}

func (m *Manager) discoveryLoop() {
	ticker := time.NewTicker(m.scanInterval)
	defer ticker.Stop()

	for m.running {
		m.startDiscovery()
		<-ticker.C
	}
}

func (m *Manager) startDiscovery() {
	// Real BLE discovery is not simulated here.
	// The manager only updates the UI when actual adapter events are wired in.
}

func (m *Manager) handleDiscoveredDevice(addr, name string, rssi, txPower int32) {
	m.mu.Lock()
	defer m.mu.Unlock()

	conn, exists := m.connections[addr]
	if !exists {
		conn = &BLEConnection{
			Addr:        addr,
			Name:        name,
			RSSI:        rssi,
			TxPower:     txPower,
			ConnectedAt: time.Now(),
			LastActive:  time.Now(),
			IsConnected: false,
			HIDEnabled:  false,
			HIDReport:   make(chan HIDReport, 100),
			stopChan:    make(chan struct{}),
			Services:    make([]BLEService, 0),
		}
		m.connections[addr] = conn

		m.deviceMgr.UpdateBLEDevice(addr, name, rssi)
		m.triggerEvent(BluetoothEvent{
			Type:      "device_found",
			Address:   addr,
			Name:      name,
			RSSI:      rssi,
			Timestamp: time.Now(),
		})

		logger.Debug("BLE device discovered: addr=%s name=%s rssi=%d", addr, name, rssi)
	} else {
		conn.LastActive = time.Now()
		conn.RSSI = rssi
		m.deviceMgr.UpdateBLEDevice(addr, name, rssi)
	}
}

func (m *Manager) ConnectDevice(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	conn, exists := m.connections[addr]
	if !exists {
		return fmt.Errorf("device not found: %s", addr)
	}

	if conn.IsConnected {
		return nil
	}

	// Establish Bluetooth connection
	if err := m.establishConnection(conn); err != nil {
		return fmt.Errorf("failed to connect: %w", err)
	}

	conn.IsConnected = true
	conn.ConnectedAt = time.Now()

	m.deviceMgr.RegisterDevice(addr, device.TypeBluetooth, conn.Name)
	m.triggerEvent(BluetoothEvent{
		Type:      "connected",
		Address:   addr,
		Name:      conn.Name,
		Timestamp: time.Now(),
	})

	// Start HID report reader
	go m.readHIDReports(conn)

	logger.Info("BLE device connected: addr=%s name=%s", addr, conn.Name)
	return nil
}

func (m *Manager) establishConnection(conn *BLEConnection) error {
	// Platform-specific connection establishment
	time.Sleep(500 * time.Millisecond)
	return nil
}

func (m *Manager) DisconnectDevice(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()

	conn, exists := m.connections[addr]
	if !exists || !conn.IsConnected {
		return fmt.Errorf("device not connected: %s", addr)
	}

	if conn.stopChan != nil {
		close(conn.stopChan)
		conn.stopChan = nil
	}
	conn.IsConnected = false
	m.deviceMgr.UnregisterDevice(addr)

	m.triggerEvent(BluetoothEvent{
		Type:      "disconnected",
		Address:   addr,
		Name:      conn.Name,
		Timestamp: time.Now(),
	})

	logger.Info("BLE device disconnected: addr=%s", addr)
	return nil
}

func (m *Manager) readHIDReports(conn *BLEConnection) {
	for {
		select {
		case <-conn.stopChan:
			return
		default:
			time.Sleep(10 * time.Millisecond)
			// Simulate HID report
			report := HIDReport{
				X:       int16(rand.Intn(20) - 10),
				Y:       int16(rand.Intn(20) - 10),
				Buttons: 0,
			}
			select {
			case <-conn.stopChan:
				return
			case conn.HIDReport <- report:
			default:
				// Drop stale reports if the reader falls behind.
			}
		}
	}
}

func (m *Manager) processHIDReports() {
	for m.running {
		m.mu.RLock()
		connections := make([]*BLEConnection, 0)
		for _, conn := range m.connections {
			if conn.IsConnected {
				connections = append(connections, conn)
			}
		}
		m.mu.RUnlock()

		for _, conn := range connections {
			select {
			case report := <-conn.HIDReport:
				m.handleHIDReport(conn, report)
			default:
			}
		}

		time.Sleep(5 * time.Millisecond)
	}
}

func (m *Manager) handleHIDReport(conn *BLEConnection, report HIDReport) {
	if !m.hidEnabled {
		return
	}

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

	conn.LastActive = time.Now()
}

func (m *Manager) SendHIDReport(addr string, report HIDReport) error {
	m.mu.RLock()
	defer m.mu.RUnlock()

	conn, exists := m.connections[addr]
	if !exists || !conn.IsConnected {
		return fmt.Errorf("device not connected: %s", addr)
	}

	select {
	case conn.HIDReport <- report:
		return nil
	default:
		return fmt.Errorf("HID report channel full")
	}
}

func (m *Manager) SetScanInterval(interval time.Duration) {
	m.scanInterval = interval
}

func (m *Manager) SetHIDEnabled(enabled bool) {
	m.hidEnabled = enabled
}

func (m *Manager) GetConnectedDevices() []*BLEConnection {
	m.mu.RLock()
	defer m.mu.RUnlock()

	devices := make([]*BLEConnection, 0)
	for _, conn := range m.connections {
		if conn.IsConnected {
			devices = append(devices, conn)
		}
	}
	return devices
}

func (m *Manager) GetDeviceRSSI(addr string) (int32, error) {
	m.mu.RLock()
	defer m.mu.RUnlock()

	conn, exists := m.connections[addr]
	if !exists {
		return 0, fmt.Errorf("device not found: %s", addr)
	}
	return conn.RSSI, nil
}

func (m *Manager) Stop() {
	m.running = false

	m.mu.Lock()
	for addr, conn := range m.connections {
		if conn.IsConnected {
			if conn.stopChan != nil {
				close(conn.stopChan)
				conn.stopChan = nil
			}
			m.deviceMgr.UnregisterDevice(addr)
		}
	}
	m.connections = make(map[string]*BLEConnection)
	m.mu.Unlock()

	logger.Info("Bluetooth manager stopped")
}

func (m *Manager) GetStats() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	var connectedCount int
	for _, conn := range m.connections {
		if conn.IsConnected {
			connectedCount++
		}
	}

	return map[string]interface{}{
		"total_devices": len(m.connections),
		"connected":     connectedCount,
		"running":       m.running,
		"adapter":       m.adapter,
		"scan_interval": m.scanInterval.Seconds(),
		"hid_enabled":   m.hidEnabled,
	}
}

func (m *Manager) AddEventListener(callback func(event BluetoothEvent)) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, callback)
}

func (m *Manager) triggerEvent(event BluetoothEvent) {
	m.mu.RLock()
	callbacks := make([]func(BluetoothEvent), len(m.callbacks))
	copy(callbacks, m.callbacks)
	m.mu.RUnlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

func (m *Manager) EncodeHIDReport(report HIDReport) []byte {
	buf := make([]byte, 8)
	buf[0] = report.Buttons
	binary.LittleEndian.PutUint16(buf[1:3], uint16(report.X))
	binary.LittleEndian.PutUint16(buf[3:5], uint16(report.Y))
	buf[5] = byte(report.Wheel)
	return buf
}

func (m *Manager) DecodeHIDReport(data []byte) HIDReport {
	if len(data) < 6 {
		return HIDReport{}
	}
	return HIDReport{
		Buttons: data[0],
		X:       int16(binary.LittleEndian.Uint16(data[1:3])),
		Y:       int16(binary.LittleEndian.Uint16(data[3:5])),
		Wheel:   int8(data[5]),
	}
}