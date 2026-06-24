package ui

import (
	"fmt"
	"image"
	"image/color"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ConnectionQualityWidget shows Wi‑Fi signal strength and latency.
type ConnectionQualityWidget struct {
	icon         *canvas.Image
	label        *widget.Label
	statusLabel  *widget.Label
	signalBar    *widget.ProgressBar
	latencyLabel *widget.Label
	status       string
	rssi         int
	latency      int64
	mu           sync.RWMutex
	lastUpdate   time.Time
	history      []int64
	maxHistory   int
	callbacks    []func(quality int)
	stopChan     chan struct{}
	initialized  bool
}

// NewConnectionQualityWidget creates a widget that displays connection status.
func NewConnectionQualityWidget() *ConnectionQualityWidget {
	defaultImg := image.NewUniform(color.RGBA{128, 128, 128, 255})

	ic := canvas.NewImageFromImage(defaultImg)
	ic.FillMode = canvas.ImageFillContain
	ic.SetMinSize(fyne.NewSize(32, 32))
	ic.Refresh()

	lbl := widget.NewLabel("Waiting for approval")
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
		callbacks:    make([]func(quality int), 0),
		stopChan:     make(chan struct{}),
		initialized:  true,
	}

	go w.updater()
	return w
}

func (w *ConnectionQualityWidget) updater() {
	if !w.initialized {
		return
	}
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			w.updateMovingAverage()
		case <-w.stopChan:
			return
		}
	}
}

func (w *ConnectionQualityWidget) updateMovingAverage() {
	if !w.initialized {
		return
	}
	w.mu.Lock()
	defer w.mu.Unlock()

	if time.Since(w.lastUpdate) > 5*time.Second {
		w.status = "unknown"
		w.rssi = -99
		w.signalBar.SetValue(0)
		return
	}
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
	if !w.initialized {
		return
	}
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

	// Execute callbacks safely
	for _, cb := range w.callbacks {
		go func(callback func(int)) {
			defer func() {
				if r := recover(); r != nil {
					// Recover from panics in callbacks
				}
			}()
			callback(quality)
		}(cb)
	}
}

// SetQuality updates the widget with a new RSSI and latency value.
func (w *ConnectionQualityWidget) SetQuality(rssi int, latencyMs int64) {
	if !w.initialized {
		return
	}
	w.mu.Lock()
	defer w.mu.Unlock()

	w.rssi = rssi
	w.lastUpdate = time.Now()
	w.history = append(w.history, latencyMs)
	if len(w.history) > w.maxHistory {
		w.history = w.history[1:]
	}
	var sum int64
	for _, l := range w.history {
		sum += l
	}
	avgLatency := sum / int64(len(w.history))
	w.updateDisplay(avgLatency)
	w.updateIcon(rssi)
}

func (w *ConnectionQualityWidget) updateIcon(rssi int) {
	if !w.initialized || w.icon == nil {
		return
	}
	var col color.Color
	if rssi > -50 {
		col = color.RGBA{16, 185, 129, 255} // Green
	} else if rssi > -70 {
		col = color.RGBA{245, 158, 11, 255} // Yellow
	} else {
		col = color.RGBA{239, 68, 68, 255} // Red
	}

	newImg := image.NewUniform(col)
	if newImg != nil {
		w.icon.Image = newImg
		w.icon.Refresh()
	}
}

// GetQuality returns the current quality rating (0‑100).
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

// GetLatency returns the current average latency.
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

// GetRSSI returns the current RSSI value.
func (w *ConnectionQualityWidget) GetRSSI() int {
	w.mu.RLock()
	defer w.mu.RUnlock()
	return w.rssi
}

// IsConnected returns true if there's an active connection.
func (w *ConnectionQualityWidget) IsConnected() bool {
	w.mu.RLock()
	defer w.mu.RUnlock()
	return time.Since(w.lastUpdate) < 5*time.Second && w.rssi > -90
}

// Reset clears the widget state.
func (w *ConnectionQualityWidget) Reset() {
	if !w.initialized {
		return
	}
	w.mu.Lock()
	defer w.mu.Unlock()

	w.rssi = -99
	w.history = make([]int64, 0, w.maxHistory)
	w.status = "unknown"
	w.lastUpdate = time.Time{}
	w.label.SetText("Waiting for approval")
	w.label.Importance = widget.DangerImportance
	w.statusLabel.SetText("No connection")
	w.statusLabel.Importance = widget.MediumImportance
	w.latencyLabel.SetText("Latency: -- ms")
	w.signalBar.SetValue(0)

	defaultImg := image.NewUniform(color.RGBA{128, 128, 128, 255})
	if defaultImg != nil && w.icon != nil {
		w.icon.Image = defaultImg
		w.icon.Refresh()
	}
}

// AddEventListener adds a callback for quality changes.
func (w *ConnectionQualityWidget) AddEventListener(callback func(quality int)) {
	if !w.initialized || callback == nil {
		return
	}
	w.mu.Lock()
	defer w.mu.Unlock()
	w.callbacks = append(w.callbacks, callback)
}

// Stop stops the background updater goroutine.
func (w *ConnectionQualityWidget) Stop() {
	if !w.initialized {
		return
	}
	select {
	case <-w.stopChan:
		// Already stopped
	default:
		close(w.stopChan)
	}
}

// Widget returns the full‑size widget container.
func (w *ConnectionQualityWidget) Widget() fyne.CanvasObject {
	if !w.initialized {
		return widget.NewLabel("Connection widget not initialized")
	}
	return container.NewVBox(
		container.NewHBox(w.icon, w.label),
		w.statusLabel,
		w.signalBar,
		w.latencyLabel,
	)
}

// CompactWidget returns a compact version of the widget.
func (w *ConnectionQualityWidget) CompactWidget() fyne.CanvasObject {
	if !w.initialized {
		return widget.NewLabel("Connection widget not initialized")
	}
	return container.NewHBox(w.icon, w.label, w.latencyLabel)
}

// IsInitialized returns true if the widget is properly initialized.
func (w *ConnectionQualityWidget) IsInitialized() bool {
	return w.initialized
}