//go:build darwin

package mouse

/*
#cgo CFLAGS: -x objective-c
#cgo LDFLAGS: -framework Cocoa -framework CoreGraphics

#include <CoreGraphics/CoreGraphics.h>

void moveMouse(float dx, float dy) {
    CGEventRef event = CGEventCreateMouseEvent(NULL, kCGEventMouseMoved, CGPointMake(dx, dy), kCGMouseButtonLeft);
    CGEventPost(kCGHIDEventTap, event);
    CFRelease(event);
}

void clickMouse(int button, int down) {
    CGEventType type;
    CGMouseButton btn;
    if (button == 0) {
        type = down ? kCGEventLeftMouseDown : kCGEventLeftMouseUp;
        btn = kCGMouseButtonLeft;
    } else if (button == 1) {
        type = down ? kCGEventRightMouseDown : kCGEventRightMouseUp;
        btn = kCGMouseButtonRight;
    } else {
        type = down ? kCGEventOtherMouseDown : kCGEventOtherMouseUp;
        btn = kCGMouseButtonCenter;
    }
    CGEventRef event = CGEventCreateMouseEvent(NULL, type, CGPointMake(0, 0), btn);
    CGEventPost(kCGHIDEventTap, event);
    CFRelease(event);
}

void doubleClick() {
    clickMouse(0, 1);
    clickMouse(0, 0);
    clickMouse(0, 1);
    clickMouse(0, 0);
}

void scrollWheel(int delta) {
    CGEventRef event = CGEventCreateScrollWheelEvent(NULL, kCGScrollEventUnitLine, 1, delta);
    CGEventPost(kCGHIDEventTap, event);
    CFRelease(event);
}
*/
import "C"
import "time"

type DarwinMouse struct {
    *BaseMouse
}

func NewMouseController(sensitivity float64) (MouseController, error) {
    return &DarwinMouse{
        BaseMouse: NewBaseMouse(sensitivity),
    }, nil
}

func (m *DarwinMouse) Move(dx, dy float64) {
    m.mu.RLock()
    sx, sy := m.applySensitivity(dx, dy)
    m.mu.RUnlock()
    C.moveMouse(C.float(sx), C.float(sy))
}

func (m *DarwinMouse) Click(button string) {
    m.mu.Lock()
    if button == "left" {
        m.clicks++
    } else if button == "right" {
        m.rightClicks++
    }
    m.mu.Unlock()

    btn := 0
    if button == "right" {
        btn = 1
    } else if button == "middle" {
        btn = 2
    }
    C.clickMouse(C.int(btn), 1)
    time.Sleep(10 * time.Millisecond)
    C.clickMouse(C.int(btn), 0)
}

func (m *DarwinMouse) DoubleClick() {
    m.mu.Lock()
    m.doubleClicks++
    m.mu.Unlock()
    C.doubleClick()
}

func (m *DarwinMouse) Scroll(delta int) {
    m.mu.Lock()
    m.scrolls++
    m.mu.Unlock()
    C.scrollWheel(C.int(delta))
}