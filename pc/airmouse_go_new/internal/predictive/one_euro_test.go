package predictive

import (
	"math"
	"math/rand"
	"testing"
	"time"
)

func TestOneEuroFilterLowSpeedJitter(t *testing.T) {
	// At low speeds (e.g. hand tremor), the One Euro filter should heavily smooth the signal.
	f := NewOneEuroFilter(1.0, 0.007, 1.0)
	now := time.Now()

	constantSignal := 10.0
	var outputs []float64

	// Inject noise +/- 1.0
	r := rand.New(rand.NewSource(42))
	for i := 0; i < 50; i++ {
		noisy := constantSignal + (r.Float64()*2.0 - 1.0)
		tVal := now.Add(time.Duration(i*20) * time.Millisecond)
		out := f.Filter(noisy, tVal)
		outputs = append(outputs, out)
	}

	// Calculate variance of input noise vs variance of filtered output
	var sumDiff float64
	for i := 10; i < len(outputs); i++ {
		sumDiff += math.Abs(outputs[i] - constantSignal)
	}
	avgError := sumDiff / float64(len(outputs)-10)

	if avgError > 0.4 {
		t.Errorf("OneEuroFilter failed to smooth low-speed jitter: avg error = %.3f", avgError)
	}
	t.Logf("OneEuroFilter low speed avg error: %.4f", avgError)
}

func TestOneEuroFilterHighSpeedResponsiveness(t *testing.T) {
	// At high speeds, the cutoff frequency increases dramatically, causing the filter to follow closely with minimal lag.
	f := NewOneEuroFilter(1.0, 0.1, 1.0)
	now := time.Now()

	// High speed step function / ramp
	var lastOut float64
	for i := 0; i < 30; i++ {
		val := float64(i * 50) // Fast movement: 50 px per 20ms = 2500 px/sec
		tVal := now.Add(time.Duration(i*20) * time.Millisecond)
		lastOut = f.Filter(val, tVal)
	}

	expected := float64(29 * 50)
	lag := math.Abs(expected - lastOut)
	// Because of high beta (speed coefficient), lag should be minimal
	t.Logf("High speed target: %.1f, filtered: %.1f, lag: %.1f", expected, lastOut, lag)
	if lag > 150 {
		t.Errorf("OneEuroFilter lag too high during high speed movement: %.1f", lag)
	}
}

func TestOneEuroFilter2D(t *testing.T) {
	f2d := NewOneEuroFilter2D(1.0, 0.01, 1.0)
	now := time.Now()

	for i := 0; i < 20; i++ {
		tVal := now.Add(time.Duration(i*10) * time.Millisecond)
		fx, fy := f2d.Filter(float64(i*2), float64(i*3), tVal)
		if i == 19 {
			t.Logf("2D step 19: filtered (%.2f, %.2f)", fx, fy)
		}
	}

	f2d.Reset()
	fx, fy := f2d.Filter(100.0, 200.0, now)
	if fx != 100.0 || fy != 200.0 {
		t.Errorf("Expected initial value after reset to be (100, 200), got (%.2f, %.2f)", fx, fy)
	}
}

func TestPredictiveMotionFilter(t *testing.T) {
	pmf := NewPredictiveMotionFilter(1.0, 0.01, 1.0, 0.5)
	now := time.Now()

	for i := 0; i < 15; i++ {
		tVal := now.Add(time.Duration(i*20) * time.Millisecond)
		smX, smY, predX, predY := pmf.FilterAndPredict(10.0, 5.0, 0.02, tVal)
		t.Logf("Step %d: smoothed (%.2f, %.2f), predicted (%.2f, %.2f)", i, smX, smY, predX, predY)
	}

	pmf.Reset()
}
