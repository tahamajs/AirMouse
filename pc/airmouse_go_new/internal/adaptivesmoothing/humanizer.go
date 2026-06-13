package adaptivesmoothing

import (
    "math"
    "math/rand"
    "sync"
    "time"
)

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
}

type HumanizerStats struct {
    TotalProcessed int64
    AvgTremorX     float64
    AvgTremorY     float64
}

func NewHumanizer(config HumanizerConfig) *Humanizer {
    h := &Humanizer{
        config:       config,
        velocityProg: 0,
        splineBuffer: make([][2]float64, 0),
        initialized:  true,
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
    
    if !h.initialized {
        return dx, dy
    }
    
    outDx, outDy = dx, dy
    h.stats.TotalProcessed++
    
    // Apply B-spline smoothing
    if h.config.EnableBSpline && len(h.splineBuffer) > 4 {
        outDx, outDy = h.applyBSpline(outDx, outDy, currentX, currentY)
    }
    
    // Apply velocity profile
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
    
    // Apply tremor
    if h.config.EnableTremor && h.tremor != nil {
        tx, ty := h.tremor.Update()
        outDx += tx
        outDy += ty
        h.stats.AvgTremorX = (h.stats.AvgTremorX*float64(h.stats.TotalProcessed-1) + math.Abs(tx)) / float64(h.stats.TotalProcessed)
        h.stats.AvgTremorY = (h.stats.AvgTremorY*float64(h.stats.TotalProcessed-1) + math.Abs(ty)) / float64(h.stats.TotalProcessed)
    }
    
    // Apply smoothing
    if h.config.SmoothingFactor > 0 {
        outDx = h.config.SmoothingFactor*outDx + (1-h.config.SmoothingFactor)*h.lastDx
        outDy = h.config.SmoothingFactor*outDy + (1-h.config.SmoothingFactor)*h.lastDy
    }
    
    // Apply random noise
    noiseX := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
    noiseY := (rand.Float64() - 0.5) * h.config.NoiseAmplitude
    outDx += noiseX
    outDy += noiseY
    
    h.lastDx, h.lastDy = outDx, outDy
    return
}

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
    
    // Limit changes
    maxChange := math.Hypot(dx, dy) * 1.5
    if math.Abs(newDx) > maxChange {
        newDx = dx
    }
    if math.Abs(newDy) > maxChange {
        newDy = dy
    }
    
    return
}

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

func (h *Humanizer) SetConfig(config HumanizerConfig) {
    h.mu.Lock()
    defer h.mu.Unlock()
    h.config = config
    if config.EnableTremor && h.tremor == nil {
        h.tremor = NewTremorSimulator()
        h.tremor.SetAmplitude(config.TremorAmplitude)
    }
}

func (h *Humanizer) GetStats() HumanizerStats {
    h.mu.Lock()
    defer h.mu.Unlock()
    return h.stats
}

func (h *Humanizer) IsInitialized() bool {
    return h.initialized
}