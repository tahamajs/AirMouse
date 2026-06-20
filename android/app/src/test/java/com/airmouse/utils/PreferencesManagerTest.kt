package com.airmouse.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.SensorCalibrationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        prefs = PreferencesManager(context)
    }

    @Test
    fun `sensitivity and thresholds are clamped to safe ranges`() {
        prefs.setSensitivity(10f)
        prefs.setClickThreshold(0f)
        prefs.setScrollThreshold(99f)
        prefs.setAccelerationFactor(0f)
        prefs.setFontSize(100f)

        assertEquals(2.0f, prefs.getSensitivity(), 0.0001f)
        assertEquals(3.0f, prefs.getClickThreshold(), 0.0001f)
        assertEquals(20f, prefs.getScrollThreshold(), 0.0001f)
        assertEquals(1.0f, prefs.getAccelerationFactor(), 0.0001f)
        assertEquals(24f, prefs.getFontSize(), 0.0001f)
    }

    @Test
    fun `theme and connection settings persist`() {
        prefs.setTheme("dark")
        prefs.setLastIp("192.168.1.50")
        prefs.setLastPort(8081)
        prefs.setAutoConnect(false)
        prefs.setUdpDiscoveryEnabled(false)

        assertEquals("dark", prefs.getTheme())
        assertEquals("192.168.1.50", prefs.getLastIp())
        assertEquals(8081, prefs.getLastPort())
        assertFalse(prefs.isAutoConnect())
        assertFalse(prefs.isUdpDiscoveryEnabled())
    }

    @Test
    fun `calibration data round trips through storage`() {
        val calibrationData = CalibrationData(
            gyroBias = SensorCalibrationData(0.1f, 0.2f, 0.3f),
            accelOffset = SensorCalibrationData(1.0f, 2.0f, 3.0f),
            magOffset = SensorCalibrationData(4.0f, 5.0f, 6.0f),
            isCalibrated = true,
            quality = CalibrationQuality.GOOD,
            timestamp = 123456789L
        )

        prefs.saveCalibrationData(calibrationData)
        val restored = prefs.getCalibrationData()

        assertTrue(restored.isCalibrated)
        assertEquals(CalibrationQuality.GOOD, restored.quality)
        assertEquals(0.1f, restored.gyroBias.offsetX, 0.0001f)
        assertEquals(2.0f, restored.accelOffset.offsetY, 0.0001f)
        assertEquals(6.0f, restored.magOffset.offsetZ, 0.0001f)
        assertEquals(123456789L, restored.timestamp)
    }
}
