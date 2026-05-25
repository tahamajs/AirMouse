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
 * Manages all persistent user preferences using AndroidX DataStore.
 * Provides both Flow APIs (non‑blocking) and suspend getters/setters.
 */
class PreferencesDataStore(context: Context) {

    private val dataStore = context.dataStore

    // Preference keys
    private val SENSITIVITY = floatPreferencesKey("sensitivity")
    private val CLICK_THRESHOLD = floatPreferencesKey("click_threshold")
    private val DOUBLE_CLICK_INTERVAL = longPreferencesKey("double_click_interval")
    private val SCROLL_THRESHOLD = floatPreferencesKey("scroll_threshold")
    private val SCROLL_DEBOUNCE = floatPreferencesKey("scroll_debounce")
    private val RIGHT_CLICK_TILT = floatPreferencesKey("rightclick_tilt")
    private val RIGHT_CLICK_DURATION = longPreferencesKey("rightclick_duration")
    private val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    private val LAST_IP = stringPreferencesKey("last_ip")

    // --- Flows (observable preferences) ---
    val lastIpFlow: Flow<String> = dataStore.data.map { prefs -> prefs[LAST_IP] ?: "" }
    val sensitivityFlow: Flow<Float> = dataStore.data.map { it[SENSITIVITY] ?: 0.5f }
    val clickThresholdFlow: Flow<Float> = dataStore.data.map { it[CLICK_THRESHOLD] ?: 5.0f }
    val doubleClickIntervalFlow: Flow<Long> = dataStore.data.map { it[DOUBLE_CLICK_INTERVAL] ?: 400L }
    val scrollThresholdFlow: Flow<Float> = dataStore.data.map { it[SCROLL_THRESHOLD] ?: 8.0f }
    val scrollDebounceFlow: Flow<Float> = dataStore.data.map { it[SCROLL_DEBOUNCE] ?: 2.0f }
    val rightClickTiltFlow: Flow<Float> = dataStore.data.map { it[RIGHT_CLICK_TILT] ?: 45f }
    val rightClickDurationFlow: Flow<Long> = dataStore.data.map { it[RIGHT_CLICK_DURATION] ?: 500L }
    val hapticEnabledFlow: Flow<Boolean> = dataStore.data.map { it[HAPTIC_ENABLED] ?: true }

    // --- Suspend getters (read once) ---
    suspend fun getSensitivity(): Float = dataStore.data.map { it[SENSITIVITY] ?: 0.5f }.first()
    suspend fun getClickThreshold(): Float = dataStore.data.map { it[CLICK_THRESHOLD] ?: 5.0f }.first()
    suspend fun getDoubleClickInterval(): Long = dataStore.data.map { it[DOUBLE_CLICK_INTERVAL] ?: 400L }.first()
    suspend fun getScrollThreshold(): Float = dataStore.data.map { it[SCROLL_THRESHOLD] ?: 8.0f }.first()
    suspend fun getScrollDebounce(): Float = dataStore.data.map { it[SCROLL_DEBOUNCE] ?: 2.0f }.first()
    suspend fun getRightClickTilt(): Float = dataStore.data.map { it[RIGHT_CLICK_TILT] ?: 45f }.first()
    suspend fun getRightClickDuration(): Long = dataStore.data.map { it[RIGHT_CLICK_DURATION] ?: 500L }.first()
    suspend fun isHapticEnabled(): Boolean = dataStore.data.map { it[HAPTIC_ENABLED] ?: true }.first()
    suspend fun getLastIp(): String = dataStore.data.map { it[LAST_IP] ?: "" }.first()

    // --- Setters (suspend, recommended) ---
    suspend fun setSensitivity(value: Float) = dataStore.edit { it[SENSITIVITY] = value }
    suspend fun setClickThreshold(value: Float) = dataStore.edit { it[CLICK_THRESHOLD] = value }
    suspend fun setDoubleClickInterval(value: Long) = dataStore.edit { it[DOUBLE_CLICK_INTERVAL] = value }
    suspend fun setScrollThreshold(value: Float) = dataStore.edit { it[SCROLL_THRESHOLD] = value }
    suspend fun setScrollDebounce(value: Float) = dataStore.edit { it[SCROLL_DEBOUNCE] = value }
    suspend fun setRightClickTilt(value: Float) = dataStore.edit { it[RIGHT_CLICK_TILT] = value }
    suspend fun setRightClickDuration(value: Long) = dataStore.edit { it[RIGHT_CLICK_DURATION] = value }
    suspend fun setHapticEnabled(enabled: Boolean) = dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    suspend fun setLastIp(ip: String) = dataStore.edit { it[LAST_IP] = ip }

    // --- Blocking setters (for compatibility with older callers; avoid in production) ---
    fun setSensitivityBlocking(value: Float) = runBlocking { setSensitivity(value) }
    fun setClickThresholdBlocking(value: Float) = runBlocking { setClickThreshold(value) }
    fun setDoubleClickIntervalBlocking(value: Long) = runBlocking { setDoubleClickInterval(value) }
    fun setScrollThresholdBlocking(value: Float) = runBlocking { setScrollThreshold(value) }
    fun setScrollDebounceBlocking(value: Float) = runBlocking { setScrollDebounce(value) }
    fun setRightClickTiltBlocking(value: Float) = runBlocking { setRightClickTilt(value) }
    fun setRightClickDurationBlocking(value: Long) = runBlocking { setRightClickDuration(value) }
    fun setHapticEnabledBlocking(enabled: Boolean) = runBlocking { setHapticEnabled(enabled) }
    fun setLastIpBlocking(ip: String) = runBlocking { setLastIp(ip) }
}