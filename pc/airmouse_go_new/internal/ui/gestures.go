//go:build gui

package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// GesturesTab allows users to manage saved gesture templates.
type GesturesTab struct {
	list      *widget.List
	templates []string
	selected  int
}

// NewGesturesTab creates a tab for gesture management.
func NewGesturesTab() fyne.CanvasObject {
	tab := &GesturesTab{
		templates: []string{"LeftSwipe", "RightSwipe", "CircleCW", "ThumbsUp"},
	}
	tab.list = widget.NewList(
		func() int { return len(tab.templates) },
		func() fyne.CanvasObject { return widget.NewLabel("template") },
		func(id int, obj fyne.CanvasObject) {
			obj.(*widget.Label).SetText(tab.templates[id])
		},
	)
	tab.list.OnSelected = func(id int) {
		tab.selected = id
		// Show details or delete option
	}

	deleteBtn := widget.NewButton("Delete Selected", func() {
		if id := tab.selected; id >= 0 && id < len(tab.templates) {
			tab.templates = append(tab.templates[:id], tab.templates[id+1:]...)
			tab.selected = -1
			tab.list.Refresh()
		}
	})
	importBtn := widget.NewButton("Import Template", func() {
		// Open file dialog to load a JSON template
	})
	trainBtn := widget.NewButton("Train New Gesture", func() {
		// Show recording dialog
	})

	return container.NewBorder(
		widget.NewLabelWithStyle("Gesture Templates", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		container.NewHBox(importBtn, trainBtn, deleteBtn),
		nil, nil,
		tab.list,
	)
}
