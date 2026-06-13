// app/src/main/java/com/airmouse/presentation/ui/battery/BatteryUiState.kt
package com.airmouse.presentation.ui.battery

data class BatteryUiState(
    val level: Int = 0,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val capacity: Int = 0,
    val health: String = "Unknown",
    val status: String = "Unknown",
    val plugged: String = "Unknown",
    val technology: String = "Unknown",
    val history: List<BatteryHistoryEntry> = emptyList(),
    val estimatedRemaining: Long = 0,
    val isCharging: Boolean = false,
    val isPresent: Boolean = true,
    val powerSource: String = "Unknown",
    val chargeCounter: Int = 0,
    val energyCounter: Long = 0,
    val batterySaverEnabled: Boolean = false
)

data class BatteryHistoryEntry(
    val timestamp: Long,
    val level: Int,
    val temperature: Float,
    val isCharging: Boolean
)