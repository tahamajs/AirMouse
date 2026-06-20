// app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadViewModel.kt
package com.airmouse.presentation.ui.touchpad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<TouchpadEffect?>(null)
    val effect: StateFlow<TouchpadEffect?> = _effect.asStateFlow()

    // Touch tracking
    private var lastX = 0f
    private var lastY = 0f
    private var lastScrollX = 0f
    private var lastScrollY = 0f
    private var lastPinchDistance = 0f
    private var lastRotation = 0f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var inertiaJob: Job? = null
    private var gestureTimeoutJob: Job? = null
    private var isGestureActive = false

    init {
        loadSettings()
    }

    // ==========================================
    // SETTINGS LOADING
    // ==========================================

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                // Core
                isActive = prefs.getBoolean("touchpad_active", false),

                // Cursor
                sensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
                cursorSpeed = prefs.getFloat("touchpad_cursor_speed", 1.0f),
                pointerSpeed = prefs.getInt("touchpad_pointer_speed", 50),
                accelerationEnabled = prefs.getBoolean("touchpad_acceleration", true),
                invertVertical = prefs.getBoolean("touchpad_invert_vertical", false),
                invertHorizontal = prefs.getBoolean("touchpad_invert_horizontal", false),

                // Scroll
                scrollSpeed = prefs.getFloat("touchpad_scroll_speed", 1.0f),
                naturalScrolling = prefs.getBoolean("touchpad_natural_scrolling", true),
                twoFingerScroll = prefs.getBoolean("touchpad_two_finger_scroll", true),
                edgeScrolling = prefs.getBoolean("touchpad_edge_scrolling", false),
                scrollInertia = prefs.getBoolean("touchpad_scroll_inertia", true),

                // Gesture
                tapToClick = prefs.getBoolean("touchpad_tap_to_click", true),
                doubleTapDelay = prefs.getInt("touchpad_double_tap_delay", 300),
                threeFingerSwipe = prefs.getBoolean("touchpad_three_finger_swipe", true),
                pinchToZoom = prefs.getBoolean("touchpad_pinch_to_zoom", true),
                rotateToRotate = prefs.getBoolean("touchpad_rotate_to_rotate", false),

                // Feedback
                hapticFeedback = prefs.getBoolean("touchpad_haptic_feedback", true),
                showTouchPoints = prefs.getBoolean("touchpad_show_touch_points", false)
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

    // ==========================================
    // EVENT HANDLING
    // ==========================================

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

            // Cursor
            is TouchpadEvent.UpdateSensitivity -> updateSensitivity(event.value)
            is TouchpadEvent.UpdateCursorSpeed -> updateCursorSpeed(event.value)
            is TouchpadEvent.UpdatePointerSpeed -> updatePointerSpeed(event.value)
            TouchpadEvent.ToggleAcceleration -> toggleAcceleration()
            TouchpadEvent.ToggleInvertVertical -> toggleInvertVertical()
            TouchpadEvent.ToggleInvertHorizontal -> toggleInvertHorizontal()

            // Scroll
            is TouchpadEvent.UpdateScrollSpeed -> updateScrollSpeed(event.value)
            TouchpadEvent.ToggleNaturalScrolling -> toggleNaturalScrolling()
            TouchpadEvent.ToggleTwoFingerScroll -> toggleTwoFingerScroll()
            TouchpadEvent.ToggleEdgeScrolling -> toggleEdgeScrolling()
            TouchpadEvent.ToggleScrollInertia -> toggleScrollInertia()

            // Gesture
            TouchpadEvent.ToggleTapToClick -> toggleTapToClick()
            is TouchpadEvent.UpdateDoubleTapDelay -> updateDoubleTapDelay(event.value)
            TouchpadEvent.ToggleThreeFingerSwipe -> toggleThreeFingerSwipe()
            TouchpadEvent.TogglePinchToZoom -> togglePinchToZoom()
            TouchpadEvent.ToggleRotateToRotate -> toggleRotateToRotate()

            // Feedback
            TouchpadEvent.ToggleHapticFeedback -> toggleHapticFeedback()
            TouchpadEvent.ToggleShowTouchPoints -> toggleShowTouchPoints()

            // Navigation
            TouchpadEvent.NavigateBack -> navigateBack()
            TouchpadEvent.NavigateToSettings -> navigateToSettings()
        }
    }

    // ==========================================
    // CORE FUNCTIONS
    // ==========================================

    private fun toggleTouchpad() {
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

        // Clear all touchpad preferences
        prefs.clear()

        // Save defaults
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

    // ==========================================
    // TOUCH PROCESSING
    // ==========================================

    fun processTouchEvent(x: Float, y: Float, count: Int, pointers: List<Pair<Float, Float>>, pressure: Float) {
        if (!_uiState.value.isActive) return

        _uiState.update {
            it.copy(
                currentX = x,
                currentY = y,
                touchPoints = pointers.mapIndexed { i, p ->
                    TouchPoint(i, p.first, p.second, pressure)
                }
            )
        }

        when (count) {
            1 -> handleSingleFingerMove(x, y)
            2 -> handleTwoFingerGesture(pointers)
            3 -> if (_uiState.value.threeFingerSwipe) handleThreeFingerGesture(pointers)
            4 -> handleFourFingerGesture(pointers)
        }
    }

    private fun handleSingleFingerMove(x: Float, y: Float) {
        if (lastX == 0f && lastY == 0f) {
            lastX = x
            lastY = y
            return
        }

        var dx = (x - lastX) * _uiState.value.sensitivity * _uiState.value.cursorSpeed
        var dy = (y - lastY) * _uiState.value.sensitivity * _uiState.value.cursorSpeed

        // Apply acceleration
        if (_uiState.value.accelerationEnabled) {
            val speed = sqrt(dx * dx + dy * dy)
            val factor = 1 + (speed / 50f).coerceIn(0f, 2f)
            dx *= factor
            dy *= factor
        }

        // Apply inversion
        if (_uiState.value.invertHorizontal) dx = -dx
        if (_uiState.value.invertVertical) dy = -dy

        // Send movement
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

    private fun handleTwoFingerGesture(pointers: List<Pair<Float, Float>>) {
        if (!_uiState.value.twoFingerScroll) return

        val centerX = (pointers[0].first + pointers[1].first) / 2f
        val centerY = (pointers[0].second + pointers[1].second) / 2f

        if (lastScrollX != 0f || lastScrollY != 0f) {
            var dx = (centerX - lastScrollX) * _uiState.value.scrollSpeed
            var dy = (centerY - lastScrollY) * _uiState.value.scrollSpeed

            if (_uiState.value.naturalScrolling) {
                dx = -dx
                dy = -dy
            }

            // Scroll threshold
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
    }

    private fun handleThreeFingerGesture(pointers: List<Pair<Float, Float>>) {
        // Calculate center and movement
        val centerX = pointers.sumOf { it.first } / 3f
        val centerY = pointers.sumOf { it.second } / 3f

        // Simplified swipe detection
        if (lastX != 0f || lastY != 0f) {
            val dx = centerX - lastX
            val dy = centerY - lastY

            if (abs(dx) > 50 || abs(dy) > 50) {
                val gesture = when {
                    abs(dx) > abs(dy) && dx > 0 -> TouchpadGesture.THREE_FINGER_SWIPE_RIGHT
                    abs(dx) > abs(dy) && dx < 0 -> TouchpadGesture.THREE_FINGER_SWIPE_LEFT
                    dy > 0 -> TouchpadGesture.THREE_FINGER_SWIPE_DOWN
                    else -> TouchpadGesture.THREE_FINGER_SWIPE_UP
                }

                sendGesture(gesture.displayName)
                _uiState.update {
                    it.copy(lastGesture = gesture.displayName)
                }

                // Reset to prevent multiple triggers
                lastX = 0f
                lastY = 0f
                return
            }
        }

        lastX = centerX
        lastY = centerY
    }

    private fun handleFourFingerGesture(pointers: List<Pair<Float, Float>>) {
        // 4-finger gestures (e.g., volume up/down)
        val centerY = pointers.sumOf { it.second } / 4f

        if (lastY != 0f) {
            val dy = centerY - lastY
            if (abs(dy) > 30) {
                val action = if (dy > 0) "volume_down" else "volume_up"
                sendCommand(action)
                vibrate(20)
                _uiState.update {
                    it.copy(lastGesture = "4-Finger ${if (dy > 0) "Down" else "Up"}")
                }
                lastY = 0f
                return
            }
        }
        lastY = centerY
    }

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
                _uiState.update {
                    it.copy(lastGesture = TouchpadGesture.DOUBLE_TAP.displayName)
                }
                vibrate(40)
            }
        } else {
            tapCount = 1
            // Single tap - send immediately
            sendClick("left")
            _uiState.update {
                it.copy(lastGesture = TouchpadGesture.TAP.displayName)
            }
            vibrate(20)
        }

        lastTapTime = now
    }

    fun processLongPress() {
        if (!_uiState.value.isActive) return

        sendClick("right")
        _uiState.update {
            it.copy(lastGesture = TouchpadGesture.LONG_PRESS.displayName)
        }
        vibrate(50)
    }

    fun resetGestureState() {
        lastX = 0f
        lastY = 0f
        lastScrollX = 0f
        lastScrollY = 0f
        lastPinchDistance = 0f
        lastRotation = 0f
        isGestureActive = false

        _uiState.update {
            it.copy(
                isDragging = false,
                isScrolling = false
            )
        }
    }

    // ==========================================
    // UPDATE FUNCTIONS
    // ==========================================

    // Cursor
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

    // Scroll
    private fun updateScrollSpeed(value: Float) {
        saveSetting("touchpad_scroll_speed", value)
        _uiState.update { it.copy(scrollSpeed = value) }
    }

    private fun toggleNaturalScrolling() {
        val newValue = !_uiState.value.naturalScrolling
        saveSetting("touchpad_natural_scrolling", newValue)
        _uiState.update { it.copy(naturalScrolling = newValue) }
    }

    private fun toggleTwoFingerScroll() {
        val newValue = !_uiState.value.twoFingerScroll
        saveSetting("touchpad_two_finger_scroll", newValue)
        _uiState.update { it.copy(twoFingerScroll = newValue) }
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

    // Gesture
    private fun toggleTapToClick() {
        val newValue = !_uiState.value.tapToClick
        saveSetting("touchpad_tap_to_click", newValue)
        _uiState.update { it.copy(tapToClick = newValue) }
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

    // Feedback
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

    // ==========================================
    // SEND FUNCTIONS
    // ==========================================

    private fun sendMove(dx: Float, dy: Float) {
        triggerEffect(TouchpadEffect.SendMove(dx, dy))
        connectionManager.sendMove(dx, dy)
    }

    private fun sendScroll(delta: Int) {
        triggerEffect(TouchpadEffect.SendScroll(delta))
        connectionManager.sendScroll(delta)
    }

    private fun sendClick(button: String) {
        triggerEffect(TouchpadEffect.SendClick(button))
        connectionManager.sendClick(button)
    }

    private fun sendGesture(gesture: String) {
        triggerEffect(TouchpadEffect.SendGesture(gesture))
        connectionManager.sendGesture(gesture, 0.9f)
    }

    private fun sendCommand(command: String) {
        triggerEffect(TouchpadEffect.SendCommand(command))
        connectionManager.sendControl(command)
    }

    // ==========================================
    // NAVIGATION
    // ==========================================

    private fun navigateBack() {
        triggerEffect(TouchpadEffect.NavigateBack)
    }

    private fun navigateToSettings() {
        triggerEffect(TouchpadEffect.NavigateToSettings)
    }

    // ==========================================
    // VIBRATION
    // ==========================================

    private fun vibrate(duration: Long) {
        if (!_uiState.value.hapticFeedback) return
        triggerEffect(TouchpadEffect.Vibrate(duration))

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ==========================================
    // EFFECT HELPERS
    // ==========================================

    private fun triggerEffect(effect: TouchpadEffect) {
        _effect.value = effect
    }

    fun clearEffect() {
        _effect.value = null
    }

    override fun onCleared() {
        super.onCleared()
        inertiaJob?.cancel()
        gestureTimeoutJob?.cancel()
    }
}