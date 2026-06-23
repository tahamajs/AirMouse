
package com.airmouse.utils

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    private val _optimizationLevel = MutableStateFlow(OptimizationLevel.BALANCED)
    val optimizationLevel: StateFlow<OptimizationLevel> = _optimizationLevel.asStateFlow()

    enum class OptimizationLevel {
        PERFORMANCE,
        BALANCED,
        POWER_SAVE,
        ULTRA_POWER_SAVE
    }

    fun updateBatteryStatus() {
        _batteryLevel.value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
        _isPowerSaveMode.value = powerManager.isPowerSaveMode
        _optimizationLevel.value = calculateOptimizationLevel()
    }

    private fun calculateOptimizationLevel(): OptimizationLevel {
        return when {
            _isPowerSaveMode.value || (_batteryLevel.value < 15 && !_isCharging.value) -> OptimizationLevel.ULTRA_POWER_SAVE
            _batteryLevel.value < 30 && !_isCharging.value -> OptimizationLevel.POWER_SAVE
            _isCharging.value || _batteryLevel.value > 80 -> OptimizationLevel.PERFORMANCE
            else -> OptimizationLevel.BALANCED
        }
    }

    fun getRecommendedSensorDelay(): Int {
        return when (_optimizationLevel.value) {
            OptimizationLevel.PERFORMANCE -> android.hardware.SensorManager.SENSOR_DELAY_GAME
            OptimizationLevel.BALANCED -> android.hardware.SensorManager.SENSOR_DELAY_GAME
            OptimizationLevel.POWER_SAVE -> android.hardware.SensorManager.SENSOR_DELAY_NORMAL
            OptimizationLevel.ULTRA_POWER_SAVE -> android.hardware.SensorManager.SENSOR_DELAY_UI
        }
    }
}