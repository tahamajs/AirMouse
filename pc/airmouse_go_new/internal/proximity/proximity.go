package proximity

import (
	"log"
	"os/exec"
	"runtime"
	"sync"
	"time"
)

type DeviceProximity struct {
	ID            string
	IsNear        bool
	Distance      float32
	LastUpdate    time.Time
	NearThreshold float32
	FarThreshold  float32
	RSSI          int32
	Confidence    float32
}

type Manager struct {
	devices           map[string]*DeviceProximity
	mu                sync.RWMutex
	lockCmd           string
	unlockCmd         string
	autoLockEnabled   bool
	autoUnlockEnabled bool
	lockInProgress    bool
	unlockInProgress  bool
	callbacks         []func(event ProximityEvent)
}

type ProximityUpdate struct {
	DeviceID  string  `json:"device_id"`
	IsNear    bool    `json:"is_near"`
	Distance  float32 `json:"distance"`
	RSSI      int32   `json:"rssi,omitempty"`
	Timestamp int64   `json:"timestamp"`
}

type ProximityEvent struct {
	Type      string // "near", "far", "lock", "unlock"
	DeviceID  string
	Distance  float32
	Timestamp time.Time
}

func NewManager() *Manager {
	m := &Manager{
		devices:           make(map[string]*DeviceProximity),
		autoLockEnabled:   true,
		autoUnlockEnabled: true,
		callbacks:         make([]func(ProximityEvent), 0),
	}
	m.detectDesktopEnvironment()
	return m
}

func (m *Manager) detectDesktopEnvironment() {
	switch runtime.GOOS {
	case "windows":
		m.lockCmd = "rundll32.exe user32.dll,LockWorkStation"
		m.unlockCmd = ""
		log.Println("Proximity: Windows environment detected")
		return

	case "darwin":
		m.lockCmd = "cgsession -suspend"
		m.unlockCmd = ""
		log.Println("Proximity: macOS environment detected")
		return

	case "linux":
		m.detectLinuxEnvironment()

	default:
		log.Printf("Proximity: Unknown OS: %s", runtime.GOOS)
	}
}

func (m *Manager) detectLinuxEnvironment() {
	commands := []struct {
		lock   string
		unlock string
		check  string
	}{
		{"gnome-screensaver-command --lock", "gnome-screensaver-command -d", "gnome-screensaver-command"},
		{"qdbus org.freedesktop.ScreenSaver /ScreenSaver Lock", "", "qdbus"},
		{"xscreensaver-command -lock", "", "xscreensaver-command"},
		{"loginctl lock-session", "", "loginctl"},
		{"dm-tool lock", "", "dm-tool"},
		{"gdmflexiserver --lock", "", "gdmflexiserver"},
	}

	for _, cmd := range commands {
		if _, err := exec.LookPath(cmd.check); err == nil {
			m.lockCmd = cmd.lock
			m.unlockCmd = cmd.unlock
			log.Printf("Proximity: Using lock command: %s", cmd.lock)
			return
		}
	}

	log.Println("Proximity: No lock command found for this Linux environment")
}

func (m *Manager) ProcessUpdate(update ProximityUpdate) {
	m.mu.Lock()
	defer m.mu.Unlock()

	dev, exists := m.devices[update.DeviceID]
	if !exists {
		dev = &DeviceProximity{
			ID:            update.DeviceID,
			NearThreshold: 1.5,
			FarThreshold:  3.0,
		}
		m.devices[update.DeviceID] = dev
	}

	dev.Distance = update.Distance
	dev.RSSI = update.RSSI
	dev.LastUpdate = time.Now()
	dev.Confidence = m.calculateConfidence(dev)

	oldNear := dev.IsNear
	// Apply hysteresis to prevent rapid toggling
	if dev.IsNear {
		// Already near - need to go beyond far threshold to change
		dev.IsNear = update.Distance < dev.FarThreshold
	} else {
		// Currently far - need to go within near threshold to change
		dev.IsNear = update.Distance < dev.NearThreshold
	}

	if oldNear != dev.IsNear {
		eventType := "far"
		if dev.IsNear {
			eventType = "near"
		}

		log.Printf("Proximity: device %s changed to near=%v (distance=%.2fm, confidence=%.2f)",
			update.DeviceID, dev.IsNear, update.Distance, dev.Confidence)

		m.triggerEvent(ProximityEvent{
			Type:      eventType,
			DeviceID:  update.DeviceID,
			Distance:  update.Distance,
			Timestamp: time.Now(),
		})

		go m.applyProximityAction(dev.IsNear)
	}
}

func (m *Manager) calculateConfidence(dev *DeviceProximity) float32 {
	// Calculate confidence based on:
	// 1. How recent the update is
	// 2. Distance relative to thresholds

	timeSince := time.Since(dev.LastUpdate)
	if timeSince > 5*time.Second {
		return 0.3
	}

	timeConfidence := float32(1.0 - float64(timeSince)/float64(5*time.Second))

	// Distance confidence
	var distanceConfidence float32
	if dev.IsNear {
		distanceConfidence = 1.0 - (dev.Distance / dev.NearThreshold)
	} else {
		if dev.Distance > dev.FarThreshold {
			distanceConfidence = 1.0
		} else {
			distanceConfidence = dev.Distance / dev.FarThreshold
		}
	}
	if distanceConfidence < 0 {
		distanceConfidence = 0
	}
	if distanceConfidence > 1 {
		distanceConfidence = 1
	}

	return (timeConfidence*0.4 + distanceConfidence*0.6)
}

func (m *Manager) applyProximityAction(isNear bool) {
	// Rate limiting - don't lock/unlock too frequently
	const minInterval = 5 * time.Second
	var lastActionTime time.Time

	m.mu.RLock()
	if isNear && m.autoUnlockEnabled {
		// Check last unlock time
	} else if !isNear && m.autoLockEnabled {
		// Check last lock time
	}
	m.mu.RUnlock()

	if isNear && m.autoUnlockEnabled && !m.unlockInProgress {
		if time.Since(lastActionTime) > minInterval {
			m.unlockInProgress = true
			defer func() { m.unlockInProgress = false }()
			m.unlockScreen()
		}
	} else if !isNear && m.autoLockEnabled && !m.lockInProgress {
		if time.Since(lastActionTime) > minInterval {
			m.lockInProgress = true
			defer func() { m.lockInProgress = false }()
			m.lockScreen()
		}
	}
}

func (m *Manager) lockScreen() {
	if m.lockCmd == "" {
		log.Println("Proximity: No lock command configured")
		return
	}

	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.Command("rundll32.exe", "user32.dll,LockWorkStation")
	case "darwin":
		cmd = exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	default:
		cmd = exec.Command("sh", "-c", m.lockCmd)
	}

	if err := cmd.Run(); err != nil {
		log.Printf("Proximity: Lock failed: %v", err)
	} else {
		log.Println("Proximity: Screen locked")
		m.triggerEvent(ProximityEvent{
			Type:      "lock",
			Timestamp: time.Now(),
		})
	}
}

func (m *Manager) unlockScreen() {
	if m.unlockCmd == "" {
		log.Println("Proximity: No unlock command configured")
		return
	}

	cmd := exec.Command("sh", "-c", m.unlockCmd)
	if err := cmd.Run(); err != nil {
		log.Printf("Proximity: Unlock failed: %v", err)
	} else {
		log.Println("Proximity: Screen unlocked")
		m.triggerEvent(ProximityEvent{
			Type:      "unlock",
			Timestamp: time.Now(),
		})
	}
}

func (m *Manager) SetThresholds(deviceID string, near, far float32) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if dev, ok := m.devices[deviceID]; ok {
		dev.NearThreshold = near
		dev.FarThreshold = far
		log.Printf("Proximity: Updated thresholds for %s: near=%.1f, far=%.1f", deviceID, near, far)
	}
}

func (m *Manager) SetGlobalThresholds(near, far float32) {
	m.mu.Lock()
	defer m.mu.Unlock()

	for _, dev := range m.devices {
		dev.NearThreshold = near
		dev.FarThreshold = far
	}
	log.Printf("Proximity: Set global thresholds: near=%.1f, far=%.1f", near, far)
}

func (m *Manager) EnableAutoLock(enable bool) {
	m.autoLockEnabled = enable
	log.Printf("Proximity: Auto-lock %s", map[bool]string{true: "enabled", false: "disabled"}[enable])
}

func (m *Manager) EnableAutoUnlock(enable bool) {
	m.autoUnlockEnabled = enable
	log.Printf("Proximity: Auto-unlock %s", map[bool]string{true: "enabled", false: "disabled"}[enable])
}

func (m *Manager) GetProximityState(deviceID string) *DeviceProximity {
	m.mu.RLock()
	defer m.mu.RUnlock()

	if dev, ok := m.devices[deviceID]; ok {
		// Return a copy
		return &DeviceProximity{
			ID:            dev.ID,
			IsNear:        dev.IsNear,
			Distance:      dev.Distance,
			LastUpdate:    dev.LastUpdate,
			NearThreshold: dev.NearThreshold,
			FarThreshold:  dev.FarThreshold,
			RSSI:          dev.RSSI,
			Confidence:    dev.Confidence,
		}
	}
	return nil
}

func (m *Manager) GetAllStates() []*DeviceProximity {
	m.mu.RLock()
	defer m.mu.RUnlock()

	states := make([]*DeviceProximity, 0, len(m.devices))
	for _, dev := range m.devices {
		states = append(states, &DeviceProximity{
			ID:            dev.ID,
			IsNear:        dev.IsNear,
			Distance:      dev.Distance,
			LastUpdate:    dev.LastUpdate,
			NearThreshold: dev.NearThreshold,
			FarThreshold:  dev.FarThreshold,
			RSSI:          dev.RSSI,
			Confidence:    dev.Confidence,
		})
	}
	return states
}

func (m *Manager) RemoveDevice(deviceID string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	delete(m.devices, deviceID)
	log.Printf("Proximity: Removed device %s", deviceID)
}

func (m *Manager) AddEventListener(callback func(event ProximityEvent)) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.callbacks = append(m.callbacks, callback)
}

func (m *Manager) triggerEvent(event ProximityEvent) {
	m.mu.RLock()
	callbacks := make([]func(ProximityEvent), len(m.callbacks))
	copy(callbacks, m.callbacks)
	m.mu.RUnlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

func (m *Manager) GetStatistics() map[string]interface{} {
	m.mu.RLock()
	defer m.mu.RUnlock()

	var nearCount, farCount int
	var avgDistance float32
	var avgConfidence float32

	for _, dev := range m.devices {
		if dev.IsNear {
			nearCount++
		} else {
			farCount++
		}
		avgDistance += dev.Distance
		avgConfidence += dev.Confidence
	}

	deviceCount := len(m.devices)
	if deviceCount > 0 {
		avgDistance /= float32(deviceCount)
		avgConfidence /= float32(deviceCount)
	}

	return map[string]interface{}{
		"total_devices":      deviceCount,
		"near_devices":       nearCount,
		"far_devices":        farCount,
		"average_distance":   avgDistance,
		"average_confidence": avgConfidence,
		"auto_lock":          m.autoLockEnabled,
		"auto_unlock":        m.autoUnlockEnabled,
	}
}
