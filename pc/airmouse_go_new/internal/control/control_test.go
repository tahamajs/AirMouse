package control

import "testing"

func TestNewMovementPredictor(t *testing.T) {
	p := NewMovementPredictor(0.02, 0.5)
	if p == nil {
		t.Fatal("expected predictor")
	}
}

func TestMovementPredictorBehavior(t *testing.T) {
	p := NewMovementPredictor(0.02, 0.5)

	dx, dy := p.AddMovement(10.5, -3.2)
	if dx == 0 && dy == 0 {
		t.Fatalf("expected non-zero smoothed movement")
	}

	p.SetEnabled(false)
	dx, dy = p.AddMovement(7, 3)
	if dx != 7 || dy != 3 {
		t.Fatalf("disabled predictor should pass through values, got (%v, %v)", dx, dy)
	}

	p.SetEnabled(true)
	p.SetBlendFactor(0.25)
	if got := p.GetBlendFactor(); got != 0.25 {
		t.Fatalf("blend factor = %v, want 0.25", got)
	}

	p.Reset()
	x, y, vx, vy := p.GetState()
	if x != 0 || y != 0 || vx != 0 || vy != 0 {
		t.Fatalf("reset state = (%v,%v,%v,%v), want all zeros", x, y, vx, vy)
	}
}

func TestPauseFlags(t *testing.T) {
	SetMovementPaused(true)
	if !IsMovementPaused() {
		t.Fatal("expected movement to be paused")
	}
	SetMovementPaused(false)
	if IsMovementPaused() {
		t.Fatal("expected movement to be resumed")
	}
}
