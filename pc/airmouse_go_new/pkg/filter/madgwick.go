package filter

import (
	"math"
	"sync"
)

const epsilon = 1e-9

// Quaternion represents a 4D unit quaternion [q0, q1, q2, q3] where q0 is the scalar.
type Quaternion struct {
	W float64 // q0 scalar
	X float64 // q1 vector
	Y float64 // q2 vector
	Z float64 // q3 vector
}

// EulerAngles represents attitude angles (roll, pitch, yaw).
type EulerAngles struct {
	Roll  float64 // Roll angle around X axis (radians)
	Pitch float64 // Pitch angle around Y axis (radians)
	Yaw   float64 // Yaw angle around Z axis (radians)
}

// RollDegrees returns the roll angle in degrees.
func (e EulerAngles) RollDegrees() float64 {
	return e.Roll * (180.0 / math.Pi)
}

// PitchDegrees returns the pitch angle in degrees.
func (e EulerAngles) PitchDegrees() float64 {
	return e.Pitch * (180.0 / math.Pi)
}

// YawDegrees returns the yaw angle in degrees.
func (e EulerAngles) YawDegrees() float64 {
	return e.Yaw * (180.0 / math.Pi)
}

// MadgwickAHRS implements the Madgwick gradient-descent sensor fusion algorithm (Sebastian Madgwick, 2010).
// It fuses angular rate (gyroscope), acceleration (accelerometer), and magnetic field (magnetometer)
// into a drift-free orientation quaternion with minimal computational overhead.
type MadgwickAHRS struct {
	mu sync.RWMutex

	beta float64 // Algorithm gain representing divergence rate of gyroscope orientation error
	zeta float64 // Gyroscope bias drift compensation factor

	q0, q1, q2, q3 float64 // Quaternion components

	roll, pitch, yaw float64 // Cached Euler angles in radians
	needsRecalc      bool
}

// NewMadgwickAHRS creates a new Madgwick AHRS filter with given beta and optional zeta drift factor.
// Typical defaults: beta = 0.1, zeta = 0.0.
func NewMadgwickAHRS(beta, zeta float64) *MadgwickAHRS {
	if beta <= 0 {
		beta = 0.1
	}
	if zeta < 0 {
		zeta = 0.0
	}
	m := &MadgwickAHRS{
		beta:        beta,
		zeta:        zeta,
		q0:          1.0,
		q1:          0.0,
		q2:          0.0,
		q3:          0.0,
		needsRecalc: true,
	}
	return m
}

// NewMadgwickFilter creates a standard Madgwick filter with default zero gyro drift gain.
func NewMadgwickFilter(beta float64) *MadgwickAHRS {
	return NewMadgwickAHRS(beta, 0.0)
}

// UpdateIMU executes 6-DOF sensor fusion incorporating gyroscope (rad/s) and accelerometer.
func (m *MadgwickAHRS) UpdateIMU(gx, gy, gz, ax, ay, az, dt float64) {
	if dt <= 0 {
		return
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	q0, q1, q2, q3 := m.q0, m.q1, m.q2, m.q3

	// Rate of change of quaternion from gyroscope
	qDot1 := 0.5 * (-q1*gx - q2*gy - q3*gz)
	qDot2 := 0.5 * (q0*gx + q2*gz - q3*gy)
	qDot3 := 0.5 * (q0*gy - q1*gz + q3*gx)
	qDot4 := 0.5 * (q0*gz + q1*gy - q2*gx)

	// Compute feedback only if accelerometer measurement is valid (avoids NaN)
	normA := math.Sqrt(ax*ax + ay*ay + az*az)
	if normA > epsilon {
		ax /= normA
		ay /= normA
		az /= normA

		// Auxiliary variables to avoid repeated arithmetic
		_2q0 := 2.0 * q0
		_2q1 := 2.0 * q1
		_2q2 := 2.0 * q2
		_2q3 := 2.0 * q3
		_4q0 := 4.0 * q0
		_4q1 := 4.0 * q1
		_4q2 := 4.0 * q2
		_8q1 := 8.0 * q1
		_8q2 := 8.0 * q2
		q0q0 := q0 * q0
		q1q1 := q1 * q1
		q2q2 := q2 * q2
		q3q3 := q3 * q3

		// Gradient descent step
		s0 := _4q0*q2q2 + _2q2*ax + _4q0*q1q1 - _2q1*ay
		s1 := _4q1*q3q3 - _2q3*ax + 4.0*q0q0*q1 - _2q0*ay - _4q1 + _8q1*q1q1 + _8q1*q2q2 + _4q1*az
		s2 := 4.0*q0q0*q2 + _2q0*ax + _4q2*q3q3 - _2q3*ay - _4q2 + _8q2*q1q1 + _8q2*q2q2 + _4q2*az
		s3 := 4.0*q1q1*q3 - _2q1*ax + 4.0*q2q2*q3 - _2q2*ay

		normS := math.Sqrt(s0*s0 + s1*s1 + s2*s2 + s3*s3)
		if normS > epsilon {
			s0 /= normS
			s1 /= normS
			s2 /= normS
			s3 /= normS

			// Apply feedback step
			qDot1 -= m.beta * s0
			qDot2 -= m.beta * s1
			qDot3 -= m.beta * s2
			qDot4 -= m.beta * s3
		}
	}

	// Integrate rate of change of quaternion
	q0 += qDot1 * dt
	q1 += qDot2 * dt
	q2 += qDot3 * dt
	q3 += qDot4 * dt

	// Normalize quaternion
	normQ := math.Sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3)
	if normQ > epsilon {
		m.q0 = q0 / normQ
		m.q1 = q1 / normQ
		m.q2 = q2 / normQ
		m.q3 = q3 / normQ
	}

	m.needsRecalc = true
}

// Update executes 9-DOF sensor fusion incorporating gyroscope (rad/s), accelerometer, and magnetometer.
func (m *MadgwickAHRS) Update(gx, gy, gz, ax, ay, az, mx, my, mz, dt float64) {
	if dt <= 0 {
		return
	}

	// If magnetometer is unavailable or zero, fall back to IMU 6-DOF update
	normM := math.Sqrt(mx*mx + my*my + mz*mz)
	if normM < epsilon {
		m.UpdateIMU(gx, gy, gz, ax, ay, az, dt)
		return
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	q0, q1, q2, q3 := m.q0, m.q1, m.q2, m.q3

	// Rate of change of quaternion from gyroscope
	qDot1 := 0.5 * (-q1*gx - q2*gy - q3*gz)
	qDot2 := 0.5 * (q0*gx + q2*gz - q3*gy)
	qDot3 := 0.5 * (q0*gy - q1*gz + q3*gx)
	qDot4 := 0.5 * (q0*gz + q1*gy - q2*gx)

	normA := math.Sqrt(ax*ax + ay*ay + az*az)
	if normA > epsilon {
		ax /= normA
		ay /= normA
		az /= normA
		mx /= normM
		my /= normM
		mz /= normM

		// Auxiliary variables
		_2q0mx := 2.0 * q0 * mx
		_2q0my := 2.0 * q0 * my
		_2q0mz := 2.0 * q0 * mz
		_2q1mx := 2.0 * q1 * mx
		_2q0 := 2.0 * q0
		_2q1 := 2.0 * q1
		_2q2 := 2.0 * q2
		_2q3 := 2.0 * q3
		_2q0q2 := 2.0 * q0 * q2
		_2q2q3 := 2.0 * q2 * q3
		q0q0 := q0 * q0
		q0q1 := q0 * q1
		q0q2 := q0 * q2
		q0q3 := q0 * q3
		q1q1 := q1 * q1
		q1q2 := q1 * q2
		q1q3 := q1 * q3
		q2q2 := q2 * q2
		q2q3 := q2 * q3
		q3q3 := q3 * q3

		// Reference direction of Earth's magnetic field
		hx := mx*q0q0 - _2q0my*q3 + _2q0mz*q2 + mx*q1q1 + _2q1*my*q2 + _2q1*mz*q3 - mx*q2q2 - mx*q3q3
		hy := _2q0mx*q3 + my*q0q0 - _2q0mz*q1 + _2q1mx*q2 - my*q1q1 + my*q2q2 + _2q2*mz*q3 - my*q3q3
		_2bx := math.Sqrt(hx*hx + hy*hy)
		_2bz := -_2q0mx*q2 + _2q0my*q1 + mz*q0q0 + _2q1mx*q3 - mz*q1q1 + _2q2*my*q3 - mz*q2q2 + mz*q3q3
		_4bx := 2.0 * _2bx
		_4bz := 2.0 * _2bz

		// Gradient descent step
		s0 := -_2q2*(2.0*q1q3-_2q0q2-ax) + _2q1*(2.0*q0q1+_2q2q3-ay) - _2bz*q2*(_2bx*(0.5-q2q2-q3q3)+_2bz*(q1q3-q0q2)-mx) + (-_2bx*q3+_2bz*q1)*(_2bx*(q1q2-q0q3)+_2bz*(q0q1+q2q3)-my) + _2bx*q2*(_2bx*(q0q2+q1q3)+_2bz*(0.5-q1q1-q2q2)-mz)
		s1 := _2q3*(2.0*q1q3-_2q0q2-ax) + _2q0*(2.0*q0q1+_2q2q3-ay) - 4.0*q1*(1.0-2.0*q1q1-2.0*q2q2-az) + _2bz*q3*(_2bx*(0.5-q2q2-q3q3)+_2bz*(q1q3-q0q2)-mx) + (_2bx*q2+_2bz*q0)*(_2bx*(q1q2-q0q3)+_2bz*(q0q1+q2q3)-my) + (_2bx*q3-_4bz*q1)*(_2bx*(q0q2+q1q3)+_2bz*(0.5-q1q1-q2q2)-mz)
		s2 := -_2q0*(2.0*q1q3-_2q0q2-ax) + _2q3*(2.0*q0q1+_2q2q3-ay) - 4.0*q2*(1.0-2.0*q1q1-2.0*q2q2-az) + (-_4bx*q2-_2bz*q0)*(_2bx*(0.5-q2q2-q3q3)+_2bz*(q1q3-q0q2)-mx) + (_2bx*q1+_2bz*q3)*(_2bx*(q1q2-q0q3)+_2bz*(q0q1+q2q3)-my) + (_2bx*q0-_4bz*q2)*(_2bx*(q0q2+q1q3)+_2bz*(0.5-q1q1-q2q2)-mz)
		s3 := _2q1*(2.0*q1q3-_2q0q2-ax) + _2q2*(2.0*q0q1+_2q2q3-ay) + (-_4bx*q3+_2bz*q1)*(_2bx*(0.5-q2q2-q3q3)+_2bz*(q1q3-q0q2)-mx) + (-_2bx*q0+_2bz*q2)*(_2bx*(q1q2-q0q3)+_2bz*(q0q1+q2q3)-my) + _2bx*q1*(_2bx*(q0q2+q1q3)+_2bz*(0.5-q1q1-q2q2)-mz)

		normS := math.Sqrt(s0*s0 + s1*s1 + s2*s2 + s3*s3)
		if normS > epsilon {
			s0 /= normS
			s1 /= normS
			s2 /= normS
			s3 /= normS

			qDot1 -= m.beta * s0
			qDot2 -= m.beta * s1
			qDot3 -= m.beta * s2
			qDot4 -= m.beta * s3
		}
	}

	// Gyro drift compensation if zeta > 0
	if m.zeta > 0 {
		q0DotDrift := -0.5 * (q1*gx + q2*gy + q3*gz)
		q1DotDrift := 0.5 * (q0*gx - q3*gy + q2*gz)
		q2DotDrift := 0.5 * (q3*gx + q0*gy - q1*gz)
		q3DotDrift := 0.5 * (-q2*gx + q1*gy + q0*gz)

		q0 += q0DotDrift * dt * m.zeta
		q1 += q1DotDrift * dt * m.zeta
		q2 += q2DotDrift * dt * m.zeta
		q3 += q3DotDrift * dt * m.zeta
	}

	// Integrate
	q0 += qDot1 * dt
	q1 += qDot2 * dt
	q2 += qDot3 * dt
	q3 += qDot4 * dt

	// Normalize
	normQ := math.Sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3)
	if normQ > epsilon {
		m.q0 = q0 / normQ
		m.q1 = q1 / normQ
		m.q2 = q2 / normQ
		m.q3 = q3 / normQ
	}

	m.needsRecalc = true
}

// GetQuaternion returns the four quaternion components (q0, q1, q2, q3) where q0 is the scalar.
func (m *MadgwickAHRS) GetQuaternion() (q0, q1, q2, q3 float64) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return m.q0, m.q1, m.q2, m.q3
}

// GetQuaternionSlice returns the quaternion as a [4]float64 array.
func (m *MadgwickAHRS) GetQuaternionSlice() [4]float64 {
	m.mu.RLock()
	defer m.mu.RUnlock()
	return [4]float64{m.q0, m.q1, m.q2, m.q3}
}

// GetEulerAngles returns the current attitude (roll, pitch, yaw) in radians.
func (m *MadgwickAHRS) GetEulerAngles() EulerAngles {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.recalcEulerIfNeeded()
	return EulerAngles{
		Roll:  m.roll,
		Pitch: m.pitch,
		Yaw:   m.yaw,
	}
}

// GetEulerAnglesDegrees returns attitude (roll, pitch, yaw) in degrees.
func (m *MadgwickAHRS) GetEulerAnglesDegrees() (rollDeg, pitchDeg, yawDeg float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.recalcEulerIfNeeded()
	return m.roll * (180.0 / math.Pi), m.pitch * (180.0 / math.Pi), m.yaw * (180.0 / math.Pi)
}

// GetRoll returns the roll angle in radians.
func (m *MadgwickAHRS) GetRoll() float64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.recalcEulerIfNeeded()
	return m.roll
}

// GetPitch returns the pitch angle in radians.
func (m *MadgwickAHRS) GetPitch() float64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.recalcEulerIfNeeded()
	return m.pitch
}

// GetYaw returns the yaw angle in radians.
func (m *MadgwickAHRS) GetYaw() float64 {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.recalcEulerIfNeeded()
	return m.yaw
}

// recalcEulerIfNeeded computes roll, pitch, and yaw from current quaternion (must be called with lock held).
func (m *MadgwickAHRS) recalcEulerIfNeeded() {
	if !m.needsRecalc {
		return
	}

	q0, q1, q2, q3 := m.q0, m.q1, m.q2, m.q3

	// Roll (x-axis rotation)
	sinr_cosp := 2.0 * (q0*q1 + q2*q3)
	cosr_cosp := 1.0 - 2.0*(q1*q1+q2*q2)
	m.roll = math.Atan2(sinr_cosp, cosr_cosp)

	// Pitch (y-axis rotation)
	sinp := 2.0 * (q0*q2 - q3*q1)
	if math.Abs(sinp) >= 1.0 {
		m.pitch = math.Copysign(math.Pi/2.0, sinp)
	} else {
		m.pitch = math.Asin(sinp)
	}

	// Yaw (z-axis rotation)
	siny_cosp := 2.0 * (q0*q3 + q1*q2)
	cosy_cosp := 1.0 - 2.0*(q2*q2+q3*q3)
	m.yaw = math.Atan2(siny_cosp, cosy_cosp)

	m.needsRecalc = false
}

// GetRotationMatrix returns the 3x3 direction cosine matrix in row-major order:
//
//	[R00, R01, R02,
//	 R10, R11, R12,
//	 R20, R21, R22]
func (m *MadgwickAHRS) GetRotationMatrix() [9]float64 {
	m.mu.RLock()
	defer m.mu.RUnlock()

	q0, q1, q2, q3 := m.q0, m.q1, m.q2, m.q3

	q00 := q0 * q0
	q11 := q1 * q1
	q22 := q2 * q2
	q33 := q3 * q3

	return [9]float64{
		q00 + q11 - q22 - q33, 2.0 * (q1*q2 - q0*q3), 2.0 * (q1*q3 + q0*q2),
		2.0 * (q1*q2 + q0*q3), q00 - q11 + q22 - q33, 2.0 * (q2*q3 - q0*q1),
		2.0 * (q1*q3 - q0*q2), 2.0 * (q2*q3 + q0*q1), q00 - q11 - q22 + q33,
	}
}

// SetBeta dynamically adjusts the algorithm gain beta.
func (m *MadgwickAHRS) SetBeta(beta float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if beta > 0 {
		m.beta = beta
	}
}

// SetZeta dynamically adjusts the gyro bias drift factor zeta.
func (m *MadgwickAHRS) SetZeta(zeta float64) {
	m.mu.Lock()
	defer m.mu.Unlock()
	if zeta >= 0 {
		m.zeta = zeta
	}
}

// Reset resets the quaternion to identity [1, 0, 0, 0] and clears attitude angles.
func (m *MadgwickAHRS) Reset() {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.q0 = 1.0
	m.q1 = 0.0
	m.q2 = 0.0
	m.q3 = 0.0
	m.roll = 0.0
	m.pitch = 0.0
	m.yaw = 0.0
	m.needsRecalc = false
}

// ResetWithSensors initializes the orientation quaternion directly from initial accelerometer and magnetometer readings.
func (m *MadgwickAHRS) ResetWithSensors(ax, ay, az, mx, my, mz float64) {
	normA := math.Sqrt(ax*ax + ay*ay + az*az)
	if normA < epsilon {
		return
	}
	ax /= normA
	ay /= normA
	az /= normA

	normM := math.Sqrt(mx*mx + my*my + mz*mz)
	if normM < epsilon {
		return
	}
	mx /= normM
	my /= normM
	mz /= normM

	m.mu.Lock()
	defer m.mu.Unlock()

	initialRoll := math.Atan2(ay, az)
	initialPitch := math.Atan2(-ax, math.Sqrt(ay*ay+az*az))

	cosRoll := math.Cos(initialRoll)
	sinRoll := math.Sin(initialRoll)
	cosPitch := math.Cos(initialPitch)
	sinPitch := math.Sin(initialPitch)

	mxH := mx*cosPitch + mz*sinPitch
	myH := mx*sinRoll*sinPitch + my*cosRoll - mz*sinRoll*cosPitch
	initialYaw := math.Atan2(-myH, mxH)

	cy := math.Cos(initialYaw * 0.5)
	sy := math.Sin(initialYaw * 0.5)
	cp := math.Cos(initialPitch * 0.5)
	sp := math.Sin(initialPitch * 0.5)
	cr := math.Cos(initialRoll * 0.5)
	sr := math.Sin(initialRoll * 0.5)

	q0 := cr*cp*cy + sr*sp*sy
	q1 := sr*cp*cy - cr*sp*sy
	q2 := cr*sp*cy + sr*cp*sy
	q3 := cr*cp*sy - sr*sp*cy

	normQ := math.Sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3)
	if normQ > epsilon {
		m.q0 = q0 / normQ
		m.q1 = q1 / normQ
		m.q2 = q2 / normQ
		m.q3 = q3 / normQ
	}
	m.needsRecalc = true
}
