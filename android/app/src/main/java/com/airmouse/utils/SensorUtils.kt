
package com.airmouse.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class SensorUtils(private val context: Context) {

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun isGyroscopeAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun isAccelerometerAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    fun isMagnetometerAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    fun isRotationVectorAvailable(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    fun getAvailableSensors(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                vendor = sensor.vendor,
                version = sensor.version,
                power = sensor.power,
                resolution = sensor.resolution,
                maxRange = sensor.maximumRange
            )
        }
    }

    fun getSensorDelay(rate: SensorRate): Int {
        return when (rate) {
            SensorRate.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
            SensorRate.GAME -> SensorManager.SENSOR_DELAY_GAME
            SensorRate.UI -> SensorManager.SENSOR_DELAY_UI
            SensorRate.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
        }
    }

    data class SensorInfo(
        val name: String,
        val type: Int,
        val vendor: String,
        val version: Int,
        val power: Float,
        val resolution: Float,
        val maxRange: Float
    )

    enum class SensorRate {
        FASTEST, GAME, UI, NORMAL
    }
}