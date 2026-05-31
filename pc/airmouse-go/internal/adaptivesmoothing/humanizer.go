package adaptivesmoothing

import (
    "math"
    "math/rand"
    "time"
)

// HumanizerConfig holds all parameters for human‑like movement injection.
type HumanizerConfig struct {
    EnableTremor        bool    `json:"enable_tremor"`
    TremorAmplitude     float64 `json:"tremor_amplitude"`      // pixels (3‑5)
    EnableBSpline       bool    `json:"enable_bspline"`
    BSplineSegments     int     `json:"bspline_segments"`      // 10‑30
    EnableVelocityProfile bool  `json:"enable_velocity_profile"`
    VelocityPeakRatio   float64 `json:"velocity_peak_ratio"`   // 0.4‑0.6
    NoiseAmplitude      float64 `json:"noise_amplitude"`       // 1‑3 pixels
}

// DefaultHumanizerConfig returns biologically realistic defaults.
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

// Humanizer processes movement deltas to make them feel human‑like.
type Humanizer struct {
    config      HumanizerConfig
    tremor      *TremorSimulator
    lastX, lastY float64
    velocityProg float64
    splineBuffer [][2]float64
    mu          sync.Mutex
}

// NewHumanizer creates a new humanizer with the given configuration.
func NewHumanizer(config HumanizerConfig) *Humanizer {
    h := &Humanizer{
        config:      config,
        velocityProg: 0,
        splineBuffer: make([][2]float64, 0),
    }
    if config.EnableTremor {
        h.tremor = NewTremorSimulator()
        h.tremor.SetAmplitude(config.TremorAmplitude)
    }
    return h
}

// Process takes a raw movement delta (dx, dy) and returns a humanised delta.
func (h *Humanizer) Process(dx, dy float64, currentX, currentY float64) (outDx, outDy float64) {
    h.mu.Lock()
    defer h.mu.Unlock()
    
    // 1. Apply B‑spline smoothing (if enabled)
    if h.config.EnableBSpline && len(h.splineBuffer) > 0 {
        dx, dy = h.applyBSpline(dx, dy, currentX, currentY)
    }
    
    // 2. Apply log‑normal velocity profile (if enabled)
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
    
    // 3. Add physiological tremor (if enabled)
    if h.config.EnableTremor && h.tremor != nil {
        tremorX, tremorY := h.tremor.Update()
        dx += tremorX
        dy += tremorY
    }
    
    // 4. Add micro‑correction noise (always present for natural feel)
    noiseX := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
    noiseY := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
    dx += noiseX
    dy += noiseY
    
    return dx, dy
}

// UpdatePosition updates the humanizer’s internal position for spline calculation.
func (h *Humanizer) UpdatePosition(x, y float64) {
    h.mu.Lock()
    defer h.mu.Unlock()
    if len(h.splineBuffer) == 0 || (h.lastX != x && h.lastY != y) {
        h.splineBuffer = append(h.splineBuffer, [2]float64{x, y})
        if len(h.splineBuffer) > 10 {
            h.splineBuffer = h.splineBuffer[1:]
        }
    }
    h.lastX = x
    h.lastY = y
}

func (h *Humanizer) applyBSpline(dx, dy float64, currentX, currentY float64) (newDx, newDy float64) {
    if len(h.splineBuffer) < 4 {
        return dx, dy
    }
    
    // Create a B‑spline through the last few points
    spline := NewBSpline3(h.splineBuffer, h.config.BSplineSegments)
    path := spline.SmoothPath()
    
    if len(path) < 2 {
        return dx, dy
    }
    
    // Follow the smooth path
    targetIdx := len(path) - 1
    target := path[targetIdx]
    newDx = target[0] - currentX
    newDy = target[1] - currentY
    
    // Clamp to reasonable movement
    if math.Abs(newDx) > math.Abs(dx)*1.5 {
        newDx = dx
    }
    if math.Abs(newDy) > math.Abs(dy)*1.5 {
        newDy = dy
    }
    return
}

// Reset clears internal state for a new movement (e.g., after click).
func (h *Humanizer) Reset() {
    h.mu.Lock()
    defer h.mu.Unlock()
    h.velocityProg = 0
    h.splineBuffer = nil
}