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

class CalibrationHelper(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val GYRO_SAMPLES_NEEDED = 500
        private const val MAG_SAMPLES_NEEDED = 300
        private const val ACCEL_SAMPLES_PER_POSE = 50
        private const val STABILITY_THRESHOLD = 0.05f
        private const val GRAVITY = 9.81f
    }

    // State flows for UI observation
    private val _state = MutableStateFlow(CalibrationState.IDLE)
    val state: StateFlow<CalibrationState> = _state.asStateFlow()
    
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()
    
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    
    private val _currentOrientation = MutableStateFlow(Orientation(0f, 0f, 0f))
    val currentOrientation: StateFlow<Orientation> = _currentOrientation.asStateFlow()
    
    private val _quality = MutableStateFlow(CalibrationQuality.UNKNOWN)
    val quality: StateFlow<CalibrationQuality> = _quality.asStateFlow()

    // Calibration data
    private var gyroBias = FloatArray(3)
    private var magOffset = FloatArray(3)
    private var magScale = FloatArray(3)
    private var accelOffset = FloatArray(3)
    private var accelScale = FloatArray(3)
    
    // Temporary storage
    private val gyroSamples = mutableListOf<FloatArray>()
    private val magSamples = mutableListOf<FloatArray>()
    private val accelSamples = mutableListOf<FloatArray>()
    
    private var sensorManager: SensorManager? = null
    private var calibrationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    enum class CalibrationState { IDLE, CALIBRATING_GYRO, CALIBRATING_MAG, CALIBRATING_ACCEL, COMPLETED, FAILED }
    enum class CalibrationQuality { EXCELLENT, GOOD, FAIR, POOR, UNKNOWN }
    
    data class Orientation(val roll: Float, val pitch: Float, val yaw: Float)

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        loadCalibrationData()
    }

    private fun loadCalibrationData() {
        gyroBias = floatArrayOf(
            prefs.getFloat("gyro_bias_x", 0f),
            prefs.getFloat("gyro_bias_y", 0f),
            prefs.getFloat("gyro_bias_z", 0f)
        )
        magOffset = floatArrayOf(
            prefs.getFloat("mag_offset_x", 0f),
            prefs.getFloat("mag_offset_y", 0f),
            prefs.getFloat("mag_offset_z", 0f)
        )
        magScale = floatArrayOf(
            prefs.getFloat("mag_scale_x", 1f),
            prefs.getFloat("mag_scale_y", 1f),
            prefs.getFloat("mag_scale_z", 1f)
        )
        accelOffset = floatArrayOf(
            prefs.getFloat("accel_offset_x", 0f),
            prefs.getFloat("accel_offset_y", 0f),
            prefs.getFloat("accel_offset_z", 0f)
        )
        accelScale = floatArrayOf(
            prefs.getFloat("accel_scale_x", 1f),
            prefs.getFloat("accel_scale_y", 1f),
            prefs.getFloat("accel_scale_z", 1f)
        )
    }

    private fun saveCalibrationData() {
        prefs.putFloat("gyro_bias_x", gyroBias[0])
        prefs.putFloat("gyro_bias_y", gyroBias[1])
        prefs.putFloat("gyro_bias_z", gyroBias[2])
        prefs.putFloat("mag_offset_x", magOffset[0])
        prefs.putFloat("mag_offset_y", magOffset[1])
        prefs.putFloat("mag_offset_z", magOffset[2])
        prefs.putFloat("mag_scale_x", magScale[0])
        prefs.putFloat("mag_scale_y", magScale[1])
        prefs.putFloat("mag_scale_z", magScale[2])
        prefs.putFloat("accel_offset_x", accelOffset[0])
        prefs.putFloat("accel_offset_y", accelOffset[1])
        prefs.putFloat("accel_offset_z", accelOffset[2])
        prefs.putFloat("accel_scale_x", accelScale[0])
        prefs.putFloat("accel_scale_y", accelScale[1])
        prefs.putFloat("accel_scale_z", accelScale[2])
        prefs.putBoolean("calibration_complete", true)
    }

    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean = suspendCancellableCoroutine { continuation ->
        _state.value = CalibrationState.CALIBRATING_GYRO
        _message.value = "Place device on flat surface"
        gyroSamples.clear()
        
        val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroscope == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                gyroSamples.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                onProgress(gyroSamples.size * 100 / GYRO_SAMPLES_NEEDED)
                _progress.value = gyroSamples.size * 100 / GYRO_SAMPLES_NEEDED
                
                if (gyroSamples.size >= GYRO_SAMPLES_NEEDED) {
                    sensorManager?.unregisterListener(this)
                    calculateGyroBias()
                    saveCalibrationData()
                    _state.value = CalibrationState.COMPLETED
                    continuation.resume(true)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        
        sensorManager?.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        
        scope.launch {
            delay(10000)
            if (gyroSamples.size < GYRO_SAMPLES_NEEDED) {
                sensorManager?.unregisterListener(listener)
                _state.value = CalibrationState.FAILED
                continuation.resume(false)
            }
        }
    }

    private fun calculateGyroBias() {
        var sumX = 0f; var sumY = 0f; var sumZ = 0f
        var sumSqX = 0f; var sumSqY = 0f; var sumSqZ = 0f
        
        gyroSamples.forEach { (x, y, z) ->
            sumX += x; sumY += y; sumZ += z
            sumSqX += x * x; sumSqY += y * y; sumSqZ += z * z
        }
        
        val n = gyroSamples.size
        gyroBias[0] = sumX / n
        gyroBias[1] = sumY / n
        gyroBias[2] = sumZ / n
        
        val varX = (sumSqX / n) - (gyroBias[0] * gyroBias[0])
        val varY = (sumSqY / n) - (gyroBias[1] * gyroBias[1])
        val varZ = (sumSqZ / n) - (gyroBias[2] * gyroBias[2])
        val avgVar = (varX + varY + varZ) / 3f
        
        _quality.value = when {
            avgVar < 0.01f -> CalibrationQuality.EXCELLENT
            avgVar < 0.05f -> CalibrationQuality.GOOD
            avgVar < 0.1f -> CalibrationQuality.FAIR
            else -> CalibrationQuality.POOR
        }
        vibrate(200)
    }

    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean = suspendCancellableCoroutine { continuation ->
        _state.value = CalibrationState.CALIBRATING_MAG
        _message.value = "Move device in figure-8 pattern"
        magSamples.clear()
        
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magnetometer == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE
        val startTime = System.currentTimeMillis()
        val duration = 8000L
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < duration) {
                    val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                    magSamples.add(floatArrayOf(x, y, z))
                    minX = min(minX, x); maxX = max(maxX, x)
                    minY = min(minY, y); maxY = max(maxY, y)
                    minZ = min(minZ, z); maxZ = max(maxZ, z)
                    onProgress((elapsed * 100 / duration).toInt())
                    _progress.value = (elapsed * 100 / duration).toInt()
                } else {
                    sensorManager?.unregisterListener(this)
                    calculateMagnetometerCalibration(minX, maxX, minY, maxY, minZ, maxZ)
                    saveCalibrationData()
                    _state.value = CalibrationState.COMPLETED
                    continuation.resume(true)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        
        sensorManager?.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)
        
        scope.launch {
            delay(duration + 2000)
            if (magSamples.size < MAG_SAMPLES_NEEDED) {
                sensorManager?.unregisterListener(listener)
                _state.value = CalibrationState.FAILED
                continuation.resume(false)
            }
        }
    }

    private fun calculateMagnetometerCalibration(
        minX: Float, maxX: Float, minY: Float, maxY: Float, minZ: Float, maxZ: Float
    ) {
        magOffset[0] = (minX + maxX) / 2f
        magOffset[1] = (minY + maxY) / 2f
        magOffset[2] = (minZ + maxZ) / 2f
        
        val rangeX = maxX - minX
        val rangeY = maxY - minY
        val rangeZ = maxZ - minZ
        val avgRange = (rangeX + rangeY + rangeZ) / 3f
        
        magScale[0] = if (rangeX > 0) avgRange / rangeX else 1f
        magScale[1] = if (rangeY > 0) avgRange / rangeY else 1f
        magScale[2] = if (rangeZ > 0) avgRange / rangeZ else 1f
        vibrate(200)
    }

    suspend fun calibrateAccelerometer(
        orientation: Int,
        onInstruction: (String) -> Unit
    ): Boolean {
        val orientations = listOf(
            "Place device flat facing UP" to floatArrayOf(0f, 0f, GRAVITY),
            "Place device flat facing DOWN" to floatArrayOf(0f, 0f, -GRAVITY),
            "Place device on LEFT side" to floatArrayOf(GRAVITY, 0f, 0f),
            "Place device on RIGHT side" to floatArrayOf(-GRAVITY, 0f, 0f),
            "Place device standing UP" to floatArrayOf(0f, GRAVITY, 0f),
            "Place device standing DOWN" to floatArrayOf(0f, -GRAVITY, 0f)
        )
        
        if (orientation >= orientations.size) return true
        
        val (instruction, expected) = orientations[orientation]
        _state.value = CalibrationState.CALIBRATING_ACCEL
        _message.value = instruction
        onInstruction(instruction)
        accelSamples.clear()
        
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) return false
        
        return suspendCancellableCoroutine { continuation ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (accelSamples.size < ACCEL_SAMPLES_PER_POSE) {
                        accelSamples.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                        _progress.value = accelSamples.size * 100 / ACCEL_SAMPLES_PER_POSE
                    } else {
                        sensorManager?.unregisterListener(this)
                        val avgX = accelSamples.map { it[0] }.average().toFloat()
                        val avgY = accelSamples.map { it[1] }.average().toFloat()
                        val avgZ = accelSamples.map { it[2] }.average().toFloat()
                        
                        // Store measurement for later calculation
                        // In production, collect all 6 orientations then calculate
                        continuation.resume(true)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            
            sensorManager?.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
            
            scope.launch {
                delay(3000)
                if (accelSamples.size < ACCEL_SAMPLES_PER_POSE) {
                    sensorManager?.unregisterListener(listener)
                    continuation.resume(false)
                }
            }
        }
    }

    fun finishAccelerometerCalibration(measurements: List<FloatArray>) {
        val expected = listOf(
            floatArrayOf(0f, 0f, GRAVITY),
            floatArrayOf(0f, 0f, -GRAVITY),
            floatArrayOf(GRAVITY, 0f, 0f),
            floatArrayOf(-GRAVITY, 0f, 0f),
            floatArrayOf(0f, GRAVITY, 0f),
            floatArrayOf(0f, -GRAVITY, 0f)
        )
        
        var sumXx = 0f; var sumYy = 0f; var sumZz = 0f
        var sumX = 0f; var sumY = 0f; var sumZ = 0f
        
        for (i in measurements.indices) {
            val (mx, my, mz) = measurements[i]
            val (ex, ey, ez) = expected[i]
            
            sumXx += mx * ex; sumYy += my * ey; sumZz += mz * ez
            sumX += mx * mx; sumY += my * my; sumZ += mz * mz
        }
        
        accelScale[0] = if (sumX > 0) sumXx / sumX else 1f
        accelScale[1] = if (sumY > 0) sumYy / sumY else 1f
        accelScale[2] = if (sumZ > 0) sumZz / sumZ else 1f
        
        var sumOx = 0f; var sumOy = 0f; var sumOz = 0f
        for (i in measurements.indices) {
            val (mx, my, mz) = measurements[i]
            val (ex, ey, ez) = expected[i]
            sumOx += ex - mx * accelScale[0]
            sumOy += ey - my * accelScale[1]
            sumOz += ez - mz * accelScale[2]
        }
        
        accelOffset[0] = sumOx / measurements.size
        accelOffset[1] = sumOy / measurements.size
        accelOffset[2] = sumOz / measurements.size
        
        saveCalibrationData()
        _state.value = CalibrationState.COMPLETED
        vibrate(300)
    }

    fun correctGyro(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(x - gyroBias[0], y - gyroBias[1], z - gyroBias[2])
    }

    fun correctGyroAxis(value: Float, axis: Int): Float {
        return value - gyroBias[axis]
    }

    fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - magOffset[0]) * magScale[0],
            (y - magOffset[1]) * magScale[1],
            (z - magOffset[2]) * magScale[2]
        )
    }

    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffset[0]) / accelScale[0],
            (y - accelOffset[1]) / accelScale[1],
            (z - accelOffset[2]) / accelScale[2]
        )
    }

    fun getGyroBias(): FloatArray = gyroBias.copyOf()
    fun getMagOffset(): FloatArray = magOffset.copyOf()
    fun getMagScale(): FloatArray = magScale.copyOf()
    fun getAccelOffset(): FloatArray = accelOffset.copyOf()
    fun getAccelScale(): FloatArray = accelScale.copyOf()
    
    fun isCalibrated(): Boolean = prefs.getBoolean("calibration_complete", false)
    fun getQuality(): CalibrationQuality = _quality.value

    private fun vibrate(duration: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun reset() {
        gyroBias = floatArrayOf(0f, 0f, 0f)
        magOffset = floatArrayOf(0f, 0f, 0f)
        magScale = floatArrayOf(1f, 1f, 1f)
        accelOffset = floatArrayOf(0f, 0f, 0f)
        accelScale = floatArrayOf(1f, 1f, 1f)
        prefs.putBoolean("calibration_complete", false)
        saveCalibrationData()
        _state.value = CalibrationState.IDLE
        _quality.value = CalibrationQuality.UNKNOWN
    }

    fun destroy() {
        calibrationJob?.cancel()
        scope.cancel()
    }
}