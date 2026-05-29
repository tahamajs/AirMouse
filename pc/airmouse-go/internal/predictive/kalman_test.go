package predictive

import (
    "testing"
    "math"
)

func TestKalmanFilter(t *testing.T) {
    kf := NewKalmanFilter2D(0.02)
    // Simulate constant velocity
    for i := 0; i < 100; i++ {
        kf.Predict()
        kf.Update(1.0, 0.5) // dx, dy
    }
    _, _, vx, vy := kf.GetState()
    if math.Abs(vx-1.0) > 0.1 || math.Abs(vy-0.5) > 0.1 {
        t.Errorf("velocity not converging: vx=%f, vy=%f", vx, vy)
    }
}