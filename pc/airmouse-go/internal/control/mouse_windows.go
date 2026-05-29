//go:build windows

package control

import (
	"syscall"
	"unsafe"
)

var (
	user32          = syscall.NewLazyDLL("user32.dll")
	procMouseEvent  = user32.NewProc("mouse_event")
	procSetCursorPos = user32.NewProc("SetCursorPos")
)

const (
	MOUSEEVENTF_MOVE      = 0x0001
	MOUSEEVENTF_LEFTDOWN  = 0x0002
	MOUSEEVENTF_LEFTUP    = 0x0004
	MOUSEEVENTF_RIGHTDOWN = 0x0008
	MOUSEEVENTF_RIGHTUP   = 0x0010
	MOUSEEVENTF_WHEEL     = 0x0800
)

func (m *mouseController) executeMove(dx, dy float64) {
	x := int32(dx)
	y := int32(dy)
	procMouseEvent.Call(MOUSEEVENTF_MOVE, uintptr(x), uintptr(y), 0, 0)
}

func (m *mouseController) executeClick(button string) {
	if button == "left" {
		procMouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
	} else if button == "right" {
		procMouseEvent.Call(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
	}
}

func (m *mouseController) executeDoubleClick() {
	m.executeClick("left")
	m.executeClick("left")
}

func (m *mouseController) executeScroll(delta int) {
	procMouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}