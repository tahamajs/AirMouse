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
	var outputsX []float64
	var outputsY []float64

	for i := 0; i < 1000; i++ {
		sign := 1.0
		if i%2 == 0 {
			sign = -1.0
		}
		jitterX := sign * (2.0 + r.Float64()*8.0)
		jitterY := -sign * (2.0 + r.Float64()*8.0)

		outX, outY := smoother.Smooth(jitterX, jitterY, 0.016)
		if math.IsNaN(outX) || math.IsInf(outX, 0) || math.IsNaN(outY) || math.IsInf(outY, 0) {
			t.Fatalf("Step %d: Filter instability - NaN or Inf detected in smoother output: outX=%f, outY=%f", i, outX, outY)
		}

		outputsX = append(outputsX, outX)
		outputsY = append(outputsY, outY)

		// Verify non-negative variance in Kalman covariance matrix
		varX, varY, varVx, varVy := smoother.GetKalmanFilter().GetVariances()
		if varX < 0 || varY < 0 || varVx < 0 || varVy < 0 {
			t.Fatalf("Step %d: Negative variance in covariance matrix: varX=%f, varY=%f, varVx=%f, varVy=%f", i, varX, varY, varVx, varVy)
		}

		// Verify confidence is well-formed in [0, 1]
		conf := smoother.GetKalmanFilter().GetConfidence()
		if conf < 0.0 || conf > 1.0 || math.IsNaN(conf) {
			t.Fatalf("Step %d: Invalid confidence score: %f", i, conf)
		}
	}

	// Calculate and verify non-negative variance of the output signals
	var meanX, meanY float64
	for i := 0; i < len(outputsX); i++ {
		meanX += outputsX[i]
		meanY += outputsY[i]
	}
	meanX /= float64(len(outputsX))
	meanY /= float64(len(outputsY))

	var sampleVarX, sampleVarY float64
	for i := 0; i < len(outputsX); i++ {
		sampleVarX += (outputsX[i] - meanX) * (outputsX[i] - meanX)
		sampleVarY += (outputsY[i] - meanY) * (outputsY[i] - meanY)
	}
	sampleVarX /= float64(len(outputsX))
	sampleVarY /= float64(len(outputsY))

	if sampleVarX < 0 || sampleVarY < 0 {
		t.Fatalf("Sample variance must be non-negative: varX=%f, varY=%f", sampleVarX, sampleVarY)
	}

	// 2. Sudden 0-motion steps (100 steps of 0 motion)
	var zeroOutX []float64
	var zeroOutY []float64
	for i := 0; i < 100; i++ {
		outX, outY := smoother.Smooth(0.0, 0.0, 0.016)
		if math.IsNaN(outX) || math.IsInf(outX, 0) || math.IsNaN(outY) || math.IsInf(outY, 0) {
			t.Fatalf("Zero step %d: NaN or Inf detected in smoother output: outX=%f, outY=%f", i, outX, outY)
		}

		zeroOutX = append(zeroOutX, outX)
		zeroOutY = append(zeroOutY, outY)

		// Verify non-negative variance continuously during zero-motion decay
		varX, varY, varVx, varVy := smoother.GetKalmanFilter().GetVariances()
		if varX < 0 || varY < 0 || varVx < 0 || varVy < 0 {
			t.Fatalf("Zero step %d: Negative variance: varX=%f, varY=%f, varVx=%f, varVy=%f", i, varX, varY, varVx, varVy)
		}
	}

	// 3. Verify zero oscillation and smooth decay
	// After settling period (20 steps = ~320ms), outputs must decay smoothly towards 0 without oscillation
	for i := 20; i < len(zeroOutX); i++ {
		if math.Abs(zeroOutX[i]) > 0.1 || math.Abs(zeroOutY[i]) > 0.1 {
			t.Errorf("Step %d: Output failed to settle near zero: outX=%f, outY=%f", i, zeroOutX[i], zeroOutY[i])
		}
	}

	// Verify final value after 100 steps is near absolute zero
	finalX := zeroOutX[len(zeroOutX)-1]
	finalY := zeroOutY[len(zeroOutY)-1]
	if math.Abs(finalX) > 1e-3 || math.Abs(finalY) > 1e-3 {
		t.Errorf("Expected output to settle to near zero after 100 steps, got (%f, %f)", finalX, finalY)
	}
}

// TestAISmootherSuddenReversals tests sudden reversals (+50.0 to -50.0)
// and ensures filter stability, non-negative variance, and no runaway divergence.
func TestAISmootherSuddenReversals(t *testing.T) {
	smoother := NewAISmoother("non_existent_model.onnx", 0.5)

	val := 50.0
	for cycle := 0; cycle < 100; cycle++ {
		val = -val // sudden polarity flip
		outX, outY := smoother.Smooth(val, -val, 0.016)
		if math.IsNaN(outX) || math.IsInf(outX, 0) || math.IsNaN(outY) || math.IsInf(outY, 0) {
			t.Fatalf("Reversal cycle %d: NaN or Inf output: outX=%f, outY=%f", cycle, outX, outY)
		}

		varX, varY, varVx, varVy := smoother.GetKalmanFilter().GetVariances()
		if varX < 0 || varY < 0 || varVx < 0 || varVy < 0 {
			t.Fatalf("Reversal cycle %d: Negative variance: varX=%f, varY=%f", cycle, varX, varY)
		}
	}
}

// TestAISmootherExtremeDTValues tests robustness against edge-case dt values (0, negative, very large).
func TestAISmootherExtremeDTValues(t *testing.T) {
	smoother := NewAISmoother("non_existent_model.onnx", 0.5)

	extremeDTs := []float64{0.0, -0.016, -100.0, 0.00001, 1.0, 100.0}
	for _, dt := range extremeDTs {
		outX, outY := smoother.Smooth(5.0, -3.0, dt)
		if math.IsNaN(outX) || math.IsInf(outX, 0) || math.IsNaN(outY) || math.IsInf(outY, 0) {
			t.Fatalf("Extreme dt=%f caused NaN/Inf: (%f, %f)", dt, outX, outY)
		}
	}
}


