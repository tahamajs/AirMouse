package main

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/widget"
)

func main() {
	a := app.New()
	w := a.NewWindow("Test")
	w.Resize(fyne.NewSize(400, 300))
	w.SetContent(widget.NewLabel("Hello Fyne"))
	w.ShowAndRun()
}
