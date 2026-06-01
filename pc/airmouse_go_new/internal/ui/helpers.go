//go:build gui

package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/dialog"
)

// ShowError displays an error dialog with the given message.
func ShowError(parent fyne.Window, err error) {
	dialog.ShowError(err, parent)
}

// ShowInfo displays an information dialog.
func ShowInfo(parent fyne.Window, title, message string) {
	dialog.ShowInformation(title, message, parent)
}

// ConfirmAction displays a yes/no confirmation dialog.
func ConfirmAction(parent fyne.Window, title, message string, onConfirm func()) {
	dialog.ShowConfirm(title, message, func(confirmed bool) {
		if confirmed {
			onConfirm()
		}
	}, parent)
}
