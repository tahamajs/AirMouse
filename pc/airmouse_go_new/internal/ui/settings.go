package ui

import (
    "fmt"
    "strings"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
)

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
    
    // Buttons
    saveBtn               *widget.Button
    resetBtn              *widget.Button
    exportBtn             *widget.Button
    importBtn             *widget.Button
    statusLabel           *widget.Label
    
    cfg                   *config.Config
    mouse                 control.MouseController
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
    tab := &SettingsTab{
        cfg:   cfg,
        mouse: mouse,
    }
    
    // Server Settings Section
    serverSection := tab.createServerSection()
    
    // Cursor Settings Section
    cursorSection := tab.createCursorSection()
    
    // Click & Gesture Settings Section
    clickSection := tab.createClickSection()
    
    // AI & Prediction Section
    aiSection := tab.createAISection()
    
    // Personalization Section
    personalizationSection := tab.createPersonalizationSection()
    
    // Proximity Section
    proximitySection := tab.createProximitySection()
    
    // Network Section
    networkSection := tab.createNetworkSection()
    
    // Appearance Section
    appearanceSection := tab.createAppearanceSection()
    
    // Advanced Section
    advancedSection := tab.createAdvancedSection()
    
    // Action buttons
    actionButtons := container.NewHBox(
        tab.saveBtn,
        tab.resetBtn,
        tab.exportBtn,
        tab.importBtn,
        tab.statusLabel,
    )
    
    // Main content with tabs
    settingsTabs := container.NewAppTabs(
        container.NewTabItemWithIcon("Server", theme.ComputerIcon(), serverSection),
        container.NewTabItemWithIcon("Cursor", theme.MouseIcon(), cursorSection),
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

func (t *SettingsTab) createServerSection() fyne.CanvasObject {
    t.serverNameEntry = widget.NewEntry()
    t.serverNameEntry.SetText(t.cfg.ServerName)
    t.serverNameEntry.SetPlaceHolder("Server Name")
    
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
    // Sensitivity
    t.sensitivitySlider = widget.NewSlider(0.2, 3.0)
    t.sensitivitySlider.Step = 0.01
    t.sensitivitySlider.Value = t.cfg.Sensitivity
    t.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", t.cfg.Sensitivity))
    t.sensitivitySlider.OnChanged = func(v float64) {
        t.cfg.SetSensitivity(v)
        t.mouse.SetSensitivity(v)
        t.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
    }
    
    // Smoothing
    t.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) { 
        t.mouse.SetSmoothing(b)
        t.cfg.SmoothingEnabled = b
    })
    t.smoothingCheck.SetChecked(t.cfg.SmoothingEnabled)
    
    // Acceleration
    t.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) { 
        t.mouse.SetAcceleration(b, 1.5)
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
    // Click Threshold
    t.clickThresholdSlider = widget.NewSlider(0, 20)
    t.clickThresholdSlider.Step = 0.5
    t.clickThresholdSlider.Value = t.cfg.ClickThreshold
    t.clickThresholdLabel = widget.NewLabel(fmt.Sprintf("Click speed: %.1f rad/s", t.cfg.ClickThreshold))
    t.clickThresholdSlider.OnChanged = func(v float64) {
        t.cfg.ClickThreshold = v
        t.clickThresholdLabel.SetText(fmt.Sprintf("Click speed: %.1f rad/s", v))
    }
    
    // Double Click Interval
    t.doubleClickSlider = widget.NewSlider(200, 1000)
    t.doubleClickSlider.Step = 10
    t.doubleClickSlider.Value = float64(t.cfg.DoubleClickInterval)
    t.doubleClickLabel = widget.NewLabel(fmt.Sprintf("Double click interval: %d ms", t.cfg.DoubleClickInterval))
    t.doubleClickSlider.OnChanged = func(v float64) {
        t.cfg.DoubleClickInterval = int64(v)
        t.doubleClickLabel.SetText(fmt.Sprintf("Double click interval: %d ms", int(v)))
    }
    
    // Right Click Tilt
    t.rightClickTiltSlider = widget.NewSlider(0, 90)
    t.rightClickTiltSlider.Step = 1
    t.rightClickTiltSlider.Value = t.cfg.RightClickTilt
    t.rightClickTiltLabel = widget.NewLabel(fmt.Sprintf("Right click tilt: %.0f°", t.cfg.RightClickTilt))
    t.rightClickTiltSlider.OnChanged = func(v float64) {
        t.cfg.RightClickTilt = v
        t.rightClickTiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", v))
    }
    
    // Scroll Threshold
    t.scrollThresholdSlider = widget.NewSlider(0, 20)
    t.scrollThresholdSlider.Step = 0.5
    t.scrollThresholdSlider.Value = t.cfg.ScrollThreshold
    t.scrollThresholdLabel = widget.NewLabel(fmt.Sprintf("Scroll speed: %.1f m/s²", t.cfg.ScrollThreshold))
    t.scrollThresholdSlider.OnChanged = func(v float64) {
        t.cfg.ScrollThreshold = v
        t.scrollThresholdLabel.SetText(fmt.Sprintf("Scroll speed: %.1f m/s²", v))
    }
    
    // Haptic Feedback
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
    // AI Smoothing
    t.aiCheck = widget.NewCheck("AI Smoothing (ONNX)", func(b bool) {
        t.mouse.EnableAISmoothing(b)
        t.cfg.EnableAISmoothing = b
    })
    t.aiCheck.SetChecked(t.cfg.EnableAISmoothing)
    
    // Predictive Movement
    t.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman)", func(b bool) {
        t.cfg.EnablePredictive = b
        t.mouse.EnablePredictive(b)
    })
    t.predictiveCheck.SetChecked(t.cfg.EnablePredictive)
    
    t.predictiveBlendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", t.cfg.PredictiveBlendFactor))
    t.predictiveBlendSlider = widget.NewSlider(0, 1)
    t.predictiveBlendSlider.Step = 0.05
    t.predictiveBlendSlider.Value = t.cfg.PredictiveBlendFactor
    t.predictiveBlendSlider.OnChanged = func(v float64) {
        t.cfg.PredictiveBlendFactor = v
        t.mouse.SetPredictiveBlendFactor(v)
        t.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
    }
    
    // Jitter Compensation
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
    portEntry := widget.NewEntry()
    portEntry.SetText(fmt.Sprintf("%d", t.cfg.Port))
    
    wsPortEntry := widget.NewEntry()
    wsPortEntry.SetText(fmt.Sprintf("%d", t.cfg.WebSocketPort))
    
    udpPortEntry := widget.NewEntry()
    udpPortEntry.SetText(fmt.Sprintf("%d", t.cfg.UDPPort))
    
    return container.NewVBox(
        widget.NewLabelWithStyle("Network Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("TCP Port:"), portEntry,
        widget.NewLabel("WebSocket Port:"), wsPortEntry,
        widget.NewLabel("UDP Port:"), udpPortEntry,
    )
}

func (t *SettingsTab) createAppearanceSection() fyne.CanvasObject {
    themes := GetAllThemes()
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
    
    // Create buttons
    t.saveBtn = widget.NewButtonWithIcon("Save Settings", theme.DocumentSaveIcon(), t.saveSettings)
    t.resetBtn = widget.NewButtonWithIcon("Reset to Defaults", theme.ViewRefreshIcon(), t.resetSettings)
    t.exportBtn = widget.NewButtonWithIcon("Export", theme.DownloadIcon(), t.exportSettings)
    t.importBtn = widget.NewButtonWithIcon("Import", theme.UploadIcon(), t.importSettings)
    t.statusLabel = widget.NewLabel("")
    
    return container.NewVBox(
        widget.NewLabelWithStyle("Advanced Settings", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        debugCheck,
        metricsCheck,
    )
}

func (t *SettingsTab) saveSettings() {
    if err := t.cfg.Save(); err != nil {
        t.statusLabel.SetText("❌ Error saving: " + err.Error())
        t.statusLabel.Importance = widget.DangerImportance
    } else {
        t.statusLabel.SetText("✅ Settings saved successfully!")
        t.statusLabel.Importance = widget.SuccessImportance
        
        // Show success dialog
        dialog.ShowInformation("Settings Saved", "Your settings have been saved successfully.", fyne.CurrentApp().Driver().AllWindows()[0])
    }
}

func (t *SettingsTab) resetSettings() {
    dialog.ShowConfirm("Reset Settings", "Are you sure you want to reset all settings to default values?", func(confirmed bool) {
        if confirmed {
            t.cfg.ResetToDefaults()
            t.statusLabel.SetText("✅ Settings reset to defaults!")
            t.statusLabel.Importance = widget.SuccessImportance
            
            // Refresh UI
            t.sensitivitySlider.Value = t.cfg.Sensitivity
            t.themeSelect.SetSelected(t.cfg.Theme)
            // Update other controls...
            
            dialog.ShowInformation("Settings Reset", "All settings have been reset to default values.", fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *SettingsTab) exportSettings() {
    dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
        if err == nil && writer != nil {
            defer writer.Close()
            data := t.cfg.ToJSON()
            writer.Write([]byte(data))
            dialog.ShowInformation("Export Complete", "Settings exported successfully.", fyne.CurrentApp().Driver().AllWindows()[0])
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *SettingsTab) importSettings() {
    dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
        if err == nil && reader != nil {
            defer reader.Close()
            buf := make([]byte, 1024*1024)
            n, _ := reader.Read(buf)
            if err := t.cfg.FromJSON(string(buf[:n])); err == nil {
                dialog.ShowInformation("Import Complete", "Settings imported successfully. Please restart the app for changes to take effect.", fyne.CurrentApp().Driver().AllWindows()[0])
            } else {
                dialog.ShowError(err, fyne.CurrentApp().Driver().AllWindows()[0])
            }
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}
