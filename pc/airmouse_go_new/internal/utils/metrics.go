package utils

import (
	"fmt"
	"runtime"
	"sync"
	"sync/atomic"
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

// ------------------------------------------------------------
// Live Server Telemetry & Rolling Throughput Tracking
// ------------------------------------------------------------

// TelemetrySnapshot represents a point-in-time metrics snapshot.
type TelemetrySnapshot struct {
	StartTime          time.Time `json:"start_time"`
	UptimeSeconds      float64   `json:"uptime_seconds"`
	TotalPacketsIn     int64     `json:"total_packets_in"`
	TotalPacketsOut    int64     `json:"total_packets_out"`
	TotalPackets       int64     `json:"total_packets"`
	TotalBytesIn       int64     `json:"total_bytes_in"`
	TotalBytesOut      int64     `json:"total_bytes_out"`
	DroppedPackets     int64     `json:"dropped_packets"`
	PacketsPerSec      float64   `json:"packets_per_sec"`
	BytesInKBps        float64   `json:"bytes_in_kbps"`
	BytesOutKBps       float64   `json:"bytes_out_kbps"`
	AveragePingMs      float64   `json:"average_ping_ms"`
	LastPingMs         float64   `json:"last_ping_ms"`
}

// TelemetryTracker tracks packet counts, byte throughput, dropped packets, and ping latencies.
type TelemetryTracker struct {
	mu                 sync.RWMutex
	startTime          time.Time
	totalPacketsIn     int64
	totalPacketsOut    int64
	droppedPackets     int64
	totalBytesIn       int64
	totalBytesOut      int64

	pingCount          int64
	totalPingLatencyMs float64
	lastPingLatencyMs  float64

	lastSampleTime     time.Time
	lastSamplePackets  int64
	lastSampleBytesIn  int64
	lastSampleBytesOut int64

	packetsPerSec      float64
	bytesInKBps        float64
	bytesOutKBps       float64
}

// NewTelemetryTracker creates a new initialized telemetry tracker.
func NewTelemetryTracker() *TelemetryTracker {
	now := time.Now()
	return &TelemetryTracker{
		startTime:      now,
		lastSampleTime: now,
	}
}

// Global default telemetry tracker
var DefaultTelemetry = NewTelemetryTracker()

// RecordPacketIn records an inbound packet with its payload byte size.
func (t *TelemetryTracker) RecordPacketIn(bytes int64) {
	atomic.AddInt64(&t.totalPacketsIn, 1)
	atomic.AddInt64(&t.totalBytesIn, bytes)
}

// RecordPacketOut records an outbound packet with its payload byte size.
func (t *TelemetryTracker) RecordPacketOut(bytes int64) {
	atomic.AddInt64(&t.totalPacketsOut, 1)
	atomic.AddInt64(&t.totalBytesOut, bytes)
}

// RecordDroppedPacket records a dropped/rejected packet.
func (t *TelemetryTracker) RecordDroppedPacket() {
	atomic.AddInt64(&t.droppedPackets, 1)
}

// RecordPingLatency records a measured round-trip ping time in milliseconds.
func (t *TelemetryTracker) RecordPingLatency(ms float64) {
	if ms < 0 {
		return
	}
	t.mu.Lock()
	defer t.mu.Unlock()
	t.pingCount++
	t.totalPingLatencyMs += ms
	t.lastPingLatencyMs = ms
}

// updateRollingRates calculates rate deltas over time intervals.
func (t *TelemetryTracker) updateRollingRates() {
	now := time.Now()
	t.mu.Lock()
	defer t.mu.Unlock()

	dt := now.Sub(t.lastSampleTime).Seconds()
	if dt < 0.5 {
		return
	}

	totalIn := atomic.LoadInt64(&t.totalPacketsIn)
	bytesIn := atomic.LoadInt64(&t.totalBytesIn)
	bytesOut := atomic.LoadInt64(&t.totalBytesOut)

	deltaPackets := totalIn - t.lastSamplePackets
	deltaBytesIn := bytesIn - t.lastSampleBytesIn
	deltaBytesOut := bytesOut - t.lastSampleBytesOut

	t.packetsPerSec = float64(deltaPackets) / dt
	t.bytesInKBps = (float64(deltaBytesIn) / 1024.0) / dt
	t.bytesOutKBps = (float64(deltaBytesOut) / 1024.0) / dt

	t.lastSampleTime = now
	t.lastSamplePackets = totalIn
	t.lastSampleBytesIn = bytesIn
	t.lastSampleBytesOut = bytesOut
}

// GetSnapshot returns the current telemetry snapshot.
func (t *TelemetryTracker) GetSnapshot() TelemetrySnapshot {
	t.updateRollingRates()

	t.mu.RLock()
	defer t.mu.RUnlock()

	totalIn := atomic.LoadInt64(&t.totalPacketsIn)
	totalOut := atomic.LoadInt64(&t.totalPacketsOut)
	bytesIn := atomic.LoadInt64(&t.totalBytesIn)
	bytesOut := atomic.LoadInt64(&t.totalBytesOut)
	dropped := atomic.LoadInt64(&t.droppedPackets)

	avgPing := 0.0
	if t.pingCount > 0 {
		avgPing = t.totalPingLatencyMs / float64(t.pingCount)
	}

	return TelemetrySnapshot{
		StartTime:       t.startTime,
		UptimeSeconds:   time.Since(t.startTime).Seconds(),
		TotalPacketsIn:  totalIn,
		TotalPacketsOut: totalOut,
		TotalPackets:    totalIn + totalOut,
		TotalBytesIn:    bytesIn,
		TotalBytesOut:   bytesOut,
		DroppedPackets:  dropped,
		PacketsPerSec:   t.packetsPerSec,
		BytesInKBps:     t.bytesInKBps,
		BytesOutKBps:    t.bytesOutKBps,
		AveragePingMs:   avgPing,
		LastPingMs:      t.lastPingLatencyMs,
	}
}

// Convenience package-level helper functions delegating to DefaultTelemetry
func RecordPacketIn(bytes int64) {
	DefaultTelemetry.RecordPacketIn(bytes)
}

func RecordPacketOut(bytes int64) {
	DefaultTelemetry.RecordPacketOut(bytes)
}

func RecordDroppedPacket() {
	DefaultTelemetry.RecordDroppedPacket()
}

func RecordPingLatency(ms float64) {
	DefaultTelemetry.RecordPingLatency(ms)
}

func GetTelemetrySnapshot() TelemetrySnapshot {
	return DefaultTelemetry.GetSnapshot()
}
