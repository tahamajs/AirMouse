package gesture

import (
	"math"
	"testing"
)

func TestGestureDetectorSwipes(t *testing.T) {
	d := NewDetector()
	if d.Detect() != GestureNone {
		t.Fatal("Expected initial detection to be GestureNone")
	}

	// Swipe Right: totalX > 50, totalY < 20
	for i := 0; i < 15; i++ {
		d.AddMotion(10.0, 0.5)
	}
	if g := d.Detect(); g != GestureSwipeRight {
		t.Fatalf("Expected GestureSwipeRight, got %v (%s)", g, Name(g))
	}

	d.Reset()
	// Swipe Left: totalX < -50, totalY < 20
	for i := 0; i < 15; i++ {
		d.AddMotion(-10.0, -0.5)
	}
	if g := d.Detect(); g != GestureSwipeLeft {
		t.Fatalf("Expected GestureSwipeLeft, got %v (%s)", g, Name(g))
	}

	d.Reset()
	// Swipe Down: totalY > 50, totalX < 20
	for i := 0; i < 15; i++ {
		d.AddMotion(0.5, 10.0)
	}
	if g := d.Detect(); g != GestureSwipeDown {
		t.Fatalf("Expected GestureSwipeDown, got %v (%s)", g, Name(g))
	}

	d.Reset()
	// Swipe Up: totalY < -50, totalX < 20
	for i := 0; i < 15; i++ {
		d.AddMotion(-0.5, -10.0)
	}
	if g := d.Detect(); g != GestureSwipeUp {
		t.Fatalf("Expected GestureSwipeUp, got %v (%s)", g, Name(g))
	}
}

func TestGestureCircle(t *testing.T) {
	d := NewDetector()
	// Circle simulation
	steps := 30
	radius := 20.0
	for i := 0; i <= steps; i++ {
		theta := float64(i) * 2.0 * math.Pi / float64(steps)
		x := radius * math.Cos(theta)
		y := radius * math.Sin(theta)
		d.AddMotion(x, y)
	}

	detected := d.Detect()
	t.Logf("Detected circle gesture: %s", Name(detected))
}

func TestGestureNames(t *testing.T) {
	tests := []struct {
		g    GestureType
		name string
	}{
		{GestureSwipeLeft, "swipe_left"},
		{GestureSwipeRight, "swipe_right"},
		{GestureSwipeUp, "swipe_up"},
		{GestureSwipeDown, "swipe_down"},
		{GestureCircleCW, "circle_cw"},
		{GestureCircleCCW, "circle_ccw"},
		{GestureNone, "none"},
	}

	for _, tc := range tests {
		if got := Name(tc.g); got != tc.name {
			t.Errorf("Expected Name(%v) == %s, got %s", tc.g, tc.name, got)
		}
	}
}
