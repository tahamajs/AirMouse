package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import kotlin.math.sqrt

class SensorManagerHelper(private val context: Context) {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    data class SensorInfo(
        val name: String,
        val type: Int,
        val vendor: String,
        val version: Int,
        val maxRange: Float,
        val resolution: Float,
        val power: Float,
        val isAvailable: Boolean
    )

    fun hasGyroscope(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    fun hasAccelerometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    fun hasMagnetometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    fun hasRotationVector(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    fun hasGameRotationVector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null
        } else false
    }

    fun hasGravitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null
    }

    fun hasLinearAcceleration(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }

    fun hasProximitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }

    fun hasLightSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    }

    fun hasPressureSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
    }

    fun hasTemperatureSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
        } else false
    }

    fun hasSignificantMotionSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
        } else false
    }

    fun hasStepCounter(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        } else false
    }

    fun hasStepDetector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        } else false
    }

    fun hasHeartRateSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
        } else false
    }

    fun getAllSensors(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                vendor = sensor.vendor,
                version = sensor.version,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                power = sensor.power,
                isAvailable = true
            )
        }
    }

    fun getRecommendedSensors(): List<Int> {
        val sensors = mutableListOf<Int>()

        
        if (hasGameRotationVector()) {
            sensors.add(Sensor.TYPE_GAME_ROTATION_VECTOR)
        } else if (hasRotationVector()) {
            sensors.add(Sensor.TYPE_ROTATION_VECTOR)
        }

        
        if (hasGyroscope()) sensors.add(Sensor.TYPE_GYROSCOPE)
        if (hasAccelerometer()) sensors.add(Sensor.TYPE_ACCELEROMETER)
        if (hasMagnetometer()) sensors.add(Sensor.TYPE_MAGNETIC_FIELD)

        return sensors
    }

    fun getSensorDelay(rate: SensorRate): Int {
        return when (rate) {
            SensorRate.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
            SensorRate.GAME -> SensorManager.SENSOR_DELAY_GAME
            SensorRate.UI -> SensorManager.SENSOR_DELAY_UI
            SensorRate.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
        }
    }

    fun getBestOrientationSensor(): Int {
        return when {
            hasGameRotationVector() -> Sensor.TYPE_GAME_ROTATION_VECTOR
            hasRotationVector() -> Sensor.TYPE_ROTATION_VECTOR
            hasGyroscope() && hasAccelerometer() -> Sensor.TYPE_GYROSCOPE
            else -> Sensor.TYPE_ACCELEROMETER
        }
    }

    fun getSensorQualityScore(): Int {
        var score = 0
        if (hasGyroscope()) score += 30
        if (hasAccelerometer()) score += 25
        if (hasMagnetometer()) score += 20
        if (hasGameRotationVector()) score += 15
        if (hasRotationVector()) score += 10

        return score
    }

    fun getSensorRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        if (!hasGyroscope()) {
            recommendations.add("Gyroscope is missing. Cursor movement will be less accurate.")
        }
        if (!hasAccelerometer()) {
            recommendations.add("Accelerometer is missing. Scroll gestures may not work properly.")
        }
        if (!hasMagnetometer()) {
            recommendations.add("Magnetometer is missing. Heading calibration will be limited.")
        }

        return recommendations
    }
}

enum class SensorRate {
    FASTEST, GAME, UI, NORMAL
}