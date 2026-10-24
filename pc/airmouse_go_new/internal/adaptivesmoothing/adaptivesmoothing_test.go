package adaptivesmoothing

import (
	"testing"
)

func TestBSpline3(t *testing.T) {
	points := [][2]float64{
		{0, 0},
		{10, 5},
		{20, 15},
		{30, 25},
		{40, 30},
	}
	spline := NewBSpline3(points, 10)
	if spline == nil {
		t.Fatal("Failed to create BSpline3")
	}

	smoothed := spline.SmoothPath()
	if len(smoothed) == 0 {
		t.Fatal("Expected smoothed points from BSpline3.SmoothPath()")
	}

	x0, y0 := spline.InterpolateAt(0.0)
	xMid, yMid := spline.InterpolateAt(0.5)
	x1, y1 := spline.InterpolateAt(1.0)
	t.Logf("BSpline points: start=(%.2f, %.2f), mid=(%.2f, %.2f), end=(%.2f, %.2f)", x0, y0, xMid, yMid, x1, y1)
}

func TestTremorSimulator(t *testing.T) {
	tremor := NewTremorSimulator()
	if tremor == nil {
		t.Fatal("Failed to create TremorSimulator")
	}

	dx, dy := tremor.Update()
	t.Logf("Tremor update: dx=%.3f, dy=%.3f", dx, dy)

	tremor.SetEnabled(false)
	dx, dy = tremor.Update()
	if dx != 0 || dy != 0 {
		t.Errorf("Expected 0 tremor when disabled, got (%.2f, %.2f)", dx, dy)
	}

	tremor.SetEnabled(true)
	tremor.SetAmplitude(5.0)
	tremor.SetFrequency(10.0)
}

func TestLogNormalVelocity(t *testing.T) {
	vel := NewLogNormalVelocity(100.0, 0.5)
	if vel == nil {
		t.Fatal("Failed to create LogNormalVelocity")
	}

	v0 := vel.VelocityAt(0.0)
	vMid := vel.VelocityAt(0.5)
	v1 := vel.VelocityAt(1.0)

	t.Logf("Velocity profile: t=0: %.2f, t=0.5: %.2f, t=1.0: %.2f", v0, vMid, v1)
	if vMid < v0 {
		t.Errorf("Expected peak velocity around mid to be higher than start")
	}
}

func TestHumanizer(t *testing.T) {
	cfg := DefaultHumanizerConfig()
	h := NewHumanizer(cfg)
	if h == nil {
		t.Fatal("Failed to create Humanizer")
	}

	for i := 0; i < 20; i++ {
		smDx, smDy := h.Process(10.0, 5.0, float64(i*10), float64(i*5))
		t.Logf("Humanized step %d: (%.2f, %.2f)", i, smDx, smDy)
	}

	stats := h.GetStats()
	if stats.TotalProcessed == 0 {
		t.Errorf("Expected TotalProcessed > 0, got %d", stats.TotalProcessed)
	}

	h.Reset()
	if h.GetStats().TotalProcessed != 0 {
		t.Errorf("Expected stats to be reset")
	}
}
