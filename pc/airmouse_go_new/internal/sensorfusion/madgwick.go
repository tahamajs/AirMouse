package sensorfusion

import "math"

type MadgwickFilter struct {
	q          Quaternion
	beta       float64
	sampleFreq float64
}

func NewMadgwickFilter(sampleFreq float64) *MadgwickFilter {
	return &MadgwickFilter{
		q:          Quaternion{W: 1, X: 0, Y: 0, Z: 0},
		beta:       0.1,
		sampleFreq: sampleFreq,
	}
}

func (f *MadgwickFilter) Update(gyro, accel, mag []float64) {
	if len(gyro) < 3 || len(accel) < 3 {
		return
	}
	dt := 1.0 / f.sampleFreq
	q0, q1, q2, q3 := f.q.W, f.q.X, f.q.Y, f.q.Z

	normAcc := math.Hypot(math.Hypot(accel[0], accel[1]), accel[2])
	if normAcc == 0 {
		return
	}
	ax, ay, az := accel[0]/normAcc, accel[1]/normAcc, accel[2]/normAcc

	var mx, my, mz float64
	if mag != nil && len(mag) >= 3 {
		normMag := math.Hypot(math.Hypot(mag[0], mag[1]), mag[2])
		if normMag != 0 {
			mx, my, mz = mag[0]/normMag, mag[1]/normMag, mag[2]/normMag
		}
	}

	// Accelerometer error
	f1 := 2*(q1*q3-q0*q2) - ax
	f2 := 2*(q0*q1+q2*q3) - ay
	f3 := 2*(0.5-q1*q1-q2*q2) - az
	J11or24 := 2 * q2
	J12or23 := 2 * q3
	J13or22 := 2 * q1
	J14or21 := 2 * q0
	J32 := 2 * q2
	J33 := 2 * q1
	g0 := J14or21*f2 - J11or24*f1
	g1 := J12or23*f2 + J13or22*f3 - J14or21*f1
	g2 := J12or23*f1 - J13or22*f2 + J32*f3
	g3 := J11or24*f1 - J14or21*f2 - J33*f3

	if mag != nil {
		hx := 2 * (mx*(0.5-q2*q2-q3*q3) + my*(q1*q2-q0*q3) + mz*(q1*q3+q0*q2))
		hy := 2 * (mx*(q1*q2+q0*q3) + my*(0.5-q1*q1-q3*q3) + mz*(q2*q3-q0*q1))
		hz := 2 * (mx*(q1*q3-q0*q2) + my*(q2*q3+q0*q1) + mz*(0.5-q1*q1-q2*q2))
		normMag := math.Hypot(math.Hypot(hx, hy), hz)
		if normMag > 0 {
			hx /= normMag
			hy /= normMag
			hz /= normMag
		}
		bx := math.Sqrt(hx*hx + hy*hy)
		bz := hz
		f4 := 2*(bx*(0.5-q2*q2-q3*q3)+bz*(q1*q3-q0*q2)) - mx
		f5 := 2*(bx*(q1*q2-q0*q3)+bz*(q0*q1+q2*q3)) - my
		f6 := 2*(bx*(q0*q2+q1*q3)+bz*(0.5-q1*q1-q2*q2)) - mz
		J11 := 2 * (bz*q2 - bx*q3)
		J12 := 2 * (bx*q2 + bz*q3)
		J13 := 2 * (-bx*q1 + bz*q0)
		J14 := 2 * (bx*q0 + bz*q1)
		J21 := 2 * (bx*q3 - bz*q1)
		J22 := 2 * (bx*q2 + bz*q0)
		J23 := 2 * (bx*q1 - bz*q3)
		J24 := 2 * (-bx*q0 + bz*q2)
		J31 := 2 * (bx*q2 - bz*q0)
		J32 := 2 * (bx*q3 + bz*q1)
		J33 := 2 * (bx*q0 + bz*q2)
		J34 := 2 * (bx*q1 - bz*q3)
		g0 += J11*f4 + J21*f5 + J31*f6
		g1 += J12*f4 + J22*f5 + J32*f6
		g2 += J13*f4 + J23*f5 + J33*f6
		g3 += J14*f4 + J24*f5 + J34*f6
	}

	normG := math.Hypot(math.Hypot(g0, g1), math.Hypot(g2, g3))
	if normG > 0 {
		g0, g1, g2, g3 = g0/normG, g1/normG, g2/normG, g3/normG
	}

	// Gyroscope quaternion derivative
	qDot1 := 0.5 * (-q1*gyro[0] - q2*gyro[1] - q3*gyro[2])
	qDot2 := 0.5 * (q0*gyro[0] + q2*gyro[2] - q3*gyro[1])
	qDot3 := 0.5 * (q0*gyro[1] - q1*gyro[2] + q3*gyro[0])
	qDot4 := 0.5 * (q0*gyro[2] + q1*gyro[1] - q2*gyro[0])

	qDot1 -= f.beta * g0
	qDot2 -= f.beta * g1
	qDot3 -= f.beta * g2
	qDot4 -= f.beta * g3

	f.q.W += qDot1 * dt
	f.q.X += qDot2 * dt
	f.q.Y += qDot3 * dt
	f.q.Z += qDot4 * dt
	f.q.Normalize()
}

func (f *MadgwickFilter) GetQuaternion() Quaternion { return f.q }
func (f *MadgwickFilter) GetEuler() EulerAngles     { return f.q.ToEuler() }
func (f *MadgwickFilter) Reset()                    { f.q = Quaternion{W: 1, X: 0, Y: 0, Z: 0} }
