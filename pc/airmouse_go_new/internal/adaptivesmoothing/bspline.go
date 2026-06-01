package adaptivesmoothing

type BSpline3 struct {
	points   [][2]float64
	knots    []float64
	degree   int
	segments int
}

func NewBSpline3(points [][2]float64, segments int) *BSpline3 {
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
	}
}

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

func (b *BSpline3) InterpolateAt(t float64) (x, y float64) {
	tScaled := t*(b.knots[len(b.knots)-1]-b.knots[0]) + b.knots[0]
	for i, p := range b.points {
		basis := b.basisFunction(i, b.degree, tScaled)
		x += p[0] * basis
		y += p[1] * basis
	}
	return
}

func (b *BSpline3) SmoothPath() [][2]float64 {
	path := make([][2]float64, b.segments+1)
	for i := 0; i <= b.segments; i++ {
		t := float64(i) / float64(b.segments)
		path[i][0], path[i][1] = b.InterpolateAt(t)
	}
	return path
}
