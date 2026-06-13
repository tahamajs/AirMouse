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
    phaseZ     float64
    lastUpdate time.Time
    dt         float64
    enabled    bool
    frequencyVariation float64
}

func NewTremorSimulator() *TremorSimulator {
    return &TremorSimulator{
        amplitude:  3.0 + rand.Float64()*2.0,
        frequency:  8.0 + (rand.Float64()-0.5)*4.0,
        phaseX:     rand.Float64() * 2 * math.Pi,
        phaseY:     rand.Float64() * 2 * math.Pi,
        phaseZ:     rand.Float64() * 2 * math.Pi,
        lastUpdate: time.Now(),
        dt:         0.02,
        enabled:    true,
        frequencyVariation: 0.5,
    }
}

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
    
    // Add frequency variation
    freq := t.frequency + (rand.Float64()-0.5)*t.frequencyVariation
    if freq < 1 {
        freq = 1
    }
    if freq > 20 {
        freq = 20
    }
    
    t.phaseX += 2 * math.Pi * freq * dt
    t.phaseY += 2 * math.Pi * freq * dt
    t.phaseZ += 2 * math.Pi * (freq * 0.7) * dt
    
    // Wrap phases
    if t.phaseX > 2*math.Pi {
        t.phaseX -= 2 * math.Pi
    }
    if t.phaseY > 2*math.Pi {
        t.phaseY -= 2 * math.Pi
    }
    
    // Calculate tremor with modulation
    tremorX := t.amplitude * math.Sin(t.phaseX) * (0.5 + rand.Float64()*0.5)
    tremorY := t.amplitude * math.Sin(t.phaseY) * (0.5 + rand.Float64()*0.5)
    
    // Add random noise
    dx = tremorX + (rand.Float64()-0.5)*3.0
    dy = tremorY + (rand.Float64()-0.5)*3.0
    
    return
}

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

func (t *TremorSimulator) SetEnabled(enabled bool) {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.enabled = enabled
}

func (t *TremorSimulator) Reset() {
    t.mu.Lock()
    defer t.mu.Unlock()
    t.phaseX = rand.Float64() * 2 * math.Pi
    t.phaseY = rand.Float64() * 2 * math.Pi
    t.lastUpdate = time.Now()
}