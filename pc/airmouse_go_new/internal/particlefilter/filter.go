package particlefilter

import (
	"math"
	"math/rand"
	"sync"

	"gonum.org/v1/gonum/mat"
)

type Particle struct {
	State  *mat.VecDense
	Weight float64
}

type Filter struct {
	particles    []Particle
	numParticles int
	mu           sync.RWMutex
}

func NewFilter(numParticles int) *Filter {
	f := &Filter{
		particles:    make([]Particle, numParticles),
		numParticles: numParticles,
	}
	for i := 0; i < numParticles; i++ {
		state := mat.NewVecDense(2, []float64{
			rand.Float64() * 100,
			rand.Float64() * 100,
		})
		f.particles[i] = Particle{State: state, Weight: 1.0 / float64(numParticles)}
	}
	return f
}

func (f *Filter) Predict(dt float64) {
	for i := range f.particles {
		state := f.particles[i].State
		x := state.AtVec(0) + (rand.NormFloat64() * 10)
		y := state.AtVec(1) + (rand.NormFloat64() * 10)
		f.particles[i].State = mat.NewVecDense(2, []float64{x, y})
	}
}

func (f *Filter) Update(measurementX, measurementY float64) {
	for i := range f.particles {
		dx := f.particles[i].State.AtVec(0) - measurementX
		dy := f.particles[i].State.AtVec(1) - measurementY
		distSq := dx*dx + dy*dy
		likelihood := math.Exp(-distSq / 100.0)
		f.particles[i].Weight *= likelihood
	}
	f.normalize()
}

func (f *Filter) normalize() {
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

func (f *Filter) Resample() {
	newParticles := make([]Particle, f.numParticles)
	cumWeights := make([]float64, f.numParticles)
	cum := 0.0
	for i, p := range f.particles {
		cum += p.Weight
		cumWeights[i] = cum
	}
	for i := 0; i < f.numParticles; i++ {
		r := rand.Float64()
		idx := 0
		for idx < f.numParticles && cumWeights[idx] < r {
			idx++
		}
		if idx >= f.numParticles {
			idx = f.numParticles - 1
		}
		newState := mat.NewVecDense(2, nil)
		newState.CopyVec(f.particles[idx].State)
		newParticles[i] = Particle{State: newState, Weight: 1.0 / float64(f.numParticles)}
	}
	f.particles = newParticles
}

func (f *Filter) GetBestEstimate() (x, y float64) {
	var best Particle
	maxWeight := -1.0
	for _, p := range f.particles {
		if p.Weight > maxWeight {
			maxWeight = p.Weight
			best = p
		}
	}
	return best.State.AtVec(0), best.State.AtVec(1)
}
