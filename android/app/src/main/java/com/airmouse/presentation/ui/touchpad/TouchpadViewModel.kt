// app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadViewModel.kt
package com.airmouse.presentation.ui.touchpad

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
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

    // Gesture state
    private var lastX = 0f
    private var lastY = 0f
    private var lastScrollX = 0f
    private var lastScrollY = 0f
    private var lastPinchDistance = 0f
    private var lastRotation = 0f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var pointerCount = 0

    // Inertia simulation
    private var inertiaJob: Job? = null

    init {
        loadSettings()
    }

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

    private fun saveSettings() {
        prefs.putFloat("touchpad_sensitivity", _uiState.value.sensitivity)
        prefs.putFloat("touchpad_scroll_speed", _uiState.value.scrollSpeed)
        prefs.putBoolean("touchpad_natural_scrolling", _uiState.value.naturalScrolling)
        prefs.putBoolean("touchpad_tap_to_click", _uiState.value.tapToClick)
        prefs.putBoolean("touchpad_two_finger_scroll", _uiState.value.twoFingerScroll)
        prefs.putBoolean("touchpad_three_finger_swipe", _uiState.value.threeFingerSwipe)
        prefs.putBoolean("touchpad_edge_scrolling", _uiState.value.edgeScrolling)
        prefs.putBoolean("touchpad_haptic_feedback", _uiState.value.hapticFeedback)
        prefs.putFloat("touchpad_cursor_speed", _uiState.value.cursorSpeed)
        prefs.putBoolean("touchpad_acceleration", _uiState.value.accelerationEnabled)
        prefs.putBoolean("touchpad_invert_vertical", _uiState.value.invertVertical)
        prefs.putBoolean("touchpad_invert_horizontal", _uiState.value.invertHorizontal)
        prefs.putInt("touchpad_pointer_speed", _uiState.value.pointerSpeed)
        prefs.putInt("touchpad_double_tap_delay", _uiState.value.doubleTapDelay)
        prefs.putBoolean("touchpad_scroll_inertia", _uiState.value.scrollInertia)
        prefs.putBoolean("touchpad_pinch_to_zoom", _uiState.value.pinchToZoom)
        prefs.putBoolean("touchpad_rotate_to_rotate", _uiState.value.rotateToRotate)
        prefs.putBoolean("touchpad_show_touch_points", _uiState.value.showTouchPoints)
    }

    fun toggleTouchpad() {
        val newState = !_uiState.value.isActive
        _uiState.update { it.copy(isActive = newState) }
        if (newState) {
            connectionManager.send("""{"type":"touchpad","command":"start"}""")
            vibrate(50)
        } else {
            connectionManager.send("""{"type":"touchpad","command":"stop"}""")
            resetGestureState()
        }
    }

    /**
     * Called from the UI with touch data. This method handles:
     * - Single‑finger move (cursor)
     * - Two‑finger scroll
     * - Pinch/zoom and rotate
     * - Edge scrolling
     */
    fun processTouchEvent(
        x: Float, y: Float,
        pointerCount: Int,
        pointers: List<Pair<Float, Float>>,
        pressure: Float = 1f
    ) {
        if (!_uiState.value.isActive) return

        // Update UI with current touch points (for visual feedback)
        _uiState.update {
            it.copy(
                currentX = x,
                currentY = y,
                touchPoints = pointers.mapIndexed { idx, pos ->
                    TouchPoint(idx, pos.first, pos.second, pressure)
                }
            )
        }

        when (pointerCount) {
            1 -> handleSingleFingerMove(x, y, pressure)
            2 -> handleTwoFingerGesture(pointers)
            else -> {
                // 3+ fingers – could be swipe or ignore
                if (_uiState.value.threeFingerSwipe && pointerCount == 3) {
                    handleThreeFingerSwipe(pointers)
                }
            }
        }
        this.pointerCount = pointerCount
    }

    private fun handleSingleFingerMove(x: Float, y: Float, pressure: Float) {
        if (lastX == 0f && lastY == 0f) {
            lastX = x
            lastY = y
            return
        }

        // Edge scrolling: if finger near edges, scroll instead of move
        if (_uiState.value.edgeScrolling && (x < 50 || x > 350 || y < 50 || y > 550)) {
            var scrollX = 0f
            var scrollY = 0f
            if (x < 50) scrollX = (50 - x) / 50f * _uiState.value.scrollSpeed
            if (x > 350) scrollX = -(x - 350) / 50f * _uiState.value.scrollSpeed
            if (y < 50) scrollY = (50 - y) / 50f * _uiState.value.scrollSpeed
            if (y > 550) scrollY = -(y - 550) / 50f * _uiState.value.scrollSpeed

            if (scrollX != 0f || scrollY != 0f) {
                sendScroll(scrollX.toInt(), scrollY.toInt())
                _uiState.update { it.copy(lastGesture = "Edge Scroll") }
                return
            }
        }

        // Normal cursor movement
        var dx = (x - lastX) * _uiState.value.sensitivity * _uiState.value.cursorSpeed
        var dy = (y - lastY) * _uiState.value.sensitivity * _uiState.value.cursorSpeed

        // Acceleration based on speed
        if (_uiState.value.accelerationEnabled) {
            val speed = sqrt(dx * dx + dy * dy)
            val factor = 1 + (speed / 50f).coerceIn(0f, 2f)
            dx *= factor
            dy *= factor
        }

        if (_uiState.value.invertHorizontal) dx = -dx
        if (_uiState.value.invertVertical) dy = -dy

        val scaledDx = (dx * _uiState.value.pointerSpeed / 50).toInt()
        val scaledDy = (dy * _uiState.value.pointerSpeed / 50).toInt()

        if (scaledDx != 0 || scaledDy != 0) {
            sendMove(scaledDx, scaledDy)
            _uiState.update { it.copy(isDragging = true, lastGesture = "Move") }
        }

        lastX = x
        lastY = y
    }

    private fun handleTwoFingerGesture(pointers: List<Pair<Float, Float>>) {
        if (pointers.size < 2) return
        val p1 = pointers[0]
        val p2 = pointers[1]

        if (_uiState.value.twoFingerScroll) {
            val currentX = (p1.first + p2.first) / 2f
            val currentY = (p1.second + p2.second) / 2f

            if (lastScrollX != 0f || lastScrollY != 0f) {
                var dx = (currentX - lastScrollX) * _uiState.value.scrollSpeed
                var dy = (currentY - lastScrollY) * _uiState.value.scrollSpeed

                if (_uiState.value.naturalScrolling) {
                    dx = -dx
                    dy = -dy
                }

                if (abs(dx) > 2 || abs(dy) > 2) {
                    sendScroll(dx.toInt(), dy.toInt())
                    _uiState.update {
                        it.copy(
                            isScrolling = true,
                            lastGesture = when {
                                abs(dy) > abs(dx) -> if (dy > 0) "Scroll Down" else "Scroll Up"
                                else -> if (dx > 0) "Scroll Right" else "Scroll Left"
                            }
                        )
                    }
                    if (_uiState.value.hapticFeedback) vibrate(15)
                }
            }
            lastScrollX = currentX
            lastScrollY = currentY
        }

        // Pinch to zoom (distance between fingers)
        if (_uiState.value.pinchToZoom) {
            val dx = p1.first - p2.first
            val dy = p1.second - p2.second
            val distance = sqrt(dx * dx + dy * dy)

            if (lastPinchDistance != 0f) {
                val delta = distance - lastPinchDistance
                if (abs(delta) > 5) {
                    // Send zoom command (e.g., Ctrl+Wheel)
                    val zoomDelta = if (delta > 0) 1 else -1
                    sendZoom(zoomDelta)
                    _uiState.update { it.copy(lastGesture = if (zoomDelta > 0) "Zoom In" else "Zoom Out") }
                }
            }
            lastPinchDistance = distance
        }

        // Rotation
        if (_uiState.value.rotateToRotate) {
            val angle = atan2(p2.second - p1.second, p2.first - p1.first)
            if (lastRotation != 0f) {
                val delta = angle - lastRotation
                if (abs(delta) > 0.1f) {
                    sendRotate(delta.toFloat())
                    _uiState.update { it.copy(lastGesture = "Rotate") }
                }
            }
            lastRotation = angle
        }
    }

    private fun handleThreeFingerSwipe(pointers: List<Pair<Float, Float>>) {
        if (pointers.size < 3) return
        // Compute centroid of three fingers
        val centerX = pointers.sumOf { it.first } / 3f
        val centerY = pointers.sumOf { it.second } / 3f

        if (lastX == 0f && lastY == 0f) {
            lastX = centerX
            lastY = centerY
            return
        }

        val dx = centerX - lastX
        val dy = centerY - lastY

        if (abs(dx) > 30 || abs(dy) > 30) {
            when {
                abs(dx) > abs(dy) -> {
                    if (dx > 0) sendMediaKey("MEDIA_NEXT")
                    else sendMediaKey("MEDIA_PREV")
                    _uiState.update { it.copy(lastGesture = if (dx > 0) "Swipe Right" else "Swipe Left") }
                }
                else -> {
                    if (dy > 0) sendMediaKey("MEDIA_VOLUME_DOWN")
                    else sendMediaKey("MEDIA_VOLUME_UP")
                    _uiState.update { it.copy(lastGesture = if (dy > 0) "Swipe Down" else "Swipe Up") }
                }
            }
            vibrate(40)
            // Reset to avoid repeated triggers
            lastX = centerX
            lastY = centerY
        } else {
            lastX = centerX
            lastY = centerY
        }
    }

    fun processTap(x: Float, y: Float) {
        if (!_uiState.value.isActive || !_uiState.value.tapToClick) return

        val now = System.currentTimeMillis()
        if (now - lastTapTime < _uiState.value.doubleTapDelay) {
            tapCount++
            if (tapCount == 2) {
                sendDoubleClick()
                _uiState.update { it.copy(lastGesture = "Double Tap") }
                vibrate(30)
                tapCount = 0
            }
        } else {
            tapCount = 1
            sendClick("left")
            _uiState.update { it.copy(lastGesture = "Tap") }
            vibrate(20)
        }
        lastTapTime = now
    }

    fun processLongPress() {
        if (!_uiState.value.isActive) return
        sendClick("right")
        _uiState.update { it.copy(lastGesture = "Long Press") }
        vibrate(50)
    }

    fun resetGestureState() {
        lastX = 0f
        lastY = 0f
        lastScrollX = 0f
        lastScrollY = 0f
        lastPinchDistance = 0f
        lastRotation = 0f
        pointerCount = 0
        inertiaJob?.cancel()
        _uiState.update {
            it.copy(
                isDragging = false,
                isScrolling = false,
                lastGesture = ""
            )
        }
    }

    private fun sendMove(dx: Int, dy: Int) {
        connectionManager.sendMove(dx.toFloat(), dy.toFloat())
    }

    private fun sendClick(button: String) {
        connectionManager.sendClick(button)
    }

    private fun sendDoubleClick() {
        connectionManager.sendDoubleClick()
    }

    private fun sendRightClick() {
        connectionManager.sendRightClick()
    }

    private fun sendScroll(deltaX: Int, deltaY: Int) {
        // For simplicity, we send vertical scroll as main scroll, horizontal as shift+wheel
        if (deltaY != 0) connectionManager.sendScroll(deltaY)
        if (deltaX != 0) connectionManager.send("""{"type":"scroll","deltaX":$deltaX,"deltaY":0,"horizontal":true}""")
    }

    private fun sendZoom(delta: Int) {
        connectionManager.send("""{"type":"zoom","delta":$delta}""")
    }

    private fun sendRotate(angle: Float) {
        connectionManager.send("""{"type":"rotate","angle":$angle}""")
    }

    private fun sendMediaKey(key: String) {
        connectionManager.send("""{"type":"media","key":"$key"}""")
    }

    private fun vibrate(duration: Long) {
        if (!_uiState.value.hapticFeedback) return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // ---------- Settings updaters ----------
    fun updateSensitivity(v: Float) { _uiState.update { it.copy(sensitivity = v) }; saveSettings() }
    fun updateScrollSpeed(v: Float) { _uiState.update { it.copy(scrollSpeed = v) }; saveSettings() }
    fun updateNaturalScrolling(e: Boolean) { _uiState.update { it.copy(naturalScrolling = e) }; saveSettings() }
    fun updateTapToClick(e: Boolean) { _uiState.update { it.copy(tapToClick = e) }; saveSettings() }
    fun updateTwoFingerScroll(e: Boolean) { _uiState.update { it.copy(twoFingerScroll = e) }; saveSettings() }
    fun updateThreeFingerSwipe(e: Boolean) { _uiState.update { it.copy(threeFingerSwipe = e) }; saveSettings() }
    fun updateEdgeScrolling(e: Boolean) { _uiState.update { it.copy(edgeScrolling = e) }; saveSettings() }
    fun updateHapticFeedback(e: Boolean) { _uiState.update { it.copy(hapticFeedback = e) }; saveSettings() }
    fun updateCursorSpeed(v: Float) { _uiState.update { it.copy(cursorSpeed = v) }; saveSettings() }
    fun updateAcceleration(e: Boolean) { _uiState.update { it.copy(accelerationEnabled = e) }; saveSettings() }
    fun updateInvertVertical(e: Boolean) { _uiState.update { it.copy(invertVertical = e) }; saveSettings() }
    fun updateInvertHorizontal(e: Boolean) { _uiState.update { it.copy(invertHorizontal = e) }; saveSettings() }
    fun updatePointerSpeed(v: Int) { _uiState.update { it.copy(pointerSpeed = v) }; saveSettings() }
    fun updateDoubleTapDelay(v: Int) { _uiState.update { it.copy(doubleTapDelay = v) }; saveSettings() }
    fun updateScrollInertia(e: Boolean) { _uiState.update { it.copy(scrollInertia = e) }; saveSettings() }
    fun updatePinchToZoom(e: Boolean) { _uiState.update { it.copy(pinchToZoom = e) }; saveSettings() }
    fun updateRotateToRotate(e: Boolean) { _uiState.update { it.copy(rotateToRotate = e) }; saveSettings() }
    fun updateShowTouchPoints(e: Boolean) { _uiState.update { it.copy(showTouchPoints = e) }; saveSettings() }

    fun applyPreset(mode: TouchpadMode) {
        when (mode) {
            TouchpadMode.STANDARD -> {
                updateSensitivity(1.0f)
                updateScrollSpeed(1.0f)
                updateAcceleration(true)
                updateTwoFingerScroll(true)
                updatePinchToZoom(true)
                updateCursorSpeed(1.0f)
                updatePointerSpeed(50)
            }
            TouchpadMode.PRECISION -> {
                updateSensitivity(0.7f)
                updateScrollSpeed(0.8f)
                updateAcceleration(false)
                updateTwoFingerScroll(true)
                updatePinchToZoom(true)
                updateCursorSpeed(0.8f)
                updatePointerSpeed(40)
            }
            TouchpadMode.GAMING -> {
                updateSensitivity(1.5f)
                updateScrollSpeed(0.5f)
                updateAcceleration(true)
                updateTwoFingerScroll(false)
                updatePinchToZoom(false)
                updateCursorSpeed(1.2f)
                updatePointerSpeed(60)
            }
            TouchpadMode.PRESENTATION -> {
                updateSensitivity(0.8f)
                updateScrollSpeed(1.2f)
                updateAcceleration(false)
                updateTwoFingerScroll(true)
                updateThreeFingerSwipe(true)
                updateCursorSpeed(0.9f)
                updatePointerSpeed(30)
            }
        }
        vibrate(30)
        _uiState.update { it.copy(lastGesture = "Preset: ${mode.displayName}") }
    }

    fun resetToDefaults() {
        updateSensitivity(1.0f)
        updateScrollSpeed(1.0f)
        updateNaturalScrolling(true)
        updateTapToClick(true)
        updateTwoFingerScroll(true)
        updateThreeFingerSwipe(true)
        updateEdgeScrolling(false)
        updateHapticFeedback(true)
        updateCursorSpeed(1.0f)
        updateAcceleration(true)
        updateInvertVertical(false)
        updateInvertHorizontal(false)
        updatePointerSpeed(50)
        updateDoubleTapDelay(300)
        updateScrollInertia(true)
        updatePinchToZoom(true)
        updateRotateToRotate(false)
        updateShowTouchPoints(false)
        _uiState.update { it.copy(lastGesture = "Reset to defaults") }
        vibrate(50)
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isActive) {
            connectionManager.send("""{"type":"touchpad","command":"stop"}""")
        }
    }
}