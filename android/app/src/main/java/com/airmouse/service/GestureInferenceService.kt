package com.airmouse.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.data.model.GestureResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.math.*

@AndroidEntryPoint
class GestureInferenceService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var prefs: PreferencesManager

    private var tflite: Interpreter? = null
    private var gestureLabels: List<String> = emptyList()
    private val sensorBuffer = mutableListOf<FloatArray>()
    private val windowSize = 60 // Larger window for better accuracy
    private var isRunning = false
    private var predictionRunning = false
    private var lastPredictionTime = 0L
    private var lastGesture = ""
    private var confidenceThreshold = 0.7f
    private var cooldownMs = 500L
    private var isCalibrated = false
    private lateinit var wakeLock: PowerManager.WakeLock

    // Different gesture types
    enum class GestureType(val id: Int) {
        NONE(0),
        CLICK(1),
        DOUBLE_CLICK(2),
        RIGHT_CLICK(3),
        SWIPE_LEFT(4),
        SWIPE_RIGHT(5),
        SWIPE_UP(6),
        SWIPE_DOWN(7),
        CIRCLE_CW(8),
        CIRCLE_CCW(9),
        ZOOM_IN(10),
        ZOOM_OUT(11),
        CUSTOM(12)
    }

    private val _gestureResult = MutableSharedFlow<GestureResult>()
    val gestureResult: SharedFlow<GestureResult> = _gestureResult.asSharedFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "gesture_inference_channel"
        const val ACTION_START = "START_INFERENCE"
        const val ACTION_STOP = "STOP_INFERENCE"
        const val ACTION_UPDATE_MODEL = "UPDATE_MODEL"
        const val ACTION_SET_THRESHOLD = "SET_THRESHOLD"
        const val EXTRA_MODEL_PATH = "model_path"
        const val EXTRA_THRESHOLD = "threshold"

        fun start(context: Context) {
            val intent = Intent(context, GestureInferenceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureInferenceService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        loadSettings()
        acquireWakeLock()
        loadModel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Inference",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Recognizes gestures using TensorFlow Lite"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String = "Gesture Inference", content: String = "Recognising gestures..."): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_gesture)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun loadSettings() {
        confidenceThreshold = prefs.getFloat("gesture_confidence_threshold", 0.7f)
        cooldownMs = prefs.getLong("gesture_cooldown_ms", 500L)
        isCalibrated = prefs.getBoolean("calibration_complete", false)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GestureInferenceService::WakeLock")
        wakeLock.acquire(10 * 60 * 1000L) // 10 minutes timeout
    }

    private fun loadModel() {
        try {
            // Try to load custom trained model first
            val customModelPath = prefs.getString("custom_gesture_model", "")
            val modelBuffer = if (customModelPath.isNotEmpty() && File(customModelPath).exists()) {
                loadModelFromFile(customModelPath)
            } else {
                loadModelFromAssets("gesture_model.tflite")
            }

            tflite = Interpreter(modelBuffer)

            // Load labels
            val labelsPath = prefs.getString("gesture_labels", "")
            gestureLabels = if (labelsPath.isNotEmpty() && File(labelsPath).exists()) {
                File(labelsPath).readLines()
            } else {
                loadLabelsFromAssets()
            }

            updateNotification("Model Loaded", "Ready to recognize ${gestureLabels.size} gestures")
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotification("Error", "Failed to load model: ${e.message}")
        }
    }

    private fun loadModelFromAssets(filename: String): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadModelFromFile(path: String): MappedByteBuffer {
        val file = File(path)
        val inputStream = FileInputStream(file)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    }

    private fun loadLabelsFromAssets(): List<String> {
        return try {
            val labelsJson = assets.open("gesture_labels.json").bufferedReader().use { it.readText() }
            com.google.gson.Gson().fromJson(labelsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            // Default labels
            listOf("Click", "Double Click", "Right Click", "Swipe Left", "Swipe Right", "Swipe Up", "Swipe Down")
        }
    }

    fun startInference() {
        if (isRunning) return
        if (!isCalibrated) {
            updateNotification("Calibration Required", "Please calibrate sensors first")
            return
        }

        isRunning = true
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        gyro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        rotation?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        updateNotification("Active", "Listening for gestures")
    }

    fun stopInference() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
        sensorBuffer.clear()
        predictionRunning = false
        updateNotification("Stopped", "Gesture inference paused")
    }

    fun updateModel(modelPath: String, labelsPath: String) {
        prefs.putString("custom_gesture_model", modelPath)
        prefs.putString("gesture_labels", labelsPath)
        loadModel()
    }

    fun setConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold.coerceIn(0.5f, 0.95f)
        prefs.putFloat("gesture_confidence_threshold", confidenceThreshold)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val gyroX = values[0]
                val gyroY = values[1]
                val gyroZ = values[2]

                // Apply calibration offsets
                val calibratedX = gyroX - prefs.getFloat("gyro_offset_x", 0f)
                val calibratedY = gyroY - prefs.getFloat("gyro_offset_y", 0f)
                val calibratedZ = gyroZ - prefs.getFloat("gyro_offset_z", 0f)

                sensorBuffer.add(floatArrayOf(calibratedX, calibratedY, calibratedZ, 0f, 0f, 0f))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val last = if (sensorBuffer.isNotEmpty()) sensorBuffer.last() else floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
                sensorBuffer.add(floatArrayOf(last[0], last[1], last[2], values[0], values[1], values[2]))
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Use rotation vector for orientation-based gestures
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val roll = orientation[2]
                val pitch = orientation[1]
                val yaw = orientation[0]

                // Add to buffer
                if (sensorBuffer.isNotEmpty()) {
                    val last = sensorBuffer.last()
                    sensorBuffer.add(floatArrayOf(last[0], last[1], last[2], roll, pitch, yaw))
                }
            }
        }

        // Keep buffer at window size
        while (sensorBuffer.size > windowSize) {
            sensorBuffer.removeAt(0)
        }

        // Run prediction when buffer is full
        if (sensorBuffer.size == windowSize && !predictionRunning) {
            runPrediction()
        }

        // Also check for simple rule-based gestures for quick response
        detectSimpleGestures()
    }

    private fun detectSimpleGestures() {
        if (sensorBuffer.isEmpty()) return

        val lastSample = sensorBuffer.last()
        val gyroMagnitude = sqrt(lastSample[0] * lastSample[0] + lastSample[1] * lastSample[1] + lastSample[2] * lastSample[2])

        val now = System.currentTimeMillis()
        if (now - lastPredictionTime < cooldownMs) return

        when {
            gyroMagnitude > 8f -> {
                val timeSinceLast = now - lastPredictionTime
                if (timeSinceLast < 300 && lastGesture == "click") {
                    // Double click detected
                    serviceScope.launch {
                        _gestureResult.emit(GestureResult("double_click", 0.95f))
                    }
                    lastPredictionTime = now
                    lastGesture = "double_click"
                } else {
                    // Single click
                    serviceScope.launch {
                        _gestureResult.emit(GestureResult("click", 0.9f))
                    }
                    lastPredictionTime = now
                    lastGesture = "click"
                }
            }
            abs(lastSample[0]) > 6f -> {
                // Right click (tilt)
                serviceScope.launch {
                    _gestureResult.emit(GestureResult("right_click", 0.85f))
                }
                lastPredictionTime = now
                lastGesture = "right_click"
            }
            abs(lastSample[4]) > 5f -> {
                // Scroll
                val direction = if (lastSample[4] > 0) "scroll_up" else "scroll_down"
                serviceScope.launch {
                    _gestureResult.emit(GestureResult(direction, 0.8f))
                }
                lastPredictionTime = now
                lastGesture = direction
            }
        }
    }

    private fun runPrediction() {
        val now = System.currentTimeMillis()
        if (now - lastPredictionTime < cooldownMs) return
        if (tflite == null || gestureLabels.isEmpty()) return

        predictionRunning = true
        serviceScope.launch(Dispatchers.Default) {
            try {
                // Prepare input tensor (batch_size, timesteps, features)
                val input = arrayOf(sensorBuffer.map { it.clone() }.toTypedArray())
                val output = Array(1) { FloatArray(gestureLabels.size) }

                tflite?.run(input, output)

                val probs = output[0]
                val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: -1
                val confidence = if (maxIdx >= 0) probs[maxIdx] else 0f

                if (confidence > confidenceThreshold && maxIdx >= 0 && maxIdx < gestureLabels.size) {
                    val gestureLabel = gestureLabels[maxIdx]
                    lastPredictionTime = now
                    lastGesture = gestureLabel
                    _gestureResult.emit(GestureResult(gestureLabel, confidence))

                    // Log gesture for statistics
                    incrementGestureCount(gestureLabel)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                predictionRunning = false
            }
        }
    }

    private fun incrementGestureCount(gesture: String) {
        val key = "stat_gesture_$gesture"
        val currentCount = prefs.getInt(key, 0)
        prefs.putInt(key, currentCount + 1)
        prefs.putInt("stat_gestures", prefs.getInt("stat_gestures", 0) + 1)
    }

    fun getGestureStatistics(): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        gestureLabels.forEach { label ->
            val count = prefs.getInt("stat_gesture_$label", 0)
            if (count > 0) {
                stats[label] = count
            }
        }
        return stats
    }

    fun resetStatistics() {
        gestureLabels.forEach { label ->
            prefs.putInt("stat_gesture_$label", 0)
        }
        prefs.putInt("stat_gestures", 0)
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun isRunning(): Boolean = isRunning

    fun getCurrentGesture(): String = lastGesture

    fun getConfidenceThreshold(): Float = confidenceThreshold

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startInference()
            ACTION_STOP -> stopInference()
            ACTION_UPDATE_MODEL -> {
                val modelPath = intent.getStringExtra(EXTRA_MODEL_PATH) ?: ""
                updateModel(modelPath, "")
            }
            ACTION_SET_THRESHOLD -> {
                val threshold = intent.getFloatExtra(EXTRA_THRESHOLD, confidenceThreshold)
                setConfidenceThreshold(threshold)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInference()
        tflite?.close()
        serviceScope.cancel()

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}