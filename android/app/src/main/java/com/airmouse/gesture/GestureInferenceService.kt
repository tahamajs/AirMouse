// app/src/main/java/com/airmouse/gesture/GestureInferenceService.kt
package com.airmouse.gesture

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.google.gson.Gson
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class GestureInferenceService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var tflite: Interpreter? = null
    private var gestureLabels: List<String> = emptyList()
    private val sensorBuffer = mutableListOf<FloatArray>()
    private val windowSize = 30   // ~1.5 seconds at 20Hz
    private var predictionRunning = false
    private var lastPredictionTime = 0L
    private val confidenceThreshold = 0.7f
    private val cooldownMs = 500L
    private var isRunning = false
    private var sensorRegistered = false

    var onGestureDetected: ((String, Float) -> Unit)? = null

    companion object {
        private const val NOTIFICATION_ID = 3002
        private const val CHANNEL_ID = "gesture_inference_channel"
        private const val TAG = "GestureInference"
    }

    inner class LocalBinder : Binder() {
        fun getService(): GestureInferenceService = this@GestureInferenceService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        loadModel()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "Service created")
    }

    private fun loadModel() {
        try {
            // Load TFLite model from assets
            val modelBuffer = loadModelFile("gesture_model.tflite")
            tflite = Interpreter(modelBuffer)
            // Load labels
            val labelsJson = assets.open("gesture_labels.json").bufferedReader().use { it.readText() }
            gestureLabels = Gson().fromJson(labelsJson, Array<String>::class.java).toList()
            Log.i(TAG, "Model loaded. Labels: $gestureLabels")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        start()
        return START_STICKY
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        registerSensors()
        Log.i(TAG, "Inference started")
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        unregisterSensors()
        sensorBuffer.clear()
        Log.i(TAG, "Inference stopped")
    }

    fun isRunning(): Boolean = isRunning

    private fun registerSensors() {
        if (sensorRegistered) return
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Gyroscope registered")
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Accelerometer registered")
        }
        sensorRegistered = true
    }

    private fun unregisterSensors() {
        if (!sensorRegistered) return
        sensorManager.unregisterListener(this)
        sensorRegistered = false
        Log.d(TAG, "Sensors unregistered")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return
        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
                sensorBuffer.add(floatArrayOf(values[0], values[1], values[2], last[3], last[4], last[5]))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f)
                sensorBuffer.add(floatArrayOf(last[0], last[1], last[2], values[0], values[1], values[2]))
            }
            else -> return
        }
        if (sensorBuffer.size > windowSize) sensorBuffer.removeAt(0)
        if (sensorBuffer.size == windowSize && !predictionRunning) {
            runPrediction()
        }
    }

    private fun runPrediction() {
        val now = System.currentTimeMillis()
        if (now - lastPredictionTime < cooldownMs) return
        if (tflite == null || gestureLabels.isEmpty()) return

        predictionRunning = true
        try {
            // Prepare input: [1, windowSize, 6]
            val input = Array(1) { sensorBuffer.map { it.clone() }.toTypedArray() }
            val output = Array(1) { FloatArray(gestureLabels.size) }
            tflite?.run(input, output)

            val probs = output[0]
            val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: -1
            val confidence = if (maxIdx >= 0) probs[maxIdx] else 0f

            if (confidence > confidenceThreshold && maxIdx >= 0) {
                val gesture = gestureLabels[maxIdx]
                lastPredictionTime = now
                Log.d(TAG, "Gesture detected: $gesture (${String.format("%.2f", confidence)})")
                onGestureDetected?.invoke(gesture, confidence)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Prediction error", e)
        } finally {
            predictionRunning = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gesture Inference Active")
            .setContentText("Recognizing gestures from motion sensors")
            .setSmallIcon(R.drawable.ic_gesture)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Inference",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Recognizes gestures from sensor data" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stop()
        tflite?.close()
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = binder
}