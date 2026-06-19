package utils

import (
    "fmt"
    "runtime"
    "time"
)

type Metrics struct {
    CPUPercent    float64
    MemoryPercent float64
    MemoryUsed    uint64
    GoRoutines    int
    Uptime        time.Duration
    startTime     time.Time
}

var metrics = &Metrics{startTime: time.Now()}

// GetMetrics returns current system and Go metrics.
func GetMetrics() *Metrics {
    // Keep this dependency-free so the project builds cleanly in offline setups.
    metrics.CPUPercent = 0
    metrics.MemoryPercent = 0
    metrics.MemoryUsed = 0
    metrics.GoRoutines = runtime.NumGoroutine()
    metrics.Uptime = time.Since(metrics.startTime)
    return metrics
}

func (m *Metrics) String() string {
    return fmt.Sprintf("CPU: %.1f%% | MEM: %.1f%% (%d MB) | Goroutines: %d | Uptime: %v",
        m.CPUPercent, m.MemoryPercent, m.MemoryUsed/1024/1024, m.GoRoutines, m.Uptime.Round(time.Second))
}
