// app/src/main/java/com/airmouse/service/SensorService.kt
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airmouse.R
import com.airmouse.domain.model.Orientation
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.GestureDetector
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.*

@AndroidEntryPoint
class SensorService : Service(), SensorEventListener {

    @Inject lateinit var sensorManager: SensorManager
    @Inject lateinit var calibrationHelper: CalibrationHelper
    @Inject lateinit var gestureDetector: GestureDetector
    @Inject lateinit var prefs: PreferencesManager
    @Inject lateinit var batterySaver: BatterySaver

    private lateinit var wakeLock: PowerManager.WakeLock
    private var rotationVectorSensor: Sensor? = null
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null

    private val _orientation = MutableLiveData<Orientation>()
    val orientation: LiveData<Orientation> = _orientation

    private val _gesture = MutableLiveData<GestureDetector.MotionResult>()
    val gesture: LiveData<GestureDetector.MotionResult> = _gesture

    private val _gyroY = MutableLiveData<Float>()
    val gyroY: LiveData<Float> = _gyroY

    private val _accelY = MutableLiveData<Float>()
    val accelY: LiveData<Float> = _accelY

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isCollecting = false
    private var lastTimestamp = 0L

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sensor_service_channel"

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
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AirMouse::SensorWakeLock")
        wakeLock.acquire(10 * 60 * 1000L)
        initSensors()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sensor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Mouse Sensors")
            .setContentText("Collecting motion data...")
            .setSmallIcon(R.drawable.ic_sensor)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initSensors() {
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun startCollection() {
        if (isCollecting) return
        isCollecting = true
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stopCollection() {
        if (!isCollecting) return
        isCollecting = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = System.currentTimeMillis()
        val dt = (now - lastTimestamp).coerceAtLeast(1).toFloat() / 1000f
        lastTimestamp = now

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
                _orientation.postValue(Orientation(roll, pitch, yaw))
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gyroY = event.values[1]
                _gyroY.postValue(gyroY)
                val result = gestureDetector.process(pitch = event.values[1], roll = event.values[0], yaw = event.values[2])
                _gesture.postValue(result)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                _accelY.postValue(event.values[1])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_COLLECTION") startCollection()
        else if (intent?.action == "STOP_COLLECTION") stopCollection()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCollection()
        if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
