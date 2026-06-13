package com.airmouse.presentation.ui.sensor

data class SensorVisualizerUiState(
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val light: Float = 0f,
    val proximity: Float = 0f,
    val isSensorAvailable: Boolean = true,
    val activeSensor: ActiveSensor = ActiveSensor.ORIENTATION,
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val cubeColor: Long = 0xFFFF5722,
    val backgroundColor: Long = 0xFF1A1A2E
)

enum class ActiveSensor(val displayName: String) {
    ORIENTATION("Orientation"),
    GYROSCOPE("Gyroscope"),
    ACCELEROMETER("Accelerometer"),
    MAGNETOMETER("Magnetometer"),
    ALL("All Sensors")
}package com.airmouse.presentation.ui.sensor

data class SensorVisualizerUiState(
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val temperature: Float = 0f,
    val pressure: Float = 0f,
    val light: Float = 0f,
    val proximity: Float = 0f,
    val isSensorAvailable: Boolean = true,
    val activeSensor: ActiveSensor = ActiveSensor.ORIENTATION,
    val showAxes: Boolean = true,
    val showGrid: Boolean = true,
    val cubeColor: Long = 0xFFFF5722,
    val backgroundColor: Long = 0xFF1A1A2E
)

enum class ActiveSensor(val displayName: String) {
    ORIENTATION("Orientation"),
    GYROSCOPE("Gyroscope"),
    ACCELEROMETER("Accelerometer"),
    MAGNETOMETER("Magnetometer"),
    ALL("All Sensors")
}