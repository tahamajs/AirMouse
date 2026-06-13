package com.airmouse.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Presentation mode service for controlling slideshows and presentations.
 * Supports PowerPoint, Google Slides, Keynote, and PDF presentations.
 */
class PresentationModeService(
    private val context: Context,
    private val connectionManager: ConnectionManager,
    private val prefs: PreferencesManager
) {

    companion object {
        private const val TAG = "PresentationMode"
        private const val INACTIVITY_TIMEOUT_MS = 60000L // 60 seconds
        private const val LASER_SMOOTHING_FACTOR = 0.3f
    }

    data class PresentationState(
        val isActive: Boolean = false,
        val currentSlide: Int = 0,
        val totalSlides: Int = 0,
        val slideTitle: String = "",
        val slideNotes: String = "",
        val isFullscreen: Boolean = false,
        val pointerVisible: Boolean = true,
        val timerRunning: Boolean = false,
        val elapsedTime: Long = 0,
        val laserPointerActive: Boolean = false,
        val laserX: Float = 0.5f,
        val laserY: Float = 0.5f,
        val annotations: List<Annotation> = emptyList(),
        val currentTool: Tool = Tool.LASER,
        val blackoutActive: Boolean = false,
        val whiteoutActive: Boolean = false,
        val presentationType: PresentationType = PresentationType.UNKNOWN
    )

    data class Annotation(
        val id: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val strokeWidth: Float,
        val points: List<Pair<Float, Float>>,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class PresentationAction {
        NEXT_SLIDE,
        PREV_SLIDE,
        FIRST_SLIDE,
        LAST_SLIDE,
        GO_TO_SLIDE,
        FULLSCREEN,
        LASER_POINTER,
        HIGHLIGHT,
        ANNOTATE,
        CLEAR_ANNOTATIONS,
        START_TIMER,
        STOP_TIMER,
        BLACK_SCREEN,
        WHITE_SCREEN,
        PAUSE_PRESENTATION,
        RESUME_PRESENTATION,
        SHOW_NOTES,
        HIDE_NOTES,
        ZOOM_IN,
        ZOOM_OUT,
        RESET_ZOOM
    }

    enum class Tool {
        LASER,
        HIGHLIGHTER,
        PEN,
        ERASER,
        POINTER
    }

    enum class PresentationType {
        UNKNOWN,
        POWERPOINT,
        GOOGLE_SLIDES,
        KEYNOTE,
        PDF,
        CUSTOM
    }

    private val _state = MutableStateFlow(PresentationState())
    val state: StateFlow<PresentationState> = _state.asStateFlow()

    private var gestureMappings = mutableMapOf(
        "swipe_left" to PresentationAction.NEXT_SLIDE,
        "swipe_right" to PresentationAction.PREV_SLIDE,
        "click" to PresentationAction.LASER_POINTER,
        "double_click" to PresentationAction.FULLSCREEN,
        "swipe_up" to PresentationAction.START_TIMER,
        "swipe_down" to PresentationAction.STOP_TIMER,
        "right_click" to PresentationAction.BLACK_SCREEN,
        "long_press" to PresentationAction.SHOW_NOTES,
        "swipe_up_two" to PresentationAction.ZOOM_IN,
        "swipe_down_two" to PresentationAction.ZOOM_OUT
    )

    private var laserPointerActive = false
    private var lastLaserX = 0.5f
    private var lastLaserY = 0.5f
    private var inactivityTimer: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var timerStartTime = 0L
    private var pendingAnnotation: MutableList<Pair<Float, Float>> = mutableListOf()

    // Callbacks for UI
    var onAction: ((PresentationAction, Any?) -> Unit)? = null
    var onSlideChanged: ((Int, Int, String) -> Unit)? = null
    var onLaserMoved: ((Float, Float) -> Unit)? = null
    var onAnnotationAdded: ((Annotation) -> Unit)? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val customMappings = prefs.getString("presentation_gesture_mappings", "")
        if (customMappings.isNotEmpty()) {
            try {
                val mappings = customMappings.split(";")
                mappings.forEach { mapping ->
                    val parts = mapping.split(":")
                    if (parts.size == 2) {
                        val gesture = parts[0]
                        val action = try {
                            PresentationAction.valueOf(parts[1])
                        } catch (e: Exception) {
                            null
                        }
                        if (action != null) {
                            gestureMappings[gesture] = action
                        }
                    }
                }
            } catch (e: Exception) {
                // Use default mappings
            }
        }
    }

    private fun saveMappings() {
        val mappingsString = gestureMappings.entries.joinToString(";") { "${it.key}:${it.value.name}" }
        prefs.putString("presentation_gesture_mappings", mappingsString)
    }

    /**
     * Enable presentation mode
     */
    fun enablePresentationMode() {
        if (_state.value.isActive) return

        _state.value = _state.value.copy(isActive = true)

        // Send presentation mode command to PC
        connectionManager.sendControl("presentation_mode_start")

        // Reset inactivity timer
        resetInactivityTimer()

        // Load presentation info if available
        requestPresentationInfo()

        android.util.Log.i(TAG, "Presentation mode enabled")
    }

    /**
     * Disable presentation mode
     */
    fun disablePresentationMode() {
        if (!_state.value.isActive) return

        _state.value = _state.value.copy(
            isActive = false,
            laserPointerActive = false,
            blackoutActive = false,
            whiteoutActive = false
        )
        stopTimer()

        // Disable laser pointer
        if (laserPointerActive) {
            disableLaserPointer()
        }

        connectionManager.sendControl("presentation_mode_stop")

        android.util.Log.i(TAG, "Presentation mode disabled")
    }

    /**
     * Handle gesture for presentation control
     */
    fun handleGesture(gesture: String): Boolean {
        if (!_state.value.isActive) return false

        val action = gestureMappings[gesture]
        action?.let {
            performAction(it)
            resetInactivityTimer()
        }

        return action != null
    }

    /**
     * Handle orientation changes for laser pointer
     */
    fun handleOrientation(roll: Float, yaw: Float) {
        if (!_state.value.isActive) return

        if (laserPointerActive) {
            // Convert orientation to screen coordinates
            // roll: -90 to 90 (vertical), yaw: -180 to 180 (horizontal)
            val normalizedX = ((yaw + 180f) / 360f).coerceIn(0f, 1f)
            val normalizedY = ((roll + 90f) / 180f).coerceIn(0f, 1f)

            // Apply smoothing
            val smoothedX = lastLaserX * (1 - LASER_SMOOTHING_FACTOR) + normalizedX * LASER_SMOOTHING_FACTOR
            val smoothedY = lastLaserY * (1 - LASER_SMOOTHING_FACTOR) + normalizedY * LASER_SMOOTHING_FACTOR

            lastLaserX = smoothedX
            lastLaserY = smoothedY

            _state.value = _state.value.copy(laserX = smoothedX, laserY = smoothedY)

            // Send laser position to PC
            connectionManager.send("""{"type":"laser","x":$smoothedX,"y":$smoothedY}""")
            onLaserMoved?.invoke(smoothedX, smoothedY)
        }
    }

    /**
     * Perform a presentation action
     */
    fun performAction(action: PresentationAction, vararg args: Any) {
        when (action) {
            PresentationAction.NEXT_SLIDE -> {
                val newSlide = _state.value.currentSlide + 1
                if (newSlide <= _state.value.totalSlides) {
                    goToSlide(newSlide)
                }
            }
            PresentationAction.PREV_SLIDE -> {
                val newSlide = _state.value.currentSlide - 1
                if (newSlide >= 1) {
                    goToSlide(newSlide)
                }
            }
            PresentationAction.FIRST_SLIDE -> goToSlide(1)
            PresentationAction.LAST_SLIDE -> goToSlide(_state.value.totalSlides)
            PresentationAction.GO_TO_SLIDE -> {
                if (args.isNotEmpty() && args[0] is Int) {
                    goToSlide(args[0] as Int)
                }
            }
            PresentationAction.FULLSCREEN -> toggleFullscreen()
            PresentationAction.LASER_POINTER -> toggleLaserPointer()
            PresentationAction.HIGHLIGHT -> {
                if (args.size >= 2) {
                    highlight(args[0] as Float, args[1] as Float)
                }
            }
            PresentationAction.ANNOTATE -> {
                if (args.size >= 3) {
                    addAnnotation(args[0] as Float, args[1] as Float, args[2] as Int)
                }
            }
            PresentationAction.CLEAR_ANNOTATIONS -> clearAnnotations()
            PresentationAction.START_TIMER -> startTimer()
            PresentationAction.STOP_TIMER -> stopTimer()
            PresentationAction.BLACK_SCREEN -> activateBlackScreen()
            PresentationAction.WHITE_SCREEN -> activateWhiteScreen()
            PresentationAction.PAUSE_PRESENTATION -> pausePresentation()
            PresentationAction.RESUME_PRESENTATION -> resumePresentation()
            PresentationAction.SHOW_NOTES -> showNotes()
            PresentationAction.HIDE_NOTES -> hideNotes()
            PresentationAction.ZOOM_IN -> zoom(true)
            PresentationAction.ZOOM_OUT -> zoom(false)
            PresentationAction.RESET_ZOOM -> resetZoom()
        }

        onAction?.invoke(action, if (args.isNotEmpty()) args[0] else null)
    }

    /**
     * Go to specific slide
     */
    private fun goToSlide(slideNumber: Int) {
        connectionManager.send("""{"type":"presentation","action":"goto","slide":$slideNumber}""")
        _state.value = _state.value.copy(currentSlide = slideNumber)
        onSlideChanged?.invoke(slideNumber, _state.value.totalSlides, _state.value.slideTitle)
        android.util.Log.d(TAG, "Go to slide $slideNumber")
    }

    /**
     * Set total slides count
     */
    fun setTotalSlides(total: Int) {
        _state.value = _state.value.copy(totalSlides = total)
    }

    /**
     * Update current slide info
     */
    fun updateSlideInfo(current: Int, title: String = "", notes: String = "") {
        _state.value = _state.value.copy(
            currentSlide = current,
            slideTitle = title,
            slideNotes = notes
        )
        onSlideChanged?.invoke(current, _state.value.totalSlides, title)
    }

    /**
     * Update presentation type
     */
    fun updatePresentationType(type: PresentationType) {
        _state.value = _state.value.copy(presentationType = type)
    }

    /**
     * Request presentation info from server
     */
    private fun requestPresentationInfo() {
        connectionManager.send("""{"type":"presentation","action":"info"}""")
    }

    /**
     * Toggle fullscreen mode
     */
    private fun toggleFullscreen() {
        val newState = !_state.value.isFullscreen
        connectionManager.send("""{"type":"presentation","action":"fullscreen","enabled":$newState}""")
        _state.value = _state.value.copy(isFullscreen = newState)
    }

    /**
     * Toggle laser pointer
     */
    private fun toggleLaserPointer() {
        laserPointerActive = !laserPointerActive
        _state.value = _state.value.copy(laserPointerActive = laserPointerActive)

        if (!laserPointerActive) {
            // Reset laser position when disabled
            lastLaserX = 0.5f
            lastLaserY = 0.5f
            _state.value = _state.value.copy(laserX = 0.5f, laserY = 0.5f)
        }

        connectionManager.send("""{"type":"laser","enabled":$laserPointerActive}""")
        android.util.Log.d(TAG, "Laser pointer: ${if (laserPointerActive) "ON" else "OFF"}")
    }

    /**
     * Disable laser pointer
     */
    private fun disableLaserPointer() {
        laserPointerActive = false
        _state.value = _state.value.copy(laserPointerActive = false)
        connectionManager.send("""{"type":"laser","enabled":false}""")
    }

    /**
     * Highlight at specific coordinates
     */
    private fun highlight(x: Float, y: Float) {
        connectionManager.send("""{"type":"presentation","action":"highlight","x":$x,"y":$y}""")
    }

    /**
     * Add annotation
     */
    private fun addAnnotation(x: Float, y: Float, color: Int) {
        pendingAnnotation.add(Pair(x, y))

        // Add annotation after path is complete or on timeout
        if (pendingAnnotation.size >= 5) {
            finalizeAnnotation(color)
        }
    }

    /**
     * Start drawing annotation (for continuous drawing)
     */
    fun startAnnotation(x: Float, y: Float, color: Int) {
        pendingAnnotation.clear()
        pendingAnnotation.add(Pair(x, y))
    }

    /**
     * Continue drawing annotation
     */
    fun continueAnnotation(x: Float, y: Float) {
        pendingAnnotation.add(Pair(x, y))
        // Send intermediate points for smooth drawing
        connectionManager.send("""{"type":"annotation","action":"draw","x":$x,"y":$y}""")
    }

    /**
     * End and save annotation
     */
    fun endAnnotation(color: Int, strokeWidth: Float = 3f) {
        finalizeAnnotation(color, strokeWidth)
    }

    /**
     * Finalize and save annotation
     */
    private fun finalizeAnnotation(color: Int, strokeWidth: Float = 3f) {
        if (pendingAnnotation.isEmpty()) return

        val annotation = Annotation(
            id = System.currentTimeMillis().toString(),
            x = pendingAnnotation.first().first,
            y = pendingAnnotation.first().second,
            color = color,
            strokeWidth = strokeWidth,
            points = pendingAnnotation.toList()
        )

        _state.value = _state.value.copy(
            annotations = _state.value.annotations + annotation
        )

        // Send annotation to PC
        val pointsJson = pendingAnnotation.joinToString(",") { (x, y) ->
            "{\"x\":$x,\"y\":$y}"
        }
        connectionManager.send("""{"type":"annotation","action":"add","color":$color,"strokeWidth":$strokeWidth,"points":[$pointsJson]}""")

        onAnnotationAdded?.invoke(annotation)
        pendingAnnotation.clear()
    }

    /**
     * Clear all annotations
     */
    private fun clearAnnotations() {
        _state.value = _state.value.copy(annotations = emptyList())
        connectionManager.send("""{"type":"annotation","action":"clear"}""")
    }

    /**
     * Start presentation timer
     */
    private fun startTimer() {
        if (timerJob?.isActive == true) return

        timerStartTime = System.currentTimeMillis() - _state.value.elapsedTime
        timerJob = scope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - timerStartTime
                _state.value = _state.value.copy(
                    timerRunning = true,
                    elapsedTime = elapsed
                )
                delay(1000)
            }
        }
        android.util.Log.d(TAG, "Timer started")
    }

    /**
     * Stop presentation timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        _state.value = _state.value.copy(timerRunning = false)
        android.util.Log.d(TAG, "Timer stopped")
    }

    /**
     * Reset timer
     */
    fun resetTimer() {
        stopTimer()
        _state.value = _state.value.copy(elapsedTime = 0)
        startTimer()
    }

    /**
     * Format elapsed time for display
     */
    fun getFormattedElapsedTime(): String {
        val seconds = (_state.value.elapsedTime / 1000) % 60
        val minutes = (_state.value.elapsedTime / (1000 * 60)) % 60
        val hours = (_state.value.elapsedTime / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Activate black screen (blackout)
     */
    private fun activateBlackScreen() {
        if (_state.value.whiteoutActive) {
            _state.value = _state.value.copy(whiteoutActive = false)
        }
        _state.value = _state.value.copy(blackoutActive = !_state.value.blackoutActive)
        connectionManager.send("""{"type":"presentation","action":"blackscreen","enabled":${_state.value.blackoutActive}}""")
        android.util.Log.d(TAG, "Black screen: ${_state.value.blackoutActive}")
    }

    /**
     * Activate white screen (whiteout)
     */
    private fun activateWhiteScreen() {
        if (_state.value.blackoutActive) {
            _state.value = _state.value.copy(blackoutActive = false)
        }
        _state.value = _state.value.copy(whiteoutActive = !_state.value.whiteoutActive)
        connectionManager.send("""{"type":"presentation","action":"whitescreen","enabled":${_state.value.whiteoutActive}}""")
        android.util.Log.d(TAG, "White screen: ${_state.value.whiteoutActive}")
    }

    /**
     * Clear blackout/whiteout
     */
    fun clearScreenOverlay() {
        if (_state.value.blackoutActive || _state.value.whiteoutActive) {
            _state.value = _state.value.copy(blackoutActive = false, whiteoutActive = false)
            connectionManager.send("""{"type":"presentation","action":"clearoverlay"}""")
        }
    }

    /**
     * Pause presentation
     */
    private fun pausePresentation() {
        connectionManager.send("""{"type":"presentation","action":"pause"}""")
        stopTimer()
        android.util.Log.d(TAG, "Presentation paused")
    }

    /**
     * Resume presentation
     */
    private fun resumePresentation() {
        connectionManager.send("""{"type":"presentation","action":"resume"}""")
        if (_state.value.timerRunning) {
            startTimer()
        }
        android.util.Log.d(TAG, "Presentation resumed")
    }

    /**
     * Show speaker notes
     */
    private fun showNotes() {
        _state.value = _state.value.copy(pointerVisible = true)
        connectionManager.send("""{"type":"presentation","action":"shownotes"}""")
    }

    /**
     * Hide speaker notes
     */
    private fun hideNotes() {
        _state.value = _state.value.copy(pointerVisible = false)
        connectionManager.send("""{"type":"presentation","action":"hidenotes"}""")
    }

    /**
     * Zoom in/out
     */
    private fun zoom(zoomIn: Boolean) {
        val action = if (zoomIn) "zoomin" else "zoomout"
        connectionManager.send("""{"type":"presentation","action":"$action"}""")
    }

    /**
     * Reset zoom to default
     */
    private fun resetZoom() {
        connectionManager.send("""{"type":"presentation","action":"zoomreset"}""")
    }

    /**
     * Set current tool
     */
    fun setTool(tool: Tool) {
        _state.value = _state.value.copy(currentTool = tool)

        // Disable laser pointer if switching away
        if (tool != Tool.LASER && laserPointerActive) {
            toggleLaserPointer()
        }

        connectionManager.send("""{"type":"presentation","action":"setTool","tool":"${tool.name.lowercase()}"}""")
    }

    /**
     * Customize gesture mapping
     */
    fun customizeGesture(gesture: String, action: PresentationAction) {
        gestureMappings[gesture] = action
        saveMappings()
    }

    /**
     * Remove gesture mapping
     */
    fun removeGestureMapping(gesture: String) {
        gestureMappings.remove(gesture)
        saveMappings()
    }

    /**
     * Get current gesture mapping
     */
    fun getGestureMapping(gesture: String): PresentationAction? {
        return gestureMappings[gesture]
    }

    /**
     * Get all gesture mappings
     */
    fun getAllGestureMappings(): Map<String, PresentationAction> {
        return gestureMappings.toMap()
    }

    /**
     * Reset to default gesture mappings
     */
    fun resetToDefaultGestures() {
        gestureMappings = mutableMapOf(
            "swipe_left" to PresentationAction.NEXT_SLIDE,
            "swipe_right" to PresentationAction.PREV_SLIDE,
            "click" to PresentationAction.LASER_POINTER,
            "double_click" to PresentationAction.FULLSCREEN,
            "swipe_up" to PresentationAction.START_TIMER,
            "swipe_down" to PresentationAction.STOP_TIMER,
            "right_click" to PresentationAction.BLACK_SCREEN,
            "long_press" to PresentationAction.SHOW_NOTES,
            "swipe_up_two" to PresentationAction.ZOOM_IN,
            "swipe_down_two" to PresentationAction.ZOOM_OUT
        )
        saveMappings()
    }

    /**
     * Reset inactivity timer
     */
    private fun resetInactivityTimer() {
        inactivityTimer?.let { handler.removeCallbacks(it) }
        inactivityTimer = Runnable {
            if (_state.value.isActive && laserPointerActive) {
                disableLaserPointer()
            }
        }
        handler.postDelayed(inactivityTimer, INACTIVITY_TIMEOUT_MS)
    }

    /**
     * Check if presentation mode is active
     */
    fun isActive(): Boolean = _state.value.isActive

    /**
     * Get current slide number
     */
    fun getCurrentSlide(): Int = _state.value.currentSlide

    /**
     * Get total slides
     */
    fun getTotalSlides(): Int = _state.value.totalSlides

    /**
     * Get current tool
     */
    fun getCurrentTool(): Tool = _state.value.currentTool

    /**
     * Clear all state (when leaving presentation)
     */
    fun clearState() {
        disablePresentationMode()
        clearAnnotations()
        _state.value = PresentationState()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        disablePresentationMode()
        timerJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }
}