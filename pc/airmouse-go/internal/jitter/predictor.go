package jitter

import "time"

// DeadReckoningPredictor predicts movement using velocity and acceleration.
type DeadReckoningPredictor struct {
    lastTime     time.Time
    lastDx, lastDy float64
    velocityX    *Kalman1D
    velocityY    *Kalman1D
    accelX       *Kalman1D // optional acceleration estimation (if needed)
    accelY       *Kalman1D
    useAcceleration bool
}

// NewDeadReckoningPredictor creates a predictor with default parameters.
func NewDeadReckoningPredictor(useAcceleration bool) *DeadReckoningPredictor {
    return &DeadReckoningPredictor{
        velocityX:    NewKalman1D(0.5, 1.0),
        velocityY:    NewKalman1D(0.5, 1.0),
        accelX:       NewKalman1D(0.1, 1.0),
        accelY:       NewKalman1D(0.1, 1.0),
        useAcceleration: useAcceleration,
        lastTime:     time.Now(),
    }
}

// UpdateVelocity updates the velocity estimates with a new movement delta.
func (p *DeadReckoningPredictor) UpdateVelocity(dx, dy float64, dt float64) {
    if dt < 0.001 {
        dt = 0.02
    }
    vx := dx / dt
    vy := dy / dt
    p.velocityX.Update(vx)
    p.velocityY.Update(vy)

    if p.useAcceleration {
        // Estimate acceleration as change in velocity
        ax := (vx - p.velocityX.GetState()) / dt
        ay := (vy - p.velocityY.GetState()) / dt
        p.accelX.Update(ax)
        p.accelY.Update(ay)
    }
}

// Predict returns the predicted movement delta over a given time interval.
func (p *DeadReckoningPredictor) Predict(dt float64) (dx, dy float64) {
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
    return
}

// Reset clears the predictor state.
func (p *DeadReckoningPredictor) Reset() {
    p.velocityX.Reset()
    p.velocityY.Reset()
    p.accelX.Reset()
    p.accelY.Reset()
    p.lastTime = time.Now()
}