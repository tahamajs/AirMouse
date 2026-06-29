package com.airmouse.presentation.ui.touchpad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.usecase.TestConnectionUseCase
import com.airmouse.network.ConnectionManager
import com.airmouse.network.MessageTypes
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    // ============================================================
    // UI State
    // ============================================================

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<TouchpadEffect?>(null)
    val effect: StateFlow<TouchpadEffect?> = _effect.asStateFlow()

    // ============================================================
    // Gesture Tracking State
    // ============================================================

    // Single‑finger drag
    private var lastX = 0f
    private var lastY = 0f

    // Two‑finger scroll
    private var lastScrollX = 0f
    private var lastScrollY = 0f

    // Three‑finger swipe
    private var threeFingerLastX = 0f
    private var threeFingerLastY = 0f

    // Four‑finger swipe
    private var fourFingerLastX = 0f
    private var fourFingerLastY = 0f

    // Pinch / rotate
    private var lastPinchDistance = 0f
    private var lastRotation = 0f

    // Tap detection
    private var lastTapTime = 0L
    private var tapCount = 0

    // Inertia / timeout jobs
    private var inertiaJob: Job? = null
    private var gestureTimeoutJob: Job? = null
    private var isGestureActive = false

    // ============================================================
    // Initialisation
    // ============================================================

    init {
        loadSettings()
        observeConnectionState()
    }

    // ============================================================
    // Settings Loading & Saving
    // ============================================================

    private fun loadSettings() {
        val connectionConfig = ConnectionConfig(
            ip = prefs.getString("last_ip", ""),
            port = prefs.getInt("last_port", 0),
            autoReconnect = prefs.getBoolean("auto_connect", true),
            timeoutMs = prefs.getInt("connection_timeout", 5000).toLong()
        ).normalized()

        val mouseProfile = MovementProfile(
            sensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
            smoothingEnabled = prefs.getBoolean("touchpad_scroll_inertia", true),
            accelerationEnabled = prefs.getBoolean("touchpad_acceleration", true),
            invertX = prefs.getBoolean("touchpad_invert_horizontal", false),
            invertY = prefs.getBoolean("touchpad_invert_vertical", false),
            deadband = prefs.getFloat("deadband", 0.5f)
        )

        val appPreferences = AppPreferences(
            theme = prefs.getString("theme", "system"),
            language = prefs.getLanguage(),
            autoStart = prefs.getBoolean("auto_start_server", false),
            showTrayIcon = true,
            soundEnabled = prefs.isSoundEnabled(),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            analyticsEnabled = prefs.getBoolean("analytics_enabled", true),
            crashReportingEnabled = prefs.getBoolean("crash_reporting", true)
        )

        val userPreferences = UserPreferences(
            username = prefs.getUserName(),
            serverName = prefs.getString("server_name", "Air Mouse Pro"),
            serverIp = connectionConfig.ip,
            serverPort = connectionConfig.port,
            autoConnect = connectionConfig.autoReconnect,
            rememberCredentials = prefs.getBoolean("remember_credentials", true)
        )

        val mouseStatistics = MouseStatistics(
            totalClicks = prefs.getInt("stat_clicks", 0),
            totalDoubleClicks = prefs.getInt("stat_double_clicks", 0),
            totalRightClicks = prefs.getInt("stat_right_clicks", 0),
            totalScrolls = prefs.getInt("stat_scrolls", 0)
        )

        _uiState.update {
            it.copy(
                isActive = prefs.getBoolean("touchpad_active", false),
                sensitivity = mouseProfile.sensitivity,
                cursorSpeed = prefs.getFloat("touchpad_cursor_speed", 1.0f),
                pointerSpeed = prefs.getInt("touchpad_pointer_speed", 50),
                accelerationEnabled = mouseProfile.accelerationEnabled,
                invertVertical = mouseProfile.invertY,
                invertHorizontal = mouseProfile.invertX,
                scrollSpeed = prefs.getFloat("touchpad_scroll_speed", 1.0f),
                naturalScrolling = prefs.getBoolean("touchpad_natural_scrolling", true),
                twoFingerScroll = prefs.getBoolean("touchpad_two_finger_scroll", true),
                edgeScrolling = prefs.getBoolean("touchpad_edge_scrolling", false),
                scrollInertia = prefs.getBoolean("touchpad_scroll_inertia", true),
                tapToClick = prefs.getBoolean("touchpad_tap_to_click", true),
                doubleTapDelay = prefs.getInt("touchpad_double_tap_delay", 300),
                threeFingerSwipe = prefs.getBoolean("touchpad_three_finger_swipe", true),
                pinchToZoom = prefs.getBoolean("touchpad_pinch_to_zoom", true),
                rotateToRotate = prefs.getBoolean("touchpad_rotate_to_rotate", false),
                hapticFeedback = prefs.getBoolean("touchpad_haptic_feedback", true),
                showTouchPoints = prefs.getBoolean("touchpad_show_touch_points", false),
                connectionConfig = connectionConfig,
                mouseProfile = mouseProfile,
                appPreferences = appPreferences,
                userPreferences = userPreferences,
                mouseStatistics = mouseStatistics
            )
        }
    }

    private fun saveSetting(key: String, value: Any) {
        when (value) {
            is Boolean -> prefs.putBoolean(key, value)
            is Float -> prefs.putFloat(key, value)
            is Int -> prefs.putInt(key, value)
            is String -> prefs.putString(key, value)
        }
    }

    private fun saveAllSettings(state: TouchpadUiState) {
        saveSetting("touchpad_sensitivity", state.sensitivity)
        saveSetting("touchpad_cursor_speed", state.cursorSpeed)
        saveSetting("touchpad_pointer_speed", state.pointerSpeed)
        saveSetting("touchpad_acceleration", state.accelerationEnabled)
        saveSetting("touchpad_invert_vertical", state.invertVertical)
        saveSetting("touchpad_invert_horizontal", state.invertHorizontal)
        saveSetting("touchpad_scroll_speed", state.scrollSpeed)
        saveSetting("touchpad_natural_scrolling", state.naturalScrolling)
        saveSetting("touchpad_two_finger_scroll", state.twoFingerScroll)
        saveSetting("touchpad_edge_scrolling", state.edgeScrolling)
        saveSetting("touchpad_scroll_inertia", state.scrollInertia)
        saveSetting("touchpad_tap_to_click", state.tapToClick)
        saveSetting("touchpad_double_tap_delay", state.doubleTapDelay)
        saveSetting("touchpad_three_finger_swipe", state.threeFingerSwipe)
        saveSetting("touchpad_pinch_to_zoom", state.pinchToZoom)
        saveSetting("touchpad_rotate_to_rotate", state.rotateToRotate)
        saveSetting("touchpad_haptic_feedback", state.hapticFeedback)
        saveSetting("touchpad_show_touch_points", state.showTouchPoints)
        saveSetting("touchpad_active", state.isActive)
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                val connected = status == ConnectionManager.ConnectionStatus.CONNECTED
                _uiState.update {
                    it.copy(
                        isConnected = connected,
                        isActive = if (connected) it.isActive else false
                    )
                }

                if (!connected) {
                    resetGestureState()
                }
            }
        }
        viewModelScope.launch {
            connectionManager.isMouseControlEnabledFlow.collect { enabled ->
                _uiState.update {
                    it.copy(isMouseControlEnabled = enabled)
                }
            }
        }
    }

    // ============================================================
    // Event Handling
    // ============================================================

    fun handleEvent(event: TouchpadEvent) {
        when (event) {
            is TouchpadEvent.ToggleTouchpad -> toggleTouchpad()
            is TouchpadEvent.ResetToDefaults -> resetToDefaults()
            is TouchpadEvent.ApplyPreset -> applyPreset(event.mode)

            is TouchpadEvent.TouchEvent -> processTouchEvent(
                event.x, event.y, event.pointerCount,
                event.pointers, event.pressure
            )
            is TouchpadEvent.TapEvent -> processTap(event.x, event.y)
            is TouchpadEvent.LongPressEvent -> processLongPress()
            is TouchpadEvent.GestureEnd -> resetGestureState()

            // Settings updates
            is TouchpadEvent.UpdateSensitivity -> updateSensitivity(event.value)
            is TouchpadEvent.UpdateCursorSpeed -> updateCursorSpeed(event.value)
            is TouchpadEvent.UpdatePointerSpeed -> updatePointerSpeed(event.value)
            TouchpadEvent.ToggleAcceleration -> toggleAcceleration()
            TouchpadEvent.ToggleInvertVertical -> toggleInvertVertical()
            TouchpadEvent.ToggleInvertHorizontal -> toggleInvertHorizontal()

            is TouchpadEvent.UpdateScrollSpeed -> updateScrollSpeed(event.value)
            TouchpadEvent.ToggleNaturalScrolling -> toggleNaturalScrolling()
            TouchpadEvent.ToggleTwoFingerScroll -> toggleTwoFingerScroll()
            TouchpadEvent.ToggleEdgeScrolling -> toggleEdgeScrolling()
            TouchpadEvent.ToggleScrollInertia -> toggleScrollInertia()

            TouchpadEvent.ToggleTapToClick -> toggleTapToClick()
            is TouchpadEvent.UpdateDoubleTapDelay -> updateDoubleTapDelay(event.value)
            TouchpadEvent.ToggleThreeFingerSwipe -> toggleThreeFingerSwipe()
            TouchpadEvent.TogglePinchToZoom -> togglePinchToZoom()
            TouchpadEvent.ToggleRotateToRotate -> toggleRotateToRotate()

            TouchpadEvent.ToggleHapticFeedback -> toggleHapticFeedback()
            TouchpadEvent.ToggleShowTouchPoints -> toggleShowTouchPoints()

            TouchpadEvent.NavigateBack -> navigateBack()
            TouchpadEvent.NavigateToSettings -> navigateToSettings()
            is TouchpadEvent.ToggleMouseControl -> {
                connectionManager.isMouseControlEnabled = event.enabled
            }
        }
    }

    // ============================================================
    // Core Touchpad Actions
    // ============================================================

    private fun toggleTouchpad() {
        if (!connectionManager.isConnected()) {
            _uiState.update { it.copy(isActive = false) }
            triggerEffect(TouchpadEffect.ShowToast("Connect to the server first"))
            return
        }
        val newState = !_uiState.value.isActive
        saveSetting("touchpad_active", newState)
        _uiState.update { it.copy(isActive = newState) }

        sendCommand(if (newState) "start" else "stop")
        triggerEffect(TouchpadEffect.SendCommand(if (newState) "touchpad_start" else "touchpad_stop"))

        if (newState) {
            vibrate(50)
            triggerEffect(TouchpadEffect.ShowToast("Touchpad activated"))
        } else {
            resetGestureState()
            triggerEffect(TouchpadEffect.ShowToast("Touchpad deactivated"))
        }
    }

    private fun resetToDefaults() {
        val defaultState = TouchpadUiState()
        _uiState.update { defaultState }
        saveAllSettings(defaultState)
        triggerEffect(TouchpadEffect.ShowToast("Reset to defaults"))
    }

    private fun applyPreset(mode: TouchpadMode) {
        val preset = TouchpadPresets.getPreset(mode)

        updateSensitivity(preset.sensitivity)
        updateCursorSpeed(preset.cursorSpeed)
        updateScrollSpeed(preset.scrollSpeed)
        updateAcceleration(preset.accelerationEnabled)
        updateTapToClick(preset.tapToClick)
        updateTwoFingerScroll(preset.twoFingerScroll)
        updateThreeFingerSwipe(preset.threeFingerSwipe)
        updateNaturalScrolling(preset.naturalScrolling)

        triggerEffect(TouchpadEffect.ShowToast("Applied: ${mode.displayName}"))
    }

    // ============================================================
    // Touch Event Processing
    // ============================================================

    fun processTouchEvent(
        x: Float,
        y: Float,
        pointerCount: Int,
        pointers: List<Pair<Float, Float>>,
        pressure: Float
    ) {
        if (!_uiState.value.isActive) return

        // Update UI state with touch points
        _uiState.update {
            it.copy(
                currentX = x,
                currentY = y,
                touchPoints = pointers.mapIndexed { i, p ->
                    TouchPoint(i, p.first, p.second, pressure)
                }
            )
        }

        when (pointerCount) {
            1 -> handleSingleFingerMove(x, y)
            2 -> handleTwoFingerGesture(pointers)
            3 -> if (_uiState.value.threeFingerSwipe) handleThreeFingerGesture(pointers)
            4 -> handleFourFingerGesture(pointers)
        }
    }

    // ------------------------------------------------------------
    // 1‑Finger: Drag
    // ------------------------------------------------------------

    private fun handleSingleFingerMove(x: Float, y: Float) {
        if (lastX == 0f && lastY == 0f) {
            lastX = x
            lastY = y
            return
        }

        var dx = (x - lastX) * _uiState.value.sensitivity * _uiState.value.cursorSpeed
        var dy = (y - lastY) * _uiState.value.sensitivity * _uiState.value.cursorSpeed

        // Acceleration
        if (_uiState.value.accelerationEnabled) {
            val speed = sqrt(dx * dx + dy * dy)
            val factor = 1 + (speed / 50f).coerceIn(0f, 2f)
            dx *= factor
            dy *= factor
        }

        // Inversion
        if (_uiState.value.invertHorizontal) dx = -dx
        if (_uiState.value.invertVertical) dy = -dy

        sendMove(dx, dy)

        _uiState.update {
            it.copy(
                isDragging = true,
                lastGesture = TouchpadGesture.DRAG.displayName
            )
        }

        lastX = x
        lastY = y
    }

    // ------------------------------------------------------------
    // 2‑Finger: Scroll
    // ------------------------------------------------------------

    private fun handleTwoFingerGesture(pointers: List<Pair<Float, Float>>) {
        val centerX = (pointers[0].first + pointers[1].first) / 2f
        val centerY = (pointers[0].second + pointers[1].second) / 2f
        val distance = hypot(
            pointers[1].first - pointers[0].first,
            pointers[1].second - pointers[0].second
        )

        if (_uiState.value.pinchToZoom && lastPinchDistance != 0f) {
            val pinchDelta = distance - lastPinchDistance
            if (abs(pinchDelta) > 20f) {
                val command = if (pinchDelta > 0) {
                    MessageTypes.COMMAND_ZOOM_IN
                } else {
                    MessageTypes.COMMAND_ZOOM_OUT
                }
                sendCommand(command)
                vibrate(15)
                _uiState.update {
                    it.copy(
                        lastGesture = if (pinchDelta > 0) TouchpadGesture.PINCH_OUT.displayName else TouchpadGesture.PINCH_IN.displayName,
                        gestureHistory = (it.gestureHistory + if (pinchDelta > 0) {
                            TouchpadGesture.PINCH_OUT.displayName
                        } else {
                            TouchpadGesture.PINCH_IN.displayName
                        }).takeLast(10)
                    )
                }
                lastPinchDistance = 0f
                return
            }
        }

        if (_uiState.value.twoFingerScroll && (lastScrollX != 0f || lastScrollY != 0f)) {
            var dy = (centerY - lastScrollY) * _uiState.value.scrollSpeed

            if (_uiState.value.naturalScrolling) {
                dy = -dy
            }

            // Only scroll if movement is significant
            if (abs(dy) > 3) {
                val scrollDelta = (dy / 10f).toInt()
                if (scrollDelta != 0) {
                    sendScroll(scrollDelta)
                    _uiState.update {
                        it.copy(
                            isScrolling = true,
                            lastGesture = TouchpadGesture.TWO_FINGER_SCROLL.displayName
                        )
                    }
                }
            }
        }

        lastScrollX = centerX
        lastScrollY = centerY
        lastPinchDistance = distance
    }

    // ------------------------------------------------------------
    // 3‑Finger: Swipe (uses dedicated tracking vars)
    // ------------------------------------------------------------

    private fun handleThreeFingerGesture(pointers: List<Pair<Float, Float>>) {
        val centerX = pointers.map { it.first }.sum() / 3f
        val centerY = pointers.map { it.second }.sum() / 3f

        if (threeFingerLastX != 0f || threeFingerLastY != 0f) {
            val dx = centerX - threeFingerLastX
            val dy = centerY - threeFingerLastY

            if (abs(dx) > 50 || abs(dy) > 50) {
                val (gesture, command) = when {
                    abs(dx) > abs(dy) && dx > 0 -> TouchpadGesture.THREE_FINGER_SWIPE_RIGHT to MessageTypes.COMMAND_SWITCH_WINDOW
                    abs(dx) > abs(dy) && dx < 0 -> TouchpadGesture.THREE_FINGER_SWIPE_LEFT to MessageTypes.COMMAND_SWITCH_WINDOW
                    dy > 0 -> TouchpadGesture.THREE_FINGER_SWIPE_DOWN to MessageTypes.COMMAND_SHOW_DESKTOP
                    else -> TouchpadGesture.THREE_FINGER_SWIPE_UP to MessageTypes.COMMAND_TASK_VIEW
                }

                sendCommand(command)
                vibrate(20)
                pushGestureHistory(gesture.displayName)

                // Reset to prevent repeated triggers
                threeFingerLastX = 0f
                threeFingerLastY = 0f
                return
            }
        }

        threeFingerLastX = centerX
        threeFingerLastY = centerY
    }

    // ------------------------------------------------------------
    // 4‑Finger: Volume control (uses dedicated tracking var)
    // ------------------------------------------------------------

    private fun handleFourFingerGesture(pointers: List<Pair<Float, Float>>) {
        val centerX = pointers.map { it.first }.sum() / 4f
        val centerY = pointers.map { it.second }.sum() / 4f

        if (fourFingerLastX != 0f || fourFingerLastY != 0f) {
            val dx = centerX - fourFingerLastX
            val dy = centerY - fourFingerLastY
            if (abs(dx) > 35 || abs(dy) > 35) {
                val (gesture, command) = when {
                    abs(dx) > abs(dy) && dx > 0 -> TouchpadGesture.FOUR_FINGER_SWIPE_RIGHT to MessageTypes.COMMAND_SWITCH_WINDOW
                    abs(dx) > abs(dy) && dx < 0 -> TouchpadGesture.FOUR_FINGER_SWIPE_LEFT to MessageTypes.COMMAND_SWITCH_WINDOW
                    dy > 0 -> TouchpadGesture.FOUR_FINGER_SWIPE_DOWN to MessageTypes.COMMAND_SHOW_DESKTOP
                    else -> TouchpadGesture.FOUR_FINGER_SWIPE_UP to MessageTypes.COMMAND_TASK_VIEW
                }
                sendCommand(command)
                vibrate(25)
                pushGestureHistory(gesture.displayName)
                fourFingerLastX = 0f
                fourFingerLastY = 0f
                return
            }
        }
        fourFingerLastX = centerX
        fourFingerLastY = centerY
    }

    // ------------------------------------------------------------
    // Tap & Long‑Press
    // ------------------------------------------------------------

    @Suppress("UNUSED_PARAMETER")
    fun processTap(x: Float, y: Float) {
        if (!_uiState.value.isActive || !_uiState.value.tapToClick) return

        val now = System.currentTimeMillis()
        val tapDelay = _uiState.value.doubleTapDelay.toLong()

        if (now - lastTapTime < tapDelay) {
            tapCount++
            if (tapCount >= 2) {
                // Double tap
                sendClick("left")
                tapCount = 0
                pushGestureHistory(TouchpadGesture.DOUBLE_TAP.displayName)
                vibrate(40)
            }
        } else {
            tapCount = 1
            // Single tap
            sendClick("left")
            pushGestureHistory(TouchpadGesture.TAP.displayName)
            vibrate(20)
        }
        lastTapTime = now
    }

    fun processLongPress() {
        if (!_uiState.value.isActive) return

        sendClick("right")
        pushGestureHistory(TouchpadGesture.LONG_PRESS.displayName)
        vibrate(50)
    }

    fun resetGestureState() {
        // Reset all tracking variables
        lastX = 0f
        lastY = 0f
        lastScrollX = 0f
        lastScrollY = 0f
        threeFingerLastX = 0f
        threeFingerLastY = 0f
        lastPinchDistance = 0f
        lastRotation = 0f
        fourFingerLastX = 0f
        fourFingerLastY = 0f
        isGestureActive = false

        _uiState.update {
            it.copy(
                isDragging = false,
                isScrolling = false
            )
        }
    }

    // ============================================================
    // Settings Updaters (with persistence)
    // ============================================================

    private fun updateSensitivity(value: Float) {
        saveSetting("touchpad_sensitivity", value)
        _uiState.update { it.copy(sensitivity = value) }
    }

    private fun updateCursorSpeed(value: Float) {
        saveSetting("touchpad_cursor_speed", value)
        _uiState.update { it.copy(cursorSpeed = value) }
    }

    private fun updatePointerSpeed(value: Int) {
        saveSetting("touchpad_pointer_speed", value)
        _uiState.update { it.copy(pointerSpeed = value) }
    }

    private fun toggleAcceleration() {
        val newValue = !_uiState.value.accelerationEnabled
        saveSetting("touchpad_acceleration", newValue)
        _uiState.update { it.copy(accelerationEnabled = newValue) }
        triggerEffect(TouchpadEffect.ShowToast("Acceleration ${if (newValue) "enabled" else "disabled"}"))
    }

    private fun updateAcceleration(enabled: Boolean) {
        if (_uiState.value.accelerationEnabled != enabled) {
            toggleAcceleration()
        }
    }

    private fun toggleInvertVertical() {
        val newValue = !_uiState.value.invertVertical
        saveSetting("touchpad_invert_vertical", newValue)
        _uiState.update { it.copy(invertVertical = newValue) }
    }

    private fun toggleInvertHorizontal() {
        val newValue = !_uiState.value.invertHorizontal
        saveSetting("touchpad_invert_horizontal", newValue)
        _uiState.update { it.copy(invertHorizontal = newValue) }
    }

    private fun updateScrollSpeed(value: Float) {
        saveSetting("touchpad_scroll_speed", value)
        _uiState.update { it.copy(scrollSpeed = value) }
    }

    private fun toggleNaturalScrolling() {
        val newValue = !_uiState.value.naturalScrolling
        saveSetting("touchpad_natural_scrolling", newValue)
        _uiState.update { it.copy(naturalScrolling = newValue) }
    }

    private fun updateNaturalScrolling(enabled: Boolean) {
        if (_uiState.value.naturalScrolling != enabled) {
            toggleNaturalScrolling()
        }
    }

    private fun toggleTwoFingerScroll() {
        val newValue = !_uiState.value.twoFingerScroll
        saveSetting("touchpad_two_finger_scroll", newValue)
        _uiState.update { it.copy(twoFingerScroll = newValue) }
    }

    private fun updateTwoFingerScroll(enabled: Boolean) {
        if (_uiState.value.twoFingerScroll != enabled) {
            toggleTwoFingerScroll()
        }
    }

    private fun toggleEdgeScrolling() {
        val newValue = !_uiState.value.edgeScrolling
        saveSetting("touchpad_edge_scrolling", newValue)
        _uiState.update { it.copy(edgeScrolling = newValue) }
    }

    private fun toggleScrollInertia() {
        val newValue = !_uiState.value.scrollInertia
        saveSetting("touchpad_scroll_inertia", newValue)
        _uiState.update { it.copy(scrollInertia = newValue) }
    }

    private fun toggleTapToClick() {
        val newValue = !_uiState.value.tapToClick
        saveSetting("touchpad_tap_to_click", newValue)
        _uiState.update { it.copy(tapToClick = newValue) }
    }

    private fun updateTapToClick(enabled: Boolean) {
        if (_uiState.value.tapToClick != enabled) {
            toggleTapToClick()
        }
    }

    private fun updateDoubleTapDelay(value: Int) {
        saveSetting("touchpad_double_tap_delay", value)
        _uiState.update { it.copy(doubleTapDelay = value) }
    }

    private fun toggleThreeFingerSwipe() {
        val newValue = !_uiState.value.threeFingerSwipe
        saveSetting("touchpad_three_finger_swipe", newValue)
        _uiState.update { it.copy(threeFingerSwipe = newValue) }
    }

    private fun updateThreeFingerSwipe(enabled: Boolean) {
        if (_uiState.value.threeFingerSwipe != enabled) {
            toggleThreeFingerSwipe()
        }
    }

    private fun togglePinchToZoom() {
        val newValue = !_uiState.value.pinchToZoom
        saveSetting("touchpad_pinch_to_zoom", newValue)
        _uiState.update { it.copy(pinchToZoom = newValue) }
    }

    private fun toggleRotateToRotate() {
        val newValue = !_uiState.value.rotateToRotate
        saveSetting("touchpad_rotate_to_rotate", newValue)
        _uiState.update { it.copy(rotateToRotate = newValue) }
    }

    private fun toggleHapticFeedback() {
        val newValue = !_uiState.value.hapticFeedback
        saveSetting("touchpad_haptic_feedback", newValue)
        _uiState.update { it.copy(hapticFeedback = newValue) }
    }

    private fun toggleShowTouchPoints() {
        val newValue = !_uiState.value.showTouchPoints
        saveSetting("touchpad_show_touch_points", newValue)
        _uiState.update { it.copy(showTouchPoints = newValue) }
    }

    // ============================================================
    // Network Sending Helpers
    // ============================================================

    private fun sendMove(dx: Float, dy: Float) {
        if (!canSend()) return
        triggerEffect(TouchpadEffect.SendMove(dx, dy))
        connectionManager.sendMove(dx, dy)
    }

    private fun sendScroll(delta: Int) {
        if (!canSend()) return
        triggerEffect(TouchpadEffect.SendScroll(delta))
        connectionManager.sendScroll(delta)
    }

    private fun sendClick(button: String) {
        if (!canSend()) return
        triggerEffect(TouchpadEffect.SendClick(button))
        connectionManager.sendClick(button)
    }

    private fun sendGesture(gesture: String) {
        if (!canSend()) return
        triggerEffect(TouchpadEffect.SendGesture(gesture))
        connectionManager.sendGesture(gesture, 0.9f)
    }

    private fun sendCommand(command: String) {
        if (!canSend()) return
        triggerEffect(TouchpadEffect.SendCommand(command))
        connectionManager.sendControl(command)
    }

    private fun canSend(): Boolean {
        return _uiState.value.isConnected && connectionManager.isConnected() && _uiState.value.isActive
    }

    private fun pushGestureHistory(gesture: String) {
        _uiState.update {
            it.copy(
                lastGesture = gesture,
                gestureHistory = (it.gestureHistory + gesture).takeLast(10)
            )
        }
    }

    // ============================================================
    // Navigation
    // ============================================================

    private fun navigateBack() {
        triggerEffect(TouchpadEffect.NavigateBack)
    }

    private fun navigateToSettings() {
        triggerEffect(TouchpadEffect.NavigateToSettings)
    }

    fun testConnection() {
        viewModelScope.launch {
            val current = _uiState.value.connectionConfig
            _uiState.update { it.copy(isTestingConnection = true, connectionTestMessage = "Testing connection...") }
            val targetIp = current.ip.ifBlank { prefs.getString("last_ip", "") }
            val targetPort = current.port.takeIf { it > 0 } ?: prefs.getInt("last_port", 8080)
            val result = testConnectionUseCase(targetIp, targetPort)
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionTestResult = result.getOrNull(),
                    connectionTestMessage = result.fold(
                        onSuccess = { test ->
                            "${test.message} (${test.latency} ms)"
                        },
                        onFailure = { err ->
                            err.message ?: "Connection test failed"
                        }
                    )
                )
            }
        }
    }

    // ============================================================
    // Haptic Feedback
    // ============================================================

    private fun vibrate(duration: Long) {
        if (!_uiState.value.hapticFeedback) return
        triggerEffect(TouchpadEffect.Vibrate(duration))

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ContextCompat.getSystemService(context, Vibrator::class.java)
            } ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: SecurityException) {
            // Permission not granted – ignore
        } catch (_: Exception) {
            // Other vibration errors – ignore
        }
    }

    // ============================================================
    // Effects
    // ============================================================

    private fun triggerEffect(effect: TouchpadEffect) {
        _effect.value = effect
    }

    fun clearEffect() {
        _effect.value = null
    }

    // ============================================================
    // Lifecycle
    // ============================================================

    override fun onCleared() {
        super.onCleared()
        inertiaJob?.cancel()
        gestureTimeoutJob?.cancel()
    }
}
