package proximity

import (
    "testing"
    "math"
)

func TestRSSIToDistance(t *testing.T) {
    cfg := DefaultRSSIFusionConfig()
    f := NewRSSIFusion(cfg)
    // At 1 meter, RSSI should be close to txPower
    dist := f.rssiToDistance(cfg.TxPower)
    if math.Abs(dist-1.0) > 0.3 {
        t.Errorf("Expected distance ~1.0m, got %.2fm", dist)
    }
}

func TestKalmanFilter(t *testing.T) {
    kf := NewKalmanFilter1D(0.5, 5.0)
    // Simulate a noisy RSSI sequence
    measurements := []float64{-59, -58, -61, -57, -60, -56, -62}
    for _, z := range measurements {
        filtered := kf.Update(z)
        t.Logf("raw %.1f → filtered %.1f", z, filtered)
    }
}

func TestParticleFilterConvergence(t *testing.T) {
    cfg := DefaultRSSIFusionConfig()
    cfg.UseKalman = false
    cfg.UseParticle = true
    cfg.NumParticles = 500
    f := NewRSSIFusion(cfg)

    // Simulate moving from 0.5m to 4m
    distances := []float64{0.5, 1.0, 2.0, 3.0, 4.0}
    for _, trueDist := range distances {
        // Convert true distance to RSSI (inverse of log‑distance)
        rssi := cfg.TxPower - 10*cfg.EnvFactor*math.Log10(trueDist)
        estDist := f.ProcessRSSI(rssi)
        if math.Abs(estDist-trueDist) > 1.0 {
            t.Errorf("At true distance %.1fm, estimated %.2fm", trueDist, estDist)
        }
    }
}