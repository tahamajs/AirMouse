package com.airmouse.sensors

import android.hardware.SensorManager
import kotlin.math.*

/**
 * Tracks device orientation using sensor fusion
 */
class OrientationTracker {

    data class Orientation(
        val yaw: Float,
        val pitch: Float,
        val roll: Float,
        val quaternion: FloatArray,
        val confidence: Float
    )

    data class RotationDelta(
        val deltaYaw: Float,
        val deltaPitch: Float,
        val deltaRoll: Float,
        val timestamp: Long
    )

    private var lastYaw = 0f
    private var lastPitch = 0f
    private var lastRoll = 0f
    private var lastTimestamp = 0L

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    /**
     * Update orientation from rotation vector
     */
    fun updateFromRotationVector(rotationVector: FloatArray, timestamp: Long): Orientation {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        SensorManager.getOrientation(rotationMatrix, orientationValues)

        val yaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

        lastYaw = yaw
        lastPitch = pitch
        lastRoll = roll
        lastTimestamp = timestamp

        return Orientation(yaw, pitch, roll, rotationVector.clone(), 0.95f)
    }

    /**
     * Update orientation from gyroscope and accelerometer
     */
    fun updateFromSensors(
        gyroX: Float, gyroY: Float, gyroZ: Float,
        accelX: Float, accelY: Float, accelZ: Float,
        timestamp: Long
    ): Orientation {
        val dt = if (lastTimestamp > 0) (timestamp - lastTimestamp) / 1000f else 0.016f

        // Integrate gyroscope
        val yaw = lastYaw + gyroZ * dt
        val pitch = lastPitch + gyroY * dt
        val roll = lastRoll + gyroX * dt

        // Calculate complementary filter with accelerometer
        val accelPitch = atan2(accelY, sqrt(accelX * accelX + accelZ * accelZ)) * 180f / PI.toFloat()
        val accelRoll = atan2(-accelX, accelZ) * 180f / PI.toFloat()

        val alpha = 0.96f
        val filteredPitch = alpha * pitch + (1 - alpha) * accelPitch
        val filteredRoll = alpha * roll + (1 - alpha) * accelRoll

        lastYaw = yaw
        lastPitch = filteredPitch
        lastRoll = filteredRoll
        lastTimestamp = timestamp

        // Convert to quaternion
        val quat = eulerToQuaternion(filteredPitch, filteredRoll, yaw)

        return Orientation(yaw, filteredPitch, filteredRoll, quat, 0.85f)
    }

    /**
     * Calculate rotation delta since last update
     */
    fun getRotationDelta(): RotationDelta {
        return RotationDelta(
            deltaYaw = lastYaw - lastYaw,
            deltaPitch = lastPitch - lastPitch,
            deltaRoll = lastRoll - lastRoll,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Convert Euler angles to quaternion
     */
    private fun eulerToQuaternion(pitch: Float, roll: Float, yaw: Float): FloatArray {
        val cy = cos(Math.toRadians(yaw.toDouble()) / 2).toFloat()
        val sy = sin(Math.toRadians(yaw.toDouble()) / 2).toFloat()
        val cp = cos(Math.toRadians(pitch.toDouble()) / 2).toFloat()
        val sp = sin(Math.toRadians(pitch.toDouble()) / 2).toFloat()
        val cr = cos(Math.toRadians(roll.toDouble()) / 2).toFloat()
        val sr = sin(Math.toRadians(roll.toDouble()) / 2).toFloat()

        val w = cr * cp * cy + sr * sp * sy
        val x = sr * cp * cy - cr * sp * sy
        val y = cr * sp * cy + sr * cp * sy
        val z = cr * cp * sy - sr * sp * cy

        return floatArrayOf(w, x, y, z)
    }

    /**
     * Get rotation matrix for OpenGL
     */
    fun getRotationMatrix(): FloatArray {
        return rotationMatrix.clone()
    }

    /**
     * Reset orientation tracking
     */
    fun reset() {
        lastYaw = 0f
        lastPitch = 0f
        lastRoll = 0f
        lastTimestamp = 0L
    }
}