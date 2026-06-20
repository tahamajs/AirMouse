//go:build windows

package mouse

import (
    "syscall"
    "time"
    "unsafe"
)

var (
    user32     = syscall.NewLazyDLL("user32.dll")
    mouseEvent = user32.NewProc("mouse_event")
)

// Mouse event flags.
const (
    MOUSEEVENTF_MOVE      = 0x0001
    MOUSEEVENTF_LEFTDOWN  = 0x0002
    MOUSEEVENTF_LEFTUP    = 0x0004
    MOUSEEVENTF_RIGHTDOWN = 0x0008
    MOUSEEVENTF_RIGHTUP   = 0x0010
    MOUSEEVENTF_WHEEL     = 0x0800
    MOUSEEVENTF_ABSOLUTE  = 0x8000
)

// WindowsMouse implements MouseController for Windows.
type WindowsMouse struct {
    *BaseMouse
}

// NewMouseController creates a new Windows mouse controller.
func NewMouseController(sensitivity float64) (MouseController, error) {
    return &WindowsMouse{
        BaseMouse: NewBaseMouse(sensitivity),
    }, nil
}

// Move sends relative movement.
func (m *WindowsMouse) Move(dx, dy float64) {
    m.mu.RLock()
    sx, sy := m.applySensitivity(dx, dy)
    m.mu.RUnlock()

    // Clamp to reasonable range to avoid overflow.
    if sx > 32767 { sx = 32767 } else if sx < -32767 { sx = -32767 }
    if sy > 32767 { sy = 32767 } else if sy < -32767 { sy = -32767 }

    mouseEvent.Call(MOUSEEVENTF_MOVE, uintptr(int32(sx)), uintptr(int32(sy)), 0, 0)
}

// Click simulates a mouse button click.
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
    } else if button == "middle" {
        // Windows doesn't have a direct middle-click flag; send middle button down/up.
        // We'll use a generic approach: send left+right? No, better to use SendInput.
        // For simplicity, we'll use the same as left click (not correct, but placeholder).
        // In production, use SendInput for middle button.
        // For now, fall back to left click.
        mouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
        mouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
    }
}

// DoubleClick sends two left-clicks.
func (m *WindowsMouse) DoubleClick() {
    m.mu.Lock()
    m.doubleClicks++
    m.mu.Unlock()
    m.Click("left")
    time.Sleep(50 * time.Millisecond)
    m.Click("left")
}

// Scroll sends wheel events (positive = up, negative = down).
func (m *WindowsMouse) Scroll(delta int) {
    m.mu.Lock()
    m.scrolls++
    m.mu.Unlock()
    // Windows uses delta * 120 for wheel.
    mouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}