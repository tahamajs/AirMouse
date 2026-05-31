package adaptivesmoothing

import (
    "math"
)

// BSpline3 generates a 3rd‑order B‑spline curve (degree 3) for smooth transitions.
type BSpline3 struct {
    points     [][2]float64 // control points
    knots      []float64
    degree     int
    segments   int
}

// NewBSpline3 creates a 3rd‑order B‑spline from a slice of (x, y) points.
func NewBSpline3(points [][2]float64, segments int) *BSpline3 {
    n := len(points)
    degree := 3
    knots := make([]float64, n+degree+1)
    
    // Uniform knot vector
    for i := 0; i < len(knots); i++ {
        knots[i] = float64(i)
    }
    
    return &BSpline3{
        points:   points,
        knots:    knots,
        degree:   degree,
        segments: segments,
    }
}

// basisFunction evaluates the B‑spline basis function N(i, k) at parameter t.
func (b *BSpline3) basisFunction(i, k int, t float64) float64 {
    if k == 0 {
        if b.knots[i] <= t && t < b.knots[i+1] {
            return 1.0
        }
        return 0.0
    }
    
    denom1 := b.knots[i+k] - b.knots[i]
    denom2 := b.knots[i+k+1] - b.knots[i+1]
    
    var term1, term2 float64
    if denom1 != 0 {
        term1 = (t - b.knots[i]) / denom1 * b.basisFunction(i, k-1, t)
    }
    if denom2 != 0 {
        term2 = (b.knots[i+k+1] - t) / denom2 * b.basisFunction(i+1, k-1, t)
    }
    return term1 + term2
}

// InterpolateAt returns the (x, y) position at parameter t (0 ≤ t ≤ 1).
func (b *BSpline3) InterpolateAt(t float64) (x, y float64) {
    // Map t to knot span
    tScaled := t * float64(b.knots[len(b.knots)-1]-b.knots[0]) + b.knots[0]
    n := len(b.points)
    
    for i := 0; i < n; i++ {
        basis := b.basisFunction(i, b.degree, tScaled)
        x += b.points[i][0] * basis
        y += b.points[i][1] * basis
    }
    return
}

// SmoothPath generates a smooth path by interpolating between control points.
func (b *BSpline3) SmoothPath() [][2]float64 {
    path := make([][2]float64, b.segments+1)
    for i := 0; i <= b.segments; i++ {
        t := float64(i) / float64(b.segments)
        x, y := b.InterpolateAt(t)
        path[i] = [2]float64{x, y}
    }
    return path
}