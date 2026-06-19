package com.airmouse.presentation.ui.touchpad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class TouchpadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState = _uiState.asStateFlow()

    private var lastX = 0f; private var lastY = 0f
    private var lastScrollX = 0f; private var lastScrollY = 0f
    private var lastPinchDistance = 0f; private var lastRotation = 0f
    private var lastTapTime = 0L; private var tapCount = 0
    private var inertiaJob: Job? = null

    init { loadSettings() }

    fun resetToDefaults() {
        _uiState.value = TouchpadUiState()
        loadSettings()
    }

    fun processTap(x: Float, y: Float) {
        connectionManager.send("""{"type":"touchpad","action":"tap","x":$x,"y":$y}""")
        _uiState.update { it.copy(lastGesture = "Tap") }
        vibrate(30)
    }

    fun processLongPress() {
        connectionManager.send("""{"type":"touchpad","action":"long_press"}""")
        _uiState.update { it.copy(lastGesture = "Long Press") }
        vibrate(60)
    }

    fun applyPreset(name: String) {
        when (name.lowercase()) {
            "sensitive" -> {
                updateSensitivity(1.4f)
                updateCursorSpeed(1.3f)
                updateScrollSpeed(1.2f)
            }
            "precision" -> {
                updateSensitivity(0.8f)
                updateCursorSpeed(0.8f)
                updateScrollSpeed(0.8f)
            }
            else -> resetToDefaults()
        }
    }

    fun updateTwoFingerScroll(enabled: Boolean) = prefs.putBoolean("touchpad_two_finger_scroll", enabled).also { _uiState.update { it.copy(twoFingerScroll = enabled) } }
    fun updateNaturalScrolling(enabled: Boolean) = prefs.putBoolean("touchpad_natural_scrolling", enabled).also { _uiState.update { it.copy(naturalScrolling = enabled) } }
    fun updateScrollSpeed(speed: Float) = prefs.putFloat("touchpad_scroll_speed", speed).also { _uiState.update { it.copy(scrollSpeed = speed) } }
    fun updateEdgeScrolling(enabled: Boolean) = prefs.putBoolean("touchpad_edge_scrolling", enabled).also { _uiState.update { it.copy(edgeScrolling = enabled) } }
    fun updateScrollInertia(enabled: Boolean) = prefs.putBoolean("touchpad_scroll_inertia", enabled).also { _uiState.update { it.copy(scrollInertia = enabled) } }
    fun updateSensitivity(value: Float) = prefs.putFloat("touchpad_sensitivity", value).also { _uiState.update { it.copy(sensitivity = value) } }
    fun updateCursorSpeed(value: Float) = prefs.putFloat("touchpad_cursor_speed", value).also { _uiState.update { it.copy(cursorSpeed = value) } }
    fun updatePointerSpeed(value: Int) = prefs.putInt("touchpad_pointer_speed", value).also { _uiState.update { it.copy(pointerSpeed = value) } }
    fun updateAcceleration(enabled: Boolean) = prefs.putBoolean("touchpad_acceleration", enabled).also { _uiState.update { it.copy(accelerationEnabled = enabled) } }
    fun updateInvertVertical(enabled: Boolean) = prefs.putBoolean("touchpad_invert_vertical", enabled).also { _uiState.update { it.copy(invertVertical = enabled) } }
    fun updateInvertHorizontal(enabled: Boolean) = prefs.putBoolean("touchpad_invert_horizontal", enabled).also { _uiState.update { it.copy(invertHorizontal = enabled) } }
    fun updateTapToClick(enabled: Boolean) = prefs.putBoolean("touchpad_tap_to_click", enabled).also { _uiState.update { it.copy(tapToClick = enabled) } }
    fun updateDoubleTapDelay(delayMs: Int) = prefs.putInt("touchpad_double_tap_delay", delayMs).also { _uiState.update { it.copy(doubleTapDelay = delayMs) } }
    fun updateThreeFingerSwipe(enabled: Boolean) = prefs.putBoolean("touchpad_three_finger_swipe", enabled).also { _uiState.update { it.copy(threeFingerSwipe = enabled) } }
    fun updatePinchToZoom(enabled: Boolean) = prefs.putBoolean("touchpad_pinch_to_zoom", enabled).also { _uiState.update { it.copy(pinchToZoom = enabled) } }
    fun updateRotateToRotate(enabled: Boolean) = prefs.putBoolean("touchpad_rotate_to_rotate", enabled).also { _uiState.update { it.copy(rotateToRotate = enabled) } }
    fun updateHapticFeedback(enabled: Boolean) = prefs.putBoolean("touchpad_haptic_feedback", enabled).also { _uiState.update { it.copy(hapticFeedback = enabled) } }
    fun updateShowTouchPoints(enabled: Boolean) = prefs.putBoolean("touchpad_show_touch_points", enabled).also { _uiState.update { it.copy(showTouchPoints = enabled) } }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                sensitivity = prefs.getFloat("touchpad_sensitivity", 1.0f),
                scrollSpeed = prefs.getFloat("touchpad_scroll_speed", 1.0f),
                naturalScrolling = prefs.getBoolean("touchpad_natural_scrolling", true),
                tapToClick = prefs.getBoolean("touchpad_tap_to_click", true),
                twoFingerScroll = prefs.getBoolean("touchpad_two_finger_scroll", true),
                threeFingerSwipe = prefs.getBoolean("touchpad_three_finger_swipe", true),
                edgeScrolling = prefs.getBoolean("touchpad_edge_scrolling", false),
                hapticFeedback = prefs.getBoolean("touchpad_haptic_feedback", true),
                cursorSpeed = prefs.getFloat("touchpad_cursor_speed", 1.0f),
                accelerationEnabled = prefs.getBoolean("touchpad_acceleration", true),
                invertVertical = prefs.getBoolean("touchpad_invert_vertical", false),
                invertHorizontal = prefs.getBoolean("touchpad_invert_horizontal", false),
                pointerSpeed = prefs.getInt("touchpad_pointer_speed", 50),
                doubleTapDelay = prefs.getInt("touchpad_double_tap_delay", 300),
                scrollInertia = prefs.getBoolean("touchpad_scroll_inertia", true),
                pinchToZoom = prefs.getBoolean("touchpad_pinch_to_zoom", true),
                rotateToRotate = prefs.getBoolean("touchpad_rotate_to_rotate", false),
                showTouchPoints = prefs.getBoolean("touchpad_show_touch_points", false)
            )
        }
    }

    fun toggleTouchpad() {
        val newState = !_uiState.value.isActive
        _uiState.update { it.copy(isActive = newState) }
        connectionManager.send("""{"type":"touchpad","command":"${if (newState) "start" else "stop"}"}""")
        if (newState) vibrate(50) else resetGestureState()
    }

    fun processTouchEvent(x: Float, y: Float, count: Int, pointers: List<Pair<Float, Float>>, pressure: Float) {
        if (!_uiState.value.isActive) return
        _uiState.update { it.copy(currentX = x, currentY = y, touchPoints = pointers.mapIndexed { i, p -> TouchPoint(i, p.first, p.second, pressure) }) }
        when (count) {
            1 -> handleSingleFingerMove(x, y)
            2 -> handleTwoFingerGesture(pointers)
            3 -> if (_uiState.value.threeFingerSwipe) handleThreeFingerSwipe(pointers)
        }
    }

    private fun handleSingleFingerMove(x: Float, y: Float) {
        if (lastX == 0f && lastY == 0f) { lastX = x; lastY = y; return }

        var dx = (x - lastX) * _uiState.value.sensitivity * _uiState.value.cursorSpeed
        var dy = (y - lastY) * _uiState.value.sensitivity * _uiState.value.cursorSpeed

        if (_uiState.value.accelerationEnabled) {
            val factor = 1 + (sqrt(dx * dx + dy * dy) / 50f).coerceIn(0f, 2f)
            dx *= factor; dy *= factor
        }

        connectionManager.sendMove(if (_uiState.value.invertHorizontal) -dx else dx, if (_uiState.value.invertVertical) -dy else dy)
        lastX = x; lastY = y
        _uiState.update { it.copy(isDragging = true, lastGesture = "Move") }
    }

    private fun handleTwoFingerGesture(pointers: List<Pair<Float, Float>>) {
        val currentX = (pointers[0].first + pointers[1].first) / 2f
        val currentY = (pointers[0].second + pointers[1].second) / 2f
        if (lastScrollX != 0f || lastScrollY != 0f) {
            var dx = (currentX - lastScrollX) * _uiState.value.scrollSpeed
            var dy = (currentY - lastScrollY) * _uiState.value.scrollSpeed
            if (_uiState.value.naturalScrolling) { dx = -dx; dy = -dy }
            if (abs(dx) > 2 || abs(dy) > 2) connectionManager.sendScroll(dy.toInt())
        }
        lastScrollX = currentX; lastScrollY = currentY
    }

    private fun handleThreeFingerSwipe(pointers: List<Pair<Float, Float>>) {
        val cX = pointers.sumOf { it.first.toDouble() } / 3.0
        val cY = pointers.sumOf { it.second.toDouble() } / 3.0
        // ... (Implement your swipe logic here)
    }

    fun resetGestureState() {
        lastX = 0f; lastY = 0f; lastScrollX = 0f; lastScrollY = 0f
        _uiState.update { it.copy(isDragging = false, isScrolling = false, lastGesture = "") }
    }

    private fun vibrate(duration: Long) {
        if (!_uiState.value.hapticFeedback) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onCleared() {
        super.onCleared()
        inertiaJob?.cancel()
    }
}
