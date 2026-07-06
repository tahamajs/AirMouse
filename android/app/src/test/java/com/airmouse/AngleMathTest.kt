package com.airmouse

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Validates the core phase-wrapping mathematics used in SensorService and MainActivity.
 * Ensures that motion detection handles the 180-degree wrap correctly without generating massive cursor jumps.
 */
class AngleMathTest {

    private fun getShortestDelta(currentAngle: Float, lastAngle: Float): Float {
        var delta = (currentAngle - lastAngle) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }

    @Test
    fun testNormalRotation() {
        // User turns phone slightly right
        val delta = getShortestDelta(10f, 5f)
        assertEquals(5f, delta, 0.001f)
    }

    @Test
    fun testPhaseWrapPositiveToNegative() {
        // User turns phone past the 180 degree mark (e.g., from 179 to -179)
        // A naive subtraction (-179 - 179) gives -358. The shortest delta is actually +2.
        val delta = getShortestDelta(-179f, 179f)
        assertEquals(2f, delta, 0.001f)
    }

    @Test
    fun testPhaseWrapNegativeToPositive() {
        // User turns phone past the -180 degree mark (e.g., from -179 to 179)
        // A naive subtraction (179 - -179) gives +358. The shortest delta is actually -2.
        val delta = getShortestDelta(179f, -179f)
        assertEquals(-2f, delta, 0.001f)
    }

    @Test
    fun testFullRotation() {
        // Rotating a full 360 degrees should result in 0 net change.
        val delta = getShortestDelta(360f, 0f)
        assertEquals(0f, delta, 0.001f)
    }
}
