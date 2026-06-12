package particlefilter

import (
	"math"
	"sync"
)

// GestureState defines the hidden state of the particle filter.
type GestureState struct {
	X, Y, Vx, Vy float64 // position and velocity (for tracking)
	Gesture      string  // discrete gesture type
	Confidence   float64 // probability of this gesture
}

// Filter keeps a small history of the latest motion sample and exposes a
// lightweight heuristic gesture classifier.
type Filter struct {
	mu     sync.RWMutex
	lastDx float64
	lastDy float64
}

// NewFilter creates a filter instance.
func NewFilter(numParticles int) *Filter {
	return &Filter{}
}

// Update stores the latest movement sample.
func (f *Filter) Update(dx, dy float64) {
	f.mu.Lock()
	f.lastDx = dx
	f.lastDy = dy
	f.mu.Unlock()
}

// GetBestGesture returns the most likely gesture and its confidence.
func (f *Filter) GetBestGesture() (gesture string, confidence float64) {
	f.mu.RLock()
	dx, dy := f.lastDx, f.lastDy
	f.mu.RUnlock()

	mag := math.Hypot(dx, dy)
	if mag < 1e-6 {
		return "idle", 0.0
	}

	switch {
	case math.Abs(dx) > math.Abs(dy) && dx < 0:
		return "swipe_left", math.Min(1.0, mag/25.0)
	case math.Abs(dx) > math.Abs(dy) && dx > 0:
		return "swipe_right", math.Min(1.0, mag/25.0)
	case dy < 0:
		return "scroll_up", math.Min(1.0, mag/20.0)
	default:
		return "scroll_down", math.Min(1.0, mag/20.0)
	}
}
