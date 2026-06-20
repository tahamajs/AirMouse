// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsViewModel.kt
package com.airmouse.presentation.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.features.MouseControlFeature
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val mouseControlFeature: MouseControlFeature,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<SettingsEffect?>(null)
    val effect: StateFlow<SettingsEffect?> = _effect.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            val savedTheme = prefs.getString("theme", "system")
            it.copy(
                // ==================== CURSOR SETTINGS ====================
                sensitivity = prefs.getFloat("sensitivity", 0.5f),
                accelerationEnabled = prefs.getBoolean("acceleration_enabled", false),
                accelerationFactor = prefs.getFloat("acceleration_factor", 1.5f),
                invertX = prefs.getBoolean("invert_x", false),
                invertY = prefs.getBoolean("invert_y", false),
                smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
                smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f),

                // ==================== GESTURE SETTINGS ====================
                clickThreshold = prefs.getFloat("click_threshold", 10f),
                doubleClickInterval = prefs.getLong("double_click_interval", 300L),
                scrollThreshold = prefs.getFloat("scroll_threshold", 5f),
                rightClickTilt = prefs.getFloat("right_click_tilt", 15f),
                rightClickDuration = prefs.getLong("right_click_duration", 500L),
                gestureDebounce = prefs.getLong("gesture_debounce", 100L),

                // ==================== AI SETTINGS ====================
                aiSmoothing = prefs.getBoolean("ai_smoothing", false),
                aiBlendFactor = prefs.getFloat("ai_blend_factor", 0.7f),
                predictive = prefs.getBoolean("predictive_movement", true),
                predictionStrength = prefs.getFloat("prediction_strength", 0.5f),
                kalmanEnabled = prefs.getBoolean("kalman_enabled", true),

                // ==================== HAPTIC SETTINGS ====================
                hapticEnabled = prefs.getBoolean("haptic_enabled", true),
                hapticStrength = getHapticStrength(prefs.getString("haptic_strength", "MEDIUM")),
                soundEnabled = prefs.getBoolean("sound_enabled", false),
                visualFeedback = prefs.getBoolean("visual_feedback", true),
                notificationOnGesture = prefs.getBoolean("notification_on_gesture", false),

                // ==================== DISPLAY SETTINGS ====================
                theme = normalizeThemeId(savedTheme),
                useDynamicColors = prefs.getBoolean("dynamic_colors", true),
                fontSize = prefs.getFloat("font_size", 16f),
                showDebugInfo = prefs.getBoolean("show_debug_info", false),
                keepScreenOn = prefs.getBoolean("keep_screen_on", false),
                showFps = prefs.getBoolean("show_fps", false),

                // ==================== TOUCHPAD SETTINGS ====================
                touchpadActive = prefs.getBoolean("touchpad_active", false),
                touchpadSensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
                touchpadCursorSpeed = prefs.getFloat("touchpad_cursor_speed", 1.0f),
                touchpadPointerSpeed = prefs.getInt("touchpad_pointer_speed", 50),
                touchpadAccelerationEnabled = prefs.getBoolean("touchpad_acceleration", true),
                touchpadInvertVertical = prefs.getBoolean("touchpad_invert_vertical", false),
                touchpadInvertHorizontal = prefs.getBoolean("touchpad_invert_horizontal", false),
                touchpadScrollSpeed = prefs.getFloat("touchpad_scroll_speed", 1.0f),
                touchpadNaturalScrolling = prefs.getBoolean("touchpad_natural_scrolling", true),
                touchpadTwoFingerScroll = prefs.getBoolean("touchpad_two_finger_scroll", true),
                touchpadEdgeScrolling = prefs.getBoolean("touchpad_edge_scrolling", false),
                touchpadScrollInertia = prefs.getBoolean("touchpad_scroll_inertia", true),
                touchpadTapToClick = prefs.getBoolean("touchpad_tap_to_click", true),
                touchpadDoubleTapDelay = prefs.getInt("touchpad_double_tap_delay", 300),
                touchpadThreeFingerSwipe = prefs.getBoolean("touchpad_three_finger_swipe", true),
                touchpadPinchToZoom = prefs.getBoolean("touchpad_pinch_to_zoom", true),
                touchpadRotateToRotate = prefs.getBoolean("touchpad_rotate_to_rotate", false),
                touchpadHapticFeedback = prefs.getBoolean("touchpad_haptic_feedback", true),
                touchpadShowTouchPoints = prefs.getBoolean("touchpad_show_touch_points", false),

                // ==================== CONNECTION SETTINGS ====================
                autoConnect = prefs.getBoolean("auto_connect", true),
                reconnectAttempts = prefs.getInt("reconnect_attempts", 5),
                connectionTimeout = prefs.getInt("connection_timeout", 5000),
                useWebSocket = prefs.getBoolean("use_websocket", true),
                useUdpDiscovery = prefs.getBoolean("use_udp_discovery", true),

                // ==================== PRIVACY SETTINGS ====================
                anonymousStats = prefs.getBoolean("anonymous_stats", true),
                crashReporting = prefs.getBoolean("crash_reporting", true),
                clearDataOnExit = prefs.getBoolean("clear_data_on_exit", false),

                // ==================== PRESENTATION SETTINGS ====================
                presentationModeEnabled = prefs.getBoolean("presentation_mode_enabled", false),
                laserPointerSpeed = prefs.getFloat("laser_pointer_speed", 1.0f),
                showPresentationTimer = prefs.getBoolean("show_presentation_timer", true),
                autoHideLaser = prefs.getBoolean("auto_hide_laser", true)
            )
        }
    }

    private fun getHapticStrength(value: String): HapticStrength = when (value) {
        "LIGHT" -> HapticStrength.LIGHT
        "STRONG" -> HapticStrength.STRONG
        else -> HapticStrength.MEDIUM
    }

    private fun normalizeThemeId(theme: String): String {
        return when (theme.lowercase(Locale.ROOT)) {
            "system", "light", "dark", "pure_black", "high_contrast", "dynamic" -> theme.lowercase(Locale.ROOT)
            else -> "system"
        }
    }

    private suspend fun showToast(message: String) {
        _effect.value = SettingsEffect.ShowToast(message)
        delay(3000)
        _effect.value = null
    }

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                // ==================== CURSOR EVENTS ====================
                is SettingsEvent.UpdateSensitivity -> updateSensitivity(event.value)
                SettingsEvent.ToggleAcceleration -> toggleAcceleration()
                is SettingsEvent.UpdateAccelerationFactor -> updateAccelerationFactor(event.value)
                SettingsEvent.ToggleInvertX -> toggleInvertX()
                SettingsEvent.ToggleInvertY -> toggleInvertY()
                SettingsEvent.ToggleSmoothing -> toggleSmoothing()
                is SettingsEvent.UpdateSmoothingFactor -> updateSmoothingFactor(event.value)

                // ==================== GESTURE EVENTS ====================
                is SettingsEvent.UpdateClickThreshold -> updateClickThreshold(event.value)
                is SettingsEvent.UpdateDoubleClickInterval -> updateDoubleClickInterval(event.value)
                is SettingsEvent.UpdateScrollThreshold -> updateScrollThreshold(event.value)
                is SettingsEvent.UpdateRightClickTilt -> updateRightClickTilt(event.value)
                is SettingsEvent.UpdateRightClickDuration -> updateRightClickDuration(event.value)

                // ==================== AI EVENTS ====================
                SettingsEvent.ToggleAiSmoothing -> toggleAiSmoothing()
                is SettingsEvent.UpdateAiBlendFactor -> updateAiBlendFactor(event.value)
                SettingsEvent.TogglePredictive -> togglePredictive()
                is SettingsEvent.UpdatePredictionStrength -> updatePredictionStrength(event.value)
                SettingsEvent.ToggleKalman -> toggleKalman()

                // ==================== HAPTIC EVENTS ====================
                SettingsEvent.ToggleHaptic -> toggleHaptic()
                is SettingsEvent.UpdateHapticStrength -> updateHapticStrength(event.strength)
                SettingsEvent.ToggleSound -> toggleSound()
                SettingsEvent.ToggleVisualFeedback -> toggleVisualFeedback()
                SettingsEvent.ToggleNotificationOnGesture -> toggleNotificationOnGesture()

                // ==================== DISPLAY EVENTS ====================
                is SettingsEvent.UpdateTheme -> updateTheme(event.theme)
                SettingsEvent.ToggleDynamicColors -> toggleDynamicColors()
                is SettingsEvent.UpdateFontSize -> updateFontSize(event.value)
                SettingsEvent.ToggleDebugInfo -> toggleDebugInfo()
                SettingsEvent.ToggleKeepScreenOn -> toggleKeepScreenOn()
                SettingsEvent.ToggleShowFps -> toggleShowFps()

                // ==================== TOUCHPAD EVENTS ====================
                SettingsEvent.ToggleTouchpadActive -> toggleTouchpadActive()
                is SettingsEvent.UpdateTouchpadSensitivity -> updateTouchpadSensitivity(event.value)
                is SettingsEvent.UpdateTouchpadCursorSpeed -> updateTouchpadCursorSpeed(event.value)
                is SettingsEvent.UpdateTouchpadPointerSpeed -> updateTouchpadPointerSpeed(event.value)
                SettingsEvent.ToggleTouchpadAcceleration -> toggleTouchpadAcceleration()
                SettingsEvent.ToggleTouchpadInvertVertical -> toggleTouchpadInvertVertical()
                SettingsEvent.ToggleTouchpadInvertHorizontal -> toggleTouchpadInvertHorizontal()
                is SettingsEvent.UpdateTouchpadScrollSpeed -> updateTouchpadScrollSpeed(event.value)
                SettingsEvent.ToggleTouchpadNaturalScrolling -> toggleTouchpadNaturalScrolling()
                SettingsEvent.ToggleTouchpadTwoFingerScroll -> toggleTouchpadTwoFingerScroll()
                SettingsEvent.ToggleTouchpadEdgeScrolling -> toggleTouchpadEdgeScrolling()
                SettingsEvent.ToggleTouchpadScrollInertia -> toggleTouchpadScrollInertia()
                SettingsEvent.ToggleTouchpadTapToClick -> toggleTouchpadTapToClick()
                is SettingsEvent.UpdateTouchpadDoubleTapDelay -> updateTouchpadDoubleTapDelay(event.value)
                SettingsEvent.ToggleTouchpadThreeFingerSwipe -> toggleTouchpadThreeFingerSwipe()
                SettingsEvent.ToggleTouchpadPinchToZoom -> toggleTouchpadPinchToZoom()
                SettingsEvent.ToggleTouchpadRotateToRotate -> toggleTouchpadRotateToRotate()
                SettingsEvent.ToggleTouchpadHapticFeedback -> toggleTouchpadHapticFeedback()
                SettingsEvent.ToggleTouchpadShowTouchPoints -> toggleTouchpadShowTouchPoints()

                // ==================== CONNECTION EVENTS ====================
                SettingsEvent.ToggleAutoConnect -> toggleAutoConnect()
                is SettingsEvent.UpdateReconnectAttempts -> updateReconnectAttempts(event.value)
                is SettingsEvent.UpdateConnectionTimeout -> updateConnectionTimeout(event.value)
                SettingsEvent.ToggleUseWebSocket -> toggleUseWebSocket()
                SettingsEvent.ToggleUdpDiscovery -> toggleUdpDiscovery()

                // ==================== PRIVACY EVENTS ====================
                SettingsEvent.ToggleAnonymousStats -> toggleAnonymousStats()
                SettingsEvent.ToggleCrashReporting -> toggleCrashReporting()
                SettingsEvent.ToggleClearDataOnExit -> toggleClearDataOnExit()

                // ==================== PRESENTATION EVENTS ====================
                SettingsEvent.TogglePresentationMode -> togglePresentationMode()
                is SettingsEvent.UpdateLaserPointerSpeed -> updateLaserPointerSpeed(event.value)
                SettingsEvent.ToggleShowPresentationTimer -> toggleShowPresentationTimer()
                SettingsEvent.ToggleAutoHideLaser -> toggleAutoHideLaser()

                // ==================== ACTIONS ====================
                SettingsEvent.SaveSettings -> saveSettings()
                SettingsEvent.ResetDefaults -> resetDefaults()
                SettingsEvent.ExportSettings -> exportSettings()
                SettingsEvent.ImportSettings -> importSettings()
                SettingsEvent.ClearCache -> clearCache()
                SettingsEvent.ClearAllData -> clearAllData()
                SettingsEvent.OpenSystemSettings -> openSystemSettings()
                SettingsEvent.OpenAccessibilitySettings -> openAccessibilitySettings()
                SettingsEvent.OpenWebsite -> openUrl("https://airmouse.io")
                SettingsEvent.OpenPrivacyPolicy -> openUrl("https://airmouse.io/privacy")
                SettingsEvent.OpenLicense -> openUrl("https://airmouse.io/license")
                SettingsEvent.OpenGitHub -> openUrl("https://github.com/airmouse/airmouse-android")
                SettingsEvent.OpenSupport -> openUrl("mailto:support@airmouse.io")
                is SettingsEvent.ShowToast -> showToast(event.message)
                is SettingsEvent.ShowError -> _uiState.update { it.copy(error = event.message) }
            }
        }
    }

    // ======================================================
    // CURSOR METHODS - ALL SAVED
    // ======================================================

    private suspend fun updateSensitivity(value: Float) {
        prefs.putFloat("sensitivity", value)
        _uiState.update { it.copy(sensitivity = value) }
    }

    private suspend fun toggleAcceleration() {
        val current = _uiState.value.accelerationEnabled
        prefs.putBoolean("acceleration_enabled", !current)
        _uiState.update { it.copy(accelerationEnabled = !current) }
        showToast("Acceleration ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateAccelerationFactor(value: Float) {
        prefs.putFloat("acceleration_factor", value)
        _uiState.update { it.copy(accelerationFactor = value) }
    }

    private suspend fun toggleInvertX() {
        val current = _uiState.value.invertX
        prefs.putBoolean("invert_x", !current)
        _uiState.update { it.copy(invertX = !current) }
    }

    private suspend fun toggleInvertY() {
        val current = _uiState.value.invertY
        prefs.putBoolean("invert_y", !current)
        _uiState.update { it.copy(invertY = !current) }
    }

    private suspend fun toggleSmoothing() {
        val current = _uiState.value.smoothingEnabled
        prefs.putBoolean("smoothing_enabled", !current)
        _uiState.update { it.copy(smoothingEnabled = !current) }
    }

    private suspend fun updateSmoothingFactor(value: Float) {
        prefs.putFloat("smoothing_factor", value)
        _uiState.update { it.copy(smoothingFactor = value) }
    }

    // ======================================================
    // GESTURE METHODS - ALL SAVED
    // ======================================================

    private suspend fun updateClickThreshold(value: Float) {
        prefs.putFloat("click_threshold", value)
        _uiState.update { it.copy(clickThreshold = value) }
    }

    private suspend fun updateDoubleClickInterval(value: Long) {
        prefs.putLong("double_click_interval", value)
        _uiState.update { it.copy(doubleClickInterval = value) }
    }

    private suspend fun updateScrollThreshold(value: Float) {
        prefs.putFloat("scroll_threshold", value)
        _uiState.update { it.copy(scrollThreshold = value) }
    }

    private suspend fun updateRightClickTilt(value: Float) {
        prefs.putFloat("right_click_tilt", value)
        _uiState.update { it.copy(rightClickTilt = value) }
    }

    private suspend fun updateRightClickDuration(value: Long) {
        prefs.putLong("right_click_duration", value)
        _uiState.update { it.copy(rightClickDuration = value) }
    }

    // ======================================================
    // AI METHODS - ALL SAVED
    // ======================================================

    private suspend fun toggleAiSmoothing() {
        val current = _uiState.value.aiSmoothing
        prefs.putBoolean("ai_smoothing", !current)
        _uiState.update { it.copy(aiSmoothing = !current) }
        showToast("AI Smoothing ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateAiBlendFactor(value: Float) {
        prefs.putFloat("ai_blend_factor", value)
        _uiState.update { it.copy(aiBlendFactor = value) }
    }

    private suspend fun togglePredictive() {
        val current = _uiState.value.predictive
        prefs.putBoolean("predictive_movement", !current)
        _uiState.update { it.copy(predictive = !current) }
    }

    private suspend fun updatePredictionStrength(value: Float) {
        prefs.putFloat("prediction_strength", value)
        _uiState.update { it.copy(predictionStrength = value) }
    }

    private suspend fun toggleKalman() {
        val current = _uiState.value.kalmanEnabled
        prefs.putBoolean("kalman_enabled", !current)
        _uiState.update { it.copy(kalmanEnabled = !current) }
    }

    // ======================================================
    // HAPTIC METHODS - ALL SAVED
    // ======================================================

    private suspend fun toggleHaptic() {
        val current = _uiState.value.hapticEnabled
        prefs.putBoolean("haptic_enabled", !current)
        _uiState.update { it.copy(hapticEnabled = !current) }
        showToast("Haptic ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateHapticStrength(strength: HapticStrength) {
        prefs.putString("haptic_strength", strength.name)
        _uiState.update { it.copy(hapticStrength = strength) }
        showToast("Haptic strength: ${strength.displayName}")
    }

    private suspend fun toggleSound() {
        val current = _uiState.value.soundEnabled
        prefs.putBoolean("sound_enabled", !current)
        _uiState.update { it.copy(soundEnabled = !current) }
    }

    private suspend fun toggleVisualFeedback() {
        val current = _uiState.value.visualFeedback
        prefs.putBoolean("visual_feedback", !current)
        _uiState.update { it.copy(visualFeedback = !current) }
    }

    private suspend fun toggleNotificationOnGesture() {
        val current = _uiState.value.notificationOnGesture
        prefs.putBoolean("notification_on_gesture", !current)
        _uiState.update { it.copy(notificationOnGesture = !current) }
    }

    // ======================================================
    // DISPLAY METHODS - ALL SAVED
    // ======================================================

    private suspend fun updateTheme(theme: String) {
        val normalizedTheme = normalizeThemeId(theme)
        prefs.putString("theme", normalizedTheme)
        _uiState.update { it.copy(theme = normalizedTheme) }
        showToast("Theme: $normalizedTheme")
    }

    private suspend fun toggleDynamicColors() {
        val current = _uiState.value.useDynamicColors
        prefs.putBoolean("dynamic_colors", !current)
        _uiState.update { it.copy(useDynamicColors = !current) }
    }

    private suspend fun updateFontSize(value: Float) {
        prefs.putFloat("font_size", value)
        _uiState.update { it.copy(fontSize = value) }
    }

    private suspend fun toggleDebugInfo() {
        val current = _uiState.value.showDebugInfo
        prefs.putBoolean("show_debug_info", !current)
        _uiState.update { it.copy(showDebugInfo = !current) }
    }

    private suspend fun toggleKeepScreenOn() {
        val current = _uiState.value.keepScreenOn
        prefs.putBoolean("keep_screen_on", !current)
        _uiState.update { it.copy(keepScreenOn = !current) }
    }

    private suspend fun toggleShowFps() {
        val current = _uiState.value.showFps
        prefs.putBoolean("show_fps", !current)
        _uiState.update { it.copy(showFps = !current) }
    }

    // ======================================================
    // TOUCHPAD METHODS - ALL SAVED
    // ======================================================

    private suspend fun toggleTouchpadActive() {
        val current = _uiState.value.touchpadActive
        prefs.putBoolean("touchpad_active", !current)
        _uiState.update { it.copy(touchpadActive = !current) }
    }

    private suspend fun updateTouchpadSensitivity(value: Float) {
        prefs.putFloat("touchpad_sensitivity", value)
        _uiState.update { it.copy(touchpadSensitivity = value) }
    }

    private suspend fun updateTouchpadCursorSpeed(value: Float) {
        prefs.putFloat("touchpad_cursor_speed", value)
        _uiState.update { it.copy(touchpadCursorSpeed = value) }
    }

    private suspend fun updateTouchpadPointerSpeed(value: Int) {
        prefs.putInt("touchpad_pointer_speed", value)
        _uiState.update { it.copy(touchpadPointerSpeed = value) }
    }

    private suspend fun toggleTouchpadAcceleration() {
        val current = _uiState.value.touchpadAccelerationEnabled
        prefs.putBoolean("touchpad_acceleration", !current)
        _uiState.update { it.copy(touchpadAccelerationEnabled = !current) }
    }

    private suspend fun toggleTouchpadInvertVertical() {
        val current = _uiState.value.touchpadInvertVertical
        prefs.putBoolean("touchpad_invert_vertical", !current)
        _uiState.update { it.copy(touchpadInvertVertical = !current) }
    }

    private suspend fun toggleTouchpadInvertHorizontal() {
        val current = _uiState.value.touchpadInvertHorizontal
        prefs.putBoolean("touchpad_invert_horizontal", !current)
        _uiState.update { it.copy(touchpadInvertHorizontal = !current) }
    }

    private suspend fun updateTouchpadScrollSpeed(value: Float) {
        prefs.putFloat("touchpad_scroll_speed", value)
        _uiState.update { it.copy(touchpadScrollSpeed = value) }
    }

    private suspend fun toggleTouchpadNaturalScrolling() {
        val current = _uiState.value.touchpadNaturalScrolling
        prefs.putBoolean("touchpad_natural_scrolling", !current)
        _uiState.update { it.copy(touchpadNaturalScrolling = !current) }
    }

    private suspend fun toggleTouchpadTwoFingerScroll() {
        val current = _uiState.value.touchpadTwoFingerScroll
        prefs.putBoolean("touchpad_two_finger_scroll", !current)
        _uiState.update { it.copy(touchpadTwoFingerScroll = !current) }
    }

    private suspend fun toggleTouchpadEdgeScrolling() {
        val current = _uiState.value.touchpadEdgeScrolling
        prefs.putBoolean("touchpad_edge_scrolling", !current)
        _uiState.update { it.copy(touchpadEdgeScrolling = !current) }
    }

    private suspend fun toggleTouchpadScrollInertia() {
        val current = _uiState.value.touchpadScrollInertia
        prefs.putBoolean("touchpad_scroll_inertia", !current)
        _uiState.update { it.copy(touchpadScrollInertia = !current) }
    }

    private suspend fun toggleTouchpadTapToClick() {
        val current = _uiState.value.touchpadTapToClick
        prefs.putBoolean("touchpad_tap_to_click", !current)
        _uiState.update { it.copy(touchpadTapToClick = !current) }
    }

    private suspend fun updateTouchpadDoubleTapDelay(value: Int) {
        prefs.putInt("touchpad_double_tap_delay", value)
        _uiState.update { it.copy(touchpadDoubleTapDelay = value) }
    }

    private suspend fun toggleTouchpadThreeFingerSwipe() {
        val current = _uiState.value.touchpadThreeFingerSwipe
        prefs.putBoolean("touchpad_three_finger_swipe", !current)
        _uiState.update { it.copy(touchpadThreeFingerSwipe = !current) }
    }

    private suspend fun toggleTouchpadPinchToZoom() {
        val current = _uiState.value.touchpadPinchToZoom
        prefs.putBoolean("touchpad_pinch_to_zoom", !current)
        _uiState.update { it.copy(touchpadPinchToZoom = !current) }
    }

    private suspend fun toggleTouchpadRotateToRotate() {
        val current = _uiState.value.touchpadRotateToRotate
        prefs.putBoolean("touchpad_rotate_to_rotate", !current)
        _uiState.update { it.copy(touchpadRotateToRotate = !current) }
    }

    private suspend fun toggleTouchpadHapticFeedback() {
        val current = _uiState.value.touchpadHapticFeedback
        prefs.putBoolean("touchpad_haptic_feedback", !current)
        _uiState.update { it.copy(touchpadHapticFeedback = !current) }
    }

    private suspend fun toggleTouchpadShowTouchPoints() {
        val current = _uiState.value.touchpadShowTouchPoints
        prefs.putBoolean("touchpad_show_touch_points", !current)
        _uiState.update { it.copy(touchpadShowTouchPoints = !current) }
    }

    // ======================================================
    // CONNECTION METHODS - ALL SAVED
    // ======================================================

    private suspend fun toggleAutoConnect() {
        val current = _uiState.value.autoConnect
        prefs.putBoolean("auto_connect", !current)
        _uiState.update { it.copy(autoConnect = !current) }
    }

    private suspend fun updateReconnectAttempts(value: Int) {
        prefs.putInt("reconnect_attempts", value)
        _uiState.update { it.copy(reconnectAttempts = value) }
    }

    private suspend fun updateConnectionTimeout(value: Int) {
        prefs.putInt("connection_timeout", value)
        _uiState.update { it.copy(connectionTimeout = value) }
    }

    private suspend fun toggleUseWebSocket() {
        val current = _uiState.value.useWebSocket
        prefs.putBoolean("use_websocket", !current)
        _uiState.update { it.copy(useWebSocket = !current) }
    }

    private suspend fun toggleUdpDiscovery() {
        val current = _uiState.value.useUdpDiscovery
        prefs.putBoolean("use_udp_discovery", !current)
        _uiState.update { it.copy(useUdpDiscovery = !current) }
    }

    // ======================================================
    // PRIVACY METHODS - ALL SAVED
    // ======================================================

    private suspend fun toggleAnonymousStats() {
        val current = _uiState.value.anonymousStats
        prefs.putBoolean("anonymous_stats", !current)
        _uiState.update { it.copy(anonymousStats = !current) }
    }

    private suspend fun toggleCrashReporting() {
        val current = _uiState.value.crashReporting
        prefs.putBoolean("crash_reporting", !current)
        _uiState.update { it.copy(crashReporting = !current) }
    }

    private suspend fun toggleClearDataOnExit() {
        val current = _uiState.value.clearDataOnExit
        prefs.putBoolean("clear_data_on_exit", !current)
        _uiState.update { it.copy(clearDataOnExit = !current) }
    }

    // ======================================================
    // PRESENTATION METHODS - ALL SAVED
    // ======================================================

    private suspend fun togglePresentationMode() {
        val current = _uiState.value.presentationModeEnabled
        prefs.putBoolean("presentation_mode_enabled", !current)
        _uiState.update { it.copy(presentationModeEnabled = !current) }
        showToast("Presentation mode ${if (!current) "enabled" else "disabled"}")
    }

    private suspend fun updateLaserPointerSpeed(value: Float) {
        prefs.putFloat("laser_pointer_speed", value)
        _uiState.update { it.copy(laserPointerSpeed = value) }
    }

    private suspend fun toggleShowPresentationTimer() {
        val current = _uiState.value.showPresentationTimer
        prefs.putBoolean("show_presentation_timer", !current)
        _uiState.update { it.copy(showPresentationTimer = !current) }
    }

    private suspend fun toggleAutoHideLaser() {
        val current = _uiState.value.autoHideLaser
        prefs.putBoolean("auto_hide_laser", !current)
        _uiState.update { it.copy(autoHideLaser = !current) }
    }

    // ======================================================
    // ACTIONS
    // ======================================================

    private suspend fun saveSettings() {
        // All settings are saved individually in their respective methods
        showToast("All settings saved successfully!")
    }

    private suspend fun resetDefaults() {
        val defaultState = SettingsUiState()
        _uiState.update { defaultState }
        prefs.clear()
        // ==================== SAVE ALL DEFAULT VALUES ====================
        // Cursor
        prefs.putFloat("sensitivity", defaultState.sensitivity)
        prefs.putBoolean("acceleration_enabled", defaultState.accelerationEnabled)
        prefs.putFloat("acceleration_factor", defaultState.accelerationFactor)
        prefs.putBoolean("invert_x", defaultState.invertX)
        prefs.putBoolean("invert_y", defaultState.invertY)
        prefs.putBoolean("smoothing_enabled", defaultState.smoothingEnabled)
        prefs.putFloat("smoothing_factor", defaultState.smoothingFactor)
        // Gesture
        prefs.putFloat("click_threshold", defaultState.clickThreshold)
        prefs.putLong("double_click_interval", defaultState.doubleClickInterval)
        prefs.putFloat("scroll_threshold", defaultState.scrollThreshold)
        prefs.putFloat("right_click_tilt", defaultState.rightClickTilt)
        prefs.putLong("right_click_duration", defaultState.rightClickDuration)
        prefs.putLong("gesture_debounce", defaultState.gestureDebounce)
        // AI
        prefs.putBoolean("ai_smoothing", defaultState.aiSmoothing)
        prefs.putFloat("ai_blend_factor", defaultState.aiBlendFactor)
        prefs.putBoolean("predictive_movement", defaultState.predictive)
        prefs.putFloat("prediction_strength", defaultState.predictionStrength)
        prefs.putBoolean("kalman_enabled", defaultState.kalmanEnabled)
        // Haptic
        prefs.putBoolean("haptic_enabled", defaultState.hapticEnabled)
        prefs.putString("haptic_strength", defaultState.hapticStrength.name)
        prefs.putBoolean("sound_enabled", defaultState.soundEnabled)
        prefs.putBoolean("visual_feedback", defaultState.visualFeedback)
        prefs.putBoolean("notification_on_gesture", defaultState.notificationOnGesture)
        // Display
        prefs.putString("theme", defaultState.theme)
        prefs.putBoolean("dynamic_colors", defaultState.useDynamicColors)
        prefs.putFloat("font_size", defaultState.fontSize)
        prefs.putBoolean("show_debug_info", defaultState.showDebugInfo)
        prefs.putBoolean("keep_screen_on", defaultState.keepScreenOn)
        prefs.putBoolean("show_fps", defaultState.showFps)
        // Touchpad
        prefs.putBoolean("touchpad_active", defaultState.touchpadActive)
        prefs.putFloat("touchpad_sensitivity", defaultState.touchpadSensitivity)
        prefs.putFloat("touchpad_cursor_speed", defaultState.touchpadCursorSpeed)
        prefs.putInt("touchpad_pointer_speed", defaultState.touchpadPointerSpeed)
        prefs.putBoolean("touchpad_acceleration", defaultState.touchpadAccelerationEnabled)
        prefs.putBoolean("touchpad_invert_vertical", defaultState.touchpadInvertVertical)
        prefs.putBoolean("touchpad_invert_horizontal", defaultState.touchpadInvertHorizontal)
        prefs.putFloat("touchpad_scroll_speed", defaultState.touchpadScrollSpeed)
        prefs.putBoolean("touchpad_natural_scrolling", defaultState.touchpadNaturalScrolling)
        prefs.putBoolean("touchpad_two_finger_scroll", defaultState.touchpadTwoFingerScroll)
        prefs.putBoolean("touchpad_edge_scrolling", defaultState.touchpadEdgeScrolling)
        prefs.putBoolean("touchpad_scroll_inertia", defaultState.touchpadScrollInertia)
        prefs.putBoolean("touchpad_tap_to_click", defaultState.touchpadTapToClick)
        prefs.putInt("touchpad_double_tap_delay", defaultState.touchpadDoubleTapDelay)
        prefs.putBoolean("touchpad_three_finger_swipe", defaultState.touchpadThreeFingerSwipe)
        prefs.putBoolean("touchpad_pinch_to_zoom", defaultState.touchpadPinchToZoom)
        prefs.putBoolean("touchpad_rotate_to_rotate", defaultState.touchpadRotateToRotate)
        prefs.putBoolean("touchpad_haptic_feedback", defaultState.touchpadHapticFeedback)
        prefs.putBoolean("touchpad_show_touch_points", defaultState.touchpadShowTouchPoints)
        // Connection
        prefs.putBoolean("auto_connect", defaultState.autoConnect)
        prefs.putInt("reconnect_attempts", defaultState.reconnectAttempts)
        prefs.putInt("connection_timeout", defaultState.connectionTimeout)
        prefs.putBoolean("use_websocket", defaultState.useWebSocket)
        prefs.putBoolean("use_udp_discovery", defaultState.useUdpDiscovery)
        // Privacy
        prefs.putBoolean("anonymous_stats", defaultState.anonymousStats)
        prefs.putBoolean("crash_reporting", defaultState.crashReporting)
        prefs.putBoolean("clear_data_on_exit", defaultState.clearDataOnExit)
        // Presentation
        prefs.putBoolean("presentation_mode_enabled", defaultState.presentationModeEnabled)
        prefs.putFloat("laser_pointer_speed", defaultState.laserPointerSpeed)
        prefs.putBoolean("show_presentation_timer", defaultState.showPresentationTimer)
        prefs.putBoolean("auto_hide_laser", defaultState.autoHideLaser)
        showToast("All settings reset to defaults!")
    }

    private suspend fun exportSettings() {
        val state = _uiState.value
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "airmouse_settings_$timestamp.txt"
        val file = File(context.filesDir, fileName)

        val content = buildString {
            appendLine("=== Air Mouse Pro Settings Export ===")
            appendLine("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("=== Cursor Settings ===")
            appendLine("Sensitivity: ${state.sensitivity}")
            appendLine("Acceleration: ${state.accelerationEnabled}")
            appendLine("Acceleration Factor: ${state.accelerationFactor}")
            appendLine("Invert X: ${state.invertX}")
            appendLine("Invert Y: ${state.invertY}")
            appendLine("Smoothing: ${state.smoothingEnabled}")
            appendLine("Smoothing Factor: ${state.smoothingFactor}")
            appendLine()
            appendLine("=== Gesture Settings ===")
            appendLine("Click Threshold: ${state.clickThreshold}")
            appendLine("Double Click Interval: ${state.doubleClickInterval}")
            appendLine("Scroll Threshold: ${state.scrollThreshold}")
            appendLine("Right Click Tilt: ${state.rightClickTilt}")
            appendLine("Right Click Duration: ${state.rightClickDuration}")
            appendLine("Gesture Debounce: ${state.gestureDebounce}")
            appendLine()
            appendLine("=== AI Settings ===")
            appendLine("AI Smoothing: ${state.aiSmoothing}")
            appendLine("AI Blend Factor: ${state.aiBlendFactor}")
            appendLine("Predictive: ${state.predictive}")
            appendLine("Prediction Strength: ${state.predictionStrength}")
            appendLine("Kalman Filter: ${state.kalmanEnabled}")
            appendLine()
            appendLine("=== Haptic Settings ===")
            appendLine("Haptic Enabled: ${state.hapticEnabled}")
            appendLine("Haptic Strength: ${state.hapticStrength.displayName}")
            appendLine("Sound Enabled: ${state.soundEnabled}")
            appendLine("Visual Feedback: ${state.visualFeedback}")
            appendLine("Notification on Gesture: ${state.notificationOnGesture}")
            appendLine()
            appendLine("=== Display Settings ===")
            appendLine("Theme: ${state.theme}")
            appendLine("Dynamic Colors: ${state.useDynamicColors}")
            appendLine("Font Size: ${state.fontSize}")
            appendLine("Show Debug: ${state.showDebugInfo}")
            appendLine("Keep Screen On: ${state.keepScreenOn}")
            appendLine("Show FPS: ${state.showFps}")
            appendLine()
            appendLine("=== Touchpad Settings ===")
            appendLine("Touchpad Active: ${state.touchpadActive}")
            appendLine("Touchpad Sensitivity: ${state.touchpadSensitivity}")
            appendLine("Touchpad Cursor Speed: ${state.touchpadCursorSpeed}")
            appendLine("Touchpad Pointer Speed: ${state.touchpadPointerSpeed}")
            appendLine("Touchpad Acceleration: ${state.touchpadAccelerationEnabled}")
            appendLine("Touchpad Invert Vertical: ${state.touchpadInvertVertical}")
            appendLine("Touchpad Invert Horizontal: ${state.touchpadInvertHorizontal}")
            appendLine("Touchpad Scroll Speed: ${state.touchpadScrollSpeed}")
            appendLine("Touchpad Natural Scrolling: ${state.touchpadNaturalScrolling}")
            appendLine("Touchpad Two Finger Scroll: ${state.touchpadTwoFingerScroll}")
            appendLine("Touchpad Edge Scrolling: ${state.touchpadEdgeScrolling}")
            appendLine("Touchpad Scroll Inertia: ${state.touchpadScrollInertia}")
            appendLine("Touchpad Tap To Click: ${state.touchpadTapToClick}")
            appendLine("Touchpad Double Tap Delay: ${state.touchpadDoubleTapDelay}")
            appendLine("Touchpad Three Finger Swipe: ${state.touchpadThreeFingerSwipe}")
            appendLine("Touchpad Pinch To Zoom: ${state.touchpadPinchToZoom}")
            appendLine("Touchpad Rotate To Rotate: ${state.touchpadRotateToRotate}")
            appendLine("Touchpad Haptic Feedback: ${state.touchpadHapticFeedback}")
            appendLine("Touchpad Show Touch Points: ${state.touchpadShowTouchPoints}")
            appendLine()
            appendLine("=== Connection Settings ===")
            appendLine("Auto Connect: ${state.autoConnect}")
            appendLine("Reconnect Attempts: ${state.reconnectAttempts}")
            appendLine("Connection Timeout: ${state.connectionTimeout}")
            appendLine("Use WebSocket: ${state.useWebSocket}")
            appendLine("UDP Discovery: ${state.useUdpDiscovery}")
            appendLine()
            appendLine("=== Privacy Settings ===")
            appendLine("Anonymous Stats: ${state.anonymousStats}")
            appendLine("Crash Reporting: ${state.crashReporting}")
            appendLine("Clear Data on Exit: ${state.clearDataOnExit}")
            appendLine()
            appendLine("=== Presentation Settings ===")
            appendLine("Presentation Mode: ${state.presentationModeEnabled}")
            appendLine("Laser Pointer Speed: ${state.laserPointerSpeed}")
            appendLine("Show Timer: ${state.showPresentationTimer}")
            appendLine("Auto-Hide Laser: ${state.autoHideLaser}")
        }

        file.writeText(content)
        showToast("Settings exported to: $fileName")
    }

    private suspend fun importSettings() {
        showToast("Import settings - select a settings file")
    }

    private suspend fun clearCache() {
        val cacheDir = context.cacheDir
        cacheDir.deleteRecursively()
        showToast("Cache cleared!")
    }

    private suspend fun clearAllData() {
        prefs.clear()
        loadSettings()
        showToast("All data cleared!")
    }

    private suspend fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open system settings")
        }
    }

    private suspend fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open accessibility settings")
        }
    }

    private suspend fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Cannot open URL: ${e.message}")
        }
    }
}
