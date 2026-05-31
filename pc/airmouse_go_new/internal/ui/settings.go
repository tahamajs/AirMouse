package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/domain/service"
)

type SettingsTab struct {
	sensitivitySlider *widget.Slider
	sensitivityLabel  *widget.Label
	clickSlider       *widget.Slider
	clickLabel        *widget.Label
	doubleSlider      *widget.Slider
	doubleLabel       *widget.Label
	scrollSlider      *widget.Slider
	scrollLabel       *widget.Label
	tiltSlider        *widget.Slider
	tiltLabel         *widget.Label
	themeSelect       *widget.Select
	alwaysOnTopCheck  *widget.Check
	smoothingCheck    *widget.Check
	accelCheck        *widget.Check
	aiCheck           *widget.Check
	predictiveCheck   *widget.Check
	blendSlider       *widget.Slider
	blendLabel        *widget.Label
	hapticCheck       *widget.Check
}

func NewSettingsTab(cfg *config.Config, mouseSvc *service.MouseService) fyne.CanvasObject {
	tab := &SettingsTab{}

	// Sensitivity
	tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
	tab.sensitivitySlider.Step = 0.01
	tab.sensitivitySlider.Value = cfg.Sensitivity
	tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	tab.sensitivitySlider.OnChanged = func(v float64) {
		cfg.SetSensitivity(v)
		mouseSvc.SetSensitivity(v)
		tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	// Click threshold
	tab.clickSlider = widget.NewSlider(0, 20)
	tab.clickSlider.Step = 0.5
	tab.clickSlider.Value = cfg.ClickThreshold
	tab.clickLabel = widget.NewLabel(fmt.Sprintf("Click threshold: %.1f rad/s", cfg.ClickThreshold))
	tab.clickSlider.OnChanged = func(v float64) {
		cfg.ClickThreshold = v
		cfg.Save()
		tab.clickLabel.SetText(fmt.Sprintf("Click threshold: %.1f rad/s", v))
	}

	// Double click interval
	tab.doubleSlider = widget.NewSlider(200, 1000)
	tab.doubleSlider.Step = 10
	tab.doubleSlider.Value = float64(cfg.DoubleClickInterval)
	tab.doubleLabel = widget.NewLabel(fmt.Sprintf("Double click interval: %d ms", cfg.DoubleClickInterval))
	tab.doubleSlider.OnChanged = func(v float64) {
		cfg.DoubleClickInterval = int64(v)
		cfg.Save()
		tab.doubleLabel.SetText(fmt.Sprintf("Double click interval: %d ms", int(v)))
	}

	// Scroll threshold
	tab.scrollSlider = widget.NewSlider(0, 15)
	tab.scrollSlider.Step = 0.2
	tab.scrollSlider.Value = cfg.ScrollThreshold
	tab.scrollLabel = widget.NewLabel(fmt.Sprintf("Scroll threshold: %.1f m/s²", cfg.ScrollThreshold))
	tab.scrollSlider.OnChanged = func(v float64) {
		cfg.ScrollThreshold = v
		cfg.Save()
		tab.scrollLabel.SetText(fmt.Sprintf("Scroll threshold: %.1f m/s²", v))
	}

	// Right click tilt
	tab.tiltSlider = widget.NewSlider(0, 90)
	tab.tiltSlider.Step = 1
	tab.tiltSlider.Value = cfg.RightClickTilt
	tab.tiltLabel = widget.NewLabel(fmt.Sprintf("Right click tilt: %.0f°", cfg.RightClickTilt))
	tab.tiltSlider.OnChanged = func(v float64) {
		cfg.RightClickTilt = v
		cfg.Save()
		tab.tiltLabel.SetText(fmt.Sprintf("Right click tilt: %.0f°", v))
	}

	// Theme
	tab.themeSelect = widget.NewSelect(
		[]string{"dark", "light", "pure_black"},
		func(theme string) {
			cfg.SetTheme(theme)
			fyne.CurrentApp().Settings().SetTheme(getThemeByName(theme))
		},
	)
	tab.themeSelect.SetSelected(cfg.Theme)

	// Always on top
	tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		cfg.Save()
		win := fyne.CurrentApp().Driver().AllWindows()[0]
		win.SetAlwaysOnTop(b)
	})
	tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	// Smoothing
	tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		mouseSvc.SetSmoothing(b)
	})
	tab.smoothingCheck.SetChecked(true)

	// Acceleration
	tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		mouseSvc.SetAcceleration(b, 1.5)
	})
	tab.accelCheck.SetChecked(true)

	// AI smoothing
	tab.aiCheck = widget.NewCheck("AI Smoothing (RNN)", func(b bool) {
		cfg.EnableAISmoothing = b
		cfg.Save()
	})
	tab.aiCheck.SetChecked(cfg.EnableAISmoothing)

	// Predictive movement
	tab.predictiveCheck = widget.NewCheck("Predictive Movement (Kalman)", func(b bool) {
		cfg.EnablePredictive = b
		cfg.Save()
	})
	tab.predictiveCheck.SetChecked(cfg.EnablePredictive)

	tab.blendSlider = widget.NewSlider(0, 1)
	tab.blendSlider.Step = 0.05
	tab.blendSlider.Value = cfg.PredictiveBlendFactor
	tab.blendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", cfg.PredictiveBlendFactor))
	tab.blendSlider.OnChanged = func(v float64) {
		cfg.SetPredictiveBlendFactor(v)
		tab.blendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
	}

	// Haptic feedback
	tab.hapticCheck = widget.NewCheck("Haptic Feedback", func(b bool) {
		cfg.HapticEnabled = b
		cfg.Save()
	})
	tab.hapticCheck.SetChecked(cfg.HapticEnabled)

	return container.NewVBox(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Cursor Sensitivity"),
		tab.sensitivitySlider, tab.sensitivityLabel,
		widget.NewSeparator(),
		widget.NewLabel("Gestures"),
		tab.clickSlider, tab.clickLabel,
		tab.doubleSlider, tab.doubleLabel,
		tab.scrollSlider, tab.scrollLabel,
		tab.tiltSlider, tab.tiltLabel,
		tab.hapticCheck,
		widget.NewSeparator(),
		widget.NewLabel("Appearance"),
		tab.themeSelect, tab.alwaysOnTopCheck,
		widget.NewSeparator(),
		widget.NewLabel("Mouse Processing"),
		tab.smoothingCheck, tab.accelCheck, tab.aiCheck,
		widget.NewSeparator(),
		widget.NewLabel("Latency Compensation"),
		tab.predictiveCheck, tab.blendSlider, tab.blendLabel,
	)
}