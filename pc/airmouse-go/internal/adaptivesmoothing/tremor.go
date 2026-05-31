package adaptivesmoothing

import (
    "math"
    "math/rand"
    "sync"
    "time"
)

// TremorSimulator generates band‑limited physiological tremor.
type TremorSimulator struct {
    mu          sync.Mutex
    amplitude   float64   // pixels (typical 2‑5)
    frequency   float64   // Hz (8‑12 Hz for hands)
    phaseX      float64
    phaseY      float64
    lastUpdate  time.Time
    dt          float64
}

// NewTremorSimulator creates a tremor simulator with realistic parameters.
func NewTremorSimulator() *TremorSimulator {
    return &TremorSimulator{
        amplitude:  3.0 + rand.Float64()*2.0, // 3‑5 pixels
        frequency:  10.0 + (rand.Float64()-0.5)*2.0, // 9‑11 Hz
        phaseX:     rand.Float64() * 2 * math.Pi,
        phaseY:     rand.Float64() * 2 * math.Pi,
        lastUpdate: time.Now(),
        dt:         0.02, // 50 Hz update rate
    }
}

// Update generates tremor noise for the current time step.
func (t *TremorSimulator) Update() (dx, dy float64) {
    t.mu.Lock()
    defer t.mu.Unlock()
    
    now := time.Now()
    dt := now.Sub(t.lastUpdate).Seconds()
    if dt < 0.001 {
        dt = t.dt
    }
    t.lastUpdate = now
    
    // Simple sinusoidal oscillation (band‑limited noise)
    t.phaseX += 2 * math.Pi * t.frequency * dt
    t.phaseY += 2 * math.Pi * t.frequency * dt
    
    dx = t.amplitude * math.Sin(t.phaseX) * (rand.Float64()*0.5 + 0.5)
    dy = t.amplitude * math.Sin(t.phaseY) * (rand.Float64()*0.5 + 0.5)
    
    // Add small random jitter (micro‑corrections)
    dx += (rand.Float64() - 0.5) * 2.0
    dy += (rand.Float64() - 0.5) * 2.0
    
    return
}

// SetAmplitude adjusts tremor intensity (caller can make it user‑configurable).
func (t *TremorSimulator) SetAmplitude(amp float64) {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.amplitude = amp
}