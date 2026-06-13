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
    BTN_LEFT  = 0x110
    BTN_RIGHT = 0x111
    BTN_MIDDLE = 0x112
)

type LinuxMouse struct {
    *BaseMouse
}

func NewMouseController(sensitivity float64) (MouseController, error) {
    return &LinuxMouse{
        BaseMouse: NewBaseMouse(sensitivity),
    }, nil
}

func (m *LinuxMouse) Move(dx, dy float64) {
    if uinputFd == 0 {
        return
    }

    m.mu.RLock()
    sx, sy := m.applySensitivity(dx, dy)
    m.mu.RUnlock()

    events := []inputEvent{
        {Type: EV_REL, Code: REL_X, Value: int32(sx)},
        {Type: EV_REL, Code: REL_Y, Value: int32(sy)},
        {Type: EV_SYN, Code: 0, Value: 0},
    }
    m.writeEvents(events)
}

func (m *LinuxMouse) Click(button string) {
    if uinputFd == 0 {
        return
    }

    btn := BTN_LEFT
    if button == "right" {
        btn = BTN_RIGHT
        m.mu.Lock()
        m.rightClicks++
        m.mu.Unlock()
    } else {
        m.mu.Lock()
        m.clicks++
        m.mu.Unlock()
    }

    events := []inputEvent{
        {Type: EV_KEY, Code: uint16(btn), Value: 1},
        {Type: EV_SYN, Code: 0, Value: 0},
        {Type: EV_KEY, Code: uint16(btn), Value: 0},
        {Type: EV_SYN, Code: 0, Value: 0},
    }
    m.writeEvents(events)
}

func (m *LinuxMouse) DoubleClick() {
    m.mu.Lock()
    m.doubleClicks++
    m.mu.Unlock()
    m.Click("left")
    time.Sleep(50 * time.Millisecond)
    m.Click("left")
}

func (m *LinuxMouse) Scroll(delta int) {
    if uinputFd == 0 {
        return
    }

    m.mu.Lock()
    m.scrolls++
    m.mu.Unlock()

    events := []inputEvent{
        {Type: EV_REL, Code: REL_WHEEL, Value: int32(delta)},
        {Type: EV_SYN, Code: 0, Value: 0},
    }
    m.writeEvents(events)
}

func (m *LinuxMouse) writeEvents(events []inputEvent) {
    for _, ev := range events {
        syscall.Write(uinputFd, (*(*[24]byte)(unsafe.Pointer(&ev)))[:])
    }
}