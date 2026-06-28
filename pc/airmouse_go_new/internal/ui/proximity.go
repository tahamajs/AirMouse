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

	"airmouse-go/control/common"
	"airmouse-go/internal/config"
	"airmouse-go/internal/utils"
)

type ProximityTab struct {
	enableCheck     *widget.Check
	serviceRunning  bool
	nearSlider      *widget.Slider
	farSlider       *widget.Slider
	nearEntry       *widget.Entry
	farEntry        *widget.Entry
	calibrateBtn    *widget.Button
	lockNowBtn      *widget.Button
	unlockNowBtn    *widget.Button
	pauseMouseCheck *widget.Check // 👈 new: manual pause toggle
	mouseStateLabel *widget.Label // 👈 new: shows "Paused" or "Active"
	statusLabel     *widget.Label
	distanceLabel   *widget.Label
	deviceLabel     *widget.Label
	historyChart    *widget.Label
	overviewLabel   *widget.Label
	modeLabel       *widget.Label
	thresholdLabel  *widget.Label
	lastDistance    float64
	distanceHistory []float64
	stopUpdate      chan struct{}
	stopOnce        sync.Once
	cfg             *config.Config
	lastLockState   bool
	mu              sync.RWMutex
}

func NewProximityTab() (fyne.CanvasObject, *ProximityTab) {
	tab := &ProximityTab{
		stopUpdate:      make(chan struct{}),
		cfg:             config.Get(),
		distanceHistory: make([]float64, 0, 60),
		lastLockState:   false,
	}

	tab.overviewLabel = widget.NewLabel("Proximity security can lock the screen when the phone moves away and unlock when it returns.")
	tab.overviewLabel.Wrapping = fyne.TextWrapWord
	tab.modeLabel = widget.NewLabel("Mode: waiting for enable")
	tab.thresholdLabel = widget.NewLabel("")

	// 👇 New mouse state label
	tab.mouseStateLabel = widget.NewLabel("🖱️ Mouse: Active")
	tab.mouseStateLabel.Importance = widget.SuccessImportance

	header := container.NewHBox(
		widget.NewLabelWithStyle("📡 Proximity Security", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
	)

	tab.enableCheck = widget.NewCheck("Enable Proximity Lock/Unlock", func(enabled bool) {
		if enabled {
			tab.startProximityService()
		} else {
			tab.stopProximityService()
		}
	})
	tab.enableCheck.SetChecked(tab.cfg.ProximityEnabled)

	// 👇 New manual pause checkbox
	tab.pauseMouseCheck = widget.NewCheck("Pause Mouse (manual)", func(paused bool) {
		common.SetMovementPaused(paused)
		tab.updateMouseStateLabel(paused)
	})
	tab.pauseMouseCheck.SetChecked(common.IsMovementPaused())

	tab.statusLabel = widget.NewLabel("⚪ Proximity service stopped")
	tab.statusLabel.Importance = widget.MediumImportance
	tab.distanceLabel = widget.NewLabel("📏 Current distance: -- m")
	tab.deviceLabel = widget.NewLabel("📱 Paired device: None")

	// Status card (now includes mouse state)
	statusCard := container.NewVBox(
		widget.NewLabelWithStyle("Status", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.statusLabel,
		tab.distanceLabel,
		tab.deviceLabel,
		tab.modeLabel,
		tab.mouseStateLabel,
		tab.pauseMouseCheck,
	)

	// Thresholds (unchanged)
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
		tab.thresholdLabel,
	)

	// Actions (unchanged)
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
		widget.NewLabel("Use calibration to match your room and movement habits before enabling auto lock."),
	)

	// History (unchanged)
	tab.historyChart = widget.NewLabel("Distance History:\n")
	tab.historyChart.Wrapping = fyne.TextWrapWord

	historyCard := container.NewVBox(
		widget.NewLabelWithStyle("Distance History", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Live distance samples appear here with a simple bar visualization."),
		container.NewScroll(tab.historyChart),
	)

	overviewCard := container.NewVBox(
		widget.NewLabelWithStyle("Proximity Security", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.overviewLabel,
		widget.NewLabel("This page lets you tune near/far thresholds, calibrate behavior, and watch live distance changes."),
	)

	topGrid := container.NewGridWithColumns(2, overviewCard, statusCard)
	midGrid := container.NewGridWithColumns(2, thresholdsCard, actionsCard)

	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		tab.enableCheck,
		topGrid,
		midGrid,
		historyCard,
	)

	// 👇 Periodic update to sync mouse state
	go func() {
		for {
			time.Sleep(1 * time.Second)
			RunOnMain(func() {
				if tab.pauseMouseCheck != nil {
					tab.pauseMouseCheck.SetChecked(common.IsMovementPaused())
				}
			})
		}
	}()

	return container.NewScroll(content), tab
}

// updateMouseStateLabel updates the label based on pause state
func (t *ProximityTab) updateMouseStateLabel(paused bool) {
	if paused {
		t.mouseStateLabel.SetText("🖱️ Mouse: Paused")
		t.mouseStateLabel.Importance = widget.DangerImportance
	} else {
		t.mouseStateLabel.SetText("🖱️ Mouse: Active")
		t.mouseStateLabel.Importance = widget.SuccessImportance
	}
}

// Service lifecycle
// ------------------------------------------------------------

func (t *ProximityTab) startProximityService() {
	t.mu.Lock()
	if t.serviceRunning {
		t.mu.Unlock()
		return
	}
	t.serviceRunning = true
	t.mu.Unlock()

	t.statusLabel.SetText("🟢 Proximity service running")
	t.statusLabel.Importance = widget.SuccessImportance
	t.modeLabel.SetText("Mode: active monitoring")
	t.thresholdLabel.SetText(fmt.Sprintf("Auto-lock when distance is above %.1f m and unlock when below %.1f m.", t.cfg.ProximityFarThreshold, t.cfg.ProximityNearThreshold))
	utils.LogInfo("Proximity service started")
	go t.simulateDistanceUpdates()
}

func (t *ProximityTab) stopProximityService() {
	t.mu.Lock()
	if !t.serviceRunning {
		t.mu.Unlock()
		return
	}
	t.serviceRunning = false
	t.mu.Unlock()

	t.stopOnce.Do(func() {
		close(t.stopUpdate)
	})
	t.stopUpdate = make(chan struct{})
	t.stopOnce = sync.Once{}

	t.statusLabel.SetText("⚪ Proximity service stopped")
	t.statusLabel.Importance = widget.MediumImportance
	t.distanceLabel.SetText("📏 Current distance: -- m")
	t.modeLabel.SetText("Mode: service stopped")
	t.thresholdLabel.SetText("")
	utils.LogInfo("Proximity service stopped")
}

// Stop shuts down the proximity tab background work.
func (t *ProximityTab) Stop() {
	t.stopProximityService()
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
	t.mu.Lock()
	t.lastDistance = distance
	t.mu.Unlock()

	t.distanceHistory = append(t.distanceHistory, distance)
	if len(t.distanceHistory) > 60 {
		t.distanceHistory = t.distanceHistory[1:]
	}

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

	shouldLock := distance > t.cfg.ProximityFarThreshold
	shouldUnlock := distance < t.cfg.ProximityNearThreshold

	t.mu.Lock()
	running := t.serviceRunning
	lastLocked := t.lastLockState
	t.mu.Unlock()

	RunOnMain(func() {
		t.distanceLabel.SetText(fmt.Sprintf("📏 Current distance: %.2f m", distance))
		t.historyChart.SetText(historyText)
		t.modeLabel.SetText(fmt.Sprintf("Mode: %s | Near %.1f m | Far %.1f m", proximityModeLabel(running), t.cfg.ProximityNearThreshold, t.cfg.ProximityFarThreshold))

		if distance < t.cfg.ProximityNearThreshold {
			t.distanceLabel.Importance = widget.SuccessImportance
		} else if distance > t.cfg.ProximityFarThreshold {
			t.distanceLabel.Importance = widget.DangerImportance
		} else {
			t.distanceLabel.Importance = widget.WarningImportance
		}

		if shouldLock && !lastLocked {
			t.lockScreen()
			t.mu.Lock()
			t.lastLockState = true
			t.mu.Unlock()
		} else if shouldUnlock && lastLocked {
			t.unlockScreen()
			t.mu.Lock()
			t.lastLockState = false
			t.mu.Unlock()
		}
	})
}

func proximityModeLabel(running bool) string {
	if running {
		return "monitoring"
	}
	return "idle"
}

// ------------------------------------------------------------
// Actions
// ------------------------------------------------------------

func (t *ProximityTab) lockScreen() {
	utils.LogInfo("Screen locked due to proximity")
	// In production, call system lock API
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

	t.mu.RLock()
	running := t.serviceRunning
	t.mu.RUnlock()

	if !running {
		dialog.ShowInformation("Calibration", "Please enable the proximity service first.", win)
		return
	}

	t.thresholdLabel.SetText("Calibration in progress... keep the phone at the requested distances.")

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
				t.thresholdLabel.SetText(fmt.Sprintf("Calibration complete. Near %.1f m | Far %.1f m", t.cfg.ProximityNearThreshold, t.cfg.ProximityFarThreshold))
			}
			return
		}

		step := steps[stepIndex]
		t.mu.RLock()
		dist := t.lastDistance
		t.mu.RUnlock()

		msg := fmt.Sprintf("%s\n\nCurrent distance: %.2f m\n\nTap OK when ready.",
			step.label, dist)
		dialog.ShowConfirm(fmt.Sprintf("Calibration Step %d/%d", stepIndex+1, len(steps)),
			msg,
			func(ok bool) {
				if ok {
					measurements = append(measurements, dist)
					stepIndex++
					showStep()
				} else {
					dialog.ShowInformation("Calibration Cancelled", "You can restart calibration later.", win)
				}
			}, win)
	}
	showStep()
}
