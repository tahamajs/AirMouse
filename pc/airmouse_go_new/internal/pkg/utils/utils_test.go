package utils

import (
	"math"
	"testing"
)

func TestUtilsMath(t *testing.T) {
	if c := Clamp(15.0, 0.0, 10.0); c != 10.0 {
		t.Errorf("Expected Clamp(15, 0, 10) = 10, got %f", c)
	}
	if c := Clamp(-5.0, 0.0, 10.0); c != 0.0 {
		t.Errorf("Expected Clamp(-5, 0, 10) = 0, got %f", c)
	}
	if c := Clamp(5.0, 0.0, 10.0); c != 5.0 {
		t.Errorf("Expected Clamp(5, 0, 10) = 5, got %f", c)
	}

	if m := MapRange(5.0, 0.0, 10.0, 0.0, 100.0); m != 50.0 {
		t.Errorf("Expected MapRange(5, 0, 10, 0, 100) = 50, got %f", m)
	}

	deg := RadToDeg(math.Pi)
	if math.Abs(deg-180.0) > 0.001 {
		t.Errorf("Expected 180 degrees, got %f", deg)
	}

	rad := DegToRad(180.0)
	if math.Abs(rad-math.Pi) > 0.001 {
		t.Errorf("Expected pi radians, got %f", rad)
	}

	vals := []float64{10.0, 20.0, 30.0}
	avg := FilterMovingAverage(vals, 2)
	if avg != 25.0 {
		t.Errorf("Expected moving average of last 2 (20, 30) = 25, got %f", avg)
	}

	id := GenerateID()
	if len(id) != 32 {
		t.Errorf("Expected 32-char hex ID, got len %d (%s)", len(id), id)
	}

	nowMs := TimeNowMs()
	if nowMs <= 0 {
		t.Errorf("Expected valid timestamp ms, got %d", nowMs)
	}
}
