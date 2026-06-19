package ui

import (
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
)

// ShowShortcutsDialog displays a comprehensive list of keyboard shortcuts
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
        widget.NewLabel("  ⌘/Ctrl + Tab    - Next Tab"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + Tab - Previous Tab"),
    )
    
    actionShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Actions", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + P      - Show Pairing QR Code"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + P  - Open Pairing Wizard"),
        widget.NewLabel("  ⌘/Ctrl + L      - Clear Logs"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + L  - Export Logs"),
        widget.NewLabel("  ⌘/Ctrl + H      - Show/Hide Window"),
        widget.NewLabel("  F1              - Help / Shortcuts"),
        widget.NewLabel("  F5              - Refresh"),
        widget.NewLabel("  Esc             - Close Dialog"),
    )
    
    deviceShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Device Control", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + D      - Disconnect All Devices"),
        widget.NewLabel("  ⌘/Ctrl + B      - Block Selected Device"),
        widget.NewLabel("  ⌘/Ctrl + U      - Unblock Device"),
        widget.NewLabel("  ⌘/Ctrl + R      - Rename Device"),
    )
    
    developmentShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Development", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + D  - Toggle Debug Mode"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + R  - Reload Configuration"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + T  - Run Tests"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + C  - Clear Cache"),
        widget.NewLabel("  ⌘/Ctrl + ⇧ + M  - Show Metrics"),
    )
    
    mouseShortcuts := container.NewVBox(
        widget.NewLabelWithStyle("Mouse Controls", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewLabel("  ⌘/Ctrl + Click  - Force Click"),
        widget.NewLabel("  ⌘/Ctrl + Scroll - Zoom"),
        widget.NewLabel("  Middle Click     - Quick Action"),
        widget.NewLabel("  Right Click      - Context Menu"),
    )
    
    // Main content with two columns
    content := container.NewGridWithColumns(2,
        container.NewVBox(generalShortcuts, navigationShortcuts, mouseShortcuts),
        container.NewVBox(actionShortcuts, deviceShortcuts, developmentShortcuts),
    )
    
    // Wrap in scroll
    scrollContent := container.NewVBox(
        widget.NewLabelWithStyle("Keyboard Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        content,
    )
    
    dialog.ShowCustom("Shortcuts", "Close", container.NewScroll(scrollContent), parent)
}

// ShowContextMenuShortcuts displays shortcuts for context menus
func ShowContextMenuShortcuts(parent fyne.Window) {
    content := container.NewVBox(
        widget.NewLabelWithStyle("Context Menu Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("  Right Click           - Open Context Menu"),
        widget.NewLabel("  ⌘/Ctrl + Click        - Force Touch / Secondary Action"),
        widget.NewLabel("  Double Click          - Quick Action"),
        widget.NewLabel("  ⌘/Ctrl + Right Click  - Extended Menu"),
        widget.NewLabel("  Shift + Right Click   - System Menu"),
    )
    dialog.ShowCustom("Context Menu Shortcuts", "Close", content, parent)
}

// ShowMouseShortcuts displays mouse-specific shortcuts
func ShowMouseShortcuts(parent fyne.Window) {
    content := container.NewVBox(
        widget.NewLabelWithStyle("Mouse Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("  Left Click     - Select / Click"),
        widget.NewLabel("  Right Click    - Context Menu"),
        widget.NewLabel("  Middle Click   - Quick Action / Paste"),
        widget.NewLabel("  Scroll Up      - Scroll Up / Volume Up"),
        widget.NewLabel("  Scroll Down    - Scroll Down / Volume Down"),
        widget.NewLabel("  Ctrl + Scroll  - Zoom In/Out"),
        widget.NewLabel("  Shift + Scroll - Horizontal Scroll"),
    )
    dialog.ShowCustom("Mouse Shortcuts", "Close", content, parent)
}

// GetShortcutText returns formatted shortcut text for menu items
func GetShortcutText(modifier, key string) string {
    return modifier + " + " + key
}
