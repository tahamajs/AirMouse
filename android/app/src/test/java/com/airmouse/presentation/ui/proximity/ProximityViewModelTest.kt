package com.airmouse.presentation.ui.proximity

import android.content.Context
import com.airmouse.utils.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProximityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var viewModel: ProximityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)

        // Mock default preferences
        every { mockPrefs.getString("proximity_device_mac", "") } returns ""
        every { mockPrefs.getBoolean("proximity_enabled", false) } returns false
        every { mockPrefs.getFloat("proximity_near_threshold", 1.5f) } returns 1.5f
        every { mockPrefs.getFloat("proximity_far_threshold", 3.0f) } returns 3.0f

        viewModel = ProximityViewModel(mockContext, mockPrefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_serviceDisabledAndNoDeviceSelected() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isEnabled)
        assertTrue(state.deviceMac.isBlank())
    }

    @Test
    fun testToggleService_whenNoDeviceSelected_remainsDisabled() = runTest {
        advanceUntilIdle()
        viewModel.toggleService(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Service must remain disabled when no device is selected", state.isEnabled)
        assertTrue(state.status.contains("Select a trusted Bluetooth device"))
    }

    @Test
    fun testSelectDevice_updatesDeviceState() = runTest {
        advanceUntilIdle()
        val mac = "00:11:22:33:44:55"
        val name = "Target Gaming PC"

        viewModel.selectDevice(mac, name)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mac, state.deviceMac)
        assertEquals(name, state.connectedDevice)
        assertTrue(state.deviceMac.isNotBlank())
    }

    @Test
    fun testToggleService_afterDeviceSelected_enablesService() = runTest {
        advanceUntilIdle()
        val mac = "00:11:22:33:44:55"
        viewModel.selectDevice(mac, "My PC")
        advanceUntilIdle()

        viewModel.toggleService(true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Service should enable once a device is selected", state.isEnabled)
    }

    @Test
    fun testPairedDevices_canBePopulatedAndSelected() = runTest {
        advanceUntilIdle()
        val testDevices = listOf(
            PairedDeviceInfo("Workstation PC", "AA:BB:CC:DD:EE:01"),
            PairedDeviceInfo("Gaming Rig", "AA:BB:CC:DD:EE:02")
        )

        viewModel.setPairedDevices(testDevices)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.pairedDevices.size)
        assertEquals("Workstation PC", state.pairedDevices[0].name)

        viewModel.selectDevice(state.pairedDevices[1].address, state.pairedDevices[1].name)
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals("AA:BB:CC:DD:EE:02", updatedState.deviceMac)
        assertEquals("Gaming Rig", updatedState.connectedDevice)
    }

    @Test
    fun testEmptyPairedDevices_defaultsToEmpty() = runTest {
        advanceUntilIdle()
        viewModel.setPairedDevices(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.pairedDevices.isEmpty())
    }

    @Test
    fun testThresholds_updateCorrectly() = runTest {
        advanceUntilIdle()
        viewModel.updateNearThreshold(2.2f)
        viewModel.updateFarThreshold(4.5f)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2.2f, state.nearThreshold, 0.001f)
        assertEquals(4.5f, state.farThreshold, 0.001f)
    }
}
