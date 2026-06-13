package com.airmouse.presentation.ui.home

import com.airmouse.domain.model.ConnectionStatus

data class HomeUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val serverIp: String = "",
    val serverPort: Int = 8080,
    val calibrationProgress: Int = 0,
    val sensorsCalibrated: Int = 0,
    val totalSensors: Int = 3,
    val remainingAttempts: Int = 5,
    val gestureStats: GestureStats = GestureStats(),
    val orientationYaw: Float = 0f,
    val orientationPitch: Float = 0f,
    val orientationRoll: Float = 0f,
    val controlMode: String = "motion", // motion, touchpad, arm_movement
    val isActive: Boolean = false,
    val aiSmoothingEnabled: Boolean = false,
    val predictiveEnabled: Boolean = false,
    val logMessages: List<String> = emptyList(),
    val batteryLevel: Int = 100,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN,
    val lastGesture: String = "",
    val isCalibrated: Boolean = false,
    val isConnecting: Boolean = false
)

data class GestureStats(
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gesturesDetected: Int = 0
)

enum class ConnectionQuality(val color: Long, val text: String) {
    EXCELLENT(0xFF4CAF50, "Excellent"),
    GOOD(0xFF8BC34A, "Good"),
    FAIR(0xFFFFC107, "Fair"),
    POOR(0xFFFF5722, "Poor"),
    UNKNOWN(0xFF9E9E9E, "Unknown")
}