// app/src/main/java/com/airmouse/presentation/ui/battery/BatteryUiState.kt
package com.airmouse.presentation.ui.battery

import androidx.compose.ui.graphics.vector.ImageVector

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
    val energyCounter: Long = 0,
    val isPresent: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    val capacity: Int = 100,
    val history: List<BatteryHistoryEntry> = emptyList()
)

data class BatteryHistoryEntry(
    val timestamp: Long,
    val level: Int,
    val temperature: Float,
    val isCharging: Boolean
)

data class BatteryAppUsage(
    val name: String,
    val usagePercent: Float,
    val icon: ImageVector? = null
)