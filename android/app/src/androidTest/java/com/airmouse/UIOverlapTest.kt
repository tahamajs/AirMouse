package com.airmouse

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.home.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UIOverlapTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpAccessibility() {
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockHomeViewModel = mockk<HomeViewModel>(relaxed = true)
    private val mockNavigationActions = mockk<NavigationActions>(relaxed = true)

    private val homeUiStateFlow = MutableStateFlow(HomeViewModel.HomeUiState())
    private val approvalCountdownFlow = MutableStateFlow(0L)

    @Before
    fun setUp() {
        every { mockHomeViewModel.uiState } returns homeUiStateFlow
        every { mockHomeViewModel.approvalCountdownMs } returns approvalCountdownFlow
        every { mockHomeViewModel.getSessionDuration() } returns 0L
        every { mockHomeViewModel.hasRegisteredUser() } returns true

        homeUiStateFlow.value = HomeViewModel.HomeUiState(
            isCalibrated = true,
            userName = "Test User",
            connectionStatus = ConnectionStatus.CONNECTED,
            serverIp = "192.168.1.50",
            serverPort = 8080
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setContent() {
        val viewModelStore = ViewModelStore()
        viewModelStore.put(
            "androidx.lifecycle.ViewModelProvider.DefaultKey:com.airmouse.presentation.ui.home.HomeViewModel",
            mockHomeViewModel
        )

        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = viewModelStore
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                androidx.compose.material3.Scaffold(
                    floatingActionButton = {
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { mockHomeViewModel.disconnect() }
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Disconnect"
                            )
                        }
                    }
                ) { _ ->
                    HomeScreen(navigationActions = mockNavigationActions)
                }
            }
        }
    }

    @Test
    fun testFloatingButtonDoesNotOverlapModulesOrContent() {
        setContent()

        // 1. Verify Floating Action Button exists and is displayed
        val fabNode = composeTestRule.onNodeWithContentDescription("Disconnect")
        fabNode.assertExists()
        fabNode.assertIsDisplayed()

        // 2. Verify Modules header exists and is displayed
        val modulesHeaderNode = composeTestRule.onNodeWithText("Modules")
        modulesHeaderNode.assertExists()
        modulesHeaderNode.assertIsDisplayed()

        val fabBounds = fabNode.getUnclippedBoundsInRoot()
        val modulesBounds = modulesHeaderNode.getUnclippedBoundsInRoot()

        // Verify that FAB and Modules header bounds do not overlap
        val overlapsModules = boundsOverlap(fabBounds, modulesBounds)
        assertFalse(
            "Floating button must not overlap Modules header bounds (FAB: $fabBounds, Modules: $modulesBounds)",
            overlapsModules
        )

        // 3. Verify Touchpad module tile exists and does not overlap FAB
        val touchpadTile = composeTestRule.onNodeWithText("Pointer control")
        touchpadTile.assertExists()
        val tileBounds = touchpadTile.getUnclippedBoundsInRoot()
        val overlapsTile = boundsOverlap(fabBounds, tileBounds)
        assertFalse(
            "Floating button must not overlap Pointer control module tile (FAB: $fabBounds, Tile: $tileBounds)",
            overlapsTile
        )
    }

    @Test
    fun testNoDuplicateClickableBoundsExist() {
        setContent()

        // Fetch all semantics nodes that have a click action
        val clickableNodes = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue("Expected clickable nodes on HomeScreen", clickableNodes.isNotEmpty())

        val seenBounds = mutableMapOf<String, String>()
        val duplicates = mutableListOf<String>()

        for (node in clickableNodes) {
            val bounds = node.boundsInRoot
            if (bounds.width <= 0 || bounds.height <= 0) continue

            val boundsKey = "${bounds.left.toInt()},${bounds.top.toInt()},${bounds.right.toInt()},${bounds.bottom.toInt()}"
            val nodeDescription = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
                ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString()
                ?: "Node(id=${node.id})"

            if (seenBounds.containsKey(boundsKey)) {
                val previousNode = seenBounds[boundsKey]
                if (previousNode != nodeDescription) {
                    duplicates.add("Duplicate bounds $boundsKey between '$previousNode' and '$nodeDescription'")
                }
            } else {
                seenBounds[boundsKey] = nodeDescription
            }
        }

        assertTrue(
            "Found duplicate clickable bounds (ghost elements): ${duplicates.joinToString("; ")}",
            duplicates.isEmpty()
        )
    }

    private fun boundsOverlap(a: DpRect, b: DpRect): Boolean {
        return !(a.right <= b.left || a.left >= b.right || a.bottom <= b.top || a.top >= b.bottom)
    }
}
