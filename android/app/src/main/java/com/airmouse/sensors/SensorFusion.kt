package com.airmouse.sensors

import kotlin.math.abs
import kotlin.math.sqrt

object SensorFusion {

    private var q0 = 1.0f
    private var q1 = 0.0f
    private var q2 = 0.0f
    private var q3 = 0.0f
    private var beta = 0.041f   // filter gain

    /**
     * Update the quaternion estimate.
     * @param gx gyro x (rad/s)
     * @param gy gyro y
     * @param gz gyro z
     * @param ax accelerometer x
     * @param ay accelerometer y
     * @param az accelerometer z
     * @param mx magnetometer x (optional, set to 0 if not used)
     * @param my magnetometer y
     * @param mz magnetometer z
     * @param dt sample period in seconds
     */
    fun update(gx: Float, gy: Float, gz: Float,
               ax: Float, ay: Float, az: Float,
               mx: Float, my: Float, mz: Float,
               dt: Float
    ) {
        var recipNorm: Float
        var s0: Float; var s1: Float; var s2: Float; var s3: Float
        var qDot1: Float; var qDot2: Float; var qDot3: Float; var qDot4: Float
        var hx: Float; var hy: Float
        var _2q0mx: Float; var _2q0my: Float; var _2q0mz: Float
        var _2q1mx: Float; var _2bx: Float; var _2bz: Float
        var _4bx: Float; var _4bz: Float
        var _8bx: Float; var _8bz: Float

        // Convert gyroscope to rad/s
        val gx_rad = Math.toRadians(gx.toDouble()).toFloat()
        val gy_rad = Math.toRadians(gy.toDouble()).toFloat()
        val gz_rad = Math.toRadians(gz.toDouble()).toFloat()

        // Rate of change of quaternion from gyro
        qDot1 = 0.5f * (-q1 * gx_rad - q2 * gy_rad - q3 * gz_rad)
        qDot2 = 0.5f * ( q0 * gx_rad + q2 * gz_rad - q3 * gy_rad)
        qDot3 = 0.5f * ( q0 * gy_rad - q1 * gz_rad + q3 * gx_rad)
        qDot4 = 0.5f * ( q0 * gz_rad + q1 * gy_rad - q2 * gx_rad)

        // Compute feedback only if accelerometer measurement valid
        if (!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {
            // Normalise accelerometer measurement
            recipNorm = 1.0f / sqrt(ax * ax + ay * ay + az * az)
            val axNorm = ax * recipNorm
            val ayNorm = ay * recipNorm
            val azNorm = az * recipNorm

            // Normalise magnetometer measurement
            if (!((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f))) {
                recipNorm = 1.0f / sqrt(mx * mx + my * my + mz * mz)
                val mxNorm = mx * recipNorm
                val myNorm = my * recipNorm
                val mzNorm = mz * recipNorm

                // Auxiliary variables to avoid repeated arithmetic
                _2q0mx = 2.0f * q0 * mxNorm
                _2q0my = 2.0f * q0 * myNorm
                _2q0mz = 2.0f * q0 * mzNorm
                _2q1mx = 2.0f * q1 * mxNorm
                val _2q0 = 2.0f * q0
                val _2q1 = 2.0f * q1
                val _2q2 = 2.0f * q2
                val _2q3 = 2.0f * q3
                val _4q0 = 4.0f * q0
                val _4q1 = 4.0f * q1
                val _4q2 = 4.0f * q2
                val _4q3 = 4.0f * q3
                val _8q1 = 8.0f * q1
                val _8q2 = 8.0f * q2

                // Reference direction of Earth's magnetic field
                hx = mxNorm * q0 * q0 - _2q0my * q3 + _2q0mz * q2 + mxNorm * q1 * q1 + _2q1 * myNorm * q2 + _2q1 * mzNorm * q3 - mxNorm * q2 * q2 - mxNorm * q3 * q3
                hy = _2q0mx * q3 + myNorm * q0 * q0 - _2q0mz * q1 + _2q1mx * q2 - myNorm * q1 * q1 + myNorm * q2 * q2 + _2q2 * mzNorm * q3 - myNorm * q3 * q3
                _2bx = sqrt(hx * hx + hy * hy)
                _2bz = -_2q0mx * q2 + _2q0my * q1 + mzNorm * q0 * q0 + _2q1mx * q3 - mzNorm * q1 * q1 + _2q2 * myNorm * q3 - mzNorm * q2 * q2 + mzNorm * q3 * q3
                _4bx = 2.0f * _2bx
                _4bz = 2.0f * _2bz
                _8bx = 2.0f * _4bx
                _8bz = 2.0f * _4bz

                // Gradient decent algorithm corrective step
                s0 = -_2q2 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q1 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - _4q0 * q2 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm) + (-_4bx * q2 + _4bz * q0) * (2.0f * q0 * q0 + 2.0f * q1 * q1 - 1.0f - _2bx + _2bz * q0) + (-_4bx * q1 + _4bz * q3) * (2.0f * q1 * q3 - _2q0 * q2 - _2bx) + (_4bx * q0 + _4bz * q2) * (2.0f * q2 * q3 + _2q0 * q1 - _2bz)
                s1 = _2q3 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q0 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - 4.0f * q1 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm) + (_4bx * q3 + _4bz * q1) * (2.0f * q0 * q0 + 2.0f * q1 * q1 - 1.0f - _2bx) + (-_4bx * q0 + _4bz * q2) * (2.0f * q1 * q3 - _2q0 * q2 - _2bx) + (_4bx * q1 + _4bz * q3) * (2.0f * q2 * q3 + _2q0 * q1 - _2bz)
                s2 = -_2q0 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q3 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - 4.0f * q2 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm) + (-_4bx * q1 + _4bz * q3) * (2.0f * q0 * q0 + 2.0f * q1 * q1 - 1.0f - _2bx) + (_4bx * q2 + _4bz * q0) * (2.0f * q1 * q3 - _2q0 * q2 - _2bx) + (-_4bx * q3 + _4bz * q1) * (2.0f * q2 * q3 + _2q0 * q1 - _2bz)
                s3 = _2q1 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q2 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) + (-_4bx * q0 + _4bz * q2) * (2.0f * q0 * q0 + 2.0f * q1 * q1 - 1.0f - _2bx) + (-_4bx * q2 + _4bz * q0) * (2.0f * q1 * q3 - _2q0 * q2 - _2bx) + (_4bx * q1 + _4bz * q3) * (2.0f * q2 * q3 + _2q0 * q1 - _2bz)
            } else {
                // Only accelerometer (no mag)
                // Simplified gradient descent
                s0 = -_2q2 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q1 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - _4q0 * q2 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm)
                s1 = _2q3 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q0 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - 4.0f * q1 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm)
                s2 = -_2q0 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q3 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm) - 4.0f * q2 * (1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2 - azNorm)
                s3 = _2q1 * (2.0f * q1 * q3 - _2q0 * q2 - axNorm) + _2q2 * (2.0f * q0 * q1 + _2q2 * q3 - ayNorm)
            }

            // Apply feedback step
            recipNorm = 1.0f / sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3)
            s0 *= recipNorm; s1 *= recipNorm; s2 *= recipNorm; s3 *= recipNorm

            qDot1 -= beta * s0
            qDot2 -= beta * s1
            qDot3 -= beta * s2
            qDot4 -= beta * s3
        }

        // Integrate rate of change of quaternion
        q0 += qDot1 * dt
        q1 += qDot2 * dt
        q2 += qDot3 * dt
        q3 += qDot4 * dt

        // Normalise quaternion
        recipNorm = 1.0f / sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        q0 *= recipNorm; q1 *= recipNorm; q2 *= recipNorm; q3 *= recipNorm
    }

    fun getEulerAngles(): FloatArray {
        val pitch = Math.toDegrees(atan2(2.0f * q1 * q3 - 2.0f * q0 * q2, 2.0f * q0 * q0 + 2.0f * q1 * q1 - 1.0f)).toFloat()
        val roll  = Math.toDegrees(atan2(2.0f * q0 * q1 + 2.0f * q2 * q3, 1.0f - 2.0f * q1 * q1 - 2.0f * q2 * q2)).toFloat()
        val yaw   = Math.toDegrees(atan2(2.0f * q0 * q3 + 2.0f * q1 * q2, 1.0f - 2.0f * q2 * q2 - 2.0f * q3 * q3)).toFloat()
        return floatArrayOf(pitch, roll, yaw)
    }

    fun reset() {
        q0 = 1.0f; q1 = 0.0f; q2 = 0.0f; q3 = 0.0f
    }

    private fun atan2(y: Float, x: Float): Double = Math.atan2(y.toDouble(), x.toDouble())
}