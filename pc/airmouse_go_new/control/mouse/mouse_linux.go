//go:build linux

package mouse

import (
	"os"
	"syscall"
	"time"
	"unsafe"
)

var uinputFd int

func init() {
	fd, err := os.OpenFile("/dev/uinput", os.O_RDWR, 0)
	if err == nil {
		uinputFd = int(fd.Fd())
	}
}

type inputEvent struct {
	Time  syscall.Timeval
	Type  uint16
	Code  uint16
	Value int32
}

const (
	EV_REL    = 0x02
	EV_KEY    = 0x01
	EV_SYN    = 0x00
	REL_X     = 0x00
	REL_Y     = 0x01
	REL_WHEEL = 0x08
	BTN_LEFT   = 0x110
	BTN_RIGHT  = 0x111
	BTN_MIDDLE = 0x112
)

func (m *mouseController) executeMove(dx, dy float64) {
	if uinputFd == 0 {
		return
	}
	events := []inputEvent{
		{Type: EV_REL, Code: REL_X, Value: int32(dx)},
		{Type: EV_REL, Code: REL_Y, Value: int32(dy)},
		{Type: EV_SYN, Code: 0, Value: 0},
	}
	for _, ev := range events {
		syscall.Write(uinputFd, (*(*[24]byte)(unsafe.Pointer(&ev)))[:])
	}
}

func (m *mouseController) executeClick(button string) {
	if uinputFd == 0 {
		return
	}
	btn := BTN_LEFT
	if button == "right" {
		btn = BTN_RIGHT
	} else if button == "middle" {
		btn = BTN_MIDDLE
	}
	events := []inputEvent{
		{Type: EV_KEY, Code: uint16(btn), Value: 1},
		{Type: EV_SYN, Code: 0, Value: 0},
		{Type: EV_KEY, Code: uint16(btn), Value: 0},
		{Type: EV_SYN, Code: 0, Value: 0},
	}
	for _, ev := range events {
		syscall.Write(uinputFd, (*(*[24]byte)(unsafe.Pointer(&ev)))[:])
	}
}

func (m *mouseController) executeDoubleClick() {
	m.executeClick("left")
	time.Sleep(50 * time.Millisecond)
	m.executeClick("left")
}

func (m *mouseController) executeScroll(delta int) {
	if uinputFd == 0 {
		return
	}
	events := []inputEvent{
		{Type: EV_REL, Code: REL_WHEEL, Value: int32(delta)},
		{Type: EV_SYN, Code: 0, Value: 0},
	}
	for _, ev := range events {
		syscall.Write(uinputFd, (*(*[24]byte)(unsafe.Pointer(&ev)))[:])
	}
}
