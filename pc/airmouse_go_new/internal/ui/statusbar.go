package ui

import (
    "fmt"
    "runtime"
    "time"

    "fyne.io/fyne/v2"
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
    lastCPU        time.Time
    lastCPUSample  time.Duration
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
        cpuLabel:       widget.NewLabel("💻 CPU: --%"),
        memLabel:       widget.NewLabel("🧠 MEM: --%"),
        goroutineLabel: widget.NewLabel("🔄 Goroutines: --"),
        uptimeLabel:    widget.NewLabel("⏱️ Uptime: --:--:--"),
        networkLabel:   widget.NewLabel("📡 ↓-- KB/s ↑-- KB/s"),
        stop:           make(chan struct{}),
        startTime:      time.Now(),
    }
    
    // Layout status bar
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

// Widget returns the container for the status bar
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
            
            // Calculate network speed
            now := time.Now()
            if !lastNetworkTime.IsZero() {
                elapsed := now.Sub(lastNetworkTime).Seconds()
                if elapsed > 0 {
                    rxSpeed := float64(metrics.NetworkRx-lastRx) / elapsed
                    txSpeed := float64(metrics.NetworkTx-lastTx) / elapsed
                    
                    fyne.Do(func() {
                        sb.networkLabel.SetText(fmt.Sprintf("📡 ↓%.0f KB/s ↑%.0f KB/s",
                            rxSpeed/1024, txSpeed/1024))
                    })
                }
            }
            lastRx = metrics.NetworkRx
            lastTx = metrics.NetworkTx
            lastNetworkTime = now
            
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
    
    // Get CPU percentage
    metrics.CPUPercent = sb.getCPUPercent()
    
    // Get network stats
    metrics.NetworkRx, metrics.NetworkTx = sb.getNetworkStats()
    
    // Clamp values
    if metrics.MemoryPercent > 100 {
        metrics.MemoryPercent = 100
    }
    if metrics.CPUPercent > 100 {
        metrics.CPUPercent = 100
    }
    
    return metrics
}

func (sb *StatusBar) getCPUPercent() float64 {
    // Simple CPU usage calculation
    now := time.Now()
    if sb.lastCPU.IsZero() {
        sb.lastCPU = now
        return 0
    }
    
    elapsed := now.Sub(sb.lastCPU)
    if elapsed < 10*time.Millisecond {
        return sb.lastCPUSample.Seconds() * 100
    }
    
    var rUsage runtime.MemStats
    runtime.ReadMemStats(&rUsage)
    
    // Return a simulated CPU percentage (simplified)
    // In production, use proper CPU sampling
    cpuPercent := float64(runtime.NumGoroutine()) / 10.0
    if cpuPercent > 100 {
        cpuPercent = 100
    }
    
    sb.lastCPU = now
    sb.lastCPUSample = time.Duration(cpuPercent) * time.Millisecond
    
    return cpuPercent
}

func (sb *StatusBar) getNetworkStats() (int64, int64) {
    // In production, read from netstat or /proc/net/dev
    // Return simulated values for now
    return 1024 * 1024, 512 * 1024
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

// NewAdvancedStatusBar creates an advanced status bar
func NewAdvancedStatusBar(onRefresh func()) *AdvancedStatusBar {
    sb := &AdvancedStatusBar{
        metrics:   &SystemMetrics{},
        onRefresh: onRefresh,
        stop:      make(chan struct{}),
    }
    
    sb.container = container.NewHBox(
        widget.NewLabelWithStyle("📊 System Status", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("CPU: --%"),
        widget.NewSeparator(),
        widget.NewLabel("MEM: --%"),
        widget.NewSeparator(),
        widget.NewLabel("Goroutines: --"),
        widget.NewSeparator(),
        widget.NewLabel("Uptime: --"),
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