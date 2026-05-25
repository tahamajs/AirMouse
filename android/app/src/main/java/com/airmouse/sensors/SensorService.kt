package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*

/**
 * Central service that manages raw sensors, performs sensor fusion (Madgwick),
 * and provides orientation updates and gesture events to the UI layer.
 */
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

    // Callbacks for UI (MainActivity)
    private var orientationCallback: ((roll: Float, yaw: Float) -> Unit)? = null
    private var gestureCallback: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
    private var gyroUpdateCallback: ((Float) -> Unit)? = null      // for debug overlay
    private var accelUpdateCallback: ((Float) -> Unit)? = null     // for debug overlay

    private var isRunning = false
    private var currentGyroY = 0f
    private var currentAccelY = 0f

    fun start() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val samplingPeriod = if (batterySaver.isLowPowerMode()) {
            SensorManager.SENSOR_DELAY_NORMAL   // 20 Hz
        } else {
            SensorManager.SENSOR_DELAY_GAME      // 50 Hz
        }

        sensorManager.registerListener(this, accelerometer, samplingPeriod)
        sensorManager.registerListener(this, gyroscope, samplingPeriod)
        sensorManager.registerListener(this, magnetometer, samplingPeriod)
        isRunning = true
    }

    /**
     * Change sampling rate dynamically (used by BatterySaver)
     */
    fun setSamplingRate(delay: Int) {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, delay)
        sensorManager.registerListener(this, gyroscope, delay)
        sensorManager.registerListener(this, magnetometer, delay)
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
                val (ax, ay, az) = calibrationHelper.correctAccelerometer(
                    event.values[0], event.values[1], event.values[2]
                )
                madgwick.updateAccel(ax, ay, az)
                currentAccelY = ay
                accelUpdateCallback?.invoke(ay)

                // Scroll detection (uses EnhancedGestureDetector)
                val scrollDir = gestureDetector.detectScroll(ay, dt)
                when (scrollDir) {
                    1 -> gestureCallback?.invoke(EnhancedGestureDetector.Gesture.SCROLL_DOWN)
                    -1 -> gestureCallback?.invoke(EnhancedGestureDetector.Gesture.SCROLL_UP)
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = calibrationHelper.correctGyro(event.values[0], 0)
                val gy = calibrationHelper.correctGyro(event.values[1], 1)
                val gz = calibrationHelper.correctGyro(event.values[2], 2)
                madgwick.updateGyro(gx, gy, gz, dt)
                currentGyroY = gy
                gyroUpdateCallback?.invoke(gy)

                // Click / double-click detection
                if (gestureDetector.detectClick(gy, dt)) {
                    gestureCallback?.invoke(EnhancedGestureDetector.Gesture.CLICK)
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val (mx, my, mz) = calibrationHelper.correctMagnetometer(
                    event.values[0], event.values[1], event.values[2]
                )
                madgwick.updateMag(mx, my, mz)
            }
        }

        // Right‑click detection (uses roll from fusion)
        val roll = madgwick.getRoll()
        val yaw = madgwick.getYaw()
        if (gestureDetector.detectRightClick(roll, dt)) {
            gestureCallback?.invoke(EnhancedGestureDetector.Gesture.RIGHT_CLICK)
        }

        orientationCallback?.invoke(roll, yaw)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    // Callback setters
    fun setOnOrientationChange(callback: (roll: Float, yaw: Float) -> Unit) {
        orientationCallback = callback
    }

    fun setOnGestureDetected(callback: (EnhancedGestureDetector.Gesture) -> Unit) {
        gestureCallback = callback
    }

    fun setOnGyroUpdate(callback: (Float) -> Unit) {
        gyroUpdateCallback = callback
    }

    fun setOnAccelUpdate(callback: (Float) -> Unit) {
        accelUpdateCallback = callback
    }

    // For debug overlay (if needed directly)
    fun getCurrentGyroY(): Float = currentGyroY
    fun getCurrentAccelY(): Float = currentAccelY
}