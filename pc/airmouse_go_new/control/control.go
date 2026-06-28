package control

import (
	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
)

// Re‑export types and constructors
type MouseController = mouse.Controller

var NewMouseController = mouse.NewController

// Pause/resume
var SetMovementPaused = common.SetMovementPaused
var ClearPause = common.ClearPause
var IsMovementPaused = common.IsMovementPaused

// System commands
var ExecuteSystemCommand = syscmd.ExecuteSystemCommand

// ------------------------------------------------------------
// Accessibility functions – defined directly (not aliases)
// ------------------------------------------------------------

// HasAccessibilityPermission returns true on non‑Darwin platforms.
// On macOS, the real CGO implementation is used.
func HasAccessibilityPermission() bool {
	return true
}

// RequestAccessibilityPermission is a no‑op on non‑Darwin.
func RequestAccessibilityPermission() bool {
	return true
}

// OpenAccessibilitySettings is a no‑op on non‑Darwin.
func OpenAccessibilitySettings() error {
	return nil
}
