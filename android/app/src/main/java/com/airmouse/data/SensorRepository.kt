package com.airmouse.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.airmouse.utils.PreferencesManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class SensorRepository(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
    private val preferences = PreferencesManager(context)
    private val calibrationHelper = CalibrationHelper(context, preferences)

    
    
    suspend fun calibrateGyro(): Boolean = calibrationHelper.calibrateGyroscope()
    suspend fun calibrateMagnetometer(): Boolean = calibrationHelper.calibrateMagnetometer()
    suspend fun calibrateAccelerometer(): Boolean = calibrationHelper.calibrateAccelerometer()

    fun vibrate(duration: Long) {
        val safeVibrator = vibrator ?: return
        if (!safeVibrator.hasVibrator()) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                safeVibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                safeVibrator.vibrate(duration)
            }
        } catch (_: SecurityException) {
            
        }
    }

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
        val roll: Float,   
        val yaw: Float,    
        val gyroY: Float,  
        val accelY: Float  
    )
}
