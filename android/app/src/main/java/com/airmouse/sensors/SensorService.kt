package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.util.Log
import com.airmouse.domain.model.CalibrationStatus
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Main sensor service that handles all sensor data processing.
 * Manages gyroscope, accelerometer, and magnetometer to provide
 * orientation data and gesture detection for cursor control.
 */
class SensorService(
    private val context: Context,
    private val calibrationHelper: CalibrationHelper,
    private val gestureDetector: EnhancedGestureDetector,
    private val preferences: com.airmouse.utils.PreferencesManager
) : SensorEventListener {

    companion object {
        private const val TAG = "SensorService"
        private const val GYRO_SAMPLES_FOR_STABILITY = 50
        private const val STABILITY_THRESHOLD = 0.05f
    }

    // Sensor Manager
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotationVector: Sensor? = null

    // Sensor fusion
    private val madgwick = MadgwickAHRS(beta = 0.1f)
    private var lastTimestamp = 0L
    private var dt = 0.01f

    // State
    private var isRunning = false
    private var isCalibrated = false
    private var isStable = false
    private var stabilityCounter = 0

    // Raw sensor values (calibrated)
    private var calibratedGyroX = 0f
    private var calibratedGyroY = 0f
    private var calibratedGyroZ = 0f
    private var calibratedAccelX = 0f
    private var calibratedAccelY = 0f
    private var calibratedAccelZ = 0f
    private var calibratedMagX = 0f
    private var calibratedMagY = 0f
    private var calibratedMagZ = 0f

    // Orientation
    private var yaw = 0f      // Rotation around Z (horizontal cursor)
    private var pitch = 0f    // Rotation around Y (not used)
    private var roll = 0f     // Rotation around X (vertical cursor)

    // Stability tracking
    private var lastRoll = 0f
    private var lastYaw = 0f
    private var stableStartTime = 0L

    // Background thread for sensor processing
    private val sensorThread = HandlerThread("SensorThread").apply { start() }
    private val sensorHandler = Handler(sensorThread.looper)

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Callbacks
    var onOrientationChanged: ((roll: Float, yaw: Float) -> Unit)? = null
    var onGestureDetected: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
    var onStabilityChanged: ((isStable: Boolean) -> Unit)? = null
    var onSensorData: ((gyroX: Float, gyroY: Float, gyroZ: Float, accelX: Float, accelY: Float, accelZ: Float) -> Unit)? = null

    // Movement smoothing
    private var filteredRoll = 0f
    private var filteredYaw = 0f
    private val smoothingAlpha = 0.6f

    init {
        initializeSensors()
        loadCalibrationStatus()
    }

    private fun initializeSensors() {
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (gyroscope == null) Log.w(TAG, "Gyroscope not available")
        if (accelerometer == null) Log.w(TAG, "Accelerometer not available")
        if (magnetometer == null) Log.w(TAG, "Magnetometer not available")

        isCalibrated = calibrationHelper.isDeviceCalibrated()
    }

    private fun loadCalibrationStatus() {
        isCalibrated = calibrationHelper.loadCalibrationStatus() == CalibrationStatus.COMPLETED
    }

    /**
     * Start sensor monitoring
     */
    fun start() {
        if (isRunning) return

        val sensorDelay = if (preferences.getBoolean("battery_saver", false)) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }

        gyroscope?.let {
            sensorManager.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Gyroscope registered")
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Accelerometer registered")
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Magnetometer registered")
        }
        rotationVector?.let {
            sensorManager.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Rotation vector registered")
        }

        isRunning = true
        Log.i(TAG, "Sensor service started")
    }

    /**
     * Stop sensor monitoring
     */
    fun stop() {
        if (!isRunning) return

        sensorManager.unregisterListener(this)
        isRunning = false
        Log.i(TAG, "Sensor service stopped")
    }

    /**
     * Change sensor sampling rate (used by battery saver)
     */
    fun setSamplingRate(delay: Int) {
        if (!isRunning) return

        gyroscope?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }
        accelerometer?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }
        magnetometer?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }
        rotationVector?.let { sensorManager.registerListener(this, it, delay, sensorHandler) }

        Log.d(TAG, "Sampling rate changed to $delay")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRunning) return

        val currentTime = System.currentTimeMillis()
        if (lastTimestamp > 0) {
            dt = (currentTime - lastTimestamp) / 1000f
            dt = dt.coerceIn(0.005f, 0.05f)
        }
        lastTimestamp = currentTime

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscope(event.values, dt)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometer(event.values)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                processMagnetometer(event.values)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                processRotationVector(event.values)
            }
        }

        // Run sensor fusion
        if (isCalibrated) {
            madgwick.update(
                calibratedGyroX, calibratedGyroY, calibratedGyroZ,
                calibratedAccelX, calibratedAccelY, calibratedAccelZ,
                calibratedMagX, calibratedMagY, calibratedMagZ,
                dt
            )

            // Get orientation
            roll = madgwick.getRoll()
            yaw = madgwick.getYaw()

            // Apply smoothing
            filteredRoll = smoothingAlpha * roll + (1 - smoothingAlpha) * filteredRoll
            filteredYaw = smoothingAlpha * yaw + (1 - smoothingAlpha) * filteredYaw

            // Check stability
            checkStability()

            // Send orientation to callback
            onOrientationChanged?.invoke(filteredRoll, filteredYaw)

            // Detect gestures
            detectGestures()

            // Send raw data for debug
            onSensorData?.invoke(
                calibratedGyroX, calibratedGyroY, calibratedGyroZ,
                calibratedAccelX, calibratedAccelY, calibratedAccelZ
            )
        }
    }

    private fun processGyroscope(values: FloatArray, dt: Float) {
        // Apply calibration offsets
        calibratedGyroX = calibrationHelper.correctGyro(values[0], 0)
        calibratedGyroY = calibrationHelper.correctGyro(values[1], 1)
        calibratedGyroZ = calibrationHelper.correctGyro(values[2], 2)
    }

    private fun processAccelerometer(values: FloatArray) {
        val corrected = calibrationHelper.correctAccelerometer(values[0], values[1], values[2])
        calibratedAccelX = corrected.first
        calibratedAccelY = corrected.second
        calibratedAccelZ = corrected.third
    }

    private fun processMagnetometer(values: FloatArray) {
        val corrected = calibrationHelper.correctMagnetometer(values[0], values[1], values[2])
        calibratedMagX = corrected.first
        calibratedMagY = corrected.second
        calibratedMagZ = corrected.third
    }

    private fun processRotationVector(values: FloatArray) {
        // Rotation vector gives direct orientation without fusion
        // We still use Madgwick for consistency, but could use this directly
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val directYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val directPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val directRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // Can use direct values for faster response
        // We'll stick with Madgwick for consistency
    }

    private fun checkStability() {
        val rollDelta = abs(roll - lastRoll)
        val yawDelta = abs(yaw - lastYaw)
        val isCurrentlyStable = rollDelta < STABILITY_THRESHOLD && yawDelta < STABILITY_THRESHOLD

        if (isCurrentlyStable) {
            stabilityCounter++
            if (stabilityCounter >= GYRO_SAMPLES_FOR_STABILITY && !isStable) {
                isStable = true
                onStabilityChanged?.invoke(true)
                Log.d(TAG, "Device became stable")
            }
        } else {
            stabilityCounter = 0
            if (isStable) {
                isStable = false
                onStabilityChanged?.invoke(false)
                Log.d(TAG, "Device became unstable")
            }
        }

        lastRoll = roll
        lastYaw = yaw
    }

    private fun detectGestures() {
        // Use gyro Y for click detection (fast rotation around Y axis)
        val gesture = gestureDetector.detect(
            calibratedGyroX, calibratedGyroY, calibratedGyroZ,
            calibratedAccelX, calibratedAccelY, calibratedAccelZ,
            roll
        )

        if (gesture != EnhancedGestureDetector.Gesture.NONE) {
            onGestureDetected?.invoke(gesture)
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
        Log.d(TAG, "Accuracy changed for sensor ${sensor?.name}: $accuracy")
    }

    /**
     * Get current orientation (roll, yaw) in degrees
     */
    fun getCurrentOrientation(): Pair<Float, Float> = Pair(filteredRoll, filteredYaw)

    /**
     * Get raw sensor data
     */
    fun getRawGyro(): Triple<Float, Float, Float> = Triple(calibratedGyroX, calibratedGyroY, calibratedGyroZ)
    fun getRawAccel(): Triple<Float, Float, Float> = Triple(calibratedAccelX, calibratedAccelY, calibratedAccelZ)
    fun getRawMag(): Triple<Float, Float, Float> = Triple(calibratedMagX, calibratedMagY, calibratedMagZ)

    /**
     * Check if device is stable (not moving)
     */
    fun isStable(): Boolean = isStable

    /**
     * Check if calibration is complete
     */
    fun isCalibrated(): Boolean = isCalibrated

    /**
     * Recalibrate sensors
     */
    fun recalibrate() {
        calibrationHelper.resetCalibration()
        isCalibrated = false
        Log.i(TAG, "Calibration reset, please recalibrate")
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        stop()
        sensorThread.quitSafely()
        scope.cancel()
    }
}
