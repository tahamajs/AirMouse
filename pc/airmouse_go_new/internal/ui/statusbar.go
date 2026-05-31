package ui

import (
	"fmt"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/utils"
)

// StatusBar creates a bottom bar with live metrics (CPU, memory, goroutines, uptime).
type StatusBar struct {
	label *widget.Label
	stop  chan struct{}
}

// NewStatusBar constructs a status bar and starts periodic updates.
func NewStatusBar() *StatusBar {
	sb := &StatusBar{
		label: widget.NewLabel("CPU: --% | MEM: --% | Goroutines: -- | Uptime: --"),
		stop:  make(chan struct{}),
	}
	go sb.updater()
	return sb
}

// Widget returns the container for the status bar.
func (sb *StatusBar) Widget() fyne.CanvasObject {
	return sb.label
}

func (sb *StatusBar) updater() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			metrics := utils.GetMetrics()
			sb.label.SetText(fmt.Sprintf(
				"CPU: %.0f%% | MEM: %.0f%% | Goroutines: %d | Uptime: %v",
				metrics.CPUPercent, metrics.MemoryPercent, metrics.GoRoutines,
				metrics.Uptime.Round(time.Second),
			))
		case <-sb.stop:
			return
		}
	}
}

// Stop halts the periodic updates.
func (sb *StatusBar) Stop() {
	close(sb.stop)
}