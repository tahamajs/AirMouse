package control

import (
	"testing"
)

func TestAISmootherFallback(t *testing.T) {
	smoother := NewAISmoother("non_existent_model.onnx", 0.5)
	if smoother.IsONNXLoaded() {
		t.Fatalf("Expected ONNX model to not be loaded")
	}

	// Test smooth call with small dt
	dx, dy := smoother.Smooth(10.0, 5.0, 0.016)
	if dx == 0 && dy == 0 {
		t.Errorf("Expected non-zero smoothed movement")
	}

	// Test smooth call with disabled
	smoother.SetEnabled(false)
	dx2, dy2 := smoother.Smooth(10.0, 5.0, 0.016)
	if dx2 != 10.0 || dy2 != 5.0 {
		t.Errorf("Expected raw values when disabled, got (%f, %f)", dx2, dy2)
	}
}
