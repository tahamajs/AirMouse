package jitter

import "time"

type DeadReckoningPredictor struct {
	velocityX       *Kalman1D
	velocityY       *Kalman1D
	accelX          *Kalman1D
	accelY          *Kalman1D
	useAcceleration bool
	lastTime        time.Time
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
	if dt < 0.001 {
		dt = 0.02
	}
	vx := dx / dt
	vy := dy / dt
	p.velocityX.Update(vx)
	p.velocityY.Update(vy)
	if p.useAcceleration {
		ax := (vx - p.velocityX.GetState()) / dt
		ay := (vy - p.velocityY.GetState()) / dt
		p.accelX.Update(ax)
		p.accelY.Update(ay)
	}
}

func (p *DeadReckoningPredictor) Predict(dt float64) (dx, dy float64) {
	vx := p.velocityX.GetState()
	vy := p.velocityY.GetState()
	dx = vx * dt
	dy = vy * dt
	if p.useAcceleration {
		dx += 0.5 * p.accelX.GetState() * dt * dt
		dy += 0.5 * p.accelY.GetState() * dt * dt
	}
	return
}

func (p *DeadReckoningPredictor) Reset() {
	p.velocityX.Reset()
	p.velocityY.Reset()
	p.accelX.Reset()
	p.accelY.Reset()
}
