package filter

import (
	"math"
	"sync"
	"time"
)

// KalmanFilter2D implements a constant-velocity Kalman filter for 2D motion tracking and prediction.
// State vector: [x, y, vx, vy] representing 2D position and velocity.
type KalmanFilter2D struct {
	// State vector [x, y, vx, vy]
	x           [4]float64
	P           [4][4]float64 // Covariance matrix
	F           [4][4]float64 // State transition matrix
	H           [4][4]float64 // Observation matrix
	Q           [4][4]float64 // Process noise covariance
	R           [4][4]float64 // Measurement noise covariance
	dt          float64
	mu          sync.RWMutex
	initialized bool
	stats       KalmanStats
}

// KalmanStats holds runtime statistics for convergence and prediction accuracy.
type KalmanStats struct {
	TotalUpdates    int64
	AvgInnovation   float64
	LastUpdateTime  time.Time
	ConvergenceTime time.Duration
}

// NewKalmanFilter2D creates a new Kalman filter with the given time step dt (in seconds).
func NewKalmanFilter2D(dt float64) *KalmanFilter2D {
	if dt <= 0 {
		dt = 0.02
	}
	kf := &KalmanFilter2D{
		dt:          dt,
		initialized: true,
	}

	// State initially zero
	kf.x = [4]float64{0, 0, 0, 0}

	// Initial uncertainty
	kf.P = [4][4]float64{
		{100, 0, 0, 0},
		{0, 100, 0, 0},
		{0, 0, 100, 0},
		{0, 0, 0, 100},
	}

	// Constant-velocity state transition
	kf.F = [4][4]float64{
		{1, 0, dt, 0},
		{0, 1, 0, dt},
		{0, 0, 1, 0},
		{0, 0, 0, 1},
	}

	// Observation matrix
	kf.H = [4][4]float64{
		{1, 0, 0, 0},
		{0, 1, 0, 0},
		{0, 0, 1, 0},
		{0, 0, 0, 1},
	}

	// Process noise
	kf.Q = [4][4]float64{
		{0.1, 0, 0, 0},
		{0, 0.1, 0, 0},
		{0, 0, 0.5, 0},
		{0, 0, 0.5, 0},
	}

	// Measurement noise
	kf.R = [4][4]float64{
		{5, 0, 0, 0},
		{0, 5, 0, 0},
		{0, 0, 15, 0},
		{0, 0, 0, 15},
	}

	return kf
}

// Predict advances the state vector and covariance by dt.
func (kf *KalmanFilter2D) Predict() {
	kf.mu.Lock()
	defer kf.mu.Unlock()

	if !kf.initialized {
		return
	}

	// x = F * x
	var newX [4]float64
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += kf.F[i][j] * kf.x[j]
		}
		newX[i] = sum
	}
	kf.x = newX

	// P = F * P * F^T + Q
	var FP [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += kf.F[i][k] * kf.P[k][j]
			}
			FP[i][j] = sum
		}
	}

	var Pnew [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += FP[i][k] * kf.F[j][k]
			}
			Pnew[i][j] = sum + kf.Q[i][j]
		}
	}
	kf.P = Pnew
}

// Update incorporates a new measurement (dx, dy).
func (kf *KalmanFilter2D) Update(dx, dy float64) {
	kf.mu.Lock()
	defer kf.mu.Unlock()

	if !kf.initialized {
		kf.x[2] = dx / kf.dt
		kf.x[3] = dy / kf.dt
		kf.initialized = true
		return
	}

	// Measurement vector: [dx, dy, dx/dt, dy/dt]
	z := [4]float64{dx, dy, dx / kf.dt, dy / kf.dt}

	// Innovation: y = z - H*x
	var Hx [4]float64
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += kf.H[i][j] * kf.x[j]
		}
		Hx[i] = sum
	}
	y := [4]float64{
		z[0] - Hx[0],
		z[1] - Hx[1],
		z[2] - Hx[2],
		z[3] - Hx[3],
	}

	// S = H*P*H^T + R
	var HP [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += kf.H[i][k] * kf.P[k][j]
			}
			HP[i][j] = sum
		}
	}
	var S [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += HP[i][k] * kf.H[j][k]
			}
			S[i][j] = sum + kf.R[i][j]
		}
	}

	// Kalman gain: K = P*H^T * inv(S)
	invS := kf.invertMatrix4x4(S)
	var K [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += HP[i][k] * invS[k][j]
			}
			K[i][j] = sum
		}
	}

	// State update: x = x + K*y
	for i := 0; i < 4; i++ {
		sum := 0.0
		for j := 0; j < 4; j++ {
			sum += K[i][j] * y[j]
		}
		kf.x[i] += sum
	}

	// Covariance update: P = (I - K*H)*P
	var KH [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sum := 0.0
			for k := 0; k < 4; k++ {
				sum += K[i][k] * kf.H[k][j]
			}
			KH[i][j] = sum
		}
	}
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			kf.P[i][j] -= KH[i][j]
		}
	}

	// Update runtime statistics
	kf.stats.TotalUpdates++
	innovation := math.Sqrt(y[0]*y[0] + y[1]*y[1])
	kf.stats.AvgInnovation = (kf.stats.AvgInnovation*float64(kf.stats.TotalUpdates-1) + innovation) / float64(kf.stats.TotalUpdates)
	kf.stats.LastUpdateTime = time.Now()
	if kf.stats.TotalUpdates == 1 {
		kf.stats.ConvergenceTime = 0
	} else if kf.stats.ConvergenceTime == 0 && innovation < 0.1 {
		kf.stats.ConvergenceTime = time.Since(kf.stats.LastUpdateTime)
	}
}

// invertMatrix4x4 computes the inverse of a 4x4 matrix using the adjugate method.
func (kf *KalmanFilter2D) invertMatrix4x4(m [4][4]float64) [4][4]float64 {
	det := kf.determinant4x4(m)
	if math.Abs(det) < 1e-12 {
		return [4][4]float64{
			{1, 0, 0, 0},
			{0, 1, 0, 0},
			{0, 0, 1, 0},
			{0, 0, 0, 1},
		}
	}
	adj := kf.adjugate4x4(m)
	var inv [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			inv[i][j] = adj[i][j] / det
		}
	}
	return inv
}

// determinant4x4 computes the determinant of a 4x4 matrix.
func (kf *KalmanFilter2D) determinant4x4(m [4][4]float64) float64 {
	return m[0][0]*kf.determinant3x3(m, 0, 0) -
		m[0][1]*kf.determinant3x3(m, 0, 1) +
		m[0][2]*kf.determinant3x3(m, 0, 2) -
		m[0][3]*kf.determinant3x3(m, 0, 3)
}

func (kf *KalmanFilter2D) determinant3x3(m [4][4]float64, row, col int) float64 {
	var sub [3][3]float64
	r := 0
	for i := 0; i < 4; i++ {
		if i == row {
			continue
		}
		c := 0
		for j := 0; j < 4; j++ {
			if j == col {
				continue
			}
			sub[r][c] = m[i][j]
			c++
		}
		r++
	}
	return sub[0][0]*sub[1][1]*sub[2][2] +
		sub[0][1]*sub[1][2]*sub[2][0] +
		sub[0][2]*sub[1][0]*sub[2][1] -
		sub[0][2]*sub[1][1]*sub[2][0] -
		sub[0][1]*sub[1][0]*sub[2][2] -
		sub[0][0]*sub[1][2]*sub[2][1]
}

func (kf *KalmanFilter2D) adjugate4x4(m [4][4]float64) [4][4]float64 {
	var adj [4][4]float64
	for i := 0; i < 4; i++ {
		for j := 0; j < 4; j++ {
			sign := 1.0
			if (i+j)%2 == 1 {
				sign = -1.0
			}
			adj[j][i] = sign * kf.determinant3x3(m, i, j)
		}
	}
	return adj
}

// GetPredictedMovement returns the predicted displacement (dx, dy) based on current velocity.
func (kf *KalmanFilter2D) GetPredictedMovement() (dx, dy float64) {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	dx = kf.x[2] * kf.dt
	dy = kf.x[3] * kf.dt
	return
}

// GetState returns the full state vector (x, y, vx, vy).
func (kf *KalmanFilter2D) GetState() (x, y, vx, vy float64) {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	return kf.x[0], kf.x[1], kf.x[2], kf.x[3]
}

// GetPosition returns the current estimated position (x, y).
func (kf *KalmanFilter2D) GetPosition() (x, y float64) {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	return kf.x[0], kf.x[1]
}

// GetVelocity returns the current estimated velocity (vx, vy).
func (kf *KalmanFilter2D) GetVelocity() (vx, vy float64) {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	return kf.x[2], kf.x[3]
}

// GetConfidence returns a confidence metric in [0.0, 1.0] derived from the trace of covariance matrix P.
func (kf *KalmanFilter2D) GetConfidence() float64 {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	trace := kf.P[0][0] + kf.P[1][1] + kf.P[2][2] + kf.P[3][3]
	conf := 1.0 / (1.0 + trace/100.0)
	if conf > 1.0 {
		return 1.0
	}
	if conf < 0.0 {
		return 0.0
	}
	return conf
}

// Reset clears the state vector and restores default covariance uncertainty.
func (kf *KalmanFilter2D) Reset() {
	kf.mu.Lock()
	defer kf.mu.Unlock()
	kf.x = [4]float64{0, 0, 0, 0}
	kf.P = [4][4]float64{
		{100, 0, 0, 0},
		{0, 100, 0, 0},
		{0, 0, 100, 0},
		{0, 0, 0, 100},
	}
	kf.initialized = false
	kf.stats = KalmanStats{}
}

// SetDT updates the time step and state transition matrix.
func (kf *KalmanFilter2D) SetDT(dt float64) {
	kf.mu.Lock()
	defer kf.mu.Unlock()
	kf.dt = dt
	kf.F[0][2] = dt
	kf.F[1][3] = dt
}

// SetNoise sets diagonal elements for process noise Q and measurement noise R.
func (kf *KalmanFilter2D) SetNoise(processNoise, measurementNoise float64) {
	kf.mu.Lock()
	defer kf.mu.Unlock()
	for i := 0; i < 4; i++ {
		kf.Q[i][i] = processNoise
		kf.R[i][i] = measurementNoise
	}
}

// GetStats returns a copy of current tracking statistics.
func (kf *KalmanFilter2D) GetStats() KalmanStats {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	return kf.stats
}

// IsInitialized returns true if the filter has received at least one measurement.
func (kf *KalmanFilter2D) IsInitialized() bool {
	kf.mu.RLock()
	defer kf.mu.RUnlock()
	return kf.initialized
}

// PredictiveMotionFilter combines One Euro adaptive smoothing with Kalman prediction.
type PredictiveMotionFilter struct {
	oneEuro     *OneEuroFilter2D
	kalman      *KalmanFilter2D
	blendFactor float64
	useOneEuro  bool
	useKalman   bool
	mu          sync.Mutex
}

// NewPredictiveMotionFilter creates a combined One-Euro / Kalman filter.
func NewPredictiveMotionFilter(minCutoff, beta, dCutoff, blendFactor float64) *PredictiveMotionFilter {
	return &PredictiveMotionFilter{
		oneEuro:     NewOneEuroFilter2D(minCutoff, beta, dCutoff),
		kalman:      NewKalmanFilter2D(0.02),
		blendFactor: blendFactor,
		useOneEuro:  true,
		useKalman:   true,
	}
}

// FilterAndPredict processes input delta (dx, dy) and returns smoothed and predicted deltas.
func (pmf *PredictiveMotionFilter) FilterAndPredict(dx, dy float64, dt float64, timestamp time.Time) (smoothedDx, smoothedDy, predDx, predDy float64) {
	pmf.mu.Lock()
	defer pmf.mu.Unlock()

	curDx, curDy := dx, dy

	if pmf.useOneEuro {
		curDx, curDy = pmf.oneEuro.Filter(curDx, curDy, timestamp)
	}

	smoothedDx, smoothedDy = curDx, curDy

	if pmf.useKalman {
		if dt <= 0.0001 {
			dt = 0.02
		}
		pmf.kalman.SetDT(dt)
		pmf.kalman.Predict()
		pmf.kalman.Update(curDx, curDy)
		predDx, predDy = pmf.kalman.GetPredictedMovement()

		smoothedDx = (1.0-pmf.blendFactor)*curDx + pmf.blendFactor*predDx
		smoothedDy = (1.0-pmf.blendFactor)*curDy + pmf.blendFactor*predDy
	} else {
		predDx, predDy = curDx, curDy
	}

	return smoothedDx, smoothedDy, predDx, predDy
}

// Reset clears the filter states.
func (pmf *PredictiveMotionFilter) Reset() {
	pmf.mu.Lock()
	defer pmf.mu.Unlock()
	pmf.oneEuro.Reset()
	pmf.kalman.Reset()
}
