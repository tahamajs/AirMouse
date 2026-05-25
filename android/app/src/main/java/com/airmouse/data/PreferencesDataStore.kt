package com.airmouse.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "airmouse")

class PreferencesDataStore(context: Context) {

    private val dataStore = context.dataStore

    // Keys
    private val SENSITIVITY = floatPreferencesKey("sensitivity")
    private val CLICK_THRESHOLD = floatPreferencesKey("click_threshold")
    private val DOUBLE_CLICK_INTERVAL = longPreferencesKey("double_click_interval")
    private val SCROLL_THRESHOLD = floatPreferencesKey("scroll_threshold")
    private val SCROLL_DEBOUNCE = floatPreferencesKey("scroll_debounce")
    private val RIGHT_CLICK_TILT = floatPreferencesKey("rightclick_tilt")
    private val RIGHT_CLICK_DURATION = longPreferencesKey("rightclick_duration")
    private val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    private val LAST_IP = stringPreferencesKey("last_ip")

    // Flows
    val lastIpFlow: Flow<String> = dataStore.data.map { prefs -> prefs[LAST_IP] ?: "" }

    // Getters (suspend or direct)
    suspend fun getSensitivity(): Float = dataStore.data.map { it[SENSITIVITY] ?: 0.5f }.let { runBlocking { it.first() } }
    suspend fun getClickThreshold(): Float = dataStore.data.map { it[CLICK_THRESHOLD] ?: 5.0f }.let { runBlocking { it.first() } }
    suspend fun getDoubleClickInterval(): Long = dataStore.data.map { it[DOUBLE_CLICK_INTERVAL] ?: 400L }.let { runBlocking { it.first() } }
    suspend fun getScrollThreshold(): Float = dataStore.data.map { it[SCROLL_THRESHOLD] ?: 8.0f }.let { runBlocking { it.first() } }
    suspend fun getScrollDebounce(): Float = dataStore.data.map { it[SCROLL_DEBOUNCE] ?: 2.0f }.let { runBlocking { it.first() } }
    suspend fun getRightClickTilt(): Float = dataStore.data.map { it[RIGHT_CLICK_TILT] ?: 45f }.let { runBlocking { it.first() } }
    suspend fun getRightClickDuration(): Long = dataStore.data.map { it[RIGHT_CLICK_DURATION] ?: 500L }.let { runBlocking { it.first() } }
    suspend fun isHapticEnabled(): Boolean = dataStore.data.map { it[HAPTIC_ENABLED] ?: true }.let { runBlocking { it.first() } }
    suspend fun getLastIp(): String = dataStore.data.map { it[LAST_IP] ?: "" }.let { runBlocking { it.first() } }

    // Setters (use runBlocking for simplicity in ViewModel)
    fun setSensitivity(value: Float) = runBlocking { dataStore.edit { it[SENSITIVITY] = value } }
    fun setClickThreshold(value: Float) = runBlocking { dataStore.edit { it[CLICK_THRESHOLD] = value } }
    fun setDoubleClickInterval(value: Long) = runBlocking { dataStore.edit { it[DOUBLE_CLICK_INTERVAL] = value } }
    fun setScrollThreshold(value: Float) = runBlocking { dataStore.edit { it[SCROLL_THRESHOLD] = value } }
    fun setScrollDebounce(value: Float) = runBlocking { dataStore.edit { it[SCROLL_DEBOUNCE] = value } }
    fun setRightClickTilt(value: Float) = runBlocking { dataStore.edit { it[RIGHT_CLICK_TILT] = value } }
    fun setRightClickDuration(value: Long) = runBlocking { dataStore.edit { it[RIGHT_CLICK_DURATION] = value } }
    fun setHapticEnabled(enabled: Boolean) = runBlocking { dataStore.edit { it[HAPTIC_ENABLED] = enabled } }
    fun setLastIp(ip: String) = runBlocking { dataStore.edit { it[LAST_IP] = ip } }
}