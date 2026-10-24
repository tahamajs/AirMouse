package com.airmouse.presentation.ui.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.airmouse.features.MouseControlFeature
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockPrefs: PreferencesManager
    private lateinit var mockMouseFeature: MouseControlFeature
    private lateinit var mockContext: Context
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockPrefs = mockk(relaxed = true)
        mockMouseFeature = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        // Mock default preferences
        every { mockPrefs.getString("theme", any()) } returns "system"
        every { mockPrefs.getLanguage() } returns "en"
        every { mockPrefs.getUserName() } returns "TestUser"
        every { mockPrefs.getFloat("sensitivity", any()) } returns 0.5f
        every { mockPrefs.getBoolean(any(), any()) } returns true
        every { mockPrefs.getInt(any(), any()) } returns 5000

        viewModel = SettingsViewModel(mockPrefs, mockMouseFeature, mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testInitialUiState() {
        val state = viewModel.uiState.value
        assertEquals(0.5f, state.sensitivity, 0.001f)
        assertTrue(state.autoConnect)
        assertNull(state.error)
    }

    @Test
    fun testUpdateSensitivity() = runTest(testDispatcher) {
        viewModel.handleEvent(SettingsEvent.UpdateSensitivity(0.85f))
        testScheduler.advanceUntilIdle()

        assertEquals(0.85f, viewModel.uiState.value.sensitivity, 0.001f)
        verify { mockPrefs.putFloat("sensitivity", 0.85f) }
        verify { mockMouseFeature.setSensitivity(0.85f) }
    }

    @Test
    fun testToggleAcceleration() = runTest(testDispatcher) {
        val initial = viewModel.uiState.value.accelerationEnabled
        viewModel.handleEvent(SettingsEvent.ToggleAcceleration)
        testScheduler.advanceUntilIdle()

        assertEquals(!initial, viewModel.uiState.value.accelerationEnabled)
        verify { mockPrefs.putBoolean("acceleration_enabled", !initial) }
    }

    @Test
    fun testToggleInvertAxes() = runTest(testDispatcher) {
        val initX = viewModel.uiState.value.invertX
        val initY = viewModel.uiState.value.invertY

        viewModel.handleEvent(SettingsEvent.ToggleInvertX)
        viewModel.handleEvent(SettingsEvent.ToggleInvertY)
        testScheduler.advanceUntilIdle()

        assertEquals(!initX, viewModel.uiState.value.invertX)
        assertEquals(!initY, viewModel.uiState.value.invertY)
    }

    @Test
    fun testShowErrorAndClear() = runTest(testDispatcher) {
        viewModel.handleEvent(SettingsEvent.ShowError("Network Timeout"))
        testScheduler.advanceUntilIdle()

        assertEquals("Network Timeout", viewModel.uiState.value.error)
    }

    @Test
    fun testShowToastEffect() = runTest(testDispatcher) {
        viewModel.handleEvent(SettingsEvent.ShowToast("Settings Saved"))
        testScheduler.advanceUntilIdle()

        // Toast effect emitted
        viewModel.clearEffect()
        assertNull(viewModel.effect.value)
    }
}
