// app/src/main/java/com/airmouse/data/datasource/local/IPreferencesDataSource.kt
package com.airmouse.data.datasource.local

import kotlinx.coroutines.flow.Flow

interface IPreferencesDataSource {

    // Calibration
    suspend fun setCalibrated(calibrated: Boolean)
    fun isCalibrated(): Flow<Boolean>
    suspend fun isCalibratedOnce(): Boolean
    suspend fun getCalibrationTimestamp(): Long
    suspend fun setCalibrationTimestamp(timestamp: Long)

    // Connection
    suspend fun setLastIp(ip: String)
    suspend fun getLastIp(): String
    suspend fun setLastPort(port: Int)
    suspend fun getLastPort(): Int
    suspend fun setLastProtocol(protocol: String)
    suspend fun getLastProtocol(): String

    // Sensitivity Settings
    suspend fun setSensitivity(value: Float)
    suspend fun getSensitivity(): Float
    suspend fun setClickThreshold(value: Float)
    suspend fun getClickThreshold(): Float
    suspend fun setDoubleClickInterval(value: Long)
    suspend fun getDoubleClickInterval(): Long
    suspend fun setScrollThreshold(value: Float)
    suspend fun getScrollThreshold(): Float
    suspend fun setRightClickTilt(value: Float)
    suspend fun getRightClickTilt(): Float
    suspend fun setRightClickDuration(value: Long)
    suspend fun getRightClickDuration(): Long

    // Feedback Settings
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun isHapticEnabled(): Boolean
    suspend fun setHapticStrength(strength: String)
    suspend fun getHapticStrength(): String
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun isSoundEnabled(): Boolean

    // Display Settings
    suspend fun setTheme(theme: String)
    suspend fun getTheme(): String
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun isDynamicColorsEnabled(): Boolean
    suspend fun setFontSize(size: Float)
    suspend fun getFontSize(): Float

    // AI & Predictive
    suspend fun setAISmoothingEnabled(enabled: Boolean)
    suspend fun isAISmoothingEnabled(): Boolean
    suspend fun setPredictiveEnabled(enabled: Boolean)
    suspend fun isPredictiveEnabled(): Boolean
    suspend fun setAiBlendFactor(factor: Float)
    suspend fun getAiBlendFactor(): Float

    // Inversion
    suspend fun setInvertX(enabled: Boolean)
    suspend fun isInvertXEnabled(): Boolean
    suspend fun setInvertY(enabled: Boolean)
    suspend fun isInvertYEnabled(): Boolean

    // Acceleration & Smoothing
    suspend fun setAccelerationEnabled(enabled: Boolean)
    suspend fun isAccelerationEnabled(): Boolean
    suspend fun setSmoothingEnabled(enabled: Boolean)
    suspend fun isSmoothingEnabled(): Boolean
    suspend fun setSmoothingFactor(factor: Float)
    suspend fun getSmoothingFactor(): Float

    // Connection Settings
    suspend fun setAutoConnect(enabled: Boolean)
    suspend fun isAutoConnectEnabled(): Boolean
    suspend fun setReconnectAttempts(attempts: Int)
    suspend fun getReconnectAttempts(): Int
    suspend fun setConnectionTimeout(timeout: Int)
    suspend fun getConnectionTimeout(): Int

    // Statistics
    suspend fun incrementClick()
    suspend fun incrementDoubleClick()
    suspend fun incrementRightClick()
    suspend fun incrementScroll()
    suspend fun incrementGesture(gestureName: String)
    suspend fun getClickCount(): Int
    suspend fun getDoubleClickCount(): Int
    suspend fun getRightClickCount(): Int
    suspend fun getScrollCount(): Int
    suspend fun getGestureCount(gestureName: String): Int
    suspend fun getAllGestureCounts(): Map<String, Int>

    // Reset
    suspend fun resetAllPreferences()
    suspend fun resetStatistics()
}