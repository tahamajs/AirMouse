package com.airmouse.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.EnhancedGestureDetector
import com.airmouse.sensors.SensorService
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class SensorServiceTest {

    private lateinit var mockContext: Context
    private lateinit var mockSensorManager: SensorManager
    private lateinit var mockCalibrationHelper: CalibrationHelper
    private lateinit var mockGestureDetector: EnhancedGestureDetector
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var sensorService: SensorService

    private val mockGyro = mockk<Sensor>(relaxed = true)
    private val mockAccel = mockk<Sensor>(relaxed = true)
    private val mockMag = mockk<Sensor>(relaxed = true)
    private val mockRotVec = mockk<Sensor>(relaxed = true)

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockSensorManager = mockk(relaxed = true)
        mockCalibrationHelper = mockk(relaxed = true)
        mockGestureDetector = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.SENSOR_SERVICE) } returns mockSensorManager
        every { mockSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mockGyro
        every { mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns mockAccel
        every { mockSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) } returns mockMag
        every { mockSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) } returns mockRotVec

        every { mockPrefs.getBoolean(any(), any()) } returns false

        sensorService = SensorService(
            mockContext,
            mockCalibrationHelper,
            mockGestureDetector,
            mockPrefs
        )
    }

    @After
    fun tearDown() {
        sensorService.stop()
        unmockkAll()
    }

    @Test
    fun testInitialization() {
        verify { mockSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) }
        verify { mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
        verify { mockSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }
        verify { mockSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
        
        assertFalse("Should not be running initially", sensorService.isStable())
    }

    @Test
    fun testStartService() {
        sensorService.start()
        
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockGyro, any(Int::class), any(android.os.Handler::class)) }
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockAccel, any(Int::class), any(android.os.Handler::class)) }
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockMag, any(Int::class), any(android.os.Handler::class)) }
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockRotVec, any(Int::class), any(android.os.Handler::class)) }
    }

    @Test
    fun testStopService() {
        sensorService.start()
        sensorService.stop()
        
        verify { mockSensorManager.unregisterListener(any(SensorEventListener::class)) }
    }

    @Test
    fun testSetSamplingRate() {
        sensorService.start()
        val customDelay = SensorManager.SENSOR_DELAY_FASTEST
        sensorService.setSamplingRate(customDelay)
        
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockGyro, customDelay, any(android.os.Handler::class)) }
        verify { mockSensorManager.registerListener(any(SensorEventListener::class), mockAccel, customDelay, any(android.os.Handler::class)) }
    }

    @Test
    fun testStabilityCallback() {
        var stableNotification: Boolean? = null
        sensorService.onStabilityChanged = { isStable ->
            stableNotification = isStable
        }

        
        setPrivateField(sensorService, "pitch", 1.0f)
        setPrivateField(sensorService, "yaw", 1.0f)
        setPrivateField(sensorService, "lastPitch", 1.0f)
        setPrivateField(sensorService, "lastYaw", 1.0f)
        setPrivateField(sensorService, "isStable", false)
        
        
        val checkStabilityMethod = sensorService.javaClass.getDeclaredMethod("checkStability")
        checkStabilityMethod.isAccessible = true
        
        
        for (i in 0 until 50) {
            checkStabilityMethod.invoke(sensorService)
        }
        
        assertTrue("Device should become stable", sensorService.isStable())
        assertEquals("Callback should be invoked with true", true, stableNotification)
    }

    private fun setPrivateField(obj: Any, fieldName: String, value: Any) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
