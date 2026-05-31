package particlefilter

import (
    "math"
    "math/rand"
    "sync"
    "time"

    "github.com/milosgajdos/go-estimate/particle/bf"
    "gonum.org/v1/gonum/mat"
)

// GestureState defines the hidden state of the particle filter.
type GestureState struct {
    X, Y, Vx, Vy   float64 // position and velocity (for tracking)
    Gesture        string  // discrete gesture type
    Confidence     float64 // probability of this gesture
}

// Filter is a particle filter for gesture recognition.
type Filter struct {
    pf           *bf.ParticleFilter
    particles    []*bf.Particle
    mu           sync.RWMutex
    numParticles int
    lastTime     time.Time
}

// NewFilter creates a particle filter with N particles.
func NewFilter(numParticles int) *Filter {
    f := &Filter{
        numParticles: numParticles,
        lastTime:     time.Now(),
    }
    // Initialize particles
    particles := make([]*bf.Particle, numParticles)
    for i := 0; i < numParticles; i++ {
        // Initial state: random position, zero velocity, uniform gesture probabilities
        state := mat.NewVecDense(4, []float64{
            rand.Float64() * 100,          // x
            rand.Float64() * 100,          // y
            (rand.Float64() - 0.5) * 10,   // vx
            (rand.Float64() - 0.5) * 10,   // vy
        })
        weight := 1.0 / float64(numParticles)
        particles[i] = bf.NewParticle(state, weight)
    }
    // Create particle filter with Bootstrap filter (SIR)
    f.pf = bf.NewPF(particles, bf.WithProcessNoise(processNoise), bf.WithResamplingThreshold(0.5))
    return f
}

// predict updates the filter using a simple motion model (constant velocity with noise).
func (f *Filter) predict(dt float64) {
    for _, p := range f.pf.Particles() {
        state := p.State()
        x, y, vx, vy := state.AtVec(0), state.AtVec(1), state.AtVec(2), state.AtVec(3)
        // Predict new state
        xNew := x + vx*dt
        yNew := y + vy*dt
        vxNew := vx + (rand.NormFloat64() * 5)   // acceleration noise
        vyNew := vy + (rand.NormFloat64() * 5)
        newState := mat.NewVecDense(4, []float64{xNew, yNew, vxNew, vyNew})
        p.SetState(newState)
    }
    f.pf.Predict()
}

// Update uses the new measurement (dx, dy) to update particle weights.
func (f *Filter) Update(dx, dy float64) {
    for _, p := range f.pf.Particles() {
        state := p.State()
        x, y, vx, vy := state.AtVec(0), state.AtVec(1), state.AtVec(2), state.AtVec(3)
        // Measurement prediction: the particle would move (vx*dt, vy*dt)
        dt := 0.02 // typical time step
        predDx := vx * dt
        predDy := vy * dt
        // Likelihood: Gaussian error between actual and predicted movement
        errX := dx - predDx
        errY := dy - predDy
        variance := 10.0 // measurement noise
        likelihood := math.Exp(-(errX*errX+errY*errY)/(2*variance)) / (2 * math.Pi * variance)
        p.SetWeight(p.Weight() * likelihood)
    }
    f.pf.Update()
    f.resampleIfNeeded()
}

// resampleIfNeeded performs resampling when the effective particle count is too low.
func (f *Filter) resampleIfNeeded() {
    eff := 1.0 / f.pf.EffectiveParticleCount()
    if eff < 0.5 { // threshold
        f.pf.Resample(bf.WithResampleCriterion(bf.Systematic))
    }
}

// GetBestGesture returns the most likely gesture and its confidence.
func (f *Filter) GetBestGesture() (gesture string, confidence float64) {
    // This is a placeholder; you would implement a gesture classifier here.
    // For now, return a dummy gesture.
    return "unknown", 0.0
}