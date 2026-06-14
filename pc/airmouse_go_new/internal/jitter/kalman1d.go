package jitter

import (
    "math"
    "sync"
)

type Kalman1D struct {
    x     float64 // State estimate
    P     float64 // Error covariance
    Q     float64 // Process noise
    R     float64 // Measurement noise
    K     float64 // Kalman gain
    mu    sync.RWMutex
    stats Kalman1DStats
}

type Kalman1DStats struct {
    Updates        int64
    AvgInnovation  float64
    LastInnovation float64
}

func NewKalman1D(processNoise, measurementNoise float64) *Kalman1D {
    return &Kalman1D{
        x:     0,
        P:     1,
        Q:     processNoise,
        R:     measurementNoise,
        stats: Kalman1DStats{}, // Initialize stats to zero values
    }
}

func (k *Kalman1D) Update(z float64) float64 {
    k.mu.Lock()
    defer k.mu.Unlock()

    // Prediction
    k.P = k.P + k.Q

    // Calculate innovation
    innovation := z - k.x

    // Calculate Kalman gain
    k.K = k.P / (k.P + k.R)

    // Update state estimate
    k.x = k.x + k.K*innovation

    // Update error covariance
    k.P = (1 - k.K) * k.P

    // Update statistics
    k.stats.Updates++
    k.stats.LastInnovation = math.Abs(innovation)
    // Calculate running average safely
    if k.stats.Updates > 1 {
        k.stats.AvgInnovation = (k.stats.AvgInnovation*float64(k.stats.Updates-1) + k.stats.LastInnovation) / float64(k.stats.Updates)
    } else {
        k.stats.AvgInnovation = k.stats.LastInnovation
    }

    return k.x
}

func (k *Kalman1D) Predict(dt float64) float64 {
    k.mu.RLock()
    defer k.mu.RUnlock()

    // Simple prediction using constant velocity model
    // For 1D Kalman, we assume velocity is the rate of change
    return k.x
}

func (k *Kalman1D) GetState() float64 {
    k.mu.RLock()
    defer k.mu.RUnlock()
    return k.x
}

func (k *Kalman1D) GetVariance() float64 {
    k.mu.RLock()
    defer k.mu.RUnlock()
    return k.P
}

func (k *Kalman1D) GetConfidence() float64 {
    k.mu.RLock()
    defer k.mu.RUnlock()

    // Confidence based on covariance (lower variance = higher confidence)
    confidence := 1.0 / (1.0 + k.P/10.0)
    if confidence > 1.0 {
        confidence = 1.0
    }
    if confidence < 0.0 {
        confidence = 0.0
    }
    return confidence
}

func (k *Kalman1D) SetNoise(processNoise, measurementNoise float64) {
    k.mu.Lock()
    defer k.mu.Unlock()
    k.Q = processNoise
    k.R = measurementNoise
}

func (k *Kalman1D) Reset() {
    k.mu.Lock()
    defer k.mu.Unlock()
    k.x = 0
    k.P = 1
    k.stats = Kalman1DStats{} // Reset stats to zero
}

func (k *Kalman1D) GetStats() Kalman1DStats {
    k.mu.RLock()
    defer k.mu.RUnlock()
    return k.stats
}