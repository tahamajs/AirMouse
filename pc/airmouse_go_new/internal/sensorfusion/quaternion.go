package sensorfusion

import "math"

type Quaternion struct {
	W, X, Y, Z float64
}

func (q *Quaternion) Normalize() {
	norm := q.W*q.W + q.X*q.X + q.Y*q.Y + q.Z*q.Z
	if norm == 0 {
		q.W = 1
		return
	}
	invNorm := 1.0 / norm
	q.W *= invNorm
	q.X *= invNorm
	q.Y *= invNorm
	q.Z *= invNorm
}

type EulerAngles struct {
	Roll, Pitch, Yaw float64
}

func (q *Quaternion) ToEuler() EulerAngles {
	sinr := 2.0 * (q.W*q.X + q.Y*q.Z)
	cosr := 1.0 - 2.0*(q.X*q.X+q.Y*q.Y)
	roll := math.Atan2(sinr, cosr)

	sinp := 2.0 * (q.W*q.Y - q.Z*q.X)
	var pitch float64
	if math.Abs(sinp) >= 1 {
		pitch = math.Copysign(math.Pi/2, sinp)
	} else {
		pitch = math.Asin(sinp)
	}

	siny := 2.0 * (q.W*q.Z + q.X*q.Y)
	cosy := 1.0 - 2.0*(q.Y*q.Y+q.Z*q.Z)
	yaw := math.Atan2(siny, cosy)

	return EulerAngles{
		Roll:  roll * 180 / math.Pi,
		Pitch: pitch * 180 / math.Pi,
		Yaw:   yaw * 180 / math.Pi,
	}
}
