package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)

    // ==================== Basic Operations ====================
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun getFloat(key: String, default: Float = 0f): Float = prefs.getFloat(key, default)
    
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)
    
    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String = ""): String = prefs.getString(key, default) ?: default
    
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    // ==================== Calibration Methods ====================
    fun isCalibrated(): Boolean = getBoolean("calibration_complete", false)
    fun setCalibrated(calibrated: Boolean) = putBoolean("calibration_complete", calibrated)
    
    fun resetCalibrationAttempts() = putInt("calibration_attempts", 0)
    
    fun getGyroBias(): FloatArray = floatArrayOf(
        getFloat("gyro_bias_x", 0f),
        getFloat("gyro_bias_y", 0f),
        getFloat("gyro_bias_z", 0f)
    )
    fun saveGyroBias(bias: FloatArray) {
        putFloat("gyro_bias_x", bias.getOrElse(0) { 0f })
        putFloat("gyro_bias_y", bias.getOrElse(1) { 0f })
        putFloat("gyro_bias_z", bias.getOrElse(2) { 0f })
    }
    
    fun getAccelOffset(): FloatArray = floatArrayOf(
        getFloat("accel_offset_x", 0f),
        getFloat("accel_offset_y", 0f),
        getFloat("accel_offset_z", 0f)
    )
    fun getAccelScale(): FloatArray = floatArrayOf(
        getFloat("accel_scale_x", 1f),
        getFloat("accel_scale_y", 1f),
        getFloat("accel_scale_z", 1f)
    )
    fun saveAccelParams(offset: FloatArray, scale: FloatArray) {
        putFloat("accel_offset_x", offset.getOrElse(0) { 0f })
        putFloat("accel_offset_y", offset.getOrElse(1) { 0f })
        putFloat("accel_offset_z", offset.getOrElse(2) { 0f })
        putFloat("accel_scale_x", scale.getOrElse(0) { 1f })
        putFloat("accel_scale_y", scale.getOrElse(1) { 1f })
        putFloat("accel_scale_z", scale.getOrElse(2) { 1f })
    }
    
    fun saveAccelerometerParams(offset: FloatArray, scale: FloatArray) = saveAccelParams(offset, scale)
    
    fun getMagOffset(): FloatArray = floatArrayOf(
        getFloat("mag_offset_x", 0f),
        getFloat("mag_offset_y", 0f),
        getFloat("mag_offset_z", 0f)
    )
    fun getMagScale(): FloatArray = floatArrayOf(
        getFloat("mag_scale_x", 1f),
        getFloat("mag_scale_y", 1f),
        getFloat("mag_scale_z", 1f)
    )
    fun saveMagCalibration(offset: FloatArray, scale: FloatArray) {
        putFloat("mag_offset_x", offset.getOrElse(0) { 0f })
        putFloat("mag_offset_y", offset.getOrElse(1) { 0f })
        putFloat("mag_offset_z", offset.getOrElse(2) { 0f })
        putFloat("mag_scale_x", scale.getOrElse(0) { 1f })
        putFloat("mag_scale_y", scale.getOrElse(1) { 1f })
        putFloat("mag_scale_z", scale.getOrElse(2) { 1f })
    }

    // ==================== Settings Methods ====================
    fun getSensitivity(): Float = getFloat("sensitivity", 0.5f)
    fun setSensitivity(value: Float) = putFloat("sensitivity", value)
    
    fun getClickThreshold(): Float = getFloat("click_threshold", 8f)
    fun setClickThreshold(value: Float) = putFloat("click_threshold", value)
    
    fun getDoubleClickInterval(): Long = getLong("double_click_interval", 300L)
    fun setDoubleClickInterval(value: Long) = putLong("double_click_interval", value)
    
    fun getScrollThreshold(): Float = getFloat("scroll_threshold", 6f)
    fun setScrollThreshold(value: Float) = putFloat("scroll_threshold", value)
    
    fun getScrollDebounce(): Long = getLong("scroll_debounce", 100L)
    fun setScrollDebounce(value: Long) = putLong("scroll_debounce", value)
    
    fun getRightClickTilt(): Float = getFloat("right_click_tilt", 15f)
    fun setRightClickTilt(value: Float) = putFloat("right_click_tilt", value)
    
    fun getRightClickDuration(): Long = getLong("right_click_duration", 500L)
    fun setRightClickDuration(value: Long) = putLong("right_click_duration", value)
    
    fun isHapticEnabled(): Boolean = getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = putBoolean("haptic_enabled", enabled)
    
    fun isAISmoothingEnabled(): Boolean = getBoolean("ai_smoothing", false)
    fun setAISmoothingEnabled(enabled: Boolean) = putBoolean("ai_smoothing", enabled)
    
    fun isPredictiveEnabled(): Boolean = getBoolean("predictive_movement", false)
    fun setPredictiveEnabled(enabled: Boolean) = putBoolean("predictive_movement", enabled)

    // ==================== Connection Methods ====================
    fun getLastIp(): String = getString("last_ip", "")
    fun setLastIp(ip: String) = putString("last_ip", ip)
    
    fun getLastPort(): Int = getInt("last_port", 8080)
    fun setLastPort(port: Int) = putInt("last_port", port)
    
    fun getServerMac(): String = getString("server_mac", "")
    fun setServerMac(mac: String) = putString("server_mac", mac)

    // ==================== Proximity Methods ====================
    fun getNearThreshold(): Float = getFloat("near_threshold", 0.5f)
    fun setNearThreshold(value: Float) = putFloat("near_threshold", value)
    
    fun getFarThreshold(): Float = getFloat("far_threshold", 1.5f)
    fun setFarThreshold(value: Float) = putFloat("far_threshold", value)

    // ==================== Accessibility Methods ====================
    fun isAnnounceMovementEnabled(): Boolean = getBoolean("announce_movement", false)
    fun setAnnounceMovementEnabled(enabled: Boolean) = putBoolean("announce_movement", enabled)
    
    fun isAnnounceClicksEnabled(): Boolean = getBoolean("announce_clicks", false)
    fun setAnnounceClicksEnabled(enabled: Boolean) = putBoolean("announce_clicks", enabled)

    // ==================== Onboarding Methods ====================
    fun isOnboardingCompleted(): Boolean = getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) = putBoolean("onboarding_completed", completed)

    // ==================== Profile Methods ====================
    fun getLastUsedProfile(): String = getString("last_used_profile", "Default")
    fun setLastUsedProfile(profile: String) = putString("last_used_profile", profile)
    
    fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float, scrollThreshold: Float) {
        putFloat("profile_${name}_sensitivity", sensitivity)
        putFloat("profile_${name}_click_threshold", clickThreshold)
        putFloat("profile_${name}_scroll_threshold", scrollThreshold)
    }
    
    fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float) {
        saveProfile(name, sensitivity, clickThreshold, getScrollThreshold())
    }
    
    fun getProfileSensitivity(name: String): Float = getFloat("profile_${name}_sensitivity", 0.5f)
    fun getProfileClickThreshold(name: String): Float = getFloat("profile_${name}_click_threshold", 8f)
    fun getProfileScrollThreshold(name: String): Float = getFloat("profile_${name}_scroll_threshold", 6f)
    
    fun getAllProfileNames(): List<String> {
        val allKeys = prefs.all.keys
        return allKeys.filter { it.startsWith("profile_") && it.endsWith("_sensitivity") }
            .map { it.removePrefix("profile_").removeSuffix("_sensitivity") }
            .distinct()
    }
    
    fun deleteProfile(name: String) {
        prefs.edit()
            .remove("profile_${name}_sensitivity")
            .remove("profile_${name}_click_threshold")
            .remove("profile_${name}_scroll_threshold")
            .apply()
    }

    // ==================== Statistics Methods ====================
    fun getClickCount(): Int = getInt("stat_clicks", 0)
    fun incrementClickCount() = putInt("stat_clicks", getClickCount() + 1)
    
    fun getScrollCount(): Int = getInt("stat_scrolls", 0)
    fun incrementScrollCount() = putInt("stat_scrolls", getScrollCount() + 1)
    
    fun getRightClickCount(): Int = getInt("stat_right_clicks", 0)
    fun incrementRightClickCount() = putInt("stat_right_clicks", getRightClickCount() + 1)
    
    fun getDoubleClickCount(): Int = getInt("stat_double_clicks", 0)
    fun incrementDoubleClickCount() = putInt("stat_double_clicks", getDoubleClickCount() + 1)

    // ==================== Gesture Methods ====================
    fun saveCustomGesture(name: String, data: Float) = putFloat("gesture_$name", data)
    fun getCustomGesture(name: String): Float = getFloat("gesture_$name", 0f)
    fun getAllCustomGestures(): Map<String, Float> {
        val allKeys = prefs.all.keys
        return allKeys.filter { it.startsWith("gesture_") }
            .associate { it.removePrefix("gesture_") to getFloat(it, 0f) }
    }

    // ==================== Theme Methods ====================
    fun getTheme(): String = getString("theme", "system")
    fun setTheme(theme: String) = putString("theme", theme)
}
