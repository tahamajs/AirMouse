// app/src/main/java/com/airmouse/data/repository/SettingsRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ISettingsRepository {

    // ==================== State Flows ====================

    private val _sensitivity = MutableStateFlow(prefs.getSensitivity())
    override fun observeSensitivity(): Flow<Float> = _sensitivity.asStateFlow()

    private val _smoothingEnabled = MutableStateFlow(prefs.isSmoothingEnabled())
    override fun observeSmoothingEnabled(): Flow<Boolean> = _smoothingEnabled.asStateFlow()

    private val _accelerationEnabled = MutableStateFlow(prefs.isAccelerationEnabled())
    override fun observeAccelerationEnabled(): Flow<Boolean> = _accelerationEnabled.asStateFlow()

    private val _invertX = MutableStateFlow(prefs.isInvertX())
    override fun observeInvertX(): Flow<Boolean> = _invertX.asStateFlow()

    private val _invertY = MutableStateFlow(prefs.isInvertY())
    override fun observeInvertY(): Flow<Boolean> = _invertY.asStateFlow()

    private val _swapAxes = MutableStateFlow(prefs.getBoolean("swap_axes", false))
    override fun observeSwapAxes(): Flow<Boolean> = _swapAxes.asStateFlow()

    private val _deadband = MutableStateFlow(prefs.getFloat("deadband", 0.5f))
    override fun observeDeadband(): Flow<Float> = _deadband.asStateFlow()

    private val _maxSpeed = MutableStateFlow(prefs.getFloat("max_speed", 100f))
    override fun observeMaxSpeed(): Flow<Float> = _maxSpeed.asStateFlow()

    private val _minSpeed = MutableStateFlow(prefs.getFloat("min_speed", 0.5f))
    override fun observeMinSpeed(): Flow<Float> = _minSpeed.asStateFlow()

    private val _predictiveBlend = MutableStateFlow(prefs.getFloat("predictive_blend", 0.6f))
    override fun observePredictiveBlend(): Flow<Float> = _predictiveBlend.asStateFlow()

    private val _smoothingAlpha = MutableStateFlow(prefs.getFloat("smoothing_alpha", 0.3f))
    override fun observeSmoothingAlpha(): Flow<Float> = _smoothingAlpha.asStateFlow()

    private val _theme = MutableStateFlow(prefs.getTheme())
    override fun observeTheme(): Flow<String> = _theme.asStateFlow()

    private val _dynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", true))
    override fun observeDynamicColorsEnabled(): Flow<Boolean> = _dynamicColors.asStateFlow()

    private val _fontSize = MutableStateFlow(prefs.getFloat("font_size", 16f))
    override fun observeFontSize(): Flow<Float> = _fontSize.asStateFlow()

    private val _debugInfo = MutableStateFlow(prefs.getBoolean("show_debug_info", false))
    override fun observeDebugInfoEnabled(): Flow<Boolean> = _debugInfo.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", false))
    override fun observeKeepScreenOn(): Flow<Boolean> = _keepScreenOn.asStateFlow()

    private val _showFps = MutableStateFlow(prefs.getBoolean("show_fps", false))
    override fun observeShowFpsEnabled(): Flow<Boolean> = _showFps.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.isHapticEnabled())
    override fun observeHapticEnabled(): Flow<Boolean> = _hapticEnabled.asStateFlow()

    private val _hapticStrength = MutableStateFlow(prefs.getString("haptic_strength", "MEDIUM"))
    override fun observeHapticStrength(): Flow<String> = _hapticStrength.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", false))
    override fun observeSoundEnabled(): Flow<Boolean> = _soundEnabled.asStateFlow()

    private val _visualFeedback = MutableStateFlow(prefs.getBoolean("visual_feedback", true))
    override fun observeVisualFeedbackEnabled(): Flow<Boolean> = _visualFeedback.asStateFlow()

    private val _notificationOnGesture = MutableStateFlow(prefs.getBoolean("notification_on_gesture", false))
    override fun observeNotificationOnGestureEnabled(): Flow<Boolean> = _notificationOnGesture.asStateFlow()

    private val _language = MutableStateFlow(prefs.getLanguage())
    override fun observeLanguage(): Flow<String> = _language.asStateFlow()

    private val _autoStartServer = MutableStateFlow(prefs.isAutoStartServer())
    override fun observeAutoStartServer(): Flow<Boolean> = _autoStartServer.asStateFlow()

    private val _autoConnect = MutableStateFlow(prefs.getBoolean("auto_connect", true))
    override fun observeAutoConnect(): Flow<Boolean> = _autoConnect.asStateFlow()

    private val _reconnectAttempts = MutableStateFlow(prefs.getInt("reconnect_attempts", 5))
    override fun observeReconnectAttempts(): Flow<Int> = _reconnectAttempts.asStateFlow()

    private val _connectionTimeout = MutableStateFlow(prefs.getInt("connection_timeout", 5000))
    override fun observeConnectionTimeout(): Flow<Int> = _connectionTimeout.asStateFlow()

    private val _useWebSocket = MutableStateFlow(prefs.getBoolean("use_websocket", true))
    override fun observeWebSocketEnabled(): Flow<Boolean> = _useWebSocket.asStateFlow()

    private val _useUdpDiscovery = MutableStateFlow(prefs.getBoolean("use_udp_discovery", true))
    override fun observeUdpDiscoveryEnabled(): Flow<Boolean> = _useUdpDiscovery.asStateFlow()

    private val _logLevel = MutableStateFlow(prefs.getLogLevel())
    override fun observeLogLevel(): Flow<String> = _logLevel.asStateFlow()

    private val _aiSmoothing = MutableStateFlow(prefs.getBoolean("ai_smoothing", false))
    override fun observeAiSmoothingEnabled(): Flow<Boolean> = _aiSmoothing.asStateFlow()

    private val _aiBlendFactor = MutableStateFlow(prefs.getFloat("ai_blend_factor", 0.7f))
    override fun observeAiBlendFactor(): Flow<Float> = _aiBlendFactor.asStateFlow()

    private val _predictiveEnabled = MutableStateFlow(prefs.getBoolean("predictive_movement", true))
    override fun observePredictiveEnabled(): Flow<Boolean> = _predictiveEnabled.asStateFlow()

    private val _predictionStrength = MutableStateFlow(prefs.getFloat("prediction_strength", 0.5f))
    override fun observePredictionStrength(): Flow<Float> = _predictionStrength.asStateFlow()

    private val _kalmanEnabled = MutableStateFlow(prefs.getBoolean("kalman_enabled", true))
    override fun observeKalmanEnabled(): Flow<Boolean> = _kalmanEnabled.asStateFlow()

    private val _anonymousStats = MutableStateFlow(prefs.getBoolean("anonymous_stats", true))
    override fun observeAnonymousStatsEnabled(): Flow<Boolean> = _anonymousStats.asStateFlow()

    private val _crashReporting = MutableStateFlow(prefs.getBoolean("crash_reporting", true))
    override fun observeCrashReportingEnabled(): Flow<Boolean> = _crashReporting.asStateFlow()

    private val _clearDataOnExit = MutableStateFlow(prefs.getBoolean("clear_data_on_exit", false))
    override fun observeClearDataOnExitEnabled(): Flow<Boolean> = _clearDataOnExit.asStateFlow()

    private val _presentationMode = MutableStateFlow(prefs.getBoolean("presentation_mode_enabled", false))
    override fun observePresentationModeEnabled(): Flow<Boolean> = _presentationMode.asStateFlow()

    private val _laserPointerSpeed = MutableStateFlow(prefs.getFloat("laser_pointer_speed", 1.0f))
    override fun observeLaserPointerSpeed(): Flow<Float> = _laserPointerSpeed.asStateFlow()

    private val _showPresentationTimer = MutableStateFlow(prefs.getBoolean("show_presentation_timer", true))
    override fun observeShowPresentationTimerEnabled(): Flow<Boolean> = _showPresentationTimer.asStateFlow()

    private val _autoHideLaser = MutableStateFlow(prefs.getBoolean("auto_hide_laser", true))
    override fun observeAutoHideLaserEnabled(): Flow<Boolean> = _autoHideLaser.asStateFlow()

    private val _clickThreshold = MutableStateFlow(prefs.getFloat("click_threshold", 5.0f))
    override fun observeClickThreshold(): Flow<Float> = _clickThreshold.asStateFlow()

    private val _doubleClickInterval = MutableStateFlow(prefs.getLong("double_click_interval", 400L))
    override fun observeDoubleClickInterval(): Flow<Long> = _doubleClickInterval.asStateFlow()

    private val _scrollThreshold = MutableStateFlow(prefs.getFloat("scroll_threshold", 8.0f))
    override fun observeScrollThreshold(): Flow<Float> = _scrollThreshold.asStateFlow()

    private val _rightClickTilt = MutableStateFlow(prefs.getFloat("right_click_tilt", 45f))
    override fun observeRightClickTilt(): Flow<Float> = _rightClickTilt.asStateFlow()

    private val _rightClickDuration = MutableStateFlow(prefs.getLong("right_click_duration", 500L))
    override fun observeRightClickDuration(): Flow<Long> = _rightClickDuration.asStateFlow()

    private val _gestureDebounce = MutableStateFlow(prefs.getLong("gesture_debounce", 100L))
    override fun observeGestureDebounce(): Flow<Long> = _gestureDebounce.asStateFlow()

    private val _profileSettings = MutableStateFlow(ProfileSettings())
    override fun observeProfileSettings(): Flow<ProfileSettings> = _profileSettings.asStateFlow()

    private val _movementProfile = MutableStateFlow(MovementProfile())
    override fun observeMovementProfile(): Flow<MovementProfile> = _movementProfile.asStateFlow()

    // ==================== Sensitivity ====================

    override suspend fun getSensitivity(): Float {
        return prefs.getSensitivity()
    }

    override suspend fun setSensitivity(value: Float) {
        val clamped = value.coerceIn(0.1f, 3.0f)
        prefs.setSensitivity(clamped)
        _sensitivity.value = clamped
        updateProfileSettings()
        updateMovementProfile()
    }

    // ==================== Smoothing ====================

    override suspend fun isSmoothingEnabled(): Boolean {
        return prefs.isSmoothingEnabled()
    }

    override suspend fun setSmoothingEnabled(enabled: Boolean) {
        prefs.setSmoothingEnabled(enabled)
        _smoothingEnabled.value = enabled
        updateProfileSettings()
        updateMovementProfile()
    }

    // ==================== Acceleration ====================

    override suspend fun isAccelerationEnabled(): Boolean {
        return prefs.isAccelerationEnabled()
    }

    override suspend fun setAccelerationEnabled(enabled: Boolean) {
        prefs.setAccelerationEnabled(enabled)
        _accelerationEnabled.value = enabled
        updateProfileSettings()
        updateMovementProfile()
    }

    override suspend fun getAccelerationFactor(): Float {
        return prefs.getFloat("acceleration_factor", 1.5f)
    }

    override suspend fun setAccelerationFactor(factor: Float) {
        val clamped = factor.coerceIn(1.0f, 3.0f)
        prefs.putFloat("acceleration_factor", clamped)
        updateMovementProfile()
    }

    // ==================== Invert Axes ====================

    override suspend fun isInvertX(): Boolean {
        return prefs.isInvertX()
    }

    override suspend fun setInvertX(enabled: Boolean) {
        prefs.setInvertX(enabled)
        _invertX.value = enabled
        updateProfileSettings()
        updateMovementProfile()
    }

    override suspend fun isInvertY(): Boolean {
        return prefs.isInvertY()
    }

    override suspend fun setInvertY(enabled: Boolean) {
        prefs.setInvertY(enabled)
        _invertY.value = enabled
        updateProfileSettings()
        updateMovementProfile()
    }

    // ==================== Swap Axes ====================

    override suspend fun isSwapAxes(): Boolean {
        return prefs.getBoolean("swap_axes", false)
    }

    override suspend fun setSwapAxes(enabled: Boolean) {
        prefs.putBoolean("swap_axes", enabled)
        _swapAxes.value = enabled
        updateMovementProfile()
    }

    // ==================== Deadband ====================

    override suspend fun getDeadband(): Float {
        return prefs.getFloat("deadband", 0.5f)
    }

    override suspend fun setDeadband(value: Float) {
        val clamped = value.coerceIn(0f, 5f)
        prefs.putFloat("deadband", clamped)
        _deadband.value = clamped
        updateMovementProfile()
    }

    // ==================== Max Speed ====================

    override suspend fun getMaxSpeed(): Float {
        return prefs.getFloat("max_speed", 100f)
    }

    override suspend fun setMaxSpeed(value: Float) {
        val clamped = value.coerceIn(10f, 500f)
        prefs.putFloat("max_speed", clamped)
        _maxSpeed.value = clamped
        updateMovementProfile()
    }

    // ==================== Min Speed ====================

    override suspend fun getMinSpeed(): Float {
        return prefs.getFloat("min_speed", 0.5f)
    }

    override suspend fun setMinSpeed(value: Float) {
        val clamped = value.coerceIn(0.1f, 10f)
        prefs.putFloat("min_speed", clamped)
        _minSpeed.value = clamped
        updateMovementProfile()
    }

    // ==================== Predictive Blend ====================

    override suspend fun getPredictiveBlend(): Float {
        return prefs.getFloat("predictive_blend", 0.6f)
    }

    override suspend fun setPredictiveBlend(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        prefs.putFloat("predictive_blend", clamped)
        _predictiveBlend.value = clamped
        updateMovementProfile()
    }

    // ==================== Smoothing Alpha ====================

    override suspend fun getSmoothingAlpha(): Float {
        return prefs.getFloat("smoothing_alpha", 0.3f)
    }

    override suspend fun setSmoothingAlpha(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        prefs.putFloat("smoothing_alpha", clamped)
        _smoothingAlpha.value = clamped
        updateMovementProfile()
    }

    // ==================== Theme ====================

    override suspend fun getTheme(): String {
        return prefs.getTheme()
    }

    override suspend fun setTheme(theme: String) {
        prefs.setTheme(theme)
        _theme.value = theme
        updateProfileSettings()
    }

    override suspend fun getAvailableThemes(): List<String> {
        return listOf(
            "system", "dark", "light", "pure_black", "high_contrast",
            "ocean", "sunset", "forest", "purple", "cherry",
            "neon", "lavender", "mint", "peach", "sky"
        )
    }

    // ==================== Dynamic Colors ====================

    override suspend fun isDynamicColorsEnabled(): Boolean {
        return prefs.getBoolean("dynamic_colors", true)
    }

    override suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        prefs.putBoolean("dynamic_colors", enabled)
        _dynamicColors.value = enabled
    }

    // ==================== Font Size ====================

    override suspend fun getFontSize(): Float {
        return prefs.getFloat("font_size", 16f)
    }

    override suspend fun setFontSize(value: Float) {
        val clamped = value.coerceIn(10f, 30f)
        prefs.putFloat("font_size", clamped)
        _fontSize.value = clamped
    }

    // ==================== Debug Info ====================

    override suspend fun isDebugInfoEnabled(): Boolean {
        return prefs.getBoolean("show_debug_info", false)
    }

    override suspend fun setDebugInfoEnabled(enabled: Boolean) {
        prefs.putBoolean("show_debug_info", enabled)
        _debugInfo.value = enabled
    }

    // ==================== Keep Screen On ====================

    override suspend fun isKeepScreenOn(): Boolean {
        return prefs.getBoolean("keep_screen_on", false)
    }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        prefs.putBoolean("keep_screen_on", enabled)
        _keepScreenOn.value = enabled
    }

    // ==================== Show FPS ====================

    override suspend fun isShowFpsEnabled(): Boolean {
        return prefs.getBoolean("show_fps", false)
    }

    override suspend fun setShowFpsEnabled(enabled: Boolean) {
        prefs.putBoolean("show_fps", enabled)
        _showFps.value = enabled
    }

    // ==================== Haptic ====================

    override suspend fun isHapticEnabled(): Boolean {
        return prefs.isHapticEnabled()
    }

    override suspend fun setHapticEnabled(enabled: Boolean) {
        prefs.setHapticEnabled(enabled)
        _hapticEnabled.value = enabled
        updateProfileSettings()
    }

    override suspend fun getHapticStrength(): String {
        return prefs.getString("haptic_strength", "MEDIUM")
    }

    override suspend fun setHapticStrength(strength: String) {
        val validStrengths = listOf("LIGHT", "MEDIUM", "STRONG")
        val normalized = strength.uppercase()
        if (normalized in validStrengths) {
            prefs.putString("haptic_strength", normalized)
            _hapticStrength.value = normalized
        }
    }

    // ==================== Sound ====================

    override suspend fun isSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", false)
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        prefs.putBoolean("sound_enabled", enabled)
        _soundEnabled.value = enabled
    }

    // ==================== Visual Feedback ====================

    override suspend fun isVisualFeedbackEnabled(): Boolean {
        return prefs.getBoolean("visual_feedback", true)
    }

    override suspend fun setVisualFeedbackEnabled(enabled: Boolean) {
        prefs.putBoolean("visual_feedback", enabled)
        _visualFeedback.value = enabled
    }

    // ==================== Notification on Gesture ====================

    override suspend fun isNotificationOnGestureEnabled(): Boolean {
        return prefs.getBoolean("notification_on_gesture", false)
    }

    override suspend fun setNotificationOnGestureEnabled(enabled: Boolean) {
        prefs.putBoolean("notification_on_gesture", enabled)
        _notificationOnGesture.value = enabled
    }

    // ==================== Language ====================

    override suspend fun getLanguage(): String {
        return prefs.getLanguage()
    }

    override suspend fun setLanguage(language: String) {
        prefs.setLanguage(language)
        _language.value = language
    }

    override suspend fun getAvailableLanguages(): List<String> {
        return listOf("en", "fa", "es", "fr", "de", "zh", "ja", "ko", "ru", "ar")
    }

    // ==================== Auto-Start ====================

    override suspend fun isAutoStartServer(): Boolean {
        return prefs.isAutoStartServer()
    }

    override suspend fun setAutoStartServer(enabled: Boolean) {
        prefs.setAutoStartServer(enabled)
        _autoStartServer.value = enabled
    }

    // ==================== Auto-Connect ====================

    override suspend fun isAutoConnect(): Boolean {
        return prefs.getBoolean("auto_connect", true)
    }

    override suspend fun setAutoConnect(enabled: Boolean) {
        prefs.putBoolean("auto_connect", enabled)
        _autoConnect.value = enabled
    }

    // ==================== Reconnect Attempts ====================

    override suspend fun getReconnectAttempts(): Int {
        return prefs.getInt("reconnect_attempts", 5)
    }

    override suspend fun setReconnectAttempts(attempts: Int) {
        val clamped = attempts.coerceIn(1, 20)
        prefs.putInt("reconnect_attempts", clamped)
        _reconnectAttempts.value = clamped
    }

    // ==================== Connection Timeout ====================

    override suspend fun getConnectionTimeout(): Int {
        return prefs.getInt("connection_timeout", 5000)
    }

    override suspend fun setConnectionTimeout(timeout: Int) {
        val clamped = timeout.coerceIn(1000, 30000)
        prefs.putInt("connection_timeout", clamped)
        _connectionTimeout.value = clamped
    }

    // ==================== Use WebSocket ====================

    override suspend fun isWebSocketEnabled(): Boolean {
        return prefs.getBoolean("use_websocket", true)
    }

    override suspend fun setWebSocketEnabled(enabled: Boolean) {
        prefs.putBoolean("use_websocket", enabled)
        _useWebSocket.value = enabled
    }

    // ==================== UDP Discovery ====================

    override suspend fun isUdpDiscoveryEnabled(): Boolean {
        return prefs.getBoolean("use_udp_discovery", true)
    }

    override suspend fun setUdpDiscoveryEnabled(enabled: Boolean) {
        prefs.putBoolean("use_udp_discovery", enabled)
        _useUdpDiscovery.value = enabled
    }

    // ==================== Log Level ====================

    override suspend fun getLogLevel(): String {
        return prefs.getLogLevel()
    }

    override suspend fun setLogLevel(level: String) {
        val validLevels = listOf("debug", "info", "warn", "error")
        val normalized = level.lowercase()
        if (normalized in validLevels) {
            prefs.setLogLevel(normalized)
            _logLevel.value = normalized
        }
    }

    override suspend fun getAvailableLogLevels(): List<String> {
        return listOf("debug", "info", "warn", "error")
    }

    // ==================== Movement Profile ====================

    override suspend fun getMovementProfile(): MovementProfile {
        return MovementProfile(
            sensitivity = prefs.getSensitivity(),
            smoothingEnabled = prefs.isSmoothingEnabled(),
            accelerationEnabled = prefs.isAccelerationEnabled(),
            accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
            invertX = prefs.isInvertX(),
            invertY = prefs.isInvertY(),
            swapAxes = prefs.getBoolean("swap_axes", false),
            deadband = prefs.getFloat("deadband", 0.5f),
            maxSpeed = prefs.getFloat("max_speed", 100f),
            minSpeed = prefs.getFloat("min_speed", 0.5f),
            predictiveBlend = prefs.getFloat("predictive_blend", 0.6f),
            smoothingAlpha = prefs.getFloat("smoothing_alpha", 0.3f)
        )
    }

    override suspend fun setMovementProfile(profile: MovementProfile) {
        setSensitivity(profile.sensitivity)
        setSmoothingEnabled(profile.smoothingEnabled)
        setAccelerationEnabled(profile.accelerationEnabled)
        setAccelerationFactor(profile.accelerationFactor)
        setInvertX(profile.invertX)
        setInvertY(profile.invertY)
        setSwapAxes(profile.swapAxes)
        setDeadband(profile.deadband)
        setMaxSpeed(profile.maxSpeed)
        setMinSpeed(profile.minSpeed)
        setPredictiveBlend(profile.predictiveBlend)
        setSmoothingAlpha(profile.smoothingAlpha)
        _movementProfile.value = profile
    }

    override suspend fun resetMovementProfile() {
        val default = MovementProfile()
        setMovementProfile(default)
    }

    // ==================== Click Detection ====================

    override suspend fun getClickThreshold(): Float {
        return prefs.getFloat("click_threshold", 5.0f)
    }

    override suspend fun setClickThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0.5f, 20f)
        prefs.putFloat("click_threshold", clamped)
        _clickThreshold.value = clamped
        updateProfileSettings()
    }

    override suspend fun getDoubleClickInterval(): Long {
        return prefs.getLong("double_click_interval", 400L)
    }

    override suspend fun setDoubleClickInterval(interval: Long) {
        val clamped = interval.coerceIn(200L, 1000L)
        prefs.putLong("double_click_interval", clamped)
        _doubleClickInterval.value = clamped
        updateProfileSettings()
    }

    override suspend fun getScrollThreshold(): Float {
        return prefs.getFloat("scroll_threshold", 8.0f)
    }

    override suspend fun setScrollThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(1f, 20f)
        prefs.putFloat("scroll_threshold", clamped)
        _scrollThreshold.value = clamped
        updateProfileSettings()
    }

    override suspend fun getRightClickTilt(): Float {
        return prefs.getFloat("right_click_tilt", 45f)
    }

    override suspend fun setRightClickTilt(tilt: Float) {
        val clamped = tilt.coerceIn(10f, 90f)
        prefs.putFloat("right_click_tilt", clamped)
        _rightClickTilt.value = clamped
        updateProfileSettings()
    }

    override suspend fun getRightClickDuration(): Long {
        return prefs.getLong("right_click_duration", 500L)
    }

    override suspend fun setRightClickDuration(duration: Long) {
        val clamped = duration.coerceIn(200L, 1000L)
        prefs.putLong("right_click_duration", clamped)
        _rightClickDuration.value = clamped
        updateProfileSettings()
    }

    override suspend fun getGestureDebounce(): Long {
        return prefs.getLong("gesture_debounce", 100L)
    }

    override suspend fun setGestureDebounce(debounce: Long) {
        val clamped = debounce.coerceIn(50L, 500L)
        prefs.putLong("gesture_debounce", clamped)
        _gestureDebounce.value = clamped
    }

    // ==================== AI & Predictive ====================

    override suspend fun isAiSmoothingEnabled(): Boolean {
        return prefs.getBoolean("ai_smoothing", false)
    }

    override suspend fun setAiSmoothingEnabled(enabled: Boolean) {
        prefs.putBoolean("ai_smoothing", enabled)
        _aiSmoothing.value = enabled
        updateProfileSettings()
    }

    override suspend fun getAiBlendFactor(): Float {
        return prefs.getFloat("ai_blend_factor", 0.7f)
    }

    override suspend fun setAiBlendFactor(factor: Float) {
        val clamped = factor.coerceIn(0f, 1f)
        prefs.putFloat("ai_blend_factor", clamped)
        _aiBlendFactor.value = clamped
    }

    override suspend fun isPredictiveEnabled(): Boolean {
        return prefs.getBoolean("predictive_movement", true)
    }

    override suspend fun setPredictiveEnabled(enabled: Boolean) {
        prefs.putBoolean("predictive_movement", enabled)
        _predictiveEnabled.value = enabled
        updateProfileSettings()
    }

    override suspend fun getPredictionStrength(): Float {
        return prefs.getFloat("prediction_strength", 0.5f)
    }

    override suspend fun setPredictionStrength(strength: Float) {
        val clamped = strength.coerceIn(0f, 1f)
        prefs.putFloat("prediction_strength", clamped)
        _predictionStrength.value = clamped
    }

    override suspend fun isKalmanEnabled(): Boolean {
        return prefs.getBoolean("kalman_enabled", true)
    }

    override suspend fun setKalmanEnabled(enabled: Boolean) {
        prefs.putBoolean("kalman_enabled", enabled)
        _kalmanEnabled.value = enabled
    }

    // ==================== Privacy ====================

    override suspend fun isAnonymousStatsEnabled(): Boolean {
        return prefs.getBoolean("anonymous_stats", true)
    }

    override suspend fun setAnonymousStatsEnabled(enabled: Boolean) {
        prefs.putBoolean("anonymous_stats", enabled)
        _anonymousStats.value = enabled
    }

    override suspend fun isCrashReportingEnabled(): Boolean {
        return prefs.getBoolean("crash_reporting", true)
    }

    override suspend fun setCrashReportingEnabled(enabled: Boolean) {
        prefs.putBoolean("crash_reporting", enabled)
        _crashReporting.value = enabled
    }

    override suspend fun isClearDataOnExitEnabled(): Boolean {
        return prefs.getBoolean("clear_data_on_exit", false)
    }

    override suspend fun setClearDataOnExitEnabled(enabled: Boolean) {
        prefs.putBoolean("clear_data_on_exit", enabled)
        _clearDataOnExit.value = enabled
    }

    // ==================== Presentation ====================

    override suspend fun isPresentationModeEnabled(): Boolean {
        return prefs.getBoolean("presentation_mode_enabled", false)
    }

    override suspend fun setPresentationModeEnabled(enabled: Boolean) {
        prefs.putBoolean("presentation_mode_enabled", enabled)
        _presentationMode.value = enabled
    }

    override suspend fun getLaserPointerSpeed(): Float {
        return prefs.getFloat("laser_pointer_speed", 1.0f)
    }

    override suspend fun setLaserPointerSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.1f, 3.0f)
        prefs.putFloat("laser_pointer_speed", clamped)
        _laserPointerSpeed.value = clamped
    }

    override suspend fun isShowPresentationTimerEnabled(): Boolean {
        return prefs.getBoolean("show_presentation_timer", true)
    }

    override suspend fun setShowPresentationTimerEnabled(enabled: Boolean) {
        prefs.putBoolean("show_presentation_timer", enabled)
        _showPresentationTimer.value = enabled
    }

    override suspend fun isAutoHideLaserEnabled(): Boolean {
        return prefs.getBoolean("auto_hide_laser", true)
    }

    override suspend fun setAutoHideLaserEnabled(enabled: Boolean) {
        prefs.putBoolean("auto_hide_laser", enabled)
        _autoHideLaser.value = enabled
    }

    // ==================== Profile Settings ====================

    override suspend fun getProfileSettings(): ProfileSettings {
        return ProfileSettings(
            sensitivity = prefs.getSensitivity(),
            clickThreshold = prefs.getFloat("click_threshold", 5.0f),
            doubleClickInterval = prefs.getLong("double_click_interval", 400L),
            scrollThreshold = prefs.getFloat("scroll_threshold", 8.0f),
            rightClickTilt = prefs.getFloat("right_click_tilt", 45f),
            hapticEnabled = prefs.isHapticEnabled(),
            theme = prefs.getTheme(),
            aiSmoothing = prefs.getBoolean("ai_smoothing", false),
            predictiveMovement = prefs.getBoolean("predictive_movement", true),
            invertX = prefs.isInvertX(),
            invertY = prefs.isInvertY(),
            accelerationEnabled = prefs.isAccelerationEnabled(),
            smoothingEnabled = prefs.isSmoothingEnabled(),
            edgeGesturesEnabled = prefs.getBoolean("edge_gestures_enabled", false),
            voiceCommandsEnabled = prefs.getBoolean("voice_commands_enabled", false)
        )
    }

    override suspend fun saveProfileSettings(settings: ProfileSettings) {
        setSensitivity(settings.sensitivity)
        setClickThreshold(settings.clickThreshold)
        setDoubleClickInterval(settings.doubleClickInterval)
        setScrollThreshold(settings.scrollThreshold)
        setRightClickTilt(settings.rightClickTilt)
        setHapticEnabled(settings.hapticEnabled)
        setTheme(settings.theme)
        setAiSmoothingEnabled(settings.aiSmoothing)
        setPredictiveEnabled(settings.predictiveMovement)
        setInvertX(settings.invertX)
        setInvertY(settings.invertY)
        setAccelerationEnabled(settings.accelerationEnabled)
        setSmoothingEnabled(settings.smoothingEnabled)
        prefs.putBoolean("edge_gestures_enabled", settings.edgeGesturesEnabled)
        prefs.putBoolean("voice_commands_enabled", settings.voiceCommandsEnabled)
        _profileSettings.value = settings
    }

    // ==================== Reset ====================

    override suspend fun resetAllSettings() {
        // Reset all settings to defaults
        setSensitivity(1.0f)
        setSmoothingEnabled(true)
        setAccelerationEnabled(true)
        setAccelerationFactor(1.5f)
        setInvertX(false)
        setInvertY(false)
        setSwapAxes(false)
        setDeadband(0.5f)
        setMaxSpeed(100f)
        setMinSpeed(0.5f)
        setPredictiveBlend(0.6f)
        setSmoothingAlpha(0.3f)

        setClickThreshold(5.0f)
        setDoubleClickInterval(400L)
        setScrollThreshold(8.0f)
        setRightClickTilt(45f)
        setRightClickDuration(500L)
        setGestureDebounce(100L)
        setHapticEnabled(true)
        setHapticStrength("MEDIUM")
        setSoundEnabled(false)
        setVisualFeedbackEnabled(true)
        setNotificationOnGestureEnabled(false)

        setTheme("system")
        setDynamicColorsEnabled(true)
        setFontSize(16f)
        setDebugInfoEnabled(false)
        setKeepScreenOn(false)
        setShowFpsEnabled(false)

        setAutoConnect(true)
        setReconnectAttempts(5)
        setConnectionTimeout(5000)
        setWebSocketEnabled(true)
        setUdpDiscoveryEnabled(true)

        setAiSmoothingEnabled(false)
        setAiBlendFactor(0.7f)
        setPredictiveEnabled(true)
        setPredictionStrength(0.5f)
        setKalmanEnabled(true)

        setAnonymousStatsEnabled(true)
        setCrashReportingEnabled(true)
        setClearDataOnExitEnabled(false)

        setPresentationModeEnabled(false)
        setLaserPointerSpeed(1.0f)
        setShowPresentationTimerEnabled(true)
        setAutoHideLaserEnabled(true)

        setLanguage("en")
        setLogLevel("info")
        setAutoStartServer(false)

        updateProfileSettings()
        updateMovementProfile()
    }

    // ==================== Private Helpers ====================

    private suspend fun updateProfileSettings() {
        _profileSettings.value = getProfileSettings()
    }

    private suspend fun updateMovementProfile() {
        _movementProfile.value = getMovementProfile()
    }
}