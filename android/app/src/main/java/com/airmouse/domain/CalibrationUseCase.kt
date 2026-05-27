package com.airmouse.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.sqrt

/**
 * Use case that encapsulates calibration logic for gyroscope, magnetometer, and accelerometer.
 * All functions are suspendable and store results in PreferencesManager.
 */
class CalibrationUseCase(
    private val context: Context,
    private val prefs: PreferencesManager
) {

    /**
     * Calibrates the gyroscope by averaging 500 samples while stationary.
     * Saves bias to PreferencesManager and returns it.
     */
    suspend fun calibrateGyro(onProgress: ((String) -> Unit)? = null): FloatArray = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyro == null) {
            cont.resume(floatArrayOf(0f, 0f, 0f))
            return@suspendCancellableCoroutine
        }

        val samples = mutableListOf<FloatArray>()
        var count = 0
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (count < 500) {
                    samples.add(event.values.clone())
                    count++
                    if (count % 50 == 0) {
                        onProgress?.invoke("Gyro: ${count / 5}%")
                    }
                } else {
                    sensorManager.unregisterListener(this)
                    val bias = floatArrayOf(
                        samples.map { it[0] }.average().toFloat(),
                        samples.map { it[1] }.average().toFloat(),
                        samples.map { it[2] }.average().toFloat()
                    )
                    prefs.saveGyroBias(bias)
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
     * Saves offset and scale to PreferencesManager.
     */
    suspend fun calibrateMagnetometer(
        durationMs: Long = 15000,
        onProgress: ((String) -> Unit)? = null
    ): Pair<FloatArray, FloatArray> = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (mag == null) {
            cont.resume(Pair(floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f)))
            return@suspendCancellableCoroutine
        }

        var min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var max = floatArrayOf(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE)
        var startTime = System.currentTimeMillis()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                for (i in 0..2) {
                    if (event.values[i] < min[i]) min[i] = event.values[i]
                    if (event.values[i] > max[i]) max[i] = event.values[i]
                }
                val elapsed = System.currentTimeMillis() - startTime
                val percent = (elapsed * 100 / durationMs).toInt().coerceIn(0, 100)
                if (percent % 10 == 0) onProgress?.invoke("Magnetometer: $percent%")
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
            for (i in 0..2) if (scale[i] == 0f) scale[i] = 1f
            prefs.saveMagnetometerParams(offset, scale)
            cont.resume(Pair(offset, scale))
        }, durationMs)
    }

    /**
     * Simplified accelerometer calibration using 1‑point (offset only).
     * Assumes phone is stationary and gravity is 9.81 m/s².
     * Saves offset and scale to PreferencesManager.
     */
    suspend fun calibrateAccelerometerSimple(
        onProgress: ((String) -> Unit)? = null
    ): Pair<FloatArray, FloatArray> = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel == null) {
            cont.resume(Pair(floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f)))
            return@suspendCancellableCoroutine
        }

        val samples = mutableListOf<FloatArray>()
        var count = 0
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (count < 200) {
                    samples.add(event.values.clone())
                    count++
                    if (count % 20 == 0) onProgress?.invoke("Accel: ${count / 2}%")
                } else {
                    sensorManager.unregisterListener(this)
                    val avg = floatArrayOf(
                        samples.map { it[0] }.average().toFloat(),
                        samples.map { it[1] }.average().toFloat(),
                        samples.map { it[2] }.average().toFloat()
                    )
                    // Offset correction: assume gravity magnitude 9.81, but we keep scale = 1
                    val offset = floatArrayOf(avg[0], avg[1], avg[2] - 9.81f)
                    val scale = floatArrayOf(1f, 1f, 1f)
                    prefs.saveAccelerometerParams(offset, scale)
                    cont.resume(Pair(offset, scale))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)
    }

    /**
     * Full 6‑point accelerometer calibration (accurate).
     * Expects the user to collect measurements in 6 orientations.
     */
    suspend fun calibrateAccelerometer6Point(
        measuredOrientations: List<FloatArray>,
        onProgress: ((String) -> Unit)? = null
    ): Pair<FloatArray, FloatArray> {
        require(measuredOrientations.size == 6) { "Need exactly 6 orientation measurements" }
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
        prefs.saveAccelerometerParams(offset, scale)
        return Pair(offset, scale)
    }
}