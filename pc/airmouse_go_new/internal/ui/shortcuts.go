package ui

import (
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
)

// ShowShortcutsDialog displays a comprehensive list of keyboard shortcuts.
func ShowShortcutsDialog(parent fyne.Window) {
    // Create shortcut categories
    generalShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("General", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + S      - Start Server"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + S  - Stop Server"),
        widget.NewLabel("  ⌘/Ctrl + R      - Restart Server"),
        widget.NewLabel("  ⌘/Ctrl + Q      - Quit Application"),
        widget.NewLabel("  ⌘/Ctrl + ,      - Open Settings"),
    )
    
    navigationShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Navigation", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + 1      - Dashboard Tab"),
        widget.NewLabel("  ⌘/Ctrl + 2      - Devices Tab"),
        widget.NewLabel("  ⌘/Ctrl + 3      - Network Tab"),
        widget.NewLabel("  ⌘/Ctrl + 4      - Gestures Tab"),
        widget.NewLabel("  ⌘/Ctrl + 5      - Proximity Tab"),
        widget.NewLabel("  ⌘/Ctrl + 6      - Analytics Tab"),
        widget.NewLabel("  ⌘/Ctrl + 7      - Settings Tab"),
        widget.NewLabel("  ⌘/Ctrl + 8      - Logs Tab"),
    )
    
    actionShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Actions", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + P      - Show Pairing QR Code"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + P  - Open Pairing Wizard"),
        widget.NewLabel("  ⌘/Ctrl + L      - Clear Logs"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + L  - Export Logs"),
        widget.NewLabel("  ⌘/Ctrl + H      - Show/Hide Window"),
        widget.NewLabel("  F1              - Help"),
        widget.NewLabel("  F5              - Refresh"),
    )
    
    deviceShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Device Control", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + D      - Disconnect All Devices"),
        widget.NewLabel("  ⌘/Ctrl + B      - Block Device"),
        widget.NewLabel("  ⌘/Ctrl + U      - Unblock Device"),
    )
    
    developmentShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Development", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + D  - Debug Mode"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + R  - Reload Config"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + T  - Run Tests"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + C  - Clear Cache"),
    )
    
    content := container.NewGridWithColumns(2,
        container.NewVBox(generalShortcuts, navigationShortcuts),
        container.NewVBox(actionShortcuts, deviceShortcuts, developmentShortcuts),
    )
    
    dialog.ShowCustom("Keyboard Shortcuts", "Close", container.NewScroll(content), parent)
}

// ShowContextMenuShortcuts displays shortcuts for context menus
func ShowContextMenuShortcuts(parent fyne.Window) {
    content := container.NewVBox(
        widget.NewLabelWithStyle("Context Menu Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("  Right Click     - Open Context Menu"),
        widget.NewLabel("  ⌘/Ctrl + Click  - Force Touch / Secondary Action"),
        widget.NewLabel("  Double Click    - Quick Action"),
    )
    dialog.ShowCustom("Context Menu Shortcuts", "Close", content, parent)
}