package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Handles calibration of gyroscope (bias), magnetometer (hard‑iron),
 * and accelerometer (offset & scale using 6‑point method).
 * Persists results to PreferencesManager.
 */
class CalibrationHelper(
    private val context: Context,
    private val prefs: PreferencesManager
) {

    private var gyroBias = FloatArray(3)
    private var accelOffset = FloatArray(3)
    private var accelScale = FloatArray(3) { 1f }
    private var magOffset = FloatArray(3)
    private var magScale = FloatArray(3) { 1f }
    private var isCalibrated = false

    private var calibrationCallback: CalibrationCallback? = null

    interface CalibrationCallback {
        fun onProgress(progress: Int, message: String)
        fun onComplete()
        fun onError(error: String)
    }

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        gyroBias = prefs.getGyroBias()
        accelOffset = prefs.getAccelOffset()
        accelScale = prefs.getAccelScale()
        magOffset = prefs.getMagOffset()
        magScale = prefs.getMagScale()
        isCalibrated = prefs.getBoolean("calibration_complete", false)
    }

    fun loadCalibrationStatus(): Boolean {
        return prefs.getBoolean("calibration_complete", false)
    }

    fun loadGyroOffsets(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("gyro_offset_x", 0f),
            prefs.getFloat("gyro_offset_y", 0f),
            prefs.getFloat("gyro_offset_z", 0f)
        )
    }

    fun getGyroOffsetX(): Float = prefs.getFloat("gyro_offset_x", 0f)
    fun getGyroOffsetY(): Float = prefs.getFloat("gyro_offset_y", 0f)
    fun getGyroOffsetZ(): Float = prefs.getFloat("gyro_offset_z", 0f)

    fun setGyroOffsets(x: Float, y: Float, z: Float) {
        prefs.putFloat("gyro_offset_x", x)
        prefs.putFloat("gyro_offset_y", y)
        prefs.putFloat("gyro_offset_z", z)
        gyroBias = floatArrayOf(x, y, z)
    }

    fun startCalibration(callback: CalibrationCallback? = null) {
        calibrationCallback = callback
        prefs.putBoolean("calibration_complete", false)
        prefs.putFloat("gyro_offset_x", 0f)
        prefs.putFloat("gyro_offset_y", 0f)
        prefs.putFloat("gyro_offset_z", 0f)
        isCalibrated = false
        calibrationCallback?.onProgress(0, "Calibration started")
    }

    fun resetCalibration() {
        prefs.putBoolean("calibration_complete", false)
        prefs.putFloat("gyro_offset_x", 0f)
        prefs.putFloat("gyro_offset_y", 0f)
        prefs.putFloat("gyro_offset_z", 0f)
        prefs.putFloat("accel_offset_x", 0f)
        prefs.putFloat("accel_offset_y", 0f)
        prefs.putFloat("accel_offset_z", 0f)
        prefs.putFloat("mag_offset_x", 0f)
        prefs.putFloat("mag_offset_y", 0f)
        prefs.putFloat("mag_offset_z", 0f)
        gyroBias = floatArrayOf(0f, 0f, 0f)
        accelOffset = floatArrayOf(0f, 0f, 0f)
        accelScale = floatArrayOf(1f, 1f, 1f)
        magOffset = floatArrayOf(0f, 0f, 0f)
        magScale = floatArrayOf(1f, 1f, 1f)
        isCalibrated = false
        calibrationCallback?.onProgress(0, "Calibration reset")
    }

    fun isDeviceCalibrated(): Boolean = loadCalibrationStatus()

    // ==================== Gyroscope Calibration ====================

    suspend fun calibrateGyro(onInstruction: (String) -> Unit) {
        onInstruction("Keep phone perfectly still on a flat surface...")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: run {
            onInstruction("Gyroscope not available on this device")
            calibrationCallback?.onError("Gyroscope not available")
            return
        }

        val sensorThread = HandlerThread("CalibrationGyroThread").also { it.start() }
        val handler = Handler(sensorThread.looper)
        val samples = mutableListOf<FloatArray>()
        var count = 0
        var lastProgress = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (count < 500) {
                    samples.add(event.values.clone())
                    count++
                    val progress = (count * 100 / 500)
                    if (progress > lastProgress) {
                        lastProgress = progress
                        onInstruction("Gyro calibration: $progress%")
                        calibrationCallback?.onProgress(progress, "Collecting gyro data...")
                    }
                } else {
                    sensorManager.unregisterListener(this)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        try {
            sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST, handler)

            // Wait for samples with timeout
            var timeout = 0
            while (count < 500 && timeout < 100) {
                delay(100)
                timeout++
            }
            sensorManager.unregisterListener(listener)

            if (samples.isNotEmpty()) {
                gyroBias = floatArrayOf(
                    samples.map { it[0] }.average().toFloat(),
                    samples.map { it[1] }.average().toFloat(),
                    samples.map { it[2] }.average().toFloat()
                )
                prefs.saveGyroBias(gyroBias)
                prefs.putFloat("gyro_offset_x", gyroBias[0])
                prefs.putFloat("gyro_offset_y", gyroBias[1])
                prefs.putFloat("gyro_offset_z", gyroBias[2])

                onInstruction("✓ Gyroscope calibrated successfully!")
                calibrationCallback?.onProgress(100, "Gyroscope calibrated")
            } else {
                onInstruction("✗ Gyroscope calibration failed: insufficient data")
                calibrationCallback?.onError("No gyro data collected")
            }
        } finally {
            sensorManager.unregisterListener(listener)
            sensorThread.quitSafely()
        }
    }

    fun correctGyro(value: Float, axis: Int): Float = value - gyroBias[axis]

    // ==================== Accelerometer Calibration (6-point) ====================

    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: run {
            onInstruction("Accelerometer not available on this device")
            calibrationCallback?.onError("Accelerometer not available")
            return
        }

        val sensorThread = HandlerThread("CalibrationAccelThread").also { it.start() }
        val handler = Handler(sensorThread.looper)

        val orientations = listOf(
            "Place phone flat on screen (Z up)",
            "Place phone flat on back (Z down)",
            "Hold phone vertical, port down (X up)",
            "Hold phone vertical, port up (X down)",
            "Hold phone on left side (Y up)",
            "Hold phone on right side (Y down)"
        )

        val measured = mutableListOf<FloatArray>()

        try {
            for (i in orientations.indices) {
                onInstruction("${orientations[i]}\nStarting in 3 seconds...")
                calibrationCallback?.onProgress(i * 100 / 6, orientations[i])
                delay(3000)
                onInstruction("Measuring... (${i + 1}/6)")

                val samples = mutableListOf<FloatArray>()
                var sampleCount = 0
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (sampleCount < 100) {
                            samples.add(event.values.clone())
                            sampleCount++
                        } else {
                            sensorManager.unregisterListener(this)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }

                try {
                    sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST, handler)

                    var timeout = 0
                    while (sampleCount < 100 && timeout < 50) {
                        delay(100)
                        timeout++
                    }
                    sensorManager.unregisterListener(listener)

                    measured.add(floatArrayOf(
                        samples.map { it[0] }.average().toFloat(),
                        samples.map { it[1] }.average().toFloat(),
                        samples.map { it[2] }.average().toFloat()
                    ))
                } finally {
                    sensorManager.unregisterListener(listener)
                }
            }

            // Compute offset and scale for each axis using ±1g measurements
            var scaleWarning = false
            for (i in 0..2) {
                val posMeas = measured[2 * i][i]
                val negMeas = measured[2 * i + 1][i]
                val scale = (posMeas - negMeas) / 19.62f
                accelScale[i] = if (scale.isFinite() && abs(scale) in 0.5f..2.0f) scale else {
                    scaleWarning = true
                    1f
                }
                accelOffset[i] = (posMeas + negMeas) / 2f
            }
            prefs.saveAccelParams(accelOffset, accelScale)

            val message = if (scaleWarning)
                "Accelerometer calibrated with fallback scale values."
            else
                "✓ Accelerometer calibrated successfully!"
            onInstruction(message)
            calibrationCallback?.onProgress(100, "Accelerometer calibrated")
        } finally {
            sensorThread.quitSafely()
        }
    }

    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffset[0]) / accelScale[0],
            (y - accelOffset[1]) / accelScale[1],
            (z - accelOffset[2]) / accelScale[2]
        )
    }

    // ==================== Magnetometer Calibration (hard-iron) ====================

    suspend fun calibrateMagnetometer(durationMs: Long = 15000, onInstruction: (String) -> Unit) {
        onInstruction("Move phone in a figure-8 pattern...")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: run {
            onInstruction("Magnetometer not available on this device, skipping...")
            calibrationCallback?.onProgress(100, "Magnetometer skipped")
            return
        }

        val sensorThread = HandlerThread("CalibrationMagThread").also { it.start() }
        val handler = Handler(sensorThread.looper)
        var min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                for (i in 0..2) {
                    if (event.values[i] < min[i]) min[i] = event.values[i]
                    if (event.values[i] > max[i]) max[i] = event.values[i]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        try {
            sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_FASTEST, handler)

            val steps = 10
            for (i in 1..steps) {
                delay(durationMs / steps)
                val progress = (i * 100 / steps)
                onInstruction("Calibrating Magnetometer... $progress%")
                calibrationCallback?.onProgress(progress, "Collecting magnetic field data...")
            }

            sensorManager.unregisterListener(listener)

            // Hard‑iron offset = (min+max)/2, scale = (max-min)/2 (to normalise to ±1)
            magOffset = floatArrayOf(
                (min[0] + max[0]) / 2f,
                (min[1] + max[1]) / 2f,
                (min[2] + max[2]) / 2f
            )
            magScale = floatArrayOf(
                (max[0] - min[0]) / 2f,
                (max[1] - min[1]) / 2f,
                (max[2] - min[2]) / 2f
            )
            for (i in 0..2) {
                if (!magScale[i].isFinite() || abs(magScale[i]) !in 0.5f..2.0f) {
                    magScale[i] = 1f
                }
            }

            prefs.saveMagCalibration(magOffset, magScale)
            onInstruction("✓ Magnetometer calibrated successfully!")
            calibrationCallback?.onProgress(100, "Magnetometer calibrated")
        } finally {
            sensorManager.unregisterListener(listener)
            sensorThread.quitSafely()
        }
    }

    fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - magOffset[0]) / magScale[0],
            (y - magOffset[1]) / magScale[1],
            (z - magOffset[2]) / magScale[2]
        )
    }

    // Helper methods for PreferencesManager compatibility
    fun applyCalibration(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            x - getGyroOffsetX(),
            y - getGyroOffsetY(),
            z - getGyroOffsetZ()
        )
    }

    fun completeCalibration() {
        prefs.putBoolean("calibration_complete", true)
        isCalibrated = true
        calibrationCallback?.onComplete()
    }
}