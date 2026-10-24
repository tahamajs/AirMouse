package com.airmouse.sensor

import com.airmouse.sensors.OneEuroFilter
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class OneEuroFilterTest {

    @Test
    fun testInitializationReturnsFirstValue() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)
        val initial = filter.filter(15.0f, timestampMs = 1000L)
        assertEquals(15.0f, initial, 0.0001f)
    }

    @Test
    fun testJitterReductionOnStationarySignal() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)
        var t = 1000L
        filter.filter(10.0f, t)

        // Inject high-frequency noise around 10.0 with small steps (e.g., 9.8, 10.2, 9.9, 10.1)
        var maxJitter = 0.0f
        for (i in 1..20) {
            t += 16L // ~60fps
            val noise = if (i % 2 == 0) 0.3f else -0.3f
            val filtered = filter.filter(10.0f + noise, t)
            val deviation = abs(filtered - 10.0f)
            if (deviation > maxJitter) {
                maxJitter = deviation
            }
        }

        // Filtered signal should significantly dampen the ±0.3 noise
        assertTrue("Filtered jitter ($maxJitter) should be significantly lower than raw noise 0.3", maxJitter < 0.2f)
    }

    @Test
    fun testFastTrackingWithBetaBoost() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.1, dCutoff = 1.0)
        var t = 1000L
        filter.filter(0.0f, t)

        // Fast movement transition from 0 to 100
        var lastFiltered = 0.0f
        for (i in 1..10) {
            t += 16L
            lastFiltered = filter.filter(100.0f, t)
        }

        // Higher beta should aggressively adapt cutoff to track the fast step change
        assertTrue("Fast moving signal should quickly converge, got $lastFiltered", lastFiltered > 85.0f)
    }

    @Test
    fun testResetClearsState() {
        val filter = OneEuroFilter()
        filter.filter(50.0f, 1000L)
        filter.reset()
        val afterReset = filter.filter(100.0f, 2000L)
        assertEquals(100.0f, afterReset, 0.0001f)
    }

    @Test
    fun testNanoPrecisionAndZeroDivisionGuards() {
        val filter = OneEuroFilter(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)
        // Test nanosecond filtering
        val initial = filter.filterNano(10.0f, 1_000_000_000L)
        assertEquals(10.0f, initial, 0.0001f)

        // Successive call with identical or backward timestamp should not produce NaN or Infinity
        val sameTime = filter.filterNano(12.0f, 1_000_000_000L)
        assertFalse("Output must not be NaN", sameTime.isNaN())
        assertFalse("Output must not be Infinite", sameTime.isInfinite())

        // Pass NaN or Infinity inputs: should handle gracefully
        val nanHandled = filter.filter(Float.NaN, 1_000_000_016L)
        assertFalse("Output must recover from NaN", nanHandled.isNaN())
    }
}
