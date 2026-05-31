package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

func ShowAboutDialog(w fyne.Window) {
	dialog := widget.NewModalPopUp(
		container.NewVBox(
			widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
			widget.NewLabel("Version 3.0.0"),
			widget.NewLabel("University of Tehran – Embedded Systems Lab"),
			widget.NewSeparator(),
			widget.NewLabel("Multi-protocol mouse server\nTCP | WebSocket | UDP | Bluetooth"),
			widget.NewButton("OK", func() { dialog.Hide() }),
		),
		w.Canvas(),
	)
	dialog.Resize(fyne.NewSize(400, 300))
	dialog.Show()
}