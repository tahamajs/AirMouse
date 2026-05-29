package bluetooth

import (
	"encoding/binary"
	"fmt"
	"sync"
	"time"
	
	"github.com/go-ble/ble"
	"github.com/go-ble/ble/examples/lib/dev"
	
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

type BLEState int

const (
	StateUnknown BLEState = iota
	StatePoweredOff
	StatePoweredOn
	StateUnauthorized
	StateUnsupported
)

type BLEConnection struct {
	ID         string
	Name       string
	Device     ble.Device
	Client     *ble.Client
	ConnectedAt time.Time
	LastActive  time.Time
}

type Manager struct {
	adapter    string
	mouse      control.MouseController
	deviceMgr  *device.Manager
	connections map[string]*BLEConnection
	mu         sync.RWMutex
	running    bool
	state      BLEState
}

type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

const (
	HIDReportSize = 5
	HIDReportID   = 0x02
	CHAR_UUID_HID = "00002a4a-0000-1000-8000-00805f9b34fb"
)

func NewManager(adapter string, mouse control.MouseController, deviceMgr *device.Manager) *Manager {
	return &Manager{
		adapter:     adapter,
		mouse:       mouse,
		deviceMgr:   deviceMgr,
		connections: make(map[string]*BLEConnection),
	}
}

func (m *Manager) Start() error {
	d, err := dev.NewDevice(m.adapter)
	if err != nil {
		logger.Error("Failed to create BLE device", "error", err)
		return err
	}
	
	ble.SetDevice(d)
	
	// Start advertising
	adv := ble.AdvPacket{}
	adv.AddFlag(ble.FlagGeneralDiscoverable | ble.FlagBREDRNotSupported)
	adv.AddService(ble.UUID16(0x1812)) // HID Service
	adv.AddService(ble.UUID16(0x180A)) // Device Information
	
	if err := ble.Advertise(context.Background(), adv); err != nil {
		logger.Error("Failed to advertise", "error", err)
		return err
	}
	
	// Start scanning for devices
	go m.scanLoop()
	
	m.running = true
	m.state = StatePoweredOn
	
	logger.Info("Bluetooth manager started", "adapter", m.adapter)
	return nil
}

func (m *Manager) scanLoop() {
	for m.running {
		filter := ble.NewAdvFilter()
		filter.AddService(ble.UUID16(0x1812)) // Look for HID devices
		
		ctx := ble.WithScanHandler(context.Background(), func(a ble.Advertisement) {
			m.handleAdvertisement(a)
		})
		
		ble.Scan(ctx, true, filter)
		time.Sleep(5 * time.Second)
	}
}

func (m *Manager) handleAdvertisement(a ble.Advertisement) {
	addr := a.Addr().String()
	name := a.LocalName()
	
	if name == "" {
		name = addr
	}
	
	logger.Debug("BLE device discovered", "addr", addr, "name", name, "rssi", a.RSSI())
	
	// Update device list
	m.deviceMgr.UpdateBLEDevice(addr, name, int(a.RSSI()))
}

func (m *Manager) Connect(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	if _, exists := m.connections[addr]; exists {
		return fmt.Errorf("already connected")
	}
	
	device, err := ble.Dial(context.Background(), ble.NewAddr(addr))
	if err != nil {
		return err
	}
	
	conn := &BLEConnection{
		ID:          addr,
		Name:        addr,
		Device:      device,
		Client:      device.Client(),
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}
	
	m.connections[addr] = conn
	
	// Register as remote device
	m.deviceMgr.RegisterDevice(addr, "bluetooth", addr)
	
	logger.Info("BLE device connected", "addr", addr)
	
	// Start reading HID reports
	go m.readHIDReports(conn)
	
	return nil
}

func (m *Manager) readHIDReports(conn *BLEConnection) {
	uuid := ble.UUID16(0x2A4A) // Report characteristic
	
	for m.running {
		data, err := conn.Client.ReadCharacteristic(uuid)
		if err != nil {
			logger.Error("Failed to read HID report", "error", err)
			break
		}
		
		conn.LastActive = time.Now()
		m.processHIDReport(data)
	}
}

func (m *Manager) processHIDReport(data []byte) {
	if len(data) < HIDReportSize {
		return
	}
	
	// Parse HID report
	buttons := data[0]
	x := int16(binary.LittleEndian.Uint16(data[1:3]))
	y := int16(binary.LittleEndian.Uint16(data[3:5]))
	
	// Convert to delta movement
	m.mouse.Move(float64(x)/1000.0, float64(y)/1000.0)
	
	// Handle clicks
	if buttons&0x01 != 0 {
		m.mouse.Click("left")
	}
	if buttons&0x02 != 0 {
		m.mouse.Click("right")
	}
	if buttons&0x04 != 0 {
		m.mouse.Click("middle")
	}
}

func (m *Manager) Disconnect(addr string) error {
	m.mu.Lock()
	defer m.mu.Unlock()
	
	conn, exists := m.connections[addr]
	if !exists {
		return fmt.Errorf("not connected")
	}
	
	if err := conn.Client.CancelConnection(); err != nil {
		return err
	}
	
	delete(m.connections, addr)
	m.deviceMgr.UnregisterDevice(addr)
	
	logger.Info("BLE device disconnected", "addr", addr)
	return nil
}

func (m *Manager) Stop() {
	m.running = false
	
	m.mu.Lock()
	for addr, conn := range m.connections {
		conn.Client.CancelConnection()
		m.deviceMgr.UnregisterDevice(addr)
	}
	m.connections = make(map[string]*BLEConnection)
	m.mu.Unlock()
	
	logger.Info("Bluetooth manager stopped")
}

func (m *Manager) GetConnections() []*BLEConnection {
	m.mu.RLock()
	defer m.mu.RUnlock()
	
	conns := make([]*BLEConnection, 0, len(m.connections))
	for _, conn := range m.connections {
		conns = append(conns, conn)
	}
	return conns
}

func (m *Manager) GetState() BLEState {
	return m.state
}

func (m *Manager) GetStats() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()
	
	return map[string]interface{}{
		"connections": len(m.connections),
		"state":       m.state,
	}
}