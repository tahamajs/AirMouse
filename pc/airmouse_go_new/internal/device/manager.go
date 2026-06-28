// Package device manages connected devices and their state.
package device

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sync"
	"time"
)

// DeviceType represents the communication protocol.
type DeviceType string

const (
	TypeTCP       DeviceType = "TCP"
	TypeWebSocket DeviceType = "WebSocket"
	TypeUDP       DeviceType = "UDP"
	TypeBluetooth DeviceType = "Bluetooth"
	TypeUSB       DeviceType = "USB"
	TypeSerial    DeviceType = "Serial"
)

// DeviceStatus represents the connection state.
type DeviceStatus string

const (
	StatusConnected       DeviceStatus = "connected"
	StatusDisconnected    DeviceStatus = "disconnected"
	StatusIdle            DeviceStatus = "idle"
	StatusPendingApproval DeviceStatus = "pending_approval"
	StatusBlocked         DeviceStatus = "blocked"
)

// DeviceInfo holds all information about a connected device.
type DeviceInfo struct {
	ID             string       `json:"id"`
	Fingerprint    string       `json:"fingerprint,omitempty"`
	Name           string       `json:"name"`
	Type           DeviceType   `json:"type"`
	Status         DeviceStatus `json:"status"`
	ConnectedAt    time.Time    `json:"connected_at"`
	LastActive     time.Time    `json:"last_active"`
	BytesSent      int64        `json:"bytes_sent"`
	BytesRecv      int64        `json:"bytes_recv"`
	MessagesSent   int64        `json:"messages_sent"`
	MessagesRecv   int64        `json:"messages_recv"`
	RSSI           int32        `json:"rssi,omitempty"`
	MACAddress     string       `json:"mac_address,omitempty"`
	IPAddress      string       `json:"ip_address,omitempty"`
	UserAgent      string       `json:"user_agent,omitempty"`
	Version        string       `json:"version,omitempty"`
	DeviceModel    string       `json:"device_model,omitempty"`
	AndroidVersion string       `json:"android_version,omitempty"`
	Manufacturer   string       `json:"manufacturer,omitempty"`
	Brand          string       `json:"brand,omitempty"`
	SDKInt         string       `json:"sdk_int,omitempty"`
	Protocol       string       `json:"protocol,omitempty"`
	Transport      string       `json:"transport,omitempty"`
}

// DeviceEvent is emitted when a device changes state.
type DeviceEvent struct {
	Type       string // "registered", "unregistered", "updated", "blocked", "unblocked", "approved", "status_changed"
	DeviceID   string
	DeviceName string
	Timestamp  time.Time
}

// DeviceManager manages all connected devices.
type DeviceManager struct {
	mu          sync.RWMutex
	devices     map[string]*DeviceInfo
	blockedIDs  map[string]bool
	callbacks   []func(event DeviceEvent)
	maxDevices  int
	stopped     bool
	stopChan    chan struct{}
	initialized bool
	storePath   string
}

// Manager is an alias for DeviceManager.
type Manager = DeviceManager

// Logger functions (injected by main).
var (
	logInfoFn  func(msg string, args ...interface{})
	logDebugFn func(msg string, args ...interface{})
)

// SetLogger sets the logger functions.
func SetLogger(infoFn, debugFn func(msg string, args ...interface{})) {
	logInfoFn = infoFn
	logDebugFn = debugFn
}

func logInfo(msg string, args ...interface{}) {
	if logInfoFn != nil {
		logInfoFn(msg, args...)
	} else {
		fmt.Printf("[INFO] "+msg+"\n", args...)
	}
}

func logDebug(msg string, args ...interface{}) {
	if logDebugFn != nil {
		logDebugFn(msg, args...)
	} else {
		fmt.Printf("[DEBUG] "+msg+"\n", args...)
	}
}

// NewManager creates a new device manager.
func NewManager() *DeviceManager {
	storePath := deviceStorePath()
	m := &DeviceManager{
		devices:     make(map[string]*DeviceInfo),
		blockedIDs:  make(map[string]bool),
		callbacks:   make([]func(DeviceEvent), 0),
		maxDevices:  100,
		stopChan:    make(chan struct{}),
		stopped:     false,
		initialized: true,
		storePath:   storePath,
	}
	m.loadPersistedDevices()
	go m.backgroundCleanup()
	return m
}

// deviceStorePath returns the path where device data is persisted.
func deviceStorePath() string {
	if dir, err := os.UserConfigDir(); err == nil && dir != "" {
		return filepath.Join(dir, "airmouse", "saved_devices.json")
	}
	return filepath.Join(os.TempDir(), "airmouse_saved_devices.json")
}

// loadPersistedDevices loads saved devices from disk.
func (m *DeviceManager) loadPersistedDevices() {
	data, err := os.ReadFile(m.storePath)
	if err != nil {
		return
	}
	var devices []*DeviceInfo
	if err := json.Unmarshal(data, &devices); err != nil {
		logDebug("Failed to load persisted devices: %v", err)
		return
	}
	for _, d := range devices {
		if d == nil || d.ID == "" {
			continue
		}
		m.devices[d.ID] = d
	}
}

// persistLocked saves devices to disk (must be called with lock held).
func (m *DeviceManager) persistLocked() {
	if m.storePath == "" {
		return
	}
	if err := os.MkdirAll(filepath.Dir(m.storePath), 0o755); err != nil {
		logDebug("Failed to create device store dir: %v", err)
		return
	}
	devices := make([]*DeviceInfo, 0, len(m.devices))
	for _, d := range m.devices {
		devices = append(devices, d)
	}
	data, err := json.MarshalIndent(devices, "", "  ")
	if err != nil {
		logDebug("Failed to persist devices: %v", err)
		return
	}
	_ = os.WriteFile(m.storePath, data, 0o644)
}

// backgroundCleanup periodically removes inactive devices.
func (m *DeviceManager) backgroundCleanup() {
	ticker := time.NewTicker(5 * time.Minute)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			if m.stopped {
				return
			}
			removed := m.PruneInactive(10 * time.Minute)
			if removed > 0 {
				logInfo("Pruned %d inactive devices", removed)
			}
		case <-m.stopChan:
			return
		}
	}
}

// StableDeviceID generates a stable device ID from parts.
// If all parts are empty, it falls back to a timestamp‑based ID.
func StableDeviceID(parts ...string) string {
	if len(parts) == 0 {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	h := sha256.New()
	for _, p := range parts {
		if p != "" {
			_, _ = h.Write([]byte(p))
			_, _ = h.Write([]byte("|"))
		}
	}
	sum := h.Sum(nil)
	if len(sum) == 0 {
		return fmt.Sprintf("%d", time.Now().UnixNano())
	}
	return hex.EncodeToString(sum)[:16]
}

// RegisterDevice registers a new device.
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
	m.persistLocked()
	go m.triggerEvent(DeviceEvent{
		Type:       "registered",
		DeviceID:   id,
		DeviceName: name,
		Timestamp:  now,
	})
	logInfo("Device registered: %s (%s)", name, deviceType)
	return nil
}

// UpsertDevice creates or updates a device with metadata.
// It returns the updated device info.
func (m *DeviceManager) UpsertDevice(id string, deviceType DeviceType, name string, meta map[string]string) *DeviceInfo {
	if !m.initialized || id == "" {
		return nil
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	now := time.Now()
	device, exists := m.devices[id]
	if !exists {
		device = &DeviceInfo{
			ID:          id,
			Name:        name,
			Type:        deviceType,
			Status:      StatusConnected,
			ConnectedAt: now,
			LastActive:  now,
		}
		m.devices[id] = device
	} else {
		if deviceType != "" {
			device.Type = deviceType
		}
		if name != "" {
			device.Name = name
		}
		device.Status = StatusConnected
		device.LastActive = now
	}
	// Apply metadata
	if v, ok := meta["fingerprint"]; ok && v != "" {
		device.Fingerprint = v
	}
	if v, ok := meta["ip_address"]; ok && v != "" {
		device.IPAddress = v
	}
	if v, ok := meta["mac_address"]; ok && v != "" {
		device.MACAddress = v
	}
	if v, ok := meta["rssi"]; ok && v != "" {
		var rssi int32
		fmt.Sscanf(v, "%d", &rssi)
		device.RSSI = rssi
	}
	if v, ok := meta["user_agent"]; ok && v != "" {
		device.UserAgent = v
	}
	if v, ok := meta["version"]; ok && v != "" {
		device.Version = v
	}
	if v, ok := meta["device_model"]; ok && v != "" {
		device.DeviceModel = v
	}
	if v, ok := meta["android_version"]; ok && v != "" {
		device.AndroidVersion = v
	}
	if v, ok := meta["manufacturer"]; ok && v != "" {
		device.Manufacturer = v
	}
	if v, ok := meta["brand"]; ok && v != "" {
		device.Brand = v
	}
	if v, ok := meta["sdk_int"]; ok && v != "" {
		device.SDKInt = v
	}
	if v, ok := meta["protocol"]; ok && v != "" {
		device.Protocol = v
	}
	if v, ok := meta["transport"]; ok && v != "" {
		device.Transport = v
	}
	m.persistLocked()
	return device
}

// RenameDeviceID merges oldID into newID.
func (m *DeviceManager) RenameDeviceID(oldID, newID string) error {
	if !m.initialized {
		return fmt.Errorf("device manager not initialized")
	}
	if oldID == "" || newID == "" || oldID == newID {
		return nil
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	device, exists := m.devices[oldID]
	if !exists {
		return fmt.Errorf("device not found: %s", oldID)
	}
	// If newID already exists, merge them (keep the newer device).
	if existing, exists := m.devices[newID]; exists {
		existing.LastActive = time.Now()
		existing.Status = StatusConnected
		delete(m.devices, oldID)
		m.persistLocked()
		return nil
	}
	delete(m.devices, oldID)
	device.ID = newID
	m.devices[newID] = device
	m.persistLocked()
	return nil
}

// UnregisterDevice removes a device.
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
	m.persistLocked()
	go m.triggerEvent(DeviceEvent{
		Type:       "unregistered",
		DeviceID:   id,
		DeviceName: device.Name,
		Timestamp:  time.Now(),
	})
	logInfo("Device unregistered: %s (%s)", device.Name, device.Type)
	return nil
}

// UpdateDeviceName changes the device name.
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
	m.persistLocked()
	go m.triggerEvent(DeviceEvent{
		Type:       "updated",
		DeviceID:   id,
		DeviceName: name,
		Timestamp:  time.Now(),
	})
	logInfo("Device renamed: %s (%s)", name, id)
	return nil
}

// UpdateDeviceActivity updates traffic and message counters.
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
	m.persistLocked()
	return nil
}

// UpdateDeviceStatus updates the device status and triggers an event.
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
	if device.Status == status {
		return nil
	}
	device.Status = status
	device.LastActive = time.Now()
	m.persistLocked()
	eventType := "status_changed"
	if status == StatusConnected {
		eventType = "approved"
	}
	go m.triggerEvent(DeviceEvent{
		Type:       eventType,
		DeviceID:   id,
		DeviceName: device.Name,
		Timestamp:  time.Now(),
	})
	logInfo("Device status changed: %s -> %s (%s)", id, status, device.Name)
	return nil
}

// UpdateBLEDevice updates or creates a Bluetooth device entry.
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
		m.persistLocked()
		return
	}
	if m.blockedIDs[addr] || len(m.devices) >= m.maxDevices {
		return
	}
	now := time.Now()
	m.devices[addr] = &DeviceInfo{
		ID:          addr,
		Name:        name,
		Type:        TypeBluetooth,
		Status:      StatusConnected,
		ConnectedAt: now,
		LastActive:  now,
		RSSI:        rssi,
		MACAddress:  addr,
	}
	m.persistLocked()
	go m.triggerEvent(DeviceEvent{
		Type:       "discovered",
		DeviceID:   addr,
		DeviceName: name,
		Timestamp:  now,
	})
}

// BlockDevice blocks a device.
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
		m.persistLocked()
		go m.triggerEvent(DeviceEvent{
			Type:       "blocked",
			DeviceID:   id,
			DeviceName: device.Name,
			Timestamp:  time.Now(),
		})
		logInfo("Device blocked: %s (%s)", device.Name, id)
	}
	return nil
}

// UnblockDevice unblocks a device.
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
		m.persistLocked()
		go m.triggerEvent(DeviceEvent{
			Type:       "unblocked",
			DeviceID:   id,
			DeviceName: device.Name,
			Timestamp:  time.Now(),
		})
		logInfo("Device unblocked: %s (%s)", device.Name, id)
	}
	return nil
}

// ClearAll removes all devices and blocked entries, then persists.
func (m *DeviceManager) ClearAll() {
	if !m.initialized {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.devices = make(map[string]*DeviceInfo)
	m.blockedIDs = make(map[string]bool)
	m.persistLocked()
	logInfo("All devices cleared")
	go m.triggerEvent(DeviceEvent{
		Type:      "cleared",
		Timestamp: time.Now(),
	})
}

// IsBlocked checks if a device is blocked.
func (m *DeviceManager) IsBlocked(id string) bool {
	if !m.initialized || id == "" {
		return false
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.blockedIDs[id]
}

// IsDeviceApproved checks if a device with the given fingerprint is approved.
func (m *DeviceManager) IsDeviceApproved(fingerprint string) bool {
	if !m.initialized || fingerprint == "" {
		return false
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	for _, d := range m.devices {
		if d.Fingerprint == fingerprint {
			return d.Status == StatusConnected
		}
	}
	return false
}

// GetDeviceByFingerprint returns device info by fingerprint.
func (m *DeviceManager) GetDeviceByFingerprint(fingerprint string) *DeviceInfo {
	if !m.initialized || fingerprint == "" {
		return nil
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	for _, d := range m.devices {
		if d.Fingerprint == fingerprint {
			copy := *d
			return &copy
		}
	}
	return nil
}

// GetAllDevices returns a copy of all devices.
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

// GetDevice returns a device by ID.
func (m *DeviceManager) GetDevice(id string) *DeviceInfo {
	if !m.initialized || id == "" {
		return nil
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	if device, exists := m.devices[id]; exists {
		copy := *device
		return &copy
	}
	return nil
}

// GetDeviceByMAC returns a device by MAC address.
func (m *DeviceManager) GetDeviceByMAC(mac string) *DeviceInfo {
	if !m.initialized || mac == "" {
		return nil
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	for _, d := range m.devices {
		if d.MACAddress == mac {
			copy := *d
			return &copy
		}
	}
	return nil
}

// GetActiveDevices returns devices that are connected and recently active.
func (m *DeviceManager) GetActiveDevices() []*DeviceInfo {
	if !m.initialized {
		return []*DeviceInfo{}
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	active := make([]*DeviceInfo, 0)
	threshold := 30 * time.Second
	for _, d := range m.devices {
		if d.Status == StatusConnected && time.Since(d.LastActive) < threshold {
			active = append(active, d)
		}
	}
	return active
}

// GetDeviceCount returns the total number of devices.
func (m *DeviceManager) GetDeviceCount() int {
	if !m.initialized {
		return 0
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return len(m.devices)
}

// GetBlockedCount returns the number of blocked devices.
func (m *DeviceManager) GetBlockedCount() int {
	if !m.initialized {
		return 0
	}
	m.mu.RLock()
	defer m.mu.RUnlock()
	return len(m.blockedIDs)
}

// GetStatistics returns aggregated statistics.
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
		"total_devices":    len(m.devices),
		"blocked_devices":  len(m.blockedIDs),
		"by_type":          typeCount,
		"by_status":        statusCount,
		"total_bytes_sent": totalSent,
		"total_bytes_recv": totalRecv,
		"max_devices":      m.maxDevices,
	}
}

// PruneInactive removes devices that have been idle for longer than maxIdle.
// It returns the number of removed devices and persists changes.
func (m *DeviceManager) PruneInactive(maxIdle time.Duration) int {
	if !m.initialized {
		return 0
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	removed := 0
	now := time.Now()
	for id, d := range m.devices {
		if now.Sub(d.LastActive) > maxIdle {
			delete(m.devices, id)
			removed++
		}
	}
	if removed > 0 {
		m.persistLocked()
	}
	return removed
}

// AddEventListener registers a callback for device events.
func (m *DeviceManager) AddEventListener(callback func(event DeviceEvent)) {
	if !m.initialized || callback == nil {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, callback)
}

// triggerEvent fires all registered callbacks.
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

// Stop gracefully stops the manager.
func (m *DeviceManager) Stop() {
	if !m.initialized {
		return
	}
	m.mu.Lock()
	if m.stopped {
		m.mu.Unlock()
		return
	}
	m.stopped = true
	m.mu.Unlock()
	select {
	case <-m.stopChan:
	default:
		close(m.stopChan)
	}
	logInfo("Device manager stopped")
}

// IsInitialized returns true if the manager is initialized.
func (m *DeviceManager) IsInitialized() bool {
	return m.initialized
}
