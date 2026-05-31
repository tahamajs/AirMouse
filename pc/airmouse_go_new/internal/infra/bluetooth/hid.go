package bluetooth

import "airmouse-go/internal/infra/logger"

type HIDReport struct {
	Buttons byte
	X       int16
	Y       int16
	Wheel   int8
}

func SendHIDReport(report HIDReport) {
	logger.Debug("HID report sent", "buttons", report.Buttons, "x", report.X, "y", report.Y)
}