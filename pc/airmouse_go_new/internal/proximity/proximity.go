package proximity

import (
    "fmt"
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
    TxPower       int32
    Confidence    float32
    History       []ProximitySample
}

type ProximitySample struct {
    Timestamp time.Time
    Distance  float32
    RSSI      int32
    IsNear    bool
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
    lastLockTime      time.Time
    lastUnlockTime    time.Time
    callbacks         []func(event ProximityEvent)
    maxHistory        int
    calibrationFactor float32
}

type ProximityUpdate struct {
    DeviceID  string  `json:"device_id"`
    IsNear    bool    `json:"is_near"`
    Distance  float32 `json:"distance"`
    RSSI      int32   `json:"rssi,omitempty"`
    TxPower   int32   `json:"tx_power,omitempty"`
    Timestamp int64   `json:"timestamp"`
}

type ProximityEvent struct {
    Type      string    // "near", "far", "lock", "unlock", "calibrated"
    DeviceID  string
    Distance  float32
    Timestamp time.Time
}

func NewManager() *Manager {
    m := &Manager{
        devices:           make(map[string]*DeviceProximity),
        autoLockEnabled:   true,
        autoUnlockEnabled: true,
        maxHistory:        100,
        calibrationFactor: 1.0,
        callbacks:         make([]func(event ProximityEvent), 0),
    }
    m.detectDesktopEnvironment()
    
    // Start background cleaner
    go m.cleanupLoop()
    
    return m
}

func (m *Manager) detectDesktopEnvironment() {
    switch runtime.GOOS {
    case "windows":
        m.lockCmd = "LockWorkStation"
        m.unlockCmd = ""
        log.Println("Proximity: Windows environment detected")
        
    case "darwin":
        m.lockCmd = "cgsession -suspend"
        m.unlockCmd = ""
        log.Println("Proximity: macOS environment detected")
        
    case "linux":
        m.detectLinuxEnvironment()
        
    default:
        log.Printf("Proximity: Unknown OS: %s", runtime.GOOS)
    }
}

func (m *Manager) detectLinuxEnvironment() {
    // Try different Linux lock commands
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
        {"swaymsg exec 'swaylock'", "", "swaymsg"},
        {"i3lock", "", "i3lock"},
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
    
    // Apply calibration
    distance := update.Distance
    if m.calibrationFactor != 1.0 {
        distance = update.Distance * m.calibrationFactor
    }
    
    dev, exists := m.devices[update.DeviceID]
    if !exists {
        dev = &DeviceProximity{
            ID:            update.DeviceID,
            NearThreshold: 1.5,
            FarThreshold:  3.0,
            History:       make([]ProximitySample, 0, m.maxHistory),
        }
        m.devices[update.DeviceID] = dev
    }
    
    // Apply hysteresis to prevent rapid toggling
    oldNear := dev.IsNear
    if dev.IsNear {
        // Already near - need to go beyond far threshold to change
        dev.IsNear = distance < dev.FarThreshold
    } else {
        // Currently far - need to go within near threshold to change
        dev.IsNear = distance < dev.NearThreshold
    }
    
    dev.Distance = distance
    dev.RSSI = update.RSSI
    dev.TxPower = update.TxPower
    dev.LastUpdate = time.Now()
    dev.Confidence = m.calculateConfidence(dev)
    
    // Add to history
    sample := ProximitySample{
        Timestamp: time.Now(),
        Distance:  distance,
        RSSI:      update.RSSI,
        IsNear:    dev.IsNear,
    }
    dev.History = append(dev.History, sample)
    if len(dev.History) > m.maxHistory {
        dev.History = dev.History[1:]
    }
    
    // Trigger events
    if oldNear != dev.IsNear {
        eventType := "near"
        if !dev.IsNear {
            eventType = "far"
        }
        m.triggerEvent(ProximityEvent{
            Type:      eventType,
            DeviceID:  update.DeviceID,
            Distance:  distance,
            Timestamp: time.Now(),
        })
        
        log.Printf("Proximity: Device %s changed to near=%v (distance=%.2fm, confidence=%.2f)", 
            update.DeviceID, dev.IsNear, distance, dev.Confidence)
        
        go m.applyProximityAction(dev.IsNear)
    }
}

func (m *Manager) calculateConfidence(dev *DeviceProximity) float32 {
    // Calculate confidence based on:
    // 1. How recent the update is
    // 2. Consistency of recent readings
    // 3. Signal strength if available
    
    timeSince := time.Since(dev.LastUpdate)
    if timeSince > 5*time.Second {
        return 0.3
    }
    
    timeConfidence := float32(1.0 - float64(timeSince)/float64(5*time.Second))
    
    // Check consistency of last 5 readings
    consistency := float32(1.0)
    if len(dev.History) > 1 {
        var variations float32
        count := 0
        for i := len(dev.History) - 1; i > 0 && i > len(dev.History)-6; i-- {
            diff := dev.History[i].Distance - dev.History[i-1].Distance
            if diff < 0 {
                diff = -diff
            }
            variations += diff
            count++
        }
        if count > 0 {
            avgVariation := variations / float32(count)
            consistency = 1.0 - (avgVariation / dev.Distance)
            if consistency < 0 {
                consistency = 0
            }
            if consistency > 1 {
                consistency = 1
            }
        }
    }
    
    // RSSI confidence
    rssiConfidence := float32(0.5)
    if dev.RSSI != 0 {
        // RSSI typically ranges from -30 (very close) to -90 (far)
        rssiConfidence = float32(1.0 - (float64(-dev.RSSI)-30)/60)
        if rssiConfidence < 0 {
            rssiConfidence = 0
        }
        if rssiConfidence > 1 {
            rssiConfidence = 1
        }
    }
    
    return (timeConfidence*0.4 + consistency*0.4 + rssiConfidence*0.2)
}

func (m *Manager) applyProximityAction(isNear bool) {
    // Rate limiting - don't lock/unlock too frequently
    now := time.Now()
    
    if isNear && m.autoUnlockEnabled && !m.unlockInProgress {
        if now.Sub(m.lastUnlockTime) > 10*time.Second {
            m.unlockInProgress = true
            defer func() { m.unlockInProgress = false }()
            m.unlockScreen()
            m.lastUnlockTime = now
        }
    } else if !isNear && m.autoLockEnabled && !m.lockInProgress {
        if now.Sub(m.lastLockTime) > 10*time.Second {
            m.lockInProgress = true
            defer func() { m.lockInProgress = false }()
            m.lockScreen()
            m.lastLockTime = now
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

func (m *Manager) Calibrate(distance float32) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if distance > 0 {
        m.calibrationFactor = 1.0 / distance
        log.Printf("Proximity: Calibrated with factor %.2f", m.calibrationFactor)
        
        m.triggerEvent(ProximityEvent{
            Type:      "calibrated",
            Timestamp: time.Now(),
        })
    }
}

func (m *Manager) ResetCalibration() {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    m.calibrationFactor = 1.0
    log.Println("Proximity: Calibration reset")
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

func (m *Manager) cleanupLoop() {
    ticker := time.NewTicker(60 * time.Second)
    defer ticker.Stop()
    
    for range ticker.C {
        m.cleanupInactiveDevices()
    }
}

func (m *Manager) cleanupInactiveDevices() {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    now := time.Now()
    for id, dev := range m.devices {
        if now.Sub(dev.LastUpdate) > 5*time.Minute {
            delete(m.devices, id)
            log.Printf("Proximity: Removed inactive device %s", id)
        }
    }
}

func (m *Manager) GetStatistics() map[string]interface{} {
    m.mu.RLock()
    defer m.mu.RUnlock()
    
    var nearCount, farCount int
    var avgDistance float32
    var totalConfidence float32
    
    for _, dev := range m.devices {
        if dev.IsNear {
            nearCount++
        } else {
            farCount++
        }
        avgDistance += dev.Distance
        totalConfidence += dev.Confidence
    }
    
    deviceCount := len(m.devices)
    if deviceCount > 0 {
        avgDistance /= float32(deviceCount)
    }
    
    return map[string]interface{}{
        "total_devices":      deviceCount,
        "near_devices":       nearCount,
        "far_devices":        farCount,
        "average_distance":   avgDistance,
        "average_confidence": totalConfidence / float32(max(deviceCount, 1)),
        "auto_lock":          m.autoLockEnabled,
        "auto_unlock":        m.autoUnlockEnabled,
        "calibration_factor": m.calibrationFactor,
        "last_lock":          m.lastLockTime,
        "last_unlock":        m.lastUnlockTime,
    }
}

func max(a, b int) int {
    if a > b {
        return a
    }
    return b
}