// Package predict provides movement predictors (Kalman filter).
package predict

import (
	"sync"
	"time"

	"airmouse-go/internal/predictive"
)

// MovementPredictor uses a Kalman filter to smooth and predict movement.
type MovementPredictor struct {
	kf          *predictive.KalmanFilter2D
	lastTime    time.Time
	mu          sync.Mutex
	enabled     bool
	blendFactor float64
}

// NewMovementPredictor creates a new predictor with the given time step and blend factor.
func NewMovementPredictor(dtSeconds float64, blendFactor float64) *MovementPredictor {
	return &MovementPredictor{
		kf:          predictive.NewKalmanFilter2D(dtSeconds),
		enabled:     true,
		blendFactor: blendFactor,
		lastTime:    time.Now(),
	}
}

// AddMovement processes a raw delta and returns the smoothed/predicted delta.
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
	p.kf.SetDT(dt)
	p.kf.Predict()
	p.kf.Update(dx, dy)
	predDx, predDy := p.kf.GetPredictedMovement()
	smoothedDx = (1-p.blendFactor)*dx + p.blendFactor*predDx
	smoothedDy = (1-p.blendFactor)*dy + p.blendFactor*predDy
	p.lastTime = now
	return
}

// SetEnabled enables or disables the predictor.
func (p *MovementPredictor) SetEnabled(enabled bool) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.enabled = enabled
}

// SetBlendFactor updates the blend factor.
func (p *MovementPredictor) SetBlendFactor(factor float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.blendFactor = factor
}

// GetBlendFactor returns the current blend factor.
func (p *MovementPredictor) GetBlendFactor() float64 {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.blendFactor
}

// IsEnabled returns whether the predictor is enabled.
func (p *MovementPredictor) IsEnabled() bool {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.enabled
}

// GetState returns the current Kalman state (x, y, vx, vy).
func (p *MovementPredictor) GetState() (x, y, vx, vy float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.kf.GetState()
}

// GetVelocity returns the current velocity.
func (p *MovementPredictor) GetVelocity() (vx, vy float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	_, _, vx, vy = p.kf.GetState()
	return
}

// Reset resets the predictor state.
func (p *MovementPredictor) Reset() {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.kf.Reset()
	p.lastTime = time.Now()
}