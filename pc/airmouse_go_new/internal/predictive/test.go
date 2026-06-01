package predictive

import (
	"math"
	"testing"
)

func TestKalmanFilterConvergence(t *testing.T) {
	kf := NewKalmanFilter2D(0.02)
	for i := 0; i < 200; i++ {
		kf.Predict()
		kf.Update(1.0, 0.5)
	}
	_, _, vx, vy := kf.GetState()
	if math.Abs(vx-1.0) > 0.15 {
		t.Errorf("vx not converged: got %f, want ~1.0", vx)
	}
	if math.Abs(vy-0.5) > 0.15 {
		t.Errorf("vy not converged: got %f, want ~0.5", vy)
	}
}
