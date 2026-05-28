package com.airmouse.domain

import com.airmouse.sensors.MadgwickAHRS
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
    private val filter = MadgwickAHRS(beta)
    private var lastDt = 0.01f

    /**
     * Update the filter with new gyroscope data.
     * This also applies accelerometer and magnetometer corrections using the stored values.
     * @param gx Angular velocity around X (rad/s)
     * @param gy Angular velocity around Y
     * @param gz Angular velocity around Z
     * @param dt Time step in seconds
     */
    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        lastDt = if (dt > 0f) dt else lastDt
        filter.setBeta(beta)
        filter.updateGyro(gx, gy, gz, lastDt)
    }

    /**
     * Update the stored accelerometer data (will be used in next gyro update for correction).
     */
    fun updateAccel(x: Float, y: Float, z: Float) {
        filter.updateAccel(x, y, z, lastDt)
    }

    /**
     * Update the stored magnetometer data (will be used in next gyro update for correction).
     */
    fun updateMag(x: Float, y: Float, z: Float) {
        filter.updateMag(x, y, z, lastDt)
    }

    /**
     * @return Roll angle (rotation around X) in radians, range -π to π.
     */
    fun getRoll(): Float = filter.getRoll()

    /**
     * @return Pitch angle (rotation around Y) in radians, range -π/2 to π/2.
     */
    fun getPitch(): Float = filter.getPitch()

    /**
     * @return Yaw angle (rotation around Z) in radians, range -π to π.
     */
    fun getYaw(): Float = filter.getYaw()

    /**
     * Reset the filter to identity orientation.
     */
    fun reset() {
        filter.reset()
        lastDt = 0.01f
    }

    /**
     * Set the proportional gain (beta). Default 0.1.
     */
    fun setBeta(beta: Float) {
        this.beta = beta
        filter.setBeta(beta)
    }
}