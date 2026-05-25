package com.airmouse.sensors

import kotlin.math.*

class MadgwickAHRS(private val beta: Float = 0.1f) {
    private var q0 = 1.0f
    private var q1 = 0.0f
    private var q2 = 0.0f
    private var q3 = 0.0f

    private var ax = 0f; private var ay = 0f; private var az = 0f
    private var mx = 0f; private var my = 0f; private var mz = 0f

    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        var recipNorm: Float
        var s0: Float; var s1: Float; var s2: Float; var s3: Float
        var qDot1: Float; var qDot2: Float; var qDot3: Float; var qDot4: Float

        qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz)
        qDot2 = 0.5f * ( q0 * gx + q2 * gz - q3 * gy)
        qDot3 = 0.5f * ( q0 * gy - q1 * gz + q3 * gx)
        qDot4 = 0.5f * ( q0 * gz + q1 * gy - q2 * gx)

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
            s3 = 0f

            qDot1 -= beta * s0
            qDot2 -= beta * s1
            qDot3 -= beta * s2
            qDot4 -= beta * s3
        }

        q0 += qDot1 * dt
        q1 += qDot2 * dt
        q2 += qDot3 * dt
        q3 += qDot4 * dt

        recipNorm = 1f / sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3)
        q0 *= recipNorm
        q1 *= recipNorm
        q2 *= recipNorm
        q3 *= recipNorm
    }

    fun updateAccel(x: Float, y: Float, z: Float) {
        ax = x; ay = y; az = z
    }

    fun updateMag(x: Float, y: Float, z: Float) {
        mx = x; my = y; mz = z
    }

    fun getRoll(): Float = atan2(2f * (q0 * q1 + q2 * q3), 1f - 2f * (q1 * q1 + q2 * q2))
    fun getPitch(): Float = asin(2f * (q0 * q2 - q3 * q1))
    fun getYaw(): Float = atan2(2f * (q0 * q3 + q1 * q2), 1f - 2f * (q2 * q2 + q3 * q3))
}