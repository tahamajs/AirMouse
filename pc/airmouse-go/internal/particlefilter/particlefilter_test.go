package particlefilter

import (
    "testing"
    "time"
)

func TestParticleFilter(t *testing.T) {
    filter := NewFilter(100)
    // Simulate a swipe right motion
    for i := 0; i < 20; i++ {
        filter.Update(5.0, 0.0)
        time.Sleep(10 * time.Millisecond)
    }
    // After many updates, the filter should converge
    // (In a real test, you would check particle distribution)
    t.Log("Filter test passed (no panic)")
}

func TestGestureRecognizer(t *testing.T) {
    rec := NewRecognizer()
    // Simulate a left swipe
    for i := 0; i < 10; i++ {
        rec.AddMotion(-8.0, 0.0)
    }
    gesture, conf := rec.GetGesture()
    if gesture != "swipe_left" {
        t.Errorf("Expected swipe_left, got %s", gesture)
    }
    t.Logf("Recognised %s with confidence %.2f", gesture, conf)
}