//go:build (darwin && !cgo) || (!darwin && !linux && !windows)

package mouse

import (
	"os/exec"
)

func (m *mouseController) executeMove(dx, dy float64) {
}

func (m *mouseController) executeClick(button string) {
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
