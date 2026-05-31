package adaptivesmoothing

import (
	"math"
	"math/rand"
	"sync"
)

type HumanizerConfig struct {
	EnableTremor          bool
	TremorAmplitude       float64
	EnableBSpline         bool
	BSplineSegments       int
	EnableVelocityProfile bool
	VelocityPeakRatio     float64
	NoiseAmplitude        float64
}

func DefaultHumanizerConfig() HumanizerConfig {
	return HumanizerConfig{
		EnableTremor:          true,
		TremorAmplitude:       3.5,
		EnableBSpline:         true,
		BSplineSegments:       15,
		EnableVelocityProfile: true,
		VelocityPeakRatio:     0.55,
		NoiseAmplitude:        2.0,
	}
}

type Humanizer struct {
	config        HumanizerConfig
	tremor        *TremorSimulator
	lastX, lastY  float64
	velocityProg  float64
	splineBuffer  [][2]float64
	mu            sync.Mutex
}

func NewHumanizer(config HumanizerConfig) *Humanizer {
	h := &Humanizer{
		config:        config,
		velocityProg:  0,
		splineBuffer:  make([][2]float64, 0),
	}
	if config.EnableTremor {
		h.tremor = NewTremorSimulator()
		h.tremor.SetAmplitude(config.TremorAmplitude)
	}
	return h
}

func (h *Humanizer) Process(dx, dy, currentX, currentY float64) (outDx, outDy float64) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if h.config.EnableBSpline && len(h.splineBuffer) > 0 {
		dx, dy = h.applyBSpline(dx, dy, currentX, currentY)
	}
	if h.config.EnableVelocityProfile {
		distance := math.Hypot(dx, dy)
		if distance > 5 {
			h.velocityProg += 0.02
			if h.velocityProg > 1 {
				h.velocityProg = 1
			}
			v := NewLogNormalVelocity(distance, h.config.VelocityPeakRatio)
			factor := v.VelocityAt(h.velocityProg)
			if factor > 0 {
				dx *= factor
				dy *= factor
			}
		} else {
			h.velocityProg = 0
		}
	}
	if h.config.EnableTremor && h.tremor != nil {
		tx, ty := h.tremor.Update()
		dx += tx
		dy += ty
	}
	noiseX := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
	noiseY := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
	dx += noiseX
	dy += noiseY
	return dx, dy
}

func (h *Humanizer) UpdatePosition(x, y float64) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if len(h.splineBuffer) == 0 || (h.lastX != x && h.lastY != y) {
		h.splineBuffer = append(h.splineBuffer, [2]float64{x, y})
		if len(h.splineBuffer) > 10 {
			h.splineBuffer = h.splineBuffer[1:]
		}
	}
	h.lastX, h.lastY = x, y
}

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
	if math.Abs(newDx) > math.Abs(dx)*1.5 {
		newDx = dx
	}
	if math.Abs(newDy) > math.Abs(dy)*1.5 {
		newDy = dy
	}
	return
}

func (h *Humanizer) Reset() {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.velocityProg = 0
	h.splineBuffer = nil
}