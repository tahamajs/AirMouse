package sensorfusion

import (
	"math"
	"testing"
)

func TestMadgwick(t *testing.T) {
	f := NewMadgwickFilter(100)
	gyro := []float64{0.1, 0, 0}
	accel := []float64{0, 0, 9.81}
	for i := 0; i < 1000; i++ {
		f.Update(gyro, accel, nil)
	}
	e := f.GetEuler()
	if math.Abs(e.Roll) < 0.1 {
		t.Errorf("Roll not changed: %.2f", e.Roll)
	}
}

func TestMahony(t *testing.T) {
	f := NewMahonyFilter(100)
	gyro := []float64{0, 0.1, 0}
	accel := []float64{0, 0, 9.81}
	for i := 0; i < 1000; i++ {
		f.Update(gyro, accel, nil)
	}
	e := f.GetEuler()
	if math.Abs(e.Pitch) < 0.1 {
		t.Errorf("Pitch not changed: %.2f", e.Pitch)
	}
}

func TestQuaternion(t *testing.T) {
	q := Quaternion{W: 1, X: 0, Y: 0, Z: 0}
	e := q.ToEuler()
	if e.Roll != 0 || e.Pitch != 0 || e.Yaw != 0 {
		t.Errorf("Identity quaternion gave non-zero Euler: %+v", e)
	}
}
