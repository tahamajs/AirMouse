package proximity

import (
    "math"
)

type RSSIFusion struct {
    txPower      int32   // RSSI at 1 meter
    envFactor    float64 // Environmental factor (2.0-4.0)
    smoothing    float64 // Smoothing factor
    lastDistance float64
}

func NewRSSIFusion(txPower int32, envFactor float64) *RSSIFusion {
    return &RSSIFusion{
        txPower:      txPower,
        envFactor:    envFactor,
        smoothing:    0.3,
        lastDistance: 0,
    }
}

func (rf *RSSIFusion) RSSIToDistance(rssi int32) float64 {
    // Path loss model: distance = 10^((txPower - rssi) / (10 * n))
    ratio := float64(rf.txPower-rssi) / (10 * rf.envFactor)
    distance := math.Pow(10, ratio)
    
    // Apply smoothing
    if rf.lastDistance > 0 {
        distance = rf.smoothing*distance + (1-rf.smoothing)*rf.lastDistance
    }
    rf.lastDistance = distance
    
    // Clamp to reasonable values
    if distance < 0.3 {
        distance = 0.3
    }
    if distance > 10.0 {
        distance = 10.0
    }
    
    return distance
}

func (rf *RSSIFusion) DistanceToRSSI(distance float64) int32 {
    // Inverse of path loss model
    if distance <= 0 {
        distance = 0.3
    }
    rssi := float64(rf.txPower) - 10*rf.envFactor*math.Log10(distance)
    return int32(rssi)
}

func (rf *RSSIFusion) SetTxPower(txPower int32) {
    rf.txPower = txPower
}

func (rf *RSSIFusion) SetEnvFactor(factor float64) {
    rf.envFactor = factor
}

func (rf *RSSIFusion) GetConfidence(rssi int32) float64 {
    // RSSI typically ranges from -30 to -90
    // Higher RSSI (less negative) = higher confidence
    normalized := (float64(rssi) + 90) / 60
    if normalized < 0 {
        normalized = 0
    }
    if normalized > 1 {
        normalized = 1
    }
    return normalized
}

func (rf *RSSIFusion) AdaptiveSmoothing(rssi int32) float64 {
    // More smoothing for noisy signals
    confidence := rf.GetConfidence(rssi)
    if confidence < 0.3 {
        return 0.8 // High smoothing
    } else if confidence < 0.6 {
        return 0.5 // Medium smoothing
    }
    return 0.2 // Low smoothing
}

// Kalman filter for RSSI smoothing
type KalmanFilter struct {
    Q float64 // Process noise covariance
    R float64 // Measurement noise covariance
    P float64 // Estimation error covariance
    K float64 // Kalman gain
    X float64 // State estimate
}

func NewKalmanFilter() *KalmanFilter {
    return &KalmanFilter{
        Q: 0.01,
        R: 0.1,
        P: 1.0,
        X: -60,
    }
}

func (kf *KalmanFilter) Update(measurement float64) float64 {
    // Prediction update
    kf.P = kf.P + kf.Q
    
    // Measurement update
    kf.K = kf.P / (kf.P + kf.R)
    kf.X = kf.X + kf.K*(measurement-kf.X)
    kf.P = (1 - kf.K) * kf.P
    
    return kf.X
}

func (kf *KalmanFilter) Reset() {
    kf.P = 1.0
    kf.X = -60
}