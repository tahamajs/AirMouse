package control

import "testing"

func TestMovementPredictorUpdateAndReset(t *testing.T) {
	p := NewMovementPredictor(0.02, 0.5)

	dx, dy := p.AddMovement(10, -4)
	if dx == 0 && dy == 0 {
		t.Fatalf("expected non-zero smoothed movement")
	}

	p.SetEnabled(false)
	dx, dy = p.AddMovement(7, 3)
	if dx != 7 || dy != 3 {
		t.Fatalf("disabled predictor should pass through values, got (%v, %v)", dx, dy)
	}

	p.SetEnabled(true)
	p.Reset()
	x, y, vx, vy := p.kf.GetState()
	if x != 0 || y != 0 || vx != 0 || vy != 0 {
		t.Fatalf("reset state = (%v,%v,%v,%v), want all zeros", x, y, vx, vy)
	}
}

func TestMovementPauseFlag(t *testing.T) {
	SetMovementPaused(true)
	if !IsMovementPaused() {
		t.Fatal("expected movement to be paused")
	}
	SetMovementPaused(false)
	if IsMovementPaused() {
		t.Fatal("expected movement to be resumed")
	}
}
