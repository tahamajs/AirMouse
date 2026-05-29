package ui

import (
    "fmt"
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"
    "airmouse-go/internal/control/personalization"
)

type AnalyticsTab struct {
    statsLabel *widget.Label
    collector  *personalization.DataCollector
}

func NewAnalyticsTab(collector *personalization.DataCollector) fyne.CanvasObject {
    tab := &AnalyticsTab{collector: collector}
    tab.statsLabel = widget.NewLabel("Training data: 0 samples collected")
    return container.NewVBox(
        widget.NewLabelWithStyle("Personalization Analytics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        tab.statsLabel,
        widget.NewButton("Force Retrain Now", func() {
            go tab.collector.ForceFineTune()
        }),
        widget.NewButton("View Personalization Log", func() { showLogDialog() }),
    )
}