package sensorfusion

import (
    "fmt"
    "math"
)

// Quaternion represents a rotation in 3D space
type Quaternion struct {
    W, X, Y, Z float64 // W is scalar, (X,Y,Z) is vector
}

// NewQuaternion creates a new quaternion from components
func NewQuaternion(w, x, y, z float64) Quaternion {
    return Quaternion{W: w, X: x, Y: y, Z: z}
}

// Identity returns the identity quaternion (no rotation)
func Identity() Quaternion {
    return Quaternion{W: 1, X: 0, Y: 0, Z: 0}
}

// Normalize normalizes the quaternion to unit length
func (q *Quaternion) Normalize() {
    norm := q.W*q.W + q.X*q.X + q.Y*q.Y + q.Z*q.Z
    if norm == 0 {
        q.W = 1
        return
    }
    invNorm := 1.0 / math.Sqrt(norm)
    q.W *= invNorm
    q.X *= invNorm
    q.Y *= invNorm
    q.Z *= invNorm
}

// Normalized returns a normalized copy of the quaternion
func (q Quaternion) Normalized() Quaternion {
    result := q
    result.Normalize()
    return result
}

// Conjugate returns the conjugate of the quaternion
func (q Quaternion) Conjugate() Quaternion {
    return Quaternion{W: q.W, X: -q.X, Y: -q.Y, Z: -q.Z}
}

// Multiply multiplies two quaternions (q * r)
func (q Quaternion) Multiply(r Quaternion) Quaternion {
    return Quaternion{
        W: q.W*r.W - q.X*r.X - q.Y*r.Y - q.Z*r.Z,
        X: q.W*r.X + q.X*r.W + q.Y*r.Z - q.Z*r.Y,
        Y: q.W*r.Y - q.X*r.Z + q.Y*r.W + q.Z*r.X,
        Z: q.W*r.Z + q.X*r.Y - q.Y*r.X + q.Z*r.W,
    }
}

// Scale scales the quaternion by a factor
func (q Quaternion) Scale(factor float64) Quaternion {
    return Quaternion{
        W: q.W * factor,
        X: q.X * factor,
        Y: q.Y * factor,
        Z: q.Z * factor,
    }
}

// Add adds two quaternions
func (q Quaternion) Add(r Quaternion) Quaternion {
    return Quaternion{
        W: q.W + r.W,
        X: q.X + r.X,
        Y: q.Y + r.Y,
        Z: q.Z + r.Z,
    }
}

// Subtract subtracts two quaternions
func (q Quaternion) Subtract(r Quaternion) Quaternion {
    return Quaternion{
        W: q.W - r.W,
        X: q.X - r.X,
        Y: q.Y - r.Y,
        Z: q.Z - r.Z,
    }
}

// Dot returns the dot product of two quaternions
func (q Quaternion) Dot(r Quaternion) float64 {
    return q.W*r.W + q.X*r.X + q.Y*r.Y + q.Z*r.Z
}

// Norm returns the magnitude of the quaternion
func (q Quaternion) Norm() float64 {
    return math.Sqrt(q.W*q.W + q.X*q.X + q.Y*q.Y + q.Z*q.Z)
}

// IsUnit returns true if the quaternion is unit length (within tolerance)
func (q Quaternion) IsUnit(tolerance float64) bool {
    return math.Abs(q.Norm()-1.0) < tolerance
}

// Slerp performs spherical linear interpolation between two quaternions
func Slerp(q1, q2 Quaternion, t float64) Quaternion {
    // Calculate angle between quaternions
    dot := q1.Dot(q2)
    
    // If dot is negative, invert one quaternion to take shorter path
    if dot < 0 {
        q2 = q2.Scale(-1)
        dot = -dot
    }
    
    // If quaternions are very close, use linear interpolation
    if dot > 0.9995 {
        result := q1.Scale(1 - t).Add(q2.Scale(t))
        result.Normalize()
        return result
    }
    
    // Calculate angle and interpolation factors
    theta := math.Acos(dot)
    sinTheta := math.Sin(theta)
    factor1 := math.Sin((1-t)*theta) / sinTheta
    factor2 := math.Sin(t*theta) / sinTheta
    
    result := q1.Scale(factor1).Add(q2.Scale(factor2))
    result.Normalize()
    return result
}

// ToEuler converts quaternion to Euler angles (roll, pitch, yaw) in degrees
func (q Quaternion) ToEuler() EulerAngles {
    // Roll (x-axis rotation)
    sinr := 2.0 * (q.W*q.X + q.Y*q.Z)
    cosr := 1.0 - 2.0*(q.X*q.X+q.Y*q.Y)
    roll := math.Atan2(sinr, cosr)
    
    // Pitch (y-axis rotation)
    sinp := 2.0 * (q.W*q.Y - q.Z*q.X)
    var pitch float64
    if math.Abs(sinp) >= 1 {
        pitch = math.Copysign(math.Pi/2, sinp)
    } else {
        pitch = math.Asin(sinp)
    }
    
    // Yaw (z-axis rotation)
    siny := 2.0 * (q.W*q.Z + q.X*q.Y)
    cosy := 1.0 - 2.0*(q.Y*q.Y+q.Z*q.Z)
    yaw := math.Atan2(siny, cosy)
    
    return EulerAngles{
        Roll:  roll * 180 / math.Pi,
        Pitch: pitch * 180 / math.Pi,
        Yaw:   yaw * 180 / math.Pi,
    }
}

// FromEuler creates a quaternion from Euler angles (roll, pitch, yaw) in degrees
func FromEuler(roll, pitch, yaw float64) Quaternion {
    // Convert to radians
    rollRad := roll * math.Pi / 180
    pitchRad := pitch * math.Pi / 180
    yawRad := yaw * math.Pi / 180
    
    // Calculate half angles
    cr := math.Cos(rollRad * 0.5)
    sr := math.Sin(rollRad * 0.5)
    cp := math.Cos(pitchRad * 0.5)
    sp := math.Sin(pitchRad * 0.5)
    cy := math.Cos(yawRad * 0.5)
    sy := math.Sin(yawRad * 0.5)
    
    return Quaternion{
        W: cr*cp*cy + sr*sp*sy,
        X: sr*cp*cy - cr*sp*sy,
        Y: cr*sp*cy + sr*cp*sy,
        Z: cr*cp*sy - sr*sp*cy,
    }
}

// FromAxisAngle creates a quaternion from axis and angle (angle in radians)
func FromAxisAngle(x, y, z, angle float64) Quaternion {
    // Normalize axis
    norm := math.Sqrt(x*x + y*y + z*z)
    if norm == 0 {
        return Identity()
    }
    x /= norm
    y /= norm
    z /= norm
    
    halfAngle := angle * 0.5
    s := math.Sin(halfAngle)
    return Quaternion{
        W: math.Cos(halfAngle),
        X: x * s,
        Y: y * s,
        Z: z * s,
    }
}

// RotateVector rotates a 3D vector by the quaternion
func (q Quaternion) RotateVector(v []float64) []float64 {
    if len(v) < 3 {
        return v
    }
    
    // Convert vector to quaternion
    vecQ := Quaternion{W: 0, X: v[0], Y: v[1], Z: v[2]}
    
    // Rotate: q * v * q_conj
    result := q.Multiply(vecQ).Multiply(q.Conjugate())
    
    return []float64{result.X, result.Y, result.Z}
}

// ToRotationMatrix converts quaternion to 3x3 rotation matrix
func (q Quaternion) ToRotationMatrix() [9]float64 {
    xx := q.X * q.X
    yy := q.Y * q.Y
    zz := q.Z * q.Z
    xy := q.X * q.Y
    xz := q.X * q.Z
    yz := q.Y * q.Z
    wx := q.W * q.X
    wy := q.W * q.Y
    wz := q.W * q.Z
    
    return [9]float64{
        1 - 2*(yy+zz), 2*(xy-wz), 2*(xz+wy),
        2*(xy+wz), 1 - 2*(xx+zz), 2*(yz-wx),
        2*(xz-wy), 2*(yz+wx), 1 - 2*(xx+yy),
    }
}

// String returns a string representation
func (q Quaternion) String() string {
    return fmt.Sprintf("Quaternion(%.4f, %.4f, %.4f, %.4f)", q.W, q.X, q.Y, q.Z)
}