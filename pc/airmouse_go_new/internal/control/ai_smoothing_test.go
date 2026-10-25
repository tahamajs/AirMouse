package control

import (
	"math"
	"math/rand"
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

func TestAISmootherMathStress(t *testing.T) {
	smoother := NewAISmoother("non_existent_model.onnx", 0.5)

	// 1. Feed 1,000 rapid jitter cycles
	r := rand.New(rand.NewSource(42))
	for i := 0; i < 1000; i++ {
		sign := 1.0
		if i%2 == 0 {
			sign = -1.0
		}
		jitterX := sign * (2.0 + r.Float64()*8.0)
		jitterY := -sign * (2.0 + r.Float64()*8.0)

		outX, outY := smoother.Smooth(jitterX, jitterY, 0.016)
		if math.IsNaN(outX) || math.IsInf(outX, 0) || math.IsNaN(outY) || math.IsInf(outY, 0) {
			t.Fatalf("Step %d: NaN or Inf detected in smoother output: outX=%f, outY=%f", i, outX, outY)
		}
	}

	// 2. Sudden 0-motion steps (100 steps of 0 motion)
	var finalX, finalY float64
	for i := 0; i < 100; i++ {
		finalX, finalY = smoother.Smooth(0.0, 0.0, 0.016)
		if math.IsNaN(finalX) || math.IsInf(finalX, 0) || math.IsNaN(finalY) || math.IsInf(finalY, 0) {
			t.Fatalf("Zero step %d: NaN or Inf detected in smoother output: outX=%f, outY=%f", i, finalX, finalY)
		}
	}

	// Verify settling to zero without oscillation
	if math.Abs(finalX) > 1e-3 || math.Abs(finalY) > 1e-3 {
		t.Errorf("Expected output to settle near 0 after 100 zero steps, got (%f, %f)", finalX, finalY)
	}
}

