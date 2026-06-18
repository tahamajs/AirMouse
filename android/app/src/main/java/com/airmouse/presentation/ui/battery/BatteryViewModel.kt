package com.airmouse.presentation.ui.battery

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BatteryViewModel - Manages battery state and monitoring
 *
 * Features:
 * - Real-time battery level monitoring (every 5 seconds)
 * - Battery health, temperature, voltage tracking
 * - Charging state detection
 * - Battery saver mode detection
 * - History of battery levels (last 50 entries)
 * - Top battery consuming apps
 * - Navigation to system battery settings
 */
@HiltViewModel
class BatteryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ==================== UI State ====================

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private val _topApps = MutableStateFlow<List<BatteryAppUsage>>(emptyList())
    val topApps: StateFlow<List<BatteryAppUsage>> = _topApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ==================== Initialization ====================

    init {
        refreshBatteryInfo()
        startBatteryMonitoring()
    }

    // ==================== Monitoring ====================

    /**
     * Start continuous battery monitoring (updates every 5 seconds)
     */
    private fun startBatteryMonitoring() {
        viewModelScope.launch {
            while (true) {
                refreshBatteryInfo()
                delay(5000) // Update every 5 seconds
            }
        }
    }

    // ==================== Data Collection ====================

    /**
     * Refresh battery information from system
     */
    fun refreshBatteryInfo() {
        _isLoading.value = true

        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Get battery properties
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            val health = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_HEALTH)
            val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f
            val voltage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_VOLTAGE) / 1000f
            val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

            // ✅ FIXED: Only get energy counter on API 31+
            val energyCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            } else 0L

            val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000f

            // Determine charging state
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            // Convert health code to string
            val healthString = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                else -> "Unknown"
            }

            // Convert status code to string
            val statusString = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            // Determine power source
            val pluggedString = when {
                status == BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                status == BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                status == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "None"
            }

            // Check if battery is present
            val isPresent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_PRESENT) == 1

            // Check battery saver mode
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val batterySaverEnabled = powerManager.isPowerSaveMode

            // ✅ FIXED: Technology detection without non-existent constants
            val technology = "Li-ion" // Default technology

            // Update UI state
            _uiState.update {
                it.copy(
                    level = level,
                    isCharging = isCharging,
                    temperature = temperature,
                    voltage = voltage,
                    current = currentNow,
                    health = healthString,
                    status = statusString,
                    technology = technology,
                    plugged = pluggedString,
                    chargeCounter = chargeCounter,
                    energyCounter = energyCounter,
                    isPresent = isPresent,
                    batterySaverEnabled = batterySaverEnabled,
                    capacity = calculateBatteryCapacity(level, voltage)
                )
            }

            // Add history entry
            addHistoryEntry(level, temperature, isCharging)

            // Update top apps
            updateTopApps()

        } catch (e: Exception) {
            // Handle any errors gracefully
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Calculate battery capacity based on level and voltage
     */
    private fun calculateBatteryCapacity(level: Int, voltage: Float): Int {
        // Base capacity estimation
        val baseCapacity = 100

        // Adjust based on voltage (typical Li-ion: 3.0V = 0%, 4.2V = 100%)
        val voltageRatio = ((voltage - 3.0) / 1.2).coerceIn(0f, 1f)
        val estimatedCapacity = (voltageRatio * 100).toInt()

        // Combine with level for more accurate estimation
        return ((level * 0.6 + estimatedCapacity * 0.4)).toInt().coerceIn(0, 100)
    }

    /**
     * Add a history entry for battery level tracking
     */
    private fun addHistoryEntry(level: Int, temperature: Float, isCharging: Boolean) {
        _uiState.update { state ->
            val newHistory = state.history.toMutableList()
            newHistory.add(
                BatteryHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    temperature = temperature,
                    isCharging = isCharging
                )
            )
            // Keep only last 50 entries
            if (newHistory.size > 50) {
                newHistory.removeAt(0)
            }
            state.copy(history = newHistory)
        }
    }

    /**
     * Update top battery consuming apps
     */
    private fun updateTopApps() {
        val apps = getTopBatteryApps()
        _topApps.value = apps
    }

    /**
     * Get top battery consuming apps
     * In production, use UsageStatsManager to get real data
     */
    fun getTopBatteryApps(): List<BatteryAppUsage> {
        // This is mock data - in production, use UsageStatsManager
        return listOf(
            BatteryAppUsage("Screen", 35.0f, null),
            BatteryAppUsage("System", 25.0f, null),
            BatteryAppUsage("Air Mouse", 15.0f, null),
            BatteryAppUsage("Other Apps", 25.0f, null)
        )
    }

    // ==================== Navigation Methods ====================

    /**
     * Open system battery settings
     */
    fun openBatterySettings() {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to battery settings
            openBatteryOptimizationSettings()
        }
    }

    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Intent.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to settings
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    }

    /**
     * Open power saving settings
     */
    fun openPowerSavingSettings() {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to battery settings
            openBatterySettings()
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Check if battery is low (below 15%)
     */
    fun isBatteryLow(): Boolean {
        return _uiState.value.level < 15
    }

    /**
     * Check if battery is critical (below 5%)
     */
    fun isBatteryCritical(): Boolean {
        return _uiState.value.level < 5
    }

    /**
     * Get battery level description
     */
    fun getLevelDescription(): String {
        val level = _uiState.value.level
        return when {
            level >= 80 -> "Excellent"
            level >= 60 -> "Good"
            level >= 40 -> "Fair"
            level >= 20 -> "Low"
            else -> "Critical"
        }
    }

    /**
     * Get time remaining estimate
     */
    fun getTimeRemaining(): String {
        val state = _uiState.value
        if (state.isCharging) return "Charging..."

        return when (state.level) {
            in 80..100 -> "~4h remaining"
            in 60..79 -> "~2h 30m remaining"
            in 40..59 -> "~1h 30m remaining"
            in 20..39 -> "~45m remaining"
            in 10..19 -> "~20m remaining"
            else -> "~5m remaining"
        }
    }

    /**
     * Get battery color based on level
     */
    fun getBatteryColor(): Int {
        val level = _uiState.value.level
        return when {
            level >= 70 -> 0xFF4CAF50.toInt()
            level >= 30 -> 0xFFFFC107.toInt()
            else -> 0xFFF44336.toInt()
        }
    }

    /**
     * Check if battery is healthy
     */
    fun isBatteryHealthy(): Boolean {
        val state = _uiState.value
        return state.health == "Good" && state.temperature < 45f && state.level > 10
    }

    /**
     * Get battery health description
     */
    fun getHealthDescription(): String {
        val health = _uiState.value.health
        return when (health) {
            "Good" -> "Battery is in good condition"
            "Overheat" -> "Battery is overheating - consider cooling down"
            "Dead" -> "Battery needs replacement"
            "Over Voltage" -> "Battery voltage too high"
            "Failure" -> "Battery failure detected - replace soon"
            else -> "Battery health unknown"
        }
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
    }
}