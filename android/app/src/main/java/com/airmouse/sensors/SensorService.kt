package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.airmouse.SensorService as AirMouseSensorService
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class SensorService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calibrationHelper: CalibrationHelper,
    private val gestureDetector: EnhancedGestureDetector,
    private val preferences: PreferencesManager
) : SensorEventListener, AirMouseSensorService {

    companion object {
        private const val TAG = "SensorService"
        private const val GYRO_SAMPLES_FOR_STABILITY = 50
        private const val STABILITY_THRESHOLD = 0.05f
    }

    // ============================================================
    // Sensor Components
    // ============================================================

    private var sensorManager: SensorManager? = null

    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var rotationVector: Sensor? = null

    private val madgwick = MadgwickAHRS(beta = 0.1f)
    private var lastTimestamp = 0L
    private var dt = 0.01f

    // ============================================================
    // State
    // ============================================================

    private var isRunning = false
    private var isCalibrated = false
    private var isStable = false
    private var stabilityCounter = 0

    private var calibratedGyroX = 0f
    private var calibratedGyroY = 0f
    private var calibratedGyroZ = 0f
    private var calibratedAccelX = 0f
    private var calibratedAccelY = 0f
    private var calibratedAccelZ = 0f
    private var calibratedMagX = 0f
    private var calibratedMagY = 0f
    private var calibratedMagZ = 0f

    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f

    private var lastRoll = 0f
    private var lastYaw = 0f

    private val sensorThread = HandlerThread("SensorThread").apply { start() }
    private val sensorHandler = Handler(sensorThread.looper)

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============================================================
    // Callbacks
    // ============================================================

    var onOrientationChanged: ((roll: Float, yaw: Float) -> Unit)? = null
    var onGestureDetected: ((EnhancedGestureDetector.Gesture) -> Unit)? = null
    var onStabilityChanged: ((isStable: Boolean) -> Unit)? = null
    var onSensorData: ((gyroX: Float, gyroY: Float, gyroZ: Float, accelX: Float, accelY: Float, accelZ: Float) -> Unit)? = null

    private var filteredRoll = 0f
    private var filteredYaw = 0f
    private val smoothingAlpha = 0.6f

    // ============================================================
    // Initialization
    // ============================================================

    init {
        Log.d(TAG, "SensorService init")
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        initializeSensors()
        loadCalibrationStatus()
    }

    private fun initializeSensors() {
        val sm = sensorManager ?: return
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVector = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (gyroscope == null) Log.w(TAG, "Gyroscope not available")
        if (accelerometer == null) Log.w(TAG, "Accelerometer not available")
        if (magnetometer == null) Log.w(TAG, "Magnetometer not available")

        isCalibrated = calibrationHelper.isDeviceCalibrated()
    }

    private fun loadCalibrationStatus() {
        isCalibrated = calibrationHelper.loadCalibrationStatus()
    }

    // ============================================================
    // Service Control (AirMouseSensorService interface)
    // ============================================================

    override fun start() {
        if (isRunning) return
        val sm = sensorManager ?: return

        val sensorDelay = if (preferences.getBoolean("battery_saver", false)) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }

        gyroscope?.let {
            sm.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Gyroscope registered")
        }
        accelerometer?.let {
            sm.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Accelerometer registered")
        }
        magnetometer?.let {
            sm.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Magnetometer registered")
        }
        rotationVector?.let {
            sm.registerListener(this, it, sensorDelay, sensorHandler)
            Log.d(TAG, "Rotation vector registered")
        }

        isRunning = true
        Log.i(TAG, "Sensor service started")
    }

    override fun stop() {
        if (!isRunning) return
        
        sensorManager?.unregisterListener(this)
        isRunning = false
        Log.i(TAG, "Sensor service stopped")
    }

    override fun setSamplingRate(delay: Int) {
        if (!isRunning) return
        val sm = sensorManager ?: return

        gyroscope?.let { sm.registerListener(this, it, delay, sensorHandler) }
        accelerometer?.let { sm.registerListener(this, it, delay, sensorHandler) }
        magnetometer?.let { sm.registerListener(this, it, delay, sensorHandler) }
        rotationVector?.let { sm.registerListener(this, it, delay, sensorHandler) }

        Log.d(TAG, "Sampling rate changed to $delay")
    }

    // ============================================================
    // SensorEventListener
    // ============================================================

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
                processGyroscope(event.values)
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

        // Run Madgwick fusion if calibrated
        if (isCalibrated) {
            madgwick.update(
                calibratedGyroX, calibratedGyroY, calibratedGyroZ,
                calibratedAccelX, calibratedAccelY, calibratedAccelZ,
                calibratedMagX, calibratedMagY, calibratedMagZ,
                dt
            )

            roll = madgwick.getRoll()
            yaw = madgwick.getYaw()

            filteredRoll = smoothingAlpha * roll + (1 - smoothingAlpha) * filteredRoll
            filteredYaw = smoothingAlpha * yaw + (1 - smoothingAlpha) * filteredYaw

            checkStability()
            onOrientationChanged?.invoke(filteredRoll, filteredYaw)
            detectGestures()

            onSensorData?.invoke(
                calibratedGyroX, calibratedGyroY, calibratedGyroZ,
                calibratedAccelX, calibratedAccelY, calibratedAccelZ
            )
        }
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
        Log.d(TAG, "Accuracy changed for sensor ${sensor?.name}: $accuracy")
    }

    // ============================================================
    // Processing Methods
    // ============================================================

    private fun processGyroscope(values: FloatArray) {
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
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val directYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val directPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val directRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        if (!isCalibrated) {
            roll = directRoll
            pitch = directPitch
            yaw = directYaw
            filteredRoll = smoothingAlpha * roll + (1 - smoothingAlpha) * filteredRoll
            filteredYaw = smoothingAlpha * yaw + (1 - smoothingAlpha) * filteredYaw
            onOrientationChanged?.invoke(filteredRoll, filteredYaw)
        }
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
        val gesture = gestureDetector.detect(
            calibratedGyroX, calibratedGyroY, calibratedGyroZ,
            calibratedAccelX, calibratedAccelY, calibratedAccelZ,
            roll
        )

        if (gesture != EnhancedGestureDetector.Gesture.NONE) {
            onGestureDetected?.invoke(gesture)
        }
    }

    // ============================================================
    // Public Methods
    // ============================================================

    fun getCurrentOrientation(): Pair<Float, Float> = Pair(filteredRoll, filteredYaw)

    fun getRawGyro(): Triple<Float, Float, Float> = Triple(calibratedGyroX, calibratedGyroY, calibratedGyroZ)
    fun getRawAccel(): Triple<Float, Float, Float> = Triple(calibratedAccelX, calibratedAccelY, calibratedAccelZ)
    fun getRawMag(): Triple<Float, Float, Float> = Triple(calibratedMagX, calibratedMagY, calibratedMagZ)

    fun isStable(): Boolean = isStable
    fun isCalibrated(): Boolean = isCalibrated

    fun recalibrate() {
        calibrationHelper.resetCalibration()
        isCalibrated = false
        Log.i(TAG, "Calibration reset, please recalibrate")
    }

    fun destroy() {
        stop()
        sensorThread.quitSafely()
        scope.cancel()
    }
}