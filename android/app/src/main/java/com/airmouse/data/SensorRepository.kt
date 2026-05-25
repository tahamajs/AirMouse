package com.airmouse.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.domain.CalibrationUseCase
import com.airmouse.domain.MadgwickFusion
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/**
 * Repository that provides a flow of fused orientation data (roll, yaw)
 * and also allows calibration and haptic feedback.
 */
class SensorRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val calibrationHelper = CalibrationHelper(context)

    suspend fun calibrateGyro() = calibrationHelper.calibrateGyro()
    suspend fun calibrateMagnetometer(durationMs: Long) = calibrationHelper.calibrateMagnetometer(durationMs)
    suspend fun calibrateAccelerometer() = calibrationHelper.calibrateAccelerometer()

    fun vibrate(duration: Long) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    /**
     * Flow that emits sensor data at ~50 Hz.
     * The flow runs on a background dispatcher to avoid blocking the main thread.
     */
    val sensorEvents: Flow<SensorData> = callbackFlow {
        val madgwick = MadgwickFusion(beta = 0.1f)
        var lastTimestamp = 0L
        var lastGyroY = 0f
        var lastAccelY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = System.currentTimeMillis()
                val dt = if (lastTimestamp == 0L) 0.01f else (now - lastTimestamp) / 1000f
                lastTimestamp = now

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val (ax, ay, az) = calibrationHelper.correctAccelerometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateAccel(ax, ay, az)
                        lastAccelY = ay
                        // Send after each sensor update? Wait for gyro? We'll send after gyro+mag also.
                        // Actually we send after accelerometer because we have roll/yaw from fusion.
                        // But roll/yaw only change after gyro updates. To avoid sending too often,
                        // we send in both accel and gyro? We'll send only after gyro for consistency.
                        // However, for responsiveness, we can send after every sensor type.
                        // The standard pattern: send after magnetometer (all three received).
                        // Simpler: send after every gyro update. We'll do that inside gyro case.
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        val gx = calibrationHelper.correctGyro(event.values[0], 0)
                        val gy = calibrationHelper.correctGyro(event.values[1], 1)
                        val gz = calibrationHelper.correctGyro(event.values[2], 2)
                        madgwick.updateGyro(gx, gy, gz, dt)
                        lastGyroY = gy

                        // After gyro update, fusion state includes latest accel and mag
                        val roll = madgwick.getRoll()
                        val yaw = madgwick.getYaw()
                        trySend(SensorData(roll, yaw, lastGyroY, lastAccelY))
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val (mx, my, mz) = calibrationHelper.correctMagnetometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateMag(mx, my, mz)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        // Register listeners with SENSOR_DELAY_GAME (≈50 Hz)
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
    }.flowOn(Dispatchers.IO)

    data class SensorData(
        val roll: Float,   // radians, around X axis → vertical movement
        val yaw: Float,    // radians, around Z axis → horizontal movement
        val gyroY: Float,  // rad/s, for click detection
        val accelY: Float  // m/s², for scroll detection
    )

    /**
     * Internal helper that uses CalibrationUseCase to obtain and store calibration parameters.
     */
    private inner class CalibrationHelper(private val context: Context) {
        private var gyroBias = FloatArray(3)
        private var accelOffset = FloatArray(3)
        private var accelScale = FloatArray(3) { 1f }
        private var magOffset = FloatArray(3)
        private var magScale = FloatArray(3) { 1f }

        suspend fun calibrateGyro() {
            val useCase = CalibrationUseCase(context)
            gyroBias = useCase.calibrateGyro()
        }

        suspend fun calibrateMagnetometer(durationMs: Long) {
            val useCase = CalibrationUseCase(context)
            val (offset, scale) = useCase.calibrateMagnetometer(durationMs)
            magOffset = offset
            magScale = scale
        }

        suspend fun calibrateAccelerometer() {
            val useCase = CalibrationUseCase(context)
            val (offset, scale) = useCase.calibrateAccelerometerSimple()
            accelOffset = offset
            accelScale = scale
        }

        fun correctGyro(value: Float, axis: Int): Float = value - gyroBias[axis]

        fun correctAccelerometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            return Triple(
                (x - accelOffset[0]) / accelScale[0],
                (y - accelOffset[1]) / accelScale[1],
                (z - accelOffset[2]) / accelScale[2]
            )
        }

        fun correctMagnetometer(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            return Triple(
                (x - magOffset[0]) / magScale[0],
                (y - magOffset[1]) / magScale[1],
                (z - magOffset[2]) / magScale[2]
            )
        }
    }
}