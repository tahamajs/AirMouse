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
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.GestureDetector
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import kotlin.math.*

@AndroidEntryPoint
class SensorService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var prefs: PreferencesManager

    private var isActive = false
    private lateinit var wakeLock: PowerManager.WakeLock
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f
    private var accelX = 0f
    private var accelY = 0f
    private var accelZ = 0f
    
    private val _orientationData = MutableSharedFlow<OrientationData>()
    val orientationData: SharedFlow<OrientationData> = _orientationData.asSharedFlow()
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sensor_service"

        fun start(context: Context) {
            val intent = Intent(context, SensorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorService::WakeLock")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Mouse Pro")
            .setContentText("Sensors active - controlling cursor")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }
        
        return builder.build()
    }

    fun startSensors() {
        if (isActive) return
        
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        
        isActive = true
        wakeLock.acquire(10 * 60 * 1000L)
    }

    fun stopSensors() {
        if (!isActive) return
        sensorManager.unregisterListener(this)
        isActive = false
        
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return
        
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = event.values[0]
                accelY = event.values[1]
                accelZ = event.values[2]
            }
        }
        
        serviceScope.launch {
            _orientationData.emit(
                OrientationData(
                    yaw = 0f,
                    pitch = 0f,
                    roll = 0f,
                    gyroX = gyroX,
                    gyroY = gyroY,
                    gyroZ = gyroZ,
                    accelX = accelX,
                    accelY = accelY,
                    accelZ = accelZ
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SENSORS" -> startSensors()
            "STOP_SENSORS" -> stopSensors()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensors()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

data class OrientationData(
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float
)
