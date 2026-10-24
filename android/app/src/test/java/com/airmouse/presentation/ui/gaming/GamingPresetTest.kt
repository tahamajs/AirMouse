package com.airmouse.presentation.ui.gaming

import com.airmouse.domain.usecase.TestConnectionUseCase
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.ui.touchpad.TouchpadEvent
import com.airmouse.presentation.ui.touchpad.TouchpadMode
import com.airmouse.presentation.ui.touchpad.TouchpadViewModel
import com.airmouse.utils.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamingPresetTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var gamingViewModel: GamingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        gamingViewModel = GamingViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultState_hasStandardValuesAndProfiles() {
        val state = gamingViewModel.uiState.value
        assertEquals("WebSocket", state.transmissionProtocol)
        assertEquals("Standard (60 Hz)", state.pollingRate)
        assertTrue(state.gameProfiles.any { it.name == "FPS Shooter" })
        assertTrue(state.gameProfiles.any { it.name == "MOBA" })
    }

    @Test
    fun testApplyGamingPreset_updatesProtocolAndPollingRate() {
        gamingViewModel.applyGamingPreset()
        val state = gamingViewModel.uiState.value
        assertEquals("UDP", state.transmissionProtocol)
        assertEquals("Maximum (120 Hz)", state.pollingRate)
        assertTrue(state.isGamingModeApplied)
        assertEquals(1.5f, state.sensitivity, 0.001f)
        assertEquals("Disabled", state.defaultGestures)
    }

    @Test
    fun testTouchpadViewModel_gamingPresetUpdatesProtocolAndPollingRate() {
        val mockContext = mockk<android.content.Context>(relaxed = true)
        val mockPrefs = mockk<PreferencesManager>(relaxed = true)
        val mockConnectionManager = mockk<ConnectionManager>(relaxed = true)
        val mockTestUseCase = mockk<TestConnectionUseCase>(relaxed = true)

        every { mockConnectionManager.connectionStatus } returns MutableStateFlow(ConnectionManager.ConnectionStatus.DISCONNECTED)
        every { mockConnectionManager.isMouseControlEnabledFlow } returns MutableStateFlow(false)
        every { mockConnectionManager.isConnected() } returns false

        val touchpadViewModel = TouchpadViewModel(
            mockContext,
            mockPrefs,
            mockConnectionManager,
            mockTestUseCase
        )

        touchpadViewModel.handleEvent(TouchpadEvent.ApplyPreset(TouchpadMode.GAMING))

        val state = touchpadViewModel.uiState.value
        assertEquals("UDP", state.transmissionProtocol)
        assertEquals("Maximum (120 Hz)", state.pollingRate)
    }
}
