package predictive

import (
    "testing"
    "math"
)

func TestKalmanFilterConvergence(t *testing.T) {
    kf := NewKalmanFilter2D(0.02)

    // Simulate constant velocity movement
    for i := 0; i < 200; i++ {
        kf.Predict()
        // Measurement: dx=1.0, dy=0.5 (pixels per timestep)
        kf.Update(1.0, 0.5)
    }

    x, y, vx, vy := kf.GetState()
    // After many steps, velocity should be close to true
    if math.Abs(vx-1.0) > 0.15 {
        t.Errorf("vx not converged: got %f, want ~1.0", vx)
    }
    if math.Abs(vy-0.5) > 0.15 {
        t.Errorf("vy not converged: got %f, want ~0.5", vy)
    }
    // Position should be plausible
    if x < 0 || x > 250 {
        t.Errorf("unexpected x position: %f", x)
    }
    if y < 0 || y > 150 {
        t.Errorf("unexpected y position: %f", y)
    }
}