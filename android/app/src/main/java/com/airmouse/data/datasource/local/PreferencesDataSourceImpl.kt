
package com.airmouse.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IPreferencesDataSource {

    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)

    
    private val _isCalibrated = MutableStateFlow(prefs.getBoolean("is_calibrated", false))

    override suspend fun setCalibrated(calibrated: Boolean) {
        prefs.edit().putBoolean("is_calibrated", calibrated).apply()
        _isCalibrated.update { calibrated }
    }

    override fun isCalibrated(): Flow<Boolean> = _isCalibrated.asStateFlow()

    override suspend fun isCalibratedOnce(): Boolean = prefs.getBoolean("is_calibrated", false)

    override suspend fun getCalibrationTimestamp(): Long = prefs.getLong("calibration_timestamp", 0)

    override suspend fun setCalibrationTimestamp(timestamp: Long) {
        prefs.edit().putLong("calibration_timestamp", timestamp).apply()
    }

    override suspend fun setLastIp(ip: String) {
        prefs.edit().putString("last_ip", ip).apply()
    }

    override suspend fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""

    override suspend fun setLastPort(port: Int) {
        prefs.edit().putInt("last_port", port).apply()
    }

    override suspend fun getLastPort(): Int = prefs.getInt("last_port", 8080)

    override suspend fun setLastProtocol(protocol: String) {
        prefs.edit().putString("last_protocol", protocol.uppercase()).apply()
    }

    override suspend fun getLastProtocol(): String = prefs.getString("last_protocol", "WEBSOCKET") ?: "WEBSOCKET"

    override suspend fun setSensitivity(value: Float) {
        prefs.edit().putFloat("sensitivity", value).apply()
    }

    override suspend fun getSensitivity(): Float = prefs.getFloat("sensitivity", 0.5f)

    override suspend fun setClickThreshold(value: Float) {
        prefs.edit().putFloat("click_threshold", value).apply()
    }

    override suspend fun getClickThreshold(): Float = prefs.getFloat("click_threshold", 8f)

    override suspend fun setDoubleClickInterval(value: Long) {
        prefs.edit().putLong("double_click_interval", value).apply()
    }

    override suspend fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", 400L)

    override suspend fun setScrollThreshold(value: Float) {
        prefs.edit().putFloat("scroll_threshold", value).apply()
    }

    override suspend fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", 6f)

    override suspend fun setRightClickTilt(value: Float) {
        prefs.edit().putFloat("right_click_tilt", value).apply()
    }

    override suspend fun getRightClickTilt(): Float = prefs.getFloat("right_click_tilt", 45f)

    override suspend fun setRightClickDuration(value: Long) {
        prefs.edit().putLong("right_click_duration", value).apply()
    }

    override suspend fun getRightClickDuration(): Long = prefs.getLong("right_click_duration", 500L)

    override suspend fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    override suspend fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)

    override suspend fun setHapticStrength(strength: String) {
        prefs.edit().putString("haptic_strength", strength).apply()
    }

    override suspend fun getHapticStrength(): String = prefs.getString("haptic_strength", "medium") ?: "medium"

    override suspend fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    override suspend fun isSoundEnabled(): Boolean = prefs.getBoolean("sound_enabled", true)

    override suspend fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    override suspend fun getTheme(): String = prefs.getString("theme", "system") ?: "system"

    override suspend fun setDynamicColors(enabled: Boolean) {
        prefs.edit().putBoolean("dynamic_colors", enabled).apply()
    }

    override suspend fun isDynamicColorsEnabled(): Boolean = prefs.getBoolean("dynamic_colors", true)

    override suspend fun setFontSize(size: Float) {
        prefs.edit().putFloat("font_size", size).apply()
    }

    override suspend fun getFontSize(): Float = prefs.getFloat("font_size", 16f)

    override suspend fun setAISmoothingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_smoothing", enabled).apply()
    }

    override suspend fun isAISmoothingEnabled(): Boolean = prefs.getBoolean("ai_smoothing", false)

    override suspend fun setPredictiveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("predictive_movement", enabled).apply()
    }

    override suspend fun isPredictiveEnabled(): Boolean = prefs.getBoolean("predictive_movement", true)

    override suspend fun setAiBlendFactor(factor: Float) {
        prefs.edit().putFloat("ai_blend_factor", factor).apply()
    }

    override suspend fun getAiBlendFactor(): Float = prefs.getFloat("ai_blend_factor", 0.7f)

    override suspend fun setInvertX(enabled: Boolean) {
        prefs.edit().putBoolean("invert_x", enabled).apply()
    }

    override suspend fun isInvertXEnabled(): Boolean = prefs.getBoolean("invert_x", false)

    override suspend fun setInvertY(enabled: Boolean) {
        prefs.edit().putBoolean("invert_y", enabled).apply()
    }

    override suspend fun isInvertYEnabled(): Boolean = prefs.getBoolean("invert_y", false)

    override suspend fun setAccelerationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("acceleration_enabled", enabled).apply()
    }

    override suspend fun isAccelerationEnabled(): Boolean = prefs.getBoolean("acceleration_enabled", true)

    override suspend fun setSmoothingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("smoothing_enabled", enabled).apply()
    }

    override suspend fun isSmoothingEnabled(): Boolean = prefs.getBoolean("smoothing_enabled", true)

    override suspend fun setSmoothingFactor(factor: Float) {
        prefs.edit().putFloat("smoothing_factor", factor).apply()
    }

    override suspend fun getSmoothingFactor(): Float = prefs.getFloat("smoothing_factor", 0.5f)

    override suspend fun setAutoConnect(enabled: Boolean) {
        prefs.edit().putBoolean("auto_connect", enabled).apply()
    }

    override suspend fun isAutoConnectEnabled(): Boolean = prefs.getBoolean("auto_connect", true)

    override suspend fun setReconnectAttempts(attempts: Int) {
        prefs.edit().putInt("reconnect_attempts", attempts).apply()
    }

    override suspend fun getReconnectAttempts(): Int = prefs.getInt("reconnect_attempts", 5)

    override suspend fun setConnectionTimeout(timeout: Int) {
        prefs.edit().putInt("connection_timeout", timeout).apply()
    }

    override suspend fun getConnectionTimeout(): Int = prefs.getInt("connection_timeout", 5000)

    
    override suspend fun incrementClick() = incrementCount("stat_clicks")
    override suspend fun incrementDoubleClick() = incrementCount("stat_double_clicks")
    override suspend fun incrementRightClick() = incrementCount("stat_right_clicks")
    override suspend fun incrementScroll() = incrementCount("stat_scrolls")

    override suspend fun incrementGesture(gestureName: String) {
        incrementCount("stat_gesture_$gestureName")
        incrementCount("stat_total_gestures")
    }

    override suspend fun getClickCount(): Int = prefs.getInt("stat_clicks", 0)
    override suspend fun getDoubleClickCount(): Int = prefs.getInt("stat_double_clicks", 0)
    override suspend fun getRightClickCount(): Int = prefs.getInt("stat_right_clicks", 0)
    override suspend fun getScrollCount(): Int = prefs.getInt("stat_scrolls", 0)

    override suspend fun getGestureCount(gestureName: String): Int = prefs.getInt("stat_gesture_$gestureName", 0)

    override suspend fun getAllGestureCounts(): Map<String, Int> {
        val allEntries = prefs.all
        return allEntries.filterKeys { it.startsWith("stat_gesture_") }
            .mapKeys { it.key.removePrefix("stat_gesture_") }
            .mapValues { it.value as? Int ?: 0 }
    }

    private suspend fun incrementCount(key: String) {
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    override suspend fun resetAllPreferences() {
        prefs.edit().clear().apply()
        _isCalibrated.update { false }
    }

    override suspend fun resetStatistics() {
        val keysToKeep = listOf("is_calibrated", "last_ip", "last_port", "sensitivity", "theme")
        val editor = prefs.edit()
        prefs.all.keys.filter { !keysToKeep.contains(it) }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()
    }
}
