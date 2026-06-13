//go:build windows

package mouse

import (
    "syscall"
    "unsafe"
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
    MOUSEEVENTF_ABSOLUTE  = 0x8000
)

type WindowsMouse struct {
    *BaseMouse
}

func NewMouseController(sensitivity float64) (MouseController, error) {
    return &WindowsMouse{
        BaseMouse: NewBaseMouse(sensitivity),
    }, nil
}

func (m *WindowsMouse) Move(dx, dy float64) {
    m.mu.RLock()
    sx, sy := m.applySensitivity(dx, dy)
    m.mu.RUnlock()

    mouseEvent.Call(MOUSEEVENTF_MOVE, uintptr(int32(sx)), uintptr(int32(sy)), 0, 0)
}

func (m *WindowsMouse) Click(button string) {
    m.mu.Lock()
    if button == "left" {
        m.clicks++
    } else if button == "right" {
        m.rightClicks++
    }
    m.mu.Unlock()

    if button == "left" {
        mouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
        mouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
    } else if button == "right" {
        mouseEvent.Call(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
        mouseEvent.Call(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
    }
}

func (m *WindowsMouse) DoubleClick() {
    m.mu.Lock()
    m.doubleClicks++
    m.mu.Unlock()

    m.Click("left")
    time.Sleep(50 * time.Millisecond)
    m.Click("left")
}

func (m *WindowsMouse) Scroll(delta int) {
    m.mu.Lock()
    m.scrolls++
    m.mu.Unlock()

    mouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}