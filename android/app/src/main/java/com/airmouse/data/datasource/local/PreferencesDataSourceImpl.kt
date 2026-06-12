// app/src/main/java/com/airmouse/data/datasource/local/PreferencesDataSourceImpl.kt
package com.airmouse.data.datasource.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IPreferencesDataSource {

    private val prefs: SharedPreferences = context.getSharedPreferences("airmouse", Context.MODE_PRIVATE)

    override suspend fun setCalibrated(calibrated: Boolean) {
        prefs.edit().putBoolean("is_calibrated", calibrated).apply()
    }

    override fun isCalibrated(): Flow<Boolean> = flow {
        emit(prefs.getBoolean("is_calibrated", false))
    }

    override suspend fun setLastIp(ip: String) {
        prefs.edit().putString("last_ip", ip).apply()
    }

    override suspend fun getLastIp(): String = prefs.getString("last_ip", "") ?: ""

    override suspend fun setLastPort(port: Int) {
        prefs.edit().putInt("last_port", port).apply()
    }

    override suspend fun getLastPort(): Int = prefs.getInt("last_port", 8080)

    override suspend fun setSensitivity(value: Float) {
        prefs.edit().putFloat("sensitivity", value).apply()
    }

    override suspend fun getSensitivity(): Float = prefs.getFloat("sensitivity", 0.5f)

    override suspend fun setClickThreshold(value: Float) {
        prefs.edit().putFloat("click_threshold", value).apply()
    }

    override suspend fun getClickThreshold(): Float = prefs.getFloat("click_threshold", 10f)

    override suspend fun setDoubleClickInterval(value: Long) {
        prefs.edit().putLong("double_click_interval", value).apply()
    }

    override suspend fun getDoubleClickInterval(): Long = prefs.getLong("double_click_interval", 300L)

    override suspend fun setScrollThreshold(value: Float) {
        prefs.edit().putFloat("scroll_threshold", value).apply()
    }

    override suspend fun getScrollThreshold(): Float = prefs.getFloat("scroll_threshold", 5f)

    override suspend fun setRightClickTilt(value: Float) {
        prefs.edit().putFloat("rightclick_tilt", value).apply()
    }

    override suspend fun getRightClickTilt(): Float = prefs.getFloat("rightclick_tilt", 15f)

    override suspend fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    override suspend fun isHapticEnabled(): Boolean = prefs.getBoolean("haptic_enabled", true)

    override suspend fun setTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    override suspend fun getTheme(): String = prefs.getString("theme", "dark") ?: "dark"

    override suspend fun setAISmoothingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ai_smoothing", enabled).apply()
    }

    override suspend fun isAISmoothingEnabled(): Boolean = prefs.getBoolean("ai_smoothing", false)

    override suspend fun setPredictiveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("predictive", enabled).apply()
    }

    override suspend fun isPredictiveEnabled(): Boolean = prefs.getBoolean("predictive", true)

    override suspend fun incrementClick() = incrementCount("click_count")
    override suspend fun incrementDoubleClick() = incrementCount("double_click_count")
    override suspend fun incrementRightClick() = incrementCount("right_click_count")
    override suspend fun incrementScroll() = incrementCount("scroll_count")

    override suspend fun getClickCount(): Int = prefs.getInt("click_count", 0)
    override suspend fun getDoubleClickCount(): Int = prefs.getInt("double_click_count", 0)
    override suspend fun getRightClickCount(): Int = prefs.getInt("right_click_count", 0)
    override suspend fun getScrollCount(): Int = prefs.getInt("scroll_count", 0)

    private suspend fun incrementCount(key: String) {
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }
}
