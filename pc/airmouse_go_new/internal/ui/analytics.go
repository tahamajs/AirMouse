package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/personalization"
)

type AnalyticsTab struct {
	statsLabel     *widget.Label
	lastTrainLabel *widget.Label
	forceBtn       *widget.Button
	collector      *personalization.DataCollector
}

func NewAnalyticsTab(collector *personalization.DataCollector) fyne.CanvasObject {
	tab := &AnalyticsTab{collector: collector}
	tab.statsLabel = widget.NewLabel("Training data: 0 samples collected")
	tab.lastTrainLabel = widget.NewLabel("Last fine‑tuning: never")

	refresh := func() {
		if tab.collector == nil {
			tab.statsLabel.SetText("Personalization not enabled")
			tab.lastTrainLabel.SetText("")
			return
		}
		tab.statsLabel.SetText(fmt.Sprintf("Training data: %d samples collected", tab.collector.SampleCount()))
		last := tab.collector.LastFineTune()
		if last.IsZero() {
			tab.lastTrainLabel.SetText("Last fine‑tuning: never")
		} else {
			tab.lastTrainLabel.SetText(fmt.Sprintf("Last fine‑tuning: %s", last.Format("2006-01-02 15:04:05")))
		}
	}
	refresh()

	refreshBtn := widget.NewButton("Refresh", refresh)
	tab.forceBtn = widget.NewButton("Force Retrain Now", func() {
		if tab.collector != nil {
			tab.forceBtn.Disable()
			tab.statsLabel.SetText("Training started...")
			go func() {
				_ = tab.collector.ForceFineTune()
				refresh()
				tab.forceBtn.Enable()
			}()
		}
	})

	return container.NewVBox(
		widget.NewLabelWithStyle("Personalization Analytics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.statsLabel,
		tab.lastTrainLabel,
		container.NewHBox(refreshBtn, tab.forceBtn),
	)
}
