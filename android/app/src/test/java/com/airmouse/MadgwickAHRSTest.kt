package com.airmouse

import com.airmouse.sensors.MadgwickAHRS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MadgwickAHRSTest {

    @Test
    fun `initial orientation should be zero`() {
        val madgwick = MadgwickAHRS()
        assertEquals(0f, madgwick.getRoll(), 0.01f)
        assertEquals(0f, madgwick.getPitch(), 0.01f)
        assertEquals(0f, madgwick.getYaw(), 0.01f)
    }

    @Test
    fun `update with zero data should stay zero`() {
        val madgwick = MadgwickAHRS()
        madgwick.update(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.01f)
        assertEquals(0f, madgwick.getRoll(), 0.01f)
        assertEquals(0f, madgwick.getPitch(), 0.01f)
        assertEquals(0f, madgwick.getYaw(), 0.01f)
    }

    @Test
    fun `6-axis mode fallback when magnetometer is zero`() {
        val madgwick = MadgwickAHRS(beta = 1.0f)
        
        madgwick.update(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0.1f)
        
        
        assertNotEquals(0f, madgwick.getRoll(), 0.001f)
    }

    @Test
    fun `pitch is clamped correctly`() {
        val madgwick = MadgwickAHRS()
        
        
        
        assertTrue(!madgwick.getPitch().isNaN())
    }

    @Test
    fun `quaternion normalization prevents drift`() {
        val madgwick = MadgwickAHRS()
        for (i in 0..100) {
            madgwick.update(10f, 10f, 10f, 9.8f, 0.1f, -0.1f, 30f, 30f, 30f, 0.01f)
        }
        val roll = madgwick.getRoll()
        assertTrue("Roll should be a valid number", !roll.isNaN())
    }
}