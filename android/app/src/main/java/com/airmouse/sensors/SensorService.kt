package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.PreferencesManager
import kotlin.math.abs

class SensorService(
    private val context: Context,
    private val calibrationHelper: CalibrationHelper,
    private val gestureDetector: EnhancedGestureDetector,
    private val preferences: PreferencesManager,
    private val batterySaver: BatterySaver
) : SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private val madgwick = MadgwickAHRS(beta = 0.1f)
    private var timestamp = 0L

    private var orientationCallback: ((roll: Float, yaw: Float) -> Unit)? = null
    private var gestureCallback: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
    private var gyroUpdateCallback: ((Float) -> Unit)? = null
    private var accelUpdateCallback: ((Float) -> Unit)? = null

    private var isRunning = false
    private var lastGyroY = 0f
    private var lastAccelY = 0f

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val delay = if (batterySaver.isLowPowerMode()) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_GAME
        sensorManager.registerListener(this, accelerometer, delay)
        sensorManager.registerListener(this, gyroscope, delay)
        sensorManager.registerListener(this, magnetometer, delay)
        isRunning = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    fun setSamplingRate(delay: Int) {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, delay)
        sensorManager.registerListener(this, gyroscope, delay)
        sensorManager.registerListener(this, magnetometer, delay)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        val now = System.currentTimeMillis()
        val dt = if (timestamp == 0L) 0.01f else (now - timestamp) / 1000f
        timestamp = now

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (ax, ay, az) = calibrationHelper.correctAccelerometer(
                    event.values[0], event.values[1], event.values[2]
                )
                madgwick.updateAccel(ax, ay, az)
                lastAccelY = ay
                accelUpdateCallback?.invoke(ay)
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = calibrationHelper.correctGyro(event.values[0], 0)
                val gy = calibrationHelper.correctGyro(event.values[1], 1)
                val gz = calibrationHelper.correctGyro(event.values[2], 2)
                madgwick.updateGyro(gx, gy, gz, dt)
                lastGyroY = gy
                gyroUpdateCallback?.invoke(gy)

                val roll = madgwick.getRoll()
                val yaw = madgwick.getYaw()
                orientationCallback?.invoke(roll, yaw)

                val gesture = gestureDetector.detect(lastGyroY, lastAccelY, roll)
                if (gesture != EnhancedGestureDetector.Gesture.NONE) {
                    gestureCallback?.invoke(gesture)
                }

                batterySaver.updateMovement(roll, yaw)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val (mx, my, mz) = calibrationHelper.correctMagnetometer(
                    event.values[0], event.values[1], event.values[2]
                )
                madgwick.updateMag(mx, my, mz)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun setOnOrientationChange(callback: (roll: Float, yaw: Float) -> Unit) { orientationCallback = callback }
    fun setOnGestureDetected(callback: (EnhancedGestureDetector.Gesture) -> Unit) { gestureCallback = callback }
    fun setOnGyroUpdate(callback: (Float) -> Unit) { gyroUpdateCallback = callback }
    fun setOnAccelUpdate(callback: (Float) -> Unit) { accelUpdateCallback = callback }
}