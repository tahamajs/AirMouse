package predict

import (
	"testing"
	"time"
)

func TestMovementPredictorOneEuroAndKalman(t *testing.T) {
	mp := NewMovementPredictor(0.02, 0.6)
	if !mp.IsEnabled() {
		t.Fatal("Expected predictor to be enabled")
	}

	for i := 0; i < 20; i++ {
		smX, smY := mp.AddMovement(10.0, 5.0)
		t.Logf("Step %d: smoothed (%.2f, %.2f)", i, smX, smY)
		time.Sleep(5 * time.Millisecond)
	}

	vx, vy := mp.GetVelocity()
	t.Logf("Estimated velocity: (%.2f, %.2f)", vx, vy)
	if vx == 0 && vy == 0 {
		t.Errorf("Expected non-zero velocity")
	}

	mp.EnableOneEuro(false)
	smX, smY := mp.AddMovement(10.0, 5.0)
	t.Logf("Without OneEuro: (%.2f, %.2f)", smX, smY)

	mp.Reset()
	rx, ry, rvx, rvy := mp.GetState()
	if rx != 0 || ry != 0 || rvx != 0 || rvy != 0 {
		t.Errorf("Expected zeros after reset, got (%f, %f, %f, %f)", rx, ry, rvx, rvy)
	}
}
