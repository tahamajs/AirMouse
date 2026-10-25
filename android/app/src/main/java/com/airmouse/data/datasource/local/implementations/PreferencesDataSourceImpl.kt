package com.airmouse.data.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airmouse_preferences")

@Singleton
class PreferencesDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IPreferencesDataSource {

    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val IS_CALIBRATED = booleanPreferencesKey("is_calibrated")
        val CALIBRATION_TIMESTAMP = longPreferencesKey("calibration_timestamp")
        val LAST_IP = stringPreferencesKey("last_ip")
        val LAST_PORT = intPreferencesKey("last_port")
        val LAST_PROTOCOL = stringPreferencesKey("last_protocol")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val CLICK_THRESHOLD = floatPreferencesKey("click_threshold")
        val DOUBLE_CLICK_INTERVAL = longPreferencesKey("double_click_interval")
        val SCROLL_THRESHOLD = floatPreferencesKey("scroll_threshold")
        val RIGHT_CLICK_TILT = floatPreferencesKey("right_click_tilt")
        val RIGHT_CLICK_DURATION = longPreferencesKey("right_click_duration")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val HAPTIC_STRENGTH = stringPreferencesKey("haptic_strength")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val AI_SMOOTHING = booleanPreferencesKey("ai_smoothing")
        val PREDICTIVE = booleanPreferencesKey("predictive")
        val AI_BLEND_FACTOR = floatPreferencesKey("ai_blend_factor")
        val INVERT_X = booleanPreferencesKey("invert_x")
        val INVERT_Y = booleanPreferencesKey("invert_y")
        val ACCELERATION = booleanPreferencesKey("acceleration")
        val SMOOTHING = booleanPreferencesKey("smoothing")
        val SMOOTHING_FACTOR = floatPreferencesKey("smoothing_factor")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val RECONNECT_ATTEMPTS = intPreferencesKey("reconnect_attempts")
        val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout")
        val CLICK_COUNT = intPreferencesKey("click_count")
        val DOUBLE_CLICK_COUNT = intPreferencesKey("double_click_count")
        val RIGHT_CLICK_COUNT = intPreferencesKey("right_click_count")
        val SCROLL_COUNT = intPreferencesKey("scroll_count")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // ============================================================
    // Calibration State
    // ============================================================

    override suspend fun setCalibrated(calibrated: Boolean) {
        dataStore.edit { it[PreferencesKeys.IS_CALIBRATED] = calibrated }
    }

    override fun isCalibrated(): Flow<Boolean> = safePreferences.map {
        it[PreferencesKeys.IS_CALIBRATED] ?: false
    }

    override suspend fun isCalibratedOnce(): Boolean =
        safePreferences.first()[PreferencesKeys.IS_CALIBRATED] ?: false

    override suspend fun getCalibrationTimestamp(): Long =
        safePreferences.first()[PreferencesKeys.CALIBRATION_TIMESTAMP] ?: 0L

    override suspend fun setCalibrationTimestamp(timestamp: Long) {
        dataStore.edit { it[PreferencesKeys.CALIBRATION_TIMESTAMP] = timestamp }
    }

    // ============================================================
    // Connection Settings
    // ============================================================

    override suspend fun setLastIp(ip: String) {
        dataStore.edit { it[PreferencesKeys.LAST_IP] = ip }
    }

    override suspend fun getLastIp(): String =
        safePreferences.first()[PreferencesKeys.LAST_IP] ?: ""

    override suspend fun setLastPort(port: Int) {
        dataStore.edit { it[PreferencesKeys.LAST_PORT] = port }
    }

    override suspend fun getLastPort(): Int =
        safePreferences.first()[PreferencesKeys.LAST_PORT] ?: 8082

    override suspend fun setLastProtocol(protocol: String) {
        dataStore.edit { it[PreferencesKeys.LAST_PROTOCOL] = protocol.uppercase() }
    }

    override suspend fun getLastProtocol(): String =
        safePreferences.first()[PreferencesKeys.LAST_PROTOCOL] ?: "UDP"

    // ============================================================
    // Sensitivity & Cursor
    // ============================================================

    override suspend fun setSensitivity(value: Float) {
        dataStore.edit { it[PreferencesKeys.SENSITIVITY] = value }
    }

    override suspend fun getSensitivity(): Float =
        safePreferences.first()[PreferencesKeys.SENSITIVITY] ?: 0.5f

    override suspend fun setClickThreshold(value: Float) {
        dataStore.edit { it[PreferencesKeys.CLICK_THRESHOLD] = value }
    }

    override suspend fun getClickThreshold(): Float =
        safePreferences.first()[PreferencesKeys.CLICK_THRESHOLD] ?: 8f

    override suspend fun setDoubleClickInterval(value: Long) {
        dataStore.edit { it[PreferencesKeys.DOUBLE_CLICK_INTERVAL] = value }
    }

    override suspend fun getDoubleClickInterval(): Long =
        safePreferences.first()[PreferencesKeys.DOUBLE_CLICK_INTERVAL] ?: 300L

    override suspend fun setScrollThreshold(value: Float) {
        dataStore.edit { it[PreferencesKeys.SCROLL_THRESHOLD] = value }
    }

    override suspend fun getScrollThreshold(): Float =
        safePreferences.first()[PreferencesKeys.SCROLL_THRESHOLD] ?: 15f

    override suspend fun setRightClickTilt(value: Float) {
        dataStore.edit { it[PreferencesKeys.RIGHT_CLICK_TILT] = value }
    }

    override suspend fun getRightClickTilt(): Float =
        safePreferences.first()[PreferencesKeys.RIGHT_CLICK_TILT] ?: 25f

    override suspend fun setRightClickDuration(value: Long) {
        dataStore.edit { it[PreferencesKeys.RIGHT_CLICK_DURATION] = value }
    }

    override suspend fun getRightClickDuration(): Long =
        safePreferences.first()[PreferencesKeys.RIGHT_CLICK_DURATION] ?: 500L

    // ============================================================
    // Haptic & Sound
    // ============================================================

    override suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.HAPTIC_ENABLED] = enabled }
    }

    override suspend fun isHapticEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.HAPTIC_ENABLED] ?: true

    override suspend fun setHapticStrength(strength: String) {
        dataStore.edit { it[PreferencesKeys.HAPTIC_STRENGTH] = strength }
    }

    override suspend fun getHapticStrength(): String =
        safePreferences.first()[PreferencesKeys.HAPTIC_STRENGTH] ?: "medium"

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SOUND_ENABLED] = enabled }
    }

    override suspend fun isSoundEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.SOUND_ENABLED] ?: false

    // ============================================================
    // Theme & Display
    // ============================================================

    override suspend fun setTheme(theme: String) {
        dataStore.edit { it[PreferencesKeys.THEME] = theme }
    }

    override suspend fun getTheme(): String =
        safePreferences.first()[PreferencesKeys.THEME] ?: "dark"

    override suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DYNAMIC_COLORS] = enabled }
    }

    override suspend fun isDynamicColorsEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.DYNAMIC_COLORS] ?: true

    override suspend fun setFontSize(size: Float) {
        dataStore.edit { it[PreferencesKeys.FONT_SIZE] = size }
    }

    override suspend fun getFontSize(): Float =
        safePreferences.first()[PreferencesKeys.FONT_SIZE] ?: 14f

    // ============================================================
    // AI & Advanced Features
    // ============================================================

    override suspend fun setAISmoothingEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AI_SMOOTHING] = enabled }
    }

    override suspend fun isAISmoothingEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.AI_SMOOTHING] ?: true

    override suspend fun setPredictiveEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.PREDICTIVE] = enabled }
    }

    override suspend fun isPredictiveEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.PREDICTIVE] ?: false

    override suspend fun setAiBlendFactor(factor: Float) {
        dataStore.edit { it[PreferencesKeys.AI_BLEND_FACTOR] = factor }
    }

    override suspend fun getAiBlendFactor(): Float =
        safePreferences.first()[PreferencesKeys.AI_BLEND_FACTOR] ?: 0.5f

    // ============================================================
    // Invert Axis
    // ============================================================

    override suspend fun setInvertX(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.INVERT_X] = enabled }
    }

    override suspend fun isInvertXEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.INVERT_X] ?: false

    override suspend fun setInvertY(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.INVERT_Y] = enabled }
    }

    override suspend fun isInvertYEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.INVERT_Y] ?: false

    // ============================================================
    // Acceleration & Smoothing
    // ============================================================

    override suspend fun setAccelerationEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.ACCELERATION] = enabled }
    }

    override suspend fun isAccelerationEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.ACCELERATION] ?: false

    override suspend fun setSmoothingEnabled(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.SMOOTHING] = enabled }
    }

    override suspend fun isSmoothingEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.SMOOTHING] ?: true

    override suspend fun setSmoothingFactor(factor: Float) {
        dataStore.edit { it[PreferencesKeys.SMOOTHING_FACTOR] = factor }
    }

    override suspend fun getSmoothingFactor(): Float =
        safePreferences.first()[PreferencesKeys.SMOOTHING_FACTOR] ?: 0.8f

    // ============================================================
    // Auto Connect
    // ============================================================

    override suspend fun setAutoConnect(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.AUTO_CONNECT] = enabled }
    }

    override suspend fun isAutoConnectEnabled(): Boolean =
        safePreferences.first()[PreferencesKeys.AUTO_CONNECT] ?: false

    override suspend fun setReconnectAttempts(attempts: Int) {
        dataStore.edit { it[PreferencesKeys.RECONNECT_ATTEMPTS] = attempts }
    }

    override suspend fun getReconnectAttempts(): Int =
        safePreferences.first()[PreferencesKeys.RECONNECT_ATTEMPTS] ?: 3

    override suspend fun setConnectionTimeout(timeout: Int) {
        dataStore.edit { it[PreferencesKeys.CONNECTION_TIMEOUT] = timeout }
    }

    override suspend fun getConnectionTimeout(): Int =
        safePreferences.first()[PreferencesKeys.CONNECTION_TIMEOUT] ?: 5000

    // ============================================================
    // Statistics
    // ============================================================

    override suspend fun incrementClick() {
        dataStore.edit {
            val current = it[PreferencesKeys.CLICK_COUNT] ?: 0
            it[PreferencesKeys.CLICK_COUNT] = current + 1
        }
    }

    override suspend fun incrementDoubleClick() {
        dataStore.edit {
            val current = it[PreferencesKeys.DOUBLE_CLICK_COUNT] ?: 0
            it[PreferencesKeys.DOUBLE_CLICK_COUNT] = current + 1
        }
    }

    override suspend fun incrementRightClick() {
        dataStore.edit {
            val current = it[PreferencesKeys.RIGHT_CLICK_COUNT] ?: 0
            it[PreferencesKeys.RIGHT_CLICK_COUNT] = current + 1
        }
    }

    override suspend fun incrementScroll() {
        dataStore.edit {
            val current = it[PreferencesKeys.SCROLL_COUNT] ?: 0
            it[PreferencesKeys.SCROLL_COUNT] = current + 1
        }
    }

    override suspend fun incrementGesture(gestureName: String) {
        val key = intPreferencesKey("gesture_count_$gestureName")
        dataStore.edit {
            val current = it[key] ?: 0
            it[key] = current + 1
        }
    }

    override suspend fun getClickCount(): Int =
        safePreferences.first()[PreferencesKeys.CLICK_COUNT] ?: 0

    override suspend fun getDoubleClickCount(): Int =
        safePreferences.first()[PreferencesKeys.DOUBLE_CLICK_COUNT] ?: 0

    override suspend fun getRightClickCount(): Int =
        safePreferences.first()[PreferencesKeys.RIGHT_CLICK_COUNT] ?: 0

    override suspend fun getScrollCount(): Int =
        safePreferences.first()[PreferencesKeys.SCROLL_COUNT] ?: 0

    override suspend fun getGestureCount(gestureName: String): Int {
        val key = intPreferencesKey("gesture_count_$gestureName")
        return safePreferences.first()[key] ?: 0
    }

    override suspend fun getAllGestureCounts(): Map<String, Int> {
        val prefs = safePreferences.first()
        val result = mutableMapOf<String, Int>()
        prefs.asMap().forEach { (key, value) ->
            if (key.name.startsWith("gesture_count_") && value is Int) {
                val gestureName = key.name.removePrefix("gesture_count_")
                result[gestureName] = value
            }
        }
        return result
    }

    // ============================================================
    // Reset
    // ============================================================

    override suspend fun resetAllPreferences() {
        dataStore.edit { it.clear() }
    }

    override suspend fun resetStatistics() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.CLICK_COUNT)
            prefs.remove(PreferencesKeys.DOUBLE_CLICK_COUNT)
            prefs.remove(PreferencesKeys.RIGHT_CLICK_COUNT)
            prefs.remove(PreferencesKeys.SCROLL_COUNT)
            val gestureKeys = prefs.asMap().keys.filter { it.name.startsWith("gesture_count_") }
            gestureKeys.forEach { prefs.remove(it) }
        }
    }
}
