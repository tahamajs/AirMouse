package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/widget"
)

type ProximityTab struct{}

func NewProximityTab() (fyne.CanvasObject, *ProximityTab) {
	return widget.NewLabel("Proximity Tab (stub)"), &ProximityTab{}
}

func (t *ProximityTab) Stop() {}