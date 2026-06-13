package control

import (
    "sync"
    "time"

    "airmouse-go/internal/predictive"
)

type MovementPredictor struct {
    kf          *predictive.KalmanFilter2D
    lastTime    time.Time
    mu          sync.Mutex
    enabled     bool
    blendFactor float64
    stats       PredictorStats
}

type PredictorStats struct {
    TotalPredictions int64
    AvgLatency       time.Duration
    LastPrediction   time.Time
}

func NewMovementPredictor(dtSeconds float64, blendFactor float64) *MovementPredictor {
    if dtSeconds <= 0 {
        dtSeconds = 0.02
    }
    if blendFactor < 0 {
        blendFactor = 0
    }
    if blendFactor > 1 {
        blendFactor = 1
    }
    
    return &MovementPredictor{
        kf:          predictive.NewKalmanFilter2D(dtSeconds),
        enabled:     true,
        blendFactor: blendFactor,
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
    dt := now.Sub(p.lastTime).Seconds()
    if dt < 0.001 {
        dt = 0.001
    }
    
    p.kf.SetDT(dt)
    p.kf.Predict()
    p.kf.Update(dx, dy)
    
    predDx, predDy := p.kf.GetPredictedMovement()
    
    // Apply blend factor
    smoothedDx = (1-p.blendFactor)*dx + p.blendFactor*predDx
    smoothedDy = (1-p.blendFactor)*dy + p.blendFactor*predDy
    
    // Update stats
    p.stats.TotalPredictions++
    p.stats.LastPrediction = now
    p.stats.AvgLatency = (p.stats.AvgLatency*time.Duration(p.stats.TotalPredictions-1) + now.Sub(p.lastTime)) / time.Duration(p.stats.TotalPredictions)
    
    p.lastTime = now
    return
}

func (p *MovementPredictor) SetEnabled(enabled bool) {
    p.mu.Lock()
    defer p.mu.Unlock()
    p.enabled = enabled
}

func (p *MovementPredictor) SetBlendFactor(factor float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    if factor < 0 {
        factor = 0
    }
    if factor > 1 {
        factor = 1
    }
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

func (p *MovementPredictor) GetStats() PredictorStats {
    p.mu.Lock()
    defer p.mu.Unlock()
    return p.stats
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
    p.lastTime = time.Now()
    p.stats = PredictorStats{}
}