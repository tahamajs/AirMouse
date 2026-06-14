package sensorfusion

import (
    "fmt"
    "math"
)

// EulerAngles represents orientation in degrees
type EulerAngles struct {
    Roll, Pitch, Yaw float64 // in degrees
}

// NewEulerAngles creates new Euler angles
func NewEulerAngles(roll, pitch, yaw float64) EulerAngles {
    return EulerAngles{Roll: roll, Pitch: pitch, Yaw: yaw}
}

// Zero returns zero Euler angles
func ZeroEuler() EulerAngles {
    return EulerAngles{Roll: 0, Pitch: 0, Yaw: 0}
}

// ToRadians converts Euler angles to radians
func (e EulerAngles) ToRadians() EulerAngles {
    return EulerAngles{
        Roll:  e.Roll * math.Pi / 180,
        Pitch: e.Pitch * math.Pi / 180,
        Yaw:   e.Yaw * math.Pi / 180,
    }
}

// ToDegrees converts Euler angles to degrees
func (e EulerAngles) ToDegrees() EulerAngles {
    return EulerAngles{
        Roll:  e.Roll * 180 / math.Pi,
        Pitch: e.Pitch * 180 / math.Pi,
        Yaw:   e.Yaw * 180 / math.Pi,
    }
}

// Normalize normalizes angles to [-180, 180] range
func (e EulerAngles) Normalize() EulerAngles {
    normalize := func(angle float64) float64 {
        angle = math.Mod(angle, 360)
        if angle > 180 {
            angle -= 360
        }
        if angle < -180 {
            angle += 360
        }
        return angle
    }
    
    return EulerAngles{
        Roll:  normalize(e.Roll),
        Pitch: normalize(e.Pitch),
        Yaw:   normalize(e.Yaw),
    }
}

// Clamp clamps angles to specified ranges
func (e EulerAngles) Clamp() EulerAngles {
    return EulerAngles{
        Roll:  math.Max(-90, math.Min(90, e.Roll)),
        Pitch: math.Max(-90, math.Min(90, e.Pitch)),
        Yaw:   e.Yaw, // Yaw can be full range
    }
}

// IsValid checks if Euler angles are within reasonable ranges
func (e EulerAngles) IsValid() bool {
    return !math.IsNaN(e.Roll) && !math.IsNaN(e.Pitch) && !math.IsNaN(e.Yaw) &&
        math.Abs(e.Roll) <= 180 && math.Abs(e.Pitch) <= 180 && math.Abs(e.Yaw) <= 360
}

// ToQuaternion converts Euler angles to quaternion
func (e EulerAngles) ToQuaternion() Quaternion {
    return FromEuler(e.Roll, e.Pitch, e.Yaw)
}

// Difference returns the angular difference between two Euler angle sets
func (e EulerAngles) Difference(other EulerAngles) EulerAngles {
    return EulerAngles{
        Roll:  math.Abs(e.Roll - other.Roll),
        Pitch: math.Abs(e.Pitch - other.Pitch),
        Yaw:   math.Abs(e.Yaw - other.Yaw),
    }.Normalize()
}

// String returns string representation
func (e EulerAngles) String() string {
    return fmt.Sprintf("Euler(roll=%.2f°, pitch=%.2f°, yaw=%.2f°)", e.Roll, e.Pitch, e.Yaw)
}