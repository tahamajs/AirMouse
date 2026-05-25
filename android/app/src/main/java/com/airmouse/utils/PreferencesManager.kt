package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages all user preferences: sensitivity, gesture thresholds, haptic, etc.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    // Sensitivity
    fun getSensitivity(): Float = prefs.getFloat("sensitivity", 0.5f)
    fun setSensitivity(value: Float) = prefs.edit().putFloat("sensitivity", value).apply()

    // Click threshold (rad/s)
    fun getClickThreshold(): Float = prefs.getFloat("click_threshold", 5.0f)
    fun setClickThreshold(value: Float) = prefs.edit().putFloat("click_threshold", value).apply()

    // Double-click interval (ms)
    fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", 400L)
    fun setDoubleClickInterval(value: Long) = prefs.edit().putLong("double_click_interval", value).apply()

    // Scroll speed threshold (m/s²)
    fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", 8.0f)
    fun setScrollThreshold(value: Float) = prefs.edit().putFloat("scroll_threshold", value).apply()

    // Scroll debounce (m/s²)
    fun getScrollDebounce(): Float = prefs.getFloat("scroll_debounce", 2.0f)
    fun setScrollDebounce(value: Float) = prefs.edit().putFloat("scroll_debounce", value).apply()

    // Right‑click tilt angle (degrees)
    fun getRightClickTilt(): Float = prefs.getFloat("rightclick_tilt", 45.0f)
    fun setRightClickTilt(value: Float) = prefs.edit().putFloat("rightclick_tilt", value).apply()

    // Right‑click hold duration (ms)
    fun getRightClickDuration(): Long = prefs.getLong("rightclick_duration", 500L)
    fun setRightClickDuration(value: Long) = prefs.edit().putLong("rightclick_duration", value).apply()

    // Haptic feedback
    fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean("haptic_enabled", enabled).apply()

    // Last used IP
    fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""
    fun setLastIp(ip: String) = prefs.edit().putString("last_ip", ip).apply()
}