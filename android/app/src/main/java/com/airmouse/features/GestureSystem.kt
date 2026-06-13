// GestureSystem.kt
package com.airmouse.features

import android.content.Context
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Complete gesture system: custom air gesture recording, recognition (DTW),
 * multi‑touch gestures, and mapping to system actions.
 */
@Singleton
class GestureSystem @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) {

    // ==================== Data models ====================
    data class MotionPoint(
        val timestamp: Long,
        val gyroX: Float, val gyroY: Float, val gyroZ: Float,
        val accelX: Float, val accelY: Float, val accelZ: Float
    )

    data class CustomGesture(
        val id: String,
        val name: String,
        val pattern: List<MotionPoint>,
        val action: GestureAction,
        val sensitivity: Float = 0.7f
    ) {
        // Normalized feature vector for DTW (e.g., magnitude of gyro and accel)
        fun extractFeatures(): List<Float> {
            return pattern.map { point ->
                sqrt(point.gyroX*point.gyroX + point.gyroY*point.gyroY + point.gyroZ*point.gyroZ) +
                        sqrt(point.accelX*point.accelX + point.accelY*point.accelY + point.accelZ*point.accelZ)
            }
        }
    }

    data class GestureAction(
        val type: ActionType,
        val parameters: Map<String, Any> = emptyMap()
    )

    enum class ActionType {
        CLICK, RIGHT_CLICK, DOUBLE_CLICK,
        SCROLL_UP, SCROLL_DOWN,
        VOLUME_UP, VOLUME_DOWN,
        MEDIA_PREV, MEDIA_NEXT, MEDIA_PLAY_PAUSE,
        BRIGHTNESS_UP, BRIGHTNESS_DOWN,
        KEYBOARD_SHORTCUT,
        CUSTOM_COMMAND
    }

    enum class MultiTouchGesture {
        TWO_FINGER_SCROLL,
        THREE_FINGER_SWIPE_UP,
        THREE_FINGER_SWIPE_DOWN,
        THREE_FINGER_SWIPE_LEFT,
        THREE_FINGER_SWIPE_RIGHT,
        FOUR_FINGER_TAP,
        PINCH_TO_ZOOM,
        ROTATE_TWO_FINGERS
    }

    enum class AirGesture {
        CIRCLE_CW, CIRCLE_CCW, FIGURE_EIGHT, ZIG_ZAG, CHECK_MARK, CROSS
    }

    // ==================== State ====================
    private val _customGestures = MutableStateFlow<List<CustomGesture>>(emptyList())
    val customGestures: StateFlow<List<CustomGesture>> = _customGestures.asStateFlow()

    private val _recording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _recording.asStateFlow()

    private var recordingPoints = mutableListOf<MotionPoint>()
    private var recordingStartTime = 0L

    private var pendingGestureAction: GestureAction? = null
    private val recognizerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val gestureTemplates = mutableMapOf<String, List<Float>>() // name -> feature vector

    init {
        loadCustomGestures()
    }

    // ==================== Persistence ====================
    private fun loadCustomGestures() {
        val jsonStr = prefs.getString("custom_gestures", "")
        if (jsonStr.isEmpty()) return
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomGesture>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(parseGesture(obj))
            }
            _customGestures.value = list
            rebuildTemplates()
        } catch (e: Exception) { /* ignore */ }
    }

    private fun saveCustomGestures() {
        val array = JSONArray()
        _customGestures.value.forEach { gesture ->
            array.put(serializeGesture(gesture))
        }
        prefs.putString("custom_gestures", array.toString())
    }

    private fun serializeGesture(g: CustomGesture): JSONObject {
        return JSONObject().apply {
            put("id", g.id)
            put("name", g.name)
            put("action", g.action.type.name)
            if (g.action.parameters.isNotEmpty()) {
                put("params", JSONObject(g.action.parameters))
            }
            put("sensitivity", g.sensitivity)
            put("pattern", JSONArray().apply {
                g.pattern.forEach { point ->
                    put(JSONObject().apply {
                        put("t", point.timestamp)
                        put("gx", point.gyroX); put("gy", point.gyroY); put("gz", point.gyroZ)
                        put("ax", point.accelX); put("ay", point.accelY); put("az", point.accelZ)
                    })
                }
            })
        }
    }

    private fun parseGesture(obj: JSONObject): CustomGesture {
        val patternArray = obj.getJSONArray("pattern")
        val pattern = mutableListOf<MotionPoint>()
        for (i in 0 until patternArray.length()) {
            val p = patternArray.getJSONObject(i)
            pattern.add(MotionPoint(
                timestamp = p.getLong("t"),
                gyroX = p.getDouble("gx").toFloat(),
                gyroY = p.getDouble("gy").toFloat(),
                gyroZ = p.getDouble("gz").toFloat(),
                accelX = p.getDouble("ax").toFloat(),
                accelY = p.getDouble("ay").toFloat(),
                accelZ = p.getDouble("az").toFloat()
            ))
        }
        val actionType = ActionType.valueOf(obj.getString("action"))
        val paramsObj = obj.optJSONObject("params")
        val params = if (paramsObj != null) {
            paramsObj.keys().asSequence().associateWith { paramsObj.get(it) }
        } else emptyMap()
        return CustomGesture(
            id = obj.getString("id"),
            name = obj.getString("name"),
            pattern = pattern,
            action = GestureAction(actionType, params),
            sensitivity = obj.optDouble("sensitivity", 0.7).toFloat()
        )
    }

    private fun rebuildTemplates() {
        gestureTemplates.clear()
        _customGestures.value.forEach {
            gestureTemplates[it.name] = it.extractFeatures()
        }
    }

    // ==================== Recording gestures ====================
    fun startRecording() {
        recordingPoints.clear()
        recordingStartTime = System.currentTimeMillis()
        _recording.value = true
    }

    fun addSensorData(gyroX: Float, gyroY: Float, gyroZ: Float,
                      accelX: Float, accelY: Float, accelZ: Float) {
        if (!_recording.value) return
        recordingPoints.add(MotionPoint(
            timestamp = System.currentTimeMillis() - recordingStartTime,
            gyroX = gyroX, gyroY = gyroY, gyroZ = gyroZ,
            accelX = accelX, accelY = accelY, accelZ = accelZ
        ))
    }

    fun stopRecording(name: String, action: GestureAction): CustomGesture? {
        if (!_recording.value) return null
        _recording.value = false
        if (recordingPoints.size < 50) return null   // too short

        // Normalise time axis to 100 points
        val normalized = normalizePattern(recordingPoints, 100)
        val gesture = CustomGesture(
            id = UUID.randomUUID().toString(),
            name = name,
            pattern = normalized,
            action = action,
            sensitivity = 0.7f
        )
        _customGestures.value = _customGestures.value + gesture
        saveCustomGestures()
        rebuildTemplates()
        return gesture
    }

    private fun normalizePattern(points: List<MotionPoint>, targetSize: Int): List<MotionPoint> {
        if (points.isEmpty()) return emptyList()
        val step = points.size.toFloat() / targetSize
        return (0 until targetSize).map { i ->
            val idx = (i * step).toInt().coerceIn(0, points.lastIndex)
            points[idx]
        }
    }

    // ==================== Recognition (DTW) ====================
    /**
     * Recognizes a gesture from a live stream of sensor data (list of MotionPoints).
     * Returns the best matching custom gesture name (or null) if confidence > threshold.
     */
    fun recognizeGesture(livePoints: List<MotionPoint>): Pair<String?, Float> {
        if (livePoints.size < 20 || gestureTemplates.isEmpty()) return null to 0f
        val liveFeatures = extractFeaturesFromPoints(livePoints)
        var bestName: String? = null
        var bestDistance = Float.MAX_VALUE
        for ((name, template) in gestureTemplates) {
            val distance = dtw(liveFeatures, template)
            if (distance < bestDistance) {
                bestDistance = distance
                bestName = name
            }
        }
        val confidence = 1f / (1f + bestDistance)
        val threshold = _customGestures.value.find { it.name == bestName }?.sensitivity ?: 0.7f
        return if (confidence >= threshold) bestName to confidence else null to confidence
    }

    private fun extractFeaturesFromPoints(points: List<MotionPoint>): List<Float> {
        return points.map { point ->
            sqrt(point.gyroX*point.gyroX + point.gyroY*point.gyroY + point.gyroZ*point.gyroZ) +
                    sqrt(point.accelX*point.accelX + point.accelY*point.accelY + point.accelZ*point.accelZ)
        }
    }

    private fun dtw(seq1: List<Float>, seq2: List<Float>): Float {
        val n = seq1.size
        val m = seq2.size
        val dtw = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE } }
        dtw[0][0] = 0f
        for (i in 1..n) {
            for (j in 1..m) {
                val cost = abs(seq1[i-1] - seq2[j-1])
                dtw[i][j] = cost + minOf(dtw[i-1][j], dtw[i][j-1], dtw[i-1][j-1])
            }
        }
        return dtw[n][m]
    }

    // ==================== Predefined air gestures (simple heuristics) ====================
    fun detectAirGesture(gyroX: Float, gyroY: Float, gyroZ: Float,
                         accelX: Float, accelY: Float, accelZ: Float,
                         window: List<MotionPoint>): AirGesture? {
        // Simplified detection based on gyroZ sign changes, magnitude, etc.
        if (window.size < 10) return null
        val avgGyroZ = window.map { it.gyroZ }.average().toFloat()
        val variance = window.map { (it.gyroZ - avgGyroZ) * (it.gyroZ - avgGyroZ) }.average().toFloat()
        val maxGyro = window.maxOfOrNull { abs(it.gyroZ) } ?: 0f

        return when {
            maxGyro > 10 && variance > 30 -> AirGesture.FIGURE_EIGHT
            maxGyro > 12 && avgGyroZ > 5 -> AirGesture.CIRCLE_CW
            maxGyro > 12 && avgGyroZ < -5 -> AirGesture.CIRCLE_CCW
            else -> null
        }
    }

    // ==================== Multi‑touch gesture handling (touchpad mode) ====================
    data class MultiTouchEvent(
        val pointerCount: Int,
        val centroids: List<Pair<Float, Float>>,
        val distances: List<Float>,
        val rotation: Float
    )

    fun processMultiTouch(event: MultiTouchEvent): MultiTouchGesture? {
        return when {
            event.pointerCount == 2 && abs(event.distances[0]) > 15f -> MultiTouchGesture.PINCH_TO_ZOOM
            event.pointerCount == 2 && abs(event.rotation) > 0.2f -> MultiTouchGesture.ROTATE_TWO_FINGERS
            event.pointerCount == 2 && abs(event.centroids[1].second - event.centroids[0].second) > 10f -> MultiTouchGesture.TWO_FINGER_SCROLL
            event.pointerCount == 3 -> {
                val dx = event.centroids.last().first - event.centroids.first().first
                when {
                    abs(dx) > 20 -> if (dx > 0) MultiTouchGesture.THREE_FINGER_SWIPE_RIGHT else MultiTouchGesture.THREE_FINGER_SWIPE_LEFT
                    else -> MultiTouchGesture.THREE_FINGER_SWIPE_UP
                }
            }
            event.pointerCount == 4 -> MultiTouchGesture.FOUR_FINGER_TAP
            else -> null
        }
    }

    // ==================== Execute gesture action ====================
    fun executeAction(action: GestureAction) {
        when (action.type) {
            ActionType.CLICK -> connectionManager.sendClick("left")
            ActionType.RIGHT_CLICK -> connectionManager.sendRightClick()
            ActionType.DOUBLE_CLICK -> connectionManager.sendDoubleClick()
            ActionType.SCROLL_UP -> connectionManager.sendScroll(3)
            ActionType.SCROLL_DOWN -> connectionManager.sendScroll(-3)
            ActionType.VOLUME_UP -> connectionManager.send("""{"type":"media","action":"volumeup"}""")
            ActionType.VOLUME_DOWN -> connectionManager.send("""{"type":"media","action":"volumedown"}""")
            ActionType.MEDIA_NEXT -> connectionManager.send("""{"type":"media","action":"next"}""")
            ActionType.MEDIA_PREV -> connectionManager.send("""{"type":"media","action":"prev"}""")
            ActionType.MEDIA_PLAY_PAUSE -> connectionManager.send("""{"type":"media","action":"playpause"}""")
            ActionType.KEYBOARD_SHORTCUT -> {
                val keys = action.parameters["keys"] as? String ?: ""
                connectionManager.send("""{"type":"keys","keys":"$keys"}""")
            }
            ActionType.CUSTOM_COMMAND -> {
                val cmd = action.parameters["command"] as? String ?: return
                connectionManager.send("""{"type":"custom","command":"$cmd"}""")
            }
            else -> {}
        }
    }
}