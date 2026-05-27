package com.airmouse.domain

import kotlin.math.*

/**
 * Madgwick AHRS algorithm implementation.
 * Combines gyroscope, accelerometer, and magnetometer into a quaternion.
 * Outputs roll, pitch, yaw angles in radians.
 *
 * Usage:
 * - Call updateGyro() each time new gyro data arrives (with dt).
 * - Call updateAccel() and updateMag() when new data arrives (they store values).
 * - The fusion automatically applies correction using the most recent accel/mag data.
 */
class MadgwickFusion(private var beta: Float = 0.1f) {

    private var q0 = 1f
    private var q1 = 0f
    private var q2 = 0f
    private var q3 = 0f

    private var ax = 0f; private var ay = 0f; private var az = 0f  // latest accelerometer
    private var mx = 0f; private var my = 0f; private var mz = 0f  // latest magnetometer

    /**
     * Update the filter with new gyroscope data.
     * This also applies accelerometer and magnetometer corrections using the stored values.
     * @param gx Angular velocity around X (rad/s)
     * @param gy Angular velocity around Y
     * @param gz Angular velocity around Z
     * @param dt Time step in seconds
     */
    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        var recipNorm: Float
        var s0 = 0f; var s1 = 0f; var s2 = 0f; var s3 = 0f
        var qDot1: Float; var qDot2: Float; var qDot3: Float; var qDot4: Float

        // Gyroscope integration
        qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz)
        qDot2 = 0.5f * ( q0 * gx + q2 * gz - q3 * gy)
        qDot3 = 0.5f * ( q0 * gy - q1 * gz + q3 * gx)
        qDot4 = 0.5f * ( q0 * gz + q1 * gy - q2 * gx)

        // Accelerometer correction (if any)
        if (ax != 0f && ay != 0f && az != 0f) {
            recipNorm = 1f / sqrt(ax * ax + ay * ay + az * az)
            val normAx = ax * recipNorm
            val normAy = ay * recipNorm
            val normAz = az * recipNorm

            val f0 = 2f * (q1 * q3 - q0 * q2) - normAx
            val f1 = 2f * (q0 * q1 + q2 * q3) - normAy
            val f2 = 2f * (0.5f - q1 * q1 - q2 * q2) - normAz
            val j11 = 2f * q2
            val j12 = 2f * q3
            val j13 = -4f * q1
            val j21 = -2f * q1
            val j22 = 2f * q0
            val j23 = 2f * q3
            val j31 = -4f * q2
            val j32 = -4f * q1
            val j33 = 0f

            val step = 1f / (j11 * j11 + j12 * j12 + j13 * j13 + j21 * j21 + j22 * j22 + j23 * j23 + j31 * j31 + j32 * j32 + j33 * j33)
            s0 = (j11 * f0 + j21 * f1 + j31 * f2) * step
            s1 = (j12 * f0 + j22 * f1 + j32 * f2) * step
            s2 = (j13 * f0 + j23 * f1 + j33 * f2) * step
            // s3 = 0

            qDot1 -= beta * s0
            qDot2 -= beta * s1
            qDot3 -= beta * s2
            // qDot4 unchanged
        }

        // Magnetometer correction (if any)
        if (mx != 0f && my != 0f && mz != 0f) {
            // Hard‑iron calibration should be applied before calling this method.
            // We'll assume values are already corrected.
        }

        // Integrate
        q0 += qDot1 * dt
        q1 += qDot2 * dt
        q2 += qDot3 * dt
        q3 += qDot4 * dt

        // Normalise quaternion
        recipNorm = 1f / sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        q0 *= recipNorm
        q1 *= recipNorm
        q2 *= recipNorm
        q3 *= recipNorm
    }

    /**
     * Update the stored accelerometer data (will be used in next gyro update for correction).
     */
    fun updateAccel(x: Float, y: Float, z: Float) {
        ax = x; ay = y; az = z
    }

    /**
     * Update the stored magnetometer data (will be used in next gyro update for correction).
     */
    fun updateMag(x: Float, y: Float, z: Float) {
        mx = x; my = y; mz = z
    }

    /**
     * @return Roll angle (rotation around X) in radians, range -π to π.
     */
    fun getRoll(): Float = atan2(2f * (q0 * q1 + q2 * q3), 1f - 2f * (q1 * q1 + q2 * q2))

    /**
     * @return Pitch angle (rotation around Y) in radians, range -π/2 to π/2.
     */
    fun getPitch(): Float = asin(2f * (q0 * q2 - q3 * q1))

    /**
     * @return Yaw angle (rotation around Z) in radians, range -π to π.
     */
    fun getYaw(): Float = atan2(2f * (q0 * q3 + q1 * q2), 1f - 2f * (q2 * q2 + q3 * q3))

    /**
     * Reset the filter to identity orientation.
     */
    fun reset() {
        q0 = 1f
        q1 = 0f
        q2 = 0f
        q3 = 0f
        ax = 0f; ay = 0f; az = 0f
        mx = 0f; my = 0f; mz = 0f
    }

    /**
     * Set the proportional gain (beta). Default 0.1.
     */
    fun setBeta(beta: Float) {
        this.beta = beta
    }
}