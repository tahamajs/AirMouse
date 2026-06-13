package jitter

import (
    "math"
    "sync"
    "time"
)

type JitterBufferConfig struct {
    MaxLatency          time.Duration
    PredictionWindow    time.Duration
    BlendFactor         float64
    UseKalmanVelocity   bool
    UseAcceleration     bool
    AdaptiveBlend       bool
    MinBlend            float64
    MaxBlend            float64
}

type MovementSample struct {
    Timestamp time.Time
    Dx, Dy    float64
    Latency   time.Duration
    Quality   float64
}

type JitterBuffer struct {
    cfg         JitterBufferConfig
    predictor   *DeadReckoningPredictor
    mu          sync.Mutex
    lastTime    time.Time
    lastDx, lastDy float64
    history     []MovementSample
    maxHistory  int
    stats       BufferStats
    callbacks   []func(event BufferEvent)
}

type BufferStats struct {
    TotalSamples    int64
    AvgLatency      time.Duration
    MaxLatency      time.Duration
    AvgBlendFactor  float64
    DroppedSamples  int64
}

type BufferEvent struct {
    Type      string // "sample_added", "dropped", "latency_spike"
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
    }
}

func NewJitterBuffer(cfg JitterBufferConfig) *JitterBuffer {
    return &JitterBuffer{
        cfg:        cfg,
        predictor:  NewDeadReckoningPredictor(cfg.UseAcceleration),
        history:    make([]MovementSample, 0, 100),
        maxHistory: 100,
        callbacks:  make([]func(BufferEvent), 0),
        lastTime:   time.Now(),
    }
}

func (jb *JitterBuffer) AddMovement(dx, dy float64, receivedAt time.Time) (smoothedDx, smoothedDy float64) {
    jb.mu.Lock()
    defer jb.mu.Unlock()
    
    now := time.Now()
    dt := now.Sub(jb.lastTime).Seconds()
    if dt < 0.001 {
        dt = 0.02
    }
    
    latency := now.Sub(receivedAt)
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
    
    // Store sample
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
    
    // Update statistics
    jb.stats.TotalSamples++
    jb.stats.AvgLatency = (jb.stats.AvgLatency*time.Duration(jb.stats.TotalSamples-1) + latency) / time.Duration(jb.stats.TotalSamples)
    if latency > jb.stats.MaxLatency {
        jb.stats.MaxLatency = latency
        jb.triggerEvent(BufferEvent{
            Type:      "latency_spike",
            Timestamp: now,
            Latency:   latency,
        })
    }
    jb.stats.AvgBlendFactor = (jb.stats.AvgBlendFactor*float64(jb.stats.TotalSamples-1) + blend) / float64(jb.stats.TotalSamples)
    
    // Check for drop
    if latency > jb.cfg.MaxLatency {
        jb.stats.DroppedSamples++
        jb.triggerEvent(BufferEvent{
            Type:      "dropped",
            Timestamp: now,
            Latency:   latency,
        })
    }
    
    jb.lastTime = now
    jb.lastDx, jb.lastDy = smoothedDx, smoothedDy
    jb.triggerEvent(BufferEvent{
        Type:      "sample_added",
        Timestamp: now,
    })
    
    return
}

func (jb *JitterBuffer) calculateQuality(dx, dy float64, latency time.Duration) float64 {
    // Quality based on movement magnitude and latency
    magnitude := math.Hypot(dx, dy)
    magQuality := math.Min(1.0, magnitude/50.0)
    
    latencyQuality := 1.0 - math.Min(1.0, float64(latency)/float64(jb.cfg.MaxLatency))
    
    return magQuality * 0.4 + latencyQuality * 0.6
}

func (jb *JitterBuffer) calculateAdaptiveBlend(latency time.Duration, quality float64) float64 {
    // Higher latency = higher blend (more prediction)
    latencyFactor := float64(latency) / float64(jb.cfg.MaxLatency)
    if latencyFactor > 1 {
        latencyFactor = 1
    }
    
    // Lower quality = higher blend
    qualityFactor := 1 - quality
    
    blend := jb.cfg.MinBlend + (latencyFactor*0.7+qualityFactor*0.3)*(jb.cfg.MaxBlend-jb.cfg.MinBlend)
    
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
    jb.lastTime = time.Now()
    jb.stats = BufferStats{}
}

func (jb *JitterBuffer) SetConfig(cfg JitterBufferConfig) {
    jb.mu.Lock()
    defer jb.mu.Unlock()
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