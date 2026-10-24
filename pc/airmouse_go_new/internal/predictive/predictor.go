package predictive

import (
	"math"
	"sync"
)

// KalmanFilter2D performs 2D linear state estimation [x, y, vx, vy] for cursor displacement.
type KalmanFilter2D struct {
	mu sync.RWMutex

	// State vector [x, y, vx, vy]
	x, y   float64
	vx, vy float64

	// Covariance estimate
	p [4][4]float64

	// Process noise covariance
	q float64

	// Measurement noise covariance
	r float64

	// Last timestamp
	lastTs int64

	// Blend factor between raw input and predicted filter state [0.0 = raw, 1.0 = full filter]
	blendFactor float64

	initialized bool
}

// NewKalmanFilter2D initializes a new 2D Kalman Filter with standard defaults.
func NewKalmanFilter2D(processNoise, measurementNoise, blendFactor float64) *KalmanFilter2D {
	kf := &KalmanFilter2D{
		q:           processNoise,
		r:           measurementNoise,
		blendFactor: blendFactor,
	}
	kf.Reset()
	return kf
}

// Reset resets the state matrix and covariance.
func (kf *KalmanFilter2D) Reset() {
	kf.mu.Lock()
	defer kf.mu.Unlock()

	kf.x = 0
	kf.y = 0
	kf.vx = 0
	kf.vy = 0
	kf.lastTs = 0
	kf.initialized = false

	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			if i == j {
				kf.p[i][j] = 1.0
			} else {
				kf.p[i][j] = 0.0
			}
		}
	}
}

// Update processes raw (dx, dy) measured over dt seconds and returns smoothed/predicted (outDx, outDy).
func (kf *KalmanFilter2D) Update(dx, dy, dt float64) (float64, float64) {
	kf.mu.Lock()
	defer kf.mu.Unlock()

	// Guard against non-positive delta times
	if dt <= 0.0001 {
		dt = 0.016
	} else if dt > 0.5 {
		dt = 0.5
	}

	if !kf.initialized {
		kf.x = dx
		kf.y = dy
		kf.vx = dx / dt
		kf.vy = dy / dt
		kf.initialized = true
		return dx, dy
	}

	// 1. Prediction step:
	// x_pred = x + vx * dt
	// y_pred = y + vy * dt
	predX := kf.x + kf.vx*dt
	predY := kf.y + kf.vy*dt

	// Covariance prediction P = F * P * F^T + Q
	kf.p[0][0] += dt * (kf.p[2][0] + kf.p[0][2] + dt*kf.p[2][2]) + kf.q
	kf.p[1][1] += dt * (kf.p[3][1] + kf.p[1][3] + dt*kf.p[3][3]) + kf.q
	kf.p[2][2] += kf.q
	kf.p[3][3] += kf.q

	// 2. Kalman Gain: K = P * H^T * (H * P * H^T + R)^-1
	sX := kf.p[0][0] + kf.r
	sY := kf.p[1][1] + kf.r

	if math.Abs(sX) < 1e-9 {
		sX = 1e-9
	}
	if math.Abs(sY) < 1e-9 {
		sY = 1e-9
	}

	kX := kf.p[0][0] / sX
	kY := kf.p[1][1] / sY

	// 3. Measurement Update
	// Innovation y = z - H * x_pred
	innovX := dx - predX
	innovY := dy - predY

	kf.x = predX + kX*innovX
	kf.y = predY + kY*innovY

	// Update velocities
	kf.vx = (kf.x - predX) / dt
	kf.vy = (kf.y - predY) / dt

	// Update covariance
	kf.p[0][0] *= (1.0 - kX)
	kf.p[1][1] *= (1.0 - kY)

	// 4. Blending with raw input based on configured blendFactor
	finalDx := (1.0-kf.blendFactor)*dx + kf.blendFactor*kf.x
	finalDy := (1.0-kf.blendFactor)*dy + kf.blendFactor*kf.y

	// Check NaN / Inf guards
	if math.IsNaN(finalDx) || math.IsInf(finalDx, 0) {
		finalDx = dx
	}
	if math.IsNaN(finalDy) || math.IsInf(finalDy, 0) {
		finalDy = dy
	}

	return finalDx, finalDy
}

// SetBlendFactor updates the blending factor dynamically.
func (kf *KalmanFilter2D) SetBlendFactor(factor float64) {
	kf.mu.Lock()
	defer kf.mu.Unlock()
	if factor < 0.0 {
		factor = 0.0
	} else if factor > 1.0 {
		factor = 1.0
	}
	kf.blendFactor = factor
}
