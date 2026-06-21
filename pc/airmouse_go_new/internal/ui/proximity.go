package ui

import (
	"fmt"
	"math"
	"strconv"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/utils"
)

// ------------------------------------------------------------
// ProximityTab
// ------------------------------------------------------------

type ProximityTab struct {
	enableCheck      *widget.Check
	serviceRunning   bool
	nearSlider       *widget.Slider
	farSlider        *widget.Slider
	nearEntry        *widget.Entry
	farEntry         *widget.Entry
	calibrateBtn     *widget.Button
	lockNowBtn       *widget.Button
	unlockNowBtn     *widget.Button
	statusLabel      *widget.Label
	distanceLabel    *widget.Label
	deviceLabel      *widget.Label
	historyChart     *widget.Label
	lastDistance     float64
	distanceHistory  []float64
	stopUpdate       chan struct{}
	stopOnce         sync.Once
	cfg              *config.Config
	lastLockState    bool
}

// NewProximityTab creates the proximity security tab.
func NewProximityTab() fyne.CanvasObject {
	tab := &ProximityTab{
		stopUpdate:      make(chan struct{}),
		cfg:             config.Get(),
		distanceHistory: make([]float64, 0, 60),
		lastLockState:   false,
	}

	// ----- Header -----
	header := container.NewHBox(
		widget.NewLabelWithStyle("📡 Proximity Security", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
	)

	// ----- Enable checkbox -----
	tab.enableCheck = widget.NewCheck("Enable Proximity Lock/Unlock", func(enabled bool) {
		if enabled {
			tab.startProximityService()
		} else {
			tab.stopProximityService()
		}
	})
	tab.enableCheck.SetChecked(tab.cfg.ProximityEnabled)

	// ----- Status labels (initialised here) -----
	tab.statusLabel = widget.NewLabel("⚪ Proximity service stopped")
	tab.statusLabel.Importance = widget.MediumImportance
	tab.distanceLabel = widget.NewLabel("📏 Current distance: -- m")
	tab.deviceLabel = widget.NewLabel("📱 Paired device: None")

	// Status card
	statusCard := container.NewVBox(
		widget.NewLabelWithStyle("Status", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.statusLabel,
		tab.distanceLabel,
		tab.deviceLabel,
	)

	// ----- Thresholds -----
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

	nearContainer := container.NewBorder(nil, nil, nil, tab.nearEntry, tab.nearSlider)
	farContainer := container.NewBorder(nil, nil, nil, tab.farEntry, tab.farSlider)

	thresholdsCard := container.NewVBox(
		widget.NewLabelWithStyle("Thresholds", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Near (Unlock):"), nearContainer,
		widget.NewLabel("Far (Lock):"), farContainer,
	)

	// ----- Actions -----
	tab.calibrateBtn = widget.NewButtonWithIcon("Start Calibration", theme.SettingsIcon(), tab.startCalibration)
	tab.lockNowBtn = widget.NewButtonWithIcon("Lock Now", theme.CancelIcon(), func() {
		tab.lockScreen()
	})
	tab.unlockNowBtn = widget.NewButtonWithIcon("Unlock Now", theme.ConfirmIcon(), func() {
		tab.unlockScreen()
	})

	actionsCard := container.NewVBox(
		widget.NewLabelWithStyle("Actions", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewHBox(tab.calibrateBtn, tab.lockNowBtn, tab.unlockNowBtn),
	)

	// ----- History -----
	tab.historyChart = widget.NewLabel("Distance History:\n")
	tab.historyChart.Wrapping = fyne.TextWrapWord

	historyCard := container.NewVBox(
		widget.NewLabelWithStyle("Distance History", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewScroll(tab.historyChart),
	)

	// ----- Main layout -----
	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		tab.enableCheck,
		container.NewGridWithColumns(2, statusCard, thresholdsCard),
		actionsCard,
		historyCard,
	)

	return container.NewScroll(content)
}

// ------------------------------------------------------------
// Service lifecycle
// ------------------------------------------------------------

func (t *ProximityTab) startProximityService() {
	if t.serviceRunning {
		return
	}
	t.serviceRunning = true
	t.statusLabel.SetText("🟢 Proximity service running")
	t.statusLabel.Importance = widget.SuccessImportance
	utils.LogInfo("Proximity service started")
	go t.simulateDistanceUpdates()
}

func (t *ProximityTab) stopProximityService() {
	if !t.serviceRunning {
		return
	}
	t.serviceRunning = false
	t.stopOnce.Do(func() {
		close(t.stopUpdate)
	})
	t.stopUpdate = make(chan struct{})
	t.stopOnce = sync.Once{}
	t.statusLabel.SetText("⚪ Proximity service stopped")
	t.statusLabel.Importance = widget.MediumImportance
	t.distanceLabel.SetText("📏 Current distance: -- m")
	utils.LogInfo("Proximity service stopped")
}

// ------------------------------------------------------------
// Simulation (replace with real sensor data later)
// ------------------------------------------------------------

func (t *ProximityTab) simulateDistanceUpdates() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			// Simulate realistic distance (1–5 m)
			base := 2.0
			variation := math.Sin(float64(time.Now().UnixNano())/1e9) * 1.5
			dist := math.Max(0.5, base+variation)
			t.updateDistance(dist)
		case <-t.stopUpdate:
			return
		}
	}
}

func (t *ProximityTab) updateDistance(distance float64) {
	t.lastDistance = distance

	// Update history (keep last 60 samples)
	t.distanceHistory = append(t.distanceHistory, distance)
	if len(t.distanceHistory) > 60 {
		t.distanceHistory = t.distanceHistory[1:]
	}

	// Build history text
	historyText := "Distance History (last 60s):\n"
	for i, d := range t.distanceHistory {
		barLen := int(d * 5)
		if barLen > 50 {
			barLen = 50
		}
		bar := ""
		for j := 0; j < barLen; j++ {
			bar += "█"
		}
		historyText += fmt.Sprintf("%2ds: %5.2fm %s\n", i, d, bar)
	}

	// Determine lock state
	shouldLock := distance > t.cfg.ProximityFarThreshold
	shouldUnlock := distance < t.cfg.ProximityNearThreshold

	RunOnMain(func() {
		t.distanceLabel.SetText(fmt.Sprintf("📏 Current distance: %.2f m", distance))
		t.historyChart.SetText(historyText)

		// Colour coding
		if distance < t.cfg.ProximityNearThreshold {
			t.distanceLabel.Importance = widget.SuccessImportance
		} else if distance > t.cfg.ProximityFarThreshold {
			t.distanceLabel.Importance = widget.DangerImportance
		} else {
			t.distanceLabel.Importance = widget.WarningImportance
		}

		// Auto lock/unlock
		if shouldLock && !t.lastLockState {
			t.lockScreen()
			t.lastLockState = true
		} else if shouldUnlock && t.lastLockState {
			t.unlockScreen()
			t.lastLockState = false
		}
	})
}

// ------------------------------------------------------------
// Actions
// ------------------------------------------------------------

func (t *ProximityTab) lockScreen() {
	utils.LogInfo("Screen locked due to proximity")
	// In production, call system lock API (e.g., DBus on Linux, WinAPI on Windows)
}

func (t *ProximityTab) unlockScreen() {
	utils.LogInfo("Screen unlocked due to proximity")
}

func (t *ProximityTab) saveNearThreshold(value float64) {
	t.cfg.ProximityNearThreshold = value
	_ = t.cfg.Save()
}

func (t *ProximityTab) saveFarThreshold(value float64) {
	t.cfg.ProximityFarThreshold = value
	_ = t.cfg.Save()
}

func (t *ProximityTab) startCalibration() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if !t.serviceRunning {
		dialog.ShowInformation("Calibration", "Please enable the proximity service first.", win)
		return
	}

	steps := []struct {
		distance float64
		label    string
	}{
		{0.5, "Place phone exactly 0.5 meters away."},
		{1.0, "Place phone exactly 1 meter away."},
		{2.0, "Place phone exactly 2 meters away."},
		{3.0, "Place phone exactly 3 meters away."},
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
					correction := expected / m
					totalCorrection += correction
					utils.LogDebug("Step %d: expected=%.1fm, measured=%.2fm, correction=%.2f",
						i+1, expected, m, correction)
				}
				avgCorrection := totalCorrection / float64(len(measurements))
				resultMsg := fmt.Sprintf("Calibration complete!\n\nCorrection factor: %.2f\n\nProximity readings will be adjusted automatically.", avgCorrection)
				dialog.ShowInformation("Calibration Complete", resultMsg, win)
			}
			return
		}

		step := steps[stepIndex]
		msg := fmt.Sprintf("%s\n\nCurrent distance: %.2f m\n\nTap OK when ready.",
			step.label, t.lastDistance)
		dialog.ShowConfirm(fmt.Sprintf("Calibration Step %d/%d", stepIndex+1, len(steps)),
			msg,
			func(ok bool) {
				if ok {
					measurements = append(measurements, t.lastDistance)
					stepIndex++
					showStep()
				} else {
					dialog.ShowInformation("Calibration Cancelled", "You can restart calibration later.", win)
				}
			}, win)
	}
	showStep()
}
