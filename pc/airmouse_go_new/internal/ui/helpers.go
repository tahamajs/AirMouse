package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"strings"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"
	"github.com/skip2/go-qrcode"
)

// ------------------------------------------------------------
// Dialog helpers
// ------------------------------------------------------------

// ShowError displays an error dialog with the given message.
func ShowError(parent fyne.Window, err error) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowError(err, parent)
}

// ShowInfo displays an information dialog.
func ShowInfo(parent fyne.Window, title, message string) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowInformation(title, message, parent)
}

// ShowWarning displays a warning dialog.
func ShowWarning(parent fyne.Window, title, message string) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowInformation("⚠️ "+title, message, parent)
}

// ShowSuccess displays a success notification.
func ShowSuccess(parent fyne.Window, title, message string) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowInformation("✓ "+title, message, parent)
}

// ConfirmAction displays a yes/no confirmation dialog.
func ConfirmAction(parent fyne.Window, title, message string, onConfirm func()) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowConfirm(title, message, func(confirmed bool) {
		if confirmed {
			onConfirm()
		}
	}, parent)
}

// ShowCustomDialog displays a custom dialog.
func ShowCustomDialog(parent fyne.Window, title string, content fyne.CanvasObject) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	dialog.ShowCustom(title, "Close", content, parent)
}

// ShowProgressDialog displays a progress dialog.
func ShowProgressDialog(parent fyne.Window, title, message string) *dialog.ProgressDialog {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return nil
		}
	}
	progress := dialog.NewProgress(title, message, parent)
	progress.Show()
	return progress
}

// ------------------------------------------------------------
// Formatting helpers
// ------------------------------------------------------------

// FormatBytes converts bytes to a human‑readable string (exported).
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

// FormatDurationShort formats a duration in a compact form (exported).
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

// FormatDuration formats a duration in a full human‑readable form (exported).
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

// ------------------------------------------------------------
// UI component helpers
// ------------------------------------------------------------

// CreateStatusBadge creates a colored status badge.
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

// CreateQRImage creates a QR code image from data.
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

// ------------------------------------------------------------
// String helpers
// ------------------------------------------------------------

// JoinStrings joins a slice of strings with a separator.
func JoinStrings(strs []string, sep string) string {
	return strings.Join(strs, sep)
}

// TruncateString truncates a string to max length.
func TruncateString(s string, maxLen int) string {
	if len(s) <= maxLen {
		return s
	}
	return s[:maxLen-3] + "..."
}

// containsIgnoreCase checks if substr appears in s (case‑insensitive).
// This is used internally by devices.go and gestures.go.
func containsIgnoreCase(s, substr string) bool {
	if len(substr) == 0 {
		return true
	}
	lowerS := toLower(s)
	lowerSub := toLower(substr)
	return len(lowerS) >= len(lowerSub) &&
		(lowerS == lowerSub ||
			(len(lowerS) > len(lowerSub) && (lowerS[:len(lowerSub)] == lowerSub ||
				lowerS[len(lowerS)-len(lowerSub):] == lowerSub ||
				containsIgnoreCase(lowerS[1:], lowerSub))))
}

// toLower converts a string to lowercase (ASCII only).
func toLower(s string) string {
	b := []byte(s)
	for i := 0; i < len(b); i++ {
		if b[i] >= 'A' && b[i] <= 'Z' {
			b[i] += 32
		}
	}
	return string(b)
}

// ------------------------------------------------------------
// Window / platform helpers
// ------------------------------------------------------------

// GetPlatformModifier returns the appropriate modifier key for the platform.
func GetPlatformModifier() string {
	return "Ctrl"
}

// RunOnMain executes fn on the UI thread. It uses Fyne’s driver if available,
// otherwise it calls fn directly (which may not be safe, but is a fallback).
func RunOnMain(fn func()) {
	// The recommended way in Fyne v2 is to use the driver's RunOnMain.
	if driver, ok := fyne.CurrentApp().Driver().(interface{ RunOnMain(func()) }); ok {
		driver.RunOnMain(fn)
	} else {
		// Fallback – may cause issues if called from a goroutine.
		fn()
	}
}

// IsDarkTheme returns true if the current theme is dark.
func IsDarkTheme() bool {
	// This can be expanded to actually check the theme.
	return true
}

// CenterWindow centers a window on screen.
func CenterWindow(w fyne.Window) {
	w.CenterOnScreen()
}

// GetWindowSize returns the default window size.
func GetWindowSize() (width, height float32) {
	return 1400, 900
}

// getCurrentWindow returns the first application window, or nil if none.
// This is used internally by all dialog helpers.
func getCurrentWindow() fyne.Window {
	if fyne.CurrentApp() == nil {
		return nil
	}
	windows := fyne.CurrentApp().Driver().AllWindows()
	if len(windows) == 0 {
		return nil
	}
	return windows[0]
}

// ------------------------------------------------------------
// Type-safe join with custom separator (kept for compatibility)
// ------------------------------------------------------------

// joinStrings is a private alias for JoinStrings (used internally).
func joinStrings(strs []string, sep string) string {
	return JoinStrings(strs, sep)
}

// formatDurationShort is a private alias for FormatDurationShort.
func formatDurationShort(d time.Duration) string {
	return FormatDurationShort(d)
}