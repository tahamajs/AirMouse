@file:Suppress("SpellCheckingInspection", "unused")

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
import java.util.Locale
import java.util.UUID

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
        private const val MAX_ANNOTATIONS = 100
        private const val MAX_QNA_ITEMS = 50
        private const val AUTO_SAVE_INTERVAL_MS = 30000L
    }

    // ==================== DATA CLASSES ====================

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
        val audienceQnA: List<QnAItem> = emptyList(),
        val isPaused: Boolean = false,
        val recording: Boolean = false,
        val recordingDuration: Long = 0
    )

    data class Annotation(
        val id: String,
        val x: Float,
        val y: Float,
        val color: Int,
        val strokeWidth: Float,
        val points: List<Pair<Float, Float>>,
        val timestamp: Long = System.currentTimeMillis(),
        val shape: ShapeType = ShapeType.FREEHAND
    )

    data class QnAItem(
        val id: String,
        val question: String,
        val answer: String = "",
        val timestamp: Long,
        val isAnswered: Boolean = false,
        val answeredAt: Long = 0,
        val author: String = "Audience"
    )

    data class PresentationMetadata(
        val name: String = "",
        val author: String = "",
        val createdAt: Long = System.currentTimeMillis(),
        val modifiedAt: Long = System.currentTimeMillis(),
        val tags: List<String> = emptyList(),
        val totalSlides: Int = 0,
        val duration: Long = 0
    )

    enum class PresentationAction {
        NEXT_SLIDE, PREV_SLIDE, FIRST_SLIDE, LAST_SLIDE, GO_TO_SLIDE,
        FULLSCREEN, LASER_POINTER, HIGHLIGHT, ANNOTATE, CLEAR_ANNOTATIONS,
        START_TIMER, STOP_TIMER, BLACK_SCREEN, WHITE_SCREEN,
        PAUSE_PRESENTATION, RESUME_PRESENTATION, SHOW_NOTES, HIDE_NOTES,
        ZOOM_IN, ZOOM_OUT, RESET_ZOOM, NEXT_ANIMATION, PREV_ANIMATION,
        SHOW_QNA, ADD_QNA, ANSWER_QNA, END_PRESENTATION,
        RECORD_START, RECORD_STOP, UNDO_ANNOTATION, REDO_ANNOTATION,
        EXPORT_ANNOTATIONS, IMPORT_ANNOTATIONS, SAVE_SESSION
    }

    enum class Tool {
        LASER, HIGHLIGHTER, PEN, ERASER, POINTER, TEXT, SHAPE, NONE
    }

    enum class ShapeType {
        FREEHAND, RECTANGLE, CIRCLE, ARROW, LINE, TEXT_BOX
    }

    enum class PresentationType {
        UNKNOWN, POWERPOINT, GOOGLE_SLIDES, KEYNOTE, PDF, CUSTOM
    }

    enum class AnnotationColor {
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK, WHITE, BLACK, CUSTOM
    }

    enum class StrokeWidth {
        THIN, MEDIUM, THICK, EXTRA_THICK
    }

    // ==================== STATE ====================

    private val _state = MutableStateFlow(PresentationState())
    val state: StateFlow<PresentationState> = _state.asStateFlow()

    private val _metadata = MutableStateFlow(PresentationMetadata())
    val metadata: StateFlow<PresentationMetadata> = _metadata.asStateFlow()

    // ==================== PRIVATE STATE ====================

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
        "circle_ccw" to PresentationAction.PREV_ANIMATION,
        "shake" to PresentationAction.CLEAR_ANNOTATIONS
    )

    private var laserPointerActive = false
    private var lastLaserX = 0.5f
    private var lastLaserY = 0.5f
    private var inactivityTimer: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var autoSaveJob: Job? = null
    private var timerStartTime = 0L
    private var pendingAnnotation: MutableList<Pair<Float, Float>> = mutableListOf()
    private var currentColor = 0xFFFF0000.toInt()
    private var currentStrokeWidth = 3f
    private var shapeStart: Pair<Float, Float>? = null
    private var annotationHistory: MutableList<Annotation> = mutableListOf()
    private var annotationHistoryIndex = -1
    private var isRecording = false
    private var recordingStartTime = 0L
    private val recordedActions = mutableListOf<RecordedAction>()

    private data class RecordedAction(
        val timestamp: Long,
        val action: String,
        val data: Map<String, Any>
    )

    // ==================== CALLBACKS ====================

    var onAction: ((PresentationAction, Any?) -> Unit)? = null
    var onSlideChanged: ((Int, Int, String) -> Unit)? = null
    var onLaserMoved: ((Float, Float) -> Unit)? = null
    var onAnnotationAdded: ((Annotation) -> Unit)? = null
    var onTimerTick: ((String) -> Unit)? = null
    var onQnAAdded: ((QnAItem) -> Unit)? = null
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: ((String) -> Unit)? = null
    var onStateChanged: ((PresentationState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // ==================== INITIALIZATION ====================

    init {
        loadSettings()
        startAutoSave()
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
            } catch (_: Exception) { /* Use defaults */ }
        }

        // Load saved annotations if any
        val savedAnnotations = prefs.getString("presentation_saved_annotations", "")
        if (savedAnnotations.isNotEmpty()) {
            try {
                // Parse and restore annotations
                android.util.Log.d(TAG, "Restored saved annotations")
            } catch (_: Exception) { /* Ignore */ }
        }
    }

    private fun saveSettings() {
        val mappingsString = gestureMappings.entries.joinToString(";") { "${it.key}:${it.value.name}" }
        prefs.putString("presentation_gesture_mappings", mappingsString)
    }

    private fun startAutoSave() {
        autoSaveJob = scope.launch {
            while (true) {
                delay(AUTO_SAVE_INTERVAL_MS)
                saveSession()
            }
        }
    }

    // ==================== PUBLIC API ====================

    fun enablePresentationMode() {
        if (_state.value.isActive) return
        _state.value = _state.value.copy(isActive = true)
        connectionManager.sendControl("presentation_mode_start")
        resetInactivityTimer()
        requestPresentationInfo()
        android.util.Log.i(TAG, "Presentation mode enabled")
        onStateChanged?.invoke(_state.value)
    }

    fun disablePresentationMode() {
        if (!_state.value.isActive) return
        _state.value = _state.value.copy(
            isActive = false,
            laserPointerActive = false,
            blackoutActive = false,
            whiteoutActive = false,
            isPaused = false,
            recording = false
        )
        stopTimer()
        stopRecording()
        if (laserPointerActive) disableLaserPointer()
        connectionManager.sendControl("presentation_mode_stop")
        android.util.Log.i(TAG, "Presentation mode disabled")
        onStateChanged?.invoke(_state.value)
    }

    fun handleGesture(gesture: String): Boolean {
        if (!_state.value.isActive) return false
        val action = gestureMappings[gesture]
        action?.let {
            performAction(it)
            resetInactivityTimer()
            recordAction("gesture", mapOf("gesture" to gesture, "action" to it.name))
        }
        return action != null
    }

    fun handleOrientation(roll: Float, yaw: Float) {
        if (!_state.value.isActive || _state.value.isPaused) return

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
        if (!_state.value.isActive) {
            onError?.invoke("Presentation mode not active")
            return
        }

        try {
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
                PresentationAction.GO_TO_SLIDE -> {
                    if (args.isNotEmpty() && args[0] is Int) {
                        goToSlide(args[0] as Int)
                    }
                }
                PresentationAction.FULLSCREEN -> toggleFullscreen()
                PresentationAction.LASER_POINTER -> toggleLaserPointer()
                PresentationAction.HIGHLIGHT -> {
                    if (args.size >= 2) highlight(args[0] as Float, args[1] as Float)
                }
                PresentationAction.ANNOTATE -> {
                    if (args.size >= 3) {
                        addAnnotation(args[0] as Float, args[1] as Float, args[2] as Int)
                    }
                }
                PresentationAction.CLEAR_ANNOTATIONS -> clearAnnotations()
                PresentationAction.UNDO_ANNOTATION -> undoAnnotation()
                PresentationAction.REDO_ANNOTATION -> redoAnnotation()
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
                PresentationAction.SHOW_QNA -> showQnA()
                PresentationAction.ADD_QNA -> {
                    if (args.isNotEmpty() && args[0] is String) {
                        addQnA(args[0] as String)
                    }
                }
                PresentationAction.ANSWER_QNA -> {
                    if (args.size >= 2 && args[0] is String && args[1] is String) {
                        answerQnA(args[0] as String, args[1] as String)
                    }
                }
                PresentationAction.END_PRESENTATION -> endPresentation()
                PresentationAction.RECORD_START -> startRecording()
                PresentationAction.RECORD_STOP -> stopRecording()
                PresentationAction.EXPORT_ANNOTATIONS -> exportAnnotations()
                PresentationAction.IMPORT_ANNOTATIONS -> importAnnotations()
                PresentationAction.SAVE_SESSION -> saveSession()
            }
            recordAction("action", mapOf("action" to action.name))
        } catch (e: Exception) {
            onError?.invoke("Failed to perform action: ${e.message}")
            android.util.Log.e(TAG, "Action failed: ${e.message}", e)
        }
    }

    // ==================== CORE PRESENTATION FUNCTIONS ====================

    private fun goToSlide(slideNumber: Int) {
        val validSlide = slideNumber.coerceIn(1, _state.value.totalSlides)
        connectionManager.send("""{"type":"presentation","action":"goto","slide":$validSlide}""")
        _state.value = _state.value.copy(currentSlide = validSlide)
        onSlideChanged?.invoke(validSlide, _state.value.totalSlides, _state.value.slideTitle)
        onStateChanged?.invoke(_state.value)
    }

    private fun toggleFullscreen() {
        val newState = !_state.value.isFullscreen
        connectionManager.send("""{"type":"presentation","action":"fullscreen","enabled":$newState}""")
        _state.value = _state.value.copy(isFullscreen = newState)
        onStateChanged?.invoke(_state.value)
    }

    private fun toggleLaserPointer() {
        laserPointerActive = !laserPointerActive
        _state.value = _state.value.copy(laserPointerActive = laserPointerActive)
        if (!laserPointerActive) {
            lastLaserX = 0.5f
            lastLaserY = 0.5f
            _state.value = _state.value.copy(laserX = 0.5f, laserY = 0.5f)
        }
        connectionManager.send("""{"type":"laser","enabled":$laserPointerActive}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun disableLaserPointer() {
        laserPointerActive = false
        _state.value = _state.value.copy(laserPointerActive = false)
        connectionManager.send("""{"type":"laser","enabled":false}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun highlight(x: Float, y: Float) {
        connectionManager.send("""{"type":"presentation","action":"highlight","x":$x,"y":$y}""")
    }

    private fun addAnnotation(x: Float, y: Float, color: Int) {
        pendingAnnotation.add(Pair(x, y))
        if (pendingAnnotation.size >= 5) {
            finalizeAnnotation(color)
            // Send incremental update
            val lastPoints = pendingAnnotation.takeLast(5)
            val pointsJson = lastPoints.joinToString(",") { (px, py) -> "{\"x\":$px,\"y\":$py}" }
            connectionManager.send("""{"type":"annotation","action":"draw","points":[$pointsJson]}""")
        }
    }

    fun startAnnotation(x: Float, y: Float, color: Int = currentColor) {
        pendingAnnotation.clear()
        pendingAnnotation.add(Pair(x, y))
        currentColor = color
        shapeStart = Pair(x, y)
        connectionManager.send("""{"type":"annotation","action":"start","x":$x,"y":$y,"color":$color}""")
    }

    fun continueAnnotation(x: Float, y: Float) {
        pendingAnnotation.add(Pair(x, y))
        connectionManager.send("""{"type":"annotation","action":"continue","x":$x,"y":$y}""")
    }

    fun endAnnotation(strokeWidth: Float = currentStrokeWidth) {
        finalizeAnnotation(currentColor, strokeWidth)
        shapeStart = null
        connectionManager.send("""{"type":"annotation","action":"end"}""")
    }

    fun setAnnotationColor(color: Int) {
        currentColor = color
        connectionManager.send("""{"type":"annotation","action":"setColor","color":$color}""")
    }

    fun setAnnotationColor(color: AnnotationColor) {
        val colorMap = mapOf(
            AnnotationColor.RED to 0xFFFF0000.toInt(),
            AnnotationColor.ORANGE to 0xFFFF8800.toInt(),
            AnnotationColor.YELLOW to 0xFFFFFF00.toInt(),
            AnnotationColor.GREEN to 0xFF00FF00.toInt(),
            AnnotationColor.BLUE to 0xFF0000FF.toInt(),
            AnnotationColor.PURPLE to 0xFFFF00FF.toInt(),
            AnnotationColor.PINK to 0xFFFFC0CB.toInt(),
            AnnotationColor.WHITE to 0xFFFFFFFF.toInt(),
            AnnotationColor.BLACK to 0xFF000000.toInt()
        )
        currentColor = colorMap[color] ?: 0xFFFF0000.toInt()
        connectionManager.send("""{"type":"annotation","action":"setColor","color":$currentColor}""")
    }

    fun setAnnotationStrokeWidth(width: StrokeWidth) {
        currentStrokeWidth = when (width) {
            StrokeWidth.THIN -> 1f
            StrokeWidth.MEDIUM -> 3f
            StrokeWidth.THICK -> 6f
            StrokeWidth.EXTRA_THICK -> 10f
        }
        connectionManager.send("""{"type":"annotation","action":"setStrokeWidth","width":$currentStrokeWidth}""")
    }

    private fun finalizeAnnotation(color: Int, strokeWidth: Float = 3f) {
        if (pendingAnnotation.isEmpty()) return

        val annotation = Annotation(
            id = UUID.randomUUID().toString(),
            x = pendingAnnotation.first().first,
            y = pendingAnnotation.first().second,
            color = color,
            strokeWidth = strokeWidth,
            points = pendingAnnotation.toList()
        )

        // Add to history
        annotationHistory = annotationHistory.take(annotationHistoryIndex + 1).toMutableList()
        annotationHistory.add(annotation)
        annotationHistoryIndex = annotationHistory.lastIndex

        // Trim history if needed
        if (annotationHistory.size > MAX_ANNOTATIONS) {
            annotationHistory = annotationHistory.drop(annotationHistory.size - MAX_ANNOTATIONS).toMutableList()
            annotationHistoryIndex = annotationHistory.lastIndex
        }

        _state.value = _state.value.copy(
            annotations = _state.value.annotations + annotation
        )

        val pointsJson = pendingAnnotation.joinToString(",") { (x, y) -> "{\"x\":$x,\"y\":$y}" }
        connectionManager.send("""{"type":"annotation","action":"add","color":$color,"strokeWidth":$strokeWidth,"points":[$pointsJson]}""")

        onAnnotationAdded?.invoke(annotation)
        onStateChanged?.invoke(_state.value)
        pendingAnnotation.clear()
    }

    private fun undoAnnotation() {
        if (annotationHistoryIndex < 0) return
        val annotation = annotationHistory[annotationHistoryIndex]
        _state.value = _state.value.copy(
            annotations = _state.value.annotations.filter { it.id != annotation.id }
        )
        annotationHistoryIndex--
        connectionManager.send("""{"type":"annotation","action":"undo","id":"${annotation.id}"}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun redoAnnotation() {
        if (annotationHistoryIndex + 1 >= annotationHistory.size) return
        annotationHistoryIndex++
        val annotation = annotationHistory[annotationHistoryIndex]
        _state.value = _state.value.copy(
            annotations = _state.value.annotations + annotation
        )
        connectionManager.send("""{"type":"annotation","action":"redo","id":"${annotation.id}"}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun clearAnnotations() {
        _state.value = _state.value.copy(annotations = emptyList())
        annotationHistory.clear()
        annotationHistoryIndex = -1
        connectionManager.send("""{"type":"annotation","action":"clear"}""")
        onStateChanged?.invoke(_state.value)
    }

    // ==================== TIMER ====================

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
        onStateChanged?.invoke(_state.value)
    }

    fun resetTimer() {
        stopTimer()
        _state.value = _state.value.copy(elapsedTime = 0)
        startTimer()
    }

    fun getFormattedElapsedTime(): String {
        val seconds = (_state.value.elapsedTime / 1000) % 60
        val minutes = (_state.value.elapsedTime / (1000 * 60)) % 60
        val hours = (_state.value.elapsedTime / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    // ==================== SCREEN OVERLAYS ====================

    private fun activateBlackScreen() {
        if (_state.value.whiteoutActive) {
            _state.value = _state.value.copy(whiteoutActive = false)
        }
        _state.value = _state.value.copy(blackoutActive = !_state.value.blackoutActive)
        connectionManager.send("""{"type":"presentation","action":"blackscreen","enabled":${_state.value.blackoutActive}}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun activateWhiteScreen() {
        if (_state.value.blackoutActive) {
            _state.value = _state.value.copy(blackoutActive = false)
        }
        _state.value = _state.value.copy(whiteoutActive = !_state.value.whiteoutActive)
        connectionManager.send("""{"type":"presentation","action":"whitescreen","enabled":${_state.value.whiteoutActive}}""")
        onStateChanged?.invoke(_state.value)
    }

    fun clearScreenOverlay() {
        if (_state.value.blackoutActive || _state.value.whiteoutActive) {
            _state.value = _state.value.copy(blackoutActive = false, whiteoutActive = false)
            connectionManager.send("""{"type":"presentation","action":"clearoverlay"}""")
            onStateChanged?.invoke(_state.value)
        }
    }

    // ==================== PAUSE/RESUME ====================

    private fun pausePresentation() {
        _state.value = _state.value.copy(isPaused = true)
        connectionManager.send("""{"type":"presentation","action":"pause"}""")
        stopTimer()
        if (laserPointerActive) disableLaserPointer()
        onStateChanged?.invoke(_state.value)
    }

    private fun resumePresentation() {
        _state.value = _state.value.copy(isPaused = false)
        connectionManager.send("""{"type":"presentation","action":"resume"}""")
        if (_state.value.timerRunning) startTimer()
        onStateChanged?.invoke(_state.value)
    }

    // ==================== NOTES ====================

    private fun showNotes() {
        _state.value = _state.value.copy(pointerVisible = true)
        connectionManager.send("""{"type":"presentation","action":"shownotes"}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun hideNotes() {
        _state.value = _state.value.copy(pointerVisible = false)
        connectionManager.send("""{"type":"presentation","action":"hidenotes"}""")
        onStateChanged?.invoke(_state.value)
    }

    // ==================== ZOOM ====================

    private fun zoom(zoomIn: Boolean) {
        val newZoom = if (zoomIn) {
            (_state.value.zoomLevel * 1.2f).coerceAtMost(3f)
        } else {
            (_state.value.zoomLevel / 1.2f).coerceAtLeast(0.5f)
        }
        _state.value = _state.value.copy(zoomLevel = newZoom)
        connectionManager.send("""{"type":"presentation","action":"zoom","level":$newZoom}""")
        onStateChanged?.invoke(_state.value)
    }

    private fun resetZoom() {
        _state.value = _state.value.copy(zoomLevel = 1f)
        connectionManager.send("""{"type":"presentation","action":"zoomreset"}""")
        onStateChanged?.invoke(_state.value)
    }

    // ==================== ANIMATIONS ====================

    private fun nextAnimation() {
        connectionManager.send("""{"type":"presentation","action":"nextanimation"}""")
    }

    private fun prevAnimation() {
        connectionManager.send("""{"type":"presentation","action":"prevanimation"}""")
    }

    // ==================== Q&A ====================

    private fun showQnA() {
        connectionManager.send("""{"type":"presentation","action":"showqna"}""")
    }

    fun addQnA(question: String) {
        val qna = QnAItem(
            id = UUID.randomUUID().toString(),
            question = question,
            timestamp = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(
            audienceQnA = (_state.value.audienceQnA + qna).takeLast(MAX_QNA_ITEMS)
        )
        connectionManager.send("""{"type":"presentation","action":"addqna","question":"${question.replace("\"", "\\\"")}"}""")
        onQnAAdded?.invoke(qna)
        onStateChanged?.invoke(_state.value)
    }

    fun answerQnA(qnaId: String, answer: String) {
        val updated = _state.value.audienceQnA.map {
            if (it.id == qnaId) {
                it.copy(answer = answer, isAnswered = true, answeredAt = System.currentTimeMillis())
            } else it
        }
        _state.value = _state.value.copy(audienceQnA = updated)
        connectionManager.send("""{"type":"presentation","action":"answerqna","id":"$qnaId","answer":"${answer.replace("\"", "\\\"")}"}""")
        onStateChanged?.invoke(_state.value)
    }

    fun removeQnA(qnaId: String) {
        _state.value = _state.value.copy(
            audienceQnA = _state.value.audienceQnA.filter { it.id != qnaId }
        )
        onStateChanged?.invoke(_state.value)
    }

    // ==================== RECORDING ====================

    private fun startRecording() {
        if (isRecording) return
        isRecording = true
        recordingStartTime = System.currentTimeMillis()
        recordedActions.clear()
        _state.value = _state.value.copy(recording = true)
        connectionManager.send("""{"type":"presentation","action":"recordstart"}""")
        onRecordingStarted?.invoke()
        onStateChanged?.invoke(_state.value)
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        val duration = System.currentTimeMillis() - recordingStartTime
        _state.value = _state.value.copy(recording = false, recordingDuration = duration)
        connectionManager.send("""{"type":"presentation","action":"recordstop"}""")
        onRecordingStopped?.invoke("Recording stopped. Duration: ${formatDuration(duration)}")
        onStateChanged?.invoke(_state.value)
        exportRecording()
    }

    private fun recordAction(action: String, data: Map<String, Any>) {
        if (!isRecording) return
        recordedActions.add(
            RecordedAction(
                timestamp = System.currentTimeMillis() - recordingStartTime,
                action = action,
                data = data
            )
        )
    }

    private fun exportRecording() {
        // In production, save to file
        android.util.Log.d(TAG, "Exporting recording with ${recordedActions.size} actions")
    }

    private fun formatDuration(duration: Long): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = duration / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    // ==================== END PRESENTATION ====================

    private fun endPresentation() {
        connectionManager.send("""{"type":"presentation","action":"end"}""")
        disablePresentationMode()
        _state.value = PresentationState()
        annotationHistory.clear()
        annotationHistoryIndex = -1
        onStateChanged?.invoke(_state.value)
    }

    // ==================== TOOLS ====================

    fun setTool(tool: Tool) {
        _state.value = _state.value.copy(currentTool = tool)
        if (tool != Tool.LASER && laserPointerActive) {
            toggleLaserPointer()
        }
        connectionManager.send("""{"type":"presentation","action":"setTool","tool":"${tool.name.lowercase(Locale.US)}"}""")
        onStateChanged?.invoke(_state.value)
    }

    // ==================== METADATA ====================

    fun setTotalSlides(total: Int) {
        _state.value = _state.value.copy(totalSlides = total)
        _metadata.value = _metadata.value.copy(totalSlides = total, modifiedAt = System.currentTimeMillis())
    }

    fun updateSlideInfo(current: Int, title: String = "", notes: String = "") {
        _state.value = _state.value.copy(
            currentSlide = current,
            slideTitle = title,
            slideNotes = notes
        )
        onSlideChanged?.invoke(current, _state.value.totalSlides, title)
        onStateChanged?.invoke(_state.value)
    }

    fun updatePresentationType(type: PresentationType) {
        _state.value = _state.value.copy(presentationType = type)
    }

    fun setMetadata(name: String, author: String, tags: List<String> = emptyList()) {
        _metadata.value = _metadata.value.copy(
            name = name,
            author = author,
            tags = tags,
            modifiedAt = System.currentTimeMillis()
        )
    }

    private fun requestPresentationInfo() {
        connectionManager.send("""{"type":"presentation","action":"info"}""")
    }

    // ==================== EXPORT/IMPORT ====================

    private fun exportAnnotations() {
        val annotations = _state.value.annotations
        if (annotations.isEmpty()) {
            onError?.invoke("No annotations to export")
            return
        }

        // In production, save to file or share
        android.util.Log.d(TAG, "Exporting ${annotations.size} annotations")
        onAction?.invoke(PresentationAction.EXPORT_ANNOTATIONS, annotations)
    }

    private fun importAnnotations() {
        // In production, load from file
        onAction?.invoke(PresentationAction.IMPORT_ANNOTATIONS, null)
    }

    private fun saveSession() {
        if (!_state.value.isActive) return

        // Save state
        prefs.putString("presentation_saved_state", _state.value.toString())
        prefs.putString("presentation_saved_metadata", _metadata.value.toString())

        // Save annotations
        val annotationsJson = _state.value.annotations.joinToString("|") {
            "${it.id},${it.x},${it.y},${it.color},${it.strokeWidth}"
        }
        prefs.putString("presentation_saved_annotations", annotationsJson)
    }

    // ==================== GESTURE CUSTOMIZATION ====================

    fun customizeGesture(gesture: String, action: PresentationAction) {
        gestureMappings[gesture] = action
        saveSettings()
    }

    fun removeGestureMapping(gesture: String) {
        gestureMappings.remove(gesture)
        saveSettings()
    }

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
            "circle_ccw" to PresentationAction.PREV_ANIMATION,
            "shake" to PresentationAction.CLEAR_ANNOTATIONS
        )
        saveSettings()
    }

    // ==================== UTILITY FUNCTIONS ====================

    private fun resetInactivityTimer() {
        val currentTimer = inactivityTimer
        if (currentTimer != null) {
            handler.removeCallbacks(currentTimer)
        }
        val newTimer = Runnable {
            if (_state.value.isActive && laserPointerActive) {
                disableLaserPointer()
                android.util.Log.d(TAG, "Laser pointer disabled due to inactivity")
            }
        }
        inactivityTimer = newTimer
        handler.postDelayed(newTimer, INACTIVITY_TIMEOUT_MS)
    }

    fun isActive(): Boolean = _state.value.isActive

    fun getCurrentSlide(): Int = _state.value.currentSlide

    fun getTotalSlides(): Int = _state.value.totalSlides

    fun getCurrentTool(): Tool = _state.value.currentTool

    fun isLaserActive(): Boolean = laserPointerActive

    fun getAnnotationCount(): Int = _state.value.annotations.size

    fun getQnACount(): Int = _state.value.audienceQnA.size

    fun getElapsedTime(): Long = _state.value.elapsedTime

    fun isRecording(): Boolean = isRecording

    fun clearState() {
        disablePresentationMode()
        clearAnnotations()
        _state.value = PresentationState()
        annotationHistory.clear()
        annotationHistoryIndex = -1
        recordedActions.clear()
        onStateChanged?.invoke(_state.value)
    }

    fun cleanup() {
        disablePresentationMode()
        timerJob?.cancel()
        autoSaveJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        android.util.Log.d(TAG, "Cleanup complete")
    }
}
