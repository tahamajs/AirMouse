package sensorfusion

import (
    "math"
    "testing"
)

func TestQuaternionBasics(t *testing.T) {
    // Test identity
    q := Identity()
    if q.W != 1 || q.X != 0 || q.Y != 0 || q.Z != 0 {
        t.Errorf("Identity quaternion incorrect: %v", q)
    }
    
    // Test normalization
    q = Quaternion{W: 2, X: 0, Y: 0, Z: 0}
    q.Normalize()
    if q.W != 1 {
        t.Errorf("Normalization failed: %v", q)
    }
    
    // Test multiplication
    q1 := Quaternion{W: 1, X: 0, Y: 0, Z: 0}
    q2 := Quaternion{W: 0, X: 1, Y: 0, Z: 0}
    q3 := q1.Multiply(q2)
    expected := Quaternion{W: 0, X: 1, Y: 0, Z: 0}
    if math.Abs(q3.W-expected.W) > 1e-6 ||
       math.Abs(q3.X-expected.X) > 1e-6 ||
       math.Abs(q3.Y-expected.Y) > 1e-6 ||
       math.Abs(q3.Z-expected.Z) > 1e-6 {
        t.Errorf("Multiplication failed: got %v, expected %v", q3, expected)
    }
    
    // Test Euler conversion
    euler := EulerAngles{Roll: 30, Pitch: 45, Yaw: 60}
    q := euler.ToQuaternion()
    euler2 := q.ToEuler()
    
    if math.Abs(euler.Roll-euler2.Roll) > 0.1 ||
       math.Abs(euler.Pitch-euler2.Pitch) > 0.1 ||
       math.Abs(euler.Yaw-euler2.Yaw) > 0.1 {
        t.Errorf("Euler conversion failed: original %v, got %v", euler, euler2)
    }
}

func TestMadgwickFilter(t *testing.T) {
    f := NewMadgwickFilter(100)
    
    // Test stationary with gravity only
    gyro := []float64{0, 0, 0}
    accel := []float64{0, 0, 9.81}
    
    for i := 0; i < 1000; i++ {
        f.Update(gyro, accel)
    }
    
    euler := f.GetEuler()
    if math.Abs(euler.Roll) > 1.0 || math.Abs(euler.Pitch) > 1.0 {
        t.Errorf("Stationary test failed: roll=%.2f, pitch=%.2f", euler.Roll, euler.Pitch)
    }
    
    // Test rotation
    gyro = []float64{0.1, 0, 0}
    for i := 0; i < 100; i++ {
        f.Update(gyro, accel)
    }
    
    euler = f.GetEuler()
    if euler.Roll < 5.0 {
        t.Errorf("Rotation test failed: roll=%.2f (should be > 5)", euler.Roll)
    }
    
    // Test confidence
    if !f.IsInitialized() {
        t.Error("Filter should be initialized")
    }
    if f.GetConfidence() < 0.9 {
        t.Errorf("Confidence too low: %.2f", f.GetConfidence())
    }
}

func TestMahonyFilter(t *testing.T) {
    f := NewMahonyFilter(100)
    f.SetGains(0.5, 0.1)
    
    // Test stationary
    gyro := []float64{0, 0, 0}
    accel := []float64{0, 0, 9.81}
    
    for i := 0; i < 1000; i++ {
        f.Update(gyro, accel)
    }
    
    euler := f.GetEuler()
    if math.Abs(euler.Roll) > 1.0 || math.Abs(euler.Pitch) > 1.0 {
        t.Errorf("Stationary test failed: roll=%.2f, pitch=%.2f", euler.Roll, euler.Pitch)
    }
    
    // Test with magnetometer
    mag := []float64{0.2, 0, 0.5}
    for i := 0; i < 100; i++ {
        f.UpdateWithMagnetometer(gyro, accel, mag)
    }
    
    euler = f.GetEuler()
    t.Logf("With magnetometer: %v", euler)
    
    // Test gains retrieval
    kp, ki := f.GetGains()
    if kp != 0.5 || ki != 0.1 {
        t.Errorf("Gain retrieval failed: kp=%.2f, ki=%.2f", kp, ki)
    }
}

func TestRotationVector(t *testing.T) {
    // Test axis-angle to quaternion conversion
    axisAngle := FromAxisAngle(0, 0, 1, math.Pi/2) // 90° around Z
    expected := Quaternion{W: 0.7071, X: 0, Y: 0, Z: 0.7071}
    
    if math.Abs(axisAngle.W-expected.W) > 0.001 ||
       math.Abs(axisAngle.X-expected.X) > 0.001 ||
       math.Abs(axisAngle.Y-expected.Y) > 0.001 ||
       math.Abs(axisAngle.Z-expected.Z) > 0.001 {
        t.Errorf("Axis-angle conversion failed: got %v, expected %v", axisAngle, expected)
    }
    
    // Test vector rotation
    q := FromEuler(0, 0, 90) // 90° around Z
    v := []float64{1, 0, 0}
    rotated := q.RotateVector(v)
    
    // Should be rotated to (0, 1, 0)
    if math.Abs(rotated[0]) > 0.001 || math.Abs(rotated[1]-1) > 0.001 {
        t.Errorf("Vector rotation failed: got %v, expected [0, 1, 0]", rotated)
    }
}

func TestSlerp(t *testing.T) {
    q1 := FromEuler(0, 0, 0)
    q2 := FromEuler(0, 0, 90)
    
    // Interpolate halfway
    qMid := Slerp(q1, q2, 0.5)
    euler := qMid.ToEuler()
    
    if math.Abs(euler.Yaw-45) > 1.0 {
        t.Errorf("Slerp interpolation failed: yaw=%.2f, expected 45", euler.Yaw)
    }
}

func BenchmarkMadgwick(b *testing.B) {
    f := NewMadgwickFilter(100)
    gyro := []float64{0.1, 0.05, 0.02}
    accel := []float64{0, 0, 9.81}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        f.Update(gyro, accel)
    }
}

func BenchmarkMahony(b *testing.B) {
    f := NewMahonyFilter(100)
    gyro := []float64{0.1, 0.05, 0.02}
    accel := []float64{0, 0, 9.81}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        f.Update(gyro, accel)
    }
}

func BenchmarkQuaternionMultiplication(b *testing.B) {
    q1 := Quaternion{W: 0.7071, X: 0, Y: 0, Z: 0.7071}
    q2 := Quaternion{W: 0.9239, X: 0, Y: 0, Z: 0.3827}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        _ = q1.Multiply(q2)
    }
}