
package com.airmouse.domain.repository

import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {

    
    suspend fun getSensitivity(): Float
    suspend fun setSensitivity(value: Float)
    fun observeSensitivity(): Flow<Float>

    
    suspend fun isSmoothingEnabled(): Boolean
    suspend fun setSmoothingEnabled(enabled: Boolean)
    fun observeSmoothingEnabled(): Flow<Boolean>

    
    suspend fun isAccelerationEnabled(): Boolean
    suspend fun setAccelerationEnabled(enabled: Boolean)
    fun observeAccelerationEnabled(): Flow<Boolean>
    suspend fun getAccelerationFactor(): Float
    suspend fun setAccelerationFactor(factor: Float)

    
    suspend fun isInvertX(): Boolean
    suspend fun setInvertX(enabled: Boolean)
    fun observeInvertX(): Flow<Boolean>
    suspend fun isInvertY(): Boolean
    suspend fun setInvertY(enabled: Boolean)
    fun observeInvertY(): Flow<Boolean>

    
    suspend fun isSwapAxes(): Boolean
    suspend fun setSwapAxes(enabled: Boolean)
    fun observeSwapAxes(): Flow<Boolean>

    
    suspend fun getDeadband(): Float
    suspend fun setDeadband(value: Float)
    fun observeDeadband(): Flow<Float>

    
    suspend fun getMaxSpeed(): Float
    suspend fun setMaxSpeed(value: Float)
    fun observeMaxSpeed(): Flow<Float>

    
    suspend fun getMinSpeed(): Float
    suspend fun setMinSpeed(value: Float)
    fun observeMinSpeed(): Flow<Float>

    
    suspend fun getPredictiveBlend(): Float
    suspend fun setPredictiveBlend(value: Float)
    fun observePredictiveBlend(): Flow<Float>

    
    suspend fun getSmoothingAlpha(): Float
    suspend fun setSmoothingAlpha(value: Float)
    fun observeSmoothingAlpha(): Flow<Float>

    
    suspend fun getTheme(): String
    suspend fun setTheme(theme: String)
    fun observeTheme(): Flow<String>
    suspend fun getAvailableThemes(): List<String>

    
    suspend fun isDynamicColorsEnabled(): Boolean
    suspend fun setDynamicColorsEnabled(enabled: Boolean)
    fun observeDynamicColorsEnabled(): Flow<Boolean>

    
    suspend fun getFontSize(): Float
    suspend fun setFontSize(value: Float)
    fun observeFontSize(): Flow<Float>

    
    suspend fun isDebugInfoEnabled(): Boolean
    suspend fun setDebugInfoEnabled(enabled: Boolean)
    fun observeDebugInfoEnabled(): Flow<Boolean>

    
    suspend fun isKeepScreenOn(): Boolean
    suspend fun setKeepScreenOn(enabled: Boolean)
    fun observeKeepScreenOn(): Flow<Boolean>

    
    suspend fun isShowFpsEnabled(): Boolean
    suspend fun setShowFpsEnabled(enabled: Boolean)
    fun observeShowFpsEnabled(): Flow<Boolean>

    
    suspend fun isHapticEnabled(): Boolean
    suspend fun setHapticEnabled(enabled: Boolean)
    fun observeHapticEnabled(): Flow<Boolean>
    suspend fun getHapticStrength(): String
    suspend fun setHapticStrength(strength: String)
    fun observeHapticStrength(): Flow<String>

    
    suspend fun isSoundEnabled(): Boolean
    suspend fun setSoundEnabled(enabled: Boolean)
    fun observeSoundEnabled(): Flow<Boolean>

    
    suspend fun isVisualFeedbackEnabled(): Boolean
    suspend fun setVisualFeedbackEnabled(enabled: Boolean)
    fun observeVisualFeedbackEnabled(): Flow<Boolean>

    
    suspend fun isNotificationOnGestureEnabled(): Boolean
    suspend fun setNotificationOnGestureEnabled(enabled: Boolean)
    fun observeNotificationOnGestureEnabled(): Flow<Boolean>

    
    suspend fun getLanguage(): String
    suspend fun setLanguage(language: String)
    fun observeLanguage(): Flow<String>
    suspend fun getAvailableLanguages(): List<String>

    
    suspend fun isAutoStartServer(): Boolean
    suspend fun setAutoStartServer(enabled: Boolean)
    fun observeAutoStartServer(): Flow<Boolean>

    
    suspend fun isAutoConnect(): Boolean
    suspend fun setAutoConnect(enabled: Boolean)
    fun observeAutoConnect(): Flow<Boolean>

    
    suspend fun getReconnectAttempts(): Int
    suspend fun setReconnectAttempts(attempts: Int)
    fun observeReconnectAttempts(): Flow<Int>

    
    suspend fun getConnectionTimeout(): Int
    suspend fun setConnectionTimeout(timeout: Int)
    fun observeConnectionTimeout(): Flow<Int>

    
    suspend fun isWebSocketEnabled(): Boolean
    suspend fun setWebSocketEnabled(enabled: Boolean)
    fun observeWebSocketEnabled(): Flow<Boolean>

    
    suspend fun isUdpDiscoveryEnabled(): Boolean
    suspend fun setUdpDiscoveryEnabled(enabled: Boolean)
    fun observeUdpDiscoveryEnabled(): Flow<Boolean>

    
    suspend fun getLogLevel(): String
    suspend fun setLogLevel(level: String)
    fun observeLogLevel(): Flow<String>
    suspend fun getAvailableLogLevels(): List<String>

    
    suspend fun getMovementProfile(): MovementProfile
    suspend fun setMovementProfile(profile: MovementProfile)
    suspend fun resetMovementProfile()
    fun observeMovementProfile(): Flow<MovementProfile>

    
    suspend fun getClickThreshold(): Float
    suspend fun setClickThreshold(threshold: Float)
    fun observeClickThreshold(): Flow<Float>

    suspend fun getDoubleClickInterval(): Long
    suspend fun setDoubleClickInterval(interval: Long)
    fun observeDoubleClickInterval(): Flow<Long>

    suspend fun getScrollThreshold(): Float
    suspend fun setScrollThreshold(threshold: Float)
    fun observeScrollThreshold(): Flow<Float>

    suspend fun getRightClickTilt(): Float
    suspend fun setRightClickTilt(tilt: Float)
    fun observeRightClickTilt(): Flow<Float>

    suspend fun getRightClickDuration(): Long
    suspend fun setRightClickDuration(duration: Long)
    fun observeRightClickDuration(): Flow<Long>

    suspend fun getGestureDebounce(): Long
    suspend fun setGestureDebounce(debounce: Long)
    fun observeGestureDebounce(): Flow<Long>

    
    suspend fun isAiSmoothingEnabled(): Boolean
    suspend fun setAiSmoothingEnabled(enabled: Boolean)
    fun observeAiSmoothingEnabled(): Flow<Boolean>

    suspend fun getAiBlendFactor(): Float
    suspend fun setAiBlendFactor(factor: Float)
    fun observeAiBlendFactor(): Flow<Float>

    suspend fun isPredictiveEnabled(): Boolean
    suspend fun setPredictiveEnabled(enabled: Boolean)
    fun observePredictiveEnabled(): Flow<Boolean>

    suspend fun getPredictionStrength(): Float
    suspend fun setPredictionStrength(strength: Float)
    fun observePredictionStrength(): Flow<Float>

    suspend fun isKalmanEnabled(): Boolean
    suspend fun setKalmanEnabled(enabled: Boolean)
    fun observeKalmanEnabled(): Flow<Boolean>

    
    suspend fun isAnonymousStatsEnabled(): Boolean
    suspend fun setAnonymousStatsEnabled(enabled: Boolean)
    fun observeAnonymousStatsEnabled(): Flow<Boolean>

    suspend fun isCrashReportingEnabled(): Boolean
    suspend fun setCrashReportingEnabled(enabled: Boolean)
    fun observeCrashReportingEnabled(): Flow<Boolean>

    suspend fun isClearDataOnExitEnabled(): Boolean
    suspend fun setClearDataOnExitEnabled(enabled: Boolean)
    fun observeClearDataOnExitEnabled(): Flow<Boolean>

    
    suspend fun isPresentationModeEnabled(): Boolean
    suspend fun setPresentationModeEnabled(enabled: Boolean)
    fun observePresentationModeEnabled(): Flow<Boolean>

    suspend fun getLaserPointerSpeed(): Float
    suspend fun setLaserPointerSpeed(speed: Float)
    fun observeLaserPointerSpeed(): Flow<Float>

    suspend fun isShowPresentationTimerEnabled(): Boolean
    suspend fun setShowPresentationTimerEnabled(enabled: Boolean)
    fun observeShowPresentationTimerEnabled(): Flow<Boolean>

    suspend fun isAutoHideLaserEnabled(): Boolean
    suspend fun setAutoHideLaserEnabled(enabled: Boolean)
    fun observeAutoHideLaserEnabled(): Flow<Boolean>

    
    suspend fun getProfileSettings(): ProfileSettings
    suspend fun saveProfileSettings(settings: ProfileSettings)
    fun observeProfileSettings(): Flow<ProfileSettings>

    
    suspend fun resetAllSettings()
}