package com.airmouse.data.datasource.local

import kotlinx.coroutines.flow.Flow

interface IPreferencesDataSource {
    suspend fun setCalibrated(calibrated: Boolean)
    fun isCalibrated(): Flow<Boolean>
    suspend fun setLastIp(ip: String)
    suspend fun getLastIp(): String
    suspend fun setLastPort(port: Int)
    suspend fun getLastPort(): Int
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
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun isHapticEnabled(): Boolean
    suspend fun setTheme(theme: String)
    suspend fun getTheme(): String
    suspend fun setAISmoothingEnabled(enabled: Boolean)
    suspend fun isAISmoothingEnabled(): Boolean
    suspend fun setPredictiveEnabled(enabled: Boolean)
    suspend fun isPredictiveEnabled(): Boolean
    suspend fun incrementClick()
    suspend fun incrementDoubleClick()
    suspend fun incrementRightClick()
    suspend fun incrementScroll()
    suspend fun getClickCount(): Int
    suspend fun getDoubleClickCount(): Int
    suspend fun getRightClickCount(): Int
    suspend fun getScrollCount(): Int
}