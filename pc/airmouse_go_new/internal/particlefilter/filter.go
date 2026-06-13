package particlefilter

import (
    "math"
    "math/rand"
    "sort"
    "sync"
    "time"

    "gonum.org/v1/gonum/mat"
)

type Particle struct {
    State   *mat.VecDense
    Weight  float64
    History [][]float64
}

type Filter struct {
    particles     []Particle
    numParticles  int
    resampleThreshold float64
    effectiveN    float64
    mu            sync.RWMutex
    stats         FilterStats
    callbacks     []func(event FilterEvent)
}

type FilterStats struct {
    Iterations       int64
    AvgWeight        float64
    EffectiveParticles float64
    LastResample     time.Time
}

type FilterEvent struct {
    Type      string // "resampled", "converged", "diverged"
    Timestamp time.Time
}

func NewFilter(numParticles int) *Filter {
    f := &Filter{
        particles:         make([]Particle, numParticles),
        numParticles:      numParticles,
        resampleThreshold: 0.5,
        callbacks:         make([]func(FilterEvent), 0),
    }
    f.initializeParticles()
    return f
}

func (f *Filter) initializeParticles() {
    for i := 0; i < f.numParticles; i++ {
        state := mat.NewVecDense(4, []float64{
            rand.Float64() * 100,  // x position
            rand.Float64() * 100,  // y position
            (rand.Float64() - 0.5) * 20, // vx velocity
            (rand.Float64() - 0.5) * 20, // vy velocity
        })
        f.particles[i] = Particle{
            State:   state,
            Weight:  1.0 / float64(f.numParticles),
            History: make([][]float64, 0),
        }
    }
}

func (f *Filter) Predict(dt float64) {
    f.mu.Lock()
    defer f.mu.Unlock()
    
    processNoise := 5.0
    velocityNoise := 2.0
    
    for i := range f.particles {
        state := f.particles[i].State
        x := state.AtVec(0)
        y := state.AtVec(1)
        vx := state.AtVec(2)
        vy := state.AtVec(3)
        
        // Motion model: x = x + vx*dt + noise
        newX := x + vx*dt + (rand.NormFloat64() * processNoise)
        newY := y + vy*dt + (rand.NormFloat64() * processNoise)
        newVx := vx + (rand.NormFloat64() * velocityNoise)
        newVy := vy + (rand.NormFloat64() * velocityNoise)
        
        f.particles[i].State = mat.NewVecDense(4, []float64{newX, newY, newVx, newVy})
        
        // Update history
        f.particles[i].History = append(f.particles[i].History, []float64{newX, newY})
        if len(f.particles[i].History) > 50 {
            f.particles[i].History = f.particles[i].History[1:]
        }
    }
    
    f.stats.Iterations++
}

func (f *Filter) Update(measurementX, measurementY float64) {
    f.mu.Lock()
    defer f.mu.Unlock()
    
    measurementNoise := 10.0
    
    for i := range f.particles {
        dx := f.particles[i].State.AtVec(0) - measurementX
        dy := f.particles[i].State.AtVec(1) - measurementY
        distSq := dx*dx + dy*dy
        
        // Likelihood based on distance
        likelihood := math.Exp(-distSq / (2 * measurementNoise * measurementNoise))
        f.particles[i].Weight *= likelihood
    }
    
    f.normalizeWeights()
    f.calculateEffectiveN()
    
    // Resample if necessary
    if f.effectiveN < float64(f.numParticles)*f.resampleThreshold {
        f.resample()
        f.triggerEvent(FilterEvent{
            Type:      "resampled",
            Timestamp: time.Now(),
        })
    }
}

func (f *Filter) normalizeWeights() {
    total := 0.0
    for _, p := range f.particles {
        total += p.Weight
    }
    
    if total > 0 {
        for i := range f.particles {
            f.particles[i].Weight /= total
        }
    }
}

func (f *Filter) calculateEffectiveN() {
    sumSq := 0.0
    for _, p := range f.particles {
        sumSq += p.Weight * p.Weight
    }
    f.effectiveN = 1.0 / sumSq
    f.stats.EffectiveParticles = f.effectiveN
    
    var sumWeight float64
    for _, p := range f.particles {
        sumWeight += p.Weight
    }
    f.stats.AvgWeight = sumWeight / float64(f.numParticles)
}

func (f *Filter) resample() {
    newParticles := make([]Particle, f.numParticles)
    
    // Systematic resampling
    cumWeights := make([]float64, f.numParticles)
    cum := 0.0
    for i, p := range f.particles {
        cum += p.Weight
        cumWeights[i] = cum
    }
    
    step := 1.0 / float64(f.numParticles)
    u := rand.Float64() * step
    
    j := 0
    for i := 0; i < f.numParticles; i++ {
        u += step
        for u > cumWeights[j] && j < f.numParticles-1 {
            j++
        }
        
        // Copy particle
        newState := mat.NewVecDense(4, nil)
        newState.CopyVec(f.particles[j].State)
        newParticles[i] = Particle{
            State:   newState,
            Weight:  1.0 / float64(f.numParticles),
            History: make([][]float64, len(f.particles[j].History)),
        }
        copy(newParticles[i].History, f.particles[j].History)
    }
    
    f.particles = newParticles
    f.stats.LastResample = time.Now()
}

func (f *Filter) GetBestEstimate() (x, y, vx, vy float64, confidence float64) {
    f.mu.RLock()
    defer f.mu.RUnlock()
    
    if len(f.particles) == 0 {
        return 0, 0, 0, 0, 0
    }
    
    // Weighted average
    var sumX, sumY, sumVx, sumVy, totalWeight float64
    maxWeight := -1.0
    var bestX, bestY, bestVx, bestVy float64
    
    for _, p := range f.particles {
        sumX += p.State.AtVec(0) * p.Weight
        sumY += p.State.AtVec(1) * p.Weight
        sumVx += p.State.AtVec(2) * p.Weight
        sumVy += p.State.AtVec(3) * p.Weight
        totalWeight += p.Weight
        
        if p.Weight > maxWeight {
            maxWeight = p.Weight
            bestX = p.State.AtVec(0)
            bestY = p.State.AtVec(1)
            bestVx = p.State.AtVec(2)
            bestVy = p.State.AtVec(3)
        }
    }
    
    if totalWeight > 0 {
        sumX /= totalWeight
        sumY /= totalWeight
        sumVx /= totalWeight
        sumVy /= totalWeight
    }
    
    confidence = f.effectiveN / float64(f.numParticles)
    if confidence > 1 {
        confidence = 1
    }
    
    return sumX, sumY, sumVx, sumVy, confidence
}

func (f *Filter) GetParticleCloud() [][]float64 {
    f.mu.RLock()
    defer f.mu.RUnlock()
    
    cloud := make([][]float64, len(f.particles))
    for i, p := range f.particles {
        cloud[i] = []float64{p.State.AtVec(0), p.State.AtVec(1), p.Weight}
    }
    return cloud
}

func (f *Filter) Reset() {
    f.mu.Lock()
    defer f.mu.Unlock()
    
    f.initializeParticles()
    f.stats = FilterStats{}
}

func (f *Filter) AddEventListener(callback func(event FilterEvent)) {
    f.mu.Lock()
    defer f.mu.Unlock()
    f.callbacks = append(f.callbacks, callback)
}

func (f *Filter) triggerEvent(event FilterEvent) {
    f.mu.RLock()
    callbacks := make([]func(FilterEvent), len(f.callbacks))
    copy(callbacks, f.callbacks)
    f.mu.RUnlock()
    
    for _, cb := range callbacks {
        go cb(event)
    }
}

func (f *Filter) GetStats() FilterStats {
    f.mu.RLock()
    defer f.mu.RUnlock()
    return f.stats
}