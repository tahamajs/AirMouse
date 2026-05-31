package entity

import (
	"time"
)

// Mouse represents the state of the mouse cursor.
type Mouse struct {
	X, Y          float64
	LastMoveTime  time.Time
	VelocityX     float64
	VelocityY     float64
	Sensitivity   float64
	Smoothing     bool
	Acceleration  bool
	AccelFactor   float64
}

// NewMouse creates a new mouse entity with default values.
func NewMouse(sensitivity float64) *Mouse {
	return &Mouse{
		Sensitivity:  sensitivity,
		Smoothing:    true,
		Acceleration: false,
		AccelFactor:  1.5,
		LastMoveTime: time.Now(),
	}
}

// Move updates the mouse position based on raw deltas.
// It applies sensitivity, smoothing, and acceleration.
func (m *Mouse) Move(rawDx, rawDy float64, dt float64) (dx, dy float64) {
	// Apply sensitivity
	dx = rawDx * m.Sensitivity
	dy = rawDy * m.Sensitivity

	// Apply smoothing (EMA)
	if m.Smoothing {
		const alpha = 0.3
		m.VelocityX = alpha*dx + (1-alpha)*m.VelocityX
		m.VelocityY = alpha*dy + (1-alpha)*m.VelocityY
		dx, dy = m.VelocityX, m.VelocityY
	}

	// Apply acceleration
	if m.Acceleration {
		speed := math.Sqrt(dx*dx + dy*dy)
		if speed > 5 {
			factor := 1.0 + m.AccelFactor*(speed/50.0)
			if factor > 3.0 {
				factor = 3.0
			}
			dx *= factor
			dy *= factor
		}
	}

	// Update position
	m.X += dx
	m.Y += dy
	m.LastMoveTime = time.Now()
	return dx, dy
}

// Reset sets the mouse state to zero.
func (m *Mouse) Reset() {
	m.X, m.Y = 0, 0
	m.VelocityX, m.VelocityY = 0, 0
	m.LastMoveTime = time.Now()
}