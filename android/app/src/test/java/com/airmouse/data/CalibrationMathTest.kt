package com.airmouse.data

import com.airmouse.presentation.ui.calibration.CalibrationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationMathTest {

    @Test
    fun testCalculateAxisScale_normalEarthGravity() {
        // When positive is +9.80665 and negative is -9.80665
        // span = abs(9.80665 - (-9.80665)) / 2 = 9.80665
        // scale = 9.80665 / 9.80665 = 1.0
        val scale = CalibrationViewModel.calculateAxisScale(9.80665f, -9.80665f)
        assertEquals(1.0f, scale, 0.001f)
        assertTrue(scale in 0.85f..1.15f)
    }

    @Test
    fun testCalculateAxisScale_nearZeroSpanReturnsOneClamped() {
        // When span is very small or zero (e.g. positive = negative = 0)
        // span <= 1.0f -> returns 1.0f
        val scale = CalibrationViewModel.calculateAxisScale(0f, 0f)
        assertEquals(1.0f, scale, 0.001f)
        assertTrue(scale in 0.85f..1.15f)
    }

    @Test
    fun testCalculateAxisScale_clampsToMaximum115() {
        // If measured span is small e.g. positive = 4.0, negative = -4.0 -> span = 4.0
        // scale = 9.80665 / 4.0 = ~2.45
        // should be clamped strictly to 1.15f
        val scale = CalibrationViewModel.calculateAxisScale(4.0f, -4.0f)
        assertEquals(1.15f, scale, 0.0001f)
        assertTrue(scale in 0.85f..1.15f)
    }

    @Test
    fun testCalculateAxisScale_clampsToMinimum085() {
        // If measured span is large e.g. positive = 15.0, negative = -15.0 -> span = 15.0
        // scale = 9.80665 / 15.0 = ~0.653
        // should be clamped strictly to 0.85f
        val scale = CalibrationViewModel.calculateAxisScale(15.0f, -15.0f)
        assertEquals(0.85f, scale, 0.0001f)
        assertTrue(scale in 0.85f..1.15f)
    }

    @Test
    fun testCalculateAxisScale_extremeValuesNeverProduceCrazyNumbers() {
        // Crazy extreme values like positive = 0.001f, negative = -0.001f
        val scaleTiny = CalibrationViewModel.calculateAxisScale(0.001f, -0.001f)
        assertTrue("Tiny span must remain clamped to 0.85..1.15", scaleTiny in 0.85f..1.15f)

        // Extreme large positive/negative values
        val scaleHuge = CalibrationViewModel.calculateAxisScale(1000f, -1000f)
        assertTrue("Huge span must remain clamped to 0.85..1.15", scaleHuge in 0.85f..1.15f)
    }

    @Test
    fun testCalculateAxisScale_slightSensorVariance() {
        // Sensor reading slightly high: +10.1 m/s^2, -9.9 m/s^2 -> span = 10.0
        // scale = 9.80665 / 10.0 = 0.980665
        val scale = CalibrationViewModel.calculateAxisScale(10.1f, -9.9f)
        assertEquals(0.980665f, scale, 0.001f)
        assertTrue(scale in 0.85f..1.15f)
    }
}
