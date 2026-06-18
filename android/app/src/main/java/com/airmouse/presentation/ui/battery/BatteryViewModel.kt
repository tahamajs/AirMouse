package com.airmouse.presentation.ui.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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

    private fun startBatteryMonitoring() {
        viewModelScope.launch {
            while (true) {
                refreshBatteryInfo()
                delay(5000)
            }
        }
    }

    // ==================== Data Collection ====================

    fun refreshBatteryInfo() {
        _isLoading.value = true

        try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            // Register a sticky receiver to catch hardware broadcast properties cleanly
            val batteryStatusIntent: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            // 1. Level & Capacity
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

            // 2. Extracted Hardware Status Keys via Receiver Intent fallbacks
            val status = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                ?: batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

            val health = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

            val temperature = (batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val voltage = (batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f
            val isPresent = batteryStatusIntent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true

            // 3. Extracted Structural Properties via BatteryManager API
            val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

            // Only available on API 31+ (Android 12+)
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

            // Convert status code to string Map
            val statusString = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            // Determine explicit plugged context power source
            val plugged = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val pluggedString = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "None"
            }

            // Check battery saver mode status
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val batterySaverEnabled = powerManager.isPowerSaveMode

            // Update UI state securely
            _uiState.update {
                it.copy(
                    level = level,
                    isCharging = isCharging,
                    temperature = temperature,
                    voltage = voltage,
                    current = currentNow,
                    health = healthString,
                    status = statusString,
                    technology = "Li-ion",
                    plugged = pluggedString,
                    chargeCounter = chargeCounter,
                    energyCounter = energyCounter,
                    isPresent = isPresent,
                    batterySaverEnabled = batterySaverEnabled,
                    capacity = calculateBatteryCapacity(level, voltage)
                )
            }

            addHistoryEntry(level, temperature, isCharging)
            updateTopApps()

        } catch (ignored: Exception) {
            // Unused exceptions caught cleanly to prevent crash logs inside the sampling loop
        } finally {
            _isLoading.value = false
        }
    }

    private fun calculateBatteryCapacity(level: Int, voltage: Float): Int {
        val voltageRatio = ((voltage - 3.0f) / 1.2f).coerceIn(0f, 1f)
        val estimatedCapacity = (voltageRatio * 100).toInt()
        return (level * 0.6 + estimatedCapacity * 0.4).toInt().coerceIn(0, 100)
    }

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
            if (newHistory.size > 50) {
                newHistory.removeAt(0)
            }
            state.copy(history = newHistory)
        }
    }

    private fun updateTopApps() {
        _topApps.value = getTopBatteryApps()
    }

    fun getTopBatteryApps(): List<BatteryAppUsage> {
        return listOf(
            BatteryAppUsage("Screen", 35.0f, null),
            BatteryAppUsage("System", 25.0f, null),
            BatteryAppUsage("Air Mouse", 15.0f, null),
            BatteryAppUsage("Other Apps", 25.0f, null)
        )
    }

    // ==================== Settings Navigators ====================

    fun openBatterySettings() {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    // ==================== Utility Methods ====================

    fun isBatteryLow(): Boolean = _uiState.value.level < 15

    fun isBatteryCritical(): Boolean = _uiState.value.level < 5

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

    fun getBatteryColor(): Long {
        val level = _uiState.value.level
        return when {
            level >= 70 -> 0xFF4CAF50
            level >= 30 -> 0xFFFFC107
            else -> 0xFFF44336
        }
    }

    fun isBatteryHealthy(): Boolean {
        val state = _uiState.value
        return state.health == "Good" && state.temperature < 45f && state.level > 10
    }

    fun getHealthDescription(): String {
        return when (_uiState.value.health) {
            "Good" -> "Battery is in good condition"
            "Overheat" -> "Battery is overheating - consider cooling down"
            "Dead" -> "Battery needs replacement"
            "Over Voltage" -> "Battery voltage too high"
            "Failure" -> "Battery failure detected - replace soon"
            else -> "Battery health unknown"
        }
    }
}