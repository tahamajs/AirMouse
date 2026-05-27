package com.airmouse.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "airmouse")

/**
 * Central preferences manager using DataStore.
 * Contains all user settings, calibration data, gesture counters, profiles, themes, etc.
 */
class PreferencesDataStore(context: Context) {

    private val dataStore = context.dataStore

    // -------------------- Settings --------------------
    private val SENSITIVITY = floatPreferencesKey("sensitivity")
    private val CLICK_THRESHOLD = floatPreferencesKey("click_threshold")
    private val DOUBLE_CLICK_INTERVAL = longPreferencesKey("double_click_interval")
    private val SCROLL_THRESHOLD = floatPreferencesKey("scroll_threshold")
    private val SCROLL_DEBOUNCE = floatPreferencesKey("scroll_debounce")
    private val RIGHT_CLICK_TILT = floatPreferencesKey("rightclick_tilt")
    private val RIGHT_CLICK_DURATION = longPreferencesKey("rightclick_duration")
    private val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    private val LAST_IP = stringPreferencesKey("last_ip")

    // -------------------- Calibration data --------------------
    private val GYRO_BIAS_X = floatPreferencesKey("gyro_bias_x")
    private val GYRO_BIAS_Y = floatPreferencesKey("gyro_bias_y")
    private val GYRO_BIAS_Z = floatPreferencesKey("gyro_bias_z")
    private val ACCEL_OFF_X = floatPreferencesKey("accel_off_x")
    private val ACCEL_OFF_Y = floatPreferencesKey("accel_off_y")
    private val ACCEL_OFF_Z = floatPreferencesKey("accel_off_z")
    private val ACCEL_SCALE_X = floatPreferencesKey("accel_scale_x")
    private val ACCEL_SCALE_Y = floatPreferencesKey("accel_scale_y")
    private val ACCEL_SCALE_Z = floatPreferencesKey("accel_scale_z")
    private val MAG_OFF_X = floatPreferencesKey("mag_off_x")
    private val MAG_OFF_Y = floatPreferencesKey("mag_off_y")
    private val MAG_OFF_Z = floatPreferencesKey("mag_off_z")
    private val MAG_SCALE_X = floatPreferencesKey("mag_scale_x")
    private val MAG_SCALE_Y = floatPreferencesKey("mag_scale_y")
    private val MAG_SCALE_Z = floatPreferencesKey("mag_scale_z")
    private val IS_CALIBRATED = booleanPreferencesKey("is_calibrated")

    // -------------------- Gesture counters --------------------
    private val CLICK_COUNT = intPreferencesKey("click_count")
    private val SCROLL_COUNT = intPreferencesKey("scroll_count")
    private val RIGHT_CLICK_COUNT = intPreferencesKey("right_click_count")
    private val DOUBLE_CLICK_COUNT = intPreferencesKey("double_click_count")

    // -------------------- Themes --------------------
    private val THEME = stringPreferencesKey("theme")

    // -------------------- Accessibility --------------------
    private val ANNOUNCE_MOVEMENT = booleanPreferencesKey("announce_movement")
    private val ANNOUNCE_CLICKS = booleanPreferencesKey("announce_clicks")

    // -------------------- Edge gestures --------------------
    private val EDGE_GESTURES_ENABLED = booleanPreferencesKey("edge_gestures_enabled")

    // -------------------- Custom gestures (stored as JSON strings) --------------------
    private fun customGestureKey(action: String) = stringPreferencesKey("custom_gesture_${action.replace(" ", "_")}")

    // -------------------- Profiles (stored as JSON strings) --------------------
    private fun profileKey(name: String) = stringPreferencesKey("profile_$name")

    // -------------------- Server logs --------------------
    private val SERVER_LOGS = stringPreferencesKey("server_logs")

    // -------------------- Onboarding --------------------
    private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    // ==================== FLOWS (observable) ====================
    val lastIpFlow: Flow<String> = dataStore.data.map { it[LAST_IP] ?: "" }
    val sensitivityFlow: Flow<Float> = dataStore.data.map { it[SENSITIVITY] ?: 0.5f }
    val clickThresholdFlow: Flow<Float> = dataStore.data.map { it[CLICK_THRESHOLD] ?: 5.0f }
    val doubleClickIntervalFlow: Flow<Long> = dataStore.data.map { it[DOUBLE_CLICK_INTERVAL] ?: 400L }
    val scrollThresholdFlow: Flow<Float> = dataStore.data.map { it[SCROLL_THRESHOLD] ?: 8.0f }
    val scrollDebounceFlow: Flow<Float> = dataStore.data.map { it[SCROLL_DEBOUNCE] ?: 2.0f }
    val rightClickTiltFlow: Flow<Float> = dataStore.data.map { it[RIGHT_CLICK_TILT] ?: 45f }
    val rightClickDurationFlow: Flow<Long> = dataStore.data.map { it[RIGHT_CLICK_DURATION] ?: 500L }
    val hapticEnabledFlow: Flow<Boolean> = dataStore.data.map { it[HAPTIC_ENABLED] ?: true }
    val themeFlow: Flow<String> = dataStore.data.map { it[THEME] ?: "system" }
    val announceMovementFlow: Flow<Boolean> = dataStore.data.map { it[ANNOUNCE_MOVEMENT] ?: false }
    val announceClicksFlow: Flow<Boolean> = dataStore.data.map { it[ANNOUNCE_CLICKS] ?: false }
    val edgeGesturesEnabledFlow: Flow<Boolean> = dataStore.data.map { it[EDGE_GESTURES_ENABLED] ?: false }
    val isCalibratedFlow: Flow<Boolean> = dataStore.data.map { it[IS_CALIBRATED] ?: false }

    // ==================== SUSPEND GETTERS (read once) ====================
    suspend fun getSensitivity(): Float = dataStore.data.map { it[SENSITIVITY] ?: 0.5f }.first()
    suspend fun getClickThreshold(): Float = dataStore.data.map { it[CLICK_THRESHOLD] ?: 5.0f }.first()
    suspend fun getDoubleClickInterval(): Long = dataStore.data.map { it[DOUBLE_CLICK_INTERVAL] ?: 400L }.first()
    suspend fun getScrollThreshold(): Float = dataStore.data.map { it[SCROLL_THRESHOLD] ?: 8.0f }.first()
    suspend fun getScrollDebounce(): Float = dataStore.data.map { it[SCROLL_DEBOUNCE] ?: 2.0f }.first()
    suspend fun getRightClickTilt(): Float = dataStore.data.map { it[RIGHT_CLICK_TILT] ?: 45f }.first()
    suspend fun getRightClickDuration(): Long = dataStore.data.map { it[RIGHT_CLICK_DURATION] ?: 500L }.first()
    suspend fun isHapticEnabled(): Boolean = dataStore.data.map { it[HAPTIC_ENABLED] ?: true }.first()
    suspend fun getLastIp(): String = dataStore.data.map { it[LAST_IP] ?: "" }.first()
    suspend fun getTheme(): String = dataStore.data.map { it[THEME] ?: "system" }.first()
    suspend fun isAnnounceMovementEnabled(): Boolean = dataStore.data.map { it[ANNOUNCE_MOVEMENT] ?: false }.first()
    suspend fun isAnnounceClicksEnabled(): Boolean = dataStore.data.map { it[ANNOUNCE_CLICKS] ?: false }.first()
    suspend fun isEdgeGesturesEnabled(): Boolean = dataStore.data.map { it[EDGE_GESTURES_ENABLED] ?: false }.first()
    suspend fun isOnboardingCompleted(): Boolean = dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }.first()

    // Calibration data
    suspend fun getGyroBias(): FloatArray = floatArrayOf(
        dataStore.data.map { it[GYRO_BIAS_X] ?: 0f }.first(),
        dataStore.data.map { it[GYRO_BIAS_Y] ?: 0f }.first(),
        dataStore.data.map { it[GYRO_BIAS_Z] ?: 0f }.first()
    )
    suspend fun getAccelOffset(): FloatArray = floatArrayOf(
        dataStore.data.map { it[ACCEL_OFF_X] ?: 0f }.first(),
        dataStore.data.map { it[ACCEL_OFF_Y] ?: 0f }.first(),
        dataStore.data.map { it[ACCEL_OFF_Z] ?: 0f }.first()
    )
    suspend fun getAccelScale(): FloatArray = floatArrayOf(
        dataStore.data.map { it[ACCEL_SCALE_X] ?: 1f }.first(),
        dataStore.data.map { it[ACCEL_SCALE_Y] ?: 1f }.first(),
        dataStore.data.map { it[ACCEL_SCALE_Z] ?: 1f }.first()
    )
    suspend fun getMagOffset(): FloatArray = floatArrayOf(
        dataStore.data.map { it[MAG_OFF_X] ?: 0f }.first(),
        dataStore.data.map { it[MAG_OFF_Y] ?: 0f }.first(),
        dataStore.data.map { it[MAG_OFF_Z] ?: 0f }.first()
    )
    suspend fun getMagScale(): FloatArray = floatArrayOf(
        dataStore.data.map { it[MAG_SCALE_X] ?: 1f }.first(),
        dataStore.data.map { it[MAG_SCALE_Y] ?: 1f }.first(),
        dataStore.data.map { it[MAG_SCALE_Z] ?: 1f }.first()
    )
    suspend fun isCalibrated(): Boolean = dataStore.data.map { it[IS_CALIBRATED] ?: false }.first()

    // Gesture counters
    suspend fun getClickCount(): Int = dataStore.data.map { it[CLICK_COUNT] ?: 0 }.first()
    suspend fun getScrollCount(): Int = dataStore.data.map { it[SCROLL_COUNT] ?: 0 }.first()
    suspend fun getRightClickCount(): Int = dataStore.data.map { it[RIGHT_CLICK_COUNT] ?: 0 }.first()
    suspend fun getDoubleClickCount(): Int = dataStore.data.map { it[DOUBLE_CLICK_COUNT] ?: 0 }.first()

    // Server logs
    suspend fun getServerLogs(): List<String> {
        val joined = dataStore.data.map { it[SERVER_LOGS] ?: "" }.first()
        return if (joined.isBlank()) emptyList() else joined.split("\n")
    }

    // Custom gesture value
    suspend fun getCustomGesture(action: String): Float {
        val key = customGestureKey(action)
        return dataStore.data.map { it[key]?.toFloatOrNull() ?: 0f }.first()
    }

    // Profile data (as JSON string, parse later)
    suspend fun getProfile(name: String): String? {
        return dataStore.data.map { it[profileKey(name)] }.first()
    }

    // ==================== SUSPEND SETTERS ====================
    suspend fun setSensitivity(value: Float) = dataStore.edit { it[SENSITIVITY] = value }
    suspend fun setClickThreshold(value: Float) = dataStore.edit { it[CLICK_THRESHOLD] = value }
    suspend fun setDoubleClickInterval(value: Long) = dataStore.edit { it[DOUBLE_CLICK_INTERVAL] = value }
    suspend fun setScrollThreshold(value: Float) = dataStore.edit { it[SCROLL_THRESHOLD] = value }
    suspend fun setScrollDebounce(value: Float) = dataStore.edit { it[SCROLL_DEBOUNCE] = value }
    suspend fun setRightClickTilt(value: Float) = dataStore.edit { it[RIGHT_CLICK_TILT] = value }
    suspend fun setRightClickDuration(value: Long) = dataStore.edit { it[RIGHT_CLICK_DURATION] = value }
    suspend fun setHapticEnabled(enabled: Boolean) = dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    suspend fun setLastIp(ip: String) = dataStore.edit { it[LAST_IP] = ip }
    suspend fun setTheme(theme: String) = dataStore.edit { it[THEME] = theme }
    suspend fun setAnnounceMovementEnabled(enabled: Boolean) = dataStore.edit { it[ANNOUNCE_MOVEMENT] = enabled }
    suspend fun setAnnounceClicksEnabled(enabled: Boolean) = dataStore.edit { it[ANNOUNCE_CLICKS] = enabled }
    suspend fun setEdgeGesturesEnabled(enabled: Boolean) = dataStore.edit { it[EDGE_GESTURES_ENABLED] = enabled }
    suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.edit { it[ONBOARDING_COMPLETED] = completed }

    suspend fun saveCalibration(gyroBias: FloatArray, accelOffset: FloatArray, accelScale: FloatArray,
                                magOffset: FloatArray, magScale: FloatArray) {
        dataStore.edit {
            it[GYRO_BIAS_X] = gyroBias[0]; it[GYRO_BIAS_Y] = gyroBias[1]; it[GYRO_BIAS_Z] = gyroBias[2]
            it[ACCEL_OFF_X] = accelOffset[0]; it[ACCEL_OFF_Y] = accelOffset[1]; it[ACCEL_OFF_Z] = accelOffset[2]
            it[ACCEL_SCALE_X] = accelScale[0]; it[ACCEL_SCALE_Y] = accelScale[1]; it[ACCEL_SCALE_Z] = accelScale[2]
            it[MAG_OFF_X] = magOffset[0]; it[MAG_OFF_Y] = magOffset[1]; it[MAG_OFF_Z] = magOffset[2]
            it[MAG_SCALE_X] = magScale[0]; it[MAG_SCALE_Y] = magScale[1]; it[MAG_SCALE_Z] = magScale[2]
            it[IS_CALIBRATED] = true
        }
    }

    suspend fun incrementClick() {
        dataStore.edit { it[CLICK_COUNT] = (it[CLICK_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementScroll() {
        dataStore.edit { it[SCROLL_COUNT] = (it[SCROLL_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementRightClick() {
        dataStore.edit { it[RIGHT_CLICK_COUNT] = (it[RIGHT_CLICK_COUNT] ?: 0) + 1 }
    }
    suspend fun incrementDoubleClick() {
        dataStore.edit { it[DOUBLE_CLICK_COUNT] = (it[DOUBLE_CLICK_COUNT] ?: 0) + 1 }
    }

    suspend fun addServerLog(entry: String) {
        val logs = getServerLogs().toMutableList()
        logs.add(0, entry)
        while (logs.size > 200) logs.removeAt(logs.lastIndex)
        dataStore.edit { it[SERVER_LOGS] = logs.joinToString("\n") }
    }

    suspend fun saveCustomGesture(action: String, value: Float) {
        dataStore.edit { it[customGestureKey(action)] = value.toString() }
    }

    suspend fun saveProfile(name: String, json: String) {
        dataStore.edit { it[profileKey(name)] = json }
    }
    suspend fun deleteProfile(name: String) {
        dataStore.edit { it.remove(profileKey(name)) }
    }
    suspend fun getAllProfileNames(): List<String> {
        val all = dataStore.data.first()
        return all.asMap().keys.filter { it.name.startsWith("profile_") }.map { it.name.removePrefix("profile_") }
    }

    // Blocking versions for compatibility with old code (use sparingly)
    fun setSensitivityBlocking(value: Float) = runBlocking { setSensitivity(value) }
    fun setClickThresholdBlocking(value: Float) = runBlocking { setClickThreshold(value) }
    fun setLastIpBlocking(ip: String) = runBlocking { setLastIp(ip) }
    fun setThemeBlocking(theme: String) = runBlocking { setTheme(theme) }
    fun setHapticEnabledBlocking(enabled: Boolean) = runBlocking { setHapticEnabled(enabled) }
    fun incrementClickBlocking() = runBlocking { incrementClick() }
    fun incrementScrollBlocking() = runBlocking { incrementScroll() }
    fun incrementRightClickBlocking() = runBlocking { incrementRightClick() }
    fun incrementDoubleClickBlocking() = runBlocking { incrementDoubleClick() }
}