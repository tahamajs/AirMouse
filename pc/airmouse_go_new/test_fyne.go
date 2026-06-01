package main

import "fyne.io/fyne/v2/app"
import "fyne.io/fyne/v2/widget"

func main() {
    a := app.New()
    w := a.NewWindow("Test")
    w.SetContent(widget.NewLabel("Hello"))
    w.ShowAndRun()
}