// internal/predictive/predictor.go
package predictive

import (
    "sync"
    "time"
)

// MovementPredictor applies Kalman filtering to incoming movement deltas.
type MovementPredictor struct {
    kf        *KalmanFilter2D
    lastTime  time.Time
    mu        sync.Mutex
    enabled   bool
    // Smoothing factor for blending raw and predicted (0 = raw, 1 = predicted)
    blendFactor float64
}

// NewMovementPredictor creates a predictor with a given time step and blend factor.
func NewMovementPredictor(dtSeconds float64, blendFactor float64) *MovementPredictor {
    return &MovementPredictor{
        kf:          NewKalmanFilter2D(dtSeconds),
        enabled:     true,
        blendFactor: blendFactor,
        lastTime:    time.Now(),
    }
}

// AddMovement feeds a raw movement delta (dx, dy) into the predictor.
// It predicts the next movement and returns a smoothed delta.
func (p *MovementPredictor) AddMovement(dx, dy float64) (smoothedDx, smoothedDy float64) {
    p.mu.Lock()
    defer p.mu.Unlock()

    if !p.enabled {
        return dx, dy
    }

    now := time.Now()
    dt := now.Sub(p.lastTime).Seconds()
    if dt < 0.001 {
        dt = 0.001
    }
    // Update filter time step (dynamic dt)
    p.kf.dt = dt

    // Prediction step
    p.kf.Predict()

    // Update with measurement (the raw movement)
    p.kf.Update(dx, dy)

    // Get predicted movement for next step
    predDx, predDy := p.kf.GetPredictedMovement()

    // Blend raw and predicted
    smoothedDx = (1-p.blendFactor)*dx + p.blendFactor*predDx
    smoothedDy = (1-p.blendFactor)*dy + p.blendFactor*predDy

    p.lastTime = now
    return smoothedDx, smoothedDy
}

// SetEnabled turns prediction on/off.
func (p *MovementPredictor) SetEnabled(enabled bool) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.enabled = enabled
}

// SetBlendFactor adjusts the influence of prediction (0..1).
func (p *MovementPredictor) SetBlendFactor(factor float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.blendFactor = factor
}

// Reset clears the filter state.
func (p *MovementPredictor) Reset() {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.kf.Reset()
    p.lastTime = time.Now()
}