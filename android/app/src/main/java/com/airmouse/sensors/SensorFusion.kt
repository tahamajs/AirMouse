package com.airmouse.sensors

import kotlin.math.*

/**
 * Sensor fusion using Madgwick AHRS algorithm.
 * Combines gyroscope, accelerometer, and optional magnetometer data
 * to estimate device orientation in quaternion form.
 *
 * This implementation is optimized for real-time performance on mobile devices.
 */
object SensorFusion {

    // Quaternion (w, x, y, z)
    private var q0 = 1.0f
    private var q1 = 0.0f
    private var q2 = 0.0f
    private var q3 = 0.0f

    // Filter gain (0.041 is the recommended default)
    private var beta = 0.041f

    // Optional gyro drift correction
    private var zeta = 0.0f

    // Cached Euler angles for fast access
    private var cachedRoll = 0f
    private var cachedPitch = 0f
    private var cachedYaw = 0f
    private var needsRecalc = true

    // Sensor availability flags
    private var hasMagnetometer = false
    private var hasAccelerometer = false
    private var hasGyroscope = false

    // Performance counters
    private var updateCount = 0
    private var lastDt = 0.01f

    /**
     * Update the filter with sensor data (full 9-DOF fusion)
     *
     * @param gx Gyroscope X-axis (deg/s)
     * @param gy Gyroscope Y-axis (deg/s)
     * @param gz Gyroscope Z-axis (deg/s)
     * @param ax Accelerometer X-axis (m/s²)
     * @param ay Accelerometer Y-axis (m/s²)
     * @param az Accelerometer Z-axis (m/s²)
     * @param mx Magnetometer X-axis (µT)
     * @param my Magnetometer Y-axis (µT)
     * @param mz Magnetometer Z-axis (µT)
     * @param dt Time step in seconds
     */
    fun update(
        gx: Float, gy: Float, gz: Float,
        ax: Float, ay: Float, az: Float,
        mx: Float, my: Float, mz: Float,
        dt: Float
    ) {
        if (dt <= 0f) return

        lastDt = dt

        // Convert gyroscope from deg/s to rad/s
        val gxRad = Math.toRadians(gx.toDouble()).toFloat()
        val gyRad = Math.toRadians(gy.toDouble()).toFloat()
        val gzRad = Math.toRadians(gz.toDouble()).toFloat()

        // Pre-compute quaternion components
        val q0Sq = q0 * q0
        val q1Sq = q1 * q1
        val q2Sq = q2 * q2
        val q3Sq = q3 * q3

        // Rate of change from gyroscope
        var qDot1 = 0.5f * (-q1 * gxRad - q2 * gyRad - q3 * gzRad)
        var qDot2 = 0.5f * ( q0 * gxRad + q2 * gzRad - q3 * gyRad)
        var qDot3 = 0.5f * ( q0 * gyRad - q1 * gzRad + q3 * gxRad)
        var qDot4 = 0.5f * ( q0 * gzRad + q1 * gyRad - q2 * gxRad)

        // Check if we have valid accelerometer data
        val hasValidAccel = abs(ax) > 0.01f || abs(ay) > 0.01f || abs(az) > 0.01f

        if (hasValidAccel) {
            // Normalize accelerometer
            val accelNorm = sqrt(ax * ax + ay * ay + az * az)
            if (accelNorm > 1e-6f) {
                val axNorm = ax / accelNorm
                val ayNorm = ay / accelNorm
                val azNorm = az / accelNorm

                // Gradient descent for accelerometer
                val vx = 2.0f * (q1 * q3 - q0 * q2)
                val vy = 2.0f * (q0 * q1 + q2 * q3)
                val vz = q0Sq - q1Sq - q2Sq + q3Sq

                val ex = (ayNorm * vz - azNorm * vy)
                val ey = (azNorm * vx - axNorm * vz)
                val ez = (axNorm * vy - ayNorm * vx)

                // Apply feedback
                val step = beta * dt
                qDot1 -= 2.0f * ex * step
                qDot2 -= 2.0f * ey * step
                qDot3 -= 2.0f * ez * step
                // qDot4 unchanged from gyro
            }
        }

        // Magnetometer correction (if data is available)
        val hasValidMag = (abs(mx) > 1f || abs(my) > 1f || abs(mz) > 1f) &&
                !(mx == 0f && my == 0f && mz == 0f)

        if (hasValidMag) {
            val magNorm = sqrt(mx * mx + my * my + mz * mz)
            if (magNorm > 1e-6f) {
                val mxNorm = mx / magNorm
                val myNorm = my / magNorm
                val mzNorm = mz / magNorm

                // Reference direction of Earth's magnetic field
                val hx = 2.0f * (mxNorm * (0.5f - q2Sq - q3Sq) +
                        myNorm * (q1 * q2 - q0 * q3) +
                        mzNorm * (q1 * q3 + q0 * q2))
                val hy = 2.0f * (mxNorm * (q1 * q2 + q0 * q3) +
                        myNorm * (0.5f - q1Sq - q3Sq) +
                        mzNorm * (q2 * q3 - q0 * q1))

                val bx = sqrt(hx * hx + hy * hy)
                val bz = 2.0f * (mxNorm * (q1 * q3 - q0 * q2) +
                        myNorm * (q2 * q3 + q0 * q1) +
                        mzNorm * (0.5f - q1Sq - q2Sq))

                // Estimated direction of magnetic field
                val wx = 2.0f * bx * (0.5f - q2Sq - q3Sq) + 2.0f * bz * (q1 * q3 - q0 * q2)
                val wy = 2.0f * bx * (q1 * q2 - q0 * q3) + 2.0f * bz * (q0 * q1 + q2 * q3)
                val wz = 2.0f * bx * (q0 * q2 + q1 * q3) + 2.0f * bz * (0.5f - q1Sq - q2Sq)

                // Error is cross product between measured and estimated field
                val ex = (myNorm * wz - mzNorm * wy)
                val ey = (mzNorm * wx - mxNorm * wz)
                val ez = (mxNorm * wy - myNorm * wx)

                // Apply magnetometer correction
                val step = beta * dt
                qDot1 -= ex * step
                qDot2 -= ey * step
                qDot3 -= ez * step
            }
        }

        // Integrate quaternion
        q0 += qDot1 * dt
        q1 += qDot2 * dt
        q2 += qDot3 * dt
        q3 += qDot4 * dt

        // Normalize quaternion
        val norm = sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        if (norm > 1e-6f) {
            q0 /= norm
            q1 /= norm
            q2 /= norm
            q3 /= norm
        }

        needsRecalc = true
        updateCount++

        // Optional gyro drift correction
        if (zeta > 0) {
            applyGyroDriftCorrection(gxRad, gyRad, gzRad, dt)
        }
    }

    /**
     * Simplified update for IMU (accelerometer + gyroscope only)
     */
    fun updateImu(
        gx: Float, gy: Float, gz: Float,
        ax: Float, ay: Float, az: Float,
        dt: Float
    ) {
        update(gx, gy, gz, ax, ay, az, 0f, 0f, 0f, dt)
    }

    /**
     * Gyro-only update (fastest, but drifts over time)
     */
    fun updateGyroOnly(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (dt <= 0f) return

        val gxRad = Math.toRadians(gx.toDouble()).toFloat()
        val gyRad = Math.toRadians(gy.toDouble()).toFloat()
        val gzRad = Math.toRadians(gz.toDouble()).toFloat()

        val qDot1 = 0.5f * (-q1 * gxRad - q2 * gyRad - q3 * gzRad)
        val qDot2 = 0.5f * ( q0 * gxRad + q2 * gzRad - q3 * gyRad)
        val qDot3 = 0.5f * ( q0 * gyRad - q1 * gzRad + q3 * gxRad)
        val qDot4 = 0.5f * ( q0 * gzRad + q1 * gyRad - q2 * gxRad)

        q0 += qDot1 * dt
        q1 += qDot2 * dt
        q2 += qDot3 * dt
        q3 += qDot4 * dt

        val norm = sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        if (norm > 1e-6f) {
            q0 /= norm
            q1 /= norm
            q2 /= norm
            q3 /= norm
        }

        needsRecalc = true
    }

    private fun applyGyroDriftCorrection(gx: Float, gy: Float, gz: Float, dt: Float) {
        val step = zeta * dt
        val qDot1 = -0.5f * (q1 * gx + q2 * gy + q3 * gz) * step
        val qDot2 = 0.5f * (q0 * gx - q3 * gy + q2 * gz) * step
        val qDot3 = 0.5f * (q3 * gx + q0 * gy - q1 * gz) * step
        val qDot4 = 0.5f * (-q2 * gx + q1 * gy + q0 * gz) * step

        q0 += qDot1
        q1 += qDot2
        q2 += qDot3
        q3 += qDot4

        val norm = sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        if (norm > 1e-6f) {
            q0 /= norm
            q1 /= norm
            q2 /= norm
            q3 /= norm
        }
    }

    /**
     * Get Euler angles (pitch, roll, yaw) in degrees
     */
    fun getEulerAngles(): FloatArray {
        if (needsRecalc) {
            // Roll (X-axis rotation)
            val sinr_cosp = 2.0f * (q0 * q1 + q2 * q3)
            val cosr_cosp = 1.0f - 2.0f * (q1 * q1 + q2 * q2)
            cachedRoll = atan2(sinr_cosp, cosr_cosp) * (180f / PI.toFloat())

            // Pitch (Y-axis rotation)
            val sinp = 2.0f * (q0 * q2 - q3 * q1)
            cachedPitch = when {
                abs(sinp) >= 1f -> sign(sinp) * 90f
                else -> asin(sinp) * (180f / PI.toFloat())
            }

            // Yaw (Z-axis rotation)
            val siny_cosp = 2.0f * (q0 * q3 + q1 * q2)
            val cosy_cosp = 1.0f - 2.0f * (q2 * q2 + q3 * q3)
            cachedYaw = atan2(siny_cosp, cosy_cosp) * (180f / PI.toFloat())

            needsRecalc = false
        }
        return floatArrayOf(cachedPitch, cachedRoll, cachedYaw)
    }

    /**
     * Get roll angle in degrees
     */
    fun getRoll(): Float = getEulerAngles()[1]

    /**
     * Get pitch angle in degrees
     */
    fun getPitch(): Float = getEulerAngles()[0]

    /**
     * Get yaw angle in degrees
     */
    fun getYaw(): Float = getEulerAngles()[2]

    /**
     * Get roll angle in radians
     */
    fun getRollRad(): Float = getRoll() * (PI.toFloat() / 180f)

    /**
     * Get pitch angle in radians
     */
    fun getPitchRad(): Float = getPitch() * (PI.toFloat() / 180f)

    /**
     * Get yaw angle in radians
     */
    fun getYawRad(): Float = getYaw() * (PI.toFloat() / 180f)

    /**
     * Get current quaternion as [w, x, y, z]
     */
    fun getQuaternion(): FloatArray = floatArrayOf(q0, q1, q2, q3)

    /**
     * Get rotation matrix from quaternion (3x3)
     */
    fun getRotationMatrix(): FloatArray {
        val q00 = q0 * q0
        val q11 = q1 * q1
        val q22 = q2 * q2
        val q33 = q3 * q3

        return floatArrayOf(
            q00 + q11 - q22 - q33, 2f * (q1 * q2 - q0 * q3), 2f * (q1 * q3 + q0 * q2),
            2f * (q1 * q2 + q0 * q3), q00 - q11 + q22 - q33, 2f * (q2 * q3 - q0 * q1),
            2f * (q1 * q3 - q0 * q2), 2f * (q2 * q3 + q0 * q1), q00 - q11 - q22 + q33
        )
    }

    /**
     * Get rotation vector (axis-angle representation)
     */
    fun getRotationVector(): FloatArray {
        val angle = 2f * acos(q0.coerceIn(-1f, 1f))
        if (angle < 1e-6f) return floatArrayOf(0f, 0f, 0f)
        val scale = angle / sin(angle / 2f)
        return floatArrayOf(q1 * scale, q2 * scale, q3 * scale)
    }

    /**
     * Set filter gain beta
     * @param value Beta value (typical: 0.01-0.1, default: 0.041)
     */
    fun setBeta(value: Float) {
        beta = value.coerceIn(0.01f, 0.2f)
    }

    /**
     * Set gyro drift correction gain zeta
     * @param value Zeta value (0.0 to 0.1, default: 0.0)
     */
    fun setZeta(value: Float) {
        zeta = value.coerceIn(0f, 0.1f)
    }

    /**
     * Reset orientation to identity
     */
    fun reset() {
        q0 = 1.0f
        q1 = 0.0f
        q2 = 0.0f
        q3 = 0.0f
        needsRecalc = true
        updateCount = 0
    }

    /**
     * Reset orientation using accelerometer and magnetometer data
     * (provides initial orientation reference)
     */
    fun resetWithSensors(ax: Float, ay: Float, az: Float, mx: Float, my: Float, mz: Float) {
        // Compute roll and pitch from accelerometer
        val rollInit = atan2(ay, az)
        val pitchInit = atan2(-ax, sqrt(ay * ay + az * az))

        // Compute yaw from magnetometer
        val cosRoll = cos(rollInit)
        val sinRoll = sin(rollInit)
        val cosPitch = cos(pitchInit)
        val sinPitch = sin(pitchInit)

        val mxH = mx * cosPitch + mz * sinPitch
        val myH = mx * sinRoll * sinPitch + my * cosRoll - mz * sinRoll * cosPitch
        val yawInit = atan2(-myH, mxH)

        // Convert Euler to quaternion
        val cy = cos(yawInit * 0.5f)
        val sy = sin(yawInit * 0.5f)
        val cp = cos(pitchInit * 0.5f)
        val sp = sin(pitchInit * 0.5f)
        val cr = cos(rollInit * 0.5f)
        val sr = sin(rollInit * 0.5f)

        q0 = cr * cp * cy + sr * sp * sy
        q1 = sr * cp * cy - cr * sp * sy
        q2 = cr * sp * cy + sr * cp * sy
        q3 = cr * cp * sy - sr * sp * cy

        val norm = sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        if (norm > 1e-6f) {
            q0 /= norm
            q1 /= norm
            q2 /= norm
            q3 /= norm
        }

        needsRecalc = true
    }

    /**
     * Get filter status and statistics
     */
    fun getStatus(): Map<String, Any> {
        return mapOf(
            "beta" to beta,
            "zeta" to zeta,
            "updateCount" to updateCount,
            "lastDt" to lastDt,
            "quaternion" to getQuaternion(),
            "euler" to getEulerAngles()
        )
    }

    private fun atan2(y: Float, x: Float): Float = Math.atan2(y.toDouble(), x.toDouble()).toFloat()
    private fun asin(value: Float): Float = Math.asin(value.toDouble()).toFloat()
    private fun acos(value: Float): Float = Math.acos(value.toDouble()).toFloat()
    private fun cos(value: Float): Float = Math.cos(value.toDouble()).toFloat()
    private fun sin(value: Float): Float = Math.sin(value.toDouble()).toFloat()
    private fun sign(value: Float): Float = if (value >= 0) 1f else -1f
}