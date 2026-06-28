package gesture

import (
	"math"
	"sync"
	"time"
)

type GestureType int

const (
	GestureNone GestureType = iota
	GestureSwipeLeft
	GestureSwipeRight
	GestureSwipeUp
	GestureSwipeDown
	GestureCircleCW
	GestureCircleCCW
	GestureDoubleTap
	GestureLongPress
)

type Point struct {
	X, Y      float64
	Timestamp time.Time
}

type Detector struct {
	history     []Point
	maxHistory  int
	lastGesture GestureType
	lastTime    time.Time
	cooldown    time.Duration
	mu          sync.Mutex
}

func NewDetector() *Detector {
	return &Detector{
		history:    make([]Point, 0, 50),
		maxHistory: 50,
		cooldown:   500 * time.Millisecond,
	}
}

func (g *Detector) AddMotion(dx, dy float64) {
	g.mu.Lock()
	defer g.mu.Unlock()
	g.history = append(g.history, Point{X: dx, Y: dy, Timestamp: time.Now()})
	if len(g.history) > g.maxHistory {
		g.history = g.history[1:]
	}
}

func (g *Detector) Detect() GestureType {
	g.mu.Lock()
	defer g.mu.Unlock()
	if len(g.history) < 10 {
		return GestureNone
	}
	if time.Since(g.lastTime) < g.cooldown && g.lastGesture != GestureNone {
		return g.lastGesture
	}
	var totalX, totalY float64
	for _, p := range g.history {
		totalX += p.X
		totalY += p.Y
	}
	if math.Abs(totalX) > 50 && math.Abs(totalY) < 20 {
		g.lastTime = time.Now()
		if totalX > 0 {
			g.lastGesture = GestureSwipeRight
			return GestureSwipeRight
		}
		g.lastGesture = GestureSwipeLeft
		return GestureSwipeLeft
	}
	if math.Abs(totalY) > 50 && math.Abs(totalX) < 20 {
		g.lastTime = time.Now()
		if totalY > 0 {
			g.lastGesture = GestureSwipeDown
			return GestureSwipeDown
		}
		g.lastGesture = GestureSwipeUp
		return GestureSwipeUp
	}
	if g.detectCircle() {
		g.lastTime = time.Now()
		g.lastGesture = GestureCircleCW
		return GestureCircleCW
	}
	return GestureNone
}

func (g *Detector) detectCircle() bool {
	if len(g.history) < 20 {
		return false
	}
	var angles []float64
	for i := 1; i < len(g.history); i++ {
		dx := g.history[i].X - g.history[i-1].X
		dy := g.history[i].Y - g.history[i-1].Y
		angle := math.Atan2(dy, dx)
		angles = append(angles, angle)
	}
	var totalChange float64
	for i := 1; i < len(angles); i++ {
		change := angles[i] - angles[i-1]
		totalChange += change
	}
	return math.Abs(totalChange) > 2*math.Pi
}

func (g *Detector) Reset() {
	g.mu.Lock()
	defer g.mu.Unlock()
	g.history = make([]Point, 0, g.maxHistory)
	g.lastGesture = GestureNone
}

func Name(gesture GestureType) string {
	switch gesture {
	case GestureSwipeLeft:
		return "swipe_left"
	case GestureSwipeRight:
		return "swipe_right"
	case GestureSwipeUp:
		return "swipe_up"
	case GestureSwipeDown:
		return "swipe_down"
	case GestureCircleCW:
		return "circle_cw"
	case GestureCircleCCW:
		return "circle_ccw"
	default:
		return "none"
	}
}
