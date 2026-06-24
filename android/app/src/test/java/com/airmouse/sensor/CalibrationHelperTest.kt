
package com.airmouse.sensor

import android.content.Context
import com.airmouse.PreferencesManager
import com.airmouse.sensors.CalibrationHelper
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalibrationHelperTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var calibrationHelper: CalibrationHelper
    private val testInstructions = mutableListOf<String>()

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        
        
        every { mockPrefs.getFloat(any(), any()) } returns 0f
        every { mockPrefs.putFloat(any(), any()) } just runs
        every { mockPrefs.getBoolean(any(), any()) } returns false
        every { mockPrefs.putBoolean(any(), any()) } just runs
        every { mockPrefs.getGyroBias() } returns floatArrayOf(0f, 0f, 0f)
        every { mockPrefs.saveGyroBias(any()) } just runs
        
        calibrationHelper = CalibrationHelper(mockContext, mockPrefs)
        testInstructions.clear()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testLoadCalibrationStatus() {
        every { mockPrefs.getBoolean("calibration_complete", false) } returns true
        
        val result = calibrationHelper.loadCalibrationStatus()
        
        assertTrue("Should return true when calibrated", result)
    }

    @Test
    fun testLoadCalibrationStatusFalse() {
        every { mockPrefs.getBoolean("calibration_complete", false) } returns false
        
        val result = calibrationHelper.loadCalibrationStatus()
        
        assertFalse("Should return false when not calibrated", result)
    }

    @Test
    fun testGetGyroOffsets() {
        every { mockPrefs.getFloat("gyro_offset_x", 0f) } returns 0.1f
        every { mockPrefs.getFloat("gyro_offset_y", 0f) } returns -0.05f
        every { mockPrefs.getFloat("gyro_offset_z", 0f) } returns 0.02f
        
        val offsets = calibrationHelper.loadGyroOffsets()
        
        assertEquals("X offset should match", 0.1f, offsets.first, 0.001f)
        assertEquals("Y offset should match", -0.05f, offsets.second, 0.001f)
        assertEquals("Z offset should match", 0.02f, offsets.third, 0.001f)
    }

    @Test
    fun testSetGyroOffsets() {
        calibrationHelper.setGyroOffsets(0.2f, -0.1f, 0.05f)
        
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_x", 0.2f) }
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_y", -0.1f) }
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_z", 0.05f) }
    }

    @Test
    fun testResetCalibration() {
        calibrationHelper.resetCalibration()
        
        verify(exactly = 1) { mockPrefs.putBoolean("calibration_complete", false) }
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_x", 0f) }
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_y", 0f) }
        verify(exactly = 1) { mockPrefs.putFloat("gyro_offset_z", 0f) }
    }

    @Test
    fun testCorrectGyro() {
        calibrationHelper.setGyroOffsets(0.1f, 0.2f, 0.3f)
        
        val correctedX = calibrationHelper.correctGyro(5.0f, 0)
        val correctedY = calibrationHelper.correctGyro(5.0f, 1)
        val correctedZ = calibrationHelper.correctGyro(5.0f, 2)
        
        assertEquals("X should be corrected", 4.9f, correctedX, 0.001f)
        assertEquals("Y should be corrected", 4.8f, correctedY, 0.001f)
        assertEquals("Z should be corrected", 4.7f, correctedZ, 0.001f)
    }

    @Test
    fun testApplyCalibration() {
        calibrationHelper.setGyroOffsets(0.1f, 0.2f, 0.3f)
        
        val (x, y, z) = calibrationHelper.applyCalibration(5.0f, 5.0f, 5.0f)
        
        assertEquals(4.9f, x, 0.001f)
        assertEquals(4.8f, y, 0.001f)
        assertEquals(4.7f, z, 0.001f)
    }

    @Test
    fun testIsDeviceCalibrated() {
        every { mockPrefs.getBoolean("calibration_complete", false) } returns true
        
        assertTrue("Should report calibrated", calibrationHelper.isDeviceCalibrated())
    }
}
