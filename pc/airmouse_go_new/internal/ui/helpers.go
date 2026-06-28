package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"strings"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	"github.com/skip2/go-qrcode"
)

// ============================================================
// Cross-Platform Helpers
// ============================================================

// GetPlatformModifier returns the appropriate modifier key for the platform.
func GetPlatformModifier() string {
	return "Ctrl"
}

// IsDarkTheme returns true if the current theme is dark.
func IsDarkTheme() bool {
	return fyne.CurrentApp().Settings().Theme() == theme.DarkTheme()
}

// GetWindowSize returns a size that works well across platforms.
func GetWindowSize() (width, height float32) {
	return 1200, 800
}

// CenterWindow centers a window on screen.
func CenterWindow(w fyne.Window) {
	w.CenterOnScreen()
}

// ============================================================
// Responsive Layout Helpers
// ============================================================

// ResponsiveGrid creates a grid that adapts to window width.
func ResponsiveGrid(minColWidth float32, objects ...fyne.CanvasObject) *fyne.Container {
	return container.NewGridWrap(
		fyne.NewSize(minColWidth, minColWidth*0.75),
		objects...,
	)
}

// ============================================================
// Dialog Helpers
// ============================================================

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

// ============================================================
// Onboarding / Welcome Dialog
// ============================================================

// ShowWelcomeDialog displays a first-run welcome screen with guided setup.
func ShowWelcomeDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	content := widget.NewRichTextFromMarkdown(
		"# Welcome to Air Mouse Pro! 🚀\n\n" +
			"## Quick Start\n" +
			"1. **Start the server** – click the big green button on the Dashboard.\n" +
			"2. **Pair your phone** – scan the QR code with the Android app.\n" +
			"3. **Approve the device** – tap Approve in the Devices tab.\n" +
			"4. **Start controlling** – move your phone to control the cursor!\n\n" +
			"## Need Help?\n" +
			"- Press **F1** anytime for keyboard shortcuts.\n" +
			"- Visit the **Help** menu for documentation.\n" +
			"- Check the **Logs** tab for troubleshooting.\n\n" +
			"💡 *Pro tip: Grant Accessibility permission on macOS for mouse control.*",
	)
	dialog.ShowCustom("Welcome to Air Mouse Pro", "Get Started", content, parent)
}

// ============================================================
// Contextual Help System
// ============================================================

// ShowContextHelp displays help for the current context.
func ShowContextHelp(parent fyne.Window, context string) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	helpMap := map[string]string{
		"dashboard": "## Dashboard\n\n" +
			"**Start/Stop Server** – controls the service.\n" +
			"**Approval Center** – shows devices waiting for approval.\n" +
			"**Statistics** – live click and device counts.\n" +
			"**Quick Actions** – pause/resume movement, reset stats.",
		"devices": "## Devices\n\n" +
			"**Pending devices** – appear with an orange dot.\n" +
			"**Approve** – tap to allow mouse control.\n" +
			"**Block** – prevent a device from connecting.\n" +
			"**Rename** – give devices friendly names.",
		"network": "## Network\n\n" +
			"**IP Address** – select the correct network interface.\n" +
			"**Ports** – TCP (8080), WebSocket (8081), UDP discovery (8082).\n" +
			"**QR Code** – scan with the Android app to pair.\n" +
			"**Test Connection** – verify the server is reachable.",
		"gestures": "## Gestures\n\n" +
			"**Templates** – pre‑defined gestures for common actions.\n" +
			"**Add/Edit/Delete** – manage your own gesture library.\n" +
			"**Test Gesture** – try a gesture before using it.\n" +
			"**Import/Export** – share gesture templates.",
		"proximity": "## Proximity\n\n" +
			"**Enable** – turn on auto‑lock/unlock.\n" +
			"**Thresholds** – near (unlock) and far (lock) distances.\n" +
			"**Calibrate** – adjust for your environment.\n" +
			"**Lock/Unlock Now** – manual override.",
	}
	markdown, ok := helpMap[context]
	if !ok {
		markdown = "## Help\n\nNo specific help available for this context."
	}
	content := widget.NewRichTextFromMarkdown(markdown)
	dialog.ShowCustom("Help", "Close", content, parent)
}

// ============================================================
// Formatting Helpers
// ============================================================

// FormatBytes converts bytes to a human‑readable string.
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

// FormatDurationShort formats a duration in a compact form.
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

// FormatDuration formats a duration in a full human‑readable form.
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

// ============================================================
// UI Component Helpers
// ============================================================

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

// ============================================================
// String Helpers
// ============================================================

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

// ============================================================
// Window / Platform Helpers
// ============================================================

// RunOnMain executes fn on the UI thread using fyne.Do.
func RunOnMain(fn func()) {
	fyne.Do(fn)
}

// getCurrentWindow returns the first application window, or nil if none.
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
