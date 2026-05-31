package bluetooth

import "airmouse-go/internal/utils"

// HIDReport represents a mouse HID report.
type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

// SendHIDReport sends a HID report to the connected device.
func (m *Manager) SendHIDReport(report HIDReport) {
	// In a real implementation, you would send over BLE HID GATT.
	utils.LogDebug("HID report sent %d %d %d", report.Buttons, report.X, report.Y)
}