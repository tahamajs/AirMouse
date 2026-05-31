import "airmouse-go/internal/sensorfusion"

// Create a Madgwick filter at 100 Hz
filter := sensorfusion.NewMadgwickFilter(100.0)

// Inside your sensor data handler (e.g., from WebSocket)
func onIMUData(gyro, accel, mag []float64) {
    filter.Update(gyro, accel, mag)
    euler := filter.GetEuler()
    // Use euler.Roll, euler.Pitch, euler.Yaw for cursor movement
}