package particlefilter

import (
	"math"
	"math/rand"
	"sync"
	"time"
)

// Particle represents a single particle with state and weight.
type Particle struct {
	State   [4]float64 // x, y, vx, vy
	Weight  float64
	History [][]float64
}

// Filter is a basic particle filter (sequential Monte Carlo).
type Filter struct {
	particles      []Particle
	numParticles   int
	resampleThresh float64
	effectiveN     float64
	mu             sync.RWMutex
	stats          FilterStats
	callbacks      []func(event FilterEvent)
	rng            *rand.Rand
	rngMu          sync.Mutex
}

// FilterStats holds statistics about the filter.
type FilterStats struct {
	Iterations        int64
	AvgWeight         float64
	EffectiveParticles float64
	LastResample      time.Time
}

// FilterEvent is emitted when the filter resamples.
type FilterEvent struct {
	Type      string // "resampled", "converged", "diverged"
	Timestamp time.Time
}

// NewFilter creates a new particle filter.
func NewFilter(numParticles int) *Filter {
	if numParticles <= 0 {
		numParticles = 500
	}
	f := &Filter{
		particles:      make([]Particle, numParticles),
		numParticles:   numParticles,
		resampleThresh: 0.5,
		callbacks:      make([]func(FilterEvent), 0),
		rng:            rand.New(rand.NewSource(time.Now().UnixNano())),
	}
	f.initializeParticles()
	return f
}

// initializeParticles distributes particles randomly.
func (f *Filter) initializeParticles() {
	f.rngMu.Lock()
	defer f.rngMu.Unlock()
	for i := 0; i < f.numParticles; i++ {
		f.particles[i] = Particle{
			State: [4]float64{
				f.rng.Float64() * 100,
				f.rng.Float64() * 100,
				(f.rng.Float64() - 0.5) * 20,
				(f.rng.Float64() - 0.5) * 20,
			},
			Weight:  1.0 / float64(f.numParticles),
			History: make([][]float64, 0, 50),
		}
	}
}

// Predict moves particles forward in time using a motion model.
func (f *Filter) Predict(dt float64) {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.rngMu.Lock()
	defer f.rngMu.Unlock()

	processNoise := 5.0
	velocityNoise := 2.0

	for i := range f.particles {
		p := &f.particles[i]
		x, y, vx, vy := p.State[0], p.State[1], p.State[2], p.State[3]

		newX := x + vx*dt + f.rng.NormFloat64()*processNoise
		newY := y + vy*dt + f.rng.NormFloat64()*processNoise
		newVx := vx + f.rng.NormFloat64()*velocityNoise
		newVy := vy + f.rng.NormFloat64()*velocityNoise

		p.State = [4]float64{newX, newY, newVx, newVy}
		p.History = append(p.History, []float64{newX, newY})
		if len(p.History) > 50 {
			p.History = p.History[1:]
		}
	}
	f.stats.Iterations++
}

// Update incorporates a new measurement (x, y).
func (f *Filter) Update(measurementX, measurementY float64) {
	f.mu.Lock()
	defer f.mu.Unlock()

	measurementNoise := 10.0

	for i := range f.particles {
		dx := f.particles[i].State[0] - measurementX
		dy := f.particles[i].State[1] - measurementY
		distSq := dx*dx + dy*dy
		likelihood := math.Exp(-distSq / (2 * measurementNoise * measurementNoise))
		f.particles[i].Weight *= likelihood
	}

	f.normalizeWeights()
	f.calculateEffectiveN()

	if f.effectiveN < float64(f.numParticles)*f.resampleThresh {
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

// resample performs systematic resampling.
func (f *Filter) resample() {
	newParticles := make([]Particle, f.numParticles)
	cumWeights := make([]float64, f.numParticles)

	cum := 0.0
	for i, p := range f.particles {
		cum += p.Weight
		cumWeights[i] = cum
	}

	step := 1.0 / float64(f.numParticles)
	f.rngMu.Lock()
	u := f.rng.Float64() * step
	f.rngMu.Unlock()
	j := 0

	for i := 0; i < f.numParticles; i++ {
		u += step
		for u > cumWeights[j] && j < f.numParticles-1 {
			j++
		}
		newParticles[i] = Particle{
			State:   f.particles[j].State,
			Weight:  1.0 / float64(f.numParticles),
			History: append([][]float64{}, f.particles[j].History...),
		}
	}
	f.particles = newParticles
	f.stats.LastResample = time.Now()
}

// GetBestEstimate returns the weighted average state and confidence.
func (f *Filter) GetBestEstimate() (x, y, vx, vy float64, confidence float64) {
	f.mu.RLock()
	defer f.mu.RUnlock()

	if len(f.particles) == 0 {
		return 0, 0, 0, 0, 0
	}

	var sumX, sumY, sumVx, sumVy, totalWeight float64
	for _, p := range f.particles {
		sumX += p.State[0] * p.Weight
		sumY += p.State[1] * p.Weight
		sumVx += p.State[2] * p.Weight
		sumVy += p.State[3] * p.Weight
		totalWeight += p.Weight
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

// GetParticleCloud returns the particle states and weights (for visualisation).
func (f *Filter) GetParticleCloud() [][]float64 {
	f.mu.RLock()
	defer f.mu.RUnlock()
	cloud := make([][]float64, len(f.particles))
	for i, p := range f.particles {
		cloud[i] = []float64{p.State[0], p.State[1], p.Weight}
	}
	return cloud
}

// Reset re‑initialises the filter.
func (f *Filter) Reset() {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.initializeParticles()
	f.stats = FilterStats{}
}

// AddEventListener registers a callback for filter events.
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

// GetStats returns current filter statistics.
func (f *Filter) GetStats() FilterStats {
	f.mu.RLock()
	defer f.mu.RUnlock()
	return f.stats
}