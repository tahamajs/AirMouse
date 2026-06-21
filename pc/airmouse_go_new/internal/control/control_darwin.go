//go:build darwin

package control

/*
#cgo CFLAGS: -x objective-c
#cgo LDFLAGS: -framework Cocoa -framework CoreGraphics
#include <CoreGraphics/CoreGraphics.h>
#include <ApplicationServices/ApplicationServices.h>

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
    } else {
        type = down ? kCGEventRightMouseDown : kCGEventRightMouseUp;
        btn = kCGMouseButtonRight;
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

int hasAccessibilityPermission() {
    return AXIsProcessTrusted();
}

int requestAccessibilityPermission() {
    const void *keys[] = { kAXTrustedCheckOptionPrompt };
    const void *values[] = { kCFBooleanTrue };
    CFDictionaryRef options = CFDictionaryCreate(NULL, keys, values, 1, &kCFCopyStringDictionaryKeyCallBacks, &kCFTypeDictionaryValueCallBacks);
    int trusted = AXIsProcessTrustedWithOptions(options);
    CFRelease(options);
    return trusted;
}
*/
import "C"
import "time"
import "os/exec"

func (m *mouseController) executeMove(dx, dy float64) {
    C.moveMouse(C.float(dx), C.float(dy))
}

func (m *mouseController) executeClick(button string) {
    if button == "left" {
        C.clickMouse(0, 1)
        time.Sleep(10 * time.Millisecond)
        C.clickMouse(0, 0)
    } else if button == "right" {
        C.clickMouse(1, 1)
        time.Sleep(10 * time.Millisecond)
        C.clickMouse(1, 0)
    }
}

func (m *mouseController) executeDoubleClick() { 
    C.doubleClick()
}

func (m *mouseController) executeScroll(delta int) {
    C.scrollWheel(C.int(delta))
}

// HasAccessibilityPermission reports whether macOS accessibility control is enabled.
func HasAccessibilityPermission() bool {
	return C.hasAccessibilityPermission() != 0
}

// RequestAccessibilityPermission opens the macOS permission prompt.
func RequestAccessibilityPermission() bool {
	return C.requestAccessibilityPermission() != 0
}

// OpenAccessibilitySettings opens the macOS Accessibility privacy pane.
func OpenAccessibilitySettings() error {
	return exec.Command("open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility").Start()
}
