package adaptivesmoothing

import (
    "math"
)

// LogNormalVelocity implements the Sigma‑Lognormal model for velocity profiles.
type LogNormalVelocity struct {
    mu      float64   // time delay (peak velocity moment)
    sigma   float64   // response time spread
    D       float64   // distance amplitude
    t0      float64   // movement onset
}

// NewLogNormalVelocity creates a velocity profile from kinematic theory.
func NewLogNormalVelocity(distance float64, peakTimeRatio float64) *LogNormalVelocity {
    // peakTimeRatio is the fraction of total time at which peak velocity occurs
    // Typical human movements peak at ≈ 40‑60% of total movement time
    mu := peakTimeRatio
    sigma := 0.2 + (rand.Float64()*0.1) // spread factor 0.2‑0.3
    D := distance
    return &LogNormalVelocity{
        mu:    mu,
        sigma: sigma,
        D:     D,
        t0:    0,
    }
}

// VelocityAt returns the instantaneous velocity at relative time t (0–1).
func (v *LogNormalVelocity) VelocityAt(t float64) float64 {
    if t < 0 {
        t = 0
    }
    if t > 1 {
        t = 1
    }
    // Normalised log‑normal curve
    // Λ(t) = 1 / (σ * √(2π) * t) * exp( - (ln(t) - μ)² / (2σ²) )
    if t <= 0.001 {
        return 0
    }
    lnTerm := math.Log(t) - v.mu
    numerator := math.Exp(-(lnTerm * lnTerm) / (2 * v.sigma * v.sigma))
    denominator := v.sigma * math.Sqrt(2*math.Pi) * t
    if denominator == 0 {
        return 0
    }
    return v.D * (numerator / denominator)
}

// ScaleMovement applies velocity‑based scaling to a movement delta.
func ScaleMovement(dx, dy float64, distanceRatio float64) (scaledDx, scaledDy float64) {
    // distanceRatio is the fraction of total movement completed (0–1)
    v := NewLogNormalVelocity(math.Hypot(dx, dy), 0.55)
    factor := v.VelocityAt(distanceRatio)
    if factor < 0.1 {
        factor = 0.1
    }
    if factor > 1.5 {
        factor = 1.5
    }
    return dx * factor, dy * factor
}