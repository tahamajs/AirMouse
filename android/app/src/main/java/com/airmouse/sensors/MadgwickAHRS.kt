package com.airmouse.sensors

import kotlin.math.sqrt

/**
 * Madgwick AHRS (Attitude and Heading Reference System) implementation.
 * Fuses gyroscope, accelerometer, and magnetometer data to estimate orientation.
 * Based on the work of Sebastian Madgwick.
 */
class MadgwickAHRS(beta: Float = 0.1f) {

    // Quaternion representing orientation (w, x, y, z)
    private val quaternion = FloatArray(4).apply {
        this[0] = 1f  // w
        this[1] = 0f  // x
        this[2] = 0f  // y
        this[3] = 0f  // z
    }

    private var beta = beta  // proportional gain (gyroscope measurement error)

    /**
     * Update the filter with gyroscope data only (fast update, no correction)
     * @param gx gyroscope x-axis angular velocity (rad/s)
     * @param gy gyroscope y-axis angular velocity (rad/s)
     * @param gz gyroscope z-axis angular velocity (rad/s)
     * @param dt time step (seconds)
     */
    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Rate of change of quaternion from gyroscope
        val qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz)
        val qDot2 = 0.5f * ( q0 * gx + q2 * gz - q3 * gy)
        val qDot3 = 0.5f * ( q0 * gy - q1 * gz + q3 * gx)
        val qDot4 = 0.5f * ( q0 * gz + q1 * gy - q2 * gx)

        // Integrate
        quaternion[0] += qDot1 * dt
        quaternion[1] += qDot2 * dt
        quaternion[2] += qDot3 * dt
        quaternion[3] += qDot4 * dt

        normalizeQuaternion()
    }

    /**
     * Update the filter with accelerometer data (gravity vector correction)
     * @param ax accelerometer x-axis (m/s²)
     * @param ay accelerometer y-axis (m/s²)
     * @param az accelerometer z-axis (m/s²)
     */
    fun updateAccel(ax: Float, ay: Float, az: Float, dt: Float) {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Normalise accelerometer measurement
        val norm = sqrt(ax * ax + ay * ay + az * az).toFloat()
        if (norm == 0f) return
        val axN = ax / norm
        val ayN = ay / norm
        val azN = az / norm

        // Estimated direction of gravity
        val vx = 2f * (q1 * q3 - q0 * q2)
        val vy = 2f * (q0 * q1 + q2 * q3)
        val vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3

        // Error is cross product between estimated and measured direction of gravity
        val ex = (ayN * vz - azN * vy)
        val ey = (azN * vx - axN * vz)
        val ez = (axN * vy - ayN * vx)

        // Apply feedback to gyroscope error estimate
        val q0Dot = -beta * 2f * ex
        val q1Dot = -beta * 2f * ey
        val q2Dot = -beta * 2f * ez
        val q3Dot = 0f  // Not used in this simplified accelerometer-only correction

        val step = if (dt > 0f) dt else 0.01f
        quaternion[0] += q0Dot * step
        quaternion[1] += q1Dot * step
        quaternion[2] += q2Dot * step
        quaternion[3] += q3Dot * step

        normalizeQuaternion()
    }

    /**
     * Update the filter with magnetometer data (yaw correction)
     * @param mx magnetometer x-axis (µT)
     * @param my magnetometer y-axis (µT)
     * @param mz magnetometer z-axis (µT)
     */
    fun updateMag(mx: Float, my: Float, mz: Float, dt: Float) {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Normalise magnetometer measurement
        val norm = sqrt(mx * mx + my * my + mz * mz).toFloat()
        if (norm == 0f) return
        val mxN = mx / norm
        val myN = my / norm
        val mzN = mz / norm

        // Reference direction of Earth's magnetic field
        val hx = 2f * (mxN * (0.5f - q2 * q2 - q3 * q3) +
                myN * (q1 * q2 - q0 * q3) +
                mzN * (q1 * q3 + q0 * q2))
        val hy = 2f * (mxN * (q1 * q2 + q0 * q3) +
                myN * (0.5f - q1 * q1 - q3 * q3) +
                mzN * (q2 * q3 - q0 * q1))
        val hz = 2f * (mxN * (q1 * q3 - q0 * q2) +
                myN * (q2 * q3 + q0 * q1) +
                mzN * (0.5f - q1 * q1 - q2 * q2))

        val bx = sqrt(hx * hx + hy * hy).toFloat()
        val bz = hz

        // Estimated direction of magnetic field
        val wx = 2f * (bx * (0.5f - q2 * q2 - q3 * q3) +
                bz * (q1 * q3 - q0 * q2))
        val wy = 2f * (bx * (q1 * q2 - q0 * q3) +
                bz * (q0 * q1 + q2 * q3))
        val wz = 2f * (bx * (q0 * q2 + q1 * q3) +
                bz * (0.5f - q1 * q1 - q2 * q2))

        // Error is cross product between measured and estimated field
        val ex = (myN * wz - mzN * wy)
        val ey = (mzN * wx - mxN * wz)
        val ez = (mxN * wy - myN * wx)

        // Apply feedback (simplified – uses same beta as accelerometer)
        val q0Dot = -beta * ex
        val q1Dot = -beta * ey
        val q2Dot = -beta * ez
        val q3Dot = 0f

        val step = if (dt > 0f) dt else 0.01f
        quaternion[0] += q0Dot * step
        quaternion[1] += q1Dot * step
        quaternion[2] += q2Dot * step
        quaternion[3] += q3Dot * step

        normalizeQuaternion()
    }

    /**
     * Combined update using gyroscope, accelerometer, and magnetometer.
     * This is the recommended method for full fusion.
     * @param gx gyro x (rad/s)
     * @param gy gyro y (rad/s)
     * @param gz gyro z (rad/s)
     * @param ax accel x (m/s²)
     * @param ay accel y (m/s²)
     * @param az accel z (m/s²)
     * @param mx mag x (µT)
     * @param my mag y (µT)
     * @param mz mag z (µT)
     * @param dt time step (seconds)
     */
    fun update(gx: Float, gy: Float, gz: Float,
               ax: Float, ay: Float, az: Float,
               mx: Float, my: Float, mz: Float,
               dt: Float) {
        // First update with gyroscope
        updateGyro(gx, gy, gz, dt)
        // Then apply accelerometer correction
        updateAccel(ax, ay, az, dt)
        // Then apply magnetometer correction
        updateMag(mx, my, mz, dt)
    }

    /**
     * Get roll angle (rotation around X axis) in radians.
     * @return roll angle (-π to π)
     */
    fun getRoll(): Float {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]
        return kotlin.math.atan2(2f * (q0 * q1 + q2 * q3), 1f - 2f * (q1 * q1 + q2 * q2)).toFloat()
    }

    /**
     * Get pitch angle (rotation around Y axis) in radians.
     * @return pitch angle (-π/2 to π/2)
     */
    fun getPitch(): Float {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]
        val value = 2f * (q0 * q2 - q3 * q1)
        return kotlin.math.asin(value.coerceIn(-1f, 1f)).toFloat()
    }

    /**
     * Get yaw angle (rotation around Z axis) in radians.
     * @return yaw angle (-π to π)
     */
    fun getYaw(): Float {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]
        return kotlin.math.atan2(2f * (q0 * q3 + q1 * q2), 1f - 2f * (q2 * q2 + q3 * q3)).toFloat()
    }

    /**
     * Get the current quaternion.
     * @return float array [w, x, y, z]
     */
    fun getQuaternion(): FloatArray = quaternion.copyOf()

    /**
     * Set the beta (proportional gain) value.
     * @param beta new beta value (typically 0.1)
     */
    fun setBeta(beta: Float) {
        this.beta = beta
    }

    /**
     * Reset orientation to identity quaternion (no rotation).
     */
    fun reset() {
        quaternion[0] = 1f
        quaternion[1] = 0f
        quaternion[2] = 0f
        quaternion[3] = 0f
    }

    /**
     * Normalize the quaternion to unit length.
     */
    private fun normalizeQuaternion() {
        val norm = sqrt(quaternion[0] * quaternion[0] +
                quaternion[1] * quaternion[1] +
                quaternion[2] * quaternion[2] +
                quaternion[3] * quaternion[3]).toFloat()
        if (norm == 0f) return
        quaternion[0] /= norm
        quaternion[1] /= norm
        quaternion[2] /= norm
        quaternion[3] /= norm
    }
}