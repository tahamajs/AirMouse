package particlefilter

import (
	"testing"
)

func TestParticleFilterLifecycle(t *testing.T) {
	pf := NewFilter(100)
	if pf == nil {
		t.Fatal("Failed to create ParticleFilter")
	}

	for i := 0; i < 10; i++ {
		pf.Predict(0.02)
		pf.Update(float64(i*5), float64(i*2))
	}

	x, y, vx, vy, conf := pf.GetBestEstimate()
	t.Logf("Particle best estimate: x=%.2f, y=%.2f, vx=%.2f, vy=%.2f, conf=%.2f", x, y, vx, vy, conf)

	cloud := pf.GetParticleCloud()
	if len(cloud) != 100 {
		t.Errorf("Expected cloud size 100, got %d", len(cloud))
	}

	stats := pf.GetStats()
	if stats.Iterations == 0 {
		t.Errorf("Expected Iterations > 0")
	}

	pf.Reset()
}

func TestGestureRecognizer(t *testing.T) {
	rec := NewRecognizer()
	if rec == nil {
		t.Fatal("Failed to create Recognizer")
	}

	gestures := rec.GetAvailableGestures()
	if len(gestures) == 0 {
		t.Errorf("Expected predefined gestures")
	}

	// Feed horizontal swipe right
	for i := 0; i < 20; i++ {
		rec.AddMotion(15.0, 0.0)
	}

	gestureName, conf := rec.GetGesture()
	t.Logf("Recognized gesture: %s (confidence: %.2f)", gestureName, conf)

	rec.Reset()
}
