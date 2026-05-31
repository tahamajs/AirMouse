//go:build windows

package mouse

import (
	"syscall"
)

var (
	user32         = syscall.NewLazyDLL("user32.dll")
	procMouseEvent = user32.NewProc("mouse_event")
)

const (
	MOUSEEVENTF_MOVE      = 0x0001
	MOUSEEVENTF_LEFTDOWN  = 0x0002
	MOUSEEVENTF_LEFTUP    = 0x0004
	MOUSEEVENTF_RIGHTDOWN = 0x0008
	MOUSEEVENTF_RIGHTUP   = 0x0010
	MOUSEEVENTF_WHEEL     = 0x0800
)

type WindowsMouse struct{}

func New() MouseController {
	return &WindowsMouse{}
}

func (m *WindowsMouse) Move(dx, dy float64) {
	procMouseEvent.Call(MOUSEEVENTF_MOVE, uintptr(int32(dx)), uintptr(int32(dy)), 0, 0)
}

func (m *WindowsMouse) Click(button string) {
	if button == "left" {
		procMouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
	} else if button == "right" {
		procMouseEvent.Call(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
	}
}

func (m *WindowsMouse) DoubleClick() {
	m.Click("left")
	m.Click("left")
}

func (m *WindowsMouse) Scroll(delta int) {
	procMouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}