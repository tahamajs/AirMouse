package adaptivesmoothing

import (
	"math"
	"math/rand"
	"sync"
	"time"
)

type TremorSimulator struct {
	mu         sync.Mutex
	amplitude  float64
	frequency  float64
	phaseX     float64
	phaseY     float64
	lastUpdate time.Time
	dt         float64
}

func NewTremorSimulator() *TremorSimulator {
	return &TremorSimulator{
		amplitude:  3.0 + rand.Float64()*2.0,
		frequency:  10.0 + (rand.Float64()-0.5)*2.0,
		phaseX:     rand.Float64() * 2 * math.Pi,
		phaseY:     rand.Float64() * 2 * math.Pi,
		lastUpdate: time.Now(),
		dt:         0.02,
	}
}

func (t *TremorSimulator) Update() (dx, dy float64) {
	t.mu.Lock()
	defer t.mu.Unlock()
	now := time.Now()
	dt := now.Sub(t.lastUpdate).Seconds()
	if dt < 0.001 {
		dt = t.dt
	}
	t.lastUpdate = now
	t.phaseX += 2 * math.Pi * t.frequency * dt
	t.phaseY += 2 * math.Pi * t.frequency * dt
	dx = t.amplitude * math.Sin(t.phaseX) * (rand.Float64()*0.5 + 0.5)
	dy = t.amplitude * math.Sin(t.phaseY) * (rand.Float64()*0.5 + 0.5)
	dx += (rand.Float64() - 0.5) * 2.0
	dy += (rand.Float64() - 0.5) * 2.0
	return
}

func (t *TremorSimulator) SetAmplitude(amp float64) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.amplitude = amp
}
