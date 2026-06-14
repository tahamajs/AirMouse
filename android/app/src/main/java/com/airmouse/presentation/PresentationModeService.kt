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
 * Professional Presentation Mode Service
 * Controls PowerPoint, Google Slides, Keynote, and PDF presentations
 * Features: Laser pointer, annotations, timer, blackout/whiteout, gestures
 */
class PresentationModeService(
    private val context: Context,
    private val connectionManager: ConnectionManager,
    private val prefs: PreferencesManager
) {

    companion object {
        private const val TAG = "PresentationMode"
        private const val INACTIVITY_TIMEOUT_MS = 60000L
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
        val presentationType: PresentationType = PresentationType.UNKNOWN,
        val zoomLevel: Float = 1f,
        val presenterNotes: String = "",
        val audienceQnA: List<QnAItem> = emptyList()
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

    data class QnAItem(
        val id: String,
        val question: String,
        val answer: String = "",
        val timestamp: Long,
        val isAnswered: Boolean = false
    )

    enum class PresentationAction {
        NEXT_SLIDE, PREV_SLIDE, FIRST_SLIDE, LAST_SLIDE, GO_TO_SLIDE,
        FULLSCREEN, LASER_POINTER, HIGHLIGHT, ANNOTATE, CLEAR_ANNOTATIONS,
        START_TIMER, STOP_TIMER, BLACK_SCREEN, WHITE_SCREEN,
        PAUSE_PRESENTATION, RESUME_PRESENTATION, SHOW_NOTES, HIDE_NOTES,
        ZOOM_IN, ZOOM_OUT, RESET_ZOOM, NEXT_ANIMATION, PREV_ANIMATION,
        SHOW_QNA, ADD_QNA, ANSWER_QNA, END_PRESENTATION
    }

    enum class Tool {
        LASER, HIGHLIGHTER, PEN, ERASER, POINTER, TEXT, SHAPE
    }

    enum class PresentationType {
        UNKNOWN, POWERPOINT, GOOGLE_SLIDES, KEYNOTE, PDF, CUSTOM
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
        "swipe_down_two" to PresentationAction.ZOOM_OUT,
        "circle_cw" to PresentationAction.NEXT_ANIMATION,
        "circle_ccw" to PresentationAction.PREV_ANIMATION
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
    private var currentColor = 0xFFFF0000.toInt()
    private var currentStrokeWidth = 3f

    // Callbacks
    var onAction: ((PresentationAction, Any?) -> Unit)? = null
    var onSlideChanged: ((Int, Int, String) -> Unit)? = null
    var onLaserMoved: ((Float, Float) -> Unit)? = null
    var onAnnotationAdded: ((Annotation) -> Unit)? = null
    var onTimerTick: ((String) -> Unit)? = null
    var onQnAAdded: ((QnAItem) -> Unit)? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val customMappings = prefs.getString("presentation_gesture_mappings", "")
        if (customMappings.isNotEmpty()) {
            try {
                customMappings.split(";").forEach { mapping ->
                    val parts = mapping.split(":")
                    if (parts.size == 2) {
                        val gesture = parts[0]
                        val action = runCatching { PresentationAction.valueOf(parts[1]) }.getOrNull()
                        action?.let { gestureMappings[gesture] = it }
                    }
                }
            } catch (e: Exception) { /* Use defaults */ }
        }
    }

    private fun saveMappings() {
        val mappingsString = gestureMappings.entries.joinToString(";") { "${it.key}:${it.value.name}" }
        prefs.putString("presentation_gesture_mappings", mappingsString)
    }

    fun enablePresentationMode() {
        if (_state.value.isActive) return
        _state.value = _state.value.copy(isActive = true)
        connectionManager.sendControl("presentation_mode_start")
        resetInactivityTimer()
        requestPresentationInfo()
        android.util.Log.i(TAG, "Presentation mode enabled")
    }

    fun disablePresentationMode() {
        if (!_state.value.isActive) return
        _state.value = _state.value.copy(
            isActive = false, laserPointerActive = false,
            blackoutActive = false, whiteoutActive = false
        )
        stopTimer()
        if (laserPointerActive) disableLaserPointer()
        connectionManager.sendControl("presentation_mode_stop")
        android.util.Log.i(TAG, "Presentation mode disabled")
    }

    fun handleGesture(gesture: String): Boolean {
        if (!_state.value.isActive) return false
        val action = gestureMappings[gesture]
        action?.let {
            performAction(it)
            resetInactivityTimer()
        }
        return action != null
    }

    fun handleOrientation(roll: Float, yaw: Float) {
        if (!_state.value.isActive) return
        if (laserPointerActive) {
            val normalizedX = ((yaw + 180f) / 360f).coerceIn(0f, 1f)
            val normalizedY = ((roll + 90f) / 180f).coerceIn(0f, 1f)
            lastLaserX = lastLaserX * (1 - LASER_SMOOTHING_FACTOR) + normalizedX * LASER_SMOOTHING_FACTOR
            lastLaserY = lastLaserY * (1 - LASER_SMOOTHING_FACTOR) + normalizedY * LASER_SMOOTHING_FACTOR
            _state.value = _state.value.copy(laserX = lastLaserX, laserY = lastLaserY)
            connectionManager.send("""{"type":"laser","x":$lastLaserX,"y":$lastLaserY}""")
            onLaserMoved?.invoke(lastLaserX, lastLaserY)
        }
    }

    fun performAction(action: PresentationAction, vararg args: Any) {
        when (action) {
            PresentationAction.NEXT_SLIDE -> {
                val newSlide = _state.value.currentSlide + 1
                if (newSlide <= _state.value.totalSlides) goToSlide(newSlide)
            }
            PresentationAction.PREV_SLIDE -> {
                val newSlide = _state.value.currentSlide - 1
                if (newSlide >= 1) goToSlide(newSlide)
            }
            PresentationAction.FIRST_SLIDE -> goToSlide(1)
            PresentationAction.LAST_SLIDE -> goToSlide(_state.value.totalSlides)
            PresentationAction.GO_TO_SLIDE -> if (args.isNotEmpty() && args[0] is Int) goToSlide(args[0] as Int)
            PresentationAction.FULLSCREEN -> toggleFullscreen()
            PresentationAction.LASER_POINTER -> toggleLaserPointer()
            PresentationAction.HIGHLIGHT -> if (args.size >= 2) highlight(args[0] as Float, args[1] as Float)
            PresentationAction.ANNOTATE -> if (args.size >= 3) addAnnotation(args[0] as Float, args[1] as Float, args[2] as Int)
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
            PresentationAction.NEXT_ANIMATION -> nextAnimation()
            PresentationAction.PREV_ANIMATION -> prevAnimation()
            PresentationAction.END_PRESENTATION -> endPresentation()
            else -> {}
        }
        onAction?.invoke(action, args.firstOrNull())
    }

    private fun goToSlide(slideNumber: Int) {
        connectionManager.send("""{"type":"presentation","action":"goto","slide":$slideNumber}""")
        _state.value = _state.value.copy(currentSlide = slideNumber)
        onSlideChanged?.invoke(slideNumber, _state.value.totalSlides, _state.value.slideTitle)
    }

    private fun toggleFullscreen() {
        val newState = !_state.value.isFullscreen
        connectionManager.send("""{"type":"presentation","action":"fullscreen","enabled":$newState}""")
        _state.value = _state.value.copy(isFullscreen = newState)
    }

    private fun toggleLaserPointer() {
        laserPointerActive = !laserPointerActive
        _state.value = _state.value.copy(laserPointerActive = laserPointerActive)
        if (!laserPointerActive) {
            lastLaserX = 0.5f; lastLaserY = 0.5f
            _state.value = _state.value.copy(laserX = 0.5f, laserY = 0.5f)
        }
        connectionManager.send("""{"type":"laser","enabled":$laserPointerActive}""")
    }

    private fun disableLaserPointer() {
        laserPointerActive = false
        _state.value = _state.value.copy(laserPointerActive = false)
        connectionManager.send("""{"type":"laser","enabled":false}""")
    }

    private fun highlight(x: Float, y: Float) {
        connectionManager.send("""{"type":"presentation","action":"highlight","x":$x,"y":$y}""")
    }

    private fun addAnnotation(x: Float, y: Float, color: Int) {
        pendingAnnotation.add(Pair(x, y))
        if (pendingAnnotation.size >= 5) finalizeAnnotation(color)
    }

    fun startAnnotation(x: Float, y: Float, color: Int = currentColor) {
        pendingAnnotation.clear()
        pendingAnnotation.add(Pair(x, y))
        currentColor = color
    }

    fun continueAnnotation(x: Float, y: Float) {
        pendingAnnotation.add(Pair(x, y))
        connectionManager.send("""{"type":"annotation","action":"draw","x":$x,"y":$y}""")
    }

    fun endAnnotation(strokeWidth: Float = currentStrokeWidth) {
        finalizeAnnotation(currentColor, strokeWidth)
    }

    fun setAnnotationColor(color: Int) {
        currentColor = color
        connectionManager.send("""{"type":"annotation","action":"setColor","color":$color}""")
    }

    fun setAnnotationStrokeWidth(width: Float) {
        currentStrokeWidth = width
        connectionManager.send("""{"type":"annotation","action":"setStrokeWidth","width":$width}""")
    }

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
        _state.value = _state.value.copy(annotations = _state.value.annotations + annotation)
        val pointsJson = pendingAnnotation.joinToString(",") { (x, y) -> "{\"x\":$x,\"y\":$y}" }
        connectionManager.send("""{"type":"annotation","action":"add","color":$color,"strokeWidth":$strokeWidth,"points":[$pointsJson]}""")
        onAnnotationAdded?.invoke(annotation)
        pendingAnnotation.clear()
    }

    private fun clearAnnotations() {
        _state.value = _state.value.copy(annotations = emptyList())
        connectionManager.send("""{"type":"annotation","action":"clear"}""")
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        timerStartTime = System.currentTimeMillis() - _state.value.elapsedTime
        timerJob = scope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - timerStartTime
                _state.value = _state.value.copy(timerRunning = true, elapsedTime = elapsed)
                onTimerTick?.invoke(getFormattedElapsedTime())
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _state.value = _state.value.copy(timerRunning = false)
    }

    fun resetTimer() { stopTimer(); _state.value = _state.value.copy(elapsedTime = 0); startTimer() }

    fun getFormattedElapsedTime(): String {
        val seconds = (_state.value.elapsedTime / 1000) % 60
        val minutes = (_state.value.elapsedTime / (1000 * 60)) % 60
        val hours = (_state.value.elapsedTime / (1000 * 60 * 60))
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
               else String.format("%02d:%02d", minutes, seconds)
    }

    private fun activateBlackScreen() {
        if (_state.value.whiteoutActive) _state.value = _state.value.copy(whiteoutActive = false)
        _state.value = _state.value.copy(blackoutActive = !_state.value.blackoutActive)
        connectionManager.send("""{"type":"presentation","action":"blackscreen","enabled":${_state.value.blackoutActive}}""")
    }

    private fun activateWhiteScreen() {
        if (_state.value.blackoutActive) _state.value = _state.value.copy(blackoutActive = false)
        _state.value = _state.value.copy(whiteoutActive = !_state.value.whiteoutActive)
        connectionManager.send("""{"type":"presentation","action":"whitescreen","enabled":${_state.value.whiteoutActive}}""")
    }

    fun clearScreenOverlay() {
        if (_state.value.blackoutActive || _state.value.whiteoutActive) {
            _state.value = _state.value.copy(blackoutActive = false, whiteoutActive = false)
            connectionManager.send("""{"type":"presentation","action":"clearoverlay"}""")
        }
    }

    private fun pausePresentation() {
        connectionManager.send("""{"type":"presentation","action":"pause"}""")
        stopTimer()
    }

    private fun resumePresentation() {
        connectionManager.send("""{"type":"presentation","action":"resume"}""")
        if (_state.value.timerRunning) startTimer()
    }

    private fun showNotes() {
        _state.value = _state.value.copy(pointerVisible = true)
        connectionManager.send("""{"type":"presentation","action":"shownotes"}""")
    }

    private fun hideNotes() {
        _state.value = _state.value.copy(pointerVisible = false)
        connectionManager.send("""{"type":"presentation","action":"hidenotes"}""")
    }

    private fun zoom(zoomIn: Boolean) {
        val newZoom = if (zoomIn) _state.value.zoomLevel * 1.2f else _state.value.zoomLevel / 1.2f
        _state.value = _state.value.copy(zoomLevel = newZoom.coerceIn(0.5f, 3f))
        connectionManager.send("""{"type":"presentation","action":"zoom","level":${_state.value.zoomLevel}}""")
    }

    private fun resetZoom() {
        _state.value = _state.value.copy(zoomLevel = 1f)
        connectionManager.send("""{"type":"presentation","action":"zoomreset"}""")
    }

    private fun nextAnimation() {
        connectionManager.send("""{"type":"presentation","action":"nextanimation"}""")
    }

    private fun prevAnimation() {
        connectionManager.send("""{"type":"presentation","action":"prevanimation"}""")
    }

    fun addQnA(question: String) {
        val qna = QnAItem(
            id = System.currentTimeMillis().toString(),
            question = question,
            timestamp = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(audienceQnA = _state.value.audienceQnA + qna)
        connectionManager.send("""{"type":"presentation","action":"addqna","question":"${question.replace("\"", "\\\"")}"}""")
        onQnAAdded?.invoke(qna)
    }

    fun answerQnA(qnaId: String, answer: String) {
        val updated = _state.value.audienceQnA.map {
            if (it.id == qnaId) it.copy(answer = answer, isAnswered = true) else it
        }
        _state.value = _state.value.copy(audienceQnA = updated)
        connectionManager.send("""{"type":"presentation","action":"answerqna","id":"$qnaId","answer":"${answer.replace("\"", "\\\"")}"}""")
    }

    private fun endPresentation() {
        connectionManager.send("""{"type":"presentation","action":"end"}""")
        disablePresentationMode()
        _state.value = PresentationState()
    }

    fun setTool(tool: Tool) {
        _state.value = _state.value.copy(currentTool = tool)
        if (tool != Tool.LASER && laserPointerActive) toggleLaserPointer()
        connectionManager.send("""{"type":"presentation","action":"setTool","tool":"${tool.name.lowercase()}"}""")
    }

    fun setTotalSlides(total: Int) { _state.value = _state.value.copy(totalSlides = total) }

    fun updateSlideInfo(current: Int, title: String = "", notes: String = "") {
        _state.value = _state.value.copy(currentSlide = current, slideTitle = title, slideNotes = notes)
        onSlideChanged?.invoke(current, _state.value.totalSlides, title)
    }

    fun updatePresentationType(type: PresentationType) { _state.value = _state.value.copy(presentationType = type) }

    private fun requestPresentationInfo() { connectionManager.send("""{"type":"presentation","action":"info"}""") }

    fun customizeGesture(gesture: String, action: PresentationAction) {
        gestureMappings[gesture] = action
        saveMappings()
    }

    fun removeGestureMapping(gesture: String) { gestureMappings.remove(gesture); saveMappings() }
    fun getGestureMapping(gesture: String): PresentationAction? = gestureMappings[gesture]
    fun getAllGestureMappings(): Map<String, PresentationAction> = gestureMappings.toMap()

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
            "swipe_down_two" to PresentationAction.ZOOM_OUT,
            "circle_cw" to PresentationAction.NEXT_ANIMATION,
            "circle_ccw" to PresentationAction.PREV_ANIMATION
        )
        saveMappings()
    }

    private fun resetInactivityTimer() {
        inactivityTimer?.let { handler.removeCallbacks(it) }
        inactivityTimer = Runnable { if (_state.value.isActive && laserPointerActive) disableLaserPointer() }
        handler.postDelayed(inactivityTimer, INACTIVITY_TIMEOUT_MS)
    }

    fun isActive(): Boolean = _state.value.isActive
    fun getCurrentSlide(): Int = _state.value.currentSlide
    fun getTotalSlides(): Int = _state.value.totalSlides
    fun getCurrentTool(): Tool = _state.value.currentTool

    fun clearState() {
        disablePresentationMode()
        clearAnnotations()
        _state.value = PresentationState()
    }

    fun cleanup() {
        disablePresentationMode()
        timerJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
    }
}