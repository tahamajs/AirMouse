package ui

import (
	"fmt"
	"strconv"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ProximityTab provides UI for Bluetooth proximity lock/unlock settings.
type ProximityTab struct {
	enableCheck     *widget.Check
	nearSlider      *widget.Slider
	farSlider       *widget.Slider
	nearLabel       *widget.Label
	farLabel        *widget.Label
	calibrateBtn    *widget.Button
	statusLabel     *widget.Label
	currentDistance *widget.Label
}

// NewProximityTab creates a new tab for proximity settings.
func NewProximityTab() fyne.CanvasObject {
	tab := &ProximityTab{}

	tab.enableCheck = widget.NewCheck("Enable Proximity Lock/Unlock", func(bool) {})
	tab.nearSlider = widget.NewSlider(0.5, 5.0)
	tab.nearSlider.Step = 0.1
	tab.nearSlider.Value = 2.0
	tab.farSlider = widget.NewSlider(1.0, 10.0)
	tab.farSlider.Step = 0.2
	tab.farSlider.Value = 4.0
	tab.nearLabel = widget.NewLabel(fmt.Sprintf("Near threshold: %.1f m", tab.nearSlider.Value))
	tab.farLabel = widget.NewLabel(fmt.Sprintf("Far threshold: %.1f m", tab.farSlider.Value))
	tab.nearSlider.OnChanged = func(v float64) {
		tab.nearLabel.SetText(fmt.Sprintf("Near threshold: %.1f m", v))
	}
	tab.farSlider.OnChanged = func(v float64) {
		tab.farLabel.SetText(fmt.Sprintf("Far threshold: %.1f m", v))
	}

	tab.calibrateBtn = widget.NewButton("Calibrate", func() {
		// In real implementation, show calibration wizard
	})
	tab.statusLabel = widget.NewLabel("Proximity service stopped")
	tab.currentDistance = widget.NewLabel("Distance: -- m")

	return container.NewVBox(
		widget.NewLabelWithStyle("Proximity Security", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.enableCheck,
		tab.nearLabel,
		tab.nearSlider,
		tab.farLabel,
		tab.farSlider,
		tab.calibrateBtn,
		tab.currentDistance,
		tab.statusLabel,
	)
}
