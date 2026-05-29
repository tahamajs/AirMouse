// GestureRecorderService.kt
package com.airmouse.gesture

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.airmouse.R
import com.opencsv.CSVWriter
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class GestureRecorderService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var isRecording = false
    private var currentGestureName = ""
    private var currentSessionId = ""
    private val sensorDataBuffer = mutableListOf<SensorDataPoint>()
    private var datasetFile: File? = null
    private val dataQueue = ConcurrentLinkedQueue<SensorDataPoint>()
    private var writeJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "gesture_recorder_channel"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val EXTRA_GESTURE_NAME = "gesture_name"
    }

    inner class LocalBinder : Binder() {
        fun getService(): GestureRecorderService = this@GestureRecorderService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
        setupDatasetFile()
        startForeground(NOTIFICATION_ID, createNotification("Idle", "Ready to record"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val name = intent.getStringExtra(EXTRA_GESTURE_NAME) ?: "unknown"
                startRecording(name)
            }
            ACTION_STOP_RECORDING -> stopRecording()
        }
        return START_STICKY
    }

    private fun setupDatasetFile() {
        val datasetDir = File(getExternalFilesDir(null), "gesture_dataset")
        if (!datasetDir.exists()) datasetDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        datasetFile = File(datasetDir, "gestures_$timestamp.csv")
        FileWriter(datasetFile!!).use { fw ->
            CSVWriter(fw).use { writer ->
                writer.writeNext(arrayOf(
                    "session_id", "gesture_name", "timestamp_ms",
                    "gyro_x", "gyro_y", "gyro_z",
                    "accel_x", "accel_y", "accel_z"
                ))
            }
        }
        Log.i("GestureRecorder", "Dataset file: ${datasetFile?.absolutePath}")
    }

    private fun startRecording(gestureName: String) {
        if (isRecording) return
        currentGestureName = gestureName
        currentSessionId = UUID.randomUUID().toString()
        sensorDataBuffer.clear()
        isRecording = true
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        updateNotification("Recording $gestureName", "Recording...")
        Log.i("GestureRecorder", "Started recording: $gestureName")
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        sensorManager.unregisterListener(this)
        flushQueue()
        updateNotification("Idle", "Recording stopped")
        Log.i("GestureRecorder", "Stopped recording, samples: ${sensorDataBuffer.size}")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording) return
        val point = SensorDataPoint(
            sessionId = currentSessionId,
            gestureName = currentGestureName,
            timestampMs = System.currentTimeMillis(),
            gyroX = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[0] else 0f,
            gyroY = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[1] else 0f,
            gyroZ = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[2] else 0f,
            accelX = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[0] else 0f,
            accelY = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[1] else 0f,
            accelZ = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[2] else 0f
        )
        sensorDataBuffer.add(point)
        dataQueue.add(point)
        if (dataQueue.size >= 50 && writeJob?.isActive != true) {
            writeJob = mainScope.launch(Dispatchers.IO) { flushQueue() }
        }
    }

    private suspend fun flushQueue() {
        val toWrite = mutableListOf<SensorDataPoint>()
        while (dataQueue.isNotEmpty()) {
            toWrite.add(dataQueue.poll())
        }
        if (toWrite.isNotEmpty()) {
            writeToCsv(toWrite)
        }
    }

    private fun writeToCsv(points: List<SensorDataPoint>) {
        try {
            FileWriter(datasetFile!!, true).use { fw ->
                CSVWriter(fw).use { writer ->
                    for (p in points) {
                        writer.writeNext(arrayOf(
                            p.sessionId, p.gestureName, p.timestampMs.toString(),
                            p.gyroX.toString(), p.gyroY.toString(), p.gyroZ.toString(),
                            p.accelX.toString(), p.accelY.toString(), p.accelZ.toString()
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GestureRecorder", "Write error", e)
        }
    }

    fun exportDataset(): File? = datasetFile

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotification(title: String, content: String): Notification {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return Notification.Builder(this, CHANNEL_ID)
                .setContentTitle(title).setContentText(content)
                .setSmallIcon(R.drawable.ic_gesture).build()
        } else {
            @Suppress("DEPRECATION")
            return Notification.Builder(this)
                .setContentTitle(title).setContentText(content)
                .setSmallIcon(R.drawable.ic_gesture).build()
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Gesture Recorder", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    data class SensorDataPoint(
        val sessionId: String, val gestureName: String, val timestampMs: Long,
        val gyroX: Float, val gyroY: Float, val gyroZ: Float,
        val accelX: Float, val accelY: Float, val accelZ: Float
    )

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onBind(intent: Intent?): IBinder? = binder
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        mainScope.cancel()
    }
}