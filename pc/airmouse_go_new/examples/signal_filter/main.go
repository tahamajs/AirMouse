// Example signal_filter demonstrates standalone usage of the OneEuroFilter, KalmanFilter2D,
// and MadgwickAHRS orientation sensor fusion filters without needing network connections.
package main

import (
	"fmt"
	"math"
	"math/rand"
	"time"

	"airmouse-go/pkg/filter"
)

func main() {
	fmt.Println("==================================================")
	fmt.Println("   AirMouse Signal Processing & Filters Demo      ")
	fmt.Println("==================================================")

	demoOneEuroFilter()
	demoKalmanFilter2D()
	demoMadgwickAHRS()
}

// demoOneEuroFilter demonstrates 1€ adaptive low-pass filtering.
func demoOneEuroFilter() {
	fmt.Println("\n--- 1. OneEuroFilter (1D & 2D Adaptive Smoothing) ---")
	// minCutoff = 1.0 Hz (jitter reduction at slow speeds)
	// beta = 0.05 (responsiveness at high speeds)
	// dCutoff = 1.0 Hz
	f1d := filter.NewOneEuroFilter(1.0, 0.05, 1.0)
	now := time.Now()

	fmt.Println("Simulating slow hand movement with sensor noise (Target = 10.0):")
	for i := 0; i < 5; i++ {
		t := now.Add(time.Duration(i*20) * time.Millisecond)
		noise := (rand.Float64() - 0.5) * 1.5
		raw := 10.0 + noise
		smoothed := f1d.Filter(raw, t)
		fmt.Printf("  Step %d: Raw=%.3f -> Smoothed=%.3f (Error=%.3f)\n", i, raw, smoothed, math.Abs(smoothed-10.0))
	}

	fmt.Println("\nSimulating high-speed flick (Rapid delta acceleration):")
	for i := 1; i <= 5; i++ {
		t := now.Add(time.Duration((5+i)*20) * time.Millisecond)
		raw := float64(i * 100) // fast jump
		smoothed := f1d.Filter(raw, t)
		fmt.Printf("  Flick Step %d: Raw=%.1f -> Smoothed=%.1f (Lag=%.2f px)\n", i, raw, smoothed, math.Abs(raw-smoothed))
	}

	// 2D Filter
	f2d := filter.NewOneEuroFilter2D(1.0, 0.05, 1.0)
	fx, fy := f2d.Filter(15.2, -8.7, time.Now())
	fmt.Printf("\nOneEuroFilter2D single-step output: fx=%.2f, fy=%.2f\n", fx, fy)
}

// demoKalmanFilter2D demonstrates constant-velocity state estimation & prediction.
func demoKalmanFilter2D() {
	fmt.Println("\n--- 2. KalmanFilter2D (State Estimation & Latency Compensation) ---")
	// dt = 0.02s (50 Hz motion updates)
	kf := filter.NewKalmanFilter2D(0.02)

	fmt.Println("Simulating continuous 2D cursor glide with measurement noise:")
	trueVx, trueVy := 12.0, -8.0 // Constant velocity: +12 px, -8 px per frame

	for step := 1; step <= 6; step++ {
		// 1. Advance state prediction
		kf.Predict()

		// 2. Add noisy sensor measurement
		noiseX := (rand.Float64() - 0.5) * 2.0
		noiseY := (rand.Float64() - 0.5) * 2.0
		measX := trueVx + noiseX
		measY := trueVy + noiseY

		// 3. Update filter with measurement
		kf.Update(measX, measY)

		// 4. Query estimated state & forward prediction
		predDx, predDy := kf.GetPredictedMovement()
		_, _, estVx, estVy := kf.GetState()
		confidence := kf.GetConfidence()

		fmt.Printf("  Step %d: Meas=(%5.1f, %5.1f) | EstVel=(%5.1f, %5.1f) | PredNext=(%4.1f, %4.1f) | Conf=%.2f\n",
			step, measX, measY, estVx, estVy, predDx, predDy, confidence)
	}
}

// demoMadgwickAHRS demonstrates IMU orientation quaternion & Euler angles.
func demoMadgwickAHRS() {
	fmt.Println("\n--- 3. MadgwickAHRS (6-DOF / 9-DOF Sensor Fusion) ---")
	// beta = 0.1
	ahrs := filter.NewMadgwickFilter(0.1)

	fmt.Println("Initial Orientation (Identity):")
	roll, pitch, yaw := ahrs.GetEulerAnglesDegrees()
	fmt.Printf("  Roll=%.2f°, Pitch=%.2f°, Yaw=%.2f°\n", roll, pitch, yaw)

	fmt.Println("\nSimulating sensor tilted 30° around X-axis (Roll = 30°):")
	dt := 0.01 // 100 Hz
	ax := 0.0
	ay := 9.81 * math.Sin(30.0*math.Pi/180.0) // 4.905 m/s²
	az := 9.81 * math.Cos(30.0*math.Pi/180.0) // 8.495 m/s²

	for i := 0; i < 150; i++ {
		ahrs.UpdateIMU(0, 0, 0, ax, ay, az, dt)
	}

	roll, pitch, yaw = ahrs.GetEulerAnglesDegrees()
	q0, q1, q2, q3 := ahrs.GetQuaternion()
	fmt.Printf("  Converged Orientation: Roll=%.2f°, Pitch=%.2f°, Yaw=%.2f°\n", roll, pitch, yaw)
	fmt.Printf("  Unit Quaternion: [w=%.4f, x=%.4f, y=%.4f, z=%.4f]\n", q0, q1, q2, q3)

	rot := ahrs.GetRotationMatrix()
	fmt.Printf("  Rotation Matrix (Top Row): [%.3f, %.3f, %.3f]\n", rot[0], rot[1], rot[2])
}
