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
	sensitivitySlider *widget.Slider
	sensitivityLabel  *widget.Label
	themeSelect       *widget.Select
	alwaysOnTopCheck  *widget.Check
	smoothingCheck    *widget.Check
	accelCheck        *widget.Check
}

func NewSettingsTab(cfg *config.Config, mouse control.MouseController) fyne.CanvasObject {
	tab := &SettingsTab{}

	tab.sensitivitySlider = widget.NewSlider(0.2, 2.0)
	tab.sensitivitySlider.Value = cfg.Sensitivity
	tab.sensitivityLabel = widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	tab.sensitivitySlider.OnChanged = func(v float64) {
		cfg.SetSensitivity(v)
		mouse.SetSensitivity(v)
		tab.sensitivityLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
	}

	tab.themeSelect = widget.NewSelect(
		[]string{"dark", "light", "pure_black", "high_contrast", "ocean", "sunset", "forest", "purple", "cherry", "neon", "lavender", "mint", "peach", "sky"},
		func(theme string) {
			cfg.SetTheme(theme)
			app := fyne.CurrentApp()
			app.Settings().SetTheme(getThemeByName(theme))
		},
	)
	tab.themeSelect.SetSelected(cfg.Theme)

	tab.alwaysOnTopCheck = widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		cfg.Save()
		win := fyne.CurrentApp().Driver().AllWindows()[0]
		win.SetAlwaysOnTop(b)
	})
	tab.alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	tab.smoothingCheck = widget.NewCheck("Mouse Smoothing (EMA)", func(b bool) {
		mouse.SetSmoothing(b)
	})
	tab.smoothingCheck.SetChecked(true)

	tab.accelCheck = widget.NewCheck("Mouse Acceleration", func(b bool) {
		mouse.SetAcceleration(b, 1.5)
	})
	tab.accelCheck.SetChecked(true)

	return container.NewVBox(
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
	)
}