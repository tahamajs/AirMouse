package com.airmouse.presentation.ui.home

import android.content.Context
import android.hardware.SensorManager
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.files.FileTransferService
import com.airmouse.files.TransferState
import com.airmouse.network.ConnectionManager
import com.airmouse.notifications.NotificationManager
import com.airmouse.presentation.PresentationModeService
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockConnectionManager: ConnectionManager
    private lateinit var mockPresentationModeService: PresentationModeService
    private lateinit var mockFileTransferService: FileTransferService
    private lateinit var mockCalibrationUseCase: CalibrationUseCase
    private lateinit var mockNotificationManager: NotificationManager
    private lateinit var mockSensorManager: SensorManager

    private lateinit var viewModel: HomeViewModel

    private val connectionStatusFlow = MutableStateFlow(ConnectionManager.ConnectionStatus.DISCONNECTED)
    private val isMouseControlEnabledFlow = MutableStateFlow(false)
    private val connectionQualityFlow = MutableStateFlow(com.airmouse.network.ConnectionQuality())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockPrefs = mockk(relaxed = true)
        mockConnectionManager = mockk(relaxed = true)
        mockPresentationModeService = mockk(relaxed = true)
        mockFileTransferService = mockk(relaxed = true)
        mockCalibrationUseCase = mockk(relaxed = true)
        mockNotificationManager = mockk(relaxed = true)
        mockSensorManager = mockk(relaxed = true)

        // Mock context system services
        every { mockContext.getSystemService(Context.SENSOR_SERVICE) } returns mockSensorManager
        val mockVibrator = mockk<android.os.Vibrator>(relaxed = true)
        every { mockContext.getSystemService(Context.VIBRATOR_SERVICE) } returns mockVibrator

        // Mock preferences default values
        every { mockPrefs.getControlMode() } returns "motion"
        every { mockPrefs.getBoolean(any(), any()) } returns true
        every { mockPrefs.getSensitivity() } returns 1.0f
        every { mockPrefs.getLanguage() } returns "en"

        // Mock connection manager flows
        every { mockConnectionManager.connectionStatus } returns connectionStatusFlow
        every { mockConnectionManager.isMouseControlEnabledFlow } returns isMouseControlEnabledFlow
        every { mockConnectionManager.connectionQuality } returns connectionQualityFlow

        // Mock presentation and transfer state flows
        every { mockPresentationModeService.state } returns MutableStateFlow(PresentationModeService.PresentationState())
        every { mockFileTransferService.state } returns MutableStateFlow(TransferState())

        // Create ViewModel
        viewModel = HomeViewModel(
            mockContext,
            mockPrefs,
            mockConnectionManager,
            mockPresentationModeService,
            mockFileTransferService,
            mockCalibrationUseCase,
            mockNotificationManager
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
        assertEquals("motion", state.controlMode)
        assertTrue(state.isAutoConnectEnabled)
    }

    @Test
    fun testToggleAirMouse() {
        assertFalse(viewModel.uiState.value.isActive)
        viewModel.toggleAirMouse()
        assertTrue(viewModel.uiState.value.isActive)
        viewModel.toggleAirMouse()
        assertFalse(viewModel.uiState.value.isActive)
    }

    @Test
    fun testUpdateIpAndPort() {
        viewModel.updateIp("192.168.1.50")
        assertEquals("192.168.1.50", viewModel.uiState.value.serverIp)

        viewModel.updatePort(9090)
        assertEquals(9090, viewModel.uiState.value.serverPort)
    }

    @Test
    fun testToggleAutoConnect() {
        assertTrue(viewModel.uiState.value.isAutoConnectEnabled)
        viewModel.toggleAutoConnect()
        assertFalse(viewModel.uiState.value.isAutoConnectEnabled)
        verify { mockPrefs.setAutoConnect(false) }
    }

    @Test
    fun testSetMouseControlEnabled() {
        viewModel.setMouseControlEnabled(true)
        verify { mockConnectionManager.isMouseControlEnabled = true }

        viewModel.setMouseControlEnabled(false)
        verify { mockConnectionManager.isMouseControlEnabled = false }
    }

    @Test
    fun testAddLogMessage() {
        assertTrue(viewModel.uiState.value.logMessages.isEmpty())
        viewModel.addLogMessage("Test message")
        assertEquals(1, viewModel.uiState.value.logMessages.size)
        assertEquals("Test message", viewModel.uiState.value.logMessages.first())
    }

    @Test
    fun testToggleAiSmoothingAndPredictive() {
        assertFalse(viewModel.uiState.value.aiSmoothingEnabled)
        viewModel.toggleAiSmoothing()
        assertTrue(viewModel.uiState.value.aiSmoothingEnabled)
        verify { mockPrefs.putBoolean("ai_smoothing", true) }

        assertFalse(viewModel.uiState.value.predictiveEnabled)
        viewModel.togglePredictive()
        assertTrue(viewModel.uiState.value.predictiveEnabled)
        verify { mockPrefs.putBoolean("predictive_movement", true) }
    }
}
