//go:build darwin

package control

import (
    "fmt"
)

// Lightweight stub implementation for macOS when robotgo is not available.
// This avoids requiring the robotgo native dependency during build.
func (m *mouseController) executeMove(dx, dy float64) {
    // In stub mode we simply log the requested movement.
    fmt.Printf("[stub] move dx=%.2f dy=%.2f\n", dx, dy)
}

func (m *mouseController) executeClick(button string) {
    fmt.Printf("[stub] click button=%s\n", button)
}

func (m *mouseController) executeDoubleClick() {
    fmt.Printf("[stub] doubleclick\n")
}

func (m *mouseController) executeScroll(delta int) {
    fmt.Printf("[stub] scroll delta=%d\n", delta)
}
