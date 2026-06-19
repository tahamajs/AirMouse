package ui

import (
    "bytes"
    "fmt"
    "image/png"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
    "github.com/skip2/go-qrcode"
)

// ShowError displays an error dialog with the given message
func ShowError(parent fyne.Window, err error) {
    dialog.ShowError(err, parent)
}

// ShowInfo displays an information dialog
func ShowInfo(parent fyne.Window, title, message string) {
    dialog.ShowInformation(title, message, parent)
}

// ShowWarning displays a warning dialog
func ShowWarning(parent fyne.Window, title, message string) {
    dialog.ShowInformation("⚠️ "+title, message, parent)
}

// ShowSuccess displays a success notification
func ShowSuccess(parent fyne.Window, title, message string) {
    dialog.ShowInformation("✓ "+title, message, parent)
}

// ConfirmAction displays a yes/no confirmation dialog
func ConfirmAction(parent fyne.Window, title, message string, onConfirm func()) {
    dialog.ShowConfirm(title, message, func(confirmed bool) {
        if confirmed {
            onConfirm()
        }
    }, parent)
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

// FormatDuration formats duration in full form
func FormatDuration(d time.Duration) string {
    hours := int(d.Hours())
    minutes := int(d.Minutes()) % 60
    seconds := int(d.Seconds()) % 60
    
    if hours > 0 {
        return fmt.Sprintf("%dh %dm %ds", hours, minutes, seconds)
    }
    if minutes > 0 {
        return fmt.Sprintf("%dm %ds", minutes, seconds)
    }
    return fmt.Sprintf("%ds", seconds)
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

// CreateQRImage creates a QR code image from data
func CreateQRImage(data string, size int) *canvas.Image {
    pngBytes, err := qrcode.Encode(data, qrcode.High, size)
    if err != nil {
        return nil
    }
    
    img, err := png.Decode(bytes.NewReader(pngBytes))
    if err != nil {
        return nil
    }
    
    qrImage := canvas.NewImageFromImage(img)
    qrImage.FillMode = canvas.ImageFillOriginal
    return qrImage
}

// JoinStrings joins a slice of strings with a separator
func JoinStrings(strs []string, sep string) string {
    result := ""
    for i, s := range strs {
        if i > 0 {
            result += sep
        }
        result += s
    }
    return result
}

// TruncateString truncates a string to max length
func TruncateString(s string, maxLen int) string {
    if len(s) <= maxLen {
        return s
    }
    return s[:maxLen-3] + "..."
}

// GetPlatformModifier returns the appropriate modifier key for the platform
func GetPlatformModifier() string {
    // For macOS use "⌘", for Windows/Linux use "Ctrl"
    return "Ctrl"
}

// RunOnMain executes fn on the UI thread when available.
func RunOnMain(fn func()) {
    fn()
}

// IsDarkTheme returns true if the current theme is dark
func IsDarkTheme() bool {
    // This would check the current theme
    return true
}

// CenterWindow centers a window on screen
func CenterWindow(w fyne.Window) {
    w.CenterOnScreen()
}

// GetWindowSize returns the default window size
func GetWindowSize() (width, height float32) {
    return 1400, 900
}

func joinStrings(strs []string, sep string) string { return JoinStrings(strs, sep) }

func formatDurationShort(d time.Duration) string { return FormatDurationShort(d) }
