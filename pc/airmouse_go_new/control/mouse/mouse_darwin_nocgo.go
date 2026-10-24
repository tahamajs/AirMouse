//go:build (darwin && !cgo) || (!darwin && !linux && !windows)

package mouse

import (
	"os/exec"
	"sync"
)

var (
	nocgoMouseMu sync.Mutex
	virtualPosX  float64
	virtualPosY  float64
)

// Headless / zero-CGO fallback implementation for Darwin.
// Allows pure Go, zero-CGO compilation on macOS.

func (m *mouseController) executeMove(dx, dy float64) {
	nocgoMouseMu.Lock()
	defer nocgoMouseMu.Unlock()
	virtualPosX += dx
	virtualPosY += dy
}

func (m *mouseController) executeClick(button string) {
}

func (m *mouseController) executeMouseDown(button string) {
}

func (m *mouseController) executeMouseUp(button string) {
}

func (m *mouseController) executeDoubleClick() {
}

func (m *mouseController) executeScroll(delta int) {
}

// HasAccessibilityPermission reports whether macOS accessibility control is enabled.
func HasAccessibilityPermission() bool {
	return true
}

// RequestAccessibilityPermission opens the macOS permission prompt.
func RequestAccessibilityPermission() bool {
	return true
}

// OpenAccessibilitySettings opens the macOS Accessibility privacy pane.
func OpenAccessibilitySettings() error {
	return exec.Command("open", "x-apple.systempreferences:com.apple.preference.security?Privacy_Accessibility").Start()
}
