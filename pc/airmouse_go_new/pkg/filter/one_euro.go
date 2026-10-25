// Package filter provides high-performance, real-time signal processing and sensor fusion filters
// tailored for air mice, spatial controllers, and motion tracking devices.
//
// It includes:
//   - OneEuroFilter: Speed-adaptive low-pass filter (Casiez et al., CHI 2012) for jitter reduction.
//   - KalmanFilter2D: 4-state constant-velocity Kalman filter for 2D position and velocity estimation.
//   - MadgwickAHRS: 6-DOF / 9-DOF orientation sensor fusion algorithm (IMU and MARG).
package filter

import (
	"math"
	"sync"
	"time"
)

// LowPassFilter implements a basic first-order exponential smoothing filter.
type LowPassFilter struct {
	hasLast bool
	lastRaw float64
	lastHat float64
}

// Filter computes the exponentially smoothed value given smoothing factor alpha.
func (f *LowPassFilter) Filter(val, alpha float64) float64 {
	if !f.hasLast {
		f.hasLast = true
		f.lastRaw = val
		f.lastHat = val
		return val
	}
	f.lastRaw = val
	f.lastHat = alpha*val + (1.0-alpha)*f.lastHat
	return f.lastHat
}

// Reset clears the filter state.
func (f *LowPassFilter) Reset() {
	f.hasLast = false
	f.lastRaw = 0
	f.lastHat = 0
}

// Last returns the most recent filtered value.
func (f *LowPassFilter) Last() float64 {
	return f.lastHat
}

// OneEuroFilter implements the 1€ Filter (Casiez, Roussel, Vogel, CHI 2012).
// It is an adaptive first-order low-pass filter that dynamically adjusts its cutoff frequency
// based on speed, eliminating low-speed jitter while introducing virtually zero latency at high speeds.
type OneEuroFilter struct {
	minCutoff float64 // Minimum cutoff frequency in Hz (jitter reduction at low speed)
	beta      float64 // Speed coefficient (reduces lag at high speed)
	dCutoff   float64 // Cutoff frequency for derivative low-pass filter in Hz

	xFilter  LowPassFilter
	dxFilter LowPassFilter

	lastTime    time.Time
	initialized bool
	mu          sync.Mutex
}

// NewOneEuroFilter creates a new 1D One Euro filter.
// Recommended defaults for airmouse / mouse tracking:
// minCutoff: 1.0 Hz, beta: 0.007 (or 0.05 for faster response), dCutoff: 1.0 Hz.
func NewOneEuroFilter(minCutoff, beta, dCutoff float64) *OneEuroFilter {
	if minCutoff <= 0 {
		minCutoff = 1.0
	}
	if beta < 0 {
		beta = 0.007
	}
	if dCutoff <= 0 {
		dCutoff = 1.0
	}
	return &OneEuroFilter{
		minCutoff: minCutoff,
		beta:      beta,
		dCutoff:   dCutoff,
	}
}

// alpha calculates the smoothing factor alpha from cutoff frequency fc and sample interval dt.
func computeAlpha(dt, fc float64) float64 {
	if fc <= 0 || math.IsNaN(fc) || math.IsInf(fc, 0) {
		fc = 1e-4
	}
	if dt <= 0 || math.IsNaN(dt) || math.IsInf(dt, 0) {
		dt = 0.016
	}
	tau := 1.0 / (2.0 * math.Pi * fc)
	return 1.0 / (1.0 + tau/dt)
}

// Filter filters a single scalar value. If timestamp is zero, a default interval dt (0.02s) is assumed.
func (o *OneEuroFilter) Filter(val float64, timestamp time.Time) float64 {
	o.mu.Lock()
	defer o.mu.Unlock()

	if !o.initialized {
		o.initialized = true
		o.lastTime = timestamp
		_ = o.xFilter.Filter(val, 1.0)
		_ = o.dxFilter.Filter(0, 1.0)
		return val
	}

	dt := 0.02 // default 50Hz if timestamp unavailable
	if !timestamp.IsZero() && !o.lastTime.IsZero() {
		dt = timestamp.Sub(o.lastTime).Seconds()
		if dt <= 0.0001 {
			dt = 0.001
		} else if dt > 0.2 {
			dt = 0.2
		}
	}
	o.lastTime = timestamp

	// Estimate raw derivative (velocity)
	var dx float64
	if o.xFilter.hasLast {
		dx = (val - o.xFilter.lastHat) / dt
	}

	// Filter the derivative
	alphaD := computeAlpha(dt, o.dCutoff)
	edx := o.dxFilter.Filter(dx, alphaD)

	// Compute dynamic cutoff frequency based on filtered speed
	cutoff := o.minCutoff + o.beta*math.Abs(edx)

	// Filter the signal
	alphaX := computeAlpha(dt, cutoff)
	return o.xFilter.Filter(val, alphaX)
}

// SetParams dynamically updates filter parameters.
func (o *OneEuroFilter) SetParams(minCutoff, beta, dCutoff float64) {
	o.mu.Lock()
	defer o.mu.Unlock()
	if minCutoff > 0 {
		o.minCutoff = minCutoff
	}
	if beta >= 0 {
		o.beta = beta
	}
	if dCutoff > 0 {
		o.dCutoff = dCutoff
	}
}

// Reset resets the filter state and history.
func (o *OneEuroFilter) Reset() {
	o.mu.Lock()
	defer o.mu.Unlock()
	o.initialized = false
	o.lastTime = time.Time{}
	o.xFilter.Reset()
	o.dxFilter.Reset()
}

// OneEuroFilter2D filters 2D coordinates or movement deltas (dx, dy).
type OneEuroFilter2D struct {
	xFilter *OneEuroFilter
	yFilter *OneEuroFilter
	mu      sync.Mutex
}

// NewOneEuroFilter2D creates a 2D One Euro filter with the given cutoff and speed coefficient.
func NewOneEuroFilter2D(minCutoff, beta, dCutoff float64) *OneEuroFilter2D {
	return &OneEuroFilter2D{
		xFilter: NewOneEuroFilter(minCutoff, beta, dCutoff),
		yFilter: NewOneEuroFilter(minCutoff, beta, dCutoff),
	}
}

// Filter filters 2D movement deltas or positions (x, y) at the specified timestamp.
func (o2 *OneEuroFilter2D) Filter(x, y float64, timestamp time.Time) (float64, float64) {
	o2.mu.Lock()
	defer o2.mu.Unlock()

	fx := o2.xFilter.Filter(x, timestamp)
	fy := o2.yFilter.Filter(y, timestamp)
	return fx, fy
}

// SetParams dynamically updates filter parameters on both X and Y axes.
func (o2 *OneEuroFilter2D) SetParams(minCutoff, beta, dCutoff float64) {
	o2.mu.Lock()
	defer o2.mu.Unlock()
	o2.xFilter.SetParams(minCutoff, beta, dCutoff)
	o2.yFilter.SetParams(minCutoff, beta, dCutoff)
}

// Reset resets both X and Y filters.
func (o2 *OneEuroFilter2D) Reset() {
	o2.mu.Lock()
	defer o2.mu.Unlock()
	o2.xFilter.Reset()
	o2.yFilter.Reset()
}
