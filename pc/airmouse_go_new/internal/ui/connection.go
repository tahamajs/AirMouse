package ui

import (
    "fmt"
    "image/color"
    "sync"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"
)

// ConnectionQualityWidget shows Wi-Fi signal strength and latency
type ConnectionQualityWidget struct {
    icon          *canvas.Image
    label         *widget.Label
    statusLabel   *widget.Label
    signalBar     *widget.ProgressBar
    latencyLabel  *widget.Label
    status        string
    rssi          int
    latency       int64
    mu            sync.RWMutex
    lastUpdate    time.Time
    history       []int64
    maxHistory    int
    callbacks     []func(quality int)
}

// NewConnectionQualityWidget creates a widget that displays connection status
func NewConnectionQualityWidget() *ConnectionQualityWidget {
    ic := canvas.NewImageFromResource(nil)
    ic.FillMode = canvas.ImageFillContain
    ic.SetMinSize(fyne.NewSize(32, 32))
    
    lbl := widget.NewLabel("Disconnected")
    lbl.Importance = widget.DangerImportance
    
    statusLbl := widget.NewLabel("No connection")
    statusLbl.Importance = widget.MediumImportance
    
    latencyLbl := widget.NewLabel("Latency: -- ms")
    latencyLbl.Importance = widget.MediumImportance
    
    signalBar := widget.NewProgressBar()
    signalBar.Min = 0
    signalBar.Max = 100
    
    w := &ConnectionQualityWidget{
        icon:         ic,
        label:        lbl,
        statusLabel:  statusLbl,
        latencyLabel: latencyLbl,
        signalBar:    signalBar,
        status:       "unknown",
        history:      make([]int64, 0, 10),
        maxHistory:   10,
        lastUpdate:   time.Now(),
        callbacks:    make([]func(int), 0),
    }
    
    // Start background updater for moving average
    go w.updater()
    
    return w
}

func (w *ConnectionQualityWidget) updater() {
    ticker := time.NewTicker(1 * time.Second)
    defer ticker.Stop()
    
    for range ticker.C {
        w.updateMovingAverage()
    }
}

func (w *ConnectionQualityWidget) updateMovingAverage() {
    w.mu.Lock()
    defer w.mu.Unlock()
    
    if time.Since(w.lastUpdate) > 5*time.Second {
        w.status = "unknown"
        w.rssi = -99
        w.signalBar.SetValue(0)
        return
    }
    
    // Calculate average latency
    if len(w.history) > 0 {
        var sum int64
        for _, l := range w.history {
            sum += l
        }
        avgLatency := sum / int64(len(w.history))
        w.updateDisplay(avgLatency)
    }
}

func (w *ConnectionQualityWidget) updateDisplay(avgLatency int64) {
    var text string
    var statusText string
    var signalValue float64
    quality := 0
    
    if w.rssi > -50 {
        text = "Excellent"
        statusText = "Excellent connection"
        signalValue = 100
        quality = 100
        w.label.Importance = widget.SuccessImportance
        w.statusLabel.Importance = widget.SuccessImportance
        w.latencyLabel.Importance = widget.SuccessImportance
    } else if w.rssi > -60 {
        text = "Good"
        statusText = "Good connection"
        signalValue = 80
        quality = 80
        w.label.Importance = widget.SuccessImportance
        w.statusLabel.Importance = widget.SuccessImportance
        w.latencyLabel.Importance = widget.SuccessImportance
    } else if w.rssi > -70 {
        text = "Fair"
        statusText = "Fair connection"
        signalValue = 60
        quality = 60
        w.label.Importance = widget.WarningImportance
        w.statusLabel.Importance = widget.WarningImportance
        w.latencyLabel.Importance = widget.WarningImportance
    } else if w.rssi > -80 {
        text = "Poor"
        statusText = "Poor connection"
        signalValue = 40
        quality = 40
        w.label.Importance = widget.WarningImportance
        w.statusLabel.Importance = widget.WarningImportance
        w.latencyLabel.Importance = widget.WarningImportance
    } else {
        text = "Very Poor"
        statusText = "Very poor connection"
        signalValue = 20
        quality = 20
        w.label.Importance = widget.DangerImportance
        w.statusLabel.Importance = widget.DangerImportance
        w.latencyLabel.Importance = widget.DangerImportance
    }
    
    w.label.SetText(fmt.Sprintf("Signal: %s (%d dBm)", text, w.rssi))
    w.statusLabel.SetText(statusText)
    w.latencyLabel.SetText(fmt.Sprintf("Latency: %d ms", avgLatency))
    w.signalBar.SetValue(signalValue)
    
    // Trigger callbacks
    for _, cb := range w.callbacks {
        go cb(quality)
    }
}

// SetQuality updates the widget with RSSI value and latency
func (w *ConnectionQualityWidget) SetQuality(rssi int, latencyMs int64) {
    w.mu.Lock()
    defer w.mu.Unlock()
    
    w.rssi = rssi
    w.lastUpdate = time.Now()
    
    // Add to history
    w.history = append(w.history, latencyMs)
    if len(w.history) > w.maxHistory {
        w.history = w.history[1:]
    }
    
    // Calculate average
    var sum int64
    for _, l := range w.history {
        sum += l
    }
    avgLatency := sum / int64(len(w.history))
    
    w.updateDisplay(avgLatency)
    
    // Update icon based on signal strength
    w.updateIcon(rssi)
}

func (w *ConnectionQualityWidget) updateIcon(rssi int) {
    var col color.Color
    if rssi > -50 {
        col = color.RGBA{16, 185, 129, 255} // Green
    } else if rssi > -70 {
        col = color.RGBA{245, 158, 11, 255} // Yellow
    } else {
        col = color.RGBA{239, 68, 68, 255} // Red
    }
    
    // Create a simple colored circle as icon
    circle := canvas.NewCircle(col)
    circle.Resize(fyne.NewSize(24, 24))
    w.icon.Image = circle.Image
    w.icon.Refresh()
}

// GetQuality returns the current quality rating (0-100)
func (w *ConnectionQualityWidget) GetQuality() int {
    w.mu.RLock()
    defer w.mu.RUnlock()
    
    if w.rssi > -50 {
        return 100
    } else if w.rssi > -60 {
        return 80
    } else if w.rssi > -70 {
        return 60
    } else if w.rssi > -80 {
        return 40
    }
    return 20
}

// GetLatency returns the current average latency
func (w *ConnectionQualityWidget) GetLatency() int64 {
    w.mu.RLock()
    defer w.mu.RUnlock()
    
    if len(w.history) == 0 {
        return 0
    }
    
    var sum int64
    for _, l := range w.history {
        sum += l
    }
    return sum / int64(len(w.history))
}

// GetRSSI returns the current RSSI
func (w *ConnectionQualityWidget) GetRSSI() int {
    w.mu.RLock()
    defer w.mu.RUnlock()
    return w.rssi
}

// IsConnected returns true if there's an active connection
func (w *ConnectionQualityWidget) IsConnected() bool {
    w.mu.RLock()
    defer w.mu.RUnlock()
    return time.Since(w.lastUpdate) < 5*time.Second && w.rssi > -90
}

// Reset resets the widget state
func (w *ConnectionQualityWidget) Reset() {
    w.mu.Lock()
    defer w.mu.Unlock()
    
    w.rssi = -99
    w.history = make([]int64, 0, w.maxHistory)
    w.status = "unknown"
    w.lastUpdate = time.Time{}
    w.label.SetText("Disconnected")
    w.label.Importance = widget.DangerImportance
    w.statusLabel.SetText("No connection")
    w.statusLabel.Importance = widget.MediumImportance
    w.latencyLabel.SetText("Latency: -- ms")
    w.signalBar.SetValue(0)
}

// AddEventListener adds a callback for quality changes
func (w *ConnectionQualityWidget) AddEventListener(callback func(quality int)) {
    w.mu.Lock()
    defer w.mu.Unlock()
    w.callbacks = append(w.callbacks, callback)
}

// Widget returns the container for this component
func (w *ConnectionQualityWidget) Widget() fyne.CanvasObject {
    return container.NewVBox(
        container.NewHBox(
            w.icon,
            w.label,
        ),
        w.statusLabel,
        w.signalBar,
        w.latencyLabel,
    )
}

// CompactWidget returns a compact version of the widget
func (w *ConnectionQualityWidget) CompactWidget() fyne.CanvasObject {
    return container.NewHBox(
        w.icon,
        w.label,
        w.latencyLabel,
    )
}

// DetailedWidget returns a detailed version with more information
func (w *ConnectionQualityWidget) DetailedWidget() fyne.CanvasObject {
    return container.NewVBox(
        container.NewHBox(
            w.icon,
            w.label,
        ),
        w.statusLabel,
        w.signalBar,
        w.latencyLabel,
        widget.NewLabel("Last updated: "+time.Now().Format("15:04:05")),
    )
}