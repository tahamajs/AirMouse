package control

import "math"

type GestureType int

const (
	GestureNone GestureType = iota
	GestureSwipeLeft
	GestureSwipeRight
	GestureSwipeUp
	GestureSwipeDown
)

type gestureDetector struct {
	history []point
}

type point struct{ X, Y float64 }

func (g *gestureDetector) Detect(dx, dy float64) GestureType {
	if math.Abs(dx) > 50 && math.Abs(dy) < 20 {
		if dx > 0 {
			return GestureSwipeRight
		}
		return GestureSwipeLeft
	}
	if math.Abs(dy) > 50 && math.Abs(dx) < 20 {
		if dy > 0 {
			return GestureSwipeDown
		}
		return GestureSwipeUp
	}
	return GestureNone
}