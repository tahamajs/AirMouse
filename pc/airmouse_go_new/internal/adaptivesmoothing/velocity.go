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
}

func NewLogNormalVelocity(distance float64, peakTimeRatio float64) *LogNormalVelocity {
	mu := peakTimeRatio
	sigma := 0.2 + (rand.Float64() * 0.1)
	return &LogNormalVelocity{
		mu:    mu,
		sigma: sigma,
		D:     distance,
		t0:    0,
	}
}

func (v *LogNormalVelocity) VelocityAt(t float64) float64 {
	if t <= 0.001 {
		return 0
	}
	if t > 1 {
		t = 1
	}
	lnTerm := math.Log(t) - v.mu
	numer := math.Exp(-(lnTerm * lnTerm) / (2 * v.sigma * v.sigma))
	denom := v.sigma * math.Sqrt(2*math.Pi) * t
	if denom == 0 {
		return 0
	}
	return v.D * (numer / denom)
}

func ScaleMovement(dx, dy float64, distanceRatio float64) (scaledDx, scaledDy float64) {
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