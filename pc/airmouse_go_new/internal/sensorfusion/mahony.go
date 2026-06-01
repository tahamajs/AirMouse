package sensorfusion

import "math"

type MahonyFilter struct {
    q          Quaternion
    integralFB [3]float64
    kp, ki     float64
    sampleFreq float64
}

func NewMahonyFilter(sampleFreq float64) *MahonyFilter {
    return &MahonyFilter{
        q:          Quaternion{W: 1, X: 0, Y: 0, Z: 0},
        kp:         0.5,
        ki:         0.0,
        sampleFreq: sampleFreq,
    }
}

func (f *MahonyFilter) SetGains(kp, ki float64) { f.kp, f.ki = kp, ki }

func (f *MahonyFilter) Update(gyro, accel, mag []float64) {
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

    vx := 2*(q1*q3 - q0*q2)
    vy := 2*(q0*q1 + q2*q3)
    vz := q0*q0 - q1*q1 - q2*q2 + q3*q3

    ex := ay*vz - az*vy
    ey := az*vx - ax*vz
    ez := ax*vy - ay*vx

    if mag != nil && len(mag) >= 3 {
        normMag := math.Hypot(math.Hypot(mag[0], mag[1]), mag[2])
        if normMag > 0 {
            mx, my, mz := mag[0]/normMag, mag[1]/normMag, mag[2]/normMag
            hx := 2*(mx*(0.5-q2*q2-q3*q3) + my*(q1*q2-q0*q3) + mz*(q1*q3+q0*q2))
            hy := 2*(mx*(q1*q2+q0*q3) + my*(0.5-q1*q1-q3*q3) + mz*(q2*q3-q0*q1))
            hz := 2*(mx*(q1*q3-q0*q2) + my*(q2*q3+q0*q1) + mz*(0.5-q1*q1-q2*q2))
            bx := math.Sqrt(hx*hx + hy*hy)
            bz := hz
            wx := 2*(bx*(0.5-q2*q2-q3*q3) + bz*(q1*q3-q0*q2))
            wy := 2*(bx*(q1*q2-q0*q3) + bz*(q0*q1+q2*q3))
            wz := 2*(bx*(q0*q2+q1*q3) + bz*(0.5-q1*q1-q2*q2))
            ex += (my*wz - mz*wy)
            ey += (mz*wx - mx*wz)
            ez += (mx*wy - my*wx)
        }
    }

    if f.kp > 0 {
        ex *= f.kp
        ey *= f.kp
        ez *= f.kp
    }
    if f.ki > 0 {
        f.integralFB[0] += ex * f.ki * dt
        f.integralFB[1] += ey * f.ki * dt
        f.integralFB[2] += ez * f.ki * dt
        ex += f.integralFB[0]
        ey += f.integralFB[1]
        ez += f.integralFB[2]
    }

    gx := gyro[0] + ex
    gy := gyro[1] + ey
    gz := gyro[2] + ez

    qDot1 := 0.5 * (-q1*gx - q2*gy - q3*gz)
    qDot2 := 0.5 * (q0*gx + q2*gz - q3*gy)
    qDot3 := 0.5 * (q0*gy - q1*gz + q3*gx)
    qDot4 := 0.5 * (q0*gz + q1*gy - q2*gx)

    f.q.W += qDot1 * dt
    f.q.X += qDot2 * dt
    f.q.Y += qDot3 * dt
    f.q.Z += qDot4 * dt
    f.q.Normalize()
}

func (f *MahonyFilter) GetQuaternion() Quaternion { return f.q }
func (f *MahonyFilter) GetEuler() EulerAngles    { return f.q.ToEuler() }
func (f *MahonyFilter) Reset() {
    f.q = Quaternion{W: 1, X: 0, Y: 0, Z: 0}
    f.integralFB = [3]float64{0, 0, 0}
}