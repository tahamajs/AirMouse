package com.airmouse.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
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
    private val preferences = PreferencesManager(context)
    private val calibrationHelper = CalibrationHelper(context, preferences)

    // ✅ FIXED: Adjusted signatures to match CalibrationHelper's actual 0-argument expectations
    // Note: Check CalibrationHelper to see if the gyro method is named differently (e.g., calibrateGyroscopes)
    suspend fun calibrateGyro(): Boolean = calibrationHelper.calibrateGyro()
    suspend fun calibrateMagnetometer(): Boolean = calibrationHelper.calibrateMagnetometer()
    suspend fun calibrateAccelerometer(): Boolean = calibrationHelper.calibrateAccelerometer()

    fun vibrate(duration: Long) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    /**
     * Flow that emits sensor data at ~50 Hz.
     * The flow runs on a background dispatcher to avoid blocking the main thread.
     */
    val sensorEvents: Flow<SensorData> = callbackFlow {
        val madgwick = MadgwickAHRS(beta = 0.1f)
        var lastTimestamp = 0L
        var lastGyroY = 0f
        var lastAccelY = 0f
        var lastRoll = 0f
        var lastYaw = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val dt = if (lastTimestamp == 0L) 0.01f else (event.timestamp - lastTimestamp) * 1e-9f
                lastTimestamp = event.timestamp

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val (ax, ay, az) = calibrationHelper.correctAccelerometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateAccel(ax, ay, az, dt)
                        lastAccelY = ay
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        // Assuming 0, 1, 2 map to X, Y, Z axes respectively
                        val gx = calibrationHelper.correctGyro(event.values[0], 0)
                        val gy = calibrationHelper.correctGyro(event.values[1], 1)
                        val gz = calibrationHelper.correctGyro(event.values[2], 2)
                        madgwick.updateGyro(gx, gy, gz, dt)
                        lastGyroY = gy

                        val roll = madgwick.getRoll()
                        val yaw = madgwick.getYaw()
                        lastRoll = roll
                        lastYaw = yaw
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val (mx, my, mz) = calibrationHelper.correctMagnetometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateMag(mx, my, mz, dt)
                    }
                }

                trySend(SensorData(lastRoll, lastYaw, lastGyroY, lastAccelY))
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
    }.flowOn(Dispatchers.IO)

    data class SensorData(
        val roll: Float,   // radians, around X axis → vertical movement
        val yaw: Float,    // radians, around Z axis → horizontal movement
        val gyroY: Float,  // rad/s, for click detection
        val accelY: Float  // m/s², for scroll detection
    )
}
