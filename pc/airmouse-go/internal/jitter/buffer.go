package jitter

import (
    "sync"
    "time"
)

// JitterBufferConfig holds configuration for the jitter compensation.
type JitterBufferConfig struct {
    MaxLatency            time.Duration // maximum expected latency (ms)
    PredictionWindow      time.Duration // how far to predict (ms)
    BlendFactor           float64       // blend between predicted and actual (0=pred, 1=actual)
    UseKalmanVelocity     bool
    UseAcceleration       bool
}

// DefaultJitterBufferConfig returns sensible defaults.
func DefaultJitterBufferConfig() JitterBufferConfig {
    return JitterBufferConfig{
        MaxLatency:       100 * time.Millisecond,
        PredictionWindow: 20 * time.Millisecond,
        BlendFactor:      0.7,
        UseKalmanVelocity: true,
        UseAcceleration:   false,
    }
}

// JitterBuffer compensates for network jitter using dead reckoning.
type JitterBuffer struct {
    cfg        JitterBufferConfig
    predictor  *DeadReckoningPredictor
    mu         sync.Mutex
    lastTime   time.Time
    lastDx, lastDy float64
    history    []MovementSample
    maxHistory int
}

// MovementSample stores a movement delta with timestamp.
type MovementSample struct {
    Timestamp time.Time
    Dx, Dy    float64
}

// NewJitterBuffer creates a jitter buffer with the given configuration.
func NewJitterBuffer(cfg JitterBufferConfig) *JitterBuffer {
    return &JitterBuffer{
        cfg:        cfg,
        predictor:  NewDeadReckoningPredictor(cfg.UseAcceleration),
        history:    make([]MovementSample, 0, 50),
        maxHistory: 50,
        lastTime:   time.Now(),
    }
}

// AddMovement adds a new movement delta received from the network.
// It returns the movement that should be sent to the mouse controller,
// which may be a blend of the actual movement and a predicted movement
// to compensate for jitter.
func (jb *JitterBuffer) AddMovement(dx, dy float64, receivedAt time.Time) (smoothedDx, smoothedDy float64) {
    jb.mu.Lock()
    defer jb.mu.Unlock()

    now := time.Now()
    dt := now.Sub(jb.lastTime).Seconds()
    if dt < 0.001 {
        dt = 0.02
    }

    // Update predictor with the actual movement (velocity)
    if jb.cfg.UseKalmanVelocity {
        jb.predictor.UpdateVelocity(dx, dy, dt)
    } else {
        // Simple velocity estimation
        jb.predictor.velocityX.Update(dx / dt)
        jb.predictor.velocityY.Update(dy / dt)
    }

    // Calculate predicted movement for the expected next packet time
    predDx, predDy := jb.predictor.Predict(jb.cfg.PredictionWindow.Seconds())

    // Blend actual and predicted based on the network latency
    // Higher latency → more weight on prediction
    latency := now.Sub(receivedAt)
    blend := jb.cfg.BlendFactor
    if latency > jb.cfg.MaxLatency {
        blend = 0.9 // trust prediction more
    } else if latency < 20*time.Millisecond {
        blend = 0.5 // low latency: balanced blend
    }
    smoothedDx = (1-blend)*dx + blend*predDx
    smoothedDy = (1-blend)*dy + blend*predDy

    // Store history for debugging / analysis (optional)
    jb.history = append(jb.history, MovementSample{Timestamp: now, Dx: dx, Dy: dy})
    if len(jb.history) > jb.maxHistory {
        jb.history = jb.history[1:]
    }

    jb.lastTime = now
    jb.lastDx, jb.lastDy = dx, dy
    return
}

// PredictNow returns the predicted movement for the current time (if no packet arrives).
// This can be called by a timer to keep the cursor moving during packet loss.
func (jb *JitterBuffer) PredictNow() (dx, dy float64) {
    jb.mu.Lock()
    defer jb.mu.Unlock()
    now := time.Now()
    dt := now.Sub(jb.lastTime).Seconds()
    if dt > 0.1 {
        dt = 0.1 // limit extrapolation
    }
    return jb.predictor.Predict(dt)
}

// Reset clears the buffer state.
func (jb *JitterBuffer) Reset() {
    jb.mu.Lock()
    defer jb.mu.Unlock()
    jb.predictor.Reset()
    jb.history = jb.history[:0]
    jb.lastTime = time.Now()
}

// GetStats returns diagnostic information.
func (jb *JitterBuffer) GetStats() map[string]interface{} {
    jb.mu.RLock()
    defer jb.mu.RUnlock()
    return map[string]interface{}{
        "history_length": len(jb.history),
        "last_dx":        jb.lastDx,
        "last_dy":        jb.lastDy,
        "velocity_x":     jb.predictor.velocityX.GetState(),
        "velocity_y":     jb.predictor.velocityY.GetState(),
    }
}