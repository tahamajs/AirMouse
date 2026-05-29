// app/src/main/java/com/airmouse/orientation/OrientationMonitorService.kt
package com.airmouse.orientation

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
import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.*

class OrientationMonitorService : Service(), SensorEventListener {

    private val binder = LocalBinder()
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isRestingFlat = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var gravity = FloatArray(3)

    companion object {
        private const val NOTIFICATION_ID = 3004
        private const val CHANNEL_ID = "orientation_channel"
        const val ACTION_START = "START_ORIENTATION"
        const val ACTION_STOP = "STOP_ORIENTATION"
        private const val TAG = "OrientationMonitor"
        private const val ALPHA = 0.8f
        private const val FLAT_THRESHOLD = 0.2f
    }

    inner class LocalBinder : Binder() {
        fun getService(): OrientationMonitorService = this@OrientationMonitorService
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(TAG, "Monitoring started")
        } ?: Log.e(TAG, "No accelerometer")
    }

    private fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        Log.i(TAG, "Monitoring stopped")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0]
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1]
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2]

        val xyMagnitude = Math.hypot(gravity[0].toDouble(), gravity[1].toDouble())
        val isFlat = xyMagnitude < FLAT_THRESHOLD

        if (isFlat != isRestingFlat) {
            isRestingFlat = isFlat
            WebSocketManager.sendPauseMovement(isFlat)
            Log.i(TAG, if (isFlat) "Pausing movement (phone flat)" else "Resuming movement (phone picked up)")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orientation Monitor")
            .setContentText("Auto‑pauses movement when phone is flat")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Orientation Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Detects when phone is placed flat" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder
    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.i(TAG, "Service destroyed")
    }
}