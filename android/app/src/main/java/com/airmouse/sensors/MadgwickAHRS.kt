package com.airmouse.sensors

import kotlin.math.*

/**
 * Madgwick AHRS (Attitude and Heading Reference System) implementation.
 * Fuses gyroscope, accelerometer, and magnetometer data to estimate orientation.
 * Based on the work of Sebastian Madgwick (2010).
 *
 * This implementation provides accurate 3D orientation estimation with low computational cost,
 * suitable for real-time applications on mobile devices.
 */
class MadgwickAHRS(
    private var beta: Float = 0.1f,  // proportional gain (gyroscope measurement error)
    private var zeta: Float = 0.0f   // gyroscope drift correction gain (optional)
) {

    // Quaternion representing orientation (w, x, y, z)
    private val quaternion = FloatArray(4).apply {
        this[0] = 1f  // w
        this[1] = 0f  // x
        this[2] = 0f  // y
        this[3] = 0f  // z
    }

    // Alternative representation as Euler angles (cached)
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private var needsRecalc = true

    companion object {
        private const val EPSILON = 1e-6f
    }

    /**
     * Constructor with default beta value
     */
    constructor() : this(0.1f, 0.0f)

    /**
     * Update the filter with gyroscope data only (fast update, no correction)
     * Use this for high-speed updates when accuracy is less critical.
     *
     * @param gx gyroscope x-axis angular velocity (rad/s)
     * @param gy gyroscope y-axis angular velocity (rad/s)
     * @param gz gyroscope z-axis angular velocity (rad/s)
     * @param dt time step (seconds)
     */
    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (dt <= 0f) return

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
        needsRecalc = true
    }

    /**
     * Update the filter with accelerometer data (gravity vector correction)
     * This corrects roll and pitch using gravity reference.
     *
     * @param ax accelerometer x-axis (m/s²)
     * @param ay accelerometer y-axis (m/s²)
     * @param az accelerometer z-axis (m/s²)
     * @param dt time step (seconds)
     */
    fun updateAccel(ax: Float, ay: Float, az: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Normalise accelerometer measurement
        val norm = sqrt(ax * ax + ay * ay + az * az)
        if (norm < EPSILON) return
        val axN = ax / norm
        val ayN = ay / norm
        val azN = az / norm

        // Estimated direction of gravity from quaternion
        val vx = 2f * (q1 * q3 - q0 * q2)
        val vy = 2f * (q0 * q1 + q2 * q3)
        val vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3

        // Error is cross product between estimated and measured direction of gravity
        val ex = (ayN * vz - azN * vy)
        val ey = (azN * vx - axN * vz)
        val ez = (axN * vy - ayN * vx)

        // Apply feedback to gyroscope error estimate
        val step = dt.coerceAtLeast(0.001f)
        val q0Dot = -beta * 2f * ex * step
        val q1Dot = -beta * 2f * ey * step
        val q2Dot = -beta * 2f * ez * step

        quaternion[0] += q0Dot
        quaternion[1] += q1Dot
        quaternion[2] += q2Dot
        // q3 not directly corrected by accelerometer

        normalizeQuaternion()
        needsRecalc = true
    }

    /**
     * Update the filter with magnetometer data (yaw correction)
     * This corrects heading using Earth's magnetic field.
     *
     * @param mx magnetometer x-axis (µT)
     * @param my magnetometer y-axis (µT)
     * @param mz magnetometer z-axis (µT)
     * @param dt time step (seconds)
     */
    fun updateMag(mx: Float, my: Float, mz: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Normalise magnetometer measurement
        val norm = sqrt(mx * mx + my * my + mz * mz)
        if (norm < EPSILON) return
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

        val bx = sqrt(hx * hx + hy * hy)
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

        // Apply feedback using beta (or use separate zeta for gyro drift)
        val step = dt.coerceAtLeast(0.001f)
        val correctionFactor = if (zeta > 0) zeta else beta

        quaternion[0] += -correctionFactor * ex * step
        quaternion[1] += -correctionFactor * ey * step
        quaternion[2] += -correctionFactor * ez * step

        normalizeQuaternion()
        needsRecalc = true
    }

    /**
     * Combined update using gyroscope, accelerometer, and magnetometer.
     * This is the recommended method for full 9-DOF sensor fusion.
     *
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
    fun update(
        gx: Float, gy: Float, gz: Float,
        ax: Float, ay: Float, az: Float,
        mx: Float, my: Float, mz: Float,
        dt: Float
    ) {
        if (dt <= 0f) return

        // First update with gyroscope
        updateGyro(gx, gy, gz, dt)

        // Then apply accelerometer correction
        updateAccel(ax, ay, az, dt)

        // Then apply magnetometer correction
        updateMag(mx, my, mz, dt)

        // Optionally apply gyro drift correction
        if (zeta > 0) {
            applyGyroDriftCorrection(gx, gy, gz, dt)
        }
    }

    /**
     * Update using only gyroscope and accelerometer (6-DOF)
     * Use this when magnetometer is unavailable.
     */
    fun updateImu(gx: Float, gy: Float, gz: Float, ax: Float, ay: Float, az: Float, dt: Float) {
        if (dt <= 0f) return
        updateGyro(gx, gy, gz, dt)
        updateAccel(ax, ay, az, dt)
        needsRecalc = true
    }

    /**
     * Apply gyroscope drift correction using zeta parameter.
     */
    private fun applyGyroDriftCorrection(gx: Float, gy: Float, gz: Float, dt: Float) {
        // Simplified drift correction based on Madgwick's algorithm
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        val qDot1 = -0.5f * (q1 * gx + q2 * gy + q3 * gz)
        val qDot2 = 0.5f * (q0 * gx - q3 * gy + q2 * gz)
        val qDot3 = 0.5f * (q3 * gx + q0 * gy - q1 * gz)
        val qDot4 = 0.5f * (-q2 * gx + q1 * gy + q0 * gz)

        val step = dt * zeta
        quaternion[0] += qDot1 * step
        quaternion[1] += qDot2 * step
        quaternion[2] += qDot3 * step
        quaternion[3] += qDot4 * step

        normalizeQuaternion()
    }

    /**
     * Get roll angle (rotation around X axis) in degrees.
     * @return roll angle (-180 to 180)
     */
    fun getRollDegrees(): Float {
        recalcEulerIfNeeded()
        return roll
    }

    /**
     * Get pitch angle (rotation around Y axis) in degrees.
     * @return pitch angle (-90 to 90)
     */
    fun getPitchDegrees(): Float {
        recalcEulerIfNeeded()
        return pitch
    }

    /**
     * Get yaw angle (rotation around Z axis) in degrees.
     * @return yaw angle (-180 to 180)
     */
    fun getYawDegrees(): Float {
        recalcEulerIfNeeded()
        return yaw
    }

    /**
     * Get roll angle in radians.
     */
    fun getRoll(): Float = getRollDegrees() * (PI.toFloat() / 180f)

    /**
     * Get pitch angle in radians.
     */
    fun getPitch(): Float = getPitchDegrees() * (PI.toFloat() / 180f)

    /**
     * Get yaw angle in radians.
     */
    fun getYaw(): Float = getYawDegrees() * (PI.toFloat() / 180f)

    /**
     * Recalculate Euler angles from quaternion if needed.
     */
    private fun recalcEulerIfNeeded() {
        if (!needsRecalc) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        // Roll (X-axis rotation)
        val sinr_cosp = 2f * (q0 * q1 + q2 * q3)
        val cosr_cosp = 1f - 2f * (q1 * q1 + q2 * q2)
        roll = atan2(sinr_cosp, cosr_cosp) * (180f / PI.toFloat())

        // Pitch (Y-axis rotation)
        val sinp = 2f * (q0 * q2 - q3 * q1)
        pitch = when {
            abs(sinp) >= 1f -> sign(sinp) * 90f
            else -> asin(sinp) * (180f / PI.toFloat())
        }

        // Yaw (Z-axis rotation)
        val siny_cosp = 2f * (q0 * q3 + q1 * q2)
        val cosy_cosp = 1f - 2f * (q2 * q2 + q3 * q3)
        yaw = atan2(siny_cosp, cosy_cosp) * (180f / PI.toFloat())

        needsRecalc = false
    }

    /**
     * Get the current quaternion as a float array [w, x, y, z].
     */
    fun getQuaternion(): FloatArray = quaternion.copyOf()

    /**
     * Set the beta (proportional gain) value.
     * Typical values: 0.1 for fast response, 0.05 for smoother response.
     * @param beta new beta value (0.01 to 1.0)
     */
    fun setBeta(beta: Float) {
        this.beta = beta.coerceIn(0.01f, 1.0f)
    }

    /**
     * Set the zeta (gyroscope drift correction) value.
     * @param zeta new zeta value (0.0 to 0.1)
     */
    fun setZeta(zeta: Float) {
        this.zeta = zeta.coerceIn(0f, 0.1f)
    }

    /**
     * Reset orientation to identity quaternion (no rotation).
     */
    fun reset() {
        quaternion[0] = 1f
        quaternion[1] = 0f
        quaternion[2] = 0f
        quaternion[3] = 0f
        yaw = 0f
        pitch = 0f
        roll = 0f
        needsRecalc = false
    }

    /**
     * Reset orientation using accelerometer and magnetometer data.
     * This provides an initial orientation estimate before sensor fusion starts.
     */
    fun resetWithSensors(ax: Float, ay: Float, az: Float, mx: Float, my: Float, mz: Float) {
        // Normalize accelerometer
        val normA = sqrt(ax * ax + ay * ay + az * az)
        if (normA < EPSILON) return

        val axN = ax / normA
        val ayN = ay / normA
        val azN = az / normA

        // Normalize magnetometer
        val normM = sqrt(mx * mx + my * my + mz * mz)
        if (normM < EPSILON) return

        val mxN = mx / normM
        val myN = my / normM
        val mzN = mz / normM

        // Compute initial roll and pitch from accelerometer
        val initialRoll = atan2(ayN, azN)
        val initialPitch = atan2(-axN, sqrt(ayN * ayN + azN * azN))

        // Compute heading from magnetometer
        val cosRoll = cos(initialRoll)
        val sinRoll = sin(initialRoll)
        val cosPitch = cos(initialPitch)
        val sinPitch = sin(initialPitch)

        val mxH = mxN * cosPitch + mzN * sinPitch
        val myH = mxN * sinRoll * sinPitch + myN * cosRoll - mzN * sinRoll * cosPitch
        val initialYaw = atan2(-myH, mxH)

        // Convert Euler angles to quaternion
        val cy = cos(initialYaw * 0.5f)
        val sy = sin(initialYaw * 0.5f)
        val cp = cos(initialPitch * 0.5f)
        val sp = sin(initialPitch * 0.5f)
        val cr = cos(initialRoll * 0.5f)
        val sr = sin(initialRoll * 0.5f)

        quaternion[0] = cr * cp * cy + sr * sp * sy
        quaternion[1] = sr * cp * cy - cr * sp * sy
        quaternion[2] = cr * sp * cy + sr * cp * sy
        quaternion[3] = cr * cp * sy - sr * sp * cy

        normalizeQuaternion()
        needsRecalc = true
    }

    /**
     * Normalize the quaternion to unit length.
     */
    private fun normalizeQuaternion() {
        val norm = sqrt(quaternion[0] * quaternion[0] +
                quaternion[1] * quaternion[1] +
                quaternion[2] * quaternion[2] +
                quaternion[3] * quaternion[3])
        if (norm < EPSILON) return
        quaternion[0] /= norm
        quaternion[1] /= norm
        quaternion[2] /= norm
        quaternion[3] /= norm
    }

    /**
     * Get the current orientation as a rotation matrix (3x3).
     */
    fun getRotationMatrix(): FloatArray {
        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

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
     * Get current configuration parameters.
     */
    fun getConfig(): Map<String, Float> {
        return mapOf(
            "beta" to beta,
            "zeta" to zeta,
            "yaw" to yaw,
            "pitch" to pitch,
            "roll" to roll
        )
    }
}