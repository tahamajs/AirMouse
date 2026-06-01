package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ConnectionQualityWidget shows Wi‑Fi signal strength and latency.
type ConnectionQualityWidget struct {
	icon   *canvas.Image
	label  *widget.Label
	status string
}

// NewConnectionQualityWidget creates a widget that displays connection status.
func NewConnectionQualityWidget() *ConnectionQualityWidget {
	ic := canvas.NewImageFromResource(nil)
	ic.FillMode = canvas.ImageFillContain
	ic.SetMinSize(fyne.NewSize(24, 24))
	lbl := widget.NewLabel("Disconnected")
	return &ConnectionQualityWidget{
		icon:   ic,
		label:  lbl,
		status: "unknown",
	}
}

// SetQuality updates the widget with RSSI value and latency.
func (w *ConnectionQualityWidget) SetQuality(rssi int, latencyMs int64) {
	var icon fyne.Resource
	var text string
	if rssi > -50 {
		icon = nil // use a green dot resource
		text = fmt.Sprintf("Signal: Good (%d dBm) | Latency: %d ms", rssi, latencyMs)
	} else if rssi > -70 {
		icon = nil
		text = fmt.Sprintf("Signal: Fair (%d dBm) | Latency: %d ms", rssi, latencyMs)
	} else {
		icon = nil
		text = fmt.Sprintf("Signal: Poor (%d dBm) | Latency: %d ms", rssi, latencyMs)
	}
	if icon != nil {
		w.icon.Resource = icon
	}
	w.label.SetText(text)
}

// Widget returns the container for this component.
func (w *ConnectionQualityWidget) Widget() fyne.CanvasObject {
	return container.NewBorder(nil, nil, w.icon, nil, w.label)
}
