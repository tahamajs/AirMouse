package sensorfusion

import (
    "fmt"
    "time"
)

// ExampleMadgwickUsage demonstrates how to use the Madgwick filter
func ExampleMadgwickUsage() {
    // Create filter with 100 Hz sample rate
    filter := NewMadgwickFilter(100)
    filter.SetBeta(0.1)
    
    // Simulate sensor data
    gyro := []float64{0.01, 0.005, 0.002}  // rad/s
    accel := []float64{0.1, 0.05, 9.81}    // m/s²
    
    // Update filter
    filter.Update(gyro, accel)
    
    // Get orientation
    quat := filter.GetQuaternion()
    euler := filter.GetEuler()
    
    fmt.Printf("Quaternion: %v\n", quat)
    fmt.Printf("Euler: roll=%.2f°, pitch=%.2f°, yaw=%.2f°\n", 
        euler.Roll, euler.Pitch, euler.Yaw)
}

// ExampleMahonyUsage demonstrates how to use the Mahony filter
func ExampleMahonyUsage() {
    filter := NewMahonyFilter(100)
    filter.SetGains(0.5, 0.1)
    
    gyro := []float64{0.01, 0.005, 0.002}
    accel := []float64{0.1, 0.05, 9.81}
    mag := []float64{0.2, 0.1, 0.5}
    
    filter.UpdateWithMagnetometer(gyro, accel, mag)
    euler := filter.GetEuler()
    
    fmt.Printf("Mahony filter output: roll=%.2f°, pitch=%.2f°, yaw=%.2f°\n",
        euler.Roll, euler.Pitch, euler.Yaw)
}

// RealTimeOrientationTracking demonstrates real-time tracking
func RealTimeOrientationTracking() {
    filter := NewMadgwickFilter(100)
    
    // Simulate real-time data stream
    ticker := time.NewTicker(10 * time.Millisecond)
    defer ticker.Stop()
    
    for range ticker {
        // In real application, read from IMU sensor
        gyro := []float64{0.01, 0.005, 0.002}
        accel := []float64{0.1, 0.05, 9.81}
        
        filter.Update(gyro, accel)
        euler := filter.GetEuler()
        
        fmt.Printf("\rRoll: %6.2f° Pitch: %6.2f° Yaw: %6.2f°", 
            euler.Roll, euler.Pitch, euler.Yaw)
    }
}