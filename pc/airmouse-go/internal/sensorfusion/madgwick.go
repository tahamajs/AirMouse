package sensorfusion

import "math"

// MadgwickFilter implements the Madgwick AHRS algorithm for orientation estimation
// from gyroscope, accelerometer, and magnetometer data.
// Based on: https://x-io.co.uk/open-source-imu-and-ahrs-algorithms/
type MadgwickFilter struct {
    q          Quaternion // estimated quaternion
    beta       float64    // algorithm gain (default 0.1)
    sampleFreq float64    // sampling frequency in Hz
}

// NewMadgwickFilter creates a new Madgwick filter with default parameters.
func NewMadgwickFilter(sampleFreq float64) *MadgwickFilter {
    return &MadgwickFilter{
        q:          Quaternion{W: 1, X: 0, Y: 0, Z: 0},
        beta:       0.1,
        sampleFreq: sampleFreq,
    }
}

// SetBeta sets the algorithm gain (default 0.1). Higher = faster convergence.
func (f *MadgwickFilter) SetBeta(beta float64) {
    f.beta = beta
}

// Update performs a single filter step using gyroscope, accelerometer, and magnetometer data.
// gyro: angular velocity in rad/s (gx, gy, gz)
// accel: linear acceleration in m/s² (ax, ay, az)
// mag: magnetic field in µT (mx, my, mz) – optional, pass nil if not used.
func (f *MadgwickFilter) Update(gyro, accel, mag []float64) {
    if len(gyro) < 3 || len(accel) < 3 {
        return
    }
    dt := 1.0 / f.sampleFreq

    var q = f.q

    // Normalise accelerometer and magnetometer readings
    normAcc := math.Hypot(math.Hypot(accel[0], accel[1]), accel[2])
    if normAcc == 0 {
        return
    }
    ax := accel[0] / normAcc
    ay := accel[1] / normAcc
    az := accel[2] / normAcc

    var mx, my, mz float64
    if mag != nil && len(mag) >= 3 {
        normMag := math.Hypot(math.Hypot(mag[0], mag[1]), mag[2])
        if normMag != 0 {
            mx = mag[0] / normMag
            my = mag[1] / normMag
            mz = mag[2] / normMag
        }
    }

    // Auxiliary variables to avoid repeated calculations
    q0, q1, q2, q3 := q.W, q.X, q.Y, q.Z

    // Gradient descent algorithm (Madgwick)
    var f1, f2, f3, f4, f5, f6 float64
    var J_11or24, J_12or23, J_13or22, J_14or21, J_32, J_33 float64

    // Compute objective function for accelerometer
    f1 = 2*(q1*q3 - q0*q2) - ax
    f2 = 2*(q0*q1 + q2*q3) - ay
    f3 = 2*(0.5 - q1*q1 - q2*q2) - az

    // Compute Jacobian matrix for accelerometer
    J_11or24 = 2 * q2
    J_12or23 = 2 * q3
    J_13or22 = 2 * q1
    J_14or21 = 2 * q0
    J_32 = 2 * q2
    J_33 = 2 * q1

    // Compute gradient for accelerometer
    g0 := J_14or21*f2 - J_11or24*f1
    g1 := J_12or23*f2 + J_13or22*f3 - J_14or21*f1
    g2 := J_12or23*f1 - J_13or22*f2 + J_32*f3
    g3 := J_11or24*f1 - J_14or21*f2 - J_33*f3

    if mag != nil {
        // Magnetometer part
        hx := 2 * (mx*(0.5-q2*q2-q3*q3) + my*(q1*q2-q0*q3) + mz*(q1*q3+q0*q2))
        hy := 2 * (mx*(q1*q2+q0*q3) + my*(0.5-q1*q1-q3*q3) + mz*(q2*q3-q0*q1))
        hz := 2 * (mx*(q1*q3-q0*q2) + my*(q2*q3+q0*q1) + mz*(0.5-q1*q1-q2*q2))

        // Normalise magnetic field
        normMag := math.Hypot(math.Hypot(hx, hy), hz)
        if normMag > 0 {
            hx /= normMag
            hy /= normMag
            hz /= normMag
        }

        // Reference direction of Earth's magnetic field
        bx := math.Sqrt(hx*hx + hy*hy)
        bz := hz

        // Magnetometer objective function
        f4 := 2 * (bx*(0.5-q2*q2-q3*q3) + bz*(q1*q3-q0*q2)) - mx
        f5 := 2 * (bx*(q1*q2-q0*q3) + bz*(q0*q1+q2*q3)) - my
        f6 := 2 * (bx*(q0*q2+q1*q3) + bz*(0.5-q1*q1-q2*q2)) - mz

        // Jacobian for magnetometer
        J_11 := 2 * (bz*q2 - bx*q3)
        J_12 := 2 * (bx*q2 + bz*q3)
        J_13 := 2 * (-bx*q1 + bz*q0)
        J_14 := 2 * (bx*q0 + bz*q1)
        J_21 := 2 * (bx*q3 - bz*q1)
        J_22 := 2 * (bx*q2 + bz*q0)
        J_23 := 2 * (bx*q1 - bz*q3)
        J_24 := 2 * (-bx*q0 + bz*q2)
        J_31 := 2 * (bx*q2 - bz*q0)
        J_32 := 2 * (bx*q3 + bz*q1)
        J_33 := 2 * (bx*q0 + bz*q2)
        J_34 := 2 * (bx*q1 - bz*q3)

        // Combine gradients
        g0 += J_11*f4 + J_21*f5 + J_31*f6
        g1 += J_12*f4 + J_22*f5 + J_32*f6
        g2 += J_13*f4 + J_23*f5 + J_33*f6
        g3 += J_14*f4 + J_24*f5 + J_34*f6
    }

    // Normalise gradient
    normG := math.Hypot(math.Hypot(g0, g1), math.Hypot(g2, g3))
    if normG > 0 {
        g0 /= normG
        g1 /= normG
        g2 /= normG
        g3 /= normG
    }

    // Compute estimated quaternion derivative from gyroscope
    qDot1 := 0.5 * (-q1*gyro[0] - q2*gyro[1] - q3*gyro[2])
    qDot2 := 0.5 * (q0*gyro[0] + q2*gyro[2] - q3*gyro[1])
    qDot3 := 0.5 * (q0*gyro[1] - q1*gyro[2] + q3*gyro[0])
    qDot4 := 0.5 * (q0*gyro[2] + q1*gyro[1] - q2*gyro[0])

    // Apply feedback step (Madgwick's fusion)
    qDot1 -= f.beta * g0
    qDot2 -= f.beta * g1
    qDot3 -= f.beta * g2
    qDot4 -= f.beta * g3

    // Integrate rate of change of quaternion
    q.W += qDot1 * dt
    q.X += qDot2 * dt
    q.Y += qDot3 * dt
    q.Z += qDot4 * dt

    // Normalise quaternion
    q.Normalize()
    f.q = q
}

// GetQuaternion returns the current estimated orientation as a quaternion.
func (f *MadgwickFilter) GetQuaternion() Quaternion {
    return f.q
}

// GetEuler returns the current orientation as Euler angles (degrees).
func (f *MadgwickFilter) GetEuler() EulerAngles {
    return f.q.ToEuler()
}

// Reset resets the filter to identity orientation.
func (f *MadgwickFilter) Reset() {
    f.q = Quaternion{W: 1, X: 0, Y: 0, Z: 0}
}