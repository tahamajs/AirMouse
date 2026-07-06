package com.airmouse

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.airmouse.domain.usecase.TestConnectionUseCase
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.touchpad.TouchpadScreen
import com.airmouse.presentation.ui.touchpad.TouchpadUiState
import com.airmouse.presentation.ui.touchpad.TouchpadViewModel
import com.airmouse.utils.PreferencesManager
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TouchpadScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockContext = mockk<Context>(relaxed = true)
    private val mockPrefs = mockk<PreferencesManager>(relaxed = true)
    private val mockConnectionManager = mockk<ConnectionManager>(relaxed = true)
    private val mockTestUseCase = mockk<TestConnectionUseCase>(relaxed = true)
    private val mockNavigationActions = mockk<NavigationActions>(relaxed = true)

    private val uiStateFlow = MutableStateFlow(TouchpadUiState())
    private val effectFlow = MutableStateFlow<com.airmouse.presentation.ui.touchpad.TouchpadEffect?>(null)

    private lateinit var mockViewModel: TouchpadViewModel

    @Before
    fun setUp() {
        mockViewModel = mockk(relaxed = true)

        every { mockViewModel.uiState } returns uiStateFlow
        every { mockViewModel.effect } returns effectFlow

        // Default mock preference values
        every { mockPrefs.getString("theme", any()) } returns "system"
        every { mockPrefs.getLanguage() } returns "en"
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testMouseControlEnabled_showsActiveText() {
        uiStateFlow.value = TouchpadUiState(
            isConnected = true,
            isActive = true,
            isMouseControlEnabled = true
        )

        composeTestRule.setContent {
            TouchpadScreen(
                navigationActions = mockNavigationActions,
                viewModel = mockViewModel
            )
        }

        // Verify that the text says "Active and sending moves"
        composeTestRule.onNodeWithText("Active and sending moves").assertExists()
    }

    @Test
    fun testMouseControlDisabled_showsPausedText() {
        uiStateFlow.value = TouchpadUiState(
            isConnected = true,
            isActive = true,
            isMouseControlEnabled = false
        )

        composeTestRule.setContent {
            TouchpadScreen(
                navigationActions = mockNavigationActions,
                viewModel = mockViewModel
            )
        }

        // Verify that the text says "Paused"
        composeTestRule.onNodeWithText("Paused").assertExists()
    }
}
