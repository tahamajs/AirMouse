package com.airmouse.presentation.ui.touchpad

import android.content.Context
import com.airmouse.domain.usecase.TestConnectionUseCase
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TouchpadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockTestUseCase: TestConnectionUseCase
    private lateinit var viewModel: TouchpadViewModel

    private val connectionStatusFlow = MutableStateFlow(ConnectionManager.ConnectionStatus.DISCONNECTED)
    private val isMouseControlEnabledFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockConnectionManager = mockk(relaxed = true)
        mockTestUseCase = mockk(relaxed = true)

        // Mock ConnectionManager flows
        every { mockConnectionManager.connectionStatus } returns connectionStatusFlow
        every { mockConnectionManager.isMouseControlEnabledFlow } returns isMouseControlEnabledFlow
        every { mockConnectionManager.isConnected() } returns false

        // Default mock preference values
        every { mockPrefs.getString("theme", any()) } returns "system"
        every { mockPrefs.getLanguage() } returns "en"

        viewModel = TouchpadViewModel(
            mockContext,
            mockPrefs,
            mockConnectionManager,
            mockTestUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertFalse(state.isActive)
    }

    @Test
    fun testConnectionStateObservation() = runTest(testDispatcher) {
        // Initially disconnected
        assertFalse(viewModel.uiState.value.isConnected)

        // Change connection to CONNECTED
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.CONNECTED
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConnected)
    }

    @Test
    fun testToggleTouchpadWithoutConnection() {
        // If not connected, toggle should not activate touchpad
        every { mockConnectionManager.isConnected() } returns false
        
        viewModel.handleEvent(TouchpadEvent.ToggleTouchpad)
        
        assertFalse(viewModel.uiState.value.isActive)
        val currentEffect = viewModel.effect.value
        assertTrue(currentEffect is TouchpadEffect.ShowToast)
        assertEquals("Connect to the server first", (currentEffect as TouchpadEffect.ShowToast).message)
    }

    @Test
    fun testToggleTouchpadWithConnection() = runTest(testDispatcher) {
        every { mockConnectionManager.isConnected() } returns true
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.CONNECTED
        testScheduler.advanceUntilIdle()

        viewModel.handleEvent(TouchpadEvent.ToggleTouchpad)
        assertTrue(viewModel.uiState.value.isActive)
        verify { mockConnectionManager.isMouseControlEnabled = false }

        // Toggle again to deactivate
        viewModel.handleEvent(TouchpadEvent.ToggleTouchpad)
        assertFalse(viewModel.uiState.value.isActive)
        verify { mockConnectionManager.isMouseControlEnabled = true }
    }

    @Test
    fun testSettingsUpdates() {
        viewModel.handleEvent(TouchpadEvent.UpdateSensitivity(1.5f))
        assertEquals(1.5f, viewModel.uiState.value.sensitivity)
        verify { mockPrefs.putFloat("touchpad_sensitivity", 1.5f) }

        viewModel.handleEvent(TouchpadEvent.UpdateCursorSpeed(1.2f))
        assertEquals(1.2f, viewModel.uiState.value.cursorSpeed)
        verify { mockPrefs.putFloat("touchpad_cursor_speed", 1.2f) }
    }

    @Test
    fun testApplyPreset() {
        viewModel.handleEvent(TouchpadEvent.ApplyPreset(TouchpadMode.GAMING))
        
        verify { mockPrefs.putBoolean("gaming_mode", true) }
        verify { mockPrefs.putBoolean("use_websocket", false) }
        verify { mockConnectionManager.setProtocol(ConnectionManager.ConnectionProtocol.UDP) }
    }

    @Test
    fun testMouseControlToggleEvent() {
        viewModel.handleEvent(TouchpadEvent.ToggleMouseControl(true))
        verify { mockConnectionManager.isMouseControlEnabled = true }

        viewModel.handleEvent(TouchpadEvent.ToggleMouseControl(false))
        verify { mockConnectionManager.isMouseControlEnabled = false }
    }
}
