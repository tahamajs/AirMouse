package com.airmouse.sensor

import org.junit.Assert.*
import org.junit.Test

class MadgwickFilterTest {

    @Test
    fun testInitialOrientation() {
        val filter = MadgwickFilter()
        val quat = filter.getQuaternion()
        assertEquals(1.0f, quat[0], 0.001f) // w
        assertEquals(0.0f, quat[1], 0.001f) // x
        assertEquals(0.0f, quat[2], 0.001f) // y
        assertEquals(0.0f, quat[3], 0.001f) // z
    }

    @Test
    fun testGyroOnlyIntegration() {
        val filter = MadgwickFilter()
        val gyro = floatArrayOf(0.1f, 0f, 0f) // 0.1 rad/s around X
        val dt = 0.02f
        for (i in 0 until 50) {
            filter.update(gyro, null, null, dt)
        }
        val euler = filter.getEuler()
        // Expect roll ~0.1 * 50*0.02 = 0.1 rad ≈ 5.73 deg
        assertTrue(euler[0] > 5.0f && euler[0] < 6.5f)
    }
}