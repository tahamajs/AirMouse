package bluetooth

import (
	"airmouse-go/internal/utils"
)

// HIDReport represents a mouse HID report.
type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

// SendHIDReport sends a HID report (simulated).
func (m *Manager) SendHIDReport(report HIDReport) {
	utils.LogDebug("HID report sent", "buttons", report.Buttons, "x", report.X, "y", report.Y)
}