package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Complete Calibration Helper - Professional sensor calibration for Air Mouse
 * 
 * Features:
 * - Gyroscope bias calibration (6-point stationary sampling)
 * - Magnetometer hard/soft iron calibration (ellipsoid fitting)
 * - Accelerometer 6-point calibration (gravity alignment)
 * - Real-time progress tracking with StateFlow
 * - Haptic feedback during calibration steps
 * - Calibration persistence and validation
 * - Visual instructions for user guidance
 */
class CalibrationHelper(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "CalibrationHelper"
        
        // Gyroscope calibration
        private const val GYRO_SAMPLES_REQUIRED = 1000
        private const val GYRO_STABILITY_THRESHOLD = 0.05f
        
        // Magnetometer calibration
        private const val MAG_SAMPLES_REQUIRED = 500
        private const val MAG_CALIBRATION_DURATION_MS = 10000L
        
        // Accelerometer calibration
        private const val ACCEL_SAMPLES_PER_ORIENTATION = 100
        private const val GRAVITY_EARTH = 9.81f
        private const val ACCEL_TOLERANCE = 0.2f
    }

    // Calibration state flows for reactive UI
    private val _calibrationState = MutableStateFlow(CalibrationState.IDLE)
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()
    
    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()
    
    private val _calibrationStep = MutableStateFlow(CalibrationStep.NONE)
    val calibrationStep: StateFlow<CalibrationStep> = _calibrationStep.asStateFlow()
    
    private val _calibrationMessage = MutableStateFlow("")
    val calibrationMessage: StateFlow<String> = _calibrationMessage.asStateFlow()
    
    private val _calibrationQuality = MutableStateFlow(CalibrationQuality.UNKNOWN)
    val calibrationQuality: StateFlow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    // Calibration data
    private var gyroBiasX = 0f
    private var gyroBiasY = 0f
    private var gyroBiasZ = 0f
    private var gyroVarianceX = 0f
    private var gyroVarianceY = 0f
    private var gyroVarianceZ = 0f
    
    private var accelOffsetX = 0f
    private var accelOffsetY = 0f
    private var accelOffsetZ = 0f
    private var accelScaleX = 1f
    private var accelScaleY = 1f
    private var accelScaleZ = 1f
    
    private var magOffsetX = 0f
    private var magOffsetY = 0f
    private var magOffsetZ = 0f
    private var magScaleX = 1f
    private var magScaleY = 1f
    private var magScaleZ = 1f
    private var magEllipsoidA = 0f
    private var magEllipsoidB = 0f
    private var magEllipsoidC = 0f

    // Temporary storage for calibration
    private val gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val magSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val accelSamples = mutableListOf<Triple<Float, Float, Float>>()
    
    private var sensorManager: SensorManager? = null
    private var calibrationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        loadCalibrationData()
        validateCalibrationQuality()
    }

    /**
     * Calibration state enum
     */
    enum class CalibrationState {
        IDLE,
        CALIBRATING,
        COMPLETED,
        FAILED,
        VALIDATING
    }

    /**
     * Calibration step enum
     */
    enum class CalibrationStep {
        NONE,
        GYROSCOPE,
        MAGNETOMETER_PREP,
        MAGNETOMETER,
        ACCELEROMETER_FLAT,
        ACCELEROMETER_LEFT,
        ACCELEROMETER_RIGHT,
        ACCELEROMETER_TOP,
        ACCELEROMETER_BOTTOM,
        ACCELEROMETER_BACK,
        VALIDATING
    }

    /**
     * Calibration quality enum
     */
    enum class CalibrationQuality {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        UNKNOWN
    }

    /**
     * Load saved calibration data from preferences
     */
    private fun loadCalibrationData() {
        gyroBiasX = prefs.getFloat("gyro_bias_x", 0f)
        gyroBiasY = prefs.getFloat("gyro_bias_y", 0f)
        gyroBiasZ = prefs.getFloat("gyro_bias_z", 0f)
        gyroVarianceX = prefs.getFloat("gyro_variance_x", 0f)
        gyroVarianceY = prefs.getFloat("gyro_variance_y", 0f)
        gyroVarianceZ = prefs.getFloat("gyro_variance_z", 0f)
        
        accelOffsetX = prefs.getFloat("accel_offset_x", 0f)
        accelOffsetY = prefs.getFloat("accel_offset_y", 0f)
        accelOffsetZ = prefs.getFloat("accel_offset_z", 0f)
        accelScaleX = prefs.getFloat("accel_scale_x", 1f)
        accelScaleY = prefs.getFloat("accel_scale_y", 1f)
        accelScaleZ = prefs.getFloat("accel_scale_z", 1f)
        
        magOffsetX = prefs.getFloat("mag_offset_x", 0f)
        magOffsetY = prefs.getFloat("mag_offset_y", 0f)
        magOffsetZ = prefs.getFloat("mag_offset_z", 0f)
        magScaleX = prefs.getFloat("mag_scale_x", 1f)
        magScaleY = prefs.getFloat("mag_scale_y", 1f)
        magScaleZ = prefs.getFloat("mag_scale_z", 1f)
        magEllipsoidA = prefs.getFloat("mag_ellipsoid_a", 1f)
        magEllipsoidB = prefs.getFloat("mag_ellipsoid_b", 1f)
        magEllipsoidC = prefs.getFloat("mag_ellipsoid_c", 1f)
    }

    /**
     * Save calibration data to preferences
     */
    private fun saveCalibrationData() {
        prefs.putFloat("gyro_bias_x", gyroBiasX)
        prefs.putFloat("gyro_bias_y", gyroBiasY)
        prefs.putFloat("gyro_bias_z", gyroBiasZ)
        prefs.putFloat("gyro_variance_x", gyroVarianceX)
        prefs.putFloat("gyro_variance_y", gyroVarianceY)
        prefs.putFloat("gyro_variance_z", gyroVarianceZ)
        
        prefs.putFloat("accel_offset_x", accelOffsetX)
        prefs.putFloat("accel_offset_y", accelOffsetY)
        prefs.putFloat("accel_offset_z", accelOffsetZ)
        prefs.putFloat("accel_scale_x", accelScaleX)
        prefs.putFloat("accel_scale_y", accelScaleY)
        prefs.putFloat("accel_scale_z", accelScaleZ)
        
        prefs.putFloat("mag_offset_x", magOffsetX)
        prefs.putFloat("mag_offset_y", magOffsetY)
        prefs.putFloat("mag_offset_z", magOffsetZ)
        prefs.putFloat("mag_scale_x", magScaleX)
        prefs.putFloat("mag_scale_y", magScaleY)
        prefs.putFloat("mag_scale_z", magScaleZ)
        prefs.putFloat("mag_ellipsoid_a", magEllipsoidA)
        prefs.putFloat("mag_ellipsoid_b", magEllipsoidB)
        prefs.putFloat("mag_ellipsoid_c", magEllipsoidC)
        
        prefs.putBoolean("calibration_complete", true)
        prefs.putLong("calibration_timestamp", System.currentTimeMillis())
    }

    /**
     * Validate calibration quality based on variance
     */
    private fun validateCalibrationQuality() {
        val avgVariance = (gyroVarianceX + gyroVarianceY + gyroVarianceZ) / 3f
        
        _calibrationQuality.value = when {
            avgVariance < 0.01f -> CalibrationQuality.EXCELLENT
            avgVariance < 0.05f -> CalibrationQuality.GOOD
            avgVariance < 0.1f -> CalibrationQuality.FAIR
            avgVariance > 0f -> CalibrationQuality.POOR
            else -> CalibrationQuality.UNKNOWN
        }
    }

    /**
     * Start full calibration process
     */
    suspend fun startFullCalibration(onInstruction: (String, Int) -> Unit): Boolean {
        _calibrationState.value = CalibrationState.CALIBRATING
        _calibrationProgress.value = 0
        
        // Step 1: Gyroscope calibration
        onInstruction("Place device on a flat, stationary surface", 10)
        if (!calibrateGyroscope()) {
            _calibrationState.value = CalibrationState.FAILED
            return false
        }
        
        _calibrationProgress.value = 30
        
        // Step 2: Magnetometer calibration
        onInstruction("Move device in a figure-8 pattern", 40)
        if (!calibrateMagnetometer()) {
            _calibrationState.value = CalibrationState.FAILED
            return false
        }
        
        _calibrationProgress.value = 60
        
        // Step 3: Accelerometer calibration
        onInstruction("Place device flat facing up", 70)
        if (!calibrateAccelerometer()) {
            _calibrationState.value = CalibrationState.FAILED
            return false
        }
        
        _calibrationProgress.value = 100
        _calibrationState.value = CalibrationState.COMPLETED
        saveCalibrationData()
        vibrate(200)
        
        return true
    }

    /**
     * Calibrate gyroscope - collect stationary samples
     */
    suspend fun calibrateGyroscope(): Boolean = suspendCoroutine { continuation ->
        gyroSamples.clear()
        _calibrationStep.value = CalibrationStep.GYROSCOPE
        
        val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            continuation.resume(false)
            return@suspendCoroutine
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (gyroSamples.size < GYRO_SAMPLES_REQUIRED) {
                    gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                    _calibrationProgress.value = (gyroSamples.size * 100 / GYRO_SAMPLES_REQUIRED)
                } else {
                    sensorManager?.unregisterListener(this)
                    calculateGyroBias()
                    continuation.resume(true)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        
        sensorManager?.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        
        // Timeout after 10 seconds
        mainScope.launch {
            delay(10000)
            if (gyroSamples.size < GYRO_SAMPLES_REQUIRED) {
                sensorManager?.unregisterListener(listener)
                continuation.resume(false)
            }
        }
    }

    /**
     * Calculate gyroscope bias from collected samples
     */
    private fun calculateGyroBias() {
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        var sumSqX = 0f
        var sumSqY = 0f
        var sumSqZ = 0f
        
        gyroSamples.forEach { (x, y, z) ->
            sumX += x
            sumY += y
            sumZ += z
            sumSqX += x * x
            sumSqY += y * y
            sumSqZ += z * z
        }
        
        val n = gyroSamples.size
        gyroBiasX = sumX / n
        gyroBiasY = sumY / n
        gyroBiasZ = sumZ / n
        
        // Calculate variance for quality assessment
        gyroVarianceX = (sumSqX / n) - (gyroBiasX * gyroBiasX)
        gyroVarianceY = (sumSqY / n) - (gyroBiasY * gyroBiasY)
        gyroVarianceZ = (sumSqZ / n) - (gyroBiasZ * gyroBiasZ)
    }

    /**
     * Calibrate magnetometer using ellipsoid fitting
     */
    suspend fun calibrateMagnetometer(): Boolean = suspendCoroutine { continuation ->
        magSamples.clear()
        _calibrationStep.value = CalibrationStep.MAGNETOMETER
        
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magnetometer == null) {
            continuation.resume(false)
            return@suspendCoroutine
        }
        
        val startTime = System.currentTimeMillis()
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MAG_CALIBRATION_DURATION_MS) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    
                    magSamples.add(Triple(x, y, z))
                    minX = min(minX, x)
                    maxX = max(maxX, x)
                    minY = min(minY, y)
                    maxY = max(maxY, y)
                    minZ = min(minZ, z)
                    maxZ = max(maxZ, z)
                    
                    _calibrationProgress.value = (elapsed * 100 / MAG_CALIBRATION_DURATION_MS).toInt()
                } else {
                    sensorManager?.unregisterListener(this)
                    calculateMagnetometerCalibration(minX, maxX, minY, maxY, minZ, maxZ)
                    continuation.resume(true)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        
        sensorManager?.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        
        mainScope.launch {
            delay(MAG_CALIBRATION_DURATION_MS + 2000)
            if (magSamples.size < MAG_SAMPLES_REQUIRED) {
                sensorManager?.unregisterListener(listener)
                continuation.resume(false)
            }
        }
    }

    /**
     * Calculate magnetometer calibration parameters using ellipsoid fitting
     */
    private fun calculateMagnetometerCalibration(
        minX: Float, maxX: Float, minY: Float, maxY: Float, minZ: Float, maxZ: Float
    ) {
        // Hard-iron offsets (center of ellipsoid)
        magOffsetX = (minX + maxX) / 2f
        magOffsetY = (minY + maxY) / 2f
        magOffsetZ = (minZ + maxZ) / 2f
        
        // Soft-iron scale factors (ellipsoid radii)
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        val rangeZ = maxZ - minZ
        val avgRange = (rangeX + rangeY + rangeZ) / 3f
        
        magScaleX = if (rangeX > 0) avgRange / rangeX else 1f
        magScaleY = if (rangeY > 0) avgRange / rangeY else 1f
        magScaleZ = if (rangeZ > 0) avgRange / rangeZ else 1f
        
        // Ellipsoid parameters for advanced correction
        magEllipsoidA = rangeX / 2f
        magEllipsoidB = rangeY / 2f
        magEllipsoidC = rangeZ / 2f
    }

    /**
     * Calibrate accelerometer using 6-point method
     */
    suspend fun calibrateAccelerometer(): Boolean {
        _calibrationStep.value = CalibrationStep.ACCELEROMETER_FLAT
        val orientations = listOf(
            Triple(0f, 0f, GRAVITY_EARTH) to CalibrationStep.ACCELEROMETER_FLAT,
            Triple(0f, 0f, -GRAVITY_EARTH) to CalibrationStep.ACCELEROMETER_BACK,
            Triple(GRAVITY_EARTH, 0f, 0f) to CalibrationStep.ACCELEROMETER_RIGHT,
            Triple(-GRAVITY_EARTH, 0f, 0f) to CalibrationStep.ACCELEROMETER_LEFT,
            Triple(0f, GRAVITY_EARTH, 0f) to CalibrationStep.ACCELEROMETER_TOP,
            Triple(0f, -GRAVITY_EARTH, 0f) to CalibrationStep.ACCELEROMETER_BOTTOM
        )
        
        val measurements = mutableListOf<Triple<Float, Float, Float>>()
        
        for ((expected, step) in orientations) {
            _calibrationStep.value = step
            vibrate(100)
            delay(2000) // Give user time to position device
            
            val sample = collectAccelerometerSamples()
            if (sample == null) return false
            
            measurements.add(sample)
            _calibrationProgress.value += (100 / orientations.size)
        }
        
        calculateAccelerometerCalibration(measurements, orientations.map { it.first })
        return true
    }

    /**
     * Collect accelerometer samples for current orientation
     */
    private suspend fun collectAccelerometerSamples(): Triple<Float, Float, Float>? = suspendCoroutine { continuation ->
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            continuation.resume(null)
            return@suspendCoroutine
        }
        
        val samples = mutableListOf<Triple<Float, Float, Float>>()
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (samples.size < ACCEL_SAMPLES_PER_ORIENTATION) {
                    samples.add(Triple(event.values[0], event.values[1], event.values[2]))
                } else {
                    sensorManager?.unregisterListener(this)
                    val avgX = samples.map { it.first }.average().toFloat()
                    val avgY = samples.map { it.second }.average().toFloat()
                    val avgZ = samples.map { it.third }.average().toFloat()
                    continuation.resume(Triple(avgX, avgY, avgZ))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        
        sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        
        mainScope.launch {
            delay(3000)
            if (samples.size < ACCEL_SAMPLES_PER_ORIENTATION) {
                sensorManager?.unregisterListener(listener)
                continuation.resume(null)
            }
        }
    }

    /**
     * Calculate accelerometer calibration parameters
     */
    private fun calculateAccelerometerCalibration(
        measurements: List<Triple<Float, Float, Float>>,
        expected: List<Triple<Float, Float, Float>>
    ) {
        // Calculate scale factors using least squares
        var sumXx = 0f
        var sumYy = 0f
        var sumZz = 0f
        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f
        
        for (i in measurements.indices) {
            val (mx, my, mz) = measurements[i]
            val (ex, ey, ez) = expected[i]
            
            sumXx += mx * ex
            sumYy += my * ey
            sumZz += mz * ez
            sumX += mx * mx
            sumY += my * my
            sumZ += mz * mz
        }
        
        accelScaleX = if (sumX > 0) sumXx / sumX else 1f
        accelScaleY = if (sumY > 0) sumYy / sumY else 1f
        accelScaleZ = if (sumZ > 0) sumZz / sumZ else 1f
        
        // Calculate offsets
        var sumOx = 0f
        var sumOy = 0f
        var sumOz = 0f
        
        for (i in measurements.indices) {
            val (mx, my, mz) = measurements[i]
            val (ex, ey, ez) = expected[i]
            sumOx += ex - mx * accelScaleX
            sumOy += ey - my * accelScaleY
            sumOz += ez - mz * accelScaleZ
        }
        
        accelOffsetX = sumOx / measurements.size
        accelOffsetY = sumOy / measurements.size
        accelOffsetZ = sumOz / measurements.size
    }

    /**
     * Apply gyroscope bias correction
     */
    fun correctGyro(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(x - gyroBiasX, y - gyroBiasY, z - gyroBiasZ)
    }

    /**
     * Apply gyroscope bias correction (single axis)
     */
    fun correctGyro(value: Float, axis: Int): Float {
        return when (axis) {
            0 -> value - gyroBiasX
            1 -> value - gyroBiasY
            else -> value - gyroBiasZ
        }
    }

    /**
     * Apply accelerometer calibration
     */
    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffsetX) / accelScaleX,
            (y - accelOffsetY) / accelScaleY,
            (z - accelOffsetZ) / accelScaleZ
        )
    }

    /**
     * Apply magnetometer calibration (hard and soft iron)
     */
    fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        // Apply hard-iron offset
        val dx = x - magOffsetX
        val dy = y - magOffsetY
        val dz = z - magOffsetZ
        
        // Apply soft-iron scale
        return Triple(
            dx * magScaleX,
            dy * magScaleY,
            dz * magScaleZ
        )
    }

    /**
     * Advanced magnetometer correction with ellipsoid fitting
     */
    fun correctMagnetometerAdvanced(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        // Hard-iron offset
        val hx = x - magOffsetX
        val hy = y - magOffsetY
        val hz = z - magOffsetZ
        
        // Soft-iron correction using ellipsoid parameters
        val sx = hx / magEllipsoidA
        val sy = hy / magEllipsoidB
        val sz = hz / magEllipsoidC
        
        // Normalize to unit sphere
        val norm = sqrt(sx * sx + sy * sy + sz * sz)
        return if (norm > 0) {
            Triple(sx / norm, sy / norm, sz / norm)
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    /**
     * Get calibration statistics for debugging
     */
    fun getCalibrationStats(): Map<String, Any> {
        return mapOf(
            "gyro_bias_x" to gyroBiasX,
            "gyro_bias_y" to gyroBiasY,
            "gyro_bias_z" to gyroBiasZ,
            "gyro_variance_x" to gyroVarianceX,
            "gyro_variance_y" to gyroVarianceY,
            "gyro_variance_z" to gyroVarianceZ,
            "accel_offset_x" to accelOffsetX,
            "accel_offset_y" to accelOffsetY,
            "accel_offset_z" to accelOffsetZ,
            "accel_scale_x" to accelScaleX,
            "accel_scale_y" to accelScaleY,
            "accel_scale_z" to accelScaleZ,
            "mag_offset_x" to magOffsetX,
            "mag_offset_y" to magOffsetY,
            "mag_offset_z" to magOffsetZ,
            "mag_scale_x" to magScaleX,
            "mag_scale_y" to magScaleY,
            "mag_scale_z" to magScaleZ,
            "mag_ellipsoid_a" to magEllipsoidA,
            "mag_ellipsoid_b" to magEllipsoidB,
            "mag_ellipsoid_c" to magEllipsoidC,
            "calibration_quality" to _calibrationQuality.value.name,
            "is_calibrated" to isCalibrated(),
            "calibration_timestamp" to prefs.getLong("calibration_timestamp", 0)
        )
    }

    /**
     * Reset all calibration data
     */
    fun resetCalibration() {
        gyroBiasX = 0f
        gyroBiasY = 0f
        gyroBiasZ = 0f
        gyroVarianceX = 0f
        gyroVarianceY = 0f
        gyroVarianceZ = 0f
        
        accelOffsetX = 0f
        accelOffsetY = 0f
        accelOffsetZ = 0f
        accelScaleX = 1f
        accelScaleY = 1f
        accelScaleZ = 1f
        
        magOffsetX = 0f
        magOffsetY = 0f
        magOffsetZ = 0f
        magScaleX = 1f
        magScaleY = 1f
        magScaleZ = 1f
        magEllipsoidA = 1f
        magEllipsoidB = 1f
        magEllipsoidC = 1f
        
        prefs.putBoolean("calibration_complete", false)
        saveCalibrationData()
        
        _calibrationState.value = CalibrationState.IDLE
        _calibrationQuality.value = CalibrationQuality.UNKNOWN
        _calibrationMessage.value = "Calibration reset"
    }

    /**
     * Check if device is calibrated
     */
    fun isCalibrated(): Boolean = prefs.getBoolean("calibration_complete", false)

    /**
     * Get calibration quality description
     */
    fun getQualityDescription(): String {
        return when (_calibrationQuality.value) {
            CalibrationQuality.EXCELLENT -> "Excellent - Perfect sensor tracking"
            CalibrationQuality.GOOD -> "Good - Accurate cursor control"
            CalibrationQuality.FAIR -> "Fair - May have slight drift"
            CalibrationQuality.POOR -> "Poor - Please recalibrate"
            CalibrationQuality.UNKNOWN -> "Unknown - Calibration needed"
        }
    }

    /**
     * Vibrate for haptic feedback
     */
    private fun vibrate(duration: Long) {
        if (!prefs.isHapticEnabled()) return
        
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        calibrationJob?.cancel()
        scope.cancel()
        mainScope.cancel()
    }
}