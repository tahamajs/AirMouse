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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class GestureRecorderService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var prefs: PreferencesManager

    private var isRecording = false
    private var currentGestureName = ""
    private val sensorData = mutableListOf<SensorDataPoint>()
    private var recordingJob: Job? = null
    private var recordingTime = 0
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "gesture_recorder"
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_EXPORT_DATASET = "EXPORT_DATASET"
        const val EXTRA_GESTURE_NAME = "gesture_name"
        const val EXTRA_DURATION = "duration"

        fun start(context: Context, gestureName: String, duration: Int = 5) {
            val intent = Intent(context, GestureRecorderService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_GESTURE_NAME, gestureName)
                putExtra(EXTRA_DURATION, duration)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GestureRecorderService::class.java))
        }

        fun export(context: Context) {
            val intent = Intent(context, GestureRecorderService::class.java).apply {
                action = ACTION_EXPORT_DATASET
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeSensors()
    }

    private fun initializeSensors() {
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gesture Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Records gesture data for AI training"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_gesture)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startRecording(gestureName: String, durationSeconds: Int) {
        if (isRecording) return

        currentGestureName = gestureName
        sensorData.clear()
        recordingTime = 0
        isRecording = true

        // Register sensors
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }

        startForeground(NOTIFICATION_ID, createNotification("Recording", "Recording: $gestureName"))

        // Auto-stop after duration
        recordingJob = serviceScope.launch {
            delay(durationSeconds * 1000L)
            if (isRecording) {
                stopRecording()
            }
        }

        // Timer updates
        recordingJob = serviceScope.launch {
            while (isRecording) {
                delay(1000)
                recordingTime++
                updateNotification("Recording", "Recording: $gestureName - ${recordingTime}s")
            }
        }
    }

    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        recordingJob?.cancel()
        sensorManager.unregisterListener(this)

        // Save recorded data
        saveRecording()

        updateNotification("Saved", "Gesture saved: $currentGestureName")

        // Auto-stop service after saving
        serviceScope.launch {
            delay(2000)
            stopSelf()
        }
    }

    private fun saveRecording() {
        if (sensorData.isEmpty()) return

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "gesture_${currentGestureName}_$timestamp.csv"
        val file = File(getExternalFilesDir(null), fileName)

        try {
            FileWriter(file).use { writer ->
                // Write header
                writer.write("timestamp_ms,gyro_x,gyro_y,gyro_z,accel_x,accel_y,accel_z,mag_x,mag_y,mag_z\n")

                // Write data
                sensorData.forEach { data ->
                    writer.write("${data.timestamp},${data.gyroX},${data.gyroY},${data.gyroZ}," +
                            "${data.accelX},${data.accelY},${data.accelZ},${data.magX},${data.magY},${data.magZ}\n")
                }
            }

            // Save to preferences
            val savedGestures = prefs.getString("recorded_gestures", "")
            val newEntry = if (savedGestures.isEmpty()) fileName else "$savedGestures,$fileName"
            prefs.putString("recorded_gestures", newEntry)

            android.util.Log.i("GestureRecorder", "Saved gesture to: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("GestureRecorder", "Failed to save recording", e)
        }
    }

    fun exportDataset() {
        serviceScope.launch(Dispatchers.IO) {
            val recordedGestures = prefs.getString("recorded_gestures", "").split(",")
            if (recordedGestures.isEmpty() || recordedGestures.first().isEmpty()) {
                updateNotification("No Data", "No recorded gestures to export")
                return@launch
            }

            val exportFile = File(getExternalFilesDir(null), "gesture_dataset_${System.currentTimeMillis()}.csv")

            FileWriter(exportFile).use { writer ->
                // Header
                writer.write("gesture_name,timestamp_ms,gyro_x,gyro_y,gyro_z,accel_x,accel_y,accel_z,mag_x,mag_y,mag_z\n")

                // Combine all gesture files
                recordedGestures.forEach { fileName ->
                    if (fileName.isNotEmpty()) {
                        val gestureFile = File(getExternalFilesDir(null), fileName)
                        if (gestureFile.exists()) {
                            val gestureName = fileName.substringAfter("gesture_").substringBefore("_")
                            gestureFile.readLines().drop(1).forEach { line ->
                                writer.write("$gestureName,$line\n")
                            }
                        }
                    }
                }
            }

            updateNotification("Exported", "Dataset exported to: ${exportFile.name}")
            android.util.Log.i("GestureRecorder", "Dataset exported to: ${exportFile.absolutePath}")
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording) return

        val timestamp = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val existingIndex = sensorData.lastIndex
                if (existingIndex >= 0 && sensorData[existingIndex].timestamp == timestamp) {
                    sensorData[existingIndex] = sensorData[existingIndex].copy(
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2]
                    )
                } else {
                    sensorData.add(SensorDataPoint(
                        timestamp = timestamp,
                        gyroX = event.values[0],
                        gyroY = event.values[1],
                        gyroZ = event.values[2]
                    ))
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val existingIndex = sensorData.lastIndex
                if (existingIndex >= 0 && sensorData[existingIndex].timestamp == timestamp) {
                    sensorData[existingIndex] = sensorData[existingIndex].copy(
                        accelX = event.values[0],
                        accelY = event.values[1],
                        accelZ = event.values[2]
                    )
                } else {
                    sensorData.add(SensorDataPoint(
                        timestamp = timestamp,
                        accelX = event.values[0],
                        accelY = event.values[1],
                        accelZ = event.values[2]
                    ))
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val existingIndex = sensorData.lastIndex
                if (existingIndex >= 0 && sensorData[existingIndex].timestamp == timestamp) {
                    sensorData[existingIndex] = sensorData[existingIndex].copy(
                        magX = event.values[0],
                        magY = event.values[1],
                        magZ = event.values[2]
                    )
                } else {
                    sensorData.add(SensorDataPoint(
                        timestamp = timestamp,
                        magX = event.values[0],
                        magY = event.values[1],
                        magZ = event.values[2]
                    ))
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val gestureName = intent.getStringExtra(EXTRA_GESTURE_NAME) ?: "gesture"
                val duration = intent.getIntExtra(EXTRA_DURATION, 5)
                startRecording(gestureName, duration)
            }
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_EXPORT_DATASET -> exportDataset()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

data class SensorDataPoint(
    val timestamp: Long,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f
)