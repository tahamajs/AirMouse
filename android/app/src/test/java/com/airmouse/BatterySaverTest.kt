package com.airmouse

import android.hardware.SensorManager
import com.airmouse.sensors.SensorService
import com.airmouse.utils.BatterySaver
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BatterySaverTest {

    private lateinit var batterySaver: BatterySaver
    private val mockSensorService: SensorService = mock()

    @Before
    fun setup() {
        batterySaver = BatterySaver()
        batterySaver.start(mockSensorService)
    }

    @Test
    fun `initial state is not low power`() {
        assertFalse(batterySaver.isLowPowerMode())
    }

    @Test
    fun `movement triggers normal sampling rate`() {
        // Even if not in low power, calling onMovement should ensure SENSOR_DELAY_GAME
        batterySaver.onMovement()
        verify(mockSensorService, atLeastOnce()).setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
        assertFalse(batterySaver.isLowPowerMode())
    }

    @Test
    fun `updateMovement with large delta triggers onMovement`() {
        batterySaver.updateMovement(0f, 0f)
        batterySaver.updateMovement(0.5f, 0.5f) // Significant change
        
        verify(mockSensorService, atLeastOnce()).setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
    }
}