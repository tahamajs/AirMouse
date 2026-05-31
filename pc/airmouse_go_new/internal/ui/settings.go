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
	root                 fyne.CanvasObject
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
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
	tab := &SettingsTab{}

	tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
	tab.sensitivitySlider.Step = 0.01
	tab.sensitivitySlider.SetValue(cfg.Sensitivity)
	tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	tab.sensitivitySlider.OnChanged = func(v float64) {
		cfg.SetSensitivity(v)
		mouse.SetSensitivity(v)
		tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	tab.themeSelect = widget.NewSelect([]string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"}, func(theme string) {
		cfg.SetTheme(theme)
		if app := fyne.CurrentApp(); app != nil {
			app.Settings().SetTheme(getThemeByName(theme))
		}
	})
	tab.themeSelect.SetSelected(cfg.Theme)

	tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		_ = cfg.Save()
		if app := fyne.CurrentApp(); app != nil && len(app.Driver().AllWindows()) > 0 {
			app.Driver().AllWindows()[0].SetAlwaysOnTop(b)
		}
	})
	tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) { mouse.SetSmoothing(b) })
	tab.smoothingCheck.SetChecked(true)

	tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) { mouse.SetAcceleration(b, 1.5) })
	tab.accelCheck.SetChecked(true)

	tab.aiCheck = widget.NewCheck("AI Smoothing", func(b bool) {
		mouse.EnableAISmoothing(b)
		cfg.EnableAISmoothing = b
		_ = cfg.Save()
	})
	tab.aiCheck.SetChecked(cfg.EnableAISmoothing)

	tab.predictiveCheck = widget.NewCheck("Predictive Movement", func(b bool) {
		cfg.SetPredictiveEnabled(b)
		mouse.EnablePredictive(b)
	})
	tab.predictiveCheck.SetChecked(cfg.EnablePredictive)

	tab.predictiveBlendLabel = widget.NewLabel(fmt.Sprintf("Prediction blend: %.2f", cfg.PredictiveBlendFactor))
	tab.predictiveBlendSlider = widget.NewSlider(0, 1)
	tab.predictiveBlendSlider.Step = 0.05
	tab.predictiveBlendSlider.SetValue(cfg.PredictiveBlendFactor)
	tab.predictiveBlendSlider.OnChanged = func(v float64) {
		cfg.SetPredictiveBlendFactor(v)
		mouse.SetPredictiveBlendFactor(v)
		tab.predictiveBlendLabel.SetText(fmt.Sprintf("Prediction blend: %.2f", v))
	}

	tab.personalizationCheck = widget.NewCheck("Enable Personalization", func(b bool) {
		cfg.EnablePersonalization = b
		_ = cfg.Save()
	})
	tab.personalizationCheck.SetChecked(cfg.EnablePersonalization)

	tab.personalizationLbl = widget.NewLabel(fmt.Sprintf("Buffer size: %d samples", cfg.PersonalizationBuffer))
	tab.personalizationBuf = widget.NewSlider(500, 5000)
	tab.personalizationBuf.Step = 100
	tab.personalizationBuf.SetValue(float64(cfg.PersonalizationBuffer))
	tab.personalizationBuf.OnChanged = func(v float64) {
		cfg.PersonalizationBuffer = int(v)
		tab.personalizationLbl.SetText(fmt.Sprintf("Buffer size: %d samples", cfg.PersonalizationBuffer))
		_ = cfg.Save()
	}

	tab.intervalLbl = widget.NewLabel(fmt.Sprintf("Retrain interval: %d seconds", cfg.PersonalizationInterval))
	tab.intervalSlider = widget.NewSlider(600, 86400)
	tab.intervalSlider.Step = 300
	tab.intervalSlider.SetValue(float64(cfg.PersonalizationInterval))
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

	tab.root = container.NewVBox(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Cursor Sensitivity"),
		tab.sensitivitySlider,
		tab.sensitivityLabel,
		widget.NewSeparator(),
		widget.NewLabel("Appearance"),
		tab.themeSelect,
		tab.alwaysOnTopCheck,
		widget.NewSeparator(),
		widget.NewLabel("Mouse Behaviour"),
		tab.smoothingCheck,
		tab.accelCheck,
		widget.NewSeparator(),
		widget.NewLabel("Prediction & AI"),
		tab.aiCheck,
		tab.predictiveCheck,
		tab.predictiveBlendSlider,
		tab.predictiveBlendLabel,
		widget.NewSeparator(),
		widget.NewLabel("Personalization"),
		tab.personalizationCheck,
		tab.personalizationBuf,
		tab.personalizationLbl,
		tab.intervalSlider,
		tab.intervalLbl,
		tab.autoSwapCheck,
	)
	return tab.root
}