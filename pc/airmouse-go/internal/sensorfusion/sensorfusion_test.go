package sensorfusion

import (
    "testing"
    "math"
)

func TestMadgwickFilter(t *testing.T) {
    f := NewMadgwickFilter(100) // 100 Hz
    // Simulate gyro only (should drift over time)
    gyro := []float64{0.1, 0, 0} // 0.1 rad/s roll
    accel := []float64{0, 0, 9.81}
    for i := 0; i < 1000; i++ {
        f.Update(gyro, accel, nil)
    }
    euler := f.GetEuler()
    if math.Abs(euler.Roll) < 0.1 {
        t.Errorf("Roll should have changed significantly: got %.2f", euler.Roll)
    }
}

func TestMahonyFilter(t *testing.T) {
    f := NewMahonyFilter(100)
    f.SetGains(0.5, 0.0)
    gyro := []float64{0, 0.1, 0}
    accel := []float64{0, 0, 9.81}
    for i := 0; i < 1000; i++ {
        f.Update(gyro, accel, nil)
    }
    euler := f.GetEuler()
    if math.Abs(euler.Pitch) < 0.1 {
        t.Errorf("Pitch should have changed: got %.2f", euler.Pitch)
    }
}

func TestQuaternionToEuler(t *testing.T) {
    q := Quaternion{W: 1, X: 0, Y: 0, Z: 0}
    e := q.ToEuler()
    if e.Roll != 0 || e.Pitch != 0 || e.Yaw != 0 {
        t.Errorf("Identity quaternion gave non-zero Euler: %+v", e)
    }
}