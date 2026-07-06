package com.airmouse

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.home.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockHomeViewModel = mockk<HomeViewModel>(relaxed = true)
    private val mockNavigationActions = mockk<NavigationActions>(relaxed = true)

    private val homeUiStateFlow = MutableStateFlow(HomeViewModel.HomeUiState())
    private val approvalCountdownFlow = MutableStateFlow(0L)

    @Before
    fun setUp() {
        // Setup default mocks for HomeViewModel
        every { mockHomeViewModel.uiState } returns homeUiStateFlow
        every { mockHomeViewModel.approvalCountdownMs } returns approvalCountdownFlow
        every { mockHomeViewModel.getSessionDuration() } returns 0L
        every { mockHomeViewModel.hasRegisteredUser() } returns true

        // Ensure user is calibrated and registered so Touchpad tile is clickable
        homeUiStateFlow.value = HomeViewModel.HomeUiState(
            isCalibrated = true,
            userName = "Test User"
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testNavigationFromHomeToTouchpad() {
        val viewModelStore = ViewModelStore()
        // Inject mock HomeViewModel into ViewModelStore using standard DefaultKey format
        viewModelStore.put(
            "androidx.lifecycle.ViewModelProvider.DefaultKey:com.airmouse.presentation.ui.home.HomeViewModel",
            mockHomeViewModel
        )

        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = viewModelStore
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                HomeScreen(navigationActions = mockNavigationActions)
            }
        }

        // Verify that the "Modules" section and "Touchpad" tile exists
        composeTestRule.onNodeWithText("Modules").assertExists()
        composeTestRule.onNodeWithText("Pointer control").assertExists()

        // Click on the Touchpad tile ("Pointer control")
        composeTestRule.onNodeWithText("Pointer control").performClick()

        // Verify that navigateTo was called with Touchpad route
        verify(exactly = 1) { 
            mockNavigationActions.navigateTo(Destinations.Touchpad.route) 
        }
    }
}
