
package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"
)

// ShowShortcutsDialog displays a list of keyboard shortcuts.
func ShowShortcutsDialog(parent fyne.Window) {
	content := widget.NewLabel(
		"Ctrl+S – Start Server\n" +
			"Ctrl+Shift+S – Stop Server\n" +
			"Ctrl+Q – Quit\n" +
			"Ctrl+R – Refresh\n" +
			"Ctrl+Shift+L – Clear Logs\n" +
			"Ctrl+Shift+P – Open Pairing Wizard",
	)
	dialog.ShowCustom("Keyboard Shortcuts", "Close", content, parent)
}
