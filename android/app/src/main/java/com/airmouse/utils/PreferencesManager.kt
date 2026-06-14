// app/src/main/java/com/airmouse/utils/PreferencesManager.kt
package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)

    // ==================== Basic Operations ====================
    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
// Add these methods to PreferencesManager.kt

// Gyroscope calibration
fun getGyroBiasX(): Float = getFloat("gyro_bias_x", 0f)
fun getGyroBiasY(): Float = getFloat("gyro_bias_y", 0f)
fun getGyroBiasZ(): Float = getFloat("gyro_bias_z", 0f)

// Magnetometer calibration
fun getMagOffsetX(): Float = getFloat("mag_offset_x", 0f)
fun getMagOffsetY(): Float = getFloat("mag_offset_y", 0f)
fun getMagOffsetZ(): Float = getFloat("mag_offset_z", 0f)
fun getMagScaleX(): Float = getFloat("mag_scale_x", 1f)
fun getMagScaleY(): Float = getFloat("mag_scale_y", 1f)
fun getMagScaleZ(): Float = getFloat("mag_scale_z", 1f)

// Accelerometer calibration
fun getAccelOffsetX(): Float = getFloat("accel_offset_x", 0f)
fun getAccelOffsetY(): Float = getFloat("accel_offset_y", 0f)
fun getAccelOffsetZ(): Float = getFloat("accel_offset_z", 0f)
fun getAccelScaleX(): Float = getFloat("accel_scale_x", 1f)
fun getAccelScaleY(): Float = getFloat("accel_scale_y", 1f)
fun getAccelScaleZ(): Float = getFloat("accel_scale_z", 1f)

// Status flags
fun isGyroCalibrated(): Boolean = getBoolean("gyro_calibrated", false)
fun isMagCalibrated(): Boolean = getBoolean("mag_calibrated", false)
fun isAccelCalibrated(): Boolean = getBoolean("accel_calibrated", false)
    fun putString(key: String, value: String) = prefs.edit { putString(key, value) }
    fun getString(key: String, defaultValue: String = ""): String = prefs.getString(key, defaultValue) ?: defaultValue

    fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }
    fun getInt(key: String, defaultValue: Int = 0): Int = prefs.getInt(key, defaultValue)

    fun putLong(key: String, value: Long) = prefs.edit { putLong(key, value) }
    fun getLong(key: String, defaultValue: Long = 0L): Long = prefs.getLong(key, defaultValue)

    fun putFloat(key: String, value: Float) = prefs.edit { putFloat(key, value) }
    fun getFloat(key: String, defaultValue: Float = 0f): Float = prefs.getFloat(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = prefs.getBoolean(key, defaultValue)

    fun contains(key: String): Boolean = prefs.contains(key)
    fun remove(key: String) = prefs.edit { remove(key) }
    fun clear() = prefs.edit { clear() }

    // ==================== Cursor Settings ====================

    fun getSensitivity(): Float = getFloat("sensitivity", 0.5f)
    fun setSensitivity(value: Float) = putFloat("sensitivity", value.coerceIn(0.2f, 2.0f))

    fun getClickThreshold(): Float = getFloat("click_threshold", 8f)
    fun setClickThreshold(value: Float) = putFloat("click_threshold", value.coerceIn(3f, 15f))

    fun getDoubleClickInterval(): Long = getLong("double_click_interval", 400L)
    fun setDoubleClickInterval(value: Long) = putLong("double_click_interval", value.coerceIn(200L, 800L))

    fun getScrollThreshold(): Float = getFloat("scroll_threshold", 8f)
    fun setScrollThreshold(value: Float) = putFloat("scroll_threshold", value.coerceIn(3f, 20f))

    fun getScrollDebounce(): Float = getFloat("scroll_debounce", 2f)
    fun setScrollDebounce(value: Float) = putFloat("scroll_debounce", value.coerceIn(1f, 5f))

    fun getRightClickTilt(): Float = getFloat("right_click_tilt", 45f)
    fun setRightClickTilt(value: Float) = putFloat("right_click_tilt", value.coerceIn(20f, 80f))

    fun getRightClickDuration(): Long = getLong("right_click_duration", 500L)
    fun setRightClickDuration(value: Long) = putLong("right_click_duration", value.coerceIn(300L, 1000L))

    fun getSwipeThreshold(): Float = getFloat("swipe_threshold", 15f)
    fun setSwipeThreshold(value: Float) = putFloat("swipe_threshold", value.coerceIn(8f, 30f))

    fun getGestureCooldown(): Long = getLong("gesture_cooldown", 500L)
    fun setGestureCooldown(value: Long) = putLong("gesture_cooldown", value.coerceIn(200L, 1000L))

    fun isHapticEnabled(): Boolean = getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = putBoolean("haptic_enabled", enabled)

    fun isInvertX(): Boolean = getBoolean("invert_x", false)
    fun setInvertX(invert: Boolean) = putBoolean("invert_x", invert)

    fun isInvertY(): Boolean = getBoolean("invert_y", false)
    fun setInvertY(invert: Boolean) = putBoolean("invert_y", invert)

    fun isAccelerationEnabled(): Boolean = getBoolean("acceleration_enabled", true)
    fun setAccelerationEnabled(enabled: Boolean) = putBoolean("acceleration_enabled", enabled)

    fun getAccelerationFactor(): Float = getFloat("acceleration_factor", 1.5f)
    fun setAccelerationFactor(value: Float) = putFloat("acceleration_factor", value.coerceIn(1.0f, 3.0f))

    fun isSmoothingEnabled(): Boolean = getBoolean("smoothing_enabled", true)
    fun setSmoothingEnabled(enabled: Boolean) = putBoolean("smoothing_enabled", enabled)

    fun getSmoothingFactor(): Float = getFloat("smoothing_factor", 0.5f)
    fun setSmoothingFactor(value: Float) = putFloat("smoothing_factor", value.coerceIn(0.1f, 0.9f))

    fun getCursorSpeed(): Float = getFloat("cursor_speed", 1.0f)
    fun setCursorSpeed(value: Float) = putFloat("cursor_speed", value.coerceIn(0.5f, 3.0f))

    // ==================== Connection Settings ====================

    fun getLastServerIp(): String = getString("last_server_ip", "")
    fun setLastServerIp(ip: String) = putString("last_server_ip", ip)

    fun getLastServerPort(): Int = getInt("last_server_port", 8080)
    fun setLastServerPort(port: Int) = putInt("last_server_port", port)

    fun getLastProtocol(): Int = getInt("last_protocol", 0)
    fun setLastProtocol(protocol: Int) = putInt("last_protocol", protocol)

    fun getServerMac(): String = getString("server_mac", "")
    fun setServerMac(mac: String) = putString("server_mac", mac)

    fun isAutoConnect(): Boolean = getBoolean("auto_connect", true)
    fun setAutoConnect(enabled: Boolean) = putBoolean("auto_connect", enabled)

    fun getReconnectAttempts(): Int = getInt("reconnect_attempts", 5)
    fun setReconnectAttempts(attempts: Int) = putInt("reconnect_attempts", attempts.coerceIn(1, 20))

    fun getConnectionTimeout(): Int = getInt("connection_timeout", 5000)
    fun setConnectionTimeout(timeout: Int) = putInt("connection_timeout", timeout.coerceIn(1000, 30000))

    fun isWebSocketEnabled(): Boolean = getBoolean("use_websocket", true)
    fun setWebSocketEnabled(enabled: Boolean) = putBoolean("use_websocket", enabled)

    fun isUdpDiscoveryEnabled(): Boolean = getBoolean("use_udp_discovery", true)
    fun setUdpDiscoveryEnabled(enabled: Boolean) = putBoolean("use_udp_discovery", enabled)

    // ==================== Theme Settings ====================

    fun getTheme(): String = getString("theme", "system")
    fun setTheme(theme: String) = putString("theme", theme)

    fun isDynamicColorsEnabled(): Boolean = getBoolean("dynamic_colors", true)
    fun setDynamicColorsEnabled(enabled: Boolean) = putBoolean("dynamic_colors", enabled)

    fun getFontSize(): Float = getFloat("font_size", 16f)
    fun setFontSize(value: Float) = putFloat("font_size", value.coerceIn(12f, 24f))

    fun isDebugInfoEnabled(): Boolean = getBoolean("show_debug_info", false)
    fun setDebugInfoEnabled(enabled: Boolean) = putBoolean("show_debug_info", enabled)

    fun isKeepScreenOn(): Boolean = getBoolean("keep_screen_on", false)
    fun setKeepScreenOn(enabled: Boolean) = putBoolean("keep_screen_on", enabled)

    fun isShowFps(): Boolean = getBoolean("show_fps", false)
    fun setShowFps(enabled: Boolean) = putBoolean("show_fps", enabled)

    // ==================== Calibration Methods ====================
    fun isOnboardingCompleted(): Boolean = getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) = putBoolean("onboarding_completed", completed)
    fun isCalibrated(): Boolean = getBoolean("calibration_complete", false)
    fun setCalibrated(calibrated: Boolean) = putBoolean("calibration_complete", calibrated)

    fun setCalibrationComplete(complete: Boolean) = setCalibrated(complete)

    fun getCalibrationQuality(): Float = getFloat("calibration_quality", 0f)
    fun setCalibrationQuality(quality: Float) = putFloat("calibration_quality", quality.coerceIn(0f, 100f))

    fun getLastCalibrationTime(): Long = getLong("calibration_complete_time", 0)
    fun setLastCalibrationTime(time: Long) = putLong("calibration_complete_time", time)

    fun resetCalibrationAttempts() = putInt("calibration_attempts", 5)
    fun getCalibrationAttempts(): Int = getInt("calibration_attempts", 5)
    fun decrementCalibrationAttempts() = putInt("calibration_attempts", getCalibrationAttempts() - 1)

    // ==================== Gyroscope Calibration ====================

    fun saveGyroBias(bias: FloatArray) {
        putFloat("gyro_bias_x", bias.getOrElse(0) { 0f })
        putFloat("gyro_bias_y", bias.getOrElse(1) { 0f })
        putFloat("gyro_bias_z", bias.getOrElse(2) { 0f })
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

    // ==================== Accelerometer Calibration ====================

    fun saveAccelParams(offset: FloatArray, scale: FloatArray) {
        putFloat("accel_offset_x", offset.getOrElse(0) { 0f })
        putFloat("accel_offset_y", offset.getOrElse(1) { 0f })
        putFloat("accel_offset_z", offset.getOrElse(2) { 0f })
        putFloat("accel_scale_x", scale.getOrElse(0) { 1f })
        putFloat("accel_scale_y", scale.getOrElse(1) { 1f })
        putFloat("accel_scale_z", scale.getOrElse(2) { 1f })
    }

    fun saveAccelerometerParams(offset: FloatArray, scale: FloatArray) = saveAccelParams(offset, scale)

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

    // ==================== Magnetometer Calibration ====================

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

    // ==================== AI & Predictive Settings ====================

    fun isAiSmoothingEnabled(): Boolean = getBoolean("ai_smoothing", false)
    fun setAiSmoothingEnabled(enabled: Boolean) = putBoolean("ai_smoothing", enabled)

    fun getAiBlendFactor(): Float = getFloat("ai_blend_factor", 0.7f)
    fun setAiBlendFactor(value: Float) = putFloat("ai_blend_factor", value.coerceIn(0.3f, 0.95f))

    fun isPredictiveEnabled(): Boolean = getBoolean("predictive_movement", true)
    fun setPredictiveEnabled(enabled: Boolean) = putBoolean("predictive_movement", enabled)

    fun getPredictionStrength(): Float = getFloat("prediction_strength", 0.5f)
    fun setPredictionStrength(value: Float) = putFloat("prediction_strength", value.coerceIn(0.1f, 0.9f))

    fun isKalmanEnabled(): Boolean = getBoolean("kalman_enabled", true)
    fun setKalmanEnabled(enabled: Boolean) = putBoolean("kalman_enabled", enabled)

    // ==================== Feedback Settings ====================

    fun isSoundEnabled(): Boolean = getBoolean("sound_enabled", true)
    fun setSoundEnabled(enabled: Boolean) = putBoolean("sound_enabled", enabled)

    fun isVisualFeedbackEnabled(): Boolean = getBoolean("visual_feedback", true)
    fun setVisualFeedbackEnabled(enabled: Boolean) = putBoolean("visual_feedback", enabled)

    fun isNotificationOnGesture(): Boolean = getBoolean("notification_on_gesture", false)
    fun setNotificationOnGesture(enabled: Boolean) = putBoolean("notification_on_gesture", enabled)

    fun getHapticStrength(): String = getString("haptic_strength", "MEDIUM")
    fun setHapticStrength(strength: String) = putString("haptic_strength", strength)

    // ==================== Proximity Settings ====================

    fun isProximityEnabled(): Boolean = getBoolean("proximity_enabled", false)
    fun setProximityEnabled(enabled: Boolean) = putBoolean("proximity_enabled", enabled)

    fun getProximityNearThreshold(): Float = getFloat("proximity_near", 1.5f)
    fun setProximityNearThreshold(value: Float) = putFloat("proximity_near", value.coerceIn(0.5f, 5f))

    fun getProximityFarThreshold(): Float = getFloat("proximity_far", 3.0f)
    fun setProximityFarThreshold(value: Float) = putFloat("proximity_far", value.coerceIn(1f, 10f))

    fun isProximityVibrationEnabled(): Boolean = getBoolean("proximity_vibration", true)
    fun setProximityVibrationEnabled(enabled: Boolean) = putBoolean("proximity_vibration", enabled)

    fun isProximityNotificationEnabled(): Boolean = getBoolean("proximity_notification", true)
    fun setProximityNotificationEnabled(enabled: Boolean) = putBoolean("proximity_notification", enabled)

    // ==================== Voice Command Settings ====================

    fun isVoiceEnabled(): Boolean = getBoolean("voice_enabled", false)
    fun setVoiceEnabled(enabled: Boolean) = putBoolean("voice_enabled", enabled)

    fun isWakeWordEnabled(): Boolean = getBoolean("wake_word_enabled", true)
    fun setWakeWordEnabled(enabled: Boolean) = putBoolean("wake_word_enabled", enabled)

    fun getWakeWord(): String = getString("wake_word", "hey air mouse")
    fun setWakeWord(word: String) = putString("wake_word", word.lowercase())

    fun getVoiceWakeWordConfidence(): Float = getFloat("voice_wake_word_confidence", 0.7f)
    fun setVoiceWakeWordConfidence(confidence: Float) = putFloat("voice_wake_word_confidence", confidence.coerceIn(0.5f, 0.95f))

    fun isVoiceContinuousListening(): Boolean = getBoolean("voice_continuous", false)
    fun setVoiceContinuousListening(enabled: Boolean) = putBoolean("voice_continuous", enabled)

    fun isVoiceFeedbackEnabled(): Boolean = getBoolean("voice_feedback", true)
    fun setVoiceFeedbackEnabled(enabled: Boolean) = putBoolean("voice_feedback", enabled)

    fun isVoiceSoundEffectsEnabled(): Boolean = getBoolean("voice_sound_effects", true)
    fun setVoiceSoundEffectsEnabled(enabled: Boolean) = putBoolean("voice_sound_effects", enabled)

    fun getVoiceLanguage(): String = getString("voice_language", "en-US")
    fun setVoiceLanguage(language: String) = putString("voice_language", language)

    fun getVoiceSensitivity(): Float = getFloat("voice_sensitivity", 0.5f)
    fun setVoiceSensitivity(sensitivity: Float) = putFloat("voice_sensitivity", sensitivity.coerceIn(0.2f, 0.9f))

    // ==================== Edge Gesture Settings ====================

    fun isEdgeGesturesEnabled(): Boolean = getBoolean("edge_gestures_enabled", false)
    fun setEdgeGesturesEnabled(enabled: Boolean) = putBoolean("edge_gestures_enabled", enabled)

    fun getEdgeVolumeUpAction(): String = getString("edge_gestures_volume_up", "Left Click")
    fun setEdgeVolumeUpAction(action: String) = putString("edge_gestures_volume_up", action)

    fun getEdgeVolumeDownAction(): String = getString("edge_gestures_volume_down", "Right Click")
    fun setEdgeVolumeDownAction(action: String) = putString("edge_gestures_volume_down", action)

    fun getEdgeLongPressAction(): String = getString("edge_gestures_long_press", "Scroll")
    fun setEdgeLongPressAction(action: String) = putString("edge_gestures_long_press", action)

    fun getEdgeSensitivity(): Float = getFloat("edge_gestures_sensitivity", 0.2f)
    fun setEdgeSensitivity(sensitivity: Float) = putFloat("edge_gestures_sensitivity", sensitivity.coerceIn(0.1f, 0.5f))

    // ==================== Battery Saver Settings ====================

    fun isBatterySaverEnabled(): Boolean = getBoolean("battery_saver_enabled", true)
    fun setBatterySaverEnabled(enabled: Boolean) = putBoolean("battery_saver_enabled", enabled)

    fun getBatterySaverIdleTime(): Long = getLong("battery_saver_idle_time", 10000L)
    fun setBatterySaverIdleTime(time: Long) = putLong("battery_saver_idle_time", time.coerceIn(5000L, 30000L))

    // ==================== Accessibility Settings ====================

    fun isAnnounceMovementEnabled(): Boolean = getBoolean("announce_movement", false)
    fun setAnnounceMovementEnabled(enabled: Boolean) = putBoolean("announce_movement", enabled)

    fun isAnnounceClicksEnabled(): Boolean = getBoolean("announce_clicks", false)
    fun setAnnounceClicksEnabled(enabled: Boolean) = putBoolean("announce_clicks", enabled)

    fun isHighContrastMode(): Boolean = getBoolean("high_contrast", false)
    fun setHighContrastMode(enabled: Boolean) = putBoolean("high_contrast", enabled)

    fun isLargeTextEnabled(): Boolean = getBoolean("large_text", false)
    fun setLargeTextEnabled(enabled: Boolean) = putBoolean("large_text", enabled)

    fun isReduceMotionEnabled(): Boolean = getBoolean("reduce_motion", false)
    fun setReduceMotionEnabled(enabled: Boolean) = putBoolean("reduce_motion", enabled)

    fun getColorBlindMode(): String = getString("color_blind_mode", "NONE")
    fun setColorBlindMode(mode: String) = putString("color_blind_mode", mode.uppercase())

    // ==================== Privacy Settings ====================

    fun isAnonymousStatsEnabled(): Boolean = getBoolean("anonymous_stats", true)
    fun setAnonymousStatsEnabled(enabled: Boolean) = putBoolean("anonymous_stats", enabled)

    fun isCrashReportingEnabled(): Boolean = getBoolean("crash_reporting", true)
    fun setCrashReportingEnabled(enabled: Boolean) = putBoolean("crash_reporting", enabled)

    fun isClearDataOnExit(): Boolean = getBoolean("clear_data_on_exit", false)
    fun setClearDataOnExit(enabled: Boolean) = putBoolean("clear_data_on_exit", enabled)

    // ==================== Onboarding Settings ====================

    fun isOnboardingCompleted(): Boolean = getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) = putBoolean("onboarding_completed", completed)

    fun getUserName(): String = getString("user_name", "")
    fun setUserName(name: String) = putString("user_name", name)

    fun isFirstLaunch(): Boolean = getBoolean("first_launch", true)
    fun setFirstLaunchComplete() = putBoolean("first_launch", false)

    fun getAppVersion(): Int = getInt("app_version", 0)
    fun setAppVersion(version: Int) = putInt("app_version", version)

    // ==================== Statistics Methods ====================

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
    fun addDistance(distance: Float) = putFloat("stat_total_distance", getTotalDistance() + distance)

    fun getTotalMovements(): Int = getInt("stat_total_movements", 0)
    fun incrementMovements() = putInt("stat_total_movements", getTotalMovements() + 1)

    fun getSessionTime(): Long = getLong("stat_session_time", 0L)
    fun addSessionTime(time: Long) = putLong("stat_session_time", getSessionTime() + time)

    fun resetStatistics() {
        putInt("stat_clicks", 0)
        putInt("stat_double_clicks", 0)
        putInt("stat_right_clicks", 0)
        putInt("stat_scrolls", 0)
        putFloat("stat_total_distance", 0f)
        putInt("stat_total_movements", 0)
        putLong("stat_session_time", 0L)
    }

    fun getTotalGestures(): Int = getInt("stat_gestures", 0)
    fun incrementTotalGestures() = putInt("stat_gestures", getTotalGestures() + 1)

    // ==================== Profile Methods ====================

    fun getLastUsedProfile(): String = getString("last_used_profile", "Default")
    fun setLastUsedProfile(profile: String) = putString("last_used_profile", profile)

    fun saveProfile(name: String, sensitivity: Float, clickThreshold: Float, scrollThreshold: Float) {
        putFloat("profile_${name}_sensitivity", sensitivity)
        putFloat("profile_${name}_click_threshold", clickThreshold)
        putFloat("profile_${name}_scroll_threshold", scrollThreshold)
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
        prefs.edit {
            remove("profile_${name}_sensitivity")
            remove("profile_${name}_click_threshold")
            remove("profile_${name}_scroll_threshold")
        }
    }

    fun renameProfile(oldName: String, newName: String) {
        val sensitivity = getProfileSensitivity(oldName)
        val clickThreshold = getProfileClickThreshold(oldName)
        val scrollThreshold = getProfileScrollThreshold(oldName)
        saveProfile(newName, sensitivity, clickThreshold, scrollThreshold)
        deleteProfile(oldName)
    }

    // ==================== Custom Gesture Methods ====================

    fun saveCustomGesture(name: String, data: String) = putString("gesture_$name", data)
    fun getCustomGesture(name: String): String = getString("gesture_$name", "")
    fun deleteCustomGesture(name: String) = remove("gesture_$name")
    fun getAllCustomGestures(): Map<String, String> {
        val allKeys = prefs.all.keys
        return allKeys.filter { it.startsWith("gesture_") }
            .associate { it.removePrefix("gesture_") to getString(it, "") }
    }

    // ==================== Developer Settings ====================

    fun isDeveloperModeEnabled(): Boolean = getBoolean("developer_mode", false)
    fun setDeveloperModeEnabled(enabled: Boolean) = putBoolean("developer_mode", enabled)

    fun isExperimentalFeaturesEnabled(): Boolean = getBoolean("experimental_features", false)
    fun setExperimentalFeaturesEnabled(enabled: Boolean) = putBoolean("experimental_features", enabled)

    fun getLogLevel(): String = getString("log_level", "INFO")
    fun setLogLevel(level: String) = putString("log_level", level.uppercase())

    // ==================== Recorded Gestures (for training) ====================

    fun getRecordedGestures(): List<String> = getString("recorded_gestures", "").split(",").filter { it.isNotEmpty() }
    fun addRecordedGesture(fileName: String) {
        val current = getRecordedGestures()
        if (!current.contains(fileName)) {
            val newList = (current + fileName).joinToString(",")
            putString("recorded_gestures", newList)
        }
    }

    // ==================== Export/Import ====================

    fun exportSettings(): String {
        return buildString {
            appendLine("AIRMOUSE_SETTINGS_EXPORT")
            appendLine("version=2")
            appendLine("export_time=${System.currentTimeMillis()}")
            appendLine("sensitivity=${getSensitivity()}")
            appendLine("click_threshold=${getClickThreshold()}")
            appendLine("double_click_interval=${getDoubleClickInterval()}")
            appendLine("scroll_threshold=${getScrollThreshold()}")
            appendLine("right_click_tilt=${getRightClickTilt()}")
            appendLine("haptic_enabled=${isHapticEnabled()}")
            appendLine("theme=${getTheme()}")
            appendLine("ai_smoothing=${isAiSmoothingEnabled()}")
            appendLine("predictive_movement=${isPredictiveEnabled()}")
            appendLine("invert_x=${isInvertX()}")
            appendLine("invert_y=${isInvertY()}")
            appendLine("acceleration_enabled=${isAccelerationEnabled()}")
            appendLine("smoothing_enabled=${isSmoothingEnabled()}")
            appendLine("auto_connect=${isAutoConnect()}")
            appendLine("wake_word=${getWakeWord()}")
            appendLine("wake_word_enabled=${isWakeWordEnabled()}")
            appendLine("proximity_enabled=${isProximityEnabled()}")
            appendLine("edge_gestures_enabled=${isEdgeGesturesEnabled()}")
            appendLine("battery_saver_enabled=${isBatterySaverEnabled()}")
            appendLine("near_threshold=${getProximityNearThreshold()}")
            appendLine("far_threshold=${getProximityFarThreshold()}")
            appendLine("smoothing_factor=${getSmoothingFactor()}")
            appendLine("voice_enabled=${isVoiceEnabled()}")
            appendLine("voice_continuous=${isVoiceContinuousListening()}")
            appendLine("voice_language=${getVoiceLanguage()}")
            appendLine("announce_movement=${isAnnounceMovementEnabled()}")
            appendLine("announce_clicks=${isAnnounceClicksEnabled()}")
            appendLine("high_contrast=${isHighContrastMode()}")
        }
    }

    fun importSettings(data: String): Boolean {
        return try {
            val lines = data.lines()
            if (lines.firstOrNull() != "AIRMOUSE_SETTINGS_EXPORT") return false
            for (line in lines) {
                when {
                    line.startsWith("sensitivity=") -> setSensitivity(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("click_threshold=") -> setClickThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("double_click_interval=") -> setDoubleClickInterval(line.substringAfter("=").toLongOrNull() ?: continue)
                    line.startsWith("scroll_threshold=") -> setScrollThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("right_click_tilt=") -> setRightClickTilt(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("haptic_enabled=") -> setHapticEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("theme=") -> setTheme(line.substringAfter("="))
                    line.startsWith("ai_smoothing=") -> setAiSmoothingEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("predictive_movement=") -> setPredictiveEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("invert_x=") -> setInvertX(line.substringAfter("=").toBoolean())
                    line.startsWith("invert_y=") -> setInvertY(line.substringAfter("=").toBoolean())
                    line.startsWith("acceleration_enabled=") -> setAccelerationEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("smoothing_enabled=") -> setSmoothingEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("auto_connect=") -> setAutoConnect(line.substringAfter("=").toBoolean())
                    line.startsWith("wake_word=") -> setWakeWord(line.substringAfter("="))
                    line.startsWith("wake_word_enabled=") -> setWakeWordEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("proximity_enabled=") -> setProximityEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("edge_gestures_enabled=") -> setEdgeGesturesEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("battery_saver_enabled=") -> setBatterySaverEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("near_threshold=") -> setProximityNearThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("far_threshold=") -> setProximityFarThreshold(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("smoothing_factor=") -> setSmoothingFactor(line.substringAfter("=").toFloatOrNull() ?: continue)
                    line.startsWith("voice_enabled=") -> setVoiceEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_continuous=") -> setVoiceContinuousListening(line.substringAfter("=").toBoolean())
                    line.startsWith("voice_language=") -> setVoiceLanguage(line.substringAfter("="))
                    line.startsWith("announce_movement=") -> setAnnounceMovementEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("announce_clicks=") -> setAnnounceClicksEnabled(line.substringAfter("=").toBoolean())
                    line.startsWith("high_contrast=") -> setHighContrastMode(line.substringAfter("=").toBoolean())
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}


// app/src/main/java/com/airmouse/utils/PreferencesManager.kt
package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "airmouse_prefs"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    // Sensitivity helpers
    fun getSensitivity(): Float {
        return getFloat("sensitivity", 0.5f)
    }

    fun setSensitivity(value: Float) {
        putFloat("sensitivity", value)
    }
}


package com.airmouse.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "airmouse_prefs"
        private const val DEFAULT_SENSITIVITY = 0.5f
        private const val DEFAULT_CLICK_THRESHOLD = 5.0f
        private const val DEFAULT_DOUBLE_CLICK_INTERVAL = 400L
        private const val DEFAULT_SCROLL_THRESHOLD = 8.0f
        private const val DEFAULT_RIGHT_CLICK_TILT = 45f
        private const val DEFAULT_THEME = "dark"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ==================== String operations ====================
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    // ==================== Int operations ====================
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    // ==================== Long operations ====================
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    // ==================== Float operations ====================
    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    // ==================== Boolean operations ====================
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    // ==================== Remove operations ====================
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    // ==================== Settings Getters/Setters ====================
    fun getSensitivity(): Float = getFloat("sensitivity", DEFAULT_SENSITIVITY)
    fun setSensitivity(value: Float) = putFloat("sensitivity", value)

    fun getClickThreshold(): Float = getFloat("click_threshold", DEFAULT_CLICK_THRESHOLD)
    fun setClickThreshold(value: Float) = putFloat("click_threshold", value)

    fun getDoubleClickInterval(): Long = getLong("double_click_interval", DEFAULT_DOUBLE_CLICK_INTERVAL)
    fun setDoubleClickInterval(value: Long) = putLong("double_click_interval", value)

    fun getScrollThreshold(): Float = getFloat("scroll_threshold", DEFAULT_SCROLL_THRESHOLD)
    fun setScrollThreshold(value: Float) = putFloat("scroll_threshold", value)

    fun getRightClickTilt(): Float = getFloat("right_click_tilt", DEFAULT_RIGHT_CLICK_TILT)
    fun setRightClickTilt(value: Float) = putFloat("right_click_tilt", value)

    fun getTheme(): String = getString("theme", DEFAULT_THEME)
    fun setTheme(value: String) = putString("theme", value)

    fun getLastIp(): String = getString("last_ip", "")
    fun setLastIp(value: String) = putString("last_ip", value)

    fun isHapticEnabled(): Boolean = getBoolean("haptic_enabled", true)
    fun setHapticEnabled(enabled: Boolean) = putBoolean("haptic_enabled", enabled)

    fun isAccelerationEnabled(): Boolean = getBoolean("acceleration_enabled", true)
    fun setAccelerationEnabled(enabled: Boolean) = putBoolean("acceleration_enabled", enabled)

    fun isSmoothingEnabled(): Boolean = getBoolean("smoothing_enabled", true)
    fun setSmoothingEnabled(enabled: Boolean) = putBoolean("smoothing_enabled", enabled)

    fun isInvertX(): Boolean = getBoolean("invert_x", false)
    fun setInvertX(enabled: Boolean) = putBoolean("invert_x", enabled)

    fun isInvertY(): Boolean = getBoolean("invert_y", false)
    fun setInvertY(enabled: Boolean) = putBoolean("invert_y", enabled)

    // ==================== Statistics ====================
    fun incrementClickCount() {
        val current = getInt("click_count", 0)
        putInt("click_count", current + 1)
    }

    fun getClickCount(): Int = getInt("click_count", 0)

    fun incrementScrollCount() {
        val current = getInt("scroll_count", 0)
        putInt("scroll_count", current + 1)
    }

    fun getScrollCount(): Int = getInt("scroll_count", 0)

    fun incrementRightClickCount() {
        val current = getInt("right_click_count", 0)
        putInt("right_click_count", current + 1)
    }

    fun getRightClickCount(): Int = getInt("right_click_count", 0)

    fun incrementDoubleClickCount() {
        val current = getInt("double_click_count", 0)
        putInt("double_click_count", current + 1)
    }

    fun getDoubleClickCount(): Int = getInt("double_click_count", 0)

    fun resetStatistics() {
        putInt("click_count", 0)
        putInt("scroll_count", 0)
        putInt("right_click_count", 0)
        putInt("double_click_count", 0)
    }

    // ==================== Calibration ====================
    fun isCalibrated(): Boolean = getBoolean("calibration_complete", false)
    fun setCalibrated(calibrated: Boolean) = putBoolean("calibration_complete", calibrated)
}

// Add these methods to your existing PreferencesManager.kt
fun getTheme(): String = getString("theme", "system")
fun isOnboardingCompleted(): Boolean = getBoolean("onboarding_completed", false)
fun getFontSize(): Float = getFloat("font_size", 16f)