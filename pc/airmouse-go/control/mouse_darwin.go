//go:build darwin

package control

import (
	"fmt"
	"math"
	"os/exec"
	"sync/atomic"
)

type mouseController struct {
	sensitivity    float64
	clickCount     int64
	doubleClickCnt int64
	rightClickCnt  int64
	scrollCount    int64
}

func NewMouseController(sensitivity float64) MouseController {
	return &mouseController{sensitivity: sensitivity}
}

func (m *mouseController) Move(dx, dy float64) {
	dx = math.Max(-50, math.Min(50, dx*m.sensitivity))
	dy = math.Max(-50, math.Min(50, dy*m.sensitivity))
	if math.Abs(dx) < 0.15 && math.Abs(dy) < 0.15 {
		return
	}
	// Get current mouse position
	out, err := exec.Command("osascript", "-e",
		`tell application "System Events" to get current position of mouse`,
	).Output()
	if err != nil {
		return
	}
	var curX, curY float64
	fmt.Sscanf(string(out), "%f, %f", &curX, &curY)
	newX := int(curX + dx)
	newY := int(curY + dy)
	// Set new position
	script := fmt.Sprintf(
		`tell application "System Events" to set position of mouse to {%d, %d}`,
		newX, newY,
	)
	exec.Command("osascript", "-e", script).Run()
}

func (m *mouseController) Click(button string) {
	btn := "1"
	if button == "right" {
		btn = "2"
	}
	script := fmt.Sprintf(
		`tell application "System Events" to click at (current position of mouse) with button %s`,
		btn,
	)
	exec.Command("osascript", "-e", script).Run()
	if button == "left" {
		atomic.AddInt64(&m.clickCount, 1)
	} else if button == "right" {
		atomic.AddInt64(&m.rightClickCnt, 1)
	}
}

func (m *mouseController) DoubleClick() {
	exec.Command("osascript", "-e",
		`tell application "System Events" to double click at (current position of mouse)`,
	).Run()
	atomic.AddInt64(&m.doubleClickCnt, 1)
}

func (m *mouseController) Scroll(delta int) {
	// macOS AppleScript does not support true scrolling easily.
	// For demonstration, we just increment the counter.
	atomic.AddInt64(&m.scrollCount, 1)
}

func (m *mouseController) Stats() (clicks, dbl, right, scroll int64) {
	return atomic.LoadInt64(&m.clickCount),
		atomic.LoadInt64(&m.doubleClickCnt),
		atomic.LoadInt64(&m.rightClickCnt),
		atomic.LoadInt64(&m.scrollCount)
}