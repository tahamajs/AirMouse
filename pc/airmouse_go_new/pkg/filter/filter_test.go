package filter

import (
	"math"
	"math/rand"
	"testing"
	"time"
)

func TestOneEuroFilterLowSpeedJitter(t *testing.T) {
	filter := NewOneEuroFilter(1.0, 0.007, 1.0)
	var filteredValues []float64

	now := time.Now()
	for i := 0; i < 50; i++ {
		// Stationary signal with +/- 0.5 white noise
		noise := (rand.Float64() - 0.5) * 1.0
		val := 10.0 + noise
		tNow := now.Add(time.Duration(i*20) * time.Millisecond)
		out := filter.Filter(val, tNow)
		filteredValues = append(filteredValues, out)
	}

	// Calculate average variance from true target 10.0 after convergence
	var totalErr float64
	for i := 20; i < 50; i++ {
		totalErr += math.Abs(filteredValues[i] - 10.0)
	}
	avgErr := totalErr / 30.0
	t.Logf("OneEuro low speed avg error: %.4f", avgErr)

	if avgErr > 0.4 {
		t.Errorf("OneEuro filter did not attenuate low-speed jitter sufficiently: avgErr = %f", avgErr)
	}
}

func TestOneEuroFilterHighSpeedResponsiveness(t *testing.T) {
	filter := NewOneEuroFilter(1.0, 0.1, 1.0)
	now := time.Now()

	// High speed flick (from 0 to 1500 px/s)
	var lastOut float64
	for i := 0; i < 30; i++ {
		val := float64(i * 50)
		tNow := now.Add(time.Duration(i*20) * time.Millisecond)
		lastOut = filter.Filter(val, tNow)
	}

	target := 29.0 * 50.0
	lag := math.Abs(target - lastOut)
	t.Logf("High speed target: %.1f, filtered: %.1f, lag: %.1f", target, lastOut, lag)

	if lag > 50.0 {
		t.Errorf("OneEuro filter lagged excessively at high speed: lag = %f", lag)
	}
}

func TestOneEuroFilter2D(t *testing.T) {
	f2d := NewOneEuroFilter2D(1.0, 0.007, 1.0)
	now := time.Now()

	for i := 0; i < 20; i++ {
		tNow := now.Add(time.Duration(i*20) * time.Millisecond)
		fx, fy := f2d.Filter(float64(i*2), float64(i*3), tNow)
		if i == 19 {
			if fx == 0 && fy == 0 {
				t.Errorf("Filter2D produced zero output")
			}
		}
	}

	f2d.Reset()
}

func TestKalmanFilter2D(t *testing.T) {
	kf := NewKalmanFilter2D(0.02)
	if kf == nil {
		t.Fatal("Failed to create KalmanFilter2D")
	}

	for i := 0; i < 20; i++ {
		kf.Predict()
		kf.Update(10.0, 5.0)
		predX, predY := kf.GetPredictedMovement()
		if i == 19 {
			t.Logf("Kalman Step 19 predicted: (%.2f, %.2f)", predX, predY)
			if predX == 0 && predY == 0 {
				t.Errorf("KalmanFilter2D predicted movement is zero")
			}
		}
	}

	x, y, vx, vy := kf.GetState()
	t.Logf("Final Kalman state: x=%.2f, y=%.2f, vx=%.2f, vy=%.2f", x, y, vx, vy)

	conf := kf.GetConfidence()
	if conf < 0 || conf > 1.0 {
		t.Errorf("Invalid confidence metric: %f", conf)
	}

	kf.Reset()
	rx, ry, rvx, rvy := kf.GetState()
	if rx != 0 || ry != 0 || rvx != 0 || rvy != 0 {
		t.Errorf("Reset failed, expected zeros, got (%f, %f, %f, %f)", rx, ry, rvx, rvy)
	}
}

func TestMadgwickAHRS_IMU(t *testing.T) {
	ahrs := NewMadgwickFilter(0.1)

	// Level stationary sensor: Gravity pointing down on Z (0, 0, 9.81)
	dt := 0.01 // 100 Hz
	for i := 0; i < 200; i++ {
		ahrs.UpdateIMU(0, 0, 0, 0, 0, 9.81, dt)
	}

	roll, pitch, yaw := ahrs.GetEulerAnglesDegrees()
	t.Logf("IMU Stationary Level: Roll=%.2f°, Pitch=%.2f°, Yaw=%.2f°", roll, pitch, yaw)

	if math.Abs(roll) > 2.0 {
		t.Errorf("Expected roll near 0, got %.2f°", roll)
	}
	if math.Abs(pitch) > 2.0 {
		t.Errorf("Expected pitch near 0, got %.2f°", pitch)
	}

	q0, q1, q2, q3 := ahrs.GetQuaternion()
	norm := math.Sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3)
	if math.Abs(norm-1.0) > 1e-3 {
		t.Errorf("Quaternion not normalized: norm=%f", norm)
	}

	rotMat := ahrs.GetRotationMatrix()
	if len(rotMat) != 9 {
		t.Fatalf("Invalid rotation matrix length")
	}
}

func TestMadgwickAHRS_9DOF(t *testing.T) {
	ahrs := NewMadgwickAHRS(0.1, 0.0)

	// Level sensor pointing North
	// Accel: (0, 0, 9.81)
	// Mag: (20.0, 0.0, -40.0)
	dt := 0.02 // 50 Hz
	for i := 0; i < 200; i++ {
		ahrs.Update(0, 0, 0, 0, 0, 9.81, 20.0, 0.0, -40.0, dt)
	}

	angles := ahrs.GetEulerAngles()
	rollDeg := angles.RollDegrees()
	pitchDeg := angles.PitchDegrees()
	yawDeg := angles.YawDegrees()

	t.Logf("9-DOF Level: Roll=%.2f°, Pitch=%.2f°, Yaw=%.2f°", rollDeg, pitchDeg, yawDeg)

	if math.Abs(rollDeg) > 5.0 {
		t.Errorf("Expected roll near 0, got %.2f°", rollDeg)
	}
	if math.Abs(pitchDeg) > 5.0 {
		t.Errorf("Expected pitch near 0, got %.2f°", pitchDeg)
	}
}

func TestMadgwickAHRS_ResetWithSensors(t *testing.T) {
	ahrs := NewMadgwickFilter(0.1)

	// Sensor tilted 45 degrees around X axis: ay = 6.936, az = 6.936
	ax := 0.0
	ay := 9.81 * math.Sin(math.Pi/4.0)
	az := 9.81 * math.Cos(math.Pi/4.0)
	mx, my, mz := 20.0, 0.0, -40.0

	ahrs.ResetWithSensors(ax, ay, az, mx, my, mz)
	roll, _, _ := ahrs.GetEulerAnglesDegrees()

	t.Logf("Direct Alignment Roll: %.2f° (expected ~45.0°)", roll)
	if math.Abs(roll-45.0) > 2.0 {
		t.Errorf("ResetWithSensors failed, expected ~45° roll, got %.2f°", roll)
	}
}

func BenchmarkMadgwickUpdateIMU(b *testing.B) {
	ahrs := NewMadgwickFilter(0.1)
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		ahrs.UpdateIMU(0.01, -0.02, 0.005, 0.1, 0.2, 9.81, 0.01)
	}
}

func BenchmarkKalmanFilter2DUpdate(b *testing.B) {
	kf := NewKalmanFilter2D(0.02)
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		kf.Predict()
		kf.Update(1.5, -0.8)
	}
}

func BenchmarkOneEuroFilter(b *testing.B) {
	f := NewOneEuroFilter(1.0, 0.007, 1.0)
	now := time.Now()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		f.Filter(float64(i%100), now)
	}
}
