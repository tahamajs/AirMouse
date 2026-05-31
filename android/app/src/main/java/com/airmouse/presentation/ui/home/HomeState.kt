// app/src/main/java/com/airmouse/presentation/ui/home/HomeState.kt
package com.airmouse.presentation.ui.home

import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.UserPreferences

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
    val isTouchpadMode: Boolean = false,
    val isActive: Boolean = false,
    val aiSmoothingEnabled: Boolean = false,
    val predictiveEnabled: Boolean = false,
    val logMessages: List<String> = emptyList()
)

data class GestureStats(
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0
)