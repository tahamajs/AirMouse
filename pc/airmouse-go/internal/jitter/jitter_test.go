package jitter

import (
    "testing"
    "time"
    "math"
)

func TestJitterBuffer(t *testing.T) {
    cfg := DefaultJitterBufferConfig()
    jb := NewJitterBuffer(cfg)

    // Simulate a series of movements with constant velocity
    start := time.Now()
    for i := 0; i < 10; i++ {
        now := start.Add(time.Duration(i*20) * time.Millisecond)
        dx, dy := 10.0, 5.0
        smoothedDx, smoothedDy := jb.AddMovement(dx, dy, now)
        if math.Abs(smoothedDx-dx) > 2 || math.Abs(smoothedDy-dy) > 2 {
            t.Errorf("Smoothed movement too far from actual: got (%.2f, %.2f), expected (%.2f, %.2f)",
                smoothedDx, smoothedDy, dx, dy)
        }
    }

    // Predict when no packet arrives
    predDx, predDy := jb.PredictNow()
    if predDx == 0 && predDy == 0 {
        t.Error("Prediction returned zero movement")
    }
    t.Logf("Predicted movement: (%.2f, %.2f)", predDx, predDy)
}

func TestKalmanFilter(t *testing.T) {
    kf := NewKalman1D(0.5, 1.0)
    measurements := []float64{10, 12, 11, 13, 10, 14}
    for _, m := range measurements {
        filtered := kf.Update(m)
        t.Logf("raw %.2f → filtered %.2f", m, filtered)
    }
    if kf.GetState() == 0 {
        t.Error("Kalman filter not converging")
    }
}