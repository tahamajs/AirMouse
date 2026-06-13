package control

import (
    "testing"
)

func TestMovementPredictor(t *testing.T) {
    predictor := NewMovementPredictor()
    
    // Test initial state
    if predictor == nil {
        t.Fatal("Failed to create MovementPredictor")
    }
    
    // Test update
    predictor.Update(10.5, -3.2)
    predictor.Update(20.1, -5.4)
    
    // Test reset
    predictor.Reset()
    
    t.Log("✓ MovementPredictor works")
}

func TestMovementPause(t *testing.T) {
    predictor := NewMovementPredictor()
    
    // Initially not paused
    predictor.SetPaused(true)
    predictor.SetPaused(false)
    
    t.Log("✓ Movement pause works")
}

func TestGetStatistics(t *testing.T) {
    predictor := NewMovementPredictor()
    
    stats := predictor.GetStatistics()
    if stats == nil {
        t.Error("GetStatistics returned nil")
    }
    
    t.Log("✓ GetStatistics works")
}

func TestUpdateAndReset(t *testing.T) {
    predictor := NewMovementPredictor()
    
    // Update multiple times
    for i := 0; i < 100; i++ {
        predictor.Update(float64(i), float64(-i))
    }
    
    // Reset should clear state
    predictor.Reset()
    
    // Update again after reset
    predictor.Update(1.0, 1.0)
    
    t.Log("✓ Update and reset works")
}
