package ui

import (
	"fmt"
	"strconv"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	mctl "airmouse-go/control/mouse"
)

type SettingsTab struct {
	sensitivitySlider     *widget.Slider
	sensitivityLabel      *widget.Label
	themeSelect           *widget.Select
	languageSelect        *widget.Select
	smoothingCheck        *widget.Check
	accelCheck            *widget.Check
	aiCheck               *widget.Check
	predictiveCheck       *widget.Check
	predictiveBlendSlider *widget.Slider
	predictiveBlendLabel  *widget.Label
	personalizationCheck  *widget.Check
	personalizationBuf    *widget.Slider
	personalizationLbl    *widget.Label
	intervalSlider        *widget.Slider
	intervalLbl           *widget.Label
	autoSwapCheck         *widget.Check
	serverURLEntry        *widget.Entry
	clickThresholdSlider  *widget.Slider
	clickThresholdLabel   *widget.Label
	scrollThresholdSlider *widget.Slider
	scrollThresholdLabel  *widget.Label
	doubleClickSlider     *widget.Slider
	doubleClickLabel      *widget.Label
	rightClickTiltSlider  *widget.Slider
	rightClickTiltLabel   *widget.Label
	hapticCheck           *widget.Check
	jitterCheck           *widget.Check
	jitterBlendSlider     *widget.Slider
	jitterBlendLabel      *widget.Label
	proximityNearSlider   *widget.Slider
	proximityNearLabel    *widget.Label
	proximityFarSlider    *widget.Slider
	proximityFarLabel     *widget.Label
	serverNameEntry       *widget.Entry
	autoStartCheck        *widget.Check
	logLevelSelect        *widget.Select
	portEntry             *widget.Entry
	wsPortEntry           *widget.Entry
	udpPortEntry          *widget.Entry
	saveBtn               *widget.Button
	resetBtn              *widget.Button
	exportBtn             *widget.Button
	importBtn             *widget.Button
	helpBtn               *widget.Button
	statusLabel           *widget.Label
	cfg                   *config.Config
	mouse                 mctl.Controller
}

func NewSettingsTab(cfg *config.Config, mouse mctl.Controller) fyne.CanvasObject {
	tab := &SettingsTab{cfg: cfg, mouse: mouse}

	serverSection := tab.createServerSection()
	cursorSection := tab.createCursorSection()
	clickSection := tab.createClickSection()
	aiSection := tab.createAISection()
	personalizationSection := tab.createPersonalizationSection()
	proximitySection := tab.createProximitySection()
	networkSection := tab.createNetworkSection()
	appearanceSection := tab.createAppearanceSection()
	advancedSection := tab.createAdvancedSection()

	tab.statusLabel = widget.NewLabel("")
	tab.statusLabel.Hidden = true

	tab.saveBtn = widget.NewButtonWithIcon("Save Settings", theme.DocumentSaveIcon(), tab.saveSettings)
	tab.saveBtn.Importance = widget.HighImportance

	tab.resetBtn = widget.NewButtonWithIcon("Reset to Defaults", theme.ViewRefreshIcon(), tab.resetSettings)
	tab.exportBtn = widget.NewButtonWithIcon("Export", theme.DownloadIcon(), tab.exportSettings)
	tab.importBtn = widget.NewButtonWithIcon("Import", theme.UploadIcon(), tab.importSettings)
	tab.helpBtn = widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		if win := getCurrentWindow(); win != nil {
			ShowContextHelp(win, "settings")
		}
	})

	actionButtons := container.NewHBox(
		tab.saveBtn, tab.resetBtn, tab.exportBtn, tab.importBtn, tab.helpBtn, tab.statusLabel,
	)

	settingsTabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Server", theme.ComputerIcon(), serverSection),
		container.NewTabItemWithIcon("Cursor", theme.ComputerIcon(), cursorSection),
		container.NewTabItemWithIcon("Click", theme.InfoIcon(), clickSection),
		container.NewTabItemWithIcon("AI", theme.ComputerIcon(), aiSection),
		container.NewTabItemWithIcon("Personalization", theme.SettingsIcon(), personalizationSection),
		container.NewTabItemWithIcon("Proximity", theme.VisibilityIcon(), proximitySection),
		container.NewTabItemWithIcon("Network", theme.ComputerIcon(), networkSection),
		container.NewTabItemWithIcon("Appearance", theme.ComputerIcon(), appearanceSection),
		container.NewTabItemWithIcon("Advanced", theme.SettingsIcon(), advancedSection),
	)

	content := container.NewBorder(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		actionButtons,
		nil, nil,
		settingsTabs,
	)
	return container.NewScroll(content)
}

// ---------- Section builders (no tooltips) ----------
func (t *SettingsTab) createServerSection() fyne.CanvasObject {
	t.serverNameEntry = widget.NewEntry()
	t.serverNameEntry.SetText(t.cfg.ServerName)
	t.serverNameEntry.SetPlaceHolder("Server Name")
	t.serverNameEntry.OnChanged = func(s string) {
		if s != "" {
			t.cfg.ServerName = s
		}
	}

	t.autoStartCheck = widget.NewCheck("Auto-start server on launch", func(on bool) {
		t.cfg.AutoStartServer = on
	})
	t.autoStartCheck.SetChecked(t.cfg.AutoStartServer)

	logLevels := []string{"debug", "info", "warn", "error"}
	t.logLevelSelect = widget.NewSelect(logLevels, func(level string) {
		t.cfg.LogLevel = level
	})
	t.logLevelSelect.SetSelected(t.cfg.LogLevel)

	return container.NewVBox(
		widget.NewLabelWithStyle("Server Configuration", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Server Name:"), t.serverNameEntry,
		widget.NewLabel("Log Level:"), t.logLevelSelect,
		t.autoStartCheck,
	)
}

func (t *SettingsTab) createCursorSection() fyne.CanvasObject {
	t.sensitivitySlider = widget.NewSlider(0.2, 3.0)
	t.sensitivitySlider.Step = 0.01
	t.sensitivitySlider.Value = t.cfg.Sensitivity
	t.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", t.cfg.Sensitivity))
	t.sensitivitySlider.OnChanged = func(v float64) {
		t.cfg.Sensitivity = v
		if t.mouse != nil {
			t.mouse.SetSensitivity(v)
		}
		t.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	t.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		if t.mouse != nil {
			t.mouse.SetSmoothing(b)
		}
		t.cfg.SmoothingEnabled = b
	})
	t.smoothingCheck.SetChecked(t.cfg.SmoothingEnabled)

	t.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		if t.mouse != nil {
			t.mouse.SetAcceleration(b, 1.5)
		}
		t.cfg.AccelerationEnabled = b
	})
	t.accelCheck.SetChecked(t.cfg.AccelerationEnabled)

	return container.NewVBox(
		widget.NewLabelWithStyle("Cursor Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Sensitivity:"), t.sensitivitySlider, t.sensitivityLabel,
		t.smoothingCheck,
		t.accelCheck,
	)
}

func (t *SettingsTab) createClickSection() fyne.CanvasObject {
	t.clickThresholdSlider = widget.NewSlider(0, 20)
	t.clickThresholdSlider.Step = 0.5
	t.clickThresholdSlider.Value = t.cfg.ClickThreshold
	t.clickThresholdLabel = widget.NewLabel(fmt.Sprintf("Click speed: %.1f rad/s", t.cfg.ClickThreshold))
	t.clickThresholdSlider.OnChanged = func(v float64) {
		t.cfg.ClickThreshold = v
		t.clickThresholdLabel.SetText(fmt.Sprintf("Click speed: %.1f rad/s", v))
	}

	t.doubleClickSlider = widget.NewSlider(200, 1000)
	t.doubleClickSlider.Step = 10
	t.doubleClickSlider.Value = float64(t.cfg.DoubleClickInterval)
	t.doubleClickLabel = widget.NewLabel(fmt.Sprintf("Double click interval: %d ms", t.cfg.DoubleClickInterval))
	t.doubleClickSlider.OnChanged = func(v float64) {
		t.cfg.DoubleClickInterval = int64(v)
		t.doubleClickLabel.SetText(fmt.Sprintf("Double click interval: %d ms", int(v)))
	}

	t.rightClickTiltSlider = widget.NewSlider(0, 90)
	t.rightClickTiltSlider.Step = 1
	t.rightClickTiltSlider.Value = t.cfg.RightClickTilt
	t.rightClickTiltLabel = widget.NewLabel(fmt.Sprintf("Right click tilt: %.0f°", t.cfg.RightClickTilt))
	t.rightClickTiltSlider.OnChanged = func(v float64) {
		t.cfg.RightClickTilt = v
		t.rightClickTiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", v))
	}

	t.scrollThresholdSlider = widget.NewSlider(0, 20)
	t.scrollThresholdSlider.Step = 0.5
	t.scrollThresholdSlider.Value = t.cfg.ScrollThreshold
	t.scrollThresholdLabel = widget.NewLabel(fmt.Sprintf("Scroll speed: %.1f m/s²", t.cfg.ScrollThreshold))
	t.scrollThresholdSlider.OnChanged = func(v float64) {
		t.cfg.ScrollThreshold = v
		t.scrollThresholdLabel.SetText(fmt.Sprintf("Scroll speed: %.1f m/s²", v))
	}

	t.hapticCheck = widget.NewCheck("Haptic Feedback", func(b bool) {
		t.cfg.HapticEnabled = b
	})
	t.hapticCheck.SetChecked(t.cfg.HapticEnabled)

	return container.NewVBox(
		widget.NewLabelWithStyle("Click & Gesture Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Click Threshold:"), t.clickThresholdSlider, t.clickThresholdLabel,
		widget.NewLabel("Double Click Interval:"), t.doubleClickSlider, t.doubleClickLabel,
		widget.NewLabel("Right Click Tilt Angle:"), t.rightClickTiltSlider, t.rightClickTiltLabel,
		widget.NewLabel("Scroll Threshold:"), t.scrollThresholdSlider, t.scrollThresholdLabel,
		t.hapticCheck,
	)
}

func (t *SettingsTab) createAISection() fyne.CanvasObject {
	t.aiCheck = widget.NewCheck("AI Smoothing (ONNX)", func(b bool) {
		if t.mouse != nil {
			t.mouse.EnableAISmoothing(b)
		}
		t.cfg.EnableAISmoothing = b
	})
	t.aiCheck.SetChecked(t.cfg.EnableAISmoothing)

	t.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman)", func(b bool) {
		t.cfg.EnablePredictive = b
		if t.mouse != nil {
			t.mouse.EnablePredictive(b)
		}
	})
	t.predictiveCheck.SetChecked(t.cfg.EnablePredictive)

	t.predictiveBlendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", t.cfg.PredictiveBlendFactor))
	t.predictiveBlendSlider = widget.NewSlider(0, 1)
	t.predictiveBlendSlider.Step = 0.05
	t.predictiveBlendSlider.Value = t.cfg.PredictiveBlendFactor
	t.predictiveBlendSlider.OnChanged = func(v float64) {
		t.cfg.PredictiveBlendFactor = v
		if t.mouse != nil {
			t.mouse.SetPredictiveBlendFactor(v)
		}
		t.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
	}

	t.jitterCheck = widget.NewCheck("Enable Jitter Compensation", func(b bool) {
		t.cfg.EnableJitterCompensation = b
	})
	t.jitterCheck.SetChecked(t.cfg.EnableJitterCompensation)

	t.jitterBlendLabel = widget.NewLabel(fmt.Sprintf("Jitter blend: %.2f", t.cfg.JitterBlendFactor))
	t.jitterBlendSlider = widget.NewSlider(0, 1)
	t.jitterBlendSlider.Step = 0.05
	t.jitterBlendSlider.Value = t.cfg.JitterBlendFactor
	t.jitterBlendSlider.OnChanged = func(v float64) {
		t.cfg.JitterBlendFactor = v
		t.jitterBlendLabel.SetText(fmt.Sprintf("Jitter blend: %.2f", v))
	}

	return container.NewVBox(
		widget.NewLabelWithStyle("AI & Prediction", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		t.aiCheck,
		t.predictiveCheck,
		widget.NewLabel("Prediction Blend Factor:"), t.predictiveBlendSlider, t.predictiveBlendLabel,
		t.jitterCheck,
		widget.NewLabel("Jitter Blend Factor:"), t.jitterBlendSlider, t.jitterBlendLabel,
	)
}

func (t *SettingsTab) createPersonalizationSection() fyne.CanvasObject {
	t.personalizationCheck = widget.NewCheck("Enable Personalization", func(b bool) {
		t.cfg.EnablePersonalization = b
	})
	t.personalizationCheck.SetChecked(t.cfg.EnablePersonalization)

	t.personalizationLbl = widget.NewLabel(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	t.personalizationBuf = widget.NewSlider(500, 5000)
	t.personalizationBuf.Step = 100
	t.personalizationBuf.Value = float64(t.cfg.PersonalizationBuffer)
	t.personalizationBuf.OnChanged = func(v float64) {
		t.cfg.PersonalizationBuffer = int(v)
		t.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	}

	t.intervalLbl = widget.NewLabel(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	t.intervalSlider = widget.NewSlider(600, 86400)
	t.intervalSlider.Step = 300
	t.intervalSlider.Value = float64(t.cfg.PersonalizationInterval)
	t.intervalSlider.OnChanged = func(v float64) {
		t.cfg.PersonalizationInterval = int(v)
		t.intervalLbl.SetText(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	}

	t.autoSwapCheck = widget.NewCheck("Auto-swap trained model", func(b bool) {
		t.cfg.AutoSwapModel = b
	})
	t.autoSwapCheck.SetChecked(t.cfg.AutoSwapModel)

	t.serverURLEntry = widget.NewEntry()
	t.serverURLEntry.SetText(t.cfg.PersonalizationServerURL)
	t.serverURLEntry.SetPlaceHolder("http://localhost:5001")
	t.serverURLEntry.OnChanged = func(s string) {
		t.cfg.PersonalizationServerURL = s
	}

	return container.NewVBox(
		widget.NewLabelWithStyle("Personalization", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		t.personalizationCheck,
		widget.NewLabel("Training Buffer:"), t.personalizationBuf, t.personalizationLbl,
		widget.NewLabel("Retrain Interval:"), t.intervalSlider, t.intervalLbl,
		t.autoSwapCheck,
		widget.NewLabel("Personalization Server URL:"), t.serverURLEntry,
	)
}

func (t *SettingsTab) createProximitySection() fyne.CanvasObject {
	t.proximityNearLabel = widget.NewLabel(fmt.Sprintf("Near threshold: %.1f m", t.cfg.ProximityNearThreshold))
	t.proximityNearSlider = widget.NewSlider(0.5, 5.0)
	t.proximityNearSlider.Step = 0.1
	t.proximityNearSlider.Value = t.cfg.ProximityNearThreshold
	t.proximityNearSlider.OnChanged = func(v float64) {
		t.cfg.ProximityNearThreshold = v
		t.proximityNearLabel.SetText(fmt.Sprintf("Near threshold: %.1f m", v))
	}

	t.proximityFarLabel = widget.NewLabel(fmt.Sprintf("Far threshold: %.1f m", t.cfg.ProximityFarThreshold))
	t.proximityFarSlider = widget.NewSlider(1.0, 10.0)
	t.proximityFarSlider.Step = 0.2
	t.proximityFarSlider.Value = t.cfg.ProximityFarThreshold
	t.proximityFarSlider.OnChanged = func(v float64) {
		t.cfg.ProximityFarThreshold = v
		t.proximityFarLabel.SetText(fmt.Sprintf("Far threshold: %.1f m", v))
	}

	return container.NewVBox(
		widget.NewLabelWithStyle("Proximity Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Near Threshold (Unlock):"), t.proximityNearSlider, t.proximityNearLabel,
		widget.NewLabel("Far Threshold (Lock):"), t.proximityFarSlider, t.proximityFarLabel,
	)
}

func (t *SettingsTab) createNetworkSection() fyne.CanvasObject {
	t.portEntry = widget.NewEntry()
	t.portEntry.SetText(fmt.Sprintf("%d", t.cfg.Port))
	t.portEntry.SetPlaceHolder("TCP Port")
	t.portEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}
	t.portEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			t.cfg.Port = p
		}
	}

	t.wsPortEntry = widget.NewEntry()
	t.wsPortEntry.SetText(fmt.Sprintf("%d", t.cfg.WebSocketPort))
	t.wsPortEntry.SetPlaceHolder("WebSocket Port")
	t.wsPortEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}
	t.wsPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			t.cfg.WebSocketPort = p
		}
	}

	t.udpPortEntry = widget.NewEntry()
	t.udpPortEntry.SetText(fmt.Sprintf("%d", t.cfg.UDPPort))
	t.udpPortEntry.SetPlaceHolder("UDP Discovery Port")
	t.udpPortEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}
	t.udpPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			t.cfg.UDPPort = p
		}
	}

	return container.NewVBox(
		widget.NewLabelWithStyle("Network Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("TCP Port:"), t.portEntry,
		widget.NewLabel("WebSocket Port:"), t.wsPortEntry,
		widget.NewLabel("UDP Port:"), t.udpPortEntry,
	)
}

func (t *SettingsTab) createAppearanceSection() fyne.CanvasObject {
	themes := []string{"dark", "light", "system"}
	t.themeSelect = widget.NewSelect(themes, func(theme string) {
		t.cfg.Theme = theme
		if app := fyne.CurrentApp(); app != nil {
			app.Settings().SetTheme(getThemeByName(theme))
		}
	})
	t.themeSelect.SetSelected(t.cfg.Theme)

	languages := []string{"English", "Persian", "Spanish", "French", "German", "Chinese"}
	t.languageSelect = widget.NewSelect(languages, func(lang string) {
		t.cfg.Language = lang
	})
	t.languageSelect.SetSelected(t.cfg.Language)

	return container.NewVBox(
		widget.NewLabelWithStyle("Appearance", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Theme:"), t.themeSelect,
		widget.NewLabel("Language:"), t.languageSelect,
	)
}

func (t *SettingsTab) createAdvancedSection() fyne.CanvasObject {
	debugCheck := widget.NewCheck("Debug Mode", func(on bool) {
		t.cfg.DebugMode = on
	})
	debugCheck.SetChecked(t.cfg.DebugMode)

	metricsCheck := widget.NewCheck("Enable Metrics Collection", func(on bool) {
		t.cfg.MetricsEnabled = on
	})
	metricsCheck.SetChecked(t.cfg.MetricsEnabled)

	return container.NewVBox(
		widget.NewLabelWithStyle("Advanced Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		debugCheck,
		metricsCheck,
	)
}

// ---------- Action methods ----------
func (t *SettingsTab) saveSettings() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if err := t.cfg.Save(); err != nil {
		t.setStatus("❌ Error saving: "+err.Error(), widget.DangerImportance)
		dialog.ShowError(err, win)
	} else {
		t.setStatus("✅ Settings saved successfully!", widget.SuccessImportance)
		dialog.ShowInformation("Settings Saved", "Your settings have been saved successfully.", win)
	}
}

func (t *SettingsTab) resetSettings() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowConfirm("Reset Settings",
		"Are you sure you want to reset all settings to default values?",
		func(confirmed bool) {
			if confirmed {
				t.cfg.ResetToDefaults()
				t.refreshUI()
				t.setStatus("✅ Settings reset to defaults!", widget.SuccessImportance)
				dialog.ShowInformation("Settings Reset", "All settings have been reset to default values.", win)
			}
		},
		win)
}

func (t *SettingsTab) refreshUI() {
	t.serverNameEntry.SetText(t.cfg.ServerName)
	t.autoStartCheck.SetChecked(t.cfg.AutoStartServer)
	t.logLevelSelect.SetSelected(t.cfg.LogLevel)

	t.sensitivitySlider.Value = t.cfg.Sensitivity
	t.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", t.cfg.Sensitivity))
	t.smoothingCheck.SetChecked(t.cfg.SmoothingEnabled)
	t.accelCheck.SetChecked(t.cfg.AccelerationEnabled)

	t.clickThresholdSlider.Value = t.cfg.ClickThreshold
	t.clickThresholdLabel.SetText(fmt.Sprintf("Click speed: %.1f rad/s", t.cfg.ClickThreshold))
	t.doubleClickSlider.Value = float64(t.cfg.DoubleClickInterval)
	t.doubleClickLabel.SetText(fmt.Sprintf("Double click interval: %d ms", t.cfg.DoubleClickInterval))
	t.rightClickTiltSlider.Value = t.cfg.RightClickTilt
	t.rightClickTiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", t.cfg.RightClickTilt))
	t.scrollThresholdSlider.Value = t.cfg.ScrollThreshold
	t.scrollThresholdLabel.SetText(fmt.Sprintf("Scroll speed: %.1f m/s²", t.cfg.ScrollThreshold))
	t.hapticCheck.SetChecked(t.cfg.HapticEnabled)

	t.aiCheck.SetChecked(t.cfg.EnableAISmoothing)
	t.predictiveCheck.SetChecked(t.cfg.EnablePredictive)
	t.predictiveBlendSlider.Value = t.cfg.PredictiveBlendFactor
	t.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", t.cfg.PredictiveBlendFactor))
	t.jitterCheck.SetChecked(t.cfg.EnableJitterCompensation)
	t.jitterBlendSlider.Value = t.cfg.JitterBlendFactor
	t.jitterBlendLabel.SetText(fmt.Sprintf("Jitter blend: %.2f", t.cfg.JitterBlendFactor))

	t.personalizationCheck.SetChecked(t.cfg.EnablePersonalization)
	t.personalizationBuf.Value = float64(t.cfg.PersonalizationBuffer)
	t.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	t.intervalSlider.Value = float64(t.cfg.PersonalizationInterval)
	t.intervalLbl.SetText(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	t.autoSwapCheck.SetChecked(t.cfg.AutoSwapModel)
	t.serverURLEntry.SetText(t.cfg.PersonalizationServerURL)

	t.proximityNearSlider.Value = t.cfg.ProximityNearThreshold
	t.proximityNearLabel.SetText(fmt.Sprintf("Near threshold: %.1f m", t.cfg.ProximityNearThreshold))
	t.proximityFarSlider.Value = t.cfg.ProximityFarThreshold
	t.proximityFarLabel.SetText(fmt.Sprintf("Far threshold: %.1f m", t.cfg.ProximityFarThreshold))

	t.portEntry.SetText(fmt.Sprintf("%d", t.cfg.Port))
	t.wsPortEntry.SetText(fmt.Sprintf("%d", t.cfg.WebSocketPort))
	t.udpPortEntry.SetText(fmt.Sprintf("%d", t.cfg.UDPPort))

	t.themeSelect.SetSelected(t.cfg.Theme)
	if app := fyne.CurrentApp(); app != nil {
		app.Settings().SetTheme(getThemeByName(t.cfg.Theme))
	}
	t.languageSelect.SetSelected(t.cfg.Language)
}

func (t *SettingsTab) exportSettings() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, win)
			}
			return
		}
		defer writer.Close()
		data := t.cfg.ToJSON()
		if _, err := writer.Write([]byte(data)); err != nil {
			dialog.ShowError(err, win)
			return
		}
		t.setStatus("✅ Settings exported successfully!", widget.SuccessImportance)
		dialog.ShowInformation("Export Complete", "Settings exported successfully.", win)
	}, win)
}

func (t *SettingsTab) importSettings() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, win)
			}
			return
		}
		defer reader.Close()
		buf := make([]byte, 1024*1024)
		n, err := reader.Read(buf)
		if err != nil && err.Error() != "EOF" {
			dialog.ShowError(err, win)
			return
		}
		if err := t.cfg.FromJSON(string(buf[:n])); err != nil {
			dialog.ShowError(err, win)
			return
		}
		t.refreshUI()
		t.setStatus("✅ Settings imported successfully!", widget.SuccessImportance)
		dialog.ShowInformation("Import Complete", "Settings imported successfully.", win)
	}, win)
}

func (t *SettingsTab) setStatus(msg string, importance widget.Importance) {
	t.statusLabel.SetText(msg)
	t.statusLabel.Importance = importance
	t.statusLabel.Hidden = false
	time.AfterFunc(5*time.Second, func() {
		RunOnMain(func() {
			if t.statusLabel != nil {
				t.statusLabel.SetText("")
				t.statusLabel.Hidden = true
			}
		})
	})
}