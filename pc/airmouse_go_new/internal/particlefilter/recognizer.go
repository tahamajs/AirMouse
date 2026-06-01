package particlefilter

import (
	"math"
	"sync"
)

type GestureModel struct {
	Name      string
	Templates [][]float64 // sequence of (dx, dy) steps
	Tolerance float64
}

type Recognizer struct {
	filter     *Filter
	gestures   []GestureModel
	history    [][2]float64
	maxHistory int
	mu         sync.Mutex
}

func NewRecognizer() *Recognizer {
	return &Recognizer{
		filter:     NewFilter(500),
		gestures:   predefinedGestures(),
		maxHistory: 30,
	}
}

func predefinedGestures() []GestureModel {
	return []GestureModel{
		{Name: "swipe_left", Templates: [][]float64{{-10, 0}, {-10, 0}, {-10, 0}}, Tolerance: 5.0},
		{Name: "swipe_right", Templates: [][]float64{{10, 0}, {10, 0}, {10, 0}}, Tolerance: 5.0},
		{Name: "circle_cw", Templates: [][]float64{{5, 5}, {0, 10}, {-5, 5}, {-10, 0}, {-5, -5}, {0, -10}, {5, -5}, {10, 0}}, Tolerance: 3.0},
	}
}

func (r *Recognizer) AddMotion(dx, dy float64) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.history = append(r.history, [2]float64{dx, dy})
	if len(r.history) > r.maxHistory {
		r.history = r.history[1:]
	}
	r.filter.Predict(0.02)
	r.filter.Update(dx, dy)
}

func (r *Recognizer) GetGesture() (gesture string, confidence float64) {
	r.mu.Lock()
	defer r.mu.Unlock()
	if len(r.history) < 10 {
		return "none", 0.0
	}
	bestScore := -1.0
	bestGesture := "none"
	for _, g := range r.gestures {
		score := r.matchTemplate(g)
		if score > bestScore && score > 0.6 {
			bestScore = score
			bestGesture = g.Name
		}
	}
	return bestGesture, bestScore
}

func (r *Recognizer) matchTemplate(gesture GestureModel) float64 {
	if len(r.history) < len(gesture.Templates) {
		return 0.0
	}
	var totalErr float64
	for i, t := range gesture.Templates {
		hist := r.history[i]
		errX := hist[0] - t[0]
		errY := hist[1] - t[1]
		totalErr += errX*errX + errY*errY
	}
	return math.Max(0, 1.0-totalErr/float64(len(gesture.Templates))/gesture.Tolerance)
}
