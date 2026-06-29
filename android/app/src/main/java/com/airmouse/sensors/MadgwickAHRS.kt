package com.airmouse.sensors

import kotlin.math.*

class MadgwickAHRS(
    private var beta: Float = 0.1f,  
    private var zeta: Float = 0.0f   
) {

    
    private val quaternion = FloatArray(4).apply {
        this[0] = 1f  
        this[1] = 0f  
        this[2] = 0f  
        this[3] = 0f  
    }

    
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private var needsRecalc = true

    companion object {
        private const val EPSILON = 1e-6f
    }

    constructor() : this(0.1f, 0.0f)

    fun updateGyro(gx: Float, gy: Float, gz: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        
        val qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz)
        val qDot2 = 0.5f * ( q0 * gx + q2 * gz - q3 * gy)
        val qDot3 = 0.5f * ( q0 * gy - q1 * gz + q3 * gx)
        val qDot4 = 0.5f * ( q0 * gz + q1 * gy - q2 * gx)

        
        quaternion[0] += qDot1 * dt
        quaternion[1] += qDot2 * dt
        quaternion[2] += qDot3 * dt
        quaternion[3] += qDot4 * dt

        normalizeQuaternion()
        needsRecalc = true
    }

    fun updateAccel(ax: Float, ay: Float, az: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        
        val norm = sqrt(ax * ax + ay * ay + az * az)
        if (norm < EPSILON) return
        val axN = ax / norm
        val ayN = ay / norm
        val azN = az / norm

        
        val vx = 2f * (q1 * q3 - q0 * q2)
        val vy = 2f * (q0 * q1 + q2 * q3)
        val vz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3

        
        val ex = (ayN * vz - azN * vy)
        val ey = (azN * vx - axN * vz)
        val ez = (axN * vy - ayN * vx)

        
        val step = dt.coerceAtLeast(0.001f)
        val wx = -2f * beta * ex
        val wy = -2f * beta * ey
        val wz = -2f * beta * ez

        val q0Dot = 0.5f * (-q1 * wx - q2 * wy - q3 * wz)
        val q1Dot = 0.5f * ( q0 * wx + q2 * wz - q3 * wy)
        val q2Dot = 0.5f * ( q0 * wy - q1 * wz + q3 * wx)
        val q3Dot = 0.5f * ( q0 * wz + q1 * wy - q2 * wx)

        quaternion[0] += q0Dot * step
        quaternion[1] += q1Dot * step
        quaternion[2] += q2Dot * step
        quaternion[3] += q3Dot * step

        normalizeQuaternion()
        needsRecalc = true
    }

    fun updateMag(mx: Float, my: Float, mz: Float, dt: Float) {
        if (dt <= 0f) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        
        val norm = sqrt(mx * mx + my * my + mz * mz)
        if (norm < EPSILON) return
        val mxN = mx / norm
        val myN = my / norm
        val mzN = mz / norm

        
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

        
        val wx = 2f * (bx * (0.5f - q2 * q2 - q3 * q3) +
                bz * (q1 * q3 - q0 * q2))
        val wy = 2f * (bx * (q1 * q2 - q0 * q3) +
                bz * (q0 * q1 + q2 * q3))
        val wz = 2f * (bx * (q0 * q2 + q1 * q3) +
                bz * (0.5f - q1 * q1 - q2 * q2))

        
        val ex = (myN * wz - mzN * wy)
        val ey = (mzN * wx - mxN * wz)
        val ez = (mxN * wy - myN * wx)

        
        val step = dt.coerceAtLeast(0.001f)
        val correctionFactor = if (zeta > 0) zeta else beta

        val cx = -correctionFactor * ex
        val cy = -correctionFactor * ey
        val cz = -correctionFactor * ez

        val q0Dot = 0.5f * (-q1 * cx - q2 * cy - q3 * cz)
        val q1Dot = 0.5f * ( q0 * cx + q2 * cz - q3 * cy)
        val q2Dot = 0.5f * ( q0 * cy - q1 * cz + q3 * cx)
        val q3Dot = 0.5f * ( q0 * cz + q1 * cy - q2 * cx)

        quaternion[0] += q0Dot * step
        quaternion[1] += q1Dot * step
        quaternion[2] += q2Dot * step
        quaternion[3] += q3Dot * step

        normalizeQuaternion()
        needsRecalc = true
    }

    fun update(
        gx: Float, gy: Float, gz: Float,
        ax: Float, ay: Float, az: Float,
        mx: Float, my: Float, mz: Float,
        dt: Float
    ) {
        if (dt <= 0f) return

        
        updateGyro(gx, gy, gz, dt)

        
        updateAccel(ax, ay, az, dt)

        
        updateMag(mx, my, mz, dt)

        
        if (zeta > 0) {
            applyGyroDriftCorrection(gx, gy, gz, dt)
        }
    }

    fun updateImu(gx: Float, gy: Float, gz: Float, ax: Float, ay: Float, az: Float, dt: Float) {
        if (dt <= 0f) return
        updateGyro(gx, gy, gz, dt)
        updateAccel(ax, ay, az, dt)
        needsRecalc = true
    }

    private fun applyGyroDriftCorrection(gx: Float, gy: Float, gz: Float, dt: Float) {
        
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

    fun getRollDegrees(): Float {
        recalcEulerIfNeeded()
        return roll
    }

    fun getPitchDegrees(): Float {
        recalcEulerIfNeeded()
        return pitch
    }

    fun getYawDegrees(): Float {
        recalcEulerIfNeeded()
        return yaw
    }

    fun getRoll(): Float = getRollDegrees() * (PI.toFloat() / 180f)

    fun getPitch(): Float = getPitchDegrees() * (PI.toFloat() / 180f)

    fun getYaw(): Float = getYawDegrees() * (PI.toFloat() / 180f)

    private fun recalcEulerIfNeeded() {
        if (!needsRecalc) return

        val q0 = quaternion[0]
        val q1 = quaternion[1]
        val q2 = quaternion[2]
        val q3 = quaternion[3]

        
        val sinr_cosp = 2f * (q0 * q1 + q2 * q3)
        val cosr_cosp = 1f - 2f * (q1 * q1 + q2 * q2)
        roll = atan2(sinr_cosp, cosr_cosp) * (180f / PI.toFloat())

        
        val sinp = 2f * (q0 * q2 - q3 * q1)
        pitch = when {
            abs(sinp) >= 1f -> sign(sinp) * 90f
            else -> asin(sinp) * (180f / PI.toFloat())
        }

        
        val siny_cosp = 2f * (q0 * q3 + q1 * q2)
        val cosy_cosp = 1f - 2f * (q2 * q2 + q3 * q3)
        yaw = atan2(siny_cosp, cosy_cosp) * (180f / PI.toFloat())

        needsRecalc = false
    }

    fun getQuaternion(): FloatArray = quaternion.copyOf()

    fun setBeta(beta: Float) {
        this.beta = beta.coerceIn(0.01f, 1.0f)
    }

    fun setZeta(zeta: Float) {
        this.zeta = zeta.coerceIn(0f, 0.1f)
    }

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

    fun resetWithSensors(ax: Float, ay: Float, az: Float, mx: Float, my: Float, mz: Float) {
        
        val normA = sqrt(ax * ax + ay * ay + az * az)
        if (normA < EPSILON) return

        val axN = ax / normA
        val ayN = ay / normA
        val azN = az / normA

        
        val normM = sqrt(mx * mx + my * my + mz * mz)
        if (normM < EPSILON) return

        val mxN = mx / normM
        val myN = my / normM
        val mzN = mz / normM

        
        val initialRoll = atan2(ayN, azN)
        val initialPitch = atan2(-axN, sqrt(ayN * ayN + azN * azN))

        
        val cosRoll = cos(initialRoll)
        val sinRoll = sin(initialRoll)
        val cosPitch = cos(initialPitch)
        val sinPitch = sin(initialPitch)

        val mxH = mxN * cosPitch + mzN * sinPitch
        val myH = mxN * sinRoll * sinPitch + myN * cosRoll - mzN * sinRoll * cosPitch
        val initialYaw = atan2(-myH, mxH)

        
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