package adaptivesmoothing

import (
	"math"
	"math/rand"
	"sync"
	"time"
)

// HumanizerConfig holds configuration for the humanizer.
type HumanizerConfig struct {
	EnableTremor          bool
	TremorAmplitude       float64
	EnableBSpline         bool
	BSplineSegments       int
	EnableVelocityProfile bool
	VelocityPeakRatio     float64
	NoiseAmplitude        float64
	SmoothingFactor       float64
	AccelerationFactor    float64
}

// DefaultHumanizerConfig returns a sensible default configuration.
func DefaultHumanizerConfig() HumanizerConfig {
	return HumanizerConfig{
		EnableTremor:          true,
		TremorAmplitude:       3.5,
		EnableBSpline:         true,
		BSplineSegments:       15,
		EnableVelocityProfile: true,
		VelocityPeakRatio:     0.55,
		NoiseAmplitude:        2.0,
		SmoothingFactor:       0.3,
		AccelerationFactor:    1.2,
	}
}

// Humanizer applies natural‑looking smoothing to cursor movement.
type Humanizer struct {
	config         HumanizerConfig
	tremor         *TremorSimulator
	lastX, lastY   float64
	lastDx, lastDy float64
	velocityProg   float64
	splineBuffer   [][2]float64
	mu             sync.Mutex
	initialized    bool
	stats          HumanizerStats
	rng            *rand.Rand
}

// HumanizerStats holds statistics about the humanizer.
type HumanizerStats struct {
	TotalProcessed int64
	AvgTremorX     float64
	AvgTremorY     float64
}

// NewHumanizer creates a new humanizer with the given config.
func NewHumanizer(config HumanizerConfig) *Humanizer {
	h := &Humanizer{
		config:       config,
		velocityProg: 0,
		splineBuffer: make([][2]float64, 0),
		initialized:  true,
		rng:          rand.New(rand.NewSource(time.Now().UnixNano())),
	}
	if config.EnableTremor {
		h.tremor = NewTremorSimulator()
		h.tremor.SetAmplitude(config.TremorAmplitude)
	}
	return h
}

// Process applies all enabled filters to the input delta.
func (h *Humanizer) Process(dx, dy, currentX, currentY float64) (outDx, outDy float64) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if !h.initialized {
		return dx, dy
	}

	outDx, outDy = dx, dy
	h.stats.TotalProcessed++

	// B‑spline smoothing
	if h.config.EnableBSpline && len(h.splineBuffer) > 4 {
		outDx, outDy = h.applyBSpline(outDx, outDy, currentX, currentY)
	}

	// Velocity profile
	if h.config.EnableVelocityProfile {
		distance := math.Hypot(outDx, outDy)
		if distance > 5 {
			h.velocityProg += 0.02
			if h.velocityProg > 1 {
				h.velocityProg = 1
			}
			v := NewLogNormalVelocity(distance, h.config.VelocityPeakRatio)
			factor := v.VelocityAt(h.velocityProg)
			if factor > 0 {
				outDx *= factor
				outDy *= factor
			}
		} else {
			h.velocityProg = 0
		}
	}

	// Tremor
	if h.config.EnableTremor && h.tremor != nil {
		tx, ty := h.tremor.Update()
		outDx += tx
		outDy += ty
		h.stats.AvgTremorX = (h.stats.AvgTremorX*float64(h.stats.TotalProcessed-1) + math.Abs(tx)) / float64(h.stats.TotalProcessed)
		h.stats.AvgTremorY = (h.stats.AvgTremorY*float64(h.stats.TotalProcessed-1) + math.Abs(ty)) / float64(h.stats.TotalProcessed)
	}

	// Exponential smoothing
	if h.config.SmoothingFactor > 0 {
		alpha := h.config.SmoothingFactor
		outDx = alpha*outDx + (1-alpha)*h.lastDx
		outDy = alpha*outDy + (1-alpha)*h.lastDy
	}

	// Random noise
	noiseX := (h.rng.Float64() - 0.5) * h.config.NoiseAmplitude
	noiseY := (h.rng.Float64() - 0.5) * h.config.NoiseAmplitude
	outDx += noiseX
	outDy += noiseY

	h.lastDx, h.lastDy = outDx, outDy
	return
}

// UpdatePosition updates the B‑spline buffer with the current cursor position.
func (h *Humanizer) UpdatePosition(x, y float64) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if len(h.splineBuffer) == 0 || math.Abs(h.lastX-x) > 1 || math.Abs(h.lastY-y) > 1 {
		h.splineBuffer = append(h.splineBuffer, [2]float64{x, y})
		if len(h.splineBuffer) > 20 {
			h.splineBuffer = h.splineBuffer[1:]
		}
	}
	h.lastX, h.lastY = x, y
}

// applyBSpline uses the spline to smooth the path.
func (h *Humanizer) applyBSpline(dx, dy, curX, curY float64) (newDx, newDy float64) {
	if len(h.splineBuffer) < 4 {
		return dx, dy
	}
	spline := NewBSpline3(h.splineBuffer, h.config.BSplineSegments)
	path := spline.SmoothPath()
	if len(path) < 2 {
		return dx, dy
	}
	target := path[len(path)-1]
	newDx = target[0] - curX
	newDy = target[1] - curY

	// Prevent large jumps
	maxChange := math.Hypot(dx, dy) * 1.5
	if math.Abs(newDx) > maxChange {
		newDx = dx
	}
	if math.Abs(newDy) > maxChange {
		newDy = dy
	}
	return
}

// Reset clears the humanizer state.
func (h *Humanizer) Reset() {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.velocityProg = 0
	h.splineBuffer = nil
	h.lastDx, h.lastDy = 0, 0
	h.stats = HumanizerStats{}
	if h.tremor != nil {
		h.tremor.Reset()
	}
}

// SetConfig updates the configuration.
func (h *Humanizer) SetConfig(config HumanizerConfig) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.config = config
	if config.EnableTremor && h.tremor == nil {
		h.tremor = NewTremorSimulator()
		h.tremor.SetAmplitude(config.TremorAmplitude)
	}
}

// GetStats returns current statistics.
func (h *Humanizer) GetStats() HumanizerStats {
	h.mu.Lock()
	defer h.mu.Unlock()
	return h.stats
}

// IsInitialized returns true if the humanizer is ready.
func (h *Humanizer) IsInitialized() bool {
	return h.initialized
}