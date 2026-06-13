package ui

import (
    "fmt"
    "time"
    
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
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

// ShowSuccess displays a success notification
func ShowSuccess(parent fyne.Window, title, message string) {
    dialog.ShowInformation("✓ "+title, message, parent)
}

// ShowWarning displays a warning dialog
func ShowWarning(parent fyne.Window, title, message string) {
    dialog.ShowInformation("⚠️ "+title, message, parent)
}

// ShowCustomDialog displays a custom dialog
func ShowCustomDialog(parent fyne.Window, title string, content fyne.CanvasObject) {
    dialog.ShowCustom(title, "Close", content, parent)
}

// ShowProgressDialog displays a progress dialog
func ShowProgressDialog(parent fyne.Window, title, message string) *dialog.ProgressDialog {
    progress := dialog.NewProgress(title, message, parent)
    progress.Show()
    return progress
}

// FormatBytes converts bytes to human readable string
func FormatBytes(bytes int64) string {
    const unit = 1024
    if bytes < unit {
        return fmt.Sprintf("%d B", bytes)
    }
    div, exp := int64(unit), 0
    for n := bytes / unit; n >= unit; n /= unit {
        div *= unit
        exp++
    }
    return fmt.Sprintf("%.1f %cB", float64(bytes)/float64(div), "KMGTPE"[exp])
}

// FormatDurationShort formats duration in short form
func FormatDurationShort(d time.Duration) string {
    if d < time.Minute {
        return fmt.Sprintf("%ds", int(d.Seconds()))
    }
    if d < time.Hour {
        return fmt.Sprintf("%dm", int(d.Minutes()))
    }
    if d < 24*time.Hour {
        return fmt.Sprintf("%dh", int(d.Hours()))
    }
    return fmt.Sprintf("%dd", int(d.Hours()/24))
}

// CreateStatusBadge creates a colored status badge
func CreateStatusBadge(text string, statusType string) *widget.Label {
    badge := widget.NewLabel(text)
    badge.TextStyle = fyne.TextStyle{Bold: true}
    
    switch statusType {
    case "success":
        badge.Importance = widget.SuccessImportance
    case "warning":
        badge.Importance = widget.WarningImportance
    case "error":
        badge.Importance = widget.DangerImportance
    default:
        badge.Importance = widget.MediumImportance
    }
    
    return badge
}