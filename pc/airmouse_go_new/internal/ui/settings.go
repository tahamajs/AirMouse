package ui

import (
    "fmt"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
)

type SettingsTab struct {
    sensitivitySlider    *widget.Slider
    sensitivityLabel     *widget.Label
    themeSelect          *widget.Select
    alwaysOnTopCheck     *widget.Check
    smoothingCheck       *widget.Check
    accelCheck           *widget.Check
    aiCheck              *widget.Check
    predictiveCheck      *widget.Check
    predictiveBlendSlider *widget.Slider
    predictiveBlendLabel *widget.Label
    personalizationCheck *widget.Check
    personalizationBuf   *widget.Slider
    personalizationLbl   *widget.Label
    intervalSlider       *widget.Slider
    intervalLbl          *widget.Label
    autoSwapCheck        *widget.Check
    proximityNearSlider  *widget.Slider
    proximityNearLabel   *widget.Label
    proximityFarSlider   *widget.Slider
    proximityFarLabel    *widget.Label
    jitterEnableCheck    *widget.Check
    jitterBlendSlider    *widget.Slider
    jitterBlendLabel     *widget.Label
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
    tab := &SettingsTab{}

    // Sensitivity
    tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
    tab.sensitivitySlider.Step = 0.01
    tab.sensitivitySlider.Value = cfg.Sensitivity
    tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
    tab.sensitivitySlider.OnChanged = func(v float64) {
        cfg.SetSensitivity(v)
        mouse.SetSensitivity(v)
        tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
    }

    // Theme
    themes := []string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"}
    tab.themeSelect = widget.NewSelect(themes, func(theme string) {
        cfg.SetTheme(theme)
        if app := fyne.CurrentApp(); app != nil {
            app.Settings().SetTheme(getThemeByName(theme))
        }
    })
    tab.themeSelect.SetSelected(cfg.Theme)

    // Always on Top (Fyne ندارد، فقط ذخیره می‌کنیم)
    tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
        cfg.AlwaysOnTop = b
        _ = cfg.Save()
    })
    tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

    // Smoothing (EMA)
    tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
        mouse.SetSmoothing(b)
    })
    tab.smoothingCheck.SetChecked(true)

    // Acceleration
    tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
        mouse.SetAcceleration(b, 1.5)
    })
    tab.accelCheck.SetChecked(true)

    // AI Smoothing
    tab.aiCheck = widget.NewCheck("AI Smoothing (ONNX)", func(b bool) {
        mouse.EnableAISmoothing(b)
        cfg.EnableAISmoothing = b
        _ = cfg.Save()
    })
    tab.aiCheck.SetChecked(cfg.EnableAISmoothing)

    // Predictive Movement (Kalman)
    tab.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman)", func(b bool) {
        cfg.SetPredictiveEnabled(b)
        mouse.EnablePredictive(b)
    })
    tab.predictiveCheck.SetChecked(cfg.EnablePredictive)

    tab.predictiveBlendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", cfg.PredictiveBlendFactor))
    tab.predictiveBlendSlider = widget.NewSlider(0, 1)
    tab.predictiveBlendSlider.Step = 0.05
    tab.predictiveBlendSlider.Value = cfg.PredictiveBlendFactor
    tab.predictiveBlendSlider.OnChanged = func(v float64) {
        cfg.SetPredictiveBlendFactor(v)
        mouse.SetPredictiveBlendFactor(v)
        tab.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
    }

    // Personalization (Data collector)
    tab.personalizationCheck = widget.NewCheck("Enable Personalization", func(b bool) {
        cfg.EnablePersonalization = b
        _ = cfg.Save()
    })
    tab.personalizationCheck.SetChecked(cfg.EnablePersonalization)

    tab.personalizationLbl = widget.NewLabel(fmt.Sprintf("Buffer size: %d samples", cfg.PersonalizationBuffer))
    tab.personalizationBuf = widget.NewSlider(500, 5000)
    tab.personalizationBuf.Step = 100
    tab.personalizationBuf.Value = float64(cfg.PersonalizationBuffer)
    tab.personalizationBuf.OnChanged = func(v float64) {
        cfg.PersonalizationBuffer = int(v)
        tab.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", cfg.PersonalizationBuffer))
        _ = cfg.Save()
    }

    tab.intervalLbl = widget.NewLabel(fmt.Sprintf("Retrain interval: %d seconds", cfg.PersonalizationInterval))
    tab.intervalSlider = widget.NewSlider(600, 86400)
    tab.intervalSlider.Step = 300
    tab.intervalSlider.Value = float64(cfg.PersonalizationInterval)
    tab.intervalSlider.OnChanged = func(v float64) {
        cfg.PersonalizationInterval = int(v)
        tab.intervalLbl.SetText(fmt.Sprintf("Retrain interval: %d seconds", cfg.PersonalizationInterval))
        _ = cfg.Save()
    }

    tab.autoSwapCheck = widget.NewCheck("Auto‑swap trained model", func(b bool) {
        cfg.AutoSwapModel = b
        _ = cfg.Save()
    })
    tab.autoSwapCheck.SetChecked(cfg.AutoSwapModel)

    // Proximity thresholds (near/far)
    tab.proximityNearLabel = widget.NewLabel(fmt.Sprintf("Near threshold (unlock): %.1f m", cfg.ProximityNearThreshold))
    tab.proximityNearSlider = widget.NewSlider(0.5, 5.0)
    tab.proximityNearSlider.Step = 0.1
    tab.proximityNearSlider.Value = cfg.ProximityNearThreshold
    tab.proximityNearSlider.OnChanged = func(v float64) {
        cfg.ProximityNearThreshold = v
        _ = cfg.Save()
        tab.proximityNearLabel.SetText(fmt.Sprintf("Near threshold (unlock): %.1f m", v))
    }

    tab.proximityFarLabel = widget.NewLabel(fmt.Sprintf("Far threshold (lock): %.1f m", cfg.ProximityFarThreshold))
    tab.proximityFarSlider = widget.NewSlider(1.0, 10.0)
    tab.proximityFarSlider.Step = 0.2
    tab.proximityFarSlider.Value = cfg.ProximityFarThreshold
    tab.proximityFarSlider.OnChanged = func(v float64) {
        cfg.ProximityFarThreshold = v
        _ = cfg.Save()
        tab.proximityFarLabel.SetText(fmt.Sprintf("Far threshold (lock): %.1f m", v))
    }

    // Jitter compensation
    tab.jitterEnableCheck = widget.NewCheck("Enable Jitter Compensation", func(b bool) {
        cfg.EnableJitterCompensation = b
        _ = cfg.Save()
    })
    tab.jitterEnableCheck.SetChecked(cfg.EnableJitterCompensation)

    tab.jitterBlendLabel = widget.NewLabel(fmt.Sprintf("Jitter blend factor: %.2f", cfg.JitterBlendFactor))
    tab.jitterBlendSlider = widget.NewSlider(0, 1)
    tab.jitterBlendSlider.Step = 0.05
    tab.jitterBlendSlider.Value = cfg.JitterBlendFactor
    tab.jitterBlendSlider.OnChanged = func(v float64) {
        cfg.JitterBlendFactor = v
        _ = cfg.Save()
        tab.jitterBlendLabel.SetText(fmt.Sprintf("Jitter blend factor: %.2f", v))
    }

    // جمع‌آوری همه در یک VBox
    content := container.NewVBox(
        widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("Cursor Sensitivity"), tab.sensitivitySlider, tab.sensitivityLabel,
        widget.NewSeparator(),
        widget.NewLabel("Appearance"), tab.themeSelect, tab.alwaysOnTopCheck,
        widget.NewSeparator(),
        widget.NewLabel("Mouse Behaviour"), tab.smoothingCheck, tab.accelCheck,
        widget.NewSeparator(),
        widget.NewLabel("Prediction & AI"), tab.aiCheck, tab.predictiveCheck, tab.predictiveBlendSlider, tab.predictiveBlendLabel,
        widget.NewSeparator(),
        widget.NewLabel("Personalization"), tab.personalizationCheck, tab.personalizationBuf, tab.personalizationLbl, tab.intervalSlider, tab.intervalLbl, tab.autoSwapCheck,
        widget.NewSeparator(),
        widget.NewLabel("Proximity"), tab.proximityNearSlider, tab.proximityNearLabel, tab.proximityFarSlider, tab.proximityFarLabel,
        widget.NewSeparator(),
        widget.NewLabel("Network Jitter"), tab.jitterEnableCheck, tab.jitterBlendSlider, tab.jitterBlendLabel,
    )
    return container.NewScroll(content)
}