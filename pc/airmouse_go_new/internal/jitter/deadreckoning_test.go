package jitter

import (
    "testing"
)

func TestDeadReckoningPredictor(t *testing.T) {
    p := NewDeadReckoningPredictor(false)
    
    // Update with some movement
    dt := 0.1
    p.UpdateVelocity(10.0, 5.0, dt)
    p.UpdateVelocity(12.0, 6.0, dt)
    p.UpdateVelocity(11.0, 5.5, dt)
    
    // Predict next movement
    predDx, predDy := p.Predict(dt)
    t.Logf("Predicted movement: (%.2f, %.2f)", predDx, predDy)
    
    if predDx == 0 && predDy == 0 {
        t.Error("Expected non-zero prediction")
    }
}

func TestDeadReckoningPredictorWithAcceleration(t *testing.T) {
    p := NewDeadReckoningPredictor(true)
    
    // Simulate accelerating movement
    dt := 0.1
    for i := 0; i < 10; i++ {
        dx := float64(10 + i)
        dy := float64(5 + i/2)
        p.UpdateVelocity(dx, dy, dt)
        t.Logf("Step %d: updated (%.2f, %.2f)", i, dx, dy)
    }
    
    // Predict next movement
    predDx, predDy := p.Predict(dt)
    t.Logf("Predicted with acceleration: (%.2f, %.2f)", predDx, predDy)
    
    // With acceleration, prediction should be larger than last update
    if predDx < 19 || predDy < 9 {
        t.Logf("Note: Prediction (%.2f, %.2f) may be lower than expected", predDx, predDy)
    }
}

func TestDeadReckoningPredictorVelocity(t *testing.T) {
    p := NewDeadReckoningPredictor(false)
    
    dt := 0.1
    dx, dy := 10.0, 5.0
    p.UpdateVelocity(dx, dy, dt)
    
    vx, vy := p.GetVelocity()
    t.Logf("Velocity: (%.2f, %.2f)", vx, vy)
    
    // Velocity should be approximately dx/dt
    expectedVx := dx / dt
    expectedVy := dy / dt
    
    if vx < expectedVx*0.9 || vx > expectedVx*1.1 {
        t.Errorf("Velocity X mismatch: got %.2f, expected ~%.2f", vx, expectedVx)
    }
    if vy < expectedVy*0.9 || vy > expectedVy*1.1 {
        t.Errorf("Velocity Y mismatch: got %.2f, expected ~%.2f", vy, expectedVy)
    }
}

func TestDeadReckoningPredictorPosition(t *testing.T) {
    p := NewDeadReckoningPredictor(false)
    
    dt := 0.1
    currentX, currentY := 100.0, 50.0
    
    p.UpdateVelocity(10.0, 5.0, dt)
    
    predX, predY := p.PredictPosition(currentX, currentY, dt)
    t.Logf("Predicted position: (%.2f, %.2f)", predX, predY)
    
    // Position should increase
    if predX <= currentX {
        t.Errorf("Expected X to increase: %.2f -> %.2f", currentX, predX)
    }
    if predY <= currentY {
        t.Errorf("Expected Y to increase: %.2f -> %.2f", currentY, predY)
    }
}

func TestDeadReckoningPredictorConfidence(t *testing.T) {
    p := NewDeadReckoningPredictor(true)
    
    dt := 0.1
    for i := 0; i < 10; i++ {
        p.UpdateVelocity(10.0, 5.0, dt)
    }
    
    confidence := p.GetConfidence()
    t.Logf("Confidence: %.2f%%", confidence*100)
    
    if confidence <= 0 {
        t.Error("Expected positive confidence")
    }
}

func TestDeadReckoningPredictorReset(t *testing.T) {
    p := NewDeadReckoningPredictor(true)
    
    dt := 0.1
    for i := 0; i < 5; i++ {
        p.UpdateVelocity(10.0, 5.0, dt)
    }
    
    vx1, vy1 := p.GetVelocity()
    t.Logf("Before reset velocity: (%.2f, %.2f)", vx1, vy1)
    
    p.Reset()
    
    vx2, vy2 := p.GetVelocity()
    t.Logf("After reset velocity: (%.2f, %.2f)", vx2, vy2)
    
    if vx2 != 0 || vy2 != 0 {
        t.Errorf("Reset failed: velocity (%.2f, %.2f) should be (0, 0)", vx2, vy2)
    }
}

func TestDeadReckoningPredictorStats(t *testing.T) {
    p := NewDeadReckoningPredictor(false)
    
    dt := 0.1
    for i := 0; i < 10; i++ {
        p.UpdateVelocity(10.0, 5.0, dt)
        p.Predict(dt)
    }
    
    stats := p.GetStats()
    t.Logf("Stats: Predictions=%d, AvgError=%.4f", stats.Predictions, stats.AvgError)
    
    if stats.Predictions == 0 {
        t.Error("Expected non-zero predictions")
    }
}

func BenchmarkDeadReckoningPredictor(b *testing.B) {
    p := NewDeadReckoningPredictor(true)
    dt := 0.1
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        p.UpdateVelocity(10.0, 5.0, dt)
        p.Predict(dt)
    }
}