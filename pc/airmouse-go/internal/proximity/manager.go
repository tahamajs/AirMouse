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
	    rssiFusion *RSSIFusion
    recentRSSI []float64

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
        recentRSSI:        make([]float64, 0, 10),
    }
    m.detectDesktopEnvironment()
    // Initialise RSSI fusion with default config
    cfg := DefaultRSSIFusionConfig()
    m.rssiFusion = NewRSSIFusion(cfg)
    return m
}


// ProcessRSSIUpdate receives a raw RSSI value and updates the distance estimate.
func (m *Manager) ProcessRSSIUpdate(rssi float64) {
    m.mu.Lock()
    defer m.mu.Unlock()

    // Simple sliding window for additional smoothing (optional)
    m.recentRSSI = append(m.recentRSSI, rssi)
    if len(m.recentRSSI) > 10 {
        m.recentRSSI = m.recentRSSI[1:]
    }
    var avgRSSI float64
    for _, v := range m.recentRSSI {
        avgRSSI += v
    }
    avgRSSI /= float64(len(m.recentRSSI))

    // Use the fusion filter
    distance := m.rssiFusion.ProcessRSSI(avgRSSI)

    // Update the device's distance (assuming there is a single primary device)
    for _, dev := range m.devices {
        dev.Distance = float32(distance)
        dev.LastUpdate = time.Now()

        // Check thresholds and trigger lock/unlock
        oldNear := dev.IsNear
        if dev.IsNear {
            dev.IsNear = distance < float64(dev.FarThreshold)
        } else {
            dev.IsNear = distance < float64(dev.NearThreshold)
        }
        if oldNear != dev.IsNear {
            go m.applyProximityAction(dev.IsNear)
        }
        break // only update the first device (single user scenario)
    }
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

// detectDesktopEnvironment sets the appropriate lock/unlock commands for the OS.
func (m *Manager) detectDesktopEnvironment() {
	// Try GNOME / Unity / Cinnamon
	if _, err := exec.LookPath("gnome-screensaver-command"); err == nil {
		m.lockCmd = "gnome-screensaver-command --lock"
		m.unlockCmd = "gnome-screensaver-command -d"
		return
	}
	// Try KDE (qdbus)
	if _, err := exec.LookPath("qdbus"); err == nil {
		m.lockCmd = `qdbus org.freedesktop.ScreenSaver /ScreenSaver Lock`
		return
	}
	// Try XScreenSaver
	if _, err := exec.LookPath("xscreensaver-command"); err == nil {
		m.lockCmd = "xscreensaver-command -lock"
		return
	}
	// Fallback: loginctl (works on most systemd distros)
	if _, err := exec.LookPath("loginctl"); err == nil {
		m.lockCmd = "loginctl lock-session"
		return
	}
	log.Println("Proximity: No lock command found for this OS")
}

// ProcessUpdate handles an incoming proximity update from the client.
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

// applyProximityAction triggers screen lock/unlock based on state.
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

// SetThresholds updates near/far thresholds for a device.
func (m *Manager) SetThresholds(deviceID string, near, far float32) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if dev, ok := m.devices[deviceID]; ok {
		dev.NearThreshold = near
		dev.FarThreshold = far
	}
}

// EnableAutoLock toggles automatic locking when device goes far.
func (m *Manager) EnableAutoLock(enable bool) {
	m.autoLockEnabled = enable
}

// EnableAutoUnlock toggles automatic unlocking when device returns near.
func (m *Manager) EnableAutoUnlock(enable bool) {
	m.autoUnlockEnabled = enable
}

// GetProximityState returns the current state for a device.
func (m *Manager) GetProximityState(deviceID string) *DeviceProximity {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.devices[deviceID]
}

// The following methods are implemented in OS‑specific files.
func (m *Manager) lockScreen()   {}
func (m *Manager) unlockScreen() {}