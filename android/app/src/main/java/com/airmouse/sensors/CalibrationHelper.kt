package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import kotlin.math.sqrt

/**
 * Handles calibration of gyroscope (bias), magnetometer (hard‑iron),
 * and accelerometer (offset & scale using 6‑point method).
 */
class CalibrationHelper(private val context: Context) {

    private var gyroBias = FloatArray(3)
    private var accelOffset = FloatArray(3)
    private var accelScale = FloatArray(3) { 1f }
    private var magOffset = FloatArray(3)
    private var magScale = FloatArray(3) { 1f }

    // ------------------- Gyroscope -------------------
    suspend fun calibrateGyro() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: return
        val samples = mutableListOf<FloatArray>()
        val latch = java.util.concurrent.CountDownLatch(1)

        val listener = object : SensorEventListener {
            var count = 0
            override fun onSensorChanged(event: SensorEvent) {
                if (count++ < 500) {
                    samples.add(event.values.clone())
                } else {
                    sensorManager.unregisterListener(this)
                    latch.countDown()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST)
        latch.await()

        gyroBias[0] = samples.map { it[0] }.average().toFloat()
        gyroBias[1] = samples.map { it[1] }.average().toFloat()
        gyroBias[2] = samples.map { it[2] }.average().toFloat()
    }

    fun correctGyro(value: Float, axis: Int): Float = value - gyroBias[axis]

    // ------------------- Accelerometer (6‑point) -------------------
    /**
     * Full 6‑point accelerometer calibration.
     * Place phone in 6 orientations: ±X, ±Y, ±Z aligned with gravity.
     * For each orientation, average 100 samples, then solve for offset & scale.
     */
    suspend fun calibrateAccelerometer() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

        // Known gravity vectors (ideal readings in m/s²)
        val orientations = listOf(
            floatArrayOf(9.81f, 0f, 0f),    // +X
            floatArrayOf(-9.81f, 0f, 0f),   // -X
            floatArrayOf(0f, 9.81f, 0f),    // +Y
            floatArrayOf(0f, -9.81f, 0f),   // -Y
            floatArrayOf(0f, 0f, 9.81f),    // +Z
            floatArrayOf(0f, 0f, -9.81f)    // -Z
        )

        val measured = mutableListOf<FloatArray>()

        for (orientation in orientations) {
            // Instruct user (in real UI, show a dialog)
            // For now, just collect data with a delay
            val samples = mutableListOf<FloatArray>()
            val latch = java.util.concurrent.CountDownLatch(1)
            val listener = object : SensorEventListener {
                var count = 0
                override fun onSensorChanged(event: SensorEvent) {
                    if (count++ < 100) {
                        samples.add(event.values.clone())
                    } else {
                        sensorManager.unregisterListener(this)
                        latch.countDown()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)
            latch.await()
            val avg = floatArrayOf(
                samples.map { it[0] }.average().toFloat(),
                samples.map { it[1] }.average().toFloat(),
                samples.map { it[2] }.average().toFloat()
            )
            measured.add(avg)
        }

        // Solve for offset and scale using linear regression per axis
        // For each axis i, we have: measured_i = scale_i * ideal_i + offset_i
        // With two equations (positive and negative) we can solve:
        for (i in 0..2) {
            val posIdeal = orientations[2 * i][i]
            val negIdeal = orientations[2 * i + 1][i]
            val posMeas = measured[2 * i][i]
            val negMeas = measured[2 * i + 1][i]

            accelScale[i] = (posMeas - negMeas) / (posIdeal - negIdeal)
            accelOffset[i] = posMeas - accelScale[i] * posIdeal
            if (accelScale[i] == 0f) accelScale[i] = 1f
        }
    }

    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffset[0]) / accelScale[0],
            (y - accelOffset[1]) / accelScale[1],
            (z - accelOffset[2]) / accelScale[2]
        )
    }

    // ------------------- Magnetometer (hard‑iron) -------------------
    suspend fun calibrateMagnetometer(durationMs: Long) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) ?: return
        var magMin = FloatArray(3) { Float.MAX_VALUE }
        var magMax = FloatArray(3) { Float.MIN_VALUE }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                for (i in 0..2) {
                    if (event.values[i] < magMin[i]) magMin[i] = event.values[i]
                    if (event.values[i] > magMax[i]) magMax[i] = event.values[i]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_FASTEST)
        delay(durationMs)
        sensorManager.unregisterListener(listener)

        for (i in 0..2) {
            magOffset[i] = (magMin[i] + magMax[i]) / 2f
            magScale[i] = (magMax[i] - magMin[i]) / 2f
            if (magScale[i] == 0f) magScale[i] = 1f
        }
    }

    fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - magOffset[0]) / magScale[0],
            (y - magOffset[1]) / magScale[1],
            (z - magOffset[2]) / magScale[2]
        )
    }
}