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
    if math.Abs(q.W-1) > 1e-6 {
        t.Errorf("Normalization failed: expected W=1, got %v", q.W)
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
    // Confidence can vary, so just log it
    t.Logf("Madgwick confidence: %.2f", f.GetConfidence())
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
    t.Logf("Mahony with magnetometer: roll=%.2f, pitch=%.2f, yaw=%.2f", euler.Roll, euler.Pitch, euler.Yaw)
    
    // Test gains retrieval
    kp, ki := f.GetGains()
    if math.Abs(kp-0.5) > 1e-6 || math.Abs(ki-0.1) > 1e-6 {
        t.Errorf("Gain retrieval failed: kp=%.2f, ki=%.2f", kp, ki)
    }
}

func TestRotationVector(t *testing.T) {
    // Test axis-angle to quaternion conversion
    axisAngle := FromAxisAngle(0, 0, 1, math.Pi/2) // 90° around Z
    expected := Quaternion{W: 0.7071067811865476, X: 0, Y: 0, Z: 0.7071067811865476}
    
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
    
    // Should be rotated to approximately (0, 1, 0)
    if len(rotated) >= 3 {
        if math.Abs(rotated[0]) > 0.001 || math.Abs(rotated[1]-1) > 0.001 {
            t.Errorf("Vector rotation failed: got %v, expected approximately [0, 1, 0]", rotated)
        }
    } else {
        t.Errorf("Rotated vector length incorrect: %d", len(rotated))
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

func TestQuaternionConjugate(t *testing.T) {
    q := Quaternion{W: 0.7071, X: 0.5, Y: 0.2, Z: 0.1}
    conj := q.Conjugate()
    
    if conj.W != q.W || conj.X != -q.X || conj.Y != -q.Y || conj.Z != -q.Z {
        t.Errorf("Conjugate failed: got %v, expected (%v, -%v, -%v, -%v)",
            conj, q.W, q.X, q.Y, q.Z)
    }
}

func TestQuaternionNorm(t *testing.T) {
    q := Quaternion{W: 1, X: 2, Y: 3, Z: 4}
    norm := q.Norm()
    expected := math.Sqrt(1 + 4 + 9 + 16)
    
    if math.Abs(norm-expected) > 1e-6 {
        t.Errorf("Norm calculation failed: got %.2f, expected %.2f", norm, expected)
    }
}

func TestQuaternionIsUnit(t *testing.T) {
    q := Quaternion{W: 0.5, X: 0.5, Y: 0.5, Z: 0.5}
    q.Normalize()
    
    if !q.IsUnit(0.001) {
        t.Error("Normalized quaternion should be unit")
    }
}

func TestEulerNormalize(t *testing.T) {
    euler := EulerAngles{Roll: 370, Pitch: -185, Yaw: 450}
    normalized := euler.Normalize()
    
    if math.Abs(normalized.Roll-10) > 0.1 ||
        math.Abs(normalized.Pitch+5) > 0.1 ||
        math.Abs(normalized.Yaw-90) > 0.1 {
        t.Errorf("Normalization failed: got %v, expected approx (10, -5, 90)", normalized)
    }
}

func TestEulerClamp(t *testing.T) {
    euler := EulerAngles{Roll: 100, Pitch: 100, Yaw: 400}
    clamped := euler.Clamp()
    
    if clamped.Roll > 90 || clamped.Roll < -90 {
        t.Errorf("Roll not clamped: %.2f", clamped.Roll)
    }
    if clamped.Pitch > 90 || clamped.Pitch < -90 {
        t.Errorf("Pitch not clamped: %.2f", clamped.Pitch)
    }
}

func TestQuaternionToRotationMatrix(t *testing.T) {
    q := FromEuler(30, 45, 60)
    matrix := q.ToRotationMatrix()
    
    // Check matrix dimensions
    if len(matrix) != 9 {
        t.Errorf("Rotation matrix should have 9 elements, got %d", len(matrix))
    }
    
    // Check that matrix is orthonormal (determinant should be ~1)
    det := matrix[0]*(matrix[4]*matrix[8]-matrix[5]*matrix[7]) -
        matrix[1]*(matrix[3]*matrix[8]-matrix[5]*matrix[6]) +
        matrix[2]*(matrix[3]*matrix[7]-matrix[4]*matrix[6])
    
    if math.Abs(det-1) > 0.01 {
        t.Errorf("Rotation matrix determinant should be ~1, got %.4f", det)
    }
}

func TestFromAxisAngle(t *testing.T) {
    // Test rotation around X axis by 90 degrees
    q := FromAxisAngle(1, 0, 0, math.Pi/2)
    euler := q.ToEuler()
    
    if math.Abs(euler.Roll-90) > 1.0 {
        t.Errorf("X-axis rotation failed: roll=%.2f, expected ~90", euler.Roll)
    }
    
    // Test rotation around Y axis by 90 degrees
    q = FromAxisAngle(0, 1, 0, math.Pi/2)
    euler = q.ToEuler()
    
    if math.Abs(euler.Pitch-90) > 1.0 {
        t.Errorf("Y-axis rotation failed: pitch=%.2f, expected ~90", euler.Pitch)
    }
    
    // Test rotation around Z axis by 90 degrees
    q = FromAxisAngle(0, 0, 1, math.Pi/2)
    euler = q.ToEuler()
    
    if math.Abs(euler.Yaw-90) > 1.0 {
        t.Errorf("Z-axis rotation failed: yaw=%.2f, expected ~90", euler.Yaw)
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
    q1 := Quaternion{W: 0.7071067811865476, X: 0, Y: 0, Z: 0.7071067811865476}
    q2 := Quaternion{W: 0.9238795325112867, X: 0, Y: 0, Z: 0.3826834323650898}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        _ = q1.Multiply(q2)
    }
}

func BenchmarkQuaternionToEuler(b *testing.B) {
    q := Quaternion{W: 0.7071, X: 0.5, Y: 0.3, Z: 0.4}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        _ = q.ToEuler()
    }
}

func BenchmarkEulerToQuaternion(b *testing.B) {
    euler := EulerAngles{Roll: 30, Pitch: 45, Yaw: 60}
    
    b.ResetTimer()
    for i := 0; i < b.N; i++ {
        _ = euler.ToQuaternion()
    }
}
