package com.airmouse.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.domain.MadgwickFusion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SensorRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val calibration = CalibrationHelper(context)

    suspend fun calibrateGyro() = calibration.calibrateGyro()
    suspend fun calibrateMagnetometer(durationMs: Long) = calibration.calibrateMagnetometer(durationMs)

    fun vibrate(duration: Long) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun start() {
        // No-op, the flow below starts automatically
    }

    val sensorEvents: Flow<SensorData> = callbackFlow {
        val madgwick = MadgwickFusion()
        var lastTimestamp = 0L
        var lastGyroY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                val dt = if (lastTimestamp == 0L) 0.01f else (now - lastTimestamp) / 1000f
                lastTimestamp = now

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val (ax, ay, az) = calibration.correctAccelerometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateAccel(ax, ay, az)
                        trySend(SensorData(
                            roll = madgwick.getRoll(),
                            yaw = madgwick.getYaw(),
                            gyroY = lastGyroY,
                            accelY = ay
                        ))
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        val gx = calibration.correctGyro(event.values[0], 0)
                        val gy = calibration.correctGyro(event.values[1], 1)
                        val gz = calibration.correctGyro(event.values[2], 2)
                        madgwick.updateGyro(gx, gy, gz, dt)
                        lastGyroY = gy
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val (mx, my, mz) = calibration.correctMagnetometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateMag(mx, my, mz)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        sensorManager.registerListener(listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_GAME)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    data class SensorData(val roll: Float, val yaw: Float, val gyroY: Float, val accelY: Float)

    private inner class CalibrationHelper(context: Context) {
        private var gyroBias = FloatArray(3)
        private var accelOffset = FloatArray(3)
        private var accelScale = FloatArray(3) { 1f }
        private var magOffset = FloatArray(3)
        private var magScale = FloatArray(3) { 1f }

        suspend fun calibrateGyro() {
            val useCase = com.airmouse.domain.CalibrationUseCase(context)
            gyroBias = useCase.calibrateGyro()
        }

        suspend fun calibrateMagnetometer(durationMs: Long) {
            val useCase = com.airmouse.domain.CalibrationUseCase(context)
            val (offset, scale) = useCase.calibrateMagnetometer(durationMs)
            magOffset = offset
            magScale = scale
        }

        fun correctGyro(value: Float, axis: Int) = value - gyroBias[axis]
        fun correctAccelerometer(x: Float, y: Float, z: Float) = Triple(x, y, z) // simplified
        fun correctMagnetometer(x: Float, y: Float, z: Float) = Triple(
            (x - magOffset[0]) / magScale[0],
            (y - magOffset[1]) / magScale[1],
            (z - magOffset[2]) / magScale[2]
        )
    }
}