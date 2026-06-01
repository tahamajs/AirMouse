package utils

import (
	"fmt"
	"runtime"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
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
	// CPU percent (average across cores)
	if percent, err := cpu.Percent(0, false); err == nil && len(percent) > 0 {
		metrics.CPUPercent = percent[0]
	}
	// Memory
	if memStat, err := mem.VirtualMemory(); err == nil {
		metrics.MemoryPercent = memStat.UsedPercent
		metrics.MemoryUsed = memStat.Used
	}
	// Goroutines
	metrics.GoRoutines = runtime.NumGoroutine()
	// Uptime (from process start)
	metrics.Uptime = time.Since(metrics.startTime)
	return metrics
}

func (m *Metrics) String() string {
	return fmt.Sprintf("CPU: %.1f%% | MEM: %.1f%% (%d MB) | Goroutines: %d | Uptime: %v",
		m.CPUPercent, m.MemoryPercent, m.MemoryUsed/1024/1024, m.GoRoutines, m.Uptime.Round(time.Second))
}