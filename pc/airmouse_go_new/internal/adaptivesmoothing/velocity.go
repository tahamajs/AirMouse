package adaptivesmoothing

import (
    "math"
    "math/rand"
)

type LogNormalVelocity struct {
    mu    float64
    sigma float64
    D     float64
    t0    float64
    cache map[float64]float64
}

func NewLogNormalVelocity(distance float64, peakTimeRatio float64) *LogNormalVelocity {
    mu := peakTimeRatio
    if mu < 0.1 {
        mu = 0.1
    }
    if mu > 0.9 {
        mu = 0.9
    }
    
    sigma := 0.2 + (rand.Float64() * 0.1)
    
    return &LogNormalVelocity{
        mu:    mu,
        sigma: sigma,
        D:     distance,
        t0:    0,
        cache: make(map[float64]float64),
    }
}

func (v *LogNormalVelocity) VelocityAt(t float64) float64 {
    if t <= 0.001 {
        return 0
    }
    if t > 1 {
        t = 1
    }
    
    // Check cache
    if val, ok := v.cache[t]; ok {
        return val
    }
    
    lnTerm := math.Log(t) - v.mu
    numer := math.Exp(-(lnTerm * lnTerm) / (2 * v.sigma * v.sigma))
    denom := v.sigma * math.Sqrt(2*math.Pi) * t
    
    var result float64
    if denom != 0 {
        result = v.D * (numer / denom)
    } else {
        result = 0
    }
    
    v.cache[t] = result
    return result
}

func (v *LogNormalVelocity) GetPeakVelocity() float64 {
    tPeak := math.Exp(v.mu - v.sigma*v.sigma)
    if tPeak < 0 {
        tPeak = 0
    }
    if tPeak > 1 {
        tPeak = 1
    }
    return v.VelocityAt(tPeak)
}

func (v *LogNormalVelocity) GetDuration() float64 {
    // Time to reach 90% of total distance
    t := 0.5
    total := v.VelocityAt(t)
    for t < 1 && total < v.D*0.9 {
        t += 0.05
        total += v.VelocityAt(t)
    }
    return t
}

func ScaleMovement(dx, dy float64, distanceRatio float64) (scaledDx, scaledDy float64) {
    distance := math.Hypot(dx, dy)
    if distance < 0.001 {
        return 0, 0
    }
    
    v := NewLogNormalVelocity(distance, 0.55)
    factor := v.VelocityAt(distanceRatio)
    
    if factor < 0.1 {
        factor = 0.1
    }
    if factor > 1.5 {
        factor = 1.5
    }
    
    return dx * factor, dy * factor
}

func ScaleMovementWithProfile(dx, dy float64, profile *LogNormalVelocity) (scaledDx, scaledDy float64) {
    distance := math.Hypot(dx, dy)
    if distance < 0.001 {
        return 0, 0
    }
    
    factor := profile.VelocityAt(0.5)
    if factor < 0.1 {
        factor = 0.1
    }
    if factor > 2.0 {
        factor = 2.0
    }
    
    return dx * factor, dy * factor
}