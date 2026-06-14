package jitter

import (
    "math"
    "sync"
    "time"
)

type DeadReckoningPredictor struct {
    velocityX       *Kalman1D
    velocityY       *Kalman1D
    accelX          *Kalman1D
    accelY          *Kalman1D
    useAcceleration bool
    lastTime        time.Time
    lastVX, lastVY  float64
    mu              sync.RWMutex
    stats           DRStats
}

type DRStats struct {
    Predictions    int64
    AvgError       float64
    LastError      float64
}

func NewDeadReckoningPredictor(useAcceleration bool) *DeadReckoningPredictor {
    return &DeadReckoningPredictor{
        velocityX:       NewKalman1D(0.5, 1.0),
        velocityY:       NewKalman1D(0.5, 1.0),
        accelX:          NewKalman1D(0.1, 1.0),
        accelY:          NewKalman1D(0.1, 1.0),
        useAcceleration: useAcceleration,
        lastTime:        time.Now(),
    }
}

func (p *DeadReckoningPredictor) UpdateVelocity(dx, dy, dt float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    
    if dt < 0.001 {
        dt = 0.02
    }
    
    vx := dx / dt
    vy := dy / dt
    
    p.velocityX.Update(vx)
    p.velocityY.Update(vy)
    
    if p.useAcceleration {
        ax := (vx - p.lastVX) / dt
        ay := (vy - p.lastVY) / dt
        
        // Filter acceleration to reduce noise
        if math.Abs(ax) < 100 {
            p.accelX.Update(ax)
        }
        if math.Abs(ay) < 100 {
            p.accelY.Update(ay)
        }
    }
    
    p.lastVX = vx
    p.lastVY = vy
    p.lastTime = time.Now()
}

func (p *DeadReckoningPredictor) Predict(dt float64) (dx, dy float64) {
    p.mu.RLock()
    defer p.mu.RUnlock()
    
    if dt <= 0 {
        dt = 0.02
    }
    
    vx := p.velocityX.GetState()
    vy := p.velocityY.GetState()
    
    dx = vx * dt
    dy = vy * dt
    
    if p.useAcceleration {
        ax := p.accelX.GetState()
        ay := p.accelY.GetState()
        dx += 0.5 * ax * dt * dt
        dy += 0.5 * ay * dt * dt
    }
    
    p.stats.Predictions++
    return dx, dy
}

func (p *DeadReckoningPredictor) PredictPosition(currentX, currentY, dt float64) (x, y float64) {
    dx, dy := p.Predict(dt)
    return currentX + dx, currentY + dy
}

func (p *DeadReckoningPredictor) GetVelocity() (vx, vy float64) {
    p.mu.RLock()
    defer p.mu.RUnlock()
    return p.velocityX.GetState(), p.velocityY.GetState()
}

func (p *DeadReckoningPredictor) GetAcceleration() (ax, ay float64) {
    p.mu.RLock()
    defer p.mu.RUnlock()
    if p.useAcceleration {
        return p.accelX.GetState(), p.accelY.GetState()
    }
    return 0, 0
}

func (p *DeadReckoningPredictor) GetConfidence() float64 {
    p.mu.RLock()
    defer p.mu.RUnlock()
    
    velConf := (p.velocityX.GetConfidence() + p.velocityY.GetConfidence()) / 2
    if p.useAcceleration {
        accConf := (p.accelX.GetConfidence() + p.accelY.GetConfidence()) / 2
        return velConf*0.7 + accConf*0.3
    }
    return velConf
}

func (p *DeadReckoningPredictor) Reset() {
    p.mu.Lock()
    defer p.mu.Unlock()
    
    p.velocityX.Reset()
    p.velocityY.Reset()
    p.accelX.Reset()
    p.accelY.Reset()
    p.lastTime = time.Now()
    p.lastVX = 0
    p.lastVY = 0
    p.stats = DRStats{}
}

func (p *DeadReckoningPredictor) GetStats() DRStats {
    p.mu.RLock()
    defer p.mu.RUnlock()
    return p.stats
}

func (p *DeadReckoningPredictor) SetError(error float64) {
    p.mu.Lock()
    defer p.mu.Unlock()
    
    p.stats.LastError = error
    if p.stats.Predictions > 0 {
        p.stats.AvgError = (p.stats.AvgError*float64(p.stats.Predictions-1) + error) / float64(p.stats.Predictions)
    } else {
        p.stats.AvgError = error
    }
}