package jitter

// Kalman1D is a simple 1D Kalman filter for velocity estimation.
type Kalman1D struct {
    x      float64 // estimated state (velocity)
    P      float64 // error covariance
    Q      float64 // process noise (how fast velocity can change)
    R      float64 // measurement noise (RSSI or movement delta noise)
    K      float64 // Kalman gain (recomputed each step)
}

// NewKalman1D creates a Kalman filter with default parameters.
func NewKalman1D(processNoise, measurementNoise float64) *Kalman1D {
    return &Kalman1D{
        x: 0,
        P: 1,
        Q: processNoise,
        R: measurementNoise,
    }
}

// Update processes a new measurement and returns the filtered value.
func (k *Kalman1D) Update(measurement float64) float64 {
    // Prediction
    k.P = k.P + k.Q

    // Update
    k.K = k.P / (k.P + k.R)
    k.x = k.x + k.K*(measurement - k.x)
    k.P = (1 - k.K) * k.P
    return k.x
}

// GetState returns the current velocity estimate.
func (k *Kalman1D) GetState() float64 {
    return k.x
}

// Reset resets the filter.
func (k *Kalman1D) Reset() {
    k.x = 0
    k.P = 1
}