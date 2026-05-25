package com.airmouse.domain

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CalibrationUseCase(private val context: Context) {

    suspend fun calibrateGyro(): FloatArray = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
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

    suspend fun calibrateMagnetometer(durationMs: Long): Pair<FloatArray, FloatArray> = suspendCancellableCoroutine { cont ->
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
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
            cont.resume(Pair(offset, scale))
        }, durationMs)
    }
}