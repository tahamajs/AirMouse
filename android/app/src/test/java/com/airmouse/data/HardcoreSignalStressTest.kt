package com.airmouse.data

import com.airmouse.sensors.OneEuroFilter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Hardcore Stress & Chaos Test Suite for Signal Processing and OneEuroFilter.
 * Verifies micro-tremor jitter suppression, ballistic dynamic cutoff adaptation,
 * monotonic direction reversal without ringing, and extreme numerical boundary resilience.
 */
class HardcoreSignalStressTest {

    private lateinit var filter: OneEuroFilter

    @Before
    fun setUp() {
        filter = OneEuroFilter(minCutoff = 1.0, beta = 0.05, dCutoff = 1.0)
    }

    /**
     * 1. Resting Jitter Suppression:
     * 500 micro-tremor samples (jitter < 0.005) must produce smoothed delta below deadband.
     */
    @Test
    fun testRestingJitterSuppression_producesSmoothedDeltaBelowDeadband() {
        val baseline = 15.0
        val deadband = 0.005
        var timestampMs = 10000L

        // Initialize at baseline
        var prevSmoothed = filter.filter(baseline, timestampMs)
        assertEquals(baseline, prevSmoothed, 0.0001)

        var maxDeltaFromBaseline = 0.0
        var maxConsecutiveStepDelta = 0.0

        // Inject 500 micro-tremor samples with amplitude strictly < 0.005
        // Using deterministic pseudo-random sequence to guarantee zero test flakiness
        for (i in 1..500) {
            timestampMs += 16L // ~60Hz sampling interval (16ms)
            // Deterministic high-frequency pseudo-random tremor in range [-0.0048, +0.0048]
            val pseudoNoise = (((i * 1103515245 + 12345) and 0x7FFFFFFF) % 960 - 480) / 100000.0
            assertTrue("Raw jitter must be strictly < 0.005", abs(pseudoNoise) < 0.005)

            val rawInput = baseline + pseudoNoise
            val smoothed = filter.filter(rawInput, timestampMs)

            val deltaFromBase = abs(smoothed - baseline)
            val stepDelta = abs(smoothed - prevSmoothed)

            if (deltaFromBase > maxDeltaFromBaseline) {
                maxDeltaFromBaseline = deltaFromBase
            }
            if (stepDelta > maxConsecutiveStepDelta) {
                maxConsecutiveStepDelta = stepDelta
            }

            // Smoothed step delta must always remain below the deadband
            assertTrue(
                "Step delta ($stepDelta) at step $i exceeded deadband ($deadband)",
                stepDelta < deadband
            )
            prevSmoothed = smoothed
        }

        // Verify total smoothed deviation remains comfortably below the deadband threshold
        assertTrue(
            "Max deviation from baseline ($maxDeltaFromBaseline) must be below deadband ($deadband)",
            maxDeltaFromBaseline < deadband
        )
    }

    /**
     * 2. Ballistic Motion Response:
     * Rapid flick (velocity 20.0 rad/s) must exhibit minimal phase lag (cutoff dynamic adaptation).
     */
    @Test
    fun testBallisticMotionResponse_dynamicCutoffMinimizesPhaseLag() {
        val adaptiveFilter = OneEuroFilter(minCutoff = 1.0, beta = 0.1, dCutoff = 1.0)
        val staticFilter = OneEuroFilter(minCutoff = 1.0, beta = 0.0, dCutoff = 1.0) // beta=0 disables adaptation

        var timestampMs = 5000L
        val flickVelocity = 20.0 // 20.0 rad/s rapid flick
        val dtSec = 0.016 // 16ms
        var rawPosition = 0.0

        // Settle at zero
        adaptiveFilter.filter(0.0, timestampMs)
        staticFilter.filter(0.0, timestampMs)

        var adaptiveLagSum = 0.0
        var staticLagSum = 0.0
        var lastAdaptiveOut = 0.0

        // Execute rapid ballistic flick over 15 consecutive frames (240ms)
        for (step in 1..15) {
            timestampMs += 16L
            rawPosition += flickVelocity * dtSec

            val adaptiveOut = adaptiveFilter.filter(rawPosition, timestampMs)
            val staticOut = staticFilter.filter(rawPosition, timestampMs)

            val adaptiveLag = rawPosition - adaptiveOut
            val staticLag = rawPosition - staticOut

            // After initial 3 adaptation frames, compare tracking lags
            if (step > 3) {
                adaptiveLagSum += adaptiveLag
                staticLagSum += staticLag
                // Dynamic cutoff adaptation must keep adaptive lag significantly lower than static lag
                assertTrue(
                    "Dynamic filter lag ($adaptiveLag) should be strictly less than static filter lag ($staticLag) at step $step",
                    adaptiveLag < staticLag
                )
            }
            lastAdaptiveOut = adaptiveOut
        }

        // Total phase lag of the adaptive filter must be at least 40% lower than static filter
        assertTrue(
            "Adaptive lag sum ($adaptiveLagSum) must be substantially smaller than static ($staticLagSum)",
            adaptiveLagSum < staticLagSum * 0.60
        )

        // Convergence velocity check: adaptive filter output must track close to raw position
        val finalTrackingRatio = lastAdaptiveOut / rawPosition
        assertTrue(
            "Adaptive filter must achieve > 80% of ballistic trajectory, reached $finalTrackingRatio",
            finalTrackingRatio > 0.80
        )
    }

    /**
     * 3. Direction Reversal:
     * Instantaneous direction change (+10 to -10) without overshoot oscillation.
     */
    @Test
    fun testInstantaneousDirectionReversal_noOvershootOscillation() {
        var timestampMs = 2000L

        // Settle signal firmly at +10.0
        for (i in 1..30) {
            timestampMs += 16L
            filter.filter(10.0, timestampMs)
        }

        // Instantaneous reversal to -10.0
        val outputs = ArrayList<Double>()
        for (i in 1..50) {
            timestampMs += 16L
            val out = filter.filter(-10.0, timestampMs)
            outputs.add(out)
        }

        // Strict monotonicity check: output must strictly decrease from +10.0 towards -10.0
        for (i in 0 until outputs.size - 1) {
            val curr = outputs[i]
            val next = outputs[i + 1]

            // Value must not increase during transition to -10.0
            assertTrue(
                "Oscillation detected: curr ($curr) < next ($next) at index $i",
                next <= curr + 1e-9
            )

            // Value must never overshoot beyond target bounds [-10.0, 10.0]
            assertTrue("Overshot upper bound: $curr > 10.0", curr <= 10.0 + 1e-6)
            assertTrue("Overshot lower bound: $curr < -10.0", curr >= -10.0 - 1e-6)
        }

        // Output must reach within 1% of the reversal target (-10.0) without oscillation
        val finalValue = outputs.last()
        assertEquals("Signal must settle at -10.0 without steady-state offset", -10.0, finalValue, 0.05)
    }

    /**
     * 4. Numerical Stability:
     * Feed extreme values (1e6, -1e6, 0.0, NaN, Inf) without NaN or crash.
     */
    @Test
    fun testNumericalStability_extremeValuesAndSpecialFloats() {
        var timestampMs = 1000L

        val extremeValues = doubleArrayOf(
            1e6, -1e6, 0.0,
            1e12, -1e12, 1e-12, -1e-12,
            Double.MAX_VALUE, Double.MIN_VALUE,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
            0.0, 1e6, -1e6
        )

        for (v in extremeValues) {
            timestampMs += 16L
            val result = filter.filter(v, timestampMs)

            // Must never be NaN or Infinite
            assertFalse("Filter returned NaN for input $v", result.isNaN())
            assertFalse("Filter returned Infinite for input $v", result.isInfinite())
        }

        // Verify recovery: after being bombarded with extremes, normal inputs must process accurately
        for (i in 1..20) {
            timestampMs += 16L
            filter.filter(42.0, timestampMs)
        }
        val recovered = filter.filter(42.0, timestampMs + 16L)
        assertEquals("Filter failed to recover cleanly after extreme numerical inputs", 42.0, recovered, 0.5)
    }

    /**
     * 5. Timestamp Chaos:
     * Backward, zero, and huge timestamp deltas must be handled gracefully without division by zero.
     */
    @Test
    fun testTimestampChaos_nonMonotonicAndExtremeDts() {
        val t0 = 10000L
        val initial = filter.filter(5.0f, t0)
        assertEquals(5.0f, initial, 0.001f)

        // Identical timestamp (dt = 0ms)
        val sameTime = filter.filter(6.0f, t0)
        assertFalse(sameTime.isNaN())
        assertFalse(sameTime.isInfinite())

        // Backward timestamp (clock skew / packet reordering)
        val backwardTime = filter.filter(7.0f, t0 - 500L)
        assertFalse(backwardTime.isNaN())
        assertFalse(backwardTime.isInfinite())

        // Massive timestamp jump (1000 seconds)
        val jumpTime = filter.filter(8.0f, t0 + 1_000_000L)
        assertFalse(jumpTime.isNaN())
        assertFalse(jumpTime.isInfinite())
        assertEquals(8.0f, jumpTime, 0.01f)
    }
}
