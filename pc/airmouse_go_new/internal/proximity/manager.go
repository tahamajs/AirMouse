package proximity

import (
	"log"
	"os/exec"
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
}

type ProximityUpdate struct {
	DeviceID  string  `json:"device_id"`
	IsNear    bool    `json:"is_near"`
	Distance  float32 `json:"distance"`
	Timestamp int64   `json:"timestamp"`
}

func NewManager() *Manager {
	m := &Manager{
		devices:           make(map[string]*DeviceProximity),
		autoLockEnabled:   true,
		autoUnlockEnabled: true,
	}
	m.detectDesktopEnvironment()
	return m
}

func (m *Manager) detectDesktopEnvironment() {
	if _, err := exec.LookPath("gnome-screensaver-command"); err == nil {
		m.lockCmd = "gnome-screensaver-command --lock"
		m.unlockCmd = "gnome-screensaver-command -d"
		return
	}
	if _, err := exec.LookPath("qdbus"); err == nil {
		m.lockCmd = `qdbus org.freedesktop.ScreenSaver /ScreenSaver Lock`
		return
	}
	if _, err := exec.LookPath("xscreensaver-command"); err == nil {
		m.lockCmd = "xscreensaver-command -lock"
		return
	}
	if _, err := exec.LookPath("loginctl"); err == nil {
		m.lockCmd = "loginctl lock-session"
		return
	}
	log.Println("Proximity: No lock command found for this OS")
}

func (m *Manager) ProcessUpdate(update ProximityUpdate) {
	m.mu.Lock()
	defer m.mu.Unlock()
	dev, exists := m.devices[update.DeviceID]
	if !exists {
		dev = &DeviceProximity{
			ID:            update.DeviceID,
			NearThreshold: 2.0,
			FarThreshold:  4.0,
		}
		m.devices[update.DeviceID] = dev
	}
	dev.Distance = update.Distance
	dev.LastUpdate = time.Now()
	oldNear := dev.IsNear
	if dev.IsNear {
		dev.IsNear = update.Distance < dev.FarThreshold
	} else {
		dev.IsNear = update.Distance < dev.NearThreshold
	}
	if oldNear != dev.IsNear {
		log.Printf("Proximity: device %s changed to near=%v (distance=%.2fm)", update.DeviceID, dev.IsNear, update.Distance)
		go m.applyProximityAction(dev.IsNear)
	}
}

func (m *Manager) applyProximityAction(isNear bool) {
	if isNear && m.autoUnlockEnabled && !m.unlockInProgress {
		m.unlockInProgress = true
		defer func() { m.unlockInProgress = false }()
		m.unlockScreen()
	} else if !isNear && m.autoLockEnabled && !m.lockInProgress {
		m.lockInProgress = true
		defer func() { m.lockInProgress = false }()
		m.lockScreen()
	}
}

func (m *Manager) lockScreen() {
	if m.lockCmd == "" {
		return
	}
	if err := exec.Command("sh", "-c", m.lockCmd).Run(); err != nil {
		log.Printf("Proximity: lock failed: %v", err)
	} else {
		log.Println("Proximity: screen locked")
	}
}

func (m *Manager) unlockScreen() {
	if m.unlockCmd == "" {
		return
	}
	if err := exec.Command("sh", "-c", m.unlockCmd).Run(); err != nil {
		log.Printf("Proximity: unlock failed: %v", err)
	} else {
		log.Println("Proximity: screen unlocked")
	}
}

func (m *Manager) SetThresholds(deviceID string, near, far float32) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if dev, ok := m.devices[deviceID]; ok {
		dev.NearThreshold = near
		dev.FarThreshold = far
	}
}

func (m *Manager) EnableAutoLock(enable bool) {
	m.autoLockEnabled = enable
}

func (m *Manager) EnableAutoUnlock(enable bool) {
	m.autoUnlockEnabled = enable
}

func (m *Manager) GetProximityState(deviceID string) *DeviceProximity {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.devices[deviceID]
}

// OS‑specific overrides (windows.go, darwin.go, linux.go) are provided earlier.