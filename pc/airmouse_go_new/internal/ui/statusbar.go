package ui

import (
	"fmt"
	"runtime"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"
)

// ------------------------------------------------------------
// StatusBar – basic status bar with live metrics
// ------------------------------------------------------------

type StatusBar struct {
	container      *fyne.Container
	cpuLabel       *widget.Label
	memLabel       *widget.Label
	goroutineLabel *widget.Label
	uptimeLabel    *widget.Label
	networkLabel   *widget.Label
	stop           chan struct{}
	stopOnce       sync.Once
	startTime      time.Time
	lastCPU        time.Time
	lastCPUSample  time.Duration
}

// SystemMetrics holds current system statistics.
type SystemMetrics struct {
	CPUPercent    float64
	MemoryPercent float64
	MemoryUsed    uint64
	MemoryTotal   uint64
	GoRoutines    int
	Uptime        time.Duration
	NetworkRx     int64
	NetworkTx     int64
}

// NewStatusBar creates a status bar and starts periodic updates.
func NewStatusBar() *StatusBar {
	sb := &StatusBar{
		cpuLabel:       widget.NewLabel("💻 CPU: --%"),
		memLabel:       widget.NewLabel("🧠 MEM: --%"),
		goroutineLabel: widget.NewLabel("🔄 Goroutines: --"),
		uptimeLabel:    widget.NewLabel("⏱️ Uptime: --:--:--"),
		networkLabel:   widget.NewLabel("📡 ↓-- KB/s ↑-- KB/s"),
		stop:           make(chan struct{}),
		startTime:      time.Now(),
	}

	sb.container = container.NewHBox(
		sb.cpuLabel,
		widget.NewSeparator(),
		sb.memLabel,
		widget.NewSeparator(),
		sb.goroutineLabel,
		widget.NewSeparator(),
		sb.uptimeLabel,
		widget.NewSeparator(),
		sb.networkLabel,
	)

	go sb.updater()
	return sb
}

// Widget returns the container for the status bar.
func (sb *StatusBar) Widget() fyne.CanvasObject {
	return sb.container
}

func (sb *StatusBar) updater() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()

	var lastRx, lastTx int64
	var lastNetworkTime time.Time

	for {
		select {
		case <-ticker.C:
			metrics := sb.getSystemMetrics()

			// Network speed
			now := time.Now()
			if !lastNetworkTime.IsZero() {
				elapsed := now.Sub(lastNetworkTime).Seconds()
				if elapsed > 0 {
					rxSpeed := float64(metrics.NetworkRx-lastRx) / elapsed
					txSpeed := float64(metrics.NetworkTx-lastTx) / elapsed
					RunOnMain(func() {
						sb.networkLabel.SetText(fmt.Sprintf("📡 ↓%.0f KB/s ↑%.0f KB/s",
							rxSpeed/1024, txSpeed/1024))
					})
				}
			}
			lastRx = metrics.NetworkRx
			lastTx = metrics.NetworkTx
			lastNetworkTime = now

			// Update other metrics
			RunOnMain(func() {
				sb.cpuLabel.SetText(fmt.Sprintf("💻 CPU: %.0f%%", metrics.CPUPercent))
				sb.memLabel.SetText(fmt.Sprintf("🧠 MEM: %.0f%% (%.0f/%.0f MB)",
					metrics.MemoryPercent,
					float64(metrics.MemoryUsed)/1024/1024,
					float64(metrics.MemoryTotal)/1024/1024))
				sb.goroutineLabel.SetText(fmt.Sprintf("🔄 Goroutines: %d", metrics.GoRoutines))
				sb.uptimeLabel.SetText(fmt.Sprintf("⏱️ Uptime: %02d:%02d:%02d",
					int(metrics.Uptime.Hours()),
					int(metrics.Uptime.Minutes())%60,
					int(metrics.Uptime.Seconds())%60))
			})

		case <-sb.stop:
			return
		}
	}
}

func (sb *StatusBar) getSystemMetrics() SystemMetrics {
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)

	metrics := SystemMetrics{
		MemoryUsed:    memStats.Alloc,
		MemoryTotal:   memStats.Sys,
		MemoryPercent: float64(memStats.Alloc) / float64(memStats.Sys) * 100,
		GoRoutines:    runtime.NumGoroutine(),
		Uptime:        time.Since(sb.startTime),
	}

	metrics.CPUPercent = sb.getCPUPercent()
	metrics.NetworkRx, metrics.NetworkTx = sb.getNetworkStats()

	if metrics.MemoryPercent > 100 {
		metrics.MemoryPercent = 100
	}
	if metrics.CPUPercent > 100 {
		metrics.CPUPercent = 100
	}
	return metrics
}

func (sb *StatusBar) getCPUPercent() float64 {
	// Simulated CPU usage (production would use proper sampling).
	now := time.Now()
	if sb.lastCPU.IsZero() {
		sb.lastCPU = now
		return 0
	}
	elapsed := now.Sub(sb.lastCPU)
	if elapsed < 10*time.Millisecond {
		return sb.lastCPUSample.Seconds() * 100
	}
	// Use goroutine count as a proxy (for demo).
	cpuPercent := float64(runtime.NumGoroutine()) / 10.0
	if cpuPercent > 100 {
		cpuPercent = 100
	}
	sb.lastCPU = now
	sb.lastCPUSample = time.Duration(cpuPercent) * time.Millisecond
	return cpuPercent
}

func (sb *StatusBar) getNetworkStats() (int64, int64) {
	// In production, read actual network stats.
	// Return simulated values.
	return 1024 * 1024, 512 * 1024
}

// Stop stops the background updater.
func (sb *StatusBar) Stop() {
	sb.stopOnce.Do(func() {
		close(sb.stop)
	})
}

// UpdateStartTime resets the uptime counter.
func (sb *StatusBar) UpdateStartTime(startTime time.Time) {
	sb.startTime = startTime
}

// ------------------------------------------------------------
// AdvancedStatusBar – more flexible status bar with callback
// ------------------------------------------------------------

type AdvancedStatusBar struct {
	container      *fyne.Container
	cpuLabel       *widget.Label
	memLabel       *widget.Label
	goroutineLabel *widget.Label
	uptimeLabel    *widget.Label
	onRefresh      func() SystemMetrics
	stop           chan struct{}
	stopOnce       sync.Once
	startTime      time.Time
}

// NewAdvancedStatusBar creates an advanced status bar that calls a user-provided
// function to fetch metrics. If onRefresh is nil, it uses default metrics.
func NewAdvancedStatusBar(onRefresh func() SystemMetrics) *AdvancedStatusBar {
	sb := &AdvancedStatusBar{
		cpuLabel:       widget.NewLabel("💻 CPU: --%"),
		memLabel:       widget.NewLabel("🧠 MEM: --%"),
		goroutineLabel: widget.NewLabel("🔄 Goroutines: --"),
		uptimeLabel:    widget.NewLabel("⏱️ Uptime: --:--:--"),
		onRefresh:      onRefresh,
		stop:           make(chan struct{}),
		startTime:      time.Now(),
	}

	sb.container = container.NewHBox(
		widget.NewLabelWithStyle("📊 System Status", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		sb.cpuLabel,
		widget.NewSeparator(),
		sb.memLabel,
		widget.NewSeparator(),
		sb.goroutineLabel,
		widget.NewSeparator(),
		sb.uptimeLabel,
	)

	go sb.updater()
	return sb
}

func (sb *AdvancedStatusBar) updater() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			var metrics SystemMetrics
			if sb.onRefresh != nil {
				metrics = sb.onRefresh()
			} else {
				metrics = sb.defaultMetrics()
			}
			RunOnMain(func() {
				sb.cpuLabel.SetText(fmt.Sprintf("💻 CPU: %.0f%%", metrics.CPUPercent))
				sb.memLabel.SetText(fmt.Sprintf("🧠 MEM: %.0f%%", metrics.MemoryPercent))
				sb.goroutineLabel.SetText(fmt.Sprintf("🔄 Goroutines: %d", metrics.GoRoutines))
				sb.uptimeLabel.SetText(fmt.Sprintf("⏱️ Uptime: %02d:%02d:%02d",
					int(metrics.Uptime.Hours()),
					int(metrics.Uptime.Minutes())%60,
					int(metrics.Uptime.Seconds())%60))
			})
		case <-sb.stop:
			return
		}
	}
}

func (sb *AdvancedStatusBar) defaultMetrics() SystemMetrics {
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)

	return SystemMetrics{
		CPUPercent:    float64(runtime.NumGoroutine()) / 10.0,
		MemoryPercent: float64(memStats.Alloc) / float64(memStats.Sys) * 100,
		MemoryUsed:    memStats.Alloc,
		MemoryTotal:   memStats.Sys,
		GoRoutines:    runtime.NumGoroutine(),
		Uptime:        time.Since(sb.startTime),
	}
}

// Stop stops the background updater.
func (sb *AdvancedStatusBar) Stop() {
	sb.stopOnce.Do(func() {
		close(sb.stop)
	})
}

// Widget returns the container for this status bar.
func (sb *AdvancedStatusBar) Widget() fyne.CanvasObject {
	return sb.container
}