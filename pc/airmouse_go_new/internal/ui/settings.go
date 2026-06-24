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
	"airmouse-go/internal/control"
)

// ------------------------------------------------------------
//  SettingsTab
// ------------------------------------------------------------

type SettingsTab struct {
	// Core settings
	sensitivitySlider     *widget.Slider
	sensitivityLabel      *widget.Label
	themeSelect           *widget.Select
	languageSelect        *widget.Select
	smoothingCheck        *widget.Check
	accelCheck            *widget.Check
	aiCheck               *widget.Check

	// Predictive settings
	predictiveCheck       *widget.Check
	predictiveBlendSlider *widget.Slider
	predictiveBlendLabel  *widget.Label

	// Personalization
	personalizationCheck  *widget.Check
	personalizationBuf    *widget.Slider
	personalizationLbl    *widget.Label
	intervalSlider        *widget.Slider
	intervalLbl           *widget.Label
	autoSwapCheck         *widget.Check
	serverURLEntry        *widget.Entry

	// Click detection
	clickThresholdSlider  *widget.Slider
	clickThresholdLabel   *widget.Label
	scrollThresholdSlider *widget.Slider
	scrollThresholdLabel  *widget.Label
	doubleClickSlider     *widget.Slider
	doubleClickLabel      *widget.Label
	rightClickTiltSlider  *widget.Slider
	rightClickTiltLabel   *widget.Label

	// Additional features
	hapticCheck           *widget.Check
	jitterCheck           *widget.Check
	jitterBlendSlider     *widget.Slider
	jitterBlendLabel      *widget.Label
	proximityNearSlider   *widget.Slider
	proximityNearLabel    *widget.Label
	proximityFarSlider    *widget.Slider
	proximityFarLabel     *widget.Label

	// Server settings
	serverNameEntry       *widget.Entry
	autoStartCheck        *widget.Check
	logLevelSelect        *widget.Select

	// Network settings
	portEntry             *widget.Entry
	wsPortEntry           *widget.Entry
	udpPortEntry          *widget.Entry

	// Buttons
	saveBtn               *widget.Button
	resetBtn              *widget.Button
	exportBtn             *widget.Button
	importBtn             *widget.Button
	helpBtn               *widget.Button
	statusLabel           *widget.Label

	cfg                   *config.Config
	mouse                 control.MouseController
}

// NewSettingsTab creates the settings tab.
func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
	tab := &SettingsTab{
		cfg:   cfg,
		mouse: mouse,
	}

	// Build sections
	serverSection := tab.createServerSection()
	cursorSection := tab.createCursorSection()
	clickSection := tab.createClickSection()
	aiSection := tab.createAISection()
	personalizationSection := tab.createPersonalizationSection()
	proximitySection := tab.createProximitySection()
	networkSection := tab.createNetworkSection()
	appearanceSection := tab.createAppearanceSection()
	advancedSection := tab.createAdvancedSection()

	// Action buttons (status label initially hidden)
	tab.statusLabel = widget.NewLabel("")
	tab.statusLabel.Hidden = true

	tab.saveBtn = widget.NewButtonWithIcon("Save Settings", theme.DocumentSaveIcon(), tab.saveSettings)
	tab.saveBtn.Importance = widget.HighImportance
	tab.saveBtn.ToolTip = "Save all settings to disk"

	tab.resetBtn = widget.NewButtonWithIcon("Reset to Defaults", theme.ViewRefreshIcon(), tab.resetSettings)
	tab.resetBtn.ToolTip = "Reset all settings to default values"

	tab.exportBtn = widget.NewButtonWithIcon("Export", theme.DownloadIcon(), tab.exportSettings)
	tab.exportBtn.ToolTip = "Export settings to a JSON file"

	tab.importBtn = widget.NewButtonWithIcon("Import", theme.UploadIcon(), tab.importSettings)
	tab.importBtn.ToolTip = "Import settings from a JSON file"

	tab.helpBtn = widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		win := getCurrentWindow()
		if win != nil {
			ShowContextHelp(win, "settings")
		}
	})
	tab.helpBtn.ToolTip = "Show help for settings"

	actionButtons := container.NewHBox(
		tab.saveBtn,
		tab.resetBtn,
		tab.exportBtn,
		tab.importBtn,
		tab.helpBtn,
		tab.statusLabel,
	)

	// Tabs
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

// ------------------------------------------------------------
//  Section builders
// ------------------------------------------------------------

func (t *SettingsTab) createServerSection() fyne.CanvasObject {
	t.serverNameEntry = widget.NewEntry()
	t.serverNameEntry.SetText(t.cfg.ServerName)
	t.serverNameEntry.SetPlaceHolder("Server Name")
	t.serverNameEntry.OnChanged = func(s string) {
		if s != "" {
			t.cfg.ServerName = s
		}
	}
	t.serverNameEntry.ToolTip = "The name of this server as seen by clients"

	t.autoStartCheck = widget.NewCheck("Auto-start server on launch", func(on bool) {
		t.cfg.AutoStartServer = on
	})
	t.autoStartCheck.SetChecked(t.cfg.AutoStartServer)
	t.autoStartCheck.ToolTip = "Automatically start the server when the app launches"

	logLevels := []string{"debug", "info", "warn", "error"}
	t.logLevelSelect = widget.NewSelect(logLevels, func(level string) {
		t.cfg.LogLevel = level
	})
	t.logLevelSelect.SetSelected(t.cfg.LogLevel)
	t.logLevelSelect.ToolTip = "Log verbosity level"

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
	t.sensitivitySlider.ToolTip = "Cursor speed multiplier"

	t.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		if t.mouse != nil {
			t.mouse.SetSmoothing(b)
		}
		t.cfg.SmoothingEnabled = b
	})
	t.smoothingCheck.SetChecked(t.cfg.SmoothingEnabled)
	t.smoothingCheck.ToolTip = "Apply exponential moving average smoothing"

	t.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		if t.mouse != nil {
			t.mouse.SetAcceleration(b, 1.5)
		}
		t.cfg.AccelerationEnabled = b
	})
	t.accelCheck.SetChecked(t.cfg.AccelerationEnabled)
	t.accelCheck.ToolTip = "Enable pointer acceleration"

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
	t.clickThresholdSlider.ToolTip = "Angular speed required to trigger a click"

	t.doubleClickSlider = widget.NewSlider(200, 1000)
	t.doubleClickSlider.Step = 10
	t.doubleClickSlider.Value = float64(t.cfg.DoubleClickInterval)
	t.doubleClickLabel = widget.NewLabel(fmt.Sprintf("Double click interval: %d ms", t.cfg.DoubleClickInterval))
	t.doubleClickSlider.OnChanged = func(v float64) {
		t.cfg.DoubleClickInterval = int64(v)
		t.doubleClickLabel.SetText(fmt.Sprintf("Double click interval: %d ms", int(v)))
	}
	t.doubleClickSlider.ToolTip = "Maximum time between two clicks for double‑click"

	t.rightClickTiltSlider = widget.NewSlider(0, 90)
	t.rightClickTiltSlider.Step = 1
	t.rightClickTiltSlider.Value = t.cfg.RightClickTilt
	t.rightClickTiltLabel = widget.NewLabel(fmt.Sprintf("Right click tilt: %.0f°", t.cfg.RightClickTilt))
	t.rightClickTiltSlider.OnChanged = func(v float64) {
		t.cfg.RightClickTilt = v
		t.rightClickTiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", v))
	}
	t.rightClickTiltSlider.ToolTip = "Tilt angle required for right‑click"

	t.scrollThresholdSlider = widget.NewSlider(0, 20)
	t.scrollThresholdSlider.Step = 0.5
	t.scrollThresholdSlider.Value = t.cfg.ScrollThreshold
	t.scrollThresholdLabel = widget.NewLabel(fmt.Sprintf("Scroll speed: %.1f m/s²", t.cfg.ScrollThreshold))
	t.scrollThresholdSlider.OnChanged = func(v float64) {
		t.cfg.ScrollThreshold = v
		t.scrollThresholdLabel.SetText(fmt.Sprintf("Scroll speed: %.1f m/s²", v))
	}
	t.scrollThresholdSlider.ToolTip = "Acceleration required to trigger scroll"

	t.hapticCheck = widget.NewCheck("Haptic Feedback", func(b bool) {
		t.cfg.HapticEnabled = b
	})
	t.hapticCheck.SetChecked(t.cfg.HapticEnabled)
	t.hapticCheck.ToolTip = "Enable vibration feedback on gesture detection"

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
	t.aiCheck.ToolTip = "Use ONNX AI model for movement smoothing"

	t.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman)", func(b bool) {
		t.cfg.EnablePredictive = b
		if t.mouse != nil {
			t.mouse.EnablePredictive(b)
		}
	})
	t.predictiveCheck.SetChecked(t.cfg.EnablePredictive)
	t.predictiveCheck.ToolTip = "Use Kalman filter for predictive movement"

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
	t.predictiveBlendSlider.ToolTip = "How much prediction to blend with raw movement"

	t.jitterCheck = widget.NewCheck("Enable Jitter Compensation", func(b bool) {
		t.cfg.EnableJitterCompensation = b
	})
	t.jitterCheck.SetChecked(t.cfg.EnableJitterCompensation)
	t.jitterCheck.ToolTip = "Reduce jitter in movement"

	t.jitterBlendLabel = widget.NewLabel(fmt.Sprintf("Jitter blend: %.2f", t.cfg.JitterBlendFactor))
	t.jitterBlendSlider = widget.NewSlider(0, 1)
	t.jitterBlendSlider.Step = 0.05
	t.jitterBlendSlider.Value = t.cfg.JitterBlendFactor
	t.jitterBlendSlider.OnChanged = func(v float64) {
		t.cfg.JitterBlendFactor = v
		t.jitterBlendLabel.SetText(fmt.Sprintf("Jitter blend: %.2f", v))
	}
	t.jitterBlendSlider.ToolTip = "Strength of jitter compensation"

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
	t.personalizationCheck.ToolTip = "Learn your movement patterns for smoother control"

	t.personalizationLbl = widget.NewLabel(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	t.personalizationBuf = widget.NewSlider(500, 5000)
	t.personalizationBuf.Step = 100
	t.personalizationBuf.Value = float64(t.cfg.PersonalizationBuffer)
	t.personalizationBuf.OnChanged = func(v float64) {
		t.cfg.PersonalizationBuffer = int(v)
		t.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	}
	t.personalizationBuf.ToolTip = "Number of samples to keep for training"

	t.intervalLbl = widget.NewLabel(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	t.intervalSlider = widget.NewSlider(600, 86400)
	t.intervalSlider.Step = 300
	t.intervalSlider.Value = float64(t.cfg.PersonalizationInterval)
	t.intervalSlider.OnChanged = func(v float64) {
		t.cfg.PersonalizationInterval = int(v)
		t.intervalLbl.SetText(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	}
	t.intervalSlider.ToolTip = "How often to retrain the model (seconds)"

	t.autoSwapCheck = widget.NewCheck("Auto-swap trained model", func(b bool) {
		t.cfg.AutoSwapModel = b
	})
	t.autoSwapCheck.SetChecked(t.cfg.AutoSwapModel)
	t.autoSwapCheck.ToolTip = "Automatically swap to the newly trained model"

	t.serverURLEntry = widget.NewEntry()
	t.serverURLEntry.SetText(t.cfg.PersonalizationServerURL)
	t.serverURLEntry.SetPlaceHolder("http://localhost:5001")
	t.serverURLEntry.OnChanged = func(s string) {
		t.cfg.PersonalizationServerURL = s
	}
	t.serverURLEntry.ToolTip = "URL of the personalization training server"

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
	t.proximityNearSlider.ToolTip = "Distance below which the device is considered near (unlock)"

	t.proximityFarLabel = widget.NewLabel(fmt.Sprintf("Far threshold: %.1f m", t.cfg.ProximityFarThreshold))
	t.proximityFarSlider = widget.NewSlider(1.0, 10.0)
	t.proximityFarSlider.Step = 0.2
	t.proximityFarSlider.Value = t.cfg.ProximityFarThreshold
	t.proximityFarSlider.OnChanged = func(v float64) {
		t.cfg.ProximityFarThreshold = v
		t.proximityFarLabel.SetText(fmt.Sprintf("Far threshold: %.1f m", v))
	}
	t.proximityFarSlider.ToolTip = "Distance above which the device is considered far (lock)"

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
	t.portEntry.ToolTip = "TCP port for mouse control (default 8080)"

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
	t.wsPortEntry.ToolTip = "WebSocket port for pairing (default 8081)"

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
	t.udpPortEntry.ToolTip = "UDP port for device discovery (default 8082)"

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
	t.themeSelect.ToolTip = "Application colour theme"

	languages := []string{"English", "Persian", "Spanish", "French", "German", "Chinese"}
	t.languageSelect = widget.NewSelect(languages, func(lang string) {
		t.cfg.Language = lang
	})
	t.languageSelect.SetSelected(t.cfg.Language)
	t.languageSelect.ToolTip = "UI language (currently only English is fully supported)"

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
	debugCheck.ToolTip = "Enable additional debug logging"

	metricsCheck := widget.NewCheck("Enable Metrics Collection", func(on bool) {
		t.cfg.MetricsEnabled = on
	})
	metricsCheck.SetChecked(t.cfg.MetricsEnabled)
	metricsCheck.ToolTip = "Collect anonymous usage metrics"

	return container.NewVBox(
		widget.NewLabelWithStyle("Advanced Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		debugCheck,
		metricsCheck,
	)
}

// ------------------------------------------------------------
//  Action methods
// ------------------------------------------------------------

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

// refreshUI updates all widgets to reflect the current config.
func (t *SettingsTab) refreshUI() {
	// Server
	t.serverNameEntry.SetText(t.cfg.ServerName)
	t.autoStartCheck.SetChecked(t.cfg.AutoStartServer)
	t.logLevelSelect.SetSelected(t.cfg.LogLevel)

	// Cursor
	t.sensitivitySlider.Value = t.cfg.Sensitivity
	t.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", t.cfg.Sensitivity))
	t.smoothingCheck.SetChecked(t.cfg.SmoothingEnabled)
	t.accelCheck.SetChecked(t.cfg.AccelerationEnabled)

	// Click
	t.clickThresholdSlider.Value = t.cfg.ClickThreshold
	t.clickThresholdLabel.SetText(fmt.Sprintf("Click speed: %.1f rad/s", t.cfg.ClickThreshold))
	t.doubleClickSlider.Value = float64(t.cfg.DoubleClickInterval)
	t.doubleClickLabel.SetText(fmt.Sprintf("Double click interval: %d ms", t.cfg.DoubleClickInterval))
	t.rightClickTiltSlider.Value = t.cfg.RightClickTilt
	t.rightClickTiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", t.cfg.RightClickTilt))
	t.scrollThresholdSlider.Value = t.cfg.ScrollThreshold
	t.scrollThresholdLabel.SetText(fmt.Sprintf("Scroll speed: %.1f m/s²", t.cfg.ScrollThreshold))
	t.hapticCheck.SetChecked(t.cfg.HapticEnabled)

	// AI
	t.aiCheck.SetChecked(t.cfg.EnableAISmoothing)
	t.predictiveCheck.SetChecked(t.cfg.EnablePredictive)
	t.predictiveBlendSlider.Value = t.cfg.PredictiveBlendFactor
	t.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", t.cfg.PredictiveBlendFactor))
	t.jitterCheck.SetChecked(t.cfg.EnableJitterCompensation)
	t.jitterBlendSlider.Value = t.cfg.JitterBlendFactor
	t.jitterBlendLabel.SetText(fmt.Sprintf("Jitter blend: %.2f", t.cfg.JitterBlendFactor))

	// Personalization
	t.personalizationCheck.SetChecked(t.cfg.EnablePersonalization)
	t.personalizationBuf.Value = float64(t.cfg.PersonalizationBuffer)
	t.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", t.cfg.PersonalizationBuffer))
	t.intervalSlider.Value = float64(t.cfg.PersonalizationInterval)
	t.intervalLbl.SetText(fmt.Sprintf("Retrain interval: %d seconds", t.cfg.PersonalizationInterval))
	t.autoSwapCheck.SetChecked(t.cfg.AutoSwapModel)
	t.serverURLEntry.SetText(t.cfg.PersonalizationServerURL)

	// Proximity
	t.proximityNearSlider.Value = t.cfg.ProximityNearThreshold
	t.proximityNearLabel.SetText(fmt.Sprintf("Near threshold: %.1f m", t.cfg.ProximityNearThreshold))
	t.proximityFarSlider.Value = t.cfg.ProximityFarThreshold
	t.proximityFarLabel.SetText(fmt.Sprintf("Far threshold: %.1f m", t.cfg.ProximityFarThreshold))

	// Network
	t.portEntry.SetText(fmt.Sprintf("%d", t.cfg.Port))
	t.wsPortEntry.SetText(fmt.Sprintf("%d", t.cfg.WebSocketPort))
	t.udpPortEntry.SetText(fmt.Sprintf("%d", t.cfg.UDPPort))

	// Appearance
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

// setStatus updates the status label with a message and importance.
func (t *SettingsTab) setStatus(msg string, importance widget.Importance) {
	t.statusLabel.SetText(msg)
	t.statusLabel.Importance = importance
	t.statusLabel.Hidden = false
	// Clear after 5 seconds
	time.AfterFunc(5*time.Second, func() {
		RunOnMain(func() {
			if t.statusLabel != nil {
				t.statusLabel.SetText("")
				t.statusLabel.Hidden = true
			}
		})
	})
}