package com.airmouse.presentation.ui.proximity

data class ProximityUiState(
    val isEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val currentDistance: Float? = null,
    val isNear: Boolean = false,
    val status: String = "Service stopped",
    val statusColor: Long = 0xFF9E9E9E,
    val isCalibrating: Boolean = false,
    val calibrationProgress: Int = 0,
    val calibrationStatus: String = "",
    val connectedDevice: String? = null,
    val deviceMac: String = "",
    val rssi: Int = -100,
    val signalStrength: SignalStrength = SignalStrength.NONE,
    val history: List<ProximityHistoryEntry> = emptyList(),
    val errorMessage: String? = null,
    val lockActionEnabled: Boolean = true,
    val unlockActionEnabled: Boolean = true,
    val lockScreenTimeout: Int = 0,
    val vibrationOnLock: Boolean = true,
    val notificationOnLock: Boolean = true
)

enum class SignalStrength(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF4CAF50),
    GOOD("Good", 0xFF8BC34A),
    FAIR("Fair", 0xFFFFC107),
    POOR("Poor", 0xFFFF5722),
    NONE("None", 0xFF9E9E9E)
}

data class ProximityHistoryEntry(
    val timestamp: Long,
    val distance: Float,
    val isNear: Boolean,
    val rssi: Int
)