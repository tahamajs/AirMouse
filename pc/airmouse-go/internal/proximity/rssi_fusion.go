package proximity

import (
    "math"
    "math/rand"
    "sync"
    "time"

    "github.com/milosgajdos/go-estimate/particle/bf"
    "gonum.org/v1/gonum/mat"
)

// RSSIFusionConfig holds parameters for the Bayesian proximity estimator.
type RSSIFusionConfig struct {
    TxPower        float64 // RSSI at 1m (e.g., -59 for smartphones)
    EnvFactor      float64 // environmental factor (2.0 free space, 2.5‑4 indoor)
    UseKalman      bool
    UseParticle    bool
    NumParticles   int
    ProcessNoise   float64
    MeasurementNoise float64
    MinDistance    float64
    MaxDistance    float64
}

// DefaultRSSIFusionConfig returns sensible defaults for BLE proximity.
func DefaultRSSIFusionConfig() RSSIFusionConfig {
    return RSSIFusionConfig{
        TxPower:          -59.0,   // typical for smartphones
        EnvFactor:        2.5,     // indoor office environment
        UseKalman:        true,
        UseParticle:      true,
        NumParticles:     200,
        ProcessNoise:     0.5,
        MeasurementNoise: 5.0,
        MinDistance:      0.5,     // 0.5m minimum
        MaxDistance:      10.0,    // 10m maximum
    }
}

// RSSIFusion fuses multiple Bayesian filters for accurate distance estimation.
type RSSIFusion struct {
    config         RSSIFusionConfig
    kalman         *KalmanFilter1D
    particleFilter *bf.ParticleFilter
    mu             sync.RWMutex
    lastTime       time.Time
    lastRawRSSI    float64
    lastFiltered   float64
}

// KalmanFilter1D is a simple 1D Kalman filter for RSSI smoothing.
type KalmanFilter1D struct {
    x      float64 // state (filtered RSSI)
    P      float64 // error covariance
    Q      float64 // process noise
    R      float64 // measurement noise
    K      float64 // Kalman gain
}

// NewKalmanFilter1D initialises a 1D Kalman filter.
func NewKalmanFilter1D(q, r float64) *KalmanFilter1D {
    return &KalmanFilter1D{
        x: 0,
        P: 1,
        Q: q,
        R: r,
    }
}

// Update processes a new RSSI measurement and returns the filtered value.
func (kf *KalmanFilter1D) Update(z float64) float64 {
    // Prediction
    kf.P = kf.P + kf.Q

    // Update
    kf.K = kf.P / (kf.P + kf.R)
    kf.x = kf.x + kf.K*(z-kf.x)
    kf.P = (1 - kf.K) * kf.P
    return kf.x
}

// NewRSSIFusion creates a new proximity estimator.
func NewRSSIFusion(cfg RSSIFusionConfig) *RSSIFusion {
    fusion := &RSSIFusion{
        config:   cfg,
        lastTime: time.Now(),
    }
    if cfg.UseKalman {
        fusion.kalman = NewKalmanFilter1D(cfg.ProcessNoise, cfg.MeasurementNoise)
    }
    if cfg.UseParticle && cfg.NumParticles > 0 {
        fusion.initParticleFilter()
    }
    return fusion
}

// initParticleFilter creates the particle filter and its particles.
func (f *RSSIFusion) initParticleFilter() {
    particles := make([]*bf.Particle, f.config.NumParticles)
    for i := 0; i < f.config.NumParticles; i++ {
        // State is distance (single float64)
        state := mat.NewVecDense(1, []float64{
            f.config.MinDistance + rand.Float64()*(f.config.MaxDistance-f.config.MinDistance),
        })
        weight := 1.0 / float64(f.config.NumParticles)
        particles[i] = bf.NewParticle(state, weight)
    }
    f.particleFilter = bf.NewPF(particles, bf.WithProcessNoise(f.config.ProcessNoise))
}

// rssiToDistance converts raw RSSI to a distance estimate.
func (f *RSSIFusion) rssiToDistance(rssi float64) float64 {
    // Log‑distance path loss model: distance = 10^((txPower - rssi) / (10 * n))
    ratio := (f.config.TxPower - rssi) / (10.0 * f.config.EnvFactor)
    dist := math.Pow(10.0, ratio)
    // Clamp to realistic bounds
    if dist < f.config.MinDistance {
        dist = f.config.MinDistance
    }
    if dist > f.config.MaxDistance {
        dist = f.config.MaxDistance
    }
    return dist
}

// ProcessRSSI takes a raw RSSI value (dBm) and returns the filtered distance (m).
func (f *RSSIFusion) ProcessRSSI(rssi float64) (distance float64) {
    f.mu.Lock()
    defer f.mu.Unlock()

    now := time.Now()
    dt := now.Sub(f.lastTime).Seconds()
    if dt < 0.001 {
        dt = 0.02
    }
    f.lastTime = now

    // 1. Convert RSSI to distance using path loss model
    rawDist := f.rssiToDistance(rssi)

    // 2. Apply Kalman filter on RSSI (if enabled)
    var filteredRSSI float64
    if f.config.UseKalman && f.kalman != nil {
        filteredRSSI = f.kalman.Update(rssi)
    } else {
        filteredRSSI = rssi
    }
    filteredDist := f.rssiToDistance(filteredRSSI)

    // 3. Particle filter update (if enabled)
    if f.config.UseParticle && f.particleFilter != nil {
        // Predict
        for _, p := range f.particleFilter.Particles() {
            state := p.State()
            dist := state.AtVec(0)
            // Simple Brownian motion prediction
            newDist := dist + (rand.NormFloat64() * math.Sqrt(dt))
            if newDist < f.config.MinDistance {
                newDist = f.config.MinDistance
            }
            if newDist > f.config.MaxDistance {
                newDist = f.config.MaxDistance
            }
            p.SetState(mat.NewVecDense(1, []float64{newDist}))
        }
        f.particleFilter.Predict()

        // Update weights using measurement likelihood
        for _, p := range f.particleFilter.Particles() {
            dist := p.State().AtVec(0)
            // Likelihood: Gaussian error between predicted distance and measured rawDist
            err := dist - rawDist
            variance := f.config.MeasurementNoise
            likelihood := math.Exp(-(err*err)/(2*variance)) / math.Sqrt(2*math.Pi*variance)
            p.SetWeight(p.Weight() * likelihood)
        }
        f.particleFilter.Update()
        f.resampleParticlesIfNeeded()

        // Extract maximum likelihood distance from particle filter
        bestDist := f.getParticleMaxLikelihood()
        if bestDist > 0 {
            // Blend Kalman and particle results
            distance = (filteredDist + bestDist) / 2.0
        } else {
            distance = filteredDist
        }
    } else {
        distance = filteredDist
    }

    f.lastRawRSSI = rssi
    f.lastFiltered = distance
    return distance
}

// resampleParticlesIfNeeded performs systematic resampling when particle weights decay.
func (f *RSSIFusion) resampleParticlesIfNeeded() {
    if f.particleFilter == nil {
        return
    }
    eff := 1.0 / f.particleFilter.EffectiveParticleCount()
    if eff < 0.5 {
        f.particleFilter.Resample(bf.WithResampleCriterion(bf.Systematic))
    }
}

// getParticleMaxLikelihood returns the distance of the highest‑weight particle.
func (f *RSSIFusion) getParticleMaxLikelihood() float64 {
    if f.particleFilter == nil {
        return 0
    }
    bestDist := 0.0
    maxWeight := -1.0
    for _, p := range f.particleFilter.Particles() {
        if p.Weight() > maxWeight {
            maxWeight = p.Weight()
            bestDist = p.State().AtVec(0)
        }
    }
    return bestDist
}

// GetCurrentDistance returns the latest filtered distance.
func (f *RSSIFusion) GetCurrentDistance() float64 {
    f.mu.RLock()
    defer f.mu.RUnlock()
    return f.lastFiltered
}