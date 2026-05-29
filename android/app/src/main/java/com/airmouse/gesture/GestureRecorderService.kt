// app/src/main/java/com/airmouse/gesture/GestureRecorderService.kt
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
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorRegistered = false

    companion object {
        private const val NOTIFICATION_ID = 3001
        private const val CHANNEL_ID = "gesture_recorder_channel"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val EXTRA_GESTURE_NAME = "gesture_name"
        const val BROADCAST_DATASET_READY = "com.airmouse.DATASET_READY"
        const val EXTRA_DATASET_PATH = "dataset_path"
        private const val TAG = "GestureRecorder"
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
        startForeground(NOTIFICATION_ID, createNotification("Idle", "Ready to record gestures"))
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val name = intent.getStringExtra(EXTRA_GESTURE_NAME) ?: return START_NOT_STICKY
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
        Log.i(TAG, "Dataset file created: ${datasetFile?.absolutePath}")
    }

    private fun startRecording(gestureName: String) {
        if (isRecording) {
            Log.w(TAG, "Already recording, ignoring start request")
            return
        }
        currentGestureName = gestureName
        currentSessionId = UUID.randomUUID().toString()
        sensorDataBuffer.clear()
        dataQueue.clear()
        isRecording = true
        registerSensors()
        updateNotification("Recording $gestureName", "Recording gesture...")
        Log.i(TAG, "Started recording: $gestureName (session: $currentSessionId)")
    }

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

    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Not recording, ignoring stop request")
            return
        }
        isRecording = false
        unregisterSensors()
        serviceScope.launch { flushQueue() }
        updateNotification("Idle", "Recording stopped")
        Log.i(TAG, "Stopped recording, total samples: ${sensorDataBuffer.size}")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording) return
        val now = System.currentTimeMillis()
        val point = when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> SensorDataPoint(
                sessionId = currentSessionId,
                gestureName = currentGestureName,
                timestampMs = now,
                gyroX = event.values[0], gyroY = event.values[1], gyroZ = event.values[2],
                accelX = 0f, accelY = 0f, accelZ = 0f
            )
            Sensor.TYPE_ACCELEROMETER -> SensorDataPoint(
                sessionId = currentSessionId,
                gestureName = currentGestureName,
                timestampMs = now,
                gyroX = 0f, gyroY = 0f, gyroZ = 0f,
                accelX = event.values[0], accelY = event.values[1], accelZ = event.values[2]
            )
            else -> return
        }
        // Merge with previous point if same timestamp (rare)
        val lastPoint = sensorDataBuffer.lastOrNull()
        if (lastPoint != null && lastPoint.timestampMs == now) {
            val merged = lastPoint.copy(
                gyroX = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[0] else lastPoint.gyroX,
                gyroY = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[1] else lastPoint.gyroY,
                gyroZ = if (event.sensor.type == Sensor.TYPE_GYROSCOPE) event.values[2] else lastPoint.gyroZ,
                accelX = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[0] else lastPoint.accelX,
                accelY = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[1] else lastPoint.accelY,
                accelZ = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) event.values[2] else lastPoint.accelZ
            )
            sensorDataBuffer[sensorDataBuffer.lastIndex] = merged
            dataQueue.add(merged)
        } else {
            sensorDataBuffer.add(point)
            dataQueue.add(point)
        }

        if (dataQueue.size >= 50 && writeJob?.isActive != true) {
            writeJob = serviceScope.launch { flushQueue() }
        }
    }

    private suspend fun flushQueue() {
        val toWrite = mutableListOf<SensorDataPoint>()
        while (dataQueue.isNotEmpty()) {
            toWrite.add(dataQueue.poll())
        }
        if (toWrite.isNotEmpty()) {
            withContext(Dispatchers.IO) { writeToCsv(toWrite) }
        }
    }

    private fun writeToCsv(points: List<SensorDataPoint>) {
        if (datasetFile == null) return
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
            Log.d(TAG, "Wrote ${points.size} samples to CSV")
        } catch (e: Exception) {
            Log.e(TAG, "CSV write error", e)
        }
    }

    fun exportDataset(): File? = datasetFile

    fun shareDataset() {
        val file = datasetFile ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                this@GestureRecorderService,
                "${packageName}.fileprovider",
                file
            ))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export Dataset"))
        Log.i(TAG, "Dataset shared: ${file.absolutePath}")
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_gesture)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Records gesture data for AI training" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onBind(intent: Intent?): IBinder? = binder
    override fun onDestroy() {
        super.onDestroy()
        unregisterSensors()
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
    }

    data class SensorDataPoint(
        val sessionId: String,
        val gestureName: String,
        val timestampMs: Long,
        val gyroX: Float, val gyroY: Float, val gyroZ: Float,
        val accelX: Float, val accelY: Float, val accelZ: Float
    )
}