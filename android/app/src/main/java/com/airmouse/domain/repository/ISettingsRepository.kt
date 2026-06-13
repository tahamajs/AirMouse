package com.airmouse.domain.repository

interface ISettingsRepository {
    fun getSensitivity(): Float
    fun setSensitivity(value: Float)

    fun getClickThreshold(): Float
    fun setClickThreshold(value: Float)

    fun getDoubleClickInterval(): Long
    fun setDoubleClickInterval(value: Long)

    fun getScrollThreshold(): Float
    fun setScrollThreshold(value: Float)

    fun getRightClickTilt(): Float
    fun setRightClickTilt(value: Float)

    fun isHapticEnabled(): Boolean
    fun setHapticEnabled(enabled: Boolean)

    fun isInvertX(): Boolean
    fun setInvertX(invert: Boolean)

    fun isInvertY(): Boolean
    fun setInvertY(invert: Boolean)

    fun isAccelerationEnabled(): Boolean
    fun setAccelerationEnabled(enabled: Boolean)

    fun isSmoothingEnabled(): Boolean
    fun setSmoothingEnabled(enabled: Boolean)

    fun getSmoothingFactor(): Float
    fun setSmoothingFactor(value: Float)

    fun getTheme(): String
    fun setTheme(theme: String)

    fun getLastServerIp(): String
    fun setLastServerIp(ip: String)
}