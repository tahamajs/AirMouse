package ui

import (
	"fmt"
	"strconv"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
)

// ProximityTab provides a complete UI for Bluetooth proximity lock/unlock.
type ProximityTab struct {
	enableCheck     *widget.Check
	serviceRunning  bool
	nearSlider      *widget.Slider
	farSlider       *widget.Slider
	nearEntry       *widget.Entry
	farEntry        *widget.Entry
	calibrateBtn    *widget.Button
	statusLabel     *widget.Label
	distanceLabel   *widget.Label
	lastDistance    float64
	stopUpdate      chan struct{}
	cfg             *config.Config
}

// NewProximityTab creates a fully featured tab for proximity settings.
func NewProximityTab() fyne.CanvasObject {
	tab := &ProximityTab{
		stopUpdate: make(chan struct{}),
		cfg:        config.Get(),
	}

	// Enable/disable service
	tab.enableCheck = widget.NewCheck("Enable Proximity Lock/Unlock", func(enabled bool) {
		if enabled {
			tab.startProximityService()
		} else {
			tab.stopProximityService()
		}
	})
	tab.enableCheck.SetChecked(tab.cfg.EnableProximity)

	// Near threshold controls
	tab.nearSlider = widget.NewSlider(0.5, 5.0)
	tab.nearSlider.Step = 0.1
	tab.nearSlider.Value = tab.cfg.ProximityNearThreshold
	tab.nearEntry = widget.NewEntry()
	tab.nearEntry.SetText(fmt.Sprintf("%.1f", tab.nearSlider.Value))
	tab.nearEntry.OnChanged = func(s string) {
		if val, err := strconv.ParseFloat(s, 64); err == nil && val >= 0.5 && val <= 5.0 {
			tab.nearSlider.Value = val
			tab.nearSlider.Refresh()
			tab.saveNearThreshold(val)
		}
	}
	tab.nearSlider.OnChanged = func(v float64) {
		tab.nearEntry.SetText(fmt.Sprintf("%.1f", v))
		tab.saveNearThreshold(v)
	}

	// Far threshold controls
	tab.farSlider = widget.NewSlider(1.0, 10.0)
	tab.farSlider.Step = 0.2
	tab.farSlider.Value = tab.cfg.ProximityFarThreshold
	tab.farEntry = widget.NewEntry()
	tab.farEntry.SetText(fmt.Sprintf("%.1f", tab.farSlider.Value))
	tab.farEntry.OnChanged = func(s string) {
		if val, err := strconv.ParseFloat(s, 64); err == nil && val >= 1.0 && val <= 10.0 {
			tab.farSlider.Value = val
			tab.farSlider.Refresh()
			tab.saveFarThreshold(val)
		}
	}
	tab.farSlider.OnChanged = func(v float64) {
		tab.farEntry.SetText(fmt.Sprintf("%.1f", v))
		tab.saveFarThreshold(v)
	}

	// Calibration button
	tab.calibrateBtn = widget.NewButton("Calibrate Distance", tab.startCalibration)

	// Status displays
	tab.statusLabel = widget.NewLabel("Proximity service stopped")
	tab.distanceLabel = widget.NewLabel("Current distance: -- m")

	// Layout
	nearContainer := container.NewBorder(nil, nil, nil, tab.nearEntry, tab.nearSlider)
	farContainer := container.NewBorder(nil, nil, nil, tab.farEntry, tab.farSlider)

	content := container.NewVBox(
		widget.NewLabelWithStyle("Proximity Security", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.enableCheck,
		widget.NewLabel("Near threshold (unlock distance):"),
		nearContainer,
		widget.NewLabel("Far threshold (lock distance):"),
		farContainer,
		tab.calibrateBtn,
		widget.NewSeparator(),
		widget.NewLabel("Real‑time distance:"),
		tab.distanceLabel,
		tab.statusLabel,
	)
	return container.NewScroll(content)
}

func (t *ProximityTab) startProximityService() {
	if t.serviceRunning {
		return
	}
	t.serviceRunning = true
	t.statusLabel.SetText("Proximity service starting...")
	// TODO: Start actual proximity service (e.g., start Bluetooth RSSI scanning)
	go t.simulateDistanceUpdates()
}

func (t *ProximityTab) stopProximityService() {
	if !t.serviceRunning {
		return
	}
	t.serviceRunning = false
	close(t.stopUpdate)
	t.stopUpdate = make(chan struct{}) // reset for next start
	t.statusLabel.SetText("Proximity service stopped")
	t.distanceLabel.SetText("Current distance: -- m")
}

// simulateDistanceUpdates generates fake distance values for demonstration.
// Replace with actual RSSI reading from Bluetooth.
func (t *ProximityTab) simulateDistanceUpdates() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			// Simulate random distance between 0.5 and 8 meters
			dist := 1.5 + (float64(time.Now().UnixNano())%650)/100.0
			t.updateDistance(dist)
		case <-t.stopUpdate:
			return
		}
	}
}

func (t *ProximityTab) updateDistance(distance float64) {
	t.lastDistance = distance
	t.distanceLabel.SetText(fmt.Sprintf("Current distance: %.2f m", distance))
	// TODO: Send distance to server via WebSocket/TCP
	// Example: ConnectionManager.sendProximity(distance < t.farSlider.Value, distance)
}

func (t *ProximityTab) saveNearThreshold(value float64) {
	t.cfg.ProximityNearThreshold = value
	_ = t.cfg.Save()
	// Optionally notify the proximity service
}

func (t *ProximityTab) saveFarThreshold(value float64) {
	t.cfg.ProximityFarThreshold = value
	_ = t.cfg.Save()
}

func (t *ProximityTab) startCalibration() {
	if !t.serviceRunning {
		dialog.ShowInformation("Calibration", "Please enable the proximity service first.", fyne.CurrentApp().Driver().AllWindows()[0])
		return
	}
	steps := []struct {
		distance float64
		label    string
	}{
		{0.5, "Place phone exactly 0.5 meter away from the computer."},
		{1.0, "Place phone exactly 1 meter away."},
		{2.0, "Place phone exactly 2 meters away."},
		{5.0, "Place phone exactly 5 meters away."},
	}
	var stepIndex int
	var measurements []float64

	var showStep func()
	showStep = func() {
		if stepIndex >= len(steps) {
			if len(measurements) == len(steps) {
				var totalCorrection float64
				for i, m := range measurements {
					expected := steps[i].distance
					totalCorrection += expected / m
				}
				avgCorrection := totalCorrection / float64(len(measurements))
				// Save calibration factor (e.g., txPower or scale factor)
				// For now, just show result
				dialog.ShowInformation("Calibration Complete", fmt.Sprintf("Calibration factor: %.2f\nProximity readings will be adjusted automatically.", avgCorrection), fyne.CurrentApp().Driver().AllWindows()[0])
			}
			return
		}
		step := steps[stepIndex]
		dialog.ShowConfirm("Calibration Step", step.label+"\nTap OK when ready.", func(ok bool) {
			if ok {
				dist := t.lastDistance
				measurements = append(measurements, dist)
				stepIndex++
				showStep()
			} else {
				dialog.ShowInformation("Calibration Cancelled", "You can restart calibration later.", fyne.CurrentApp().Driver().AllWindows()[0])
			}
		}, fyne.CurrentApp().Driver().AllWindows()[0])
	}
	showStep()
}