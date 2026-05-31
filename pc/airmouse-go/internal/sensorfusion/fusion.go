package sensorfusion

// EulerAngles represents the orientation in degrees (roll, pitch, yaw).
type EulerAngles struct {
    Roll  float64 // rotation around X axis (-180..180)
    Pitch float64 // rotation around Y axis (-90..90)
    Yaw   float64 // rotation around Z axis (0..360)
}

// Quaternion represents a rotation in 3D space.
type Quaternion struct {
    W, X, Y, Z float64
}

// Normalize ensures the quaternion has unit length.
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

// ToEuler converts a quaternion to Euler angles (degrees).
func (q *Quaternion) ToEuler() EulerAngles {
    // Roll (x-axis rotation)
    sinr := 2.0 * (q.W*q.X + q.Y*q.Z)
    cosr := 1.0 - 2.0*(q.X*q.X+q.Y*q.Y)
    roll := atan2(sinr, cosr)

    // Pitch (y-axis rotation)
    sinp := 2.0 * (q.W*q.Y - q.Z*q.X)
    var pitch float64
    if abs(sinp) >= 1 {
        pitch = copysign(math.Pi/2, sinp)
    } else {
        pitch = asin(sinp)
    }

    // Yaw (z-axis rotation)
    siny := 2.0 * (q.W*q.Z + q.X*q.Y)
    cosy := 1.0 - 2.0*(q.Y*q.Y+q.Z*q.Z)
    yaw := atan2(siny, cosy)

    return EulerAngles{
        Roll:  roll * 180 / math.Pi,
        Pitch: pitch * 180 / math.Pi,
        Yaw:   yaw * 180 / math.Pi,
    }
}