package jitter

import (
    "testing"
    "time"
)

func TestJitterBufferBasic(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    for i := 0; i < 10; i++ {
        smDx, smDy := jb.AddMovement(10.0, 5.0, now)
        t.Logf("smoothed: (%.2f, %.2f)", smDx, smDy)
    }
    
    stats := jb.GetStats()
    t.Logf("Stats: samples=%d, avgLatency=%v, dropped=%d", 
        stats.TotalSamples, stats.AvgLatency, stats.DroppedSamples)
}

func TestJitterBufferWithLatency(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    testCases := []struct {
        name    string
        latency time.Duration
        dx, dy  float64
    }{
        {"Low latency", 10 * time.Millisecond, 10, 5},
        {"Medium latency", 50 * time.Millisecond, 20, 10},
        {"High latency", 150 * time.Millisecond, 30, 15},
    }
    
    for _, tc := range testCases {
        t.Run(tc.name, func(t *testing.T) {
            receivedAt := time.Now().Add(-tc.latency)
            smoothedDx, smoothedDy := jb.AddMovement(tc.dx, tc.dy, receivedAt)
            t.Logf("Latency %v -> smoothed (%.2f, %.2f)", tc.latency, smoothedDx, smoothedDy)
        })
    }
}

func TestJitterBufferPrediction(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    // Add history
    for i := 0; i < 5; i++ {
        jb.AddMovement(10.0, 5.0, now)
        time.Sleep(10 * time.Millisecond)
    }
    
    predDx, predDy := jb.PredictNow()
    t.Logf("Predicted: (%.2f, %.2f)", predDx, predDy)
}

func TestJitterBufferReset(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    for i := 0; i < 5; i++ {
        jb.AddMovement(10.0, 5.0, now)
    }
    
    statsBefore := jb.GetStats()
    t.Logf("Before reset: samples=%d", statsBefore.TotalSamples)
    
    jb.Reset()
    
    statsAfter := jb.GetStats()
    t.Logf("After reset: samples=%d", statsAfter.TotalSamples)
    
    if statsAfter.TotalSamples != 0 {
        t.Errorf("Reset failed: expected 0 samples, got %d", statsAfter.TotalSamples)
    }
}

func TestJitterBufferEvents(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    eventCount := 0
    jb.AddEventListener(func(event BufferEvent) {
        eventCount++
        t.Logf("Event: %s, latency=%v", event.Type, event.Latency)
    })
    
    now := time.Now()
    jb.AddMovement(10.0, 5.0, now)
    
    // High latency to trigger events
    highLatencyReceived := time.Now().Add(-200 * time.Millisecond)
    jb.AddMovement(10.0, 5.0, highLatencyReceived)
    
    t.Logf("Total events: %d", eventCount)
}

func TestJitterBufferHealth(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    if !jb.IsHealthy() {
        t.Error("New buffer should be healthy")
    }
    
    now := time.Now()
    for i := 0; i < 10; i++ {
        jb.AddMovement(10.0, 5.0, now)
    }
    
    // Just log health status, don't fail
    t.Logf("Buffer healthy: %v", jb.IsHealthy())
}

func TestJitterBufferVelocity(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    // Add movement to establish velocity
    for i := 0; i < 5; i++ {
        jb.AddMovement(10.0, 5.0, now)
        time.Sleep(10 * time.Millisecond)
    }
    
    vx, vy := jb.GetCurrentVelocity()
    t.Logf("Current velocity: (%.2f, %.2f)", vx, vy)
}

func TestJitterBufferHistory(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    expectedSamples := 15
    for i := 0; i < expectedSamples; i++ {
        jb.AddMovement(float64(i), float64(i), now)
    }
    
    history := jb.GetHistory()
    t.Logf("History length: %d", len(history))
    
    if len(history) == 0 {
        t.Error("Expected non-empty history")
    }
}

func TestJitterBufferAdaptiveBlend(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    cfg.AdaptiveBlend = true
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    // Test with varying latencies
    latencies := []time.Duration{10, 50, 100, 200}
    
    for _, latency := range latencies {
        receivedAt := time.Now().Add(-latency)
        smoothedDx, smoothedDy := jb.AddMovement(10.0, 5.0, receivedAt)
        t.Logf("Latency %v -> smoothed (%.2f, %.2f)", latency, smoothedDx, smoothedDy)
    }
}

func TestJitterBufferConfigUpdate(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    // Change config
    newCfg := JitterBufferConfig{
        MaxLatency:         200 * time.Millisecond,
        PredictionWindow:   30 * time.Millisecond,
        BlendFactor:        0.8,
        UseKalmanVelocity:  true,
        UseAcceleration:    true,
        AdaptiveBlend:      true,
        MinBlend:           0.4,
        MaxBlend:           0.95,
    }
    
    jb.SetConfig(newCfg)
    
    now := time.Now()
    smoothedDx, smoothedDy := jb.AddMovement(10.0, 5.0, now)
    t.Logf("After config update: smoothed (%.2f, %.2f)", smoothedDx, smoothedDy)
}

func TestJitterBufferContinuous(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    // Simulate continuous movement
    for i := 0; i < 50; i++ {
        now := time.Now()
        dx := 5.0 * float64(i%10)
        dy := 3.0 * float64(i%5)
        
        smoothedDx, smoothedDy := jb.AddMovement(dx, dy, now)
        
        if i%10 == 0 {
            t.Logf("Step %d: input (%.2f, %.2f) -> smoothed (%.2f, %.2f)", i, dx, dy, smoothedDx, smoothedDy)
        }
        
        time.Sleep(5 * time.Millisecond)
    }
    
    stats := jb.GetStats()
    t.Logf("Continuous test completed: %d samples processed", stats.TotalSamples)
}

func BenchmarkJitterBuffer(b *testing.B) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        jb.AddMovement(10.0, 5.0, now)
    }
}

func BenchmarkJitterBufferWithLatency(b *testing.B) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        receivedAt := time.Now().Add(-50 * time.Millisecond)
        jb.AddMovement(10.0, 5.0, receivedAt)
    }
}

func BenchmarkJitterBufferPrediction(b *testing.B) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)
    now := time.Now()
    
    // Warm up with some samples
    for i := 0; i < 10; i++ {
        jb.AddMovement(10.0, 5.0, now)
    }
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        jb.PredictNow()
    }
}