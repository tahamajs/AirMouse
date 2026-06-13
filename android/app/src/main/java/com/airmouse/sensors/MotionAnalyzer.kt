package com.airmouse.sensors

import kotlin.math.*

/**
 * Analyzes motion patterns for gesture recognition and activity detection
 */
class MotionAnalyzer {

    data class MotionFeatures(
        val acceleration: Float,
        val jerk: Float,
        val rotationSpeed: Float,
        val direction: Float,
        val stability: Float,
        val isMoving: Boolean,
        val dominantAxis: String
    )

    data class Activity(
        val type: ActivityType,
        val confidence: Float,
        val duration: Long
    )

    enum class ActivityType {
        IDLE, WALKING, RUNNING, GESTURE, SHAKING, ROTATING
    }

    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var lastTimestamp = 0L
    private var activityStartTime = 0L
    private var currentActivity = ActivityType.IDLE

    private val idleThreshold = 0.2f
    private val walkingThreshold = 5f
    private val runningThreshold = 15f
    private val gestureThreshold = 20f

    /**
     * Analyze motion features from sensor data
     */
    fun analyzeMotion(
        accelX: Float, accelY: Float, accelZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        timestamp: Long
    ): MotionFeatures {
        val dt = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000f else 0.016f

        // Calculate acceleration magnitude
        val accelMagnitude = SensorDataProcessor.magnitude(accelX, accelY, accelZ)

        // Calculate jerk (derivative of acceleration)
        val jerkX = SensorDataProcessor.derivative(accelX, lastAccelX, dt)
        val jerkY = SensorDataProcessor.derivative(accelY, lastAccelY, dt)
        val jerkZ = SensorDataProcessor.derivative(accelZ, lastAccelZ, dt)
        val jerk = SensorDataProcessor.magnitude(jerkX, jerkY, jerkZ)

        // Calculate rotation speed
        val rotationSpeed = SensorDataProcessor.magnitude(gyroX, gyroY, gyroZ)

        // Calculate direction
        val direction = atan2(accelY, accelX)

        // Calculate stability (inverse of variance over time)
        val stability = 1f / (1f + jerk)

        // Determine if moving
        val isMoving = accelMagnitude > idleThreshold

        // Determine dominant axis
        val absX = abs(accelX)
        val absY = abs(accelY)
        val absZ = abs(accelZ)
        val dominantAxis = when {
            absX > absY && absX > absZ -> "X"
            absY > absX && absY > absZ -> "Y"
            else -> "Z"
        }

        lastAccelX = accelX
        lastAccelY = accelY
        lastAccelZ = accelZ
        lastTimestamp = timestamp

        return MotionFeatures(
            acceleration = accelMagnitude,
            jerk = jerk,
            rotationSpeed = rotationSpeed,
            direction = direction,
            stability = stability,
            isMoving = isMoving,
            dominantAxis = dominantAxis
        )
    }

    /**
     * Detect activity type from motion features
     */
    fun detectActivity(features: MotionFeatures, timestamp: Long): Activity {
        val now = System.currentTimeMillis()
        var newActivity = currentActivity
        var confidence = 0.5f

        when {
            features.acceleration < idleThreshold && features.rotationSpeed < 0.5f -> {
                newActivity = ActivityType.IDLE
                confidence = 0.9f
            }
            features.acceleration in idleThreshold..walkingThreshold -> {
                newActivity = ActivityType.WALKING
                confidence = 0.7f
            }
            features.acceleration in walkingThreshold..runningThreshold -> {
                newActivity = ActivityType.RUNNING
                confidence = 0.8f
            }
            features.jerk > gestureThreshold -> {
                newActivity = ActivityType.GESTURE
                confidence = 0.85f
            }
            features.rotationSpeed > 10f -> {
                newActivity = ActivityType.ROTATING
                confidence = 0.75f
            }
        }

        if (newActivity != currentActivity) {
            activityStartTime = timestamp
            currentActivity = newActivity
        }

        val duration = timestamp - activityStartTime

        return Activity(newActivity, confidence, duration)
    }

    /**
     * Detect if device is stationary
     */
    fun isStationary(features: MotionFeatures): Boolean {
        return features.acceleration < 0.3f && features.rotationSpeed < 0.2f
    }

    /**
     * Detect sudden movement (impulse)
     */
    fun isSuddenMovement(features: MotionFeatures): Boolean {
        return features.jerk > 30f
    }

    /**
     * Detect shaking motion
     */
    fun isShaking(features: MotionFeatures, history: List<MotionFeatures>): Boolean {
        if (history.size < 10) return false

        val recentAccel = history.takeLast(10).map { it.acceleration }
        val maxAccel = recentAccel.maxOrNull() ?: 0f
        val minAccel = recentAccel.minOrNull() ?: 0f
        val amplitude = maxAccel - minAccel

        // Shaking is characterized by rapid, alternating acceleration
        return amplitude > 15f && features.acceleration > 10f
    }

    /**
     * Detect rotation gesture
     */
    fun detectRotation(gyroZ: Float, duration: Long): Boolean {
        return abs(gyroZ) > 8f && duration > 200
    }

    /**
     * Detect tap gesture
     */
    fun detectTap(features: MotionFeatures): Boolean {
        return features.jerk > 15f && features.acceleration > 5f
    }

    /**
     * Reset analyzer state
     */
    fun reset() {
        lastAccelX = 0f
        lastAccelY = 0f
        lastAccelZ = 0f
        lastTimestamp = 0L
        activityStartTime = 0L
        currentActivity = ActivityType.IDLE
    }
}package com.airmouse.sensors

import kotlin.math.*

/**
 * Analyzes motion patterns for gesture recognition and activity detection
 */
class MotionAnalyzer {

    data class MotionFeatures(
        val acceleration: Float,
        val jerk: Float,
        val rotationSpeed: Float,
        val direction: Float,
        val stability: Float,
        val isMoving: Boolean,
        val dominantAxis: String
    )

    data class Activity(
        val type: ActivityType,
        val confidence: Float,
        val duration: Long
    )

    enum class ActivityType {
        IDLE, WALKING, RUNNING, GESTURE, SHAKING, ROTATING
    }

    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 0f
    private var lastTimestamp = 0L
    private var activityStartTime = 0L
    private var currentActivity = ActivityType.IDLE

    private val idleThreshold = 0.2f
    private val walkingThreshold = 5f
    private val runningThreshold = 15f
    private val gestureThreshold = 20f

    /**
     * Analyze motion features from sensor data
     */
    fun analyzeMotion(
        accelX: Float, accelY: Float, accelZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        timestamp: Long
    ): MotionFeatures {
        val dt = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000f else 0.016f

        // Calculate acceleration magnitude
        val accelMagnitude = SensorDataProcessor.magnitude(accelX, accelY, accelZ)

        // Calculate jerk (derivative of acceleration)
        val jerkX = SensorDataProcessor.derivative(accelX, lastAccelX, dt)
        val jerkY = SensorDataProcessor.derivative(accelY, lastAccelY, dt)
        val jerkZ = SensorDataProcessor.derivative(accelZ, lastAccelZ, dt)
        val jerk = SensorDataProcessor.magnitude(jerkX, jerkY, jerkZ)

        // Calculate rotation speed
        val rotationSpeed = SensorDataProcessor.magnitude(gyroX, gyroY, gyroZ)

        // Calculate direction
        val direction = atan2(accelY, accelX)

        // Calculate stability (inverse of variance over time)
        val stability = 1f / (1f + jerk)

        // Determine if moving
        val isMoving = accelMagnitude > idleThreshold

        // Determine dominant axis
        val absX = abs(accelX)
        val absY = abs(accelY)
        val absZ = abs(accelZ)
        val dominantAxis = when {
            absX > absY && absX > absZ -> "X"
            absY > absX && absY > absZ -> "Y"
            else -> "Z"
        }

        lastAccelX = accelX
        lastAccelY = accelY
        lastAccelZ = accelZ
        lastTimestamp = timestamp

        return MotionFeatures(
            acceleration = accelMagnitude,
            jerk = jerk,
            rotationSpeed = rotationSpeed,
            direction = direction,
            stability = stability,
            isMoving = isMoving,
            dominantAxis = dominantAxis
        )
    }

    /**
     * Detect activity type from motion features
     */
    fun detectActivity(features: MotionFeatures, timestamp: Long): Activity {
        val now = System.currentTimeMillis()
        var newActivity = currentActivity
        var confidence = 0.5f

        when {
            features.acceleration < idleThreshold && features.rotationSpeed < 0.5f -> {
                newActivity = ActivityType.IDLE
                confidence = 0.9f
            }
            features.acceleration in idleThreshold..walkingThreshold -> {
                newActivity = ActivityType.WALKING
                confidence = 0.7f
            }
            features.acceleration in walkingThreshold..runningThreshold -> {
                newActivity = ActivityType.RUNNING
                confidence = 0.8f
            }
            features.jerk > gestureThreshold -> {
                newActivity = ActivityType.GESTURE
                confidence = 0.85f
            }
            features.rotationSpeed > 10f -> {
                newActivity = ActivityType.ROTATING
                confidence = 0.75f
            }
        }

        if (newActivity != currentActivity) {
            activityStartTime = timestamp
            currentActivity = newActivity
        }

        val duration = timestamp - activityStartTime

        return Activity(newActivity, confidence, duration)
    }

    /**
     * Detect if device is stationary
     */
    fun isStationary(features: MotionFeatures): Boolean {
        return features.acceleration < 0.3f && features.rotationSpeed < 0.2f
    }

    /**
     * Detect sudden movement (impulse)
     */
    fun isSuddenMovement(features: MotionFeatures): Boolean {
        return features.jerk > 30f
    }

    /**
     * Detect shaking motion
     */
    fun isShaking(features: MotionFeatures, history: List<MotionFeatures>): Boolean {
        if (history.size < 10) return false

        val recentAccel = history.takeLast(10).map { it.acceleration }
        val maxAccel = recentAccel.maxOrNull() ?: 0f
        val minAccel = recentAccel.minOrNull() ?: 0f
        val amplitude = maxAccel - minAccel

        // Shaking is characterized by rapid, alternating acceleration
        return amplitude > 15f && features.acceleration > 10f
    }

    /**
     * Detect rotation gesture
     */
    fun detectRotation(gyroZ: Float, duration: Long): Boolean {
        return abs(gyroZ) > 8f && duration > 200
    }

    /**
     * Detect tap gesture
     */
    fun detectTap(features: MotionFeatures): Boolean {
        return features.jerk > 15f && features.acceleration > 5f
    }

    /**
     * Reset analyzer state
     */
    fun reset() {
        lastAccelX = 0f
        lastAccelY = 0f
        lastAccelZ = 0f
        lastTimestamp = 0L
        activityStartTime = 0L
        currentActivity = ActivityType.IDLE
    }
}