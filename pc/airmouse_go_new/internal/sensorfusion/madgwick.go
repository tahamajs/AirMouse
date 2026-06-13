package sensorfusion

import (
    "math"
)

// MadgwickFilter implements the Madgwick AHRS algorithm
type MadgwickFilter struct {
    q          Quaternion
    beta       float64
    sampleFreq float64
    zeta       float64 // Gyroscope drift correction
    angles     EulerAngles
    initialized bool
}

// NewMadgwickFilter creates a new Madgwick filter
func NewMadgwickFilter(sampleFreq float64) *MadgwickFilter {
    return &MadgwickFilter{
        q:          Identity(),
        beta:       0.1,    // Default beta (gain)
        sampleFreq: sampleFreq,
        zeta:       0.0,    // Gyroscope drift
        initialized: false,
    }
}

// SetBeta sets the beta gain parameter
func (f *MadgwickFilter) SetBeta(beta float64) {
    f.beta = beta
}

// SetZeta sets the zeta gain for gyroscope drift
func (f *MadgwickFilter) SetZeta(zeta float64) {
    f.zeta = zeta
}

// Update updates the filter with gyroscope and accelerometer data
func (f *MadgwickFilter) Update(gyro, accel []float64) {
    f.UpdateWithMagnetometer(gyro, accel, nil)
}

// UpdateWithMagnetometer updates with gyroscope, accelerometer, and magnetometer
func (f *MadgwickFilter) UpdateWithMagnetometer(gyro, accel, mag []float64) {
    if len(gyro) < 3 || len(accel) < 3 {
        return
    }
    
    dt := 1.0 / f.sampleFreq
    
    // Normalize accelerometer
    normAcc := math.Hypot(math.Hypot(accel[0], accel[1]), accel[2])
    if normAcc == 0 {
        return
    }
    ax, ay, az := accel[0]/normAcc, accel[1]/normAcc, accel[2]/normAcc
    
    // Get quaternion components
    q0, q1, q2, q3 := f.q.W, f.q.X, f.q.Y, f.q.Z
    
    // Gradient descent algorithm correction
    var f1, f2, f3 float64
    var J11, J12, J13, J14, J21, J22, J23, J24, J31, J32, J33, J34 float64
    
    // Accelerometer error
    f1 = 2*(q1*q3 - q0*q2) - ax
    f2 = 2*(q0*q1 + q2*q3) - ay
    f3 = 2*(0.5 - q1*q1 - q2*q2) - az
    
    J11 = 2 * q2
    J12 = 2 * q3
    J13 = 2 * q1
    J14 = 2 * q0
    J21 = -2 * q1
    J22 = 2 * q0
    J23 = 2 * q3
    J24 = 2 * q2
    J31 = 0
    J32 = -4 * q1
    J33 = -4 * q2
    J34 = 0
    
    // Gradient computation
    g0 := J11*f1 + J21*f2 + J31*f3
    g1 := J12*f1 + J22*f2 + J32*f3
    g2 := J13*f1 + J23*f2 + J33*f3
    g3 := J14*f1 + J24*f2 + J34*f3
    
    // Magnetometer correction if available
    if mag != nil && len(mag) >= 3 {
        normMag := math.Hypot(math.Hypot(mag[0], mag[1]), mag[2])
        if normMag > 0 {
            mx, my, mz := mag[0]/normMag, mag[1]/normMag, mag[2]/normMag
            
            // Compute reference direction of magnetic field
            hx := 2*(mx*(0.5-q2*q2-q3*q3) + my*(q1*q2-q0*q3) + mz*(q1*q3+q0*q2))
            hy := 2*(mx*(q1*q2+q0*q3) + my*(0.5-q1*q1-q3*q3) + mz*(q2*q3-q0*q1))
            hz := 2*(mx*(q1*q3-q0*q2) + my*(q2*q3+q0*q1) + mz*(0.5-q1*q1-q2*q2))
            
            bx := math.Sqrt(hx*hx + hy*hy)
            bz := hz
            
            // Magnetometer error
            f4 := 2*(bx*(0.5-q2*q2-q3*q3) + bz*(q1*q3-q0*q2)) - mx
            f5 := 2*(bx*(q1*q2-q0*q3) + bz*(q0*q1+q2*q3)) - my
            f6 := 2*(bx*(q0*q2+q1*q3) + bz*(0.5-q1*q1-q2*q2)) - mz
            
            J41 := 2*(bz*q2 - bx*q3)
            J42 := 2*(bx*q2 + bz*q3)
            J43 := 2*(-bx*q1 + bz*q0)
            J44 := 2*(bx*q0 + bz*q1)
            J51 := 2*(bx*q3 - bz*q1)
            J52 := 2*(bx*q2 + bz*q0)
            J53 := 2*(bx*q1 - bz*q3)
            J54 := 2*(-bx*q0 + bz*q2)
            J61 := 2*(bx*q2 - bz*q0)
            J62 := 2*(bx*q3 + bz*q1)
            J63 := 2*(bx*q0 + bz*q2)
            J64 := 2*(bx*q1 - bz*q3)
            
            // Update gradient with magnetometer
            g0 += J41*f4 + J51*f5 + J61*f6
            g1 += J42*f4 + J52*f5 + J62*f6
            g2 += J43*f4 + J53*f5 + J63*f6
            g3 += J44*f4 + J54*f5 + J64*f6
        }
    }
    
    // Normalize gradient
    normG := math.Hypot(math.Hypot(g0, g1), math.Hypot(g2, g3))
    if normG > 0 {
        g0, g1, g2, g3 = g0/normG, g1/normG, g2/normG, g3/normG
    }
    
    // Gyroscope quaternion derivative
    qDot1 := 0.5 * (-q1*gyro[0] - q2*gyro[1] - q3*gyro[2])
    qDot2 := 0.5 * (q0*gyro[0] + q2*gyro[2] - q3*gyro[1])
    qDot3 := 0.5 * (q0*gyro[1] - q1*gyro[2] + q3*gyro[0])
    qDot4 := 0.5 * (q0*gyro[2] + q1*gyro[1] - q2*gyro[0])
    
    // Apply feedback
    qDot1 -= f.beta * g0
    qDot2 -= f.beta * g1
    qDot3 -= f.beta * g2
    qDot4 -= f.beta * g3
    
    // Integrate
    f.q.W += qDot1 * dt
    f.q.X += qDot2 * dt
    f.q.Y += qDot3 * dt
    f.q.Z += qDot4 * dt
    f.q.Normalize()
    
    f.initialized = true
}

// GetQuaternion returns the current orientation quaternion
func (f *MadgwickFilter) GetQuaternion() Quaternion {
    return f.q
}

// GetEuler returns the current Euler angles in degrees
func (f *MadgwickFilter) GetEuler() EulerAngles {
    return f.q.ToEuler()
}

// GetAngularVelocity returns the estimated angular velocity
func (f *MadgwickFilter) GetAngularVelocity() [3]float64 {
    // Simplified - in real implementation, would compute from gyro and filter
    return [3]float64{0, 0, 0}
}

// Reset resets the filter to initial state
func (f *MadgwickFilter) Reset() {
    f.q = Identity()
    f.initialized = false
}

// IsInitialized returns true if filter has been updated at least once
func (f *MadgwickFilter) IsInitialized() bool {
    return f.initialized
}

// GetConfidence returns filter confidence (0-1)
func (f *MadgwickFilter) GetConfidence() float64 {
    if !f.initialized {
        return 0
    }
    // Confidence based on quaternion norm
    norm := f.q.Norm()
    if norm > 0.99 && norm < 1.01 {
        return 1.0
    }
    return math.Max(0, 1-math.Abs(1-norm))
}