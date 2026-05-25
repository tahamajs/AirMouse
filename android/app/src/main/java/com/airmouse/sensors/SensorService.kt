package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*

class SensorService(
    private val context: Context,
    private val calibrationHelper: CalibrationHelper
) : SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null

    private val madgwick = MadgwickAHRS(beta = 0.1f)
    private var timestamp = 0L

    private var orientationCallback: ((roll: Float, yaw: Float) -> Unit)? = null
    private var gestureCallback: ((MotionDetector.Gesture) -> Unit)? = null
    private val motionDetector = MotionDetector()

    private var isRunning = false

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        isRunning = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        val currentTime = System.currentTimeMillis()
        val dt = if (timestamp == 0L) 0.01f else (currentTime - timestamp) / 1000f
        timestamp = currentTime

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val (ax, ay, az) = calibrationHelper.correctAccelerometer(event.values[0], event.values[1], event.values[2])
                madgwick.updateAccel(ax, ay, az)
                val scroll = motionDetector.detectScroll(ay, dt)
                if (scroll != 0) gestureCallback?.invoke(
                    if (scroll > 0) MotionDetector.Gesture.SCROLL_DOWN else MotionDetector.Gesture.SCROLL_UP
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = calibrationHelper.correctGyro(event.values[0], 0)
                val gy = calibrationHelper.correctGyro(event.values[1], 1)
                val gz = calibrationHelper.correctGyro(event.values[2], 2)
                madgwick.updateGyro(gx, gy, gz, dt)
                if (motionDetector.detectClick(gy, dt)) {
                    gestureCallback?.invoke(MotionDetector.Gesture.CLICK)
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val (mx, my, mz) = calibrationHelper.correctMagnetometer(event.values[0], event.values[1], event.values[2])
                madgwick.updateMag(mx, my, mz)
            }
        }

        val roll = madgwick.getRoll()
        val yaw = madgwick.getYaw()
        orientationCallback?.invoke(roll, yaw)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun setOnOrientationChange(callback: (roll: Float, yaw: Float) -> Unit) {
        orientationCallback = callback
    }

    fun setOnGestureDetected(callback: (MotionDetector.Gesture) -> Unit) {
        gestureCallback = callback
    }
}