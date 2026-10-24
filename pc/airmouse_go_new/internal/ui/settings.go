package ui

import (
	"fmt"
	"image/color"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ServerConfigInterface abstracts the server configuration bindings.
type ServerConfigInterface interface {
	GetEnableAISmoothing() bool
	SetEnableAISmoothing(bool)
	GetEnablePredictive() bool
	SetEnablePredictive(bool)
	GetPredictiveBlendFactor() float64
	SetPredictiveBlendFactor(float64)
	GetPort() int
	SetPort(int)
	Save() error
}

// BuildSettingsTab creates the Settings tab for the 6-tab Fyne Desktop UI.
// Binds checkboxes and sliders directly to config fields with live updates.
func BuildSettingsTab(cfg ServerConfigInterface, onConfigChanged func()) fyne.CanvasObject {
	header := canvas.NewText("Server & Motion Predictor Settings", color.White)
	header.TextSize = 18
	header.TextStyle = fyne.TextStyle{Bold: true}

	// 1. AI Smoothing Checkbox
	aiCheck := widget.NewCheck("Enable AI / Neural Mouse Smoothing (ONNX)", func(checked bool) {
		cfg.SetEnableAISmoothing(checked)
		if onConfigChanged != nil {
			onConfigChanged()
		}
	})
	aiCheck.SetChecked(cfg.GetEnableAISmoothing())

	// 2. Predictive Filter (Kalman) Checkbox
	predictiveCheck := widget.NewCheck("Enable Predictive Kalman Filter 2D", func(checked bool) {
		cfg.SetEnablePredictive(checked)
		if onConfigChanged != nil {
			onConfigChanged()
		}
	})
	predictiveCheck.SetChecked(cfg.GetEnablePredictive())

	// 3. Predictive Blend Factor Slider
	blendLabel := widget.NewLabel(fmt.Sprintf("Prediction Blend Factor: %.2f", cfg.GetPredictiveBlendFactor()))
	blendSlider := widget.NewSlider(0.0, 1.0)
	blendSlider.Step = 0.05
	blendSlider.SetValue(cfg.GetPredictiveBlendFactor())
	blendSlider.OnChanged = func(val float64) {
		cfg.SetPredictiveBlendFactor(val)
		blendLabel.SetText(fmt.Sprintf("Prediction Blend Factor: %.2f", val))
		if onConfigChanged != nil {
			onConfigChanged()
		}
	}

	// 4. Port Configuration Entry
	portEntry := widget.NewEntry()
	portEntry.SetText(fmt.Sprintf("%d", cfg.GetPort()))
	portFormItem := widget.NewFormItem("TCP/TLS Port", portEntry)

	// Save Button
	statusLabel := widget.NewLabel("")
	saveBtn := widget.NewButton("Save Configuration", func() {
		var port int
		if _, err := fmt.Sscanf(portEntry.Text, "%d", &port); err == nil && port > 1024 && port < 65535 {
			cfg.SetPort(port)
		}
		if err := cfg.Save(); err != nil {
			statusLabel.SetText("Failed to save: " + err.Error())
		} else {
			statusLabel.SetText("Configuration saved successfully!")
		}
	})

	form := widget.NewForm(portFormItem)

	settingsContent := container.NewVBox(
		header,
		widget.NewSeparator(),
		widget.NewLabel("Signal Processing & Neural Smoothing:"),
		aiCheck,
		predictiveCheck,
		blendLabel,
		blendSlider,
		widget.NewSeparator(),
		widget.NewLabel("Network Configuration:"),
		form,
		saveBtn,
		statusLabel,
	)

	return container.NewPadded(settingsContent)
}
