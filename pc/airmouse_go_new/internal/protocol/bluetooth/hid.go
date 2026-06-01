package bluetooth

import "airmouse-go/internal/utils"

type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

func (m *Manager) SendHIDReport(report HIDReport) {
	utils.LogDebug("HID report sent", "buttons", report.Buttons, "x", report.X, "y", report.Y)
}
