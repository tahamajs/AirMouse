package com.airmouse.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

/**
 * Use case that encapsulates calibration logic for all three sensors.
 * All functions are suspendable and can be called from a coroutine.
 */
class CalibrationUseCase(private val context: Context) {

    /**
     * Calibrates the gyroscope by averaging 500 samples while stationary.
     * @return FloatArray of size 3: [biasX, biasY, biasZ] in rad/s.
     */
    suspend fun calibrateGyro(): FloatArray = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            ?: return@suspendCancellableCoroutine cont.resume(floatArrayOf(0f, 0f, 0f))

        val samples = mutableListOf<FloatArray>()
        val listener = object : SensorEventListener {
            var count = 0
            override fun onSensorChanged(event: SensorEvent) {
                if (count++ < 500) {
                    samples.add(event.values.clone())
                } else {
                    sensorManager.unregisterListener(this)
                    val bias = floatArrayOf(
                        samples.map { it[0] }.average().toFloat(),
                        samples.map { it[1] }.average().toFloat(),
                        samples.map { it[2] }.average().toFloat()
                    )
                    cont.resume(bias)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST)
    }

    /**
     * Calibrates the magnetometer (hard‑iron) by recording min/max over a duration.
     * User should move the phone in a figure‑8 pattern.
     * @param durationMs Time to collect data (recommended 30000 ms).
     * @return Pair(offsetArray, scaleArray) for hard‑iron correction.
     */
    suspend fun calibrateMagnetometer(durationMs: Long): Pair<FloatArray, FloatArray> = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            ?: return@suspendCancellableCoroutine cont.resume(Pair(floatArrayOf(0f,0f,0f), floatArrayOf(1f,1f,1f)))

        var min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var max = floatArrayOf(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)

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

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            sensorManager.unregisterListener(listener)
            val offset = floatArrayOf(
                (min[0] + max[0]) / 2f,
                (min[1] + max[1]) / 2f,
                (min[2] + max[2]) / 2f
            )
            val scale = floatArrayOf(
                (max[0] - min[0]) / 2f,
                (max[1] - min[1]) / 2f,
                (max[2] - min[2]) / 2f
            )
            // Avoid division by zero
            for (i in 0..2) if (scale[i] == 0f) scale[i] = 1f
            cont.resume(Pair(offset, scale))
        }, durationMs)
    }

    /**
     * Full 6‑point accelerometer calibration.
     * Requires the user to place the phone in 6 orientations (each axis aligned with gravity).
     * @param orientationData List of 6 FloatArray measurements, each from a different orientation.
     * The order should be: +X, -X, +Y, -Y, +Z, -Z.
     * If null is passed, this function will attempt to collect data automatically (with UI instructions).
     * For simplicity, we provide a version that expects pre‑collected data.
     */
    suspend fun calibrateAccelerometer(measuredOrientations: List<FloatArray>): Pair<FloatArray, FloatArray> {
        require(measuredOrientations.size == 6) { "Need exactly 6 orientation measurements" }

        // Ideal gravity vectors for each orientation (m/s²)
        val ideal = listOf(
            floatArrayOf(9.81f, 0f, 0f),   // +X
            floatArrayOf(-9.81f, 0f, 0f),  // -X
            floatArrayOf(0f, 9.81f, 0f),   // +Y
            floatArrayOf(0f, -9.81f, 0f),  // -Y
            floatArrayOf(0f, 0f, 9.81f),   // +Z
            floatArrayOf(0f, 0f, -9.81f)   // -Z
        )

        val offset = FloatArray(3)
        val scale = FloatArray(3) { 1f }

        for (i in 0..2) {
            val posIdeal = ideal[2 * i][i]
            val negIdeal = ideal[2 * i + 1][i]
            val posMeas = measuredOrientations[2 * i][i]
            val negMeas = measuredOrientations[2 * i + 1][i]

            scale[i] = (posMeas - negMeas) / (posIdeal - negIdeal)
            offset[i] = posMeas - scale[i] * posIdeal
            if (scale[i] == 0f) scale[i] = 1f
        }
        return Pair(offset, scale)
    }

    /**
     * Simplified accelerometer calibration using 1‑point (assumes factory scale is ok).
     * Only corrects offset by assuming the phone is stationary and gravity is 9.81 m/s².
     * This is less accurate but sufficient for many cases.
     */
    suspend fun calibrateAccelerometerSimple(): Pair<FloatArray, FloatArray> = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@suspendCancellableCoroutine cont.resume(Pair(floatArrayOf(0f,0f,0f), floatArrayOf(1f,1f,1f)))

        val samples = mutableListOf<FloatArray>()
        val listener = object : SensorEventListener {
            var count = 0
            override fun onSensorChanged(event: SensorEvent) {
                if (count++ < 200) {
                    samples.add(event.values.clone())
                } else {
                    sensorManager.unregisterListener(this)
                    val avg = floatArrayOf(
                        samples.map { it[0] }.average().toFloat(),
                        samples.map { it[1] }.average().toFloat(),
                        samples.map { it[2] }.average().toFloat()
                    )
                    // Assume gravity vector magnitude should be 9.81, but we only correct offset
                    val offset = floatArrayOf(avg[0], avg[1], avg[2] - 9.81f)
                    val scale = floatArrayOf(1f, 1f, 1f)
                    cont.resume(Pair(offset, scale))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)
    }
}