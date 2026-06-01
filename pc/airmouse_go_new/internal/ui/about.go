package ui

import (
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
)

func showAboutDialog(parent fyne.Window) {
    content := container.NewVBox(
        widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewLabel("Version 3.0.0"),
        widget.NewLabel("University of Tehran – Embedded Systems Lab"),
        widget.NewSeparator(),
        widget.NewLabel("Multi‑protocol | AI Smoothing | Proximity Lock"),
    )
    dialog.ShowCustom("About", "Close", content, parent)
}