package com.airmouse.presentation.ui.battery

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing the UI state of the battery screen
 */
data class BatteryUiState(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val health: String = "Unknown",
    val status: String = "Unknown",
    val technology: String = "Unknown",
    val plugged: String = "Unknown",
    val chargeCounter: Int = 0,
    val energyCounter: Int = 0,
    val isPresent: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    val capacity: Int = 100,
    val history: List<BatteryHistoryEntry> = emptyList()
)

/**
 * Data class representing a single battery history record
 */
data class BatteryHistoryEntry(
    val timestamp: Long,
    val level: Int,
    val temperature: Float,
    val isCharging: Boolean
)

/**
 * Data class representing battery usage by an application
 */
data class BatteryAppUsage(
    val name: String,
    val usagePercent: Float,
    val icon: ImageVector? = null
)

/**
 * Function to get color from battery level
 */
fun getBatteryColor(level: Int): Color {
    return when {
        level >= 70 -> Color(0xFF4CAF50)
        level >= 30 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

/**
 * Function to get color from health percent
 */
fun getHealthColor(percent: Int): Color {
    return when {
        percent >= 80 -> Color(0xFF4CAF50)
        percent >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

/**
 * Battery-related constants for use across the battery feature
 */
object BatteryConstants {
    object Thresholds {
        const val CRITICAL = 15
        const val LOW = 30
        const val FAIR = 50
        const val GOOD = 70
        const val EXCELLENT = 85
    }

    object Colors {
        val CRITICAL = Color(0xFFF44336)
        val LOW = Color(0xFFFFC107)
        val FAIR = Color(0xFFFF9800)
        val GOOD = Color(0xFF4CAF50)
        val EXCELLENT = Color(0xFF00C853)
        val CHARGING = Color(0xFF2196F3)
    }
}
