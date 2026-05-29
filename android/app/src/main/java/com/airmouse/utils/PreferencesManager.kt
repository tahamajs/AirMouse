package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Central preferences manager for the Air Mouse app.
 * Stores user settings, calibration data, gesture counts, profiles, themes, and more.
 */
class PreferencesManager(context: Context) {
    fun getCalibrationAttempts(): Int = prefs.getInt("calib_attempts", 0)

    fun incrementCalibrationAttempts() {
        val current = getCalibrationAttempts()
        prefs.edit().putInt("calib_attempts", current + 1).apply()
    }

    fun resetCalibrationAttempts() {
        prefs.edit().putInt("calib_attempts", 0).apply()
    }
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    // ----------------------------------------------------------------------
    // Basic settings
    // ----------------------------------------------------------------------
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

    fun getLastPort(): Int = prefs.getInt("last_port", 8080)
    fun setLastPort(port: Int) = prefs.edit().putInt("last_port", port.coerceIn(1, 65535)).apply()

    // ----------------------------------------------------------------------
    // Calibration data
    // ----------------------------------------------------------------------
    fun saveGyroBias(bias: FloatArray) {
        prefs.edit()
            .putFloat("gyro_bias_x", bias[0])
            .putFloat("gyro_bias_y", bias[1])
            .putFloat("gyro_bias_z", bias[2])
            .apply()
    }
    fun getGyroBias(): FloatArray = floatArrayOf(
        prefs.getFloat("gyro_bias_x", 0f),
        prefs.getFloat("gyro_bias_y", 0f),
        prefs.getFloat("gyro_bias_z", 0f)
    )

    fun saveAccelParams(offset: FloatArray, scale: FloatArray) {
        prefs.edit()
            .putFloat("accel_off_x", offset[0])
            .putFloat("accel_off_y", offset[1])
            .putFloat("accel_off_z", offset[2])
            .putFloat("accel_scale_x", scale[0])
            .putFloat("accel_scale_y", scale[1])
            .putFloat("accel_scale_z", scale[2])
            .apply()
    }
    fun saveAccelerometerParams(offset: FloatArray, scale: FloatArray) = saveAccelParams(offset, scale)

    fun getAccelOffset(): FloatArray = floatArrayOf(
        prefs.getFloat("accel_off_x", 0f),
        prefs.getFloat("accel_off_y", 0f),
        prefs.getFloat("accel_off_z", 0f)
    )
    fun getAccelScale(): FloatArray = floatArrayOf(
        prefs.getFloat("accel_scale_x", 1f),
        prefs.getFloat("accel_scale_y", 1f),
        prefs.getFloat("accel_scale_z", 1f)
    )

    fun saveMagnetometerParams(offset: FloatArray, scale: FloatArray) {
        prefs.edit()
            .putFloat("mag_off_x", offset[0])
            .putFloat("mag_off_y", offset[1])
            .putFloat("mag_off_z", offset[2])
            .putFloat("mag_scale_x", scale[0])
            .putFloat("mag_scale_y", scale[1])
            .putFloat("mag_scale_z", scale[2])
            .apply()
    }
    fun getMagOffset(): FloatArray = floatArrayOf(
        prefs.getFloat("mag_off_x", 0f),
        prefs.getFloat("mag_off_y", 0f),
        prefs.getFloat("mag_off_z", 0f)
    )
    fun getMagScale(): FloatArray = floatArrayOf(
        prefs.getFloat("mag_scale_x", 1f),
        prefs.getFloat("mag_scale_y", 1f),
        prefs.getFloat("mag_scale_z", 1f)
    )

    fun isCalibrated(): Boolean = prefs.getBoolean("is_calibrated", false)
    fun setCalibrated(calibrated: Boolean) = prefs.edit().putBoolean("is_calibrated", calibrated).apply()

    // ----------------------------------------------------------------------
    // Gesture counters
    // ----------------------------------------------------------------------
    fun getClickCount(): Int = prefs.getInt("click_count", 0)
    fun incrementClick() = prefs.edit().putInt("click_count", getClickCount() + 1).apply()

    fun getScrollCount(): Int = prefs.getInt("scroll_count", 0)
    fun incrementScroll() = prefs.edit().putInt("scroll_count", getScrollCount() + 1).apply()

    fun getRightClickCount(): Int = prefs.getInt("right_click_count", 0)
    fun incrementRightClick() = prefs.edit().putInt("right_click_count", getRightClickCount() + 1).apply()

    fun getDoubleClickCount(): Int = prefs.getInt("double_click_count", 0)
    fun incrementDoubleClick() = prefs.edit().putInt("double_click_count", getDoubleClickCount() + 1).apply()

    // ----------------------------------------------------------------------
    // User profiles
    // ----------------------------------------------------------------------
    fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float) {
        val json = JSONObject().apply {
            put("sensitivity", sensitivity)
            put("clickThreshold", clickThreshold)
        }
        prefs.edit().putString("profile_$name", json.toString()).apply()
    }
    fun getProfileSensitivity(name: String): Float {
        val json = prefs.getString("profile_$name", null) ?: return 0.5f
        return try { JSONObject(json).getDouble("sensitivity").toFloat() } catch (_: Exception) { 0.5f }
    }
    fun getProfileClickThreshold(name: String): Float {
        val json = prefs.getString("profile_$name", null) ?: return 10f
        return try { JSONObject(json).getDouble("clickThreshold").toFloat() } catch (_: Exception) { 10f }
    }
    fun deleteProfile(name: String) = prefs.edit().remove("profile_$name").apply()
    fun getAllProfileNames(): List<String> = prefs.all.keys.filter { it.startsWith("profile_") }
        .map { it.removePrefix("profile_") }.toList()

    // ----------------------------------------------------------------------
    // Themes
    // ----------------------------------------------------------------------
    fun getTheme(): String = prefs.getString("theme", "system") ?: "system"
    fun setTheme(theme: String) = prefs.edit().putString("theme", theme).apply()

    // ----------------------------------------------------------------------
    // Accessibility
    // ----------------------------------------------------------------------
    fun isAnnounceMovementEnabled(): Boolean = prefs.getBoolean("announce_movement", false)
    fun setAnnounceMovementEnabled(enabled: Boolean) = prefs.edit().putBoolean("announce_movement", enabled).apply()

    fun isAnnounceClicksEnabled(): Boolean = prefs.getBoolean("announce_clicks", false)
    fun setAnnounceClicksEnabled(enabled: Boolean) = prefs.edit().putBoolean("announce_clicks", enabled).apply()

    // ----------------------------------------------------------------------
    // Edge gestures
    // ----------------------------------------------------------------------
    fun isEdgeGesturesEnabled(): Boolean = prefs.getBoolean("edge_gestures", false)
    fun setEdgeGesturesEnabled(enabled: Boolean) = prefs.edit().putBoolean("edge_gestures", enabled).apply()

    // ----------------------------------------------------------------------
    // Server communication logs
    // ----------------------------------------------------------------------
    fun addServerLog(entry: String) {
        val logs = getServerLogs().toMutableList()
        logs.add(0, entry)
        while (logs.size > 200) logs.removeAt(logs.lastIndex)
        prefs.edit().putString("server_logs", logs.joinToString("\n")).apply()
    }
    fun getServerLogs(): List<String> {
        val data = prefs.getString("server_logs", "") ?: ""
        return data.split("\n").filter { it.isNotBlank() }
    }
    fun clearServerLogs() = prefs.edit().remove("server_logs").apply()

    // ----------------------------------------------------------------------
    // Custom gestures
    // ----------------------------------------------------------------------
    fun saveCustomGesture(action: String, value: Float) {
        prefs.edit().putFloat("custom_gesture_${action.replace(" ", "_")}", value).apply()
    }
    fun getCustomGesture(action: String): Float = prefs.getFloat("custom_gesture_${action.replace(" ", "_")}", 0f)

    // ----------------------------------------------------------------------
    // Onboarding
    // ----------------------------------------------------------------------
    fun isOnboardingCompleted(): Boolean = prefs.getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) = prefs.edit().putBoolean("onboarding_completed", completed).apply()
}