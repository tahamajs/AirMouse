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
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.*

@AndroidEntryPoint
class OrientationMonitorService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var connectionManager: ConnectionManager

    private var isActive = false
    private lateinit var wakeLock: PowerManager.WakeLock
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastSendTime = 0L
    private val sendIntervalMs = 50L // 20Hz

    companion object {
        private const val NOTIFICATION_ID = 2003
        private const val CHANNEL_ID = "orientation_monitor"
        const val ACTION_START = "START_MONITOR"
        const val ACTION_STOP = "STOP_MONITOR"
        const val BROADCAST_ORIENTATION = "com.airmouse.ORIENTATION_UPDATE"
        const val EXTRA_YAW = "yaw"
        const val EXTRA_PITCH = "pitch"
        const val EXTRA_ROLL = "roll"

        fun start(context: Context) {
            val intent = Intent(context, OrientationMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OrientationMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Initializing", "Starting orientation monitor"))

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OrientationMonitor::WakeLock")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orientation Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_sensor)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startMonitoring() {
        if (isActive) return

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        isActive = true
        wakeLock.acquire(10 * 60 * 1000L)

        updateNotification("Active", "Monitoring orientation")
    }

    fun stopMonitoring() {
        if (!isActive) return
        sensorManager.unregisterListener(this)
        isActive = false

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        updateNotification("Stopped", "Orientation monitor paused")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetic, 0, 3)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)

                yaw = toDegrees(orientation[0])
                pitch = toDegrees(orientation[1])
                roll = toDegrees(orientation[2])

                sendOrientationData()
            }
        }

        updateOrientationWithFusion()
    }

    private fun updateOrientationWithFusion() {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientation)

            val fusedYaw = toDegrees(orientation[0])
            val fusedPitch = toDegrees(orientation[1])
            val fusedRoll = toDegrees(orientation[2])

            // Complementary filter
            val alpha = 0.96f
            yaw = alpha * yaw + (1 - alpha) * fusedYaw
            pitch = alpha * pitch + (1 - alpha) * fusedPitch
            roll = alpha * roll + (1 - alpha) * fusedRoll

            sendOrientationData()
        }
    }

    private fun sendOrientationData() {
        val now = System.currentTimeMillis()
        if (now - lastSendTime < sendIntervalMs) return
        lastSendTime = now

        // ✅ FIXED: Send as move commands via ConnectionManager
        // Map orientation to cursor movement
        val sensitivity = 0.5f
        val dx = roll * sensitivity
        val dy = pitch * sensitivity
        connectionManager.sendMove(dx, dy)

        // Broadcast for UI
        val intent = Intent(BROADCAST_ORIENTATION).apply {
            putExtra(EXTRA_YAW, yaw)
            putExtra(EXTRA_PITCH, pitch)
            putExtra(EXTRA_ROLL, roll)
        }
        sendBroadcast(intent)
    }

    // ✅ Helper to convert radians to degrees
    private fun toDegrees(rad: Float): Float {
        return rad * 180f / PI.toFloat()
    }

    fun getCurrentOrientation(): Triple<Float, Float, Float> = Triple(yaw, pitch, roll)

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
        Timber.i("OrientationMonitorService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}