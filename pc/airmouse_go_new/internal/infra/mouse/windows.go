//go:build windows

package mouse

import (
	"syscall"
)

var (
	user32     = syscall.NewLazyDLL("user32.dll")
	mouseEvent = user32.NewProc("mouse_event")
)

const (
	MOUSEEVENTF_MOVE      = 0x0001
	MOUSEEVENTF_LEFTDOWN  = 0x0002
	MOUSEEVENTF_LEFTUP    = 0x0004
	MOUSEEVENTF_RIGHTDOWN = 0x0008
	MOUSEEVENTF_RIGHTUP   = 0x0010
	MOUSEEVENTF_WHEEL     = 0x0800
)

type windowsMouse struct{}

func NewMouseController(sensitivity float64) (MouseController, error) {
	return &windowsMouse{}, nil
}

func (m *windowsMouse) Move(dx, dy float64) {
	mouseEvent.Call(MOUSEEVENTF_MOVE, uintptr(int32(dx)), uintptr(int32(dy)), 0, 0)
}

func (m *windowsMouse) Click(button string) {
	if button == "left" {
		mouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
		mouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
	} else if button == "right" {
		mouseEvent.Call(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
		mouseEvent.Call(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
	}
}

func (m *windowsMouse) DoubleClick() {
	m.Click("left")
	m.Click("left")
}

func (m *windowsMouse) Scroll(delta int) {
	mouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}
