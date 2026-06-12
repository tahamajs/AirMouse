// app/src/main/java/com/airmouse/service/GestureInferenceService.kt
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
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.data.model.GestureResult
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class GestureInferenceService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var prefs: PreferencesManager

    private var tflite: Interpreter? = null
    private var gestureLabels: List<String> = emptyList()
    private val sensorBuffer = mutableListOf<FloatArray>()
    private val windowSize = 30
    private var isRunning = false
    private var predictionRunning = false
    private var lastPredictionTime = 0L
    private val confidenceThreshold = 0.7f
    private val cooldownMs = 500L

    private val _gestureResult = MutableSharedFlow<GestureResult>()
    val gestureResult: SharedFlow<GestureResult> = _gestureResult.asSharedFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "gesture_inference_channel"

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
        loadModel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Inference",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gesture Inference")
            .setContentText("Recognising gestures...")
            .setSmallIcon(R.drawable.ic_gesture)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("gesture_model.tflite")
            tflite = Interpreter(modelBuffer)
            val labelsJson = assets.open("gesture_labels.json").bufferedReader().use { it.readText() }
            gestureLabels = com.google.gson.Gson().fromJson(labelsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            e.printStackTrace()
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

    fun startInference() {
        if (isRunning) return
        isRunning = true
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stopInference() {
        if (!isRunning) return
        isRunning = false
        sensorManager.unregisterListener(this)
        sensorBuffer.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return
        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f,0f,0f,0f,0f,0f)
                sensorBuffer.add(floatArrayOf(values[0], values[1], values[2], last[3], last[4], last[5]))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f,0f,0f,0f,0f,0f)
                sensorBuffer.add(floatArrayOf(last[0], last[1], last[2], values[0], values[1], values[2]))
            }
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
        serviceScope.launch(Dispatchers.Default) {
            try {
                val input = arrayOf(sensorBuffer.toTypedArray())
                val output = Array(1) { FloatArray(gestureLabels.size) }
                tflite?.run(input, output)

                val probs = output[0]
                val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: -1
                val confidence = if (maxIdx >= 0) probs[maxIdx] else 0f

                if (confidence > confidenceThreshold && maxIdx >= 0) {
                    lastPredictionTime = now
                    _gestureResult.emit(GestureResult(gestureLabels[maxIdx], confidence))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                predictionRunning = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_INFERENCE") startInference()
        else if (intent?.action == "STOP_INFERENCE") stopInference()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopInference()
        tflite?.close()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
