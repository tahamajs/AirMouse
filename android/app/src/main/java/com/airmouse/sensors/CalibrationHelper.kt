package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay

class CalibrationHelper(private val context: Context) {
    private var gyroBias = FloatArray(3)
    private var accelOffset = FloatArray(3)
    private var accelScale = FloatArray(3) { 1f }
    private var magMin = FloatArray(3) { Float.MAX_VALUE }
    private var magMax = FloatArray(3) { Float.MIN_VALUE }
    private var magOffset = FloatArray(3)
    private var magScale = FloatArray(3) { 1f }

    suspend fun calibrateGyro() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
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

    suspend fun calibrateAccelerometer() {
        // Simplified; in practice collect data in 6 orientations and compute offset/scale
        accelOffset.fill(0f)
        accelScale.fill(1f)
    }

    fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        return Triple(
            (x - accelOffset[0]) / accelScale[0],
            (y - accelOffset[1]) / accelScale[1],
            (z - accelOffset[2]) / accelScale[2]
        )
    }

    suspend fun calibrateMagnetometer(durationMs: Long) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val latch = java.util.concurrent.CountDownLatch(1)
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