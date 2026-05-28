package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Trace
import com.airmouse.utils.BatterySaver
import com.airmouse.utils.PreferencesManager

class SensorService(
    private val context: Context,
    private val calibrationHelper: CalibrationHelper,
    private val gestureDetector: EnhancedGestureDetector,
    private val preferences: PreferencesManager,
    private val batterySaver: BatterySaver
) : SensorEventListener {
    fun setSamplingRate(delay: Int) {
        if (!isRunning) return
        val handler = sensorHandler ?: return
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, accelerometer, delay, handler)
        sensorManager.registerListener(this, gyroscope, delay, handler)
        sensorManager.registerListener(this, magnetometer, delay, handler)
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val madgwick = MadgwickAHRS(beta = 0.1f)
    private var lastTimestamp = 0L

    private var orientationCallback: ((roll: Float, yaw: Float) -> Unit)? = null
    private var gestureCallback: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
    private var gyroUpdateCallback: ((Float) -> Unit)? = null
    private var accelUpdateCallback: ((Float) -> Unit)? = null

    private var isRunning = false
    private var lastGyroY = 0f
    private var lastAccelY = 0f

    fun start() {
        if (isRunning) return
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorThread = HandlerThread("AirMouseSensorThread").also { it.start() }
        sensorHandler = Handler(sensorThread!!.looper)

        val delay = if (batterySaver.isLowPowerMode()) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_GAME
        sensorManager.registerListener(this, accelerometer, delay, sensorHandler)
        sensorManager.registerListener(this, gyroscope, delay, sensorHandler)
        sensorManager.registerListener(this, magnetometer, delay, sensorHandler)
        isRunning = true
        lastTimestamp = 0L
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        Trace.beginSection(sensorTraceName(event.sensor.type))
        try {
            val dt = if (lastTimestamp == 0L) 0.01f else (event.timestamp - lastTimestamp) * 1e-9f
            lastTimestamp = event.timestamp

            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val (ax, ay, az) = calibrationHelper.correctAccelerometer(
                        event.values[0], event.values[1], event.values[2]
                    )
                    Trace.beginSection("MadgwickAccelUpdate")
                    try {
                        madgwick.updateAccel(ax, ay, az)
                    } finally {
                        Trace.endSection()
                    }
                    lastAccelY = ay
                    accelUpdateCallback?.invoke(ay)
                }
                Sensor.TYPE_GYROSCOPE -> {
                    val gx = calibrationHelper.correctGyro(event.values[0], 0)
                    val gy = calibrationHelper.correctGyro(event.values[1], 1)
                    val gz = calibrationHelper.correctGyro(event.values[2], 2)
                    Trace.beginSection("MadgwickGyroUpdate")
                    try {
                        madgwick.updateGyro(gx, gy, gz, dt)
                    } finally {
                        Trace.endSection()
                    }
                    lastGyroY = gy
                    gyroUpdateCallback?.invoke(gy)

                    Trace.beginSection("AirMouseOrientation")
                    val roll: Float
                    val yaw: Float
                    try {
                        roll = madgwick.getRoll()
                        yaw = madgwick.getYaw()
                        orientationCallback?.invoke(roll, yaw)
                    } finally {
                        Trace.endSection()
                    }

                    Trace.beginSection("AirMouseGestureDetection")
                    try {
                        val gesture = gestureDetector.detect(lastGyroY, lastAccelY, roll)
                        if (gesture != EnhancedGestureDetector.Gesture.NONE) {
                            gestureCallback?.invoke(gesture)
                        }
                    } finally {
                        Trace.endSection()
                    }
                    batterySaver.updateMovement(roll, yaw)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    val (mx, my, mz) = calibrationHelper.correctMagnetometer(
                        event.values[0], event.values[1], event.values[2]
                    )
                    Trace.beginSection("MadgwickMagUpdate")
                    try {
                        madgwick.updateMag(mx, my, mz)
                    } finally {
                        Trace.endSection()
                    }
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun setOnOrientationChange(callback: (roll: Float, yaw: Float) -> Unit) { orientationCallback = callback }
    fun setOnGestureDetected(callback: (EnhancedGestureDetector.Gesture) -> Unit) { gestureCallback = callback }
    fun setOnGyroUpdate(callback: (Float) -> Unit) { gyroUpdateCallback = callback }
    fun setOnAccelUpdate(callback: (Float) -> Unit) { accelUpdateCallback = callback }

    private fun sensorTraceName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "AirMouseSensorAccelerometer"
            Sensor.TYPE_GYROSCOPE -> "AirMouseSensorGyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "AirMouseSensorMagnetometer"
            else -> "AirMouseSensorOther"
        }
    }
}
