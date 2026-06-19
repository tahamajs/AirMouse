// app/src/main/java/com/airmouse/domain/repository/ISettingsRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow

interface ISettingsRepository {

    // ==================== Sensitivity ====================
    suspend fun getSensitivity(): Float
    suspend fun setSensitivity(value: Float)
    fun observeSensitivity(): Flow<Float>

    // ==================== Smoothing ====================
    suspend fun isSmoothingEnabled(): Boolean
    suspend fun setSmoothingEnabled(enabled: Boolean)
    fun observeSmoothingEnabled(): Flow<Boolean>

    // ==================== Acceleration ====================
    suspend fun isAccelerationEnabled(): Boolean
    suspend fun setAccelerationEnabled(enabled: Boolean)
    fun observeAccelerationEnabled(): Flow<Boolean>
    suspend fun getAccelerationFactor(): Float
    suspend fun setAccelerationFactor(factor: Float)

    // ==================== Invert Axes ====================
    suspend fun isInvertX(): Boolean
    suspend fun setInvertX(enabled: Boolean)
    fun observeInvertX(): Flow<Boolean>
    suspend fun isInvertY(): Boolean
    suspend fun setInvertY(enabled: Boolean)
    fun observeInvertY(): Flow<Boolean>

    // ==================== Swap Axes ====================
    suspend fun isSwapAxes(): Boolean
    suspend fun setSwapAxes(enabled: Boolean)
    fun observeSwapAxes(): Flow<Boolean>

    // ==================== Deadband ====================
    suspend fun getDeadband(): Float
    suspend fun setDeadband(value: Float)
    fun observeDeadband(): Flow<Float>

    // ==================== Max Speed ====================
    suspend fun getMaxSpeed(): Float
    suspend fun setMaxSpeed(value: Float)
    fun observeMaxSpeed(): Flow<Float>

    // ==================== Min Speed ====================
    suspend fun getMinSpeed(): Float
    suspend fun setMinSpeed(value: Float)
    fun observeMinSpeed(): Flow<Float>

    // ==================== Predictive Blend ====================
    suspend fun getPredictiveBlend(): Float
    suspend fun setPredictiveBlend(value: Float)
    fun observePredictiveBlend(): Flow<Float>

    // ==================== Smoothing Alpha ====================
    suspend fun getSmoothingAlpha(): Float
    suspend fun setSmoothingAlpha(value: Float)
    fun observeSmoothingAlpha(): Flow<Float>

    // ==================== Theme ====================
    suspend fun getTheme(): String
    suspend fun setTheme(theme: String)
    fun observeTheme(): Flow<String>
    suspend fun getAvailableThemes(): List<String>

    // ==================== Dynamic Colors ====================
    suspend fun isDynamicColorsEnabled(): Boolean
    suspend fun setDynamicColorsEnabled(enabled: Boolean)
    fun observeDynamicColorsEnabled(): Flow<Boolean>

    // ==================== Font Size ====================
    suspend fun getFontSize(): Float
    suspend fun setFontSize(value: Float)
    fun observeFontSize(): Flow<Float>

    // ==================== Debug Info ====================
    suspend fun isDebugInfoEnabled(): Boolean
    suspend fun setDebugInfoEnabled(enabled: Boolean)
    fun observeDebugInfoEnabled(): Flow<Boolean>

    // ==================== Keep Screen On ====================
    suspend fun isKeepScreenOn(): Boolean
    suspend fun setKeepScreenOn(enabled: Boolean)
    fun observeKeepScreenOn(): Flow<Boolean>

    // ==================== Show FPS ====================
    suspend fun isShowFpsEnabled(): Boolean
    suspend fun setShowFpsEnabled(enabled: Boolean)
    fun observeShowFpsEnabled(): Flow<Boolean>

    // ==================== Haptic ====================
    suspend fun isHapticEnabled(): Boolean
    suspend fun setHapticEnabled(enabled: Boolean)
    fun observeHapticEnabled(): Flow<Boolean>
    suspend fun getHapticStrength(): String
    suspend fun setHapticStrength(strength: String)
    fun observeHapticStrength(): Flow<String>

    // ==================== Sound ====================
    suspend fun isSoundEnabled(): Boolean
    suspend fun setSoundEnabled(enabled: Boolean)
    fun observeSoundEnabled(): Flow<Boolean>

    // ==================== Visual Feedback ====================
    suspend fun isVisualFeedbackEnabled(): Boolean
    suspend fun setVisualFeedbackEnabled(enabled: Boolean)
    fun observeVisualFeedbackEnabled(): Flow<Boolean>

    // ==================== Notification on Gesture ====================
    suspend fun isNotificationOnGestureEnabled(): Boolean
    suspend fun setNotificationOnGestureEnabled(enabled: Boolean)
    fun observeNotificationOnGestureEnabled(): Flow<Boolean>

    // ==================== Language ====================
    suspend fun getLanguage(): String
    suspend fun setLanguage(language: String)
    fun observeLanguage(): Flow<String>
    suspend fun getAvailableLanguages(): List<String>

    // ==================== Auto-Start ====================
    suspend fun isAutoStartServer(): Boolean
    suspend fun setAutoStartServer(enabled: Boolean)
    fun observeAutoStartServer(): Flow<Boolean>

    // ==================== Auto-Connect ====================
    suspend fun isAutoConnect(): Boolean
    suspend fun setAutoConnect(enabled: Boolean)
    fun observeAutoConnect(): Flow<Boolean>

    // ==================== Reconnect Attempts ====================
    suspend fun getReconnectAttempts(): Int
    suspend fun setReconnectAttempts(attempts: Int)
    fun observeReconnectAttempts(): Flow<Int>

    // ==================== Connection Timeout ====================
    suspend fun getConnectionTimeout(): Int
    suspend fun setConnectionTimeout(timeout: Int)
    fun observeConnectionTimeout(): Flow<Int>

    // ==================== Use WebSocket ====================
    suspend fun isWebSocketEnabled(): Boolean
    suspend fun setWebSocketEnabled(enabled: Boolean)
    fun observeWebSocketEnabled(): Flow<Boolean>

    // ==================== UDP Discovery ====================
    suspend fun isUdpDiscoveryEnabled(): Boolean
    suspend fun setUdpDiscoveryEnabled(enabled: Boolean)
    fun observeUdpDiscoveryEnabled(): Flow<Boolean>

    // ==================== Log Level ====================
    suspend fun getLogLevel(): String
    suspend fun setLogLevel(level: String)
    fun observeLogLevel(): Flow<String>
    suspend fun getAvailableLogLevels(): List<String>

    // ==================== Movement Profile ====================
    suspend fun getMovementProfile(): MovementProfile
    suspend fun setMovementProfile(profile: MovementProfile)
    suspend fun resetMovementProfile()
    fun observeMovementProfile(): Flow<MovementProfile>

    // ==================== Click Detection ====================
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

    // ==================== AI & Predictive ====================
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

    // ==================== Privacy ====================
    suspend fun isAnonymousStatsEnabled(): Boolean
    suspend fun setAnonymousStatsEnabled(enabled: Boolean)
    fun observeAnonymousStatsEnabled(): Flow<Boolean>

    suspend fun isCrashReportingEnabled(): Boolean
    suspend fun setCrashReportingEnabled(enabled: Boolean)
    fun observeCrashReportingEnabled(): Flow<Boolean>

    suspend fun isClearDataOnExitEnabled(): Boolean
    suspend fun setClearDataOnExitEnabled(enabled: Boolean)
    fun observeClearDataOnExitEnabled(): Flow<Boolean>

    // ==================== Presentation ====================
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

    // ==================== Profile Settings ====================
    suspend fun getProfileSettings(): ProfileSettings
    suspend fun saveProfileSettings(settings: ProfileSettings)
    fun observeProfileSettings(): Flow<ProfileSettings>

    // ==================== Reset ====================
    suspend fun resetAllSettings()
}