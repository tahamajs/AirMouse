package com.airmouse.sensor

import com.airmouse.domain.model.SensorCalibrationStatus
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.data.repository.SensorRepositoryImpl
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SensorRepositoryTest {

    private lateinit var mockSensorManager: android.hardware.SensorManager
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockCalibrationHelper: CalibrationHelper
    private lateinit var repository: SensorRepositoryImpl

    @Before
    fun setUp() {
        mockSensorManager = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockCalibrationHelper = mockk(relaxed = true)

        every { mockPrefs.isCalibrated() } returns false
        every { mockPrefs.putBoolean(any(), any()) } just runs
        every { mockCalibrationHelper.setGyroOffsets(any(), any(), any()) } just runs

        repository = SensorRepositoryImpl(mockSensorManager, mockPrefs, mockCalibrationHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testInitialStatusAndData() = runTest {
        val initialData = repository.getCurrentSensorData()
        assertEquals(0f, initialData.gyroX, 0.001f)
        assertEquals(0f, initialData.accelX, 0.001f)
        assertEquals(SensorCalibrationStatus.NOT_CALIBRATED, repository.getCalibrationStatus())
    }

    @Test
    fun testManualCalibrationSuccess() = runTest {
        coEvery { mockCalibrationHelper.calibrateGyroscope() } returns true

        val success = repository.calibrateSensors()
        assertTrue(success)
        assertEquals(SensorCalibrationStatus.CALIBRATED, repository.getCalibrationStatus())
        verify { mockPrefs.putBoolean("calibration_complete", true) }
    }

    @Test
    fun testZuptStationaryAutoCalibrationTrigger() = runTest {
        // Feed samples with low variance (< 0.02) over >500ms
        val baseTime = 1000L
        var triggered = false

        // 12 samples over 600ms (50ms interval) with near zero acceleration and non-zero gyro drift
        for (i in 0..12) {
            val t = baseTime + (i * 50L)
            // Constant accel (variance = 0) and small gyro bias
            val result = repository.processZupt(
                accelX = 0.01f, accelY = 9.81f, accelZ = 0.02f,
                gyroX = 0.05f, gyroY = -0.03f, gyroZ = 0.01f,
                nowMs = t
            )
            if (result) {
                triggered = true
            }
        }

        assertTrue("ZUPT should have triggered auto-calibration when stationary > 500ms", triggered)
        assertEquals(SensorCalibrationStatus.CALIBRATED, repository.getCalibrationStatus())
        verify { mockCalibrationHelper.setGyroOffsets(any(), any(), any()) }
        verify { mockPrefs.putBoolean("calibration_complete", true) }
    }

    @Test
    fun testZuptDoesNotTriggerWhenMoving() = runTest {
        val baseTime = 1000L
        var triggered = false

        // High acceleration variance
        for (i in 0..15) {
            val t = baseTime + (i * 50L)
            val dynamicAccel = if (i % 2 == 0) 5.0f else -5.0f
            val result = repository.processZupt(
                accelX = dynamicAccel, accelY = 9.81f, accelZ = 0.0f,
                gyroX = 0.1f, gyroY = 0.1f, gyroZ = 0.1f,
                nowMs = t
            )
            if (result) {
                triggered = true
            }
        }

        assertFalse("ZUPT should not trigger auto-calibration during movement", triggered)
        assertNotEquals(SensorCalibrationStatus.CALIBRATED, repository.getCalibrationStatus())
    }
}
