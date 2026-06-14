package com.airmouse.presentation.ui.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    private var isCollecting = false
    private val batteryHistory = mutableListOf<BatteryHistoryEntry>()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryInfo(intent)
            }
        }
    }

    init {
        registerBatteryReceiver()
        loadBatterySaverState()
        startHistoryCollection()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else 0

        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000f
        } else 0f

        val capacity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } else 0

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }

        val plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Disconnected"
        }

        val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Unknown"
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val isCharging = status == "Charging" || status == "Full"
        val isPresent = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)

        val chargeCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } else 0

        val energyCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        } else 0L

        val estimatedRemaining = calculateEstimatedRemaining(batteryLevel, isCharging, current)

        _uiState.update { state ->
            state.copy(
                level = batteryLevel,
                temperature = temperature,
                voltage = voltage,
                current = current,
                capacity = capacity,
                status = status,
                plugged = plugged,
                health = health,
                technology = technology,
                isCharging = isCharging,
                isPresent = isPresent,
                chargeCounter = chargeCounter,
                energyCounter = energyCounter,
                estimatedRemaining = estimatedRemaining
            )
        }

        addToHistory(batteryLevel, temperature, isCharging)
    }

    private fun calculateEstimatedRemaining(level: Int, isCharging: Boolean, current: Float): Long {
        if (isCharging) {
            val remainingCapacity = 100 - level
            return if (current > 0) (remainingCapacity * 60 / current).toLong() else 0L
        } else {
            return if (current > 0) (level * 60 / current).toLong() else 0L
        }
    }

    private fun loadBatterySaverState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            _uiState.update { it.copy(batterySaverEnabled = powerManager.isPowerSaveMode) }
        }
    }

    private fun addToHistory(level: Int, temperature: Float, isCharging: Boolean) {
        batteryHistory.add(
            BatteryHistoryEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                temperature = temperature,
                isCharging = isCharging
            )
        )
        while (batteryHistory.size > 100) batteryHistory.removeAt(0)
        _uiState.update { it.copy(history = batteryHistory.toList()) }
    }

    private fun startHistoryCollection() {
        viewModelScope.launch {
            isCollecting = true
            while (isCollecting) {
                delay(60000)
            }
        }
    }

    fun refreshBatteryInfo() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let { updateBatteryInfo(it) }
    }

    fun getTopBatteryApps(): List<AppBatteryUsage> {
        // In production, this would use UsageStatsManager
        return listOf(
            AppBatteryUsage("Air Mouse", 25.5f, null),
            AppBatteryUsage("Screen", 18.2f, null),
            AppBatteryUsage("Android System", 12.8f, null)
        )
    }

    fun openBatterySettings() {
        val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
        context.startActivity(intent)
    }

    fun openBatteryOptimizationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    }

    fun openPowerSavingSettings() {
        val intent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
        context.startActivity(intent)
    }

    fun formatRemainingTime(minutes: Long): String {
        return when {
            minutes <= 0 -> "Calculating..."
            minutes < 60 -> "${minutes} min"
            minutes < 1440 -> "${minutes / 60} hr ${minutes % 60} min"
            else -> "${minutes / 1440} days"
        }
    }

    fun getPowerSourceDescription(): String {
        return when {
            _uiState.value.isCharging -> "Charging via ${_uiState.value.plugged}"
            _uiState.value.level <= 15 -> "Critical battery! Please charge"
            _uiState.value.level <= 30 -> "Low battery - ${_uiState.value.level}% remaining"
            else -> "Running on battery - ${_uiState.value.level}%"
        }
    }

    override fun onCleared() {
        super.onCleared()
        isCollecting = false
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
    }
}

data class AppBatteryUsage(val name: String, val usagePercent: Float, val icon: androidx.compose.ui.graphics.vector.ImageVector?)
