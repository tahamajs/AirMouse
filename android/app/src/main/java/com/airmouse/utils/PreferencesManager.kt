package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    // Sensitivity
    fun getSensitivity(): Float = prefs.getFloat("sensitivity", 0.5f)
    fun setSensitivity(value: Float) = prefs.edit().putFloat("sensitivity", value).apply()

    // Thresholds
    fun getClickThreshold(): Float = prefs.getFloat("click_threshold", 5.0f)
    fun setClickThreshold(value: Float) = prefs.edit().putFloat("click_threshold", value).apply()

    fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", 400L)
    fun setDoubleClickInterval(value: Long) = prefs.edit().putLong("double_click_interval", value).apply()

    fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", 8.0f)
    fun setScrollThreshold(value: Float) = prefs.edit().putFloat("scroll_threshold", value).apply()

    fun getScrollDebounce(): Float = prefs.getFloat("scroll_debounce", 2.0f)
    fun setScrollDebounce(value: Float) = prefs.edit().putFloat("scroll_debounce", value).apply()

    fun getRightClickTilt(): Float = prefs.getFloat("rightclick_tilt", 45.0f)
    fun setRightClickTilt(value: Float) = prefs.edit().putFloat("rightclick_tilt", value).apply()

    fun getRightClickDuration(): Long = prefs.getLong("rightclick_duration", 500L)
    fun setRightClickDuration(value: Long) = prefs.edit().putLong("rightclick_duration", value).apply()

    // Haptic
    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_enabled", enabled).apply()

    // Battery saver
    fun isBatterySaverEnabled(): Boolean = prefs.getBoolean("battery_saver", true)
    fun setBatterySaverEnabled(enabled: Boolean) = prefs.edit().putBoolean("battery_saver", enabled).apply()

    // Last IP
    fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""
    fun setLastIp(ip: String) = prefs.edit().putString("last_ip", ip).apply()
}