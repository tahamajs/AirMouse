// Package adaptivesmoothing provides human‑like cursor smoothing.
package adaptivesmoothing

import (
	"sync"
)

// BSpline3 implements a cubic B‑spline for path smoothing.
type BSpline3 struct {
	points   [][2]float64
	knots    []float64
	degree   int
	segments int
	cache    map[[2]int]float64
	mu       sync.RWMutex // protects cache
}

// NewBSpline3 creates a new cubic B‑spline with the given control points.
func NewBSpline3(points [][2]float64, segments int) *BSpline3 {
	if len(points) < 4 {
		points = duplicatePoints(points, 4)
	}
	if segments <= 0 {
		segments = 10
	}

	n := len(points)
	degree := 3
	knots := make([]float64, n+degree+1)
	for i := range knots {
		knots[i] = float64(i)
	}

	return &BSpline3{
		points:   points,
		knots:    knots,
		degree:   degree,
		segments: segments,
		cache:    make(map[[2]int]float64),
	}
}

// duplicatePoints pads the point list to at least minPoints.
func duplicatePoints(points [][2]float64, minPoints int) [][2]float64 {
	if len(points) >= minPoints {
		return points
	}
	result := make([][2]float64, minPoints)
	for i := 0; i < minPoints; i++ {
		idx := i * len(points) / minPoints
		if idx >= len(points) {
			idx = len(points) - 1
		}
		result[i] = points[idx]
	}
	return result
}

// basisFunction evaluates the B‑spline basis function.
func (b *BSpline3) basisFunction(i, k int, t float64) float64 {
	b.mu.RLock()
	key := [2]int{i, k}
	if val, ok := b.cache[key]; ok {
		b.mu.RUnlock()
		return val
	}
	b.mu.RUnlock()

	var result float64
	if k == 0 {
		if b.knots[i] <= t && t < b.knots[i+1] {
			result = 1.0
		} else {
			result = 0.0
		}
	} else {
		denom1 := b.knots[i+k] - b.knots[i]
		denom2 := b.knots[i+k+1] - b.knots[i+1]

		var term1, term2 float64
		if denom1 != 0 {
			term1 = (t - b.knots[i]) / denom1 * b.basisFunction(i, k-1, t)
		}
		if denom2 != 0 {
			term2 = (b.knots[i+k+1] - t) / denom2 * b.basisFunction(i+1, k-1, t)
		}
		result = term1 + term2
	}

	b.mu.Lock()
	b.cache[key] = result
	b.mu.Unlock()
	return result
}

// InterpolateAt returns the interpolated (x, y) at normalised parameter t in [0,1].
func (b *BSpline3) InterpolateAt(t float64) (x, y float64) {
	if t < 0 {
		t = 0
	}
	if t > 1 {
		t = 1
	}
	tScaled := t*(b.knots[len(b.knots)-1]-b.knots[0]) + b.knots[0]

	for i, p := range b.points {
		basis := b.basisFunction(i, b.degree, tScaled)
		x += p[0] * basis
		y += p[1] * basis
	}
	return
}

// SmoothPath returns a slice of interpolated points.
func (b *BSpline3) SmoothPath() [][2]float64 {
	path := make([][2]float64, b.segments+1)
	for i := 0; i <= b.segments; i++ {
		t := float64(i) / float64(b.segments)
		path[i][0], path[i][1] = b.InterpolateAt(t)
	}
	return path
}

// GetPointAt returns the i‑th control point.
func (b *BSpline3) GetPointAt(i int) (x, y float64) {
	if i < 0 || i >= len(b.points) {
		return 0, 0
	}
	return b.points[i][0], b.points[i][1]
}

// GetPoints returns a copy of the control points.
func (b *BSpline3) GetPoints() [][2]float64 {
	result := make([][2]float64, len(b.points))
	copy(result, b.points)
	return result
}

// ClearCache clears the basis‑function cache.
func (b *BSpline3) ClearCache() {
	b.mu.Lock()
	defer b.mu.Unlock()
	b.cache = make(map[[2]int]float64)
}