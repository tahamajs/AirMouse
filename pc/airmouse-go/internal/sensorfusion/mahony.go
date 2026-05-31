package sensorfusion

import "math"

// MahonyFilter implements the Mahony AHRS algorithm using a proportional‑integral (PI) controller.
// It is computationally lighter than Madgwick and Kalman filters.
type MahonyFilter struct {
    q          Quaternion
    integralFB [3]float64 // integral error term for PI controller
    kp         float64     // proportional gain (default 0.5)
    ki         float64     // integral gain (default 0.0)
    sampleFreq float64
}

// NewMahonyFilter creates a new Mahony filter with default gains.
func NewMahonyFilter(sampleFreq float64) *MahonyFilter {
    return &MahonyFilter{
        q:          Quaternion{W: 1, X: 0, Y: 0, Z: 0},
        kp:         0.5,
        ki:         0.0,
        sampleFreq: sampleFreq,
    }
}

// SetGains sets the proportional (kp) and integral (ki) gains.
// kp controls convergence speed; ki reduces steady-state error (recommended = 0.0 for most cases).
func (f *MahonyFilter) SetGains(kp, ki float64) {
    f.kp = kp
    f.ki = ki
}

// Update performs a single filter step using gyroscope, accelerometer, and magnetometer.
func (f *MahonyFilter) Update(gyro, accel, mag []float64) {
    if len(gyro) < 3 || len(accel) < 3 {
        return
    }
    dt := 1.0 / f.sampleFreq

    q0, q1, q2, q3 := f.q.W, f.q.X, f.q.Y, f.q.Z

    // Normalise accelerometer reading
    normAcc := math.Hypot(math.Hypot(accel[0], accel[1]), accel[2])
    if normAcc == 0 {
        return
    }
    ax := accel[0] / normAcc
    ay := accel[1] / normAcc
    az := accel[2] / normAcc

    // Compute estimated gravity direction from quaternion
    vx := 2 * (q1*q3 - q0*q2)
    vy := 2 * (q0*q1 + q2*q3)
    vz := q0*q0 - q1*q1 - q2*q2 + q3*q3

    // Compute error between measured and estimated gravity
    ex := ay*vz - az*vy
    ey := az*vx - ax*vz
    ez := ax*vy - ay*vx

    if mag != nil && len(mag) >= 3 {
        // Normalise magnetometer reading
        normMag := math.Hypot(math.Hypot(mag[0], mag[1]), mag[2])
        if normMag > 0 {
            mx := mag[0] / normMag
            my := mag[1] / normMag
            mz := mag[2] / normMag

            // Compute reference direction of Earth's magnetic field
            hx := 2 * (mx*(0.5-q2*q2-q3*q3) + my*(q1*q2-q0*q3) + mz*(q1*q3+q0*q2))
            hy := 2 * (mx*(q1*q2+q0*q3) + my*(0.5-q1*q1-q3*q3) + mz*(q2*q3-q0*q1))
            hz := 2 * (mx*(q1*q3-q0*q2) + my*(q2*q3+q0*q1) + mz*(0.5-q1*q1-q2*q2))

            bx := math.Sqrt(hx*hx + hy*hy)
            bz := hz

            // Compute magnetometer error
            wx := 2 * (bx*(0.5-q2*q2-q3*q3) + bz*(q1*q3-q0*q2))
            wy := 2 * (bx*(q1*q2-q0*q3) + bz*(q0*q1+q2*q3))
            wz := 2 * (bx*(q0*q2+q1*q3) + bz*(0.5-q1*q1-q2*q2))

            // Combine with accelerometer error
            ex += (my*wz - mz*wy)
            ey += (mz*wx - mx*wz)
            ez += (mx*wy - my*wx)
        }
    }

    // Apply proportional feedback
    if f.kp > 0 {
        ex *= f.kp
        ey *= f.kp
        ez *= f.kp
    }

    // Apply integral feedback
    if f.ki > 0 {
        f.integralFB[0] += ex * f.ki * dt
        f.integralFB[1] += ey * f.ki * dt
        f.integralFB[2] += ez * f.ki * dt
        ex += f.integralFB[0]
        ey += f.integralFB[1]
        ez += f.integralFB[2]
    }

    // Apply feedback to gyro measurements
    gx := gyro[0] + ex
    gy := gyro[1] + ey
    gz := gyro[2] + ez

    // Integrate quaternion
    qDot1 := 0.5 * (-q1*gx - q2*gy - q3*gz)
    qDot2 := 0.5 * (q0*gx + q2*gz - q3*gy)
    qDot3 := 0.5 * (q0*gy - q1*gz + q3*gx)
    qDot4 := 0.5 * (q0*gz + q1*gy - q2*gx)

    q0 += qDot1 * dt
    q1 += qDot2 * dt
    q2 += qDot3 * dt
    q3 += qDot4 * dt

    // Normalise
    f.q = Quaternion{W: q0, X: q1, Y: q2, Z: q3}
    f.q.Normalize()
}

// GetQuaternion returns the current estimated quaternion.
func (f *MahonyFilter) GetQuaternion() Quaternion {
    return f.q
}

// GetEuler returns the current Euler angles (degrees).
func (f *MahonyFilter) GetEuler() EulerAngles {
    return f.q.ToEuler()
}

// Reset resets the filter to identity orientation.
func (f *MahonyFilter) Reset() {
    f.q = Quaternion{W: 1, X: 0, Y: 0, Z: 0}
    f.integralFB = [3]float64{0, 0, 0}
}