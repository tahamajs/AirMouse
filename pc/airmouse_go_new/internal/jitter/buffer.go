package jitter

import (
	"math"
	"sync"
	"time"
)

type JitterBufferConfig struct {
	MaxLatency        time.Duration
	PredictionWindow  time.Duration
	BlendFactor       float64
	UseKalmanVelocity bool
	UseAcceleration   bool
	AdaptiveBlend     bool
	MinBlend          float64
	MaxBlend          float64
	MinDepth          int
	MaxDepth          int
	NominalInterval   time.Duration
}

type MovementSample struct {
	Timestamp time.Time
	Dx, Dy    float64
	Latency   time.Duration
	Quality   float64
}

type JitterBuffer struct {
	cfg            JitterBufferConfig
	predictor      *DeadReckoningPredictor
	mu             sync.Mutex
	lastTime       time.Time
	lastArrival    time.Time
	lastDx, lastDy float64
	history        []MovementSample
	queue          []MovementSample
	maxHistory     int
	targetDepth    int
	avgLatencyMs   float64
	varianceMs2    float64
	jitterMs       float64
	stats          BufferStats
	callbacks      []func(event BufferEvent)
}

type BufferStats struct {
	TotalSamples   int64
	AvgLatency     time.Duration
	MaxLatency     time.Duration
	AvgBlendFactor float64
	DroppedSamples int64
	CurrentDepth   int
	TargetDepth    int
	JitterVariance float64
	NetworkJitter  time.Duration
}

type BufferEvent struct {
	Type      string // "sample_added", "dropped", "latency_spike", "depth_changed"
	Timestamp time.Time
	Latency   time.Duration
}

func DefaultJitterBufferConfig() JitterBufferConfig {
	return JitterBufferConfig{
		MaxLatency:        100 * time.Millisecond,
		PredictionWindow:  20 * time.Millisecond,
		BlendFactor:       0.7,
		UseKalmanVelocity: true,
		UseAcceleration:   false,
		AdaptiveBlend:     true,
		MinBlend:          0.3,
		MaxBlend:          0.9,
		MinDepth:          2,
		MaxDepth:          16,
		NominalInterval:   10 * time.Millisecond,
	}
}

func NewJitterBuffer(cfg JitterBufferConfig) *JitterBuffer {
	if cfg.MinDepth <= 0 {
		cfg.MinDepth = 2
	}
	if cfg.MaxDepth < cfg.MinDepth {
		cfg.MaxDepth = cfg.MinDepth * 4
	}
	if cfg.NominalInterval <= 0 {
		cfg.NominalInterval = 10 * time.Millisecond
	}

	return &JitterBuffer{
		cfg:         cfg,
		predictor:   NewDeadReckoningPredictor(cfg.UseAcceleration),
		history:     make([]MovementSample, 0, 100),
		queue:       make([]MovementSample, 0, cfg.MaxDepth*2),
		maxHistory:  100,
		targetDepth: cfg.MinDepth,
		callbacks:   make([]func(BufferEvent), 0),
		lastTime:    time.Now(),
	}
}

// updateNetworkVariance computes running latency mean, variance and dynamically sizes target depth
func (jb *JitterBuffer) updateNetworkVariance(latency time.Duration, now time.Time, events *[]BufferEvent) {
	latMs := float64(latency.Milliseconds())
	if latMs < 0 {
		latMs = 0
	}

	if jb.lastArrival.IsZero() {
		jb.lastArrival = now
		jb.avgLatencyMs = latMs
		jb.varianceMs2 = 0
		jb.targetDepth = jb.cfg.MinDepth
		return
	}

	interArrival := now.Sub(jb.lastArrival)
	jb.lastArrival = now

	// Exponential moving average for latency variance
	alpha := 0.125
	diff := latMs - jb.avgLatencyMs
	jb.avgLatencyMs += alpha * diff
	jb.varianceMs2 = (1.0-alpha)*jb.varianceMs2 + alpha*(diff*diff)

	// RFC 3550 inter-arrival jitter estimation
	nominalMs := float64(jb.cfg.NominalInterval.Milliseconds())
	if nominalMs <= 0 {
		nominalMs = 10.0
	}
	interArrivalMs := float64(interArrival.Milliseconds())
	d := math.Abs(interArrivalMs - nominalMs)
	jb.jitterMs += (d - jb.jitterMs) / 16.0

	// Standard deviation of network delay
	stdDev := math.Sqrt(math.Max(0, jb.varianceMs2))

	// Dynamic sizing of buffer depth:
	// Variance & jitter scale the depth from MinDepth to MaxDepth
	depthOffset := int(math.Round((stdDev + jb.jitterMs) / (nominalMs * 1.5)))
	newTargetDepth := jb.cfg.MinDepth + depthOffset
	if newTargetDepth < jb.cfg.MinDepth {
		newTargetDepth = jb.cfg.MinDepth
	}
	if newTargetDepth > jb.cfg.MaxDepth {
		newTargetDepth = jb.cfg.MaxDepth
	}

	if newTargetDepth != jb.targetDepth {
		jb.targetDepth = newTargetDepth
		*events = append(*events, BufferEvent{
			Type:      "depth_changed",
			Timestamp: now,
			Latency:   latency,
		})
	}
}

func (jb *JitterBuffer) AddMovement(dx, dy float64, receivedAt time.Time) (smoothedDx, smoothedDy float64) {
	jb.mu.Lock()
	var events []BufferEvent

	now := time.Now()
	dt := now.Sub(jb.lastTime).Seconds()
	if dt < 0.001 {
		dt = 0.02
	}

	latency := now.Sub(receivedAt)
	if latency < 0 {
		latency = 0
	}

	// Update network variance and dynamically size buffer depth
	jb.updateNetworkVariance(latency, now, &events)

	quality := jb.calculateQuality(dx, dy, latency)

	// Update predictor
	if jb.cfg.UseKalmanVelocity {
		jb.predictor.UpdateVelocity(dx, dy, dt)
	}

	// Predict future movement
	predDx, predDy := jb.predictor.Predict(jb.cfg.PredictionWindow.Seconds())

	// Calculate adaptive blend factor
	blend := jb.cfg.BlendFactor
	if jb.cfg.AdaptiveBlend {
		blend = jb.calculateAdaptiveBlend(latency, quality)
	}

	// Apply smoothing
	smoothedDx = (1-blend)*dx + blend*predDx
	smoothedDy = (1-blend)*dy + blend*predDy

	// Store sample in history
	sample := MovementSample{
		Timestamp: now,
		Dx:        dx,
		Dy:        dy,
		Latency:   latency,
		Quality:   quality,
	}
	jb.history = append(jb.history, sample)
	if len(jb.history) > jb.maxHistory {
		jb.history = jb.history[1:]
	}

	// Manage dynamic queue
	jb.queue = append(jb.queue, sample)
	for len(jb.queue) > jb.targetDepth {
		jb.queue = jb.queue[1:]
	}

	// Update statistics
	jb.stats.TotalSamples++
	jb.stats.AvgLatency = (jb.stats.AvgLatency*time.Duration(jb.stats.TotalSamples-1) + latency) / time.Duration(jb.stats.TotalSamples)
	if latency > jb.stats.MaxLatency {
		jb.stats.MaxLatency = latency
		events = append(events, BufferEvent{
			Type:      "latency_spike",
			Timestamp: now,
			Latency:   latency,
		})
	}
	jb.stats.AvgBlendFactor = (jb.stats.AvgBlendFactor*float64(jb.stats.TotalSamples-1) + blend) / float64(jb.stats.TotalSamples)
	jb.stats.CurrentDepth = len(jb.queue)
	jb.stats.TargetDepth = jb.targetDepth
	jb.stats.JitterVariance = jb.varianceMs2
	jb.stats.NetworkJitter = time.Duration(jb.jitterMs * float64(time.Millisecond))

	// Check for drop
	if latency > jb.cfg.MaxLatency {
		jb.stats.DroppedSamples++
		events = append(events, BufferEvent{
			Type:      "dropped",
			Timestamp: now,
			Latency:   latency,
		})
	}

	jb.lastTime = now
	jb.lastDx, jb.lastDy = smoothedDx, smoothedDy
	events = append(events, BufferEvent{
		Type:      "sample_added",
		Timestamp: now,
	})

	jb.mu.Unlock()

	for _, event := range events {
		jb.triggerEvent(event)
	}

	return
}

func (jb *JitterBuffer) Push(sample MovementSample) {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	now := time.Now()
	var events []BufferEvent
	jb.updateNetworkVariance(sample.Latency, now, &events)

	jb.queue = append(jb.queue, sample)
	for len(jb.queue) > jb.targetDepth {
		jb.queue = jb.queue[1:]
	}

	jb.stats.TotalSamples++
	jb.stats.CurrentDepth = len(jb.queue)
	jb.stats.TargetDepth = jb.targetDepth
	jb.stats.JitterVariance = jb.varianceMs2
	jb.stats.NetworkJitter = time.Duration(jb.jitterMs * float64(time.Millisecond))
}

func (jb *JitterBuffer) Pop() (MovementSample, bool) {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	if len(jb.queue) == 0 {
		return MovementSample{}, false
	}
	sample := jb.queue[0]
	jb.queue = jb.queue[1:]
	jb.stats.CurrentDepth = len(jb.queue)
	return sample, true
}

func (jb *JitterBuffer) Len() int {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return len(jb.queue)
}

func (jb *JitterBuffer) GetDepth() int {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return len(jb.queue)
}

func (jb *JitterBuffer) GetTargetDepth() int {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return jb.targetDepth
}

func (jb *JitterBuffer) GetJitterVariance() float64 {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return jb.varianceMs2
}

func (jb *JitterBuffer) calculateQuality(dx, dy float64, latency time.Duration) float64 {
	magnitude := math.Hypot(dx, dy)
	magQuality := math.Min(1.0, magnitude/50.0)
	latencyQuality := 1.0 - math.Min(1.0, float64(latency)/float64(jb.cfg.MaxLatency))
	return magQuality*0.4 + latencyQuality*0.6
}

func (jb *JitterBuffer) calculateAdaptiveBlend(latency time.Duration, quality float64) float64 {
	latencyFactor := float64(latency) / float64(jb.cfg.MaxLatency)
	if latencyFactor > 1 {
		latencyFactor = 1
	}

	qualityFactor := 1 - quality

	// Network variance influence: higher variance increases blend towards prediction
	varianceFactor := math.Min(1.0, math.Sqrt(jb.varianceMs2)/50.0)

	blend := jb.cfg.MinBlend + (latencyFactor*0.5 + qualityFactor*0.2 + varianceFactor*0.3)*(jb.cfg.MaxBlend-jb.cfg.MinBlend)

	if blend < jb.cfg.MinBlend {
		blend = jb.cfg.MinBlend
	}
	if blend > jb.cfg.MaxBlend {
		blend = jb.cfg.MaxBlend
	}

	return blend
}

func (jb *JitterBuffer) PredictNow() (dx, dy float64) {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	dt := time.Since(jb.lastTime).Seconds()
	if dt > 0.1 {
		dt = 0.1
	}
	return jb.predictor.Predict(dt)
}

func (jb *JitterBuffer) GetCurrentVelocity() (vx, vy float64) {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return jb.predictor.GetVelocity()
}

func (jb *JitterBuffer) GetStats() BufferStats {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	jb.stats.CurrentDepth = len(jb.queue)
	jb.stats.TargetDepth = jb.targetDepth
	jb.stats.JitterVariance = jb.varianceMs2
	jb.stats.NetworkJitter = time.Duration(jb.jitterMs * float64(time.Millisecond))
	return jb.stats
}

func (jb *JitterBuffer) GetHistory() []MovementSample {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	history := make([]MovementSample, len(jb.history))
	copy(history, jb.history)
	return history
}

func (jb *JitterBuffer) Reset() {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	jb.predictor.Reset()
	jb.history = jb.history[:0]
	jb.queue = jb.queue[:0]
	jb.targetDepth = jb.cfg.MinDepth
	jb.avgLatencyMs = 0
	jb.varianceMs2 = 0
	jb.jitterMs = 0
	jb.lastArrival = time.Time{}
	jb.lastTime = time.Now()
	jb.stats = BufferStats{}
}

func (jb *JitterBuffer) SetConfig(cfg JitterBufferConfig) {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	if cfg.MinDepth <= 0 {
		cfg.MinDepth = 2
	}
	if cfg.MaxDepth < cfg.MinDepth {
		cfg.MaxDepth = cfg.MinDepth * 4
	}
	if cfg.NominalInterval <= 0 {
		cfg.NominalInterval = 10 * time.Millisecond
	}
	jb.cfg = cfg
}

func (jb *JitterBuffer) AddEventListener(callback func(event BufferEvent)) {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	jb.callbacks = append(jb.callbacks, callback)
}

func (jb *JitterBuffer) triggerEvent(event BufferEvent) {
	jb.mu.Lock()
	callbacks := make([]func(BufferEvent), len(jb.callbacks))
	copy(callbacks, jb.callbacks)
	jb.mu.Unlock()

	for _, cb := range callbacks {
		go cb(event)
	}
}

// GetAverageLatency returns the average latency
func (jb *JitterBuffer) GetAverageLatency() time.Duration {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return jb.stats.AvgLatency
}

// GetDroppedSamples returns the number of dropped samples
func (jb *JitterBuffer) GetDroppedSamples() int64 {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	return jb.stats.DroppedSamples
}

// IsHealthy returns true if the buffer is operating within acceptable parameters
func (jb *JitterBuffer) IsHealthy() bool {
	jb.mu.Lock()
	defer jb.mu.Unlock()

	if jb.stats.TotalSamples == 0 {
		return true
	}

	dropRate := float64(jb.stats.DroppedSamples) / float64(jb.stats.TotalSamples)
	return dropRate < 0.1 && jb.stats.AvgLatency < jb.cfg.MaxLatency*2
}
