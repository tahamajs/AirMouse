package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Legacy SharedPreferences wrapper.
 * For new features, use PreferencesDataStore (preference DataStore) instead.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    fun getSensitivity(): Float = prefs.getFloat("sensitivity", Constants.DEFAULT_SENSITIVITY)
    fun setSensitivity(value: Float) = prefs.edit().putFloat("sensitivity", value).apply()

    fun getClickThreshold(): Float = prefs.getFloat("click_threshold", Constants.CLICK_SPEED_THRESHOLD)
    fun setClickThreshold(value: Float) = prefs.edit().putFloat("click_threshold", value).apply()

    fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", Constants.DOUBLE_CLICK_INTERVAL_MS)
    fun setDoubleClickInterval(value: Long) = prefs.edit().putLong("double_click_interval", value).apply()

    fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", Constants.SCROLL_SPEED_THRESHOLD)
    fun setScrollThreshold(value: Float) = prefs.edit().putFloat("scroll_threshold", value).apply()

    fun getScrollDebounce(): Float = prefs.getFloat("scroll_debounce", Constants.SCROLL_DEBOUNCE)
    fun setScrollDebounce(value: Float) = prefs.edit().putFloat("scroll_debounce", value).apply()

    fun getRightClickTilt(): Float = prefs.getFloat("rightclick_tilt", Constants.RIGHT_CLICK_TILT_DEG)
    fun setRightClickTilt(value: Float) = prefs.edit().putFloat("rightclick_tilt", value).apply()

    fun getRightClickDuration(): Long = prefs.getLong("rightclick_duration", Constants.RIGHT_CLICK_DURATION_MS)
    fun setRightClickDuration(value: Long) = prefs.edit().putLong("rightclick_duration", value).apply()

    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_enabled", enabled).apply()

    fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""
    fun setLastIp(ip: String) = prefs.edit().putString("last_ip", ip).apply()
}