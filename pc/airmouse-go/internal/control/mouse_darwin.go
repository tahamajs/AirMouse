//go:build darwin

package control

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
    if (button == 0) {
        type = down ? kCGEventLeftMouseDown : kCGEventLeftMouseUp;
    } else {
        type = down ? kCGEventRightMouseDown : kCGEventRightMouseUp;
    }
    CGEventRef event = CGEventCreateMouseEvent(NULL, type, CGPointMake(0, 0), button == 0 ? kCGMouseButtonLeft : kCGMouseButtonRight);
    CGEventPost(kCGHIDEventTap, event);
    CFRelease(event);
}
*/
import "C"

func (m *mouseController) executeMove(dx, dy float64) {
	C.moveMouse(C.float(dx), C.float(dy))
}

func (m *mouseController) executeClick(button string) {
	if button == "left" {
		C.clickMouse(0, 1)
		C.clickMouse(0, 0)
	} else if button == "right" {
		C.clickMouse(1, 1)
		C.clickMouse(1, 0)
	}
}

func (m *mouseController) executeDoubleClick() {
	m.executeClick("left")
	m.executeClick("left")
}

func (m *mouseController) executeScroll(delta int) {
	// Use CGEventCreateScrollWheelEvent
	C.CGEventRef event := C.CGEventCreateScrollWheelEvent(NULL, 0, 1, C.int32(delta))
	C.CGEventPost(C.kCGHIDEventTap, event)
	C.CFRelease(event)
}