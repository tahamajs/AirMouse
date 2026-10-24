package predictive

import (
	"testing"
)

func TestKalmanFilter2DBasic(t *testing.T) {
	kf := NewKalmanFilter2D(0.02)
	if kf == nil {
		t.Fatal("Failed to create KalmanFilter2D")
	}

	for i := 0; i < 20; i++ {
		kf.Predict()
		kf.Update(10.0, 5.0)
		predX, predY := kf.GetPredictedMovement()
		if i == 19 {
			t.Logf("Step 19 predicted: (%.2f, %.2f)", predX, predY)
			if predX == 0 && predY == 0 {
				t.Errorf("KalmanFilter2D predicted movement is zero")
			}
		}
	}

	x, y, vx, vy := kf.GetState()
	t.Logf("Final state: x=%.2f, y=%.2f, vx=%.2f, vy=%.2f", x, y, vx, vy)

	kf.Reset()
	rx, ry, rvx, rvy := kf.GetState()
	if rx != 0 || ry != 0 || rvx != 0 || rvy != 0 {
		t.Errorf("Reset failed, expected zeros, got (%f, %f, %f, %f)", rx, ry, rvx, rvy)
	}
}
