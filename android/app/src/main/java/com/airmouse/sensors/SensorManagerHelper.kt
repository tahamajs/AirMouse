package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import kotlin.math.sqrt

/**
 * Helper class for managing and accessing device sensors.
 * Provides easy access to all available sensors with fallbacks.
 */
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

    /**
     * Check if gyroscope is available
     */
    fun hasGyroscope(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    /**
     * Check if accelerometer is available
     */
    fun hasAccelerometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    /**
     * Check if magnetometer is available
     */
    fun hasMagnetometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    /**
     * Check if rotation vector sensor is available
     */
    fun hasRotationVector(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    /**
     * Check if game rotation vector is available (no magnetometer)
     */
    fun hasGameRotationVector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null
        } else false
    }

    /**
     * Check if gravity sensor is available
     */
    fun hasGravitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null
    }

    /**
     * Check if linear acceleration sensor is available
     */
    fun hasLinearAcceleration(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }

    /**
     * Check if proximity sensor is available
     */
    fun hasProximitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }

    /**
     * Check if light sensor is available
     */
    fun hasLightSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    }

    /**
     * Check if pressure sensor is available
     */
    fun hasPressureSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
    }

    /**
     * Check if temperature sensor is available
     */
    fun hasTemperatureSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
        } else false
    }

    /**
     * Check if significant motion sensor is available
     */
    fun hasSignificantMotionSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
        } else false
    }

    /**
     * Check if step counter is available
     */
    fun hasStepCounter(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        } else false
    }

    /**
     * Check if step detector is available
     */
    fun hasStepDetector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        } else false
    }

    /**
     * Check if heart rate sensor is available
     */
    fun hasHeartRateSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
        } else false
    }

    /**
     * Get all available sensors with their info
     */
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

    /**
     * Get recommended sensors for Air Mouse (best quality)
     */
    fun getRecommendedSensors(): List<Int> {
        val sensors = mutableListOf<Int>()

        // Prefer game rotation vector over rotation vector for lower latency
        if (hasGameRotationVector()) {
            sensors.add(Sensor.TYPE_GAME_ROTATION_VECTOR)
        } else if (hasRotationVector()) {
            sensors.add(Sensor.TYPE_ROTATION_VECTOR)
        }

        // Add gyroscope and accelerometer as fallbacks
        if (hasGyroscope()) sensors.add(Sensor.TYPE_GYROSCOPE)
        if (hasAccelerometer()) sensors.add(Sensor.TYPE_ACCELEROMETER)
        if (hasMagnetometer()) sensors.add(Sensor.TYPE_MAGNETIC_FIELD)

        return sensors
    }

    /**
     * Get sensor delay based on desired update rate
     */
    fun getSensorDelay(rate: SensorRate): Int {
        return when (rate) {
            SensorRate.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
            SensorRate.GAME -> SensorManager.SENSOR_DELAY_GAME
            SensorRate.UI -> SensorManager.SENSOR_DELAY_UI
            SensorRate.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
        }
    }

    /**
     * Get the best available orientation sensor
     */
    fun getBestOrientationSensor(): Int {
        return when {
            hasGameRotationVector() -> Sensor.TYPE_GAME_ROTATION_VECTOR
            hasRotationVector() -> Sensor.TYPE_ROTATION_VECTOR
            hasGyroscope() && hasAccelerometer() -> Sensor.TYPE_GYROSCOPE
            else -> Sensor.TYPE_ACCELEROMETER
        }
    }

    /**
     * Calculate sensor fusion quality score (0-100)
     */
    fun getSensorQualityScore(): Int {
        var score = 0
        if (hasGyroscope()) score += 30
        if (hasAccelerometer()) score += 25
        if (hasMagnetometer()) score += 20
        if (hasGameRotationVector()) score += 15
        if (hasRotationVector()) score += 10

        return score
    }

    /**
     * Get sensor recommendations for user
     */
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
}package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import kotlin.math.sqrt

/**
 * Helper class for managing and accessing device sensors.
 * Provides easy access to all available sensors with fallbacks.
 */
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

    /**
     * Check if gyroscope is available
     */
    fun hasGyroscope(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    /**
     * Check if accelerometer is available
     */
    fun hasAccelerometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    /**
     * Check if magnetometer is available
     */
    fun hasMagnetometer(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
    }

    /**
     * Check if rotation vector sensor is available
     */
    fun hasRotationVector(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
    }

    /**
     * Check if game rotation vector is available (no magnetometer)
     */
    fun hasGameRotationVector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) != null
        } else false
    }

    /**
     * Check if gravity sensor is available
     */
    fun hasGravitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null
    }

    /**
     * Check if linear acceleration sensor is available
     */
    fun hasLinearAcceleration(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
    }

    /**
     * Check if proximity sensor is available
     */
    fun hasProximitySensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
    }

    /**
     * Check if light sensor is available
     */
    fun hasLightSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null
    }

    /**
     * Check if pressure sensor is available
     */
    fun hasPressureSensor(): Boolean {
        return sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
    }

    /**
     * Check if temperature sensor is available
     */
    fun hasTemperatureSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null
        } else false
    }

    /**
     * Check if significant motion sensor is available
     */
    fun hasSignificantMotionSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
        } else false
    }

    /**
     * Check if step counter is available
     */
    fun hasStepCounter(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        } else false
    }

    /**
     * Check if step detector is available
     */
    fun hasStepDetector(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        } else false
    }

    /**
     * Check if heart rate sensor is available
     */
    fun hasHeartRateSensor(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null
        } else false
    }

    /**
     * Get all available sensors with their info
     */
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

    /**
     * Get recommended sensors for Air Mouse (best quality)
     */
    fun getRecommendedSensors(): List<Int> {
        val sensors = mutableListOf<Int>()

        // Prefer game rotation vector over rotation vector for lower latency
        if (hasGameRotationVector()) {
            sensors.add(Sensor.TYPE_GAME_ROTATION_VECTOR)
        } else if (hasRotationVector()) {
            sensors.add(Sensor.TYPE_ROTATION_VECTOR)
        }

        // Add gyroscope and accelerometer as fallbacks
        if (hasGyroscope()) sensors.add(Sensor.TYPE_GYROSCOPE)
        if (hasAccelerometer()) sensors.add(Sensor.TYPE_ACCELEROMETER)
        if (hasMagnetometer()) sensors.add(Sensor.TYPE_MAGNETIC_FIELD)

        return sensors
    }

    /**
     * Get sensor delay based on desired update rate
     */
    fun getSensorDelay(rate: SensorRate): Int {
        return when (rate) {
            SensorRate.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
            SensorRate.GAME -> SensorManager.SENSOR_DELAY_GAME
            SensorRate.UI -> SensorManager.SENSOR_DELAY_UI
            SensorRate.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
        }
    }

    /**
     * Get the best available orientation sensor
     */
    fun getBestOrientationSensor(): Int {
        return when {
            hasGameRotationVector() -> Sensor.TYPE_GAME_ROTATION_VECTOR
            hasRotationVector() -> Sensor.TYPE_ROTATION_VECTOR
            hasGyroscope() && hasAccelerometer() -> Sensor.TYPE_GYROSCOPE
            else -> Sensor.TYPE_ACCELEROMETER
        }
    }

    /**
     * Calculate sensor fusion quality score (0-100)
     */
    fun getSensorQualityScore(): Int {
        var score = 0
        if (hasGyroscope()) score += 30
        if (hasAccelerometer()) score += 25
        if (hasMagnetometer()) score += 20
        if (hasGameRotationVector()) score += 15
        if (hasRotationVector()) score += 10

        return score
    }

    /**
     * Get sensor recommendations for user
     */
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