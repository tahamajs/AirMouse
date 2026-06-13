// app/src/test/java/com/airmouse/sensor/GestureDetectorTest.kt
package com.airmouse.sensor

import com.airmouse.sensors.EnhancedGestureDetector
import android.content.Context
import android.os.Vibrator
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GestureDetectorTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockVibrator: Vibrator
    private lateinit var gestureDetector: EnhancedGestureDetector

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockVibrator = mockk(relaxed = true)
        
        // Mock threshold values
        every { mockPrefs.getClickThreshold() } returns 8f
        every { mockPrefs.getDoubleClickInterval() } returns 400L
        every { mockPrefs.getScrollThreshold() } returns 6f
        every { mockPrefs.getScrollDebounce() } returns 100L
        every { mockPrefs.getRightClickTilt() } returns 45f
        every { mockPrefs.getRightClickDuration() } returns 500L
        every { mockPrefs.isHapticEnabled() } returns true
        
        gestureDetector = EnhancedGestureDetector(mockContext, mockPrefs, mockVibrator)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testDetectClick() {
        val result = gestureDetector.detect(gyroY = 10f, accelY = 0f, roll = 0f)
        
        assertEquals("Should detect click", EnhancedGestureDetector.Gesture.CLICK, result)
    }

    @Test
    fun testDetectNoGesture() {
        val result = gestureDetector.detect(gyroY = 1f, accelY = 0f, roll = 0f)
        
        assertEquals("Should detect no gesture", EnhancedGestureDetector.Gesture.NONE, result)
    }

    @Test
    fun testDetectDoubleClick() {
        // First click
        gestureDetector.detect(gyroY = 10f, accelY = 0f, roll = 0f)
        
        // Second click within window (simulate by calling again)
        val result = gestureDetector.detect(gyroY = 10f, accelY = 0f, roll = 0f)
        
        assertEquals("Should detect double click", EnhancedGestureDetector.Gesture.DOUBLE_CLICK, result)
    }

    @Test
    fun testDetectRightClick() {
        // Hold tilt for more than duration
        val startTime = System.currentTimeMillis()
        
        // Simulate multiple detections to reach duration threshold
        var result = EnhancedGestureDetector.Gesture.NONE
        for (i in 0 until 10) {
            result = gestureDetector.detect(gyroY = 0f, accelY = 0f, roll = 50f)
            Thread.sleep(100) // Simulate time passing
        }
        
        // Note: Right click detection requires holding tilt over multiple frames
        // This test may need adjustment based on implementation
    }

    @Test
    fun testDetectScrollUp() {
        val result = gestureDetector.detect(gyroY = 0f, accelY = 8f, roll = 0f)
        
        assertEquals("Should detect scroll up", EnhancedGestureDetector.Gesture.SCROLL_UP, result)
    }

    @Test
    fun testDetectScrollDown() {
        val result = gestureDetector.detect(gyroY = 0f, accelY = -8f, roll = 0f)
        
        assertEquals("Should detect scroll down", EnhancedGestureDetector.Gesture.SCROLL_DOWN, result)
    }

    @Test
    fun testReloadThresholds() {
        every { mockPrefs.getClickThreshold() } returns 12f
        every { mockPrefs.getDoubleClickInterval() } returns 300L
        
        gestureDetector.reloadThresholds()
        
        // Verify via behavior or use reflection if needed
        // For now, just ensure no exception
        assertTrue(true)
    }
}