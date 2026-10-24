package com.airmouse.sensors

import kotlin.math.PI

/**
 * 1€ Filter (One Euro Filter)
 * An adaptive first-order low-pass filter designed for noisy signals with low latency.
 * Reference: Casiez, G., Roussel, N. and Vogel, D. (2012).
 * 1€ Filter: A Simple Speed-based Low-pass Filter for Noisy Input in HCI.
 *
 * @param minCutoff Minimum cutoff frequency in Hz (fc_min). Lower values reduce jitter when moving slowly.
 * @param beta Speed coefficient (beta). Higher values decrease lag during rapid movement.
 * @param dCutoff Cutoff frequency for derivative calculation in Hz.
 */
class OneEuroFilter(
    private var minCutoff: Double = 1.0,
    private var beta: Double = 0.007,
    private var dCutoff: Double = 1.0
) {
    private var xFilter = LowPassFilter()
    private var dxFilter = LowPassFilter()
    private var lastTime: Long = -1L

    private class LowPassFilter {
        private var lastHatX: Double = 0.0
        private var initialized: Boolean = false

        fun filter(x: Double, alpha: Double): Double {
            lastHatX = if (initialized) {
                alpha * x + (1.0 - alpha) * lastHatX
            } else {
                initialized = true
                x
            }
            return lastHatX
        }

        fun last(): Double = lastHatX

        fun hasLast(): Boolean = initialized

        fun reset() {
            initialized = false
            lastHatX = 0.0
        }
    }

    private fun alpha(rate: Double, cutoff: Double): Double {
        val safeCutoff = if (cutoff <= 0.0 || cutoff.isNaN() || cutoff.isInfinite()) 1e-4 else cutoff
        val safeRate = if (rate <= 0.0 || rate.isNaN() || rate.isInfinite()) 60.0 else rate
        val tau = 1.0 / (2.0 * PI * safeCutoff)
        val te = 1.0 / safeRate
        return 1.0 / (1.0 + tau / te)
    }

    /**
     * Filters the input value [x] with the associated timestamp in milliseconds [timestampMs].
     */
    fun filter(x: Double, timestampMs: Long = System.currentTimeMillis()): Double {
        if (lastTime < 0L) {
            lastTime = timestampMs
            dxFilter.reset()
            return xFilter.filter(x, 1.0)
        }

        val dt = (timestampMs - lastTime) / 1000.0
        lastTime = timestampMs

        // Guard against zero, negative or unusually high dt
        val rate = if (dt > 1e-5) 1.0 / dt else 60.0

        // Estimate derivative (dx/dt)
        val dx = if (xFilter.hasLast() && dt > 1e-5) {
            (x - xFilter.last()) / dt
        } else {
            0.0
        }

        val edx = dxFilter.filter(dx, alpha(rate, dCutoff))
        val cutoff = minCutoff + beta * kotlin.math.abs(edx)
        return xFilter.filter(x, alpha(rate, cutoff))
    }

    fun filter(x: Float, timestampMs: Long = System.currentTimeMillis()): Float {
        return filter(x.toDouble(), timestampMs).toFloat()
    }

    fun reset() {
        lastTime = -1L
        xFilter.reset()
        dxFilter.reset()
    }

    fun setMinCutoff(cutoff: Double) {
        minCutoff = cutoff
    }

    fun setBeta(b: Double) {
        beta = b
    }

    fun setDerivativeCutoff(cutoff: Double) {
        dCutoff = cutoff
    }
}
