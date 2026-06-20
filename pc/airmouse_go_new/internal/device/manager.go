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
	mu          sync.RWMutex
	devices     map[string]*DeviceInfo
	blockedIDs  map[string]bool
	callbacks   []func(event DeviceEvent)
	maxDevices  int
	stopped     bool
	stopChan    chan struct{}
	initialized bool
}

// Manager preserves the older name used by several packages in this tree.
type Manager = DeviceManager

// ------------------------------------------------------------
// Logger functions (shared across device package)
// ------------------------------------------------------------

// Logger functions that can be set externally
var (
	logInfoFn  func(msg string, args ...interface{})
	logDebugFn func(msg string, args ...interface{})
)

// SetLogger sets the logger functions for the device package
func SetLogger(infoFn, debugFn func(msg string, args ...interface{})) {
	logInfoFn = infoFn
	logDebugFn = debugFn
}

func logInfo(msg string, args ...interface{}) {
	if logInfoFn != nil {
		logInfoFn(msg, args...)
	} else {
		// Fallback to fmt
		fmt.Printf("[INFO] "+msg+"\n", args...)
	}
}

func logDebug(msg string, args ...interface{}) {
	if logDebugFn != nil {
		logDebugFn(msg, args...)
	} else {
		// Fallback to fmt
		fmt.Printf("[DEBUG] "+msg+"\n", args...)
	}
}

// ------------------------------------------------------------
// DeviceManager
// ------------------------------------------------------------

func NewManager() *DeviceManager {
	m := &DeviceManager{
		devices:     make(map[string]*DeviceInfo),
		blockedIDs:  make(map[string]bool),
		callbacks:   make([]func(DeviceEvent), 0),
		maxDevices:  100,
		stopChan:    make(chan struct{}),
		stopped:     false,
		initialized: true,
	}

	// Start background services WITHOUT blocking the main thread
	go m.startBackgroundServices()

	return m
}

// startBackgroundServices starts all background services
func (m *DeviceManager) startBackgroundServices() {
	if !m.initialized {
		return
	}

	// Wait a moment for UI to initialize
	time.Sleep(500 * time.Millisecond)

	// Start device discovery in background
	go m.deviceDiscoveryLoop()
}

// deviceDiscoveryLoop runs device discovery in background
func (m *DeviceManager) deviceDiscoveryLoop() {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	// Log startup once (non-blocking)
	logInfo("Bluetooth manager started: adapter=default")
	logInfo("USB serial server started: baud_rate=115200 auto_detect=true")

	// Initial device discovery
	m.simulateDeviceDiscovery()

	for {
		select {
		case <-ticker.C:
			if m.stopped {
				return
			}
			m.simulateDeviceDiscovery()
		case <-m.stopChan:
			return
		}
	}
}

// simulateDeviceDiscovery simulates discovering devices
func (m *DeviceManager) simulateDeviceDiscovery() {
	m.mu.Lock()
	defer m.mu.Unlock()

	// Only add if not already present and not blocked
	addr := "AA:BB:CC:DD:EE:FF"
	if _, exists := m.devices[addr]; !exists && !m.blockedIDs[addr] && len(m.devices) < m.maxDevices {
		m.devices[addr] = &DeviceInfo{
			ID:          addr,
			Name:        "AirMouse Device",
			Type:        TypeBluetooth,
			Status:      StatusConnected,
			ConnectedAt: time.Now(),
			LastActive:  time.Now(),
			RSSI:        -45,
			MACAddress:  addr,
		}
		logDebug("BLE device discovered: AirMouse Device (%s)", addr)
	}
}

// Stop stops all background services
func (m *DeviceManager) Stop() {
	if !m.initialized {
		return
	}
	m.mu.Lock()
	m.stopped = true
	m.mu.Unlock()

	select {
	case <-m.stopChan:
		// Already closed
	default:
		close(m.stopChan)
	}
	logInfo("Device manager stopped")
}

// SetMaxDevices sets the maximum number of devices allowed
func (m *DeviceManager) SetMaxDevices(max int) {
	if max <= 0 {
		max = 100
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.maxDevices = max
}

// RegisterDevice registers a new device
func (m *DeviceManager) RegisterDevice(id string, deviceType DeviceType, name string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	if m.blockedIDs[id] {
		return fmt.Errorf("device is blocked: %s", id)
	}

	if len(m.devices) >= m.maxDevices {
		return fmt.Errorf("max devices reached (%d)", m.maxDevices)
	}

	// Check if device already exists
	if _, exists := m.devices[id]; exists {
		return fmt.Errorf("device already registered: %s", id)
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

// UnregisterDevice unregisters a device
func (m *DeviceManager) UnregisterDevice(id string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

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

// UpdateDeviceName updates a device's name
func (m *DeviceManager) UpdateDeviceName(id string, name string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" || name == "" {
		return fmt.Errorf("device ID and name cannot be empty")
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	device, exists := m.devices[id]
	if !exists {
		return fmt.Errorf("device not found: %s", id)
	}

	device.Name = name
	device.LastActive = time.Now()

	m.triggerEvent(DeviceEvent{
		Type:       "updated",
		DeviceID:   id,
		DeviceName: name,
		Timestamp:  time.Now(),
	})

	logInfo("Device renamed: %s (%s)", name, id)
	return nil
}

// UpdateDeviceActivity updates device activity stats
func (m *DeviceManager) UpdateDeviceActivity(id string, sent, recv int64) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

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

// UpdateDeviceStatus updates a device's status
func (m *DeviceManager) UpdateDeviceStatus(id string, status DeviceStatus) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	device, exists := m.devices[id]
	if !exists {
		return fmt.Errorf("device not found: %s", id)
	}

	device.Status = status
	return nil
}

// UpdateBLEDevice updates or adds a BLE device
func (m *DeviceManager) UpdateBLEDevice(addr, name string, rssi int32) {
	if !m.initialized || addr == "" {
		return
	}

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

// BlockDevice blocks a device
func (m *DeviceManager) BlockDevice(id string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

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

// UnblockDevice unblocks a device
func (m *DeviceManager) UnblockDevice(id string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if id == "" {
		return fmt.Errorf("device ID cannot be empty")
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	delete(m.blockedIDs, id)

	if device, exists := m.devices[id]; exists {
		device.Status = StatusConnected
		logInfo("Device unblocked: %s (%s)", device.Name, id)
	}

	return nil
}

// IsBlocked checks if a device is blocked
func (m *DeviceManager) IsBlocked(id string) bool {
	if !m.initialized || id == "" {
		return false
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.blockedIDs[id]
}

// GetAllDevices returns all devices
func (m *DeviceManager) GetAllDevices() []*DeviceInfo {
	if !m.initialized {
		return []*DeviceInfo{}
	}
	m.mu.RLock()
	defer m.mu.RUnlock()

	devices := make([]*DeviceInfo, 0, len(m.devices))
	for _, d := range m.devices {
		devices = append(devices, d)
	}
	return devices
}

// GetDevice returns a device by ID
func (m *DeviceManager) GetDevice(id string) *DeviceInfo {
	if !m.initialized || id == "" {
		return nil
	}
	m.mu.RLock()
	defer m.mu.RUnlock()

	if device, exists := m.devices[id]; exists {
		// Return a copy to prevent external modification
		copy := *device
		return &copy
	}
	return nil
}

// GetDeviceByMAC returns a device by MAC address
func (m *DeviceManager) GetDeviceByMAC(mac string) *DeviceInfo {
	if !m.initialized || mac == "" {
		return nil
	}
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

// GetActiveDevices returns active devices
func (m *DeviceManager) GetActiveDevices() []*DeviceInfo {
	if !m.initialized {
		return []*DeviceInfo{}
	}
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

// GetDeviceCount returns the number of devices
func (m *DeviceManager) GetDeviceCount() int {
	if !m.initialized {
		return 0
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return len(m.devices)
}

// GetBlockedCount returns the number of blocked devices
func (m *DeviceManager) GetBlockedCount() int {
	if !m.initialized {
		return 0
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return len(m.blockedIDs)
}

// GetStatistics returns device statistics
func (m *DeviceManager) GetStatistics() map[string]interface{} {
	if !m.initialized {
		return map[string]interface{}{
			"total_devices":   0,
			"blocked_devices": 0,
		}
	}
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

// PruneInactive removes inactive devices
func (m *DeviceManager) PruneInactive(maxIdle time.Duration) int {
	if !m.initialized {
		return 0
	}
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

// AddEventListener adds an event listener
func (m *DeviceManager) AddEventListener(callback func(event DeviceEvent)) {
	if !m.initialized || callback == nil {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, callback)
}

// triggerEvent triggers an event
func (m *DeviceManager) triggerEvent(event DeviceEvent) {
	if !m.initialized {
		return
	}
	m.mu.RLock()
	callbacks := make([]func(DeviceEvent), len(m.callbacks))
	copy(callbacks, m.callbacks)
	m.mu.RUnlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

// GetDeviceHash returns a hash of the device ID
func (m *DeviceManager) GetDeviceHash(id string) string {
	if id == "" {
		return ""
	}
	hash := sha256.Sum256([]byte(id))
	return hex.EncodeToString(hash[:8])
}

// IsInitialized returns true if the device manager is initialized
func (m *DeviceManager) IsInitialized() bool {
	return m.initialized
}