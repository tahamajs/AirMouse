package ui

import (
    "fmt"
    "runtime"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/utils"
)

// StatusBar creates a bottom bar with live metrics
type StatusBar struct {
    container      *fyne.Container
    cpuLabel       *widget.Label
    memLabel       *widget.Label
    goroutineLabel *widget.Label
    uptimeLabel    *widget.Label
    networkLabel   *widget.Label
    stop           chan struct{}
    startTime      time.Time
}

// SystemMetrics holds current system statistics
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

// NewStatusBar constructs a status bar and starts periodic updates
func NewStatusBar() *StatusBar {
    sb := &StatusBar{
        cpuLabel:       widget.NewLabel("CPU: --%"),
        memLabel:       widget.NewLabel("MEM: --%"),
        goroutineLabel: widget.NewLabel("Goroutines: --"),
        uptimeLabel:    widget.NewLabel("Uptime: --:--:--"),
        networkLabel:   widget.NewLabel("📡 --"),
        stop:           make(chan struct{}),
        startTime:      time.Now(),
    }
    
    // Create separator
    separator := canvas.NewLine(color.RGBA{100, 100, 100, 255})
    
    // Layout status bar
    sb.container = container.NewHBox(
        sb.cpuLabel,
        separator,
        sb.memLabel,
        separator,
        sb.goroutineLabel,
        separator,
        sb.uptimeLabel,
        separator,
        sb.networkLabel,
    )
    
    go sb.updater()
    return sb
}

// Widget returns the container for the status bar
func (sb *StatusBar) Widget() fyne.CanvasObject {
    return sb.container
}

func (sb *StatusBar) updater() {
    ticker := time.NewTicker(2 * time.Second)
    defer ticker.Stop()
    
    var lastRx, lastTx int64
    
    for {
        select {
        case <-ticker.C:
            metrics := sb.getSystemMetrics()
            
            // Calculate network speed
            rxSpeed := metrics.NetworkRx - lastRx
            txSpeed := metrics.NetworkTx - lastTx
            lastRx = metrics.NetworkRx
            lastTx = metrics.NetworkTx
            
            // Update UI
            fyne.Do(func() {
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
                sb.networkLabel.SetText(fmt.Sprintf("📡 ↓ %.0f KB/s ↑ %.0f KB/s",
                    float64(rxSpeed)/1024,
                    float64(txSpeed)/1024))
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
    
    // Get CPU percentage (platform-specific)
    metrics.CPUPercent = sb.getCPUPercent()
    
    // Get network stats
    metrics.NetworkRx, metrics.NetworkTx = sb.getNetworkStats()
    
    return metrics
}

func (sb *StatusBar) getCPUPercent() float64 {
    // Platform-specific CPU monitoring
    // This is a simplified version - in production, use gopsutil or similar
    return 25.0 // Placeholder
}

func (sb *StatusBar) getNetworkStats() (int64, int64) {
    // Platform-specific network monitoring
    // This would track bytes sent/received over network interfaces
    return 1024 * 1024, 512 * 1024 // Placeholder
}

// Stop halts the periodic updates
func (sb *StatusBar) Stop() {
    close(sb.stop)
}

// UpdateStartTime resets the uptime counter
func (sb *StatusBar) UpdateStartTime(startTime time.Time) {
    sb.startTime = startTime
}

// AdvancedStatusBar with more features
type AdvancedStatusBar struct {
    container    *fyne.Container
    metrics      *SystemMetrics
    onRefresh    func()
    stop         chan struct{}
}

func NewAdvancedStatusBar(onRefresh func()) *AdvancedStatusBar {
    sb := &AdvancedStatusBar{
        metrics:   &SystemMetrics{},
        onRefresh: onRefresh,
        stop:      make(chan struct{}),
    }
    
    sb.container = container.NewHBox(
        widget.NewLabelWithStyle("📊 System Status", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        sb.createMetricLabel("CPU", &sb.metrics.CPUPercent, "%"),
        sb.createMetricLabel("MEM", &sb.metrics.MemoryPercent, "%"),
        sb.createMetricLabel("Goroutines", &sb.metrics.GoRoutines, ""),
        sb.createMetricLabel("Uptime", nil, ""),
    )
    
    go sb.updater()
    return sb
}

func (sb *AdvancedStatusBar) createMetricLabel(name string, value interface{}, suffix string) *widget.Label {
    return widget.NewLabel(fmt.Sprintf("%s: --%s", name, suffix))
}

func (sb *AdvancedStatusBar) updater() {
    ticker := time.NewTicker(1 * time.Second)
    defer ticker.Stop()
    
    for {
        select {
        case <-ticker.C:
            if sb.onRefresh != nil {
                sb.onRefresh()
            }
        case <-sb.stop:
            return
        }
    }
}

func (sb *AdvancedStatusBar) Stop() {
    close(sb.stop)
}

func (sb *AdvancedStatusBar) Widget() fyne.CanvasObject {
    return sb.container
}