package adaptivesmoothing

import (
    "testing"
    "math"
)

func TestHumanizerProcess(t *testing.T) {
    h := NewHumanizer(DefaultHumanizerConfig())
    
    // Simulate a movement
    for i := 0; i < 50; i++ {
        dx, dy := h.Process(5.0, 3.0, float64(i), float64(i))
        if math.IsNaN(dx) || math.IsNaN(dy) {
            t.Errorf("NaN output at step %d: (%f, %f)", i, dx, dy)
        }
        // Tremor should add some variation
        if i > 10 && dx == 5.0 && dy == 3.0 {
            t.Error("Tremor not applied – movement unchanged")
        }
    }
}

func TestTremorSimulator(t *testing.T) {
    tRem := NewTremorSimulator()
    for i := 0; i < 100; i++ {
        dx, dy := tRem.Update()
        if math.Abs(dx) > 10 || math.Abs(dy) > 10 {
            t.Errorf("Tremor amplitude too high: %f, %f", dx, dy)
        }
    }
}