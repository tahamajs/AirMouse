package predict

import (
	"sync"
	"time"

	"airmouse-go/internal/predictive"
)

type MovementPredictor struct {
	kf          *predictive.KalmanFilter2D
	oneEuro     *predictive.OneEuroFilter2D
	lastTime    time.Time
	mu          sync.Mutex
	enabled     bool
	blendFactor float64
	useOneEuro  bool
}

func NewMovementPredictor(dtSeconds float64, blendFactor float64) *MovementPredictor {
	return &MovementPredictor{
		kf:          predictive.NewKalmanFilter2D(dtSeconds),
		oneEuro:     predictive.NewOneEuroFilter2D(1.0, 0.007, 1.0),
		enabled:     true,
		blendFactor: blendFactor,
		useOneEuro:  true,
		lastTime:    time.Now(),
	}
}

func (p *MovementPredictor) AddMovement(dx, dy float64) (smoothedDx, smoothedDy float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	if !p.enabled {
		return dx, dy
	}
	now := time.Now()
	var dt float64
	if now.Sub(p.lastTime) > 100*time.Millisecond {
		p.kf.Reset()
		p.oneEuro.Reset()
		dt = 0.02
	} else {
		dt = now.Sub(p.lastTime).Seconds()
		if dt < 0.001 {
			dt = 0.001
		}
		if dt > 0.05 {
			dt = 0.05
		}
	}

	// 1. One Euro adaptive jitter reduction
	inputDx, inputDy := dx, dy
	if p.useOneEuro {
		inputDx, inputDy = p.oneEuro.Filter(dx, dy, now)
	}

	// 2. Kalman state estimation and predictive extrapolation
	p.kf.SetDT(dt)
	p.kf.Predict()
	p.kf.Update(inputDx, inputDy)
	predDx, predDy := p.kf.GetPredictedMovement()

	smoothedDx = (1-p.blendFactor)*inputDx + p.blendFactor*predDx
	smoothedDy = (1-p.blendFactor)*inputDy + p.blendFactor*predDy
	p.lastTime = now
	return
}

func (p *MovementPredictor) EnableOneEuro(enabled bool) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.useOneEuro = enabled
}

func (p *MovementPredictor) SetOneEuroParams(minCutoff, beta, dCutoff float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.oneEuro = predictive.NewOneEuroFilter2D(minCutoff, beta, dCutoff)
}

func (p *MovementPredictor) SetEnabled(enabled bool) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.enabled = enabled
}

func (p *MovementPredictor) SetBlendFactor(factor float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.blendFactor = factor
}

func (p *MovementPredictor) GetBlendFactor() float64 {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.blendFactor
}

func (p *MovementPredictor) IsEnabled() bool {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.enabled
}

func (p *MovementPredictor) GetState() (x, y, vx, vy float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	return p.kf.GetState()
}

func (p *MovementPredictor) GetVelocity() (vx, vy float64) {
	p.mu.Lock()
	defer p.mu.Unlock()
	_, _, vx, vy = p.kf.GetState()
	return
}

func (p *MovementPredictor) Reset() {
	p.mu.Lock()
	defer p.mu.Unlock()
	p.kf.Reset()
	p.oneEuro.Reset()
	p.lastTime = time.Now()
}
