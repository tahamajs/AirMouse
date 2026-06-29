//go:build darwin

package mouse

/*
#cgo CFLAGS: -x objective-c
#cgo LDFLAGS: -framework Cocoa -framework CoreGraphics
#include <CoreGraphics/CoreGraphics.h>
#include <ApplicationServices/ApplicationServices.h>

CGPoint getMousePos() {
    CGEventRef probe = CGEventCreate(NULL);
    CGPoint point = CGEventGetLocation(probe);
    CFRelease(probe);
    return point;
}

void moveMouseTo(float x, float y, float dx, float dy) {
    CGPoint dest = CGPointMake(x, y);
    CGEventRef event = CGEventCreateMouseEvent(NULL, kCGEventMouseMoved, dest, kCGMouseButtonLeft);
    CGEventSetIntegerValueField(event, kCGMouseEventDeltaX, (int64_t)dx);
    CGEventSetIntegerValueField(event, kCGMouseEventDeltaY, (int64_t)dy);
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
    CGEventRef probe = CGEventCreate(NULL);
    CGPoint point = CGEventGetLocation(probe);
    CFRelease(probe);
    CGEventRef event = CGEventCreateMouseEvent(NULL, type, point, btn);
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
import (
	"os/exec"
	"sync"
	"time"
)

var (
	darwinMouseMu  sync.Mutex
	lastDarwinTime time.Time
	fracX          float64
	fracY          float64
	cachedX        float64
	cachedY        float64
)

func (m *mouseController) executeMove(dx, dy float64) {
	darwinMouseMu.Lock()
	defer darwinMouseMu.Unlock()

	now := time.Now()
	
	// Reset fractional accumulation and resync with OS cursor on a pause
	if now.Sub(lastDarwinTime) > 100*time.Millisecond {
		fracX = 0
		fracY = 0
		pos := C.getMousePos()
		cachedX = float64(pos.x)
		cachedY = float64(pos.y)
	}

	fracX += dx
	fracY += dy

	intDx := float64(int64(fracX))
	intDy := float64(int64(fracY))

	if intDx != 0 || intDy != 0 {
		fracX -= intDx
		fracY -= intDy

		cachedX += intDx
		cachedY += intDy

		C.moveMouseTo(C.float(cachedX), C.float(cachedY), C.float(intDx), C.float(intDy))
	}

	lastDarwinTime = now
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
	} else if button == "middle" {
		C.clickMouse(2, 1)
		time.Sleep(10 * time.Millisecond)
		C.clickMouse(2, 0)
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
