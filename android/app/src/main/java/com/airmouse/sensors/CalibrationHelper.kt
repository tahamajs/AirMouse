package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.delay

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

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        gyroBias = prefs.getGyroBias()
        accelOffset = prefs.getAccelOffset()
        accelScale = prefs.getAccelScale()
        magOffset = prefs.getMagOffset()
        magScale = prefs.getMagScale()
    }

    // ------------------- Gyroscope -------------------
    suspend fun calibrateGyro(onInstruction: (String) -> Unit) {
        onInstruction("Keep phone perfectly still...")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: return
        val samples = mutableListOf<FloatArray>()
        var count = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (count < 500) {
                    samples.add(event.values.clone())
                    count++
                    if (count % 50 == 0) onInstruction("Gyro: ${count / 5}%")
                } else {
                    sensorManager.unregisterListener(this)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST)

        // Wait for samples
        while (count < 500) delay(10)
        sensorManager.unregisterListener(listener)

        if (samples.isNotEmpty()) {
            gyroBias = floatArrayOf(
                samples.map { it[0] }.average().toFloat(),
                samples.map { it[1] }.average().toFloat(),
                samples.map { it[2] }.average().toFloat()
            )
            prefs.saveGyroBias(gyroBias)
            onInstruction("Gyroscope calibrated!")
        } else {
            onInstruction("Gyroscope calibration failed: no data")
        }
    }

    fun correctGyro(value: Float, axis: Int): Float = value - gyroBias[axis]

    // ------------------- Accelerometer (6‑point) -------------------
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        val orientations = listOf(
            "Place phone flat on screen (Z up)",
            "Place phone flat on back (Z down)",
            "Hold phone vertical, port down (X up)",
            "Hold phone vertical, port up (X down)",
            "Hold phone on left side (Y up)",
            "Hold phone on right side (Y down)"
        )

        val measured = mutableListOf<FloatArray>()

        for (i in orientations.indices) {
            onInstruction("${orientations[i]}\nStarting in 3s...")
            delay(3000)
            onInstruction("Measuring...")

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
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)

            while (sampleCount < 100) delay(10)
            sensorManager.unregisterListener(listener)

            measured.add(floatArrayOf(
                samples.map { it[0] }.average().toFloat(),
                samples.map { it[1] }.average().toFloat(),
                samples.map { it[2] }.average().toFloat()
            ))
        }

        // Compute offset and scale for each axis using ±1g measurements
        // Expected gravity: +9.81 for positive, -9.81 for negative
        for (i in 0..2) {
            val posMeas = measured[2 * i][i]
            val negMeas = measured[2 * i + 1][i]
            accelScale[i] = (posMeas - negMeas) / 19.62f
            if (accelScale[i] == 0f) accelScale[i] = 1f
            accelOffset[i] = (posMeas + negMeas) / 2f
        }
        prefs.saveAccelParams(accelOffset, accelScale)
        onInstruction("Accelerometer calibrated!")
    }

    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffset[0]) / accelScale[0],
            (y - accelOffset[1]) / accelScale[1],
            (z - accelOffset[2]) / accelScale[2]
        )
    }

    // ------------------- Magnetometer (hard‑iron) -------------------
    suspend fun calibrateMagnetometer(durationMs: Long, onInstruction: (String) -> Unit) {
        onInstruction("Rotate phone in a figure-8 pattern...")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return
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
        sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_FASTEST)

        val steps = 10
        for (i in 1..steps) {
            delay(durationMs / steps)
            onInstruction("Calibrating Magnetometer... ${(i * 100) / steps}%")
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
        for (i in 0..2) if (magScale[i] == 0f) magScale[i] = 1f

        prefs.saveMagnetometerParams(magOffset, magScale)
        onInstruction("Magnetometer optimized!")
    }

    fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - magOffset[0]) / magScale[0],
            (y - magOffset[1]) / magScale[1],
            (z - magOffset[2]) / magScale[2]
        )
    }
}