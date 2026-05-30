package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/control/personalization"
)

type AnalyticsTab struct {
	root      fyne.CanvasObject
	stats     *widget.Label
	collector *personalization.DataCollector
}

func NewAnalyticsTab(collector *personalization.DataCollector) fyne.CanvasObject {
	tab := &AnalyticsTab{collector: collector}
	tab.stats = widget.NewLabel("Training data: 0 samples collected")

	refresh := func() {
		if tab.collector == nil {
			tab.stats.SetText("Training data: unavailable")
			return
		}
		tab.stats.SetText(fmt.Sprintf("Training data: %d samples collected", tab.collector.SampleCount()))
	}
	refresh()

	tab.root = container.NewVBox(
		widget.NewLabelWithStyle("Personalization Analytics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.stats,
		widget.NewButton("Refresh", refresh),
		widget.NewButton("Force Retrain Now", func() {
			if tab.collector != nil {
				go func() { _ = tab.collector.ForceFineTune() }()
			}
		}),
	)
	return tab.root
}
