package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central preferences manager for Air Mouse Pro.
 * Handles all user settings, calibration data, statistics, and profiles.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "airmouse_prefs"

        // Default values
        private const val DEFAULT_SENSITIVITY = 1.0f
        private const val DEFAULT_CLICK_THRESHOLD = 8f
        private const val DEFAULT_DOUBLE_CLICK_INTERVAL = 400L
        private const val DEFAULT_SCROLL_THRESHOLD = 6f
        private const val DEFAULT_RIGHT_CLICK_TILT = 45f
        private const val DEFAULT_RIGHT_CLICK_DURATION = 500L
        private const val DEFAULT_GESTURE_COOLDOWN = 500L
        private const val DEFAULT_SWIPE_THRESHOLD = 15f
        private const val DEFAULT_SCROLL_DEBOUNCE = 2f
        private const val DEFAULT_ACCELERATION_FACTOR = 1.5f
        private const val DEFAULT_SMOOTHING_FACTOR = 0.5f
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_FONT_SIZE = 16f
        private const val DEFAULT_WAKE_WORD = "hey air mouse"
        private const val DEFAULT_VOICE_LANGUAGE = "en-US"
        private const val DEFAULT_PROXIMITY_NEAR = 1.5f
        private const val DEFAULT_PROXIMITY_FAR = 3.0f
        private const val DEFAULT_EDGE_SENSITIVITY = 0.2f
        private const val DEFAULT_BATTERY_SAVER_IDLE = 10000L
        private const val DEFAULT_HAPTIC_STRENGTH = "MEDIUM"
        private const val DEFAULT_COLOR_BLIND_MODE = "NONE"
        private const val DEFAULT_LOG_LEVEL = "INFO"
        private const val DEFAULT_CONNECTION_TIMEOUT = 5000
        private const val DEFAULT_RECONNECT_ATTEMPTS = 5
        private const val DEFAULT_AI_BLEND_FACTOR = 0.7f
        private const val DEFAULT_PREDICTION_STRENGTH = 0.5f
        private const val DEFAULT_CURSOR_SPEED = 1.0f
        private const val DEFAULT_VOICE_CONFIDENCE = 0.7f
        private const val DEFAULT_VOICE_SENSITIVITY = 0.5f
        private const val DEFAULT_APP_VERSION = 0
        private const val DEFAULT_LAST_PORT = 8080
        private const val DEFAULT_LAST_PROTOCOL = 0 // 0 = websocket, 1 = tcp
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ==================== Core CRUD ====================
    fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun getString(key: String, defaultValue: String = ""): String = prefs.getString(key, defaultValue) ?: defaultValue

    fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    fun getInt(key: String, defaultValue: Int = 0): Int = prefs.getInt(key, defaultValue)

    fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    fun getLong(key: String, defaultValue: Long = 0L): Long = prefs.getLong(key, defaultValue)

    fun putFloat(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
    fun getFloat(key: String, defaultValue: Float = 0f): Float = prefs.getFloat(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = prefs.getBoolean(key, defaultValue)

    fun remove(key: String) { prefs.edit().remove(key).apply() }
    fun clear() { prefs.edit().clear().apply() }
    fun contains(key: String): Boolean = prefs.contains(key)

    // ==================== Cursor & Mouse Settings ====================
    fun getSensitivity(): Float = getFloat("sensitivity", DEFAULT_SENSITIVITY)
    fun setSensitivity(value: Float) { putFloat("sensitivity", value.coerceIn(0.2f, 3.0f)) }

    fun getCursorSpeed(): Float = getFloat("cursor_speed", DEFAULT_CURSOR_SPEED)
    fun setCursorSpeed(value: Float) { putFloat("cursor_speed", value.coerceIn(0.5f, 3.0f)) }

    fun isInvertX(): Boolean = getBoolean("invert_x", false)
    fun setInvertX(invert: Boolean) { putBoolean("invert_x", invert) }

    fun isInvertY(): Boolean = getBoolean("invert_y", false)
    fun setInvertY(invert: Boolean) { putBoolean("invert_y", invert) }

    fun isAccelerationEnabled(): Boolean = getBoolean("acceleration_enabled", true)
    fun setAccelerationEnabled(enabled: Boolean) { putBoolean("acceleration_enabled", enabled) }

    fun getAccelerationFactor(): Float = getFloat("acceleration_factor", DEFAULT_ACCELERATION_FACTOR)
    fun setAccelerationFactor(value: Float) { putFloat("acceleration_factor", value.coerceIn(1.0f, 3.0f)) }

    fun isSmoothingEnabled(): Boolean = getBoolean("smoothing_enabled", true)
    fun setSmoothingEnabled(enabled: Boolean) { putBoolean("smoothing_enabled", enabled) }

    fun getSmoothingFactor(): Float = getFloat("smoothing_factor", DEFAULT_SMOOTHING_FACTOR)
    fun setSmoothingFactor(value: Float) { putFloat("smoothing_factor", value.coerceIn(0.1f, 0.9f)) }

    // ==================== Gesture & Click Settings ====================
    fun getClickThreshold(): Float = getFloat("click_threshold", DEFAULT_CLICK_THRESHOLD)
    fun setClickThreshold(value: Float) { putFloat("click_threshold", value.coerceIn(3f, 20f)) }

    fun getDoubleClickInterval(): Long = getLong("double_click_interval", DEFAULT_DOUBLE_CLICK_INTERVAL)
    fun setDoubleClickInterval(value: Long) { putLong("double_click_interval", value.coerceIn(200L, 800L)) }

    fun getScrollThreshold(): Float = getFloat("scroll_threshold", DEFAULT_SCROLL_THRESHOLD)
    fun setScrollThreshold(value: Float) { putFloat("scroll_threshold", value.coerceIn(3f, 20f)) }

    fun getScrollDebounce(): Float = getFloat("scroll_debounce", DEFAULT_SCROLL_DEBOUNCE)
    fun setScrollDebounce(value: Float) { putFloat("scroll_debounce", value.coerceIn(1f, 5f)) }

    fun getRightClickTilt(): Float = getFloat("right_click_tilt", DEFAULT_RIGHT_CLICK_TILT)
    fun setRightClickTilt(value: Float) { putFloat("right_click_tilt", value.coerceIn(20f, 80f)) }

    fun getRightClickDuration(): Long = getLong("right_click_duration", DEFAULT_RIGHT_CLICK_DURATION)
    fun setRightClickDuration(value: Long) { putLong("right_click_duration", value.coerceIn(300L, 1000L)) }

    fun getSwipeThreshold(): Float = getFloat("swipe_threshold", DEFAULT_SWIPE_THRESHOLD)
    fun setSwipeThreshold(value: Float) { putFloat("swipe_threshold", value.coerceIn(8f, 30f)) }

    fun getGestureCooldown(): Long = getLong("gesture_cooldown", DEFAULT_GESTURE_COOLDOWN)
    fun setGestureCooldown(value: Long) { putLong("gesture_cooldown", value.coerceIn(200L, 1000L)) }

    fun isHapticEnabled(): Boolean = getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) { putBoolean("haptic_enabled", enabled) }

    fun getHapticStrength(): String = getString("haptic_strength", DEFAULT_HAPTIC_STRENGTH)
    fun setHapticStrength(strength: String) { putString("haptic_strength", strength) }

    // ==================== Connection Settings ====================
    fun getLastServerIp(): String = getString("last_server_ip", "")
    fun setLastServerIp(ip: String) { putString("last_server_ip", ip) }

    fun getLastServerPort(): Int = getInt("last_server_port", DEFAULT_LAST_PORT)
    fun setLastServerPort(port: Int) { putInt("last_server_port", port.coerceIn(1, 65535)) }

    fun getLastProtocol(): Int = getInt("last_protocol", DEFAULT_LAST_PROTOCOL)
    fun setLastProtocol(protocol: Int) { putInt("last_protocol", protocol.coerceIn(0, 1)) }

    fun getServerMac(): String = getString("server_mac", "")
    fun setServerMac(mac: String) { putString("server_mac", mac) }

    fun isAutoConnect(): Boolean = getBoolean("auto_connect", true)
    fun setAutoConnect(enabled: Boolean) { putBoolean("auto_connect", enabled) }

    fun getReconnectAttempts(): Int = getInt("reconnect_attempts", DEFAULT_RECONNECT_ATTEMPTS)
    fun setReconnectAttempts(attempts: Int) { putInt("reconnect_attempts", attempts.coerceIn(1, 20)) }

    fun getConnectionTimeout(): Int = getInt("connection_timeout", DEFAULT_CONNECTION_TIMEOUT)
    fun setConnectionTimeout(timeout: Int) { putInt("connection_timeout", timeout.coerceIn(1000, 30000)) }

    fun isWebSocketEnabled(): Boolean = getBoolean("use_websocket", true)
    fun setWebSocketEnabled(enabled: Boolean) { putBoolean("use_websocket", enabled) }

    fun isUdpDiscoveryEnabled(): Boolean = getBoolean("use_udp_discovery", true)
    fun setUdpDiscoveryEnabled(enabled: Boolean) { putBoolean("use_udp_discovery", enabled) }

    // ==================== Theme & Appearance ====================
    fun getTheme(): String = getString("theme", DEFAULT_THEME)
    fun setTheme(theme: String) { putString("theme", theme) }

    fun isDynamicColorsEnabled(): Boolean = getBoolean("dynamic_colors", true)
    fun setDynamicColorsEnabled(enabled: Boolean) { putBoolean("dynamic_colors", enabled) }

    fun getFontSize(): Float = getFloat("font_size", DEFAULT_FONT_SIZE)
    fun setFontSize(value: Float) { putFloat("font_size", value.coerceIn(12f, 24f)) }

    fun isDebugInfoEnabled(): Boolean = getBoolean("show_debug_info", false)
    fun setDebugInfoEnabled(enabled: Boolean) { putBoolean("show_debug_info", enabled) }

    fun isKeepScreenOn(): Boolean = getBoolean("keep_screen_on", false)
    fun setKeepScreenOn(enabled: Boolean) { putBoolean("keep_screen_on", enabled) }

    fun isShowFps(): Boolean = getBoolean("show_fps", false)
    fun setShowFps(enabled: Boolean) { putBoolean("show_fps", enabled) }

    // ==================== Calibration ====================
    fun isCalibrated(): Boolean = getBoolean("calibration_complete", false)
    fun setCalibrated(calibrated: Boolean) { putBoolean("calibration_complete", calibrated) }

    fun getCalibrationQuality(): Float = getFloat("calibration_quality", 0f)
    fun setCalibrationQuality(quality: Float) { putFloat("calibration_quality", quality.coerceIn(0f, 100f)) }

    fun getLastCalibrationTime(): Long = getLong("calibration_complete_time", 0)
    fun setLastCalibrationTime(time: Long) { putLong("calibration_complete_time", time) }

    fun getCalibrationAttempts(): Int = getInt("calibration_attempts", 5)
    fun setCalibrationAttempts(attempts: Int) { putInt("calibration_attempts", attempts.coerceIn(0, 10)) }
    fun decrementCalibrationAttempts() { setCalibrationAttempts(getCalibrationAttempts() - 1) }

    // Gyroscope
    fun saveGyroBias(bias: FloatArray) {
        putFloat("gyro_bias_x", bias.getOrElse(0) { 0f })
        putFloat("gyro_bias_y", bias.getOrElse(1) { 0f })
        putFloat("gyro_bias_z", bias.getOrElse(2) { 0f })
        // Legacy keys
        putFloat("gyro_offset_x", bias.getOrElse(0) { 0f })
        putFloat("gyro_offset_y", bias.getOrElse(1) { 0f })
        putFloat("gyro_offset_z", bias.getOrElse(2) { 0f })
    }

    fun getGyroBias(): FloatArray = floatArrayOf(
        getFloat("gyro_bias_x", 0f),
        getFloat("gyro_bias_y", 0f),
        getFloat("gyro_bias_z", 0f)
    )
    fun getGyroOffsetX(): Float = getFloat("gyro_offset_x", 0f)
    fun getGyroOffsetY(): Float = getFloat("gyro_offset_y", 0f)
    fun getGyroOffsetZ(): Float = getFloat("gyro_offset_z", 0f)

    // Accelerometer
    fun saveAccelParams(offset: FloatArray, scale: FloatArray) {
        putFloat("accel_offset_x", offset.getOrElse(0) { 0f })
        putFloat("accel_offset_y", offset.getOrElse(1) { 0f })
        putFloat("accel_offset_z", offset.getOrElse(2) { 0f })
        putFloat("accel_scale_x", scale.getOrElse(0) { 1f })
        putFloat("accel_scale_y", scale.getOrElse(1) { 1f })
        putFloat("accel_scale_z", scale.getOrElse(2) { 1f })
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

    // Magnetometer
    fun saveMagCalibration(offset: FloatArray, scale: FloatArray) {
        putFloat("mag_offset_x", offset.getOrElse(0) { 0f })
        putFloat("mag_offset_y", offset.getOrElse(1) { 0f })
        putFloat("mag_offset_z", offset.getOrElse(2) { 0f })
        putFloat("mag_scale_x", scale.getOrElse(0) { 1f })
        putFloat("mag_scale_y", scale.getOrElse(1) { 1f })
        putFloat("mag_scale_z", scale.getOrElse(2) { 1f })
    }

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

    // ==================== AI & Predictive ====================
    fun isAiSmoothingEnabled(): Boolean = getBoolean("ai_smoothing", false)
    fun setAiSmoothingEnabled(enabled: Boolean) { putBoolean("ai_smoothing", enabled) }

    fun getAiBlendFactor(): Float = getFloat("ai_blend_factor", DEFAULT_AI_BLEND_FACTOR)
    fun setAiBlendFactor(value: Float) { putFloat("ai_blend_factor", value.coerceIn(0.3f, 0.95f)) }

    fun isPredictiveEnabled(): Boolean = getBoolean("predictive_movement", true)
    fun setPredictiveEnabled(enabled: Boolean) { putBoolean("predictive_movement", enabled) }

    fun getPredictionStrength(): Float = getFloat("prediction_strength", DEFAULT_PREDICTION_STRENGTH)
    fun setPredictionStrength(value: Float) { putFloat("prediction_strength", value.coerceIn(0.1f, 0.9f)) }

    fun isKalmanEnabled(): Boolean = getBoolean("kalman_enabled", true)
    fun setKalmanEnabled(enabled: Boolean) { putBoolean("kalman_enabled", enabled) }

    // ==================== Feedback & Sound ====================
    fun isSoundEnabled(): Boolean = getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) { putBoolean("sound_enabled", enabled) }

    fun isVisualFeedbackEnabled(): Boolean = getBoolean("visual_feedback", true)
    fun setVisualFeedbackEnabled(enabled: Boolean) { putBoolean("visual_feedback", enabled) }

    fun isNotificationOnGesture(): Boolean = getBoolean("notification_on_gesture", false)
    fun setNotificationOnGesture(enabled: Boolean) { putBoolean("notification_on_gesture", enabled) }

    // ==================== Proximity ====================
    fun isProximityEnabled(): Boolean = getBoolean("proximity_enabled", false)
    fun setProximityEnabled(enabled: Boolean) { putBoolean("proximity_enabled", enabled) }

    fun getProximityNearThreshold(): Float = getFloat("proximity_near", DEFAULT_PROXIMITY_NEAR)
    fun setProximityNearThreshold(value: Float) { putFloat("proximity_near", value.coerceIn(0.5f, 5f)) }

    fun getProximityFarThreshold(): Float = getFloat("proximity_far", DEFAULT_PROXIMITY_FAR)
    fun setProximityFarThreshold(value: Float) { putFloat("proximity_far", value.coerceIn(1f, 10f)) }

    fun isProximityVibrationEnabled(): Boolean = getBoolean("proximity_vibration", true)
    fun setProximityVibrationEnabled(enabled: Boolean) { putBoolean("proximity_vibration", enabled) }

    fun isProximityNotificationEnabled(): Boolean = getBoolean("proximity_notification", true)
    fun setProximityNotificationEnabled(enabled: Boolean) { putBoolean("proximity_notification", enabled) }

    // ==================== Voice Commands ====================
    fun isVoiceEnabled(): Boolean = getBoolean("voice_enabled", false)
    fun setVoiceEnabled(enabled: Boolean) { putBoolean("voice_enabled", enabled) }

    fun isWakeWordEnabled(): Boolean = getBoolean("wake_word_enabled", true)
    fun setWakeWordEnabled(enabled: Boolean) { putBoolean("wake_word_enabled", enabled) }

    fun getWakeWord(): String = getString("wake_word", DEFAULT_WAKE_WORD)
    fun setWakeWord(word: String) { putString("wake_word", word.lowercase().trim()) }

    fun getVoiceWakeWordConfidence(): Float = getFloat("voice_wake_word_confidence", DEFAULT_VOICE_CONFIDENCE)
    fun setVoiceWakeWordConfidence(confidence: Float) { putFloat("voice_wake_word_confidence", confidence.coerceIn(0.5f, 0.95f)) }

    fun isVoiceContinuousListening(): Boolean = getBoolean("voice_continuous", false)
    fun setVoiceContinuousListening(enabled: Boolean) { putBoolean("voice_continuous", enabled) }

    fun isVoiceFeedbackEnabled(): Boolean = getBoolean("voice_feedback", true)
    fun setVoiceFeedbackEnabled(enabled: Boolean) { putBoolean("voice_feedback", enabled) }

    fun isVoiceSoundEffectsEnabled(): Boolean = getBoolean("voice_sound_effects", true)
    fun setVoiceSoundEffectsEnabled(enabled: Boolean) { putBoolean("voice_sound_effects", enabled) }

    fun getVoiceLanguage(): String = getString("voice_language", DEFAULT_VOICE_LANGUAGE)
    fun setVoiceLanguage(language: String) { putString("voice_language", language) }

    fun getVoiceSensitivity(): Float = getFloat("voice_sensitivity", DEFAULT_VOICE_SENSITIVITY)
    fun setVoiceSensitivity(sensitivity: Float) { putFloat("voice_sensitivity", sensitivity.coerceIn(0.2f, 0.9f)) }

    // ==================== Edge Gestures ====================
    fun isEdgeGesturesEnabled(): Boolean = getBoolean("edge_gestures_enabled", false)
    fun setEdgeGesturesEnabled(enabled: Boolean) { putBoolean("edge_gestures_enabled", enabled) }

    fun getEdgeVolumeUpAction(): String = getString("edge_gestures_volume_up", "Left Click")
    fun setEdgeVolumeUpAction(action: String) { putString("edge_gestures_volume_up", action) }

    fun getEdgeVolumeDownAction(): String = getString("edge_gestures_volume_down", "Right Click")
    fun setEdgeVolumeDownAction(action: String) { putString("edge_gestures_volume_down", action) }

    fun getEdgeLongPressAction(): String = getString("edge_gestures_long_press", "Scroll")
    fun setEdgeLongPressAction(action: String) { putString("edge_gestures_long_press", action) }

    fun getEdgeSensitivity(): Float = getFloat("edge_gestures_sensitivity", DEFAULT_EDGE_SENSITIVITY)
    fun setEdgeSensitivity(sensitivity: Float) { putFloat("edge_gestures_sensitivity", sensitivity.coerceIn(0.1f, 0.5f)) }

    // ==================== Battery Saver ====================
    fun isBatterySaverEnabled(): Boolean = getBoolean("battery_saver_enabled", true)
    fun setBatterySaverEnabled(enabled: Boolean) { putBoolean("battery_saver_enabled", enabled) }

    fun getBatterySaverIdleTime(): Long = getLong("battery_saver_idle_time", DEFAULT_BATTERY_SAVER_IDLE)
    fun setBatterySaverIdleTime(time: Long) { putLong("battery_saver_idle_time", time.coerceIn(5000L, 30000L)) }

    // ==================== Accessibility ====================
    fun isAnnounceMovementEnabled(): Boolean = getBoolean("announce_movement", false)
    fun setAnnounceMovementEnabled(enabled: Boolean) { putBoolean("announce_movement", enabled) }

    fun isAnnounceClicksEnabled(): Boolean = getBoolean("announce_clicks", false)
    fun setAnnounceClicksEnabled(enabled: Boolean) { putBoolean("announce_clicks", enabled) }

    fun isHighContrastMode(): Boolean = getBoolean("high_contrast", false)
    fun setHighContrastMode(enabled: Boolean) { putBoolean("high_contrast", enabled) }

    fun isLargeTextEnabled(): Boolean = getBoolean("large_text", false)
    fun setLargeTextEnabled(enabled: Boolean) { putBoolean("large_text", enabled) }

    fun isReduceMotionEnabled(): Boolean = getBoolean("reduce_motion", false)
    fun setReduceMotionEnabled(enabled: Boolean) { putBoolean("reduce_motion", enabled) }

    fun getColorBlindMode(): String = getString("color_blind_mode", DEFAULT_COLOR_BLIND_MODE)
    fun setColorBlindMode(mode: String) { putString("color_blind_mode", mode.uppercase()) }

    // ==================== Privacy ====================
    fun isAnonymousStatsEnabled(): Boolean = getBoolean("anonymous_stats", true)
    fun setAnonymousStatsEnabled(enabled: Boolean) { putBoolean("anonymous_stats", enabled) }

    fun isCrashReportingEnabled(): Boolean = getBoolean("crash_reporting", true)
    fun setCrashReportingEnabled(enabled: Boolean) { putBoolean("crash_reporting", enabled) }

    fun isClearDataOnExit(): Boolean = getBoolean("clear_data_on_exit", false)
    fun setClearDataOnExit(enabled: Boolean) { putBoolean("clear_data_on_exit", enabled) }

    // ==================== Onboarding ====================
    fun isOnboardingCompleted(): Boolean = getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) { putBoolean("onboarding_completed", completed) }

    fun getUserName(): String = getString("user_name", "")
    fun setUserName(name: String) { putString("user_name", name) }

    fun isFirstLaunch(): Boolean = getBoolean("first_launch", true)
    fun setFirstLaunchComplete() { putBoolean("first_launch", false) }

    fun getAppVersion(): Int = getInt("app_version", DEFAULT_APP_VERSION)
    fun setAppVersion(version: Int) { putInt("app_version", version) }

    // ==================== Statistics ====================
    fun incrementStat(key: String) {
        val current = getInt("stat_$key", 0)
        putInt("stat_$key", current + 1)
    }

    fun getStat(key: String): Int = getInt("stat_$key", 0)

    fun getClickCount(): Int = getStat("clicks")
    fun incrementClickCount() = incrementStat("clicks")

    fun getDoubleClickCount(): Int = getStat("double_clicks")
    fun incrementDoubleClickCount() = incrementStat("double_clicks")

    fun getRightClickCount(): Int = getStat("right_clicks")
    fun incrementRightClickCount() = incrementStat("right_clicks")

    fun getScrollCount(): Int = getStat("scrolls")
    fun incrementScrollCount() = incrementStat("scrolls")

    fun getTotalDistance(): Float = getFloat("stat_total_distance", 0f)
    fun addDistance(distance: Float) { putFloat("stat_total_distance", getTotalDistance() + distance) }

    fun getTotalMovements(): Int = getInt("stat_total_movements", 0)
    fun incrementMovements() { putInt("stat_total_movements", getTotalMovements() + 1) }

    fun getSessionTime(): Long = getLong("stat_session_time", 0L)
    fun addSessionTime(time: Long) { putLong("stat_session_time", getSessionTime() + time) }

    fun getTotalGestures(): Int = getInt("stat_gestures", 0)
    fun incrementTotalGestures() { putInt("stat_gestures", getTotalGestures() + 1) }

    fun resetStatistics() {
        putInt("stat_clicks", 0)
        putInt("stat_double_clicks", 0)
        putInt("stat_right_clicks", 0)
        putInt("stat_scrolls", 0)
        putFloat("stat_total_distance", 0f)
        putInt("stat_total_movements", 0)
        putLong("stat_session_time", 0L)
        putInt("stat_gestures", 0)
        // Reset all gesture-specific stats
        val allKeys = prefs.all.keys
        allKeys.filter { it.startsWith("stat_gesture_") }.forEach { remove(it) }
    }

    fun incrementGestureStat(gesture: String) {
        val key = "stat_gesture_$gesture"
        val current = getInt(key, 0)
        putInt(key, current + 1)
        incrementTotalGestures()
    }

    // ==================== Profiles ====================
    fun getLastUsedProfile(): String = getString("last_used_profile", "Default")
    fun setLastUsedProfile(profile: String) { putString("last_used_profile", profile) }

    fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float, scrollThreshold: Float) {
        putFloat("profile_${name}_sensitivity", sensitivity)
        putFloat("profile_${name}_click_threshold", clickThreshold)
        putFloat("profile_${name}_scroll_threshold", scrollThreshold)
        // add more fields as needed
    }

    fun getProfileSensitivity(name: String): Float = getFloat("profile_${name}_sensitivity", DEFAULT_SENSITIVITY)
    fun getProfileClickThreshold(name: String): Float = getFloat("profile_${name}_click_threshold", DEFAULT_CLICK_THRESHOLD)
    fun getProfileScrollThreshold(name: String): Float = getFloat("profile_${name}_scroll_threshold", DEFAULT_SCROLL_THRESHOLD)

    fun getAllProfileNames(): List<String> {
        val allKeys = prefs.all.keys
        return allKeys.filter { it.startsWith("profile_") && it.endsWith("_sensitivity") }
            .map { it.removePrefix("profile_").removeSuffix("_sensitivity") }
            .distinct()
    }

    fun deleteProfile(name: String) {
        remove("profile_${name}_sensitivity")
        remove("profile_${name}_click_threshold")
        remove("profile_${name}_scroll_threshold")
        // remove other profile fields if any
    }

    fun renameProfile(oldName: String, newName: String) {
        val sensitivity = getProfileSensitivity(oldName)
        val clickThreshold = getProfileClickThreshold(oldName)
        val scrollThreshold = getProfileScrollThreshold(oldName)
        saveProfile(newName, sensitivity, clickThreshold, scrollThreshold)
        deleteProfile(oldName)
    }

    // ==================== Custom Gestures ====================
    fun saveCustomGesture(name: String, data: String) { putString("gesture_$name", data) }
    fun getCustomGesture(name: String): String = getString("gesture_$name", "")
    fun deleteCustomGesture(name: String) { remove("gesture_$name") }
    fun getAllCustomGestures(): Map<String, String> {
        val allKeys = prefs.all.keys
        return allKeys.filter { it.startsWith("gesture_") }
            .associate { it.removePrefix("gesture_") to getString(it, "") }
    }

    // ==================== Recorded Gestures (for training) ====================
    fun getRecordedGestures(): List<String> =
        getString("recorded_gestures", "").split(",").filter { it.isNotEmpty() }

    fun addRecordedGesture(fileName: String) {
        val current = getRecordedGestures()
        if (!current.contains(fileName)) {
            val newList = (current + fileName).joinToString(",")
            putString("recorded_gestures", newList)
        }
    }

    fun clearRecordedGestures() { remove("recorded_gestures") }

    // ==================== Developer Settings ====================
    fun isDeveloperModeEnabled(): Boolean = getBoolean("developer_mode", false)
    fun setDeveloperModeEnabled(enabled: Boolean) { putBoolean("developer_mode", enabled) }

    fun isExperimentalFeaturesEnabled(): Boolean = getBoolean("experimental_features", false)
    fun setExperimentalFeaturesEnabled(enabled: Boolean) { putBoolean("experimental_features", enabled) }

    fun getLogLevel(): String = getString("log_level", DEFAULT_LOG_LEVEL)
    fun setLogLevel(level: String) { putString("log_level", level.uppercase()) }

    // ==================== Export / Import ====================
    fun exportSettings(): String {
        return buildString {
            appendLine("AIRMOUSE_SETTINGS_EXPORT")
            appendLine("version=2")
            appendLine("export_time=${System.currentTimeMillis()}")
            appendLine("sensitivity=${getSensitivity()}")
            appendLine("cursor_speed=${getCursorSpeed()}")
            appendLine("invert_x=${isInvertX()}")
            appendLine("invert_y=${isInvertY()}")
            appendLine("acceleration_enabled=${isAccelerationEnabled()}")
            appendLine("acceleration_factor=${getAccelerationFactor()}")
            appendLine("smoothing_enabled=${isSmoothingEnabled()}")
            appendLine("smoothing_factor=${getSmoothingFactor()}")
            appendLine("click_threshold=${getClickThreshold()}")
            appendLine("double_click_interval=${getDoubleClickInterval()}")
            appendLine("scroll_threshold=${getScrollThreshold()}")
            appendLine("scroll_debounce=${getScrollDebounce()}")
            appendLine("right_click_tilt=${getRightClickTilt()}")
            appendLine("right_click_duration=${getRightClickDuration()}")
            appendLine("swipe_threshold=${getSwipeThreshold()}")
            appendLine("gesture_cooldown=${getGestureCooldown()}")
            appendLine("haptic_enabled=${isHapticEnabled()}")
            appendLine("haptic_strength=${getHapticStrength()}")
            appendLine("theme=${getTheme()}")
            appendLine("dynamic_colors=${isDynamicColorsEnabled()}")
            appendLine("font_size=${getFontSize()}")
            appendLine("show_debug_info=${isDebugInfoEnabled()}")
            appendLine("keep_screen_on=${isKeepScreenOn()}")
            appendLine("show_fps=${isShowFps()}")
            appendLine("ai_smoothing=${isAiSmoothingEnabled()}")
            appendLine("ai_blend_factor=${getAiBlendFactor()}")
            appendLine("predictive_movement=${isPredictiveEnabled()}")
            appendLine("prediction_strength=${getPredictionStrength()}")
            appendLine("kalman_enabled=${isKalmanEnabled()}")
            appendLine("sound_enabled=${isSoundEnabled()}")
            appendLine("visual_feedback=${isVisualFeedbackEnabled()}")
            appendLine("notification_on_gesture=${isNotificationOnGesture()}")
            appendLine("auto_connect=${isAutoConnect()}")
            appendLine("reconnect_attempts=${getReconnectAttempts()}")
            appendLine("connection_timeout=${getConnectionTimeout()}")
            appendLine("use_websocket=${isWebSocketEnabled()}")
            appendLine("use_udp_discovery=${isUdpDiscoveryEnabled()}")
            appendLine("proximity_enabled=${isProximityEnabled()}")
            appendLine("proximity_near=${getProximityNearThreshold()}")
            appendLine("proximity_far=${getProximityFarThreshold()}")
            appendLine("proximity_vibration=${isProximityVibrationEnabled()}")
            appendLine("proximity_notification=${isProximityNotificationEnabled()}")
            appendLine("voice_enabled=${isVoiceEnabled()}")
            appendLine("wake_word_enabled=${isWakeWordEnabled()}")
            appendLine("wake_word=${getWakeWord()}")
            appendLine("voice_wake_word_confidence=${getVoiceWakeWordConfidence()}")
            appendLine("voice_continuous=${isVoiceContinuousListening()}")
            appendLine("voice_feedback=${isVoiceFeedbackEnabled()}")
            appendLine("voice_sound_effects=${isVoiceSoundEffectsEnabled()}")
            appendLine("voice_language=${getVoiceLanguage()}")
            appendLine("voice_sensitivity=${getVoiceSensitivity()}")
            appendLine("edge_gestures_enabled=${isEdgeGesturesEnabled()}")
            appendLine("edge_gestures_volume_up=${getEdgeVolumeUpAction()}")
            appendLine("edge_gestures_volume_down=${getEdgeVolumeDownAction()}")
            appendLine("edge_gestures_long_press=${getEdgeLongPressAction()}")
            appendLine("edge_sensitivity=${getEdgeSensitivity()}")
            appendLine("battery_saver_enabled=${isBatterySaverEnabled()}")
            appendLine("battery_saver_idle_time=${getBatterySaverIdleTime()}")
            appendLine("announce_movement=${isAnnounceMovementEnabled()}")
            appendLine("announce_clicks=${isAnnounceClicksEnabled()}")
            appendLine("high_contrast=${isHighContrastMode()}")
            appendLine("large_text=${isLargeTextEnabled()}")
            appendLine("reduce_motion=${isReduceMotionEnabled()}")
            appendLine("color_blind_mode=${getColorBlindMode()}")
            appendLine("anonymous_stats=${isAnonymousStatsEnabled()}")
            appendLine("crash_reporting=${isCrashReportingEnabled()}")
            appendLine("clear_data_on_exit=${isClearDataOnExit()}")
            appendLine("developer_mode=${isDeveloperModeEnabled()}")
            appendLine("experimental_features=${isExperimentalFeaturesEnabled()}")
            appendLine("log_level=${getLogLevel()}")
        }
    }

    fun importSettings(data: String): Boolean {
        return try {
            val lines = data.lines()
            if (lines.firstOrNull() != "AIRMOUSE_SETTINGS_EXPORT") return false
            for (line in lines) {
                when {
                    line.startsWith("sensitivity=") -> setSensitivity(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("cursor_speed=") -> setCursorSpeed(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("invert_x=") -> setInvertX(line.substringAfter("=").toBoolean())
                    line.startsWith("invert_y=") -> setInvertY(line.substringAfter("=").toBoolean())
                    line.startsWith("acceleration_enabled=") -> setAccelerationEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("acceleration_factor=") -> setAccelerationFactor(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("smoothing_enabled=") -> setSmoothingEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("smoothing_factor=") -> setSmoothingFactor(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("click_threshold=") -> setClickThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("double_click_interval=") -> setDoubleClickInterval(line.substringAfter("=").toLongOrNull() ?: continue)
                    line.startsWith("scroll_threshold=") -> setScrollThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("scroll_debounce=") -> setScrollDebounce(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("right_click_tilt=") -> setRightClickTilt(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("right_click_duration=") -> setRightClickDuration(line.substringAfter("=").toLongOrNull() ?: continue)
                    line.startsWith("swipe_threshold=") -> setSwipeThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("gesture_cooldown=") -> setGestureCooldown(line.substringAfter("=").toLongOrNull() ?: continue)
                    line.startsWith("haptic_enabled=") -> setHapticEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("haptic_strength=") -> setHapticStrength(line.substringAfter("="))
                    line.startsWith("theme=") -> setTheme(line.substringAfter("="))
                    line.startsWith("dynamic_colors=") -> setDynamicColorsEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("font_size=") -> setFontSize(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("show_debug_info=") -> setDebugInfoEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("keep_screen_on=") -> setKeepScreenOn(line.substringAfter("=").toBoolean())
                    line.startsWith("show_fps=") -> setShowFps(line.substringAfter("=").toBoolean())
                    line.startsWith("ai_smoothing=") -> setAiSmoothingEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("ai_blend_factor=") -> setAiBlendFactor(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("predictive_movement=") -> setPredictiveEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("prediction_strength=") -> setPredictionStrength(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("kalman_enabled=") -> setKalmanEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("sound_enabled=") -> setSoundEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("visual_feedback=") -> setVisualFeedbackEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("notification_on_gesture=") -> setNotificationOnGesture(line.substringAfter("=").toBoolean())
                    line.startsWith("auto_connect=") -> setAutoConnect(line.substringAfter("=").toBoolean())
                    line.startsWith("reconnect_attempts=") -> setReconnectAttempts(line.substringAfter("=").toIntOrNull() ?: continue)
                    line.startsWith("connection_timeout=") -> setConnectionTimeout(line.substringAfter("=").toIntOrNull() ?: continue)
                    line.startsWith("use_websocket=") -> setWebSocketEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("use_udp_discovery=") -> setUdpDiscoveryEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("proximity_enabled=") -> setProximityEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("proximity_near=") -> setProximityNearThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("proximity_far=") -> setProximityFarThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("proximity_vibration=") -> setProximityVibrationEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("proximity_notification=") -> setProximityNotificationEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_enabled=") -> setVoiceEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("wake_word_enabled=") -> setWakeWordEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("wake_word=") -> setWakeWord(line.substringAfter("="))
                    line.startsWith("voice_wake_word_confidence=") -> setVoiceWakeWordConfidence(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("voice_continuous=") -> setVoiceContinuousListening(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_feedback=") -> setVoiceFeedbackEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_sound_effects=") -> setVoiceSoundEffectsEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_language=") -> setVoiceLanguage(line.substringAfter("="))
                    line.startsWith("voice_sensitivity=") -> setVoiceSensitivity(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("edge_gestures_enabled=") -> setEdgeGesturesEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("edge_gestures_volume_up=") -> setEdgeVolumeUpAction(line.substringAfter("="))
                    line.startsWith("edge_gestures_volume_down=") -> setEdgeVolumeDownAction(line.substringAfter("="))
                    line.startsWith("edge_gestures_long_press=") -> setEdgeLongPressAction(line.substringAfter("="))
                    line.startsWith("edge_sensitivity=") -> setEdgeSensitivity(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("battery_saver_enabled=") -> setBatterySaverEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("battery_saver_idle_time=") -> setBatterySaverIdleTime(line.substringAfter("=").toLongOrNull() ?: continue)
                    line.startsWith("announce_movement=") -> setAnnounceMovementEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("announce_clicks=") -> setAnnounceClicksEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("high_contrast=") -> setHighContrastMode(line.substringAfter("=").toBoolean())
                    line.startsWith("large_text=") -> setLargeTextEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("reduce_motion=") -> setReduceMotionEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("color_blind_mode=") -> setColorBlindMode(line.substringAfter("="))
                    line.startsWith("anonymous_stats=") -> setAnonymousStatsEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("crash_reporting=") -> setCrashReportingEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("clear_data_on_exit=") -> setClearDataOnExit(line.substringAfter("=").toBoolean())
                    line.startsWith("developer_mode=") -> setDeveloperModeEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("experimental_features=") -> setExperimentalFeaturesEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("log_level=") -> setLogLevel(line.substringAfter("="))
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}