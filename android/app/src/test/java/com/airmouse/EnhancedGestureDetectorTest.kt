package com.airmouse

import android.content.Context
import android.os.Vibrator
import com.airmouse.sensors.EnhancedGestureDetector
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EnhancedGestureDetectorTest {

    private lateinit var detector: EnhancedGestureDetector
    private val mockPrefs: PreferencesManager = mock()
    private val mockVibrator: Vibrator = mock()
    private val mockContext: Context = mock()

    @Before
    fun setup() {
        
        whenever(mockPrefs.getClickThreshold()).thenReturn(5.0f)
        whenever(mockPrefs.getDoubleClickInterval()).thenReturn(400L)
        whenever(mockPrefs.getScrollThreshold()).thenReturn(8.0f)
        whenever(mockPrefs.getScrollDebounce()).thenReturn(2.0f)
        whenever(mockPrefs.getRightClickTilt()).thenReturn(45f)
        whenever(mockPrefs.getRightClickDuration()).thenReturn(500L)
        whenever(mockPrefs.isHapticEnabled()).thenReturn(false)

        detector = EnhancedGestureDetector(mockContext, mockPrefs, mockVibrator)
    }

    @Test
    fun `no motion results in NONE gesture`() {
        val gesture = detector.detect(0f, 0f, 0f)
        assertEquals(EnhancedGestureDetector.Gesture.NONE, gesture)
    }

    @Test
    fun `fast gyro Y tilt triggers CLICK`() {
        
        val gesture = detector.detect(10f, 0f, 0f)
        assertEquals(EnhancedGestureDetector.Gesture.CLICK, gesture)
    }

    @Test
    fun `high vertical accel triggers SCROLL_DOWN`() {
        
        val gesture = detector.detect(0f, 15f, 0f)
        assertEquals(EnhancedGestureDetector.Gesture.SCROLL_DOWN, gesture)
    }

    @Test
    fun `high negative vertical accel triggers SCROLL_UP`() {
        val gesture = detector.detect(0f, -15f, 0f)
        assertEquals(EnhancedGestureDetector.Gesture.SCROLL_UP, gesture)
    }
}