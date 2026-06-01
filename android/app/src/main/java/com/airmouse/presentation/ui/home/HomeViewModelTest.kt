package com.airmouse.presentation.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.repository.IGestureRepository
import com.airmouse.domain.repository.ISettingsRepository
import com.airmouse.domain.usecase.SendMovementUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var connectionRepo: IConnectionRepository
    private lateinit var calibrationRepo: ICalibrationRepository
    private lateinit var gestureRepo: IGestureRepository
    private lateinit var settingsRepo: ISettingsRepository
    private lateinit var sendMovementUseCase: SendMovementUseCase
    private lateinit var viewModel: HomeViewModel

    private val connectionStatusFlow = MutableStateFlow(ConnectionStatus.DISCONNECTED)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        connectionRepo = mockk()
        calibrationRepo = mockk()
        gestureRepo = mockk()
        settingsRepo = mockk()
        sendMovementUseCase = mockk(relaxed = true)

        every { connectionRepo.connectionStatus() } returns connectionStatusFlow
        every { settingsRepo.getPreferences() } returns flowOf(UserPreferences())
        coEvery { calibrationRepo.getGyroBias() } returns GyroBias(0f,0f,0f)
        coEvery { calibrationRepo.getAccelCalibration() } returns AccelCalibration()
        coEvery { calibrationRepo.getMagCalibration() } returns MagCalibration()
        coEvery { connectionRepo.getLastConfig() } returns null

        viewModel = HomeViewModel(connectionRepo, calibrationRepo, gestureRepo, settingsRepo, sendMovementUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun connect_callsRepository() = runTest {
        viewModel.updateIp("192.168.1.10")
        viewModel.updatePort(8080)
        viewModel.connect()
        coVerify { connectionRepo.connect(any()) }
    }

    @Test
    fun connectionStatus_updatesState() = runTest {
        connectionStatusFlow.value = ConnectionStatus.CONNECTING
        assertEquals(ConnectionStatus.CONNECTING, viewModel.uiState.value.connectionStatus)
    }
}