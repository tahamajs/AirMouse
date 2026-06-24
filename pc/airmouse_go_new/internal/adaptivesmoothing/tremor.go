package adaptivesmoothing

import (
	"math"
	"math/rand"
	"sync"
	"time"
)

// TremorSimulator simulates physiological hand tremor.
type TremorSimulator struct {
	mu                 sync.Mutex
	amplitude          float64
	frequency          float64
	phaseX, phaseY     float64
	lastUpdate         time.Time
	dt                 float64
	enabled            bool
	frequencyVariation float64
	rng                *rand.Rand
}

// NewTremorSimulator creates a new tremor simulator.
func NewTremorSimulator() *TremorSimulator {
	src := rand.NewSource(time.Now().UnixNano())
	rng := rand.New(src)
	return &TremorSimulator{
		amplitude:          3.0 + rng.Float64()*2.0,
		frequency:          8.0 + (rng.Float64()-0.5)*4.0,
		phaseX:             rng.Float64() * 2 * math.Pi,
		phaseY:             rng.Float64() * 2 * math.Pi,
		lastUpdate:         time.Now(),
		dt:                 0.02,
		enabled:            true,
		frequencyVariation: 0.5,
		rng:                rng,
	}
}

// Update returns the current tremor displacement.
func (t *TremorSimulator) Update() (dx, dy float64) {
	t.mu.Lock()
	defer t.mu.Unlock()

	if !t.enabled {
		return 0, 0
	}

	now := time.Now()
	dt := now.Sub(t.lastUpdate).Seconds()
	if dt < 0.001 {
		dt = t.dt
	}
	t.lastUpdate = now

	freq := t.frequency + (t.rng.Float64()-0.5)*t.frequencyVariation
	if freq < 1 {
		freq = 1
	}
	if freq > 20 {
		freq = 20
	}

	t.phaseX += 2 * math.Pi * freq * dt
	t.phaseY += 2 * math.Pi * freq * dt

	if t.phaseX > 2*math.Pi {
		t.phaseX -= 2 * math.Pi
	}
	if t.phaseY > 2*math.Pi {
		t.phaseY -= 2 * math.Pi
	}

	tremorX := t.amplitude * math.Sin(t.phaseX) * (0.5 + t.rng.Float64()*0.5)
	tremorY := t.amplitude * math.Sin(t.phaseY) * (0.5 + t.rng.Float64()*0.5)

	dx = tremorX + (t.rng.Float64()-0.5)*3.0
	dy = tremorY + (t.rng.Float64()-0.5)*3.0
	return
}

// SetAmplitude sets the tremor amplitude.
func (t *TremorSimulator) SetAmplitude(amp float64) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if amp < 0 {
		amp = 0
	}
	if amp > 20 {
		amp = 20
	}
	t.amplitude = amp
}

// SetFrequency sets the tremor frequency (1–20 Hz).
func (t *TremorSimulator) SetFrequency(freq float64) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if freq < 1 {
		freq = 1
	}
	if freq > 20 {
		freq = 20
	}
	t.frequency = freq
}

// SetEnabled enables or disables tremor.
func (t *TremorSimulator) SetEnabled(enabled bool) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.enabled = enabled
}

// Reset resets the phase and timer.
func (t *TremorSimulator) Reset() {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.phaseX = t.rng.Float64() * 2 * math.Pi
	t.phaseY = t.rng.Float64() * 2 * math.Pi
	t.lastUpdate = time.Now()
}