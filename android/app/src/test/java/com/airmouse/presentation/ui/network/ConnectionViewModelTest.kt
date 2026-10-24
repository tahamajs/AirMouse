package com.airmouse.presentation.ui.network

import android.content.Context
import com.airmouse.network.ConnectionManager
import com.airmouse.notifications.NotificationManager
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
class ConnectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var viewModel: NetworkDiscoveryViewModel

    private val connectionStatusFlow = MutableStateFlow(ConnectionManager.ConnectionStatus.DISCONNECTED)
    private val currentIpFlow = MutableStateFlow<String?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockConnectionManager = mockk(relaxed = true)
        mockNotificationManager = mockk(relaxed = true)

        every { mockConnectionManager.connectionStatus } returns connectionStatusFlow
        every { mockConnectionManager.currentIp } returns currentIpFlow
        every { mockPrefs.getString(any(), any()) } returns "[]"

        viewModel = NetworkDiscoveryViewModel(
            mockContext,
            mockPrefs,
            mockConnectionManager,
            mockNotificationManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertFalse(state.isConnecting)
        assertNull(state.errorMessage)
    }

    @Test
    fun testConnectionStatusTransitions() = runTest(testDispatcher) {
        // Transition to CONNECTING
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.CONNECTING
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isConnecting)
        assertEquals(50, viewModel.uiState.value.connectionProgress)

        // Transition to CONNECTED
        currentIpFlow.value = "192.168.1.100"
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.CONNECTED
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isConnecting)
        assertEquals(100, viewModel.uiState.value.connectionProgress)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testConnectionFailureTransition() = runTest(testDispatcher) {
        // Transition to ERROR
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.ERROR
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isConnecting)
        assertEquals(0, viewModel.uiState.value.connectionProgress)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testClearError() = runTest(testDispatcher) {
        connectionStatusFlow.value = ConnectionManager.ConnectionStatus.ERROR
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
