// app/src/main/java/com/airmouse/data/local/PreferencesManager.kt
package com.airmouse.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    // Calibration
    fun setCalibrated(calibrated: Boolean) = prefs.edit().putBoolean("is_calibrated", calibrated).apply()
    fun isCalibrated(): Boolean = prefs.getBoolean("is_calibrated", false)

    // Connection
    fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""
    fun setLastIp(ip: String) = prefs.edit().putString("last_ip", ip).apply()
    fun getLastPort(): Int = prefs.getInt("last_port", 8080)
    fun setLastPort(port: Int) = prefs.edit().putInt("last_port", port).apply()

    // Sensitivity & thresholds
    fun getSensitivity(): Float = prefs.getFloat("sensitivity", 0.5f)
    fun setSensitivity(value: Float) = prefs.edit().putFloat("sensitivity", value).apply()
    fun getClickThreshold(): Float = prefs.getFloat("click_threshold", 10f)
    fun setClickThreshold(value: Float) = prefs.edit().putFloat("click_threshold", value).apply()
    fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", 300L)
    fun setDoubleClickInterval(value: Long) = prefs.edit().putLong("double_click_interval", value).apply()
    fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", 5f)
    fun setScrollThreshold(value: Float) = prefs.edit().putFloat("scroll_threshold", value).apply()
    fun getRightClickTilt(): Float = prefs.getFloat("rightclick_tilt", 15f)
    fun setRightClickTilt(value: Float) = prefs.edit().putFloat("rightclick_tilt", value).apply()

    // Haptic & theme
    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_enabled", enabled).apply()
    fun getTheme(): String = prefs.getString("theme", "dark") ?: "dark"
    fun setTheme(theme: String) = prefs.edit().putString("theme", theme).apply()

    // AI & predictive
    fun isAISmoothingEnabled(): Boolean = prefs.getBoolean("ai_smoothing", false)
    fun setAISmoothingEnabled(enabled: Boolean) = prefs.edit().putBoolean("ai_smoothing", enabled).apply()
    fun isPredictiveEnabled(): Boolean = prefs.getBoolean("predictive", true)
    fun setPredictiveEnabled(enabled: Boolean) = prefs.edit().putBoolean("predictive", enabled).apply()

    // Gesture counters
    fun incrementClick() = incrementCount("click_count")
    fun incrementDoubleClick() = incrementCount("double_click_count")
    fun incrementRightClick() = incrementCount("right_click_count")
    fun incrementScroll() = incrementCount("scroll_count")
    fun getClickCount(): Int = prefs.getInt("click_count", 0)
    fun getDoubleClickCount(): Int = prefs.getInt("double_click_count", 0)
    fun getRightClickCount(): Int = prefs.getInt("right_click_count", 0)
    fun getScrollCount(): Int = prefs.getInt("scroll_count", 0)

    private fun incrementCount(key: String) {
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }
}
