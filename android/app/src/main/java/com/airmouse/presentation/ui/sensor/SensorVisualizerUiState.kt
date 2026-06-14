package com.airmouse.presentation.ui.sensor

import androidx.compose.ui.graphics.Color

data class SensorVisualizerUiState(
    // Orientation (radians)
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    
    // Gyroscope (rad/s)
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    
    // Accelerometer (m/s²)
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    
    // Magnetometer (μT)
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    
    // Environmental sensors
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val light: Float = 0f,
    val proximity: Float = 0f,
    val humidity: Float = 0f,
    
    // Gravity sensor (m/s²)
    val gravityX: Float = 0f,
    val gravityY: Float = 0f,
    val gravityZ: Float = 0f,
    
    // Linear acceleration (m/s²)
    val linearAccelX: Float = 0f,
    val linearAccelY: Float = 0f,
    val linearAccelZ: Float = 0f,
    
    // Rotation vector
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    
    // Game rotation vector
    val gameRotationX: Float = 0f,
    val gameRotationY: Float = 0f,
    val gameRotationZ: Float = 0f,
    
    // Step counter
    val steps: Int = 0,
    val stepDetector: Boolean = false,
    
    // Heart rate (if available)
    val heartRate: Float = 0f,
    
    // Device orientation
    val deviceOrientation: DeviceOrientation = DeviceOrientation.PORTRAIT,
    
    // UI state
    val isSensorAvailable: Boolean = true,
    val activeSensor: ActiveSensor = ActiveSensor.ORIENTATION,
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val show3DModel: Boolean = true,
    val showRawData: Boolean = false,
    val sampleRate: Int = 0,
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val calibrationStatus: CalibrationStatus = CalibrationStatus.IDLE,
    val signalQuality: SignalQuality = SignalQuality.GOOD,
    
    // Colors
    val cubeColor: Color = Color(0xFFFF5722),
    val backgroundColor: Color = Color(0xFF1A1A2E),
    val accentColor: Color = Color(0xFF00BCD4)
)

enum class ActiveSensor(val displayName: String, val icon: String) {
    ORIENTATION("Orientation", "🔄"),
    GYROSCOPE("Gyroscope", "⚡"),
    ACCELEROMETER("Accelerometer", "📊"),
    MAGNETOMETER("Magnetometer", "🧭"),
    ENVIRONMENTAL("Environmental", "🌡️"),
    ALL("All Sensors", "📱")
}

enum class DeviceOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    REVERSE_PORTRAIT("Reverse Portrait"),
    REVERSE_LANDSCAPE("Reverse Landscape"),
    UNKNOWN("Unknown")
}

enum class CalibrationStatus(val displayName: String, val color: Color) {
    IDLE("Not Calibrated", Color(0xFF9E9E9E)),
    CALIBRATING("Calibrating...", Color(0xFFFFC107)),
    CALIBRATED("Calibrated", Color(0xFF4CAF50)),
    FAILED("Calibration Failed", Color(0xFFF44336))
}

enum class SignalQuality(val displayName: String, val color: Color, val level: Int) {
    EXCELLENT("Excellent", Color(0xFF4CAF50), 100),
    GOOD("Good", Color(0xFF8BC34A), 75),
    FAIR("Fair", Color(0xFFFFC107), 50),
    POOR("Poor", Color(0xFFFF9800), 25),
    NONE("No Signal", Color(0xFFF44336), 0)
}

data class SensorDataPoint(
    val timestamp: Long,
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
)

data class SensorHistory(
    val dataPoints: List<SensorDataPoint> = emptyList(),
    val maxHistorySize: Int = 300,
    val isRecording: Boolean = false,
    val startTime: Long = 0L,
    val recordingName: String = ""
) {
    val recordingDuration: Long get() = if (isRecording && startTime > 0) System.currentTimeMillis() - startTime else 0
    val sampleCount: Int get() = dataPoints.size
    val canExport: Boolean get() = dataPoints.isNotEmpty()
}

data class SensorStatistics(
    val minRoll: Float = 0f,
    val maxRoll: Float = 0f,
    val avgRoll: Float = 0f,
    val minPitch: Float = 0f,
    val maxPitch: Float = 0f,
    val avgPitch: Float = 0f,
    val minYaw: Float = 0f,
    val maxYaw: Float = 0f,
    val avgYaw: Float = 0f,
    val stabilityScore: Float = 0f,
    val movementIntensity: Float = 0f
)