package com.airmouse

import com.airmouse.utils.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class MathUtilsTest {

    @Test
    fun `radToDeg converts correctly`() {
        assertEquals(0f, MathUtils.radToDeg(0f), 0.01f)
        assertEquals(180f, MathUtils.radToDeg(Math.PI.toFloat()), 0.01f)
        assertEquals(90f, MathUtils.radToDeg(Math.PI.toFloat() / 2f), 0.01f)
    }

    @Test
    fun `clamp restricts values correctly`() {
        assertEquals(5f, MathUtils.clamp(5f, 0f, 10f), 0.01f)
        assertEquals(0f, MathUtils.clamp(-5f, 0f, 10f), 0.01f)
        assertEquals(10f, MathUtils.clamp(15f, 0f, 10f), 0.01f)
    }
}