package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a bottom navigation item.
 *
 * @param title Display title
 * @param icon Icon when unselected
 * @param selectedIcon Icon when selected (falls back to [icon] if null)
 * @param destination Navigation destination
 * @param badgeCount Optional badge count (shown as a badge)
 * @param enabled Whether the item is tappable
 */
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
    val destination: Destinations,
    val badgeCount: Int = 0,
    val enabled: Boolean = true
) {
    /**
     * Returns the appropriate icon based on the selection state.
     */
    fun getIcon(isSelected: Boolean): ImageVector =
        if (isSelected && selectedIcon != null) selectedIcon else icon
}

// ==================== MAIN BOTTOM NAV ITEMS (DEFAULT) ====================

/**
 * The default bottom navigation items – used in the main screen.
 * Must match the order of [Destinations.bottomNavDestinations].
 */
val bottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        destination = Destinations.Home
    ),
    BottomNavItem(
        title = "Statistics",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
        destination = Destinations.Statistics
    ),
    BottomNavItem(
        title = "Tools",
        icon = Icons.Outlined.Build,
        selectedIcon = Icons.Filled.Build,
        destination = Destinations.GestureStudio
    ),
    BottomNavItem(
        title = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        destination = Destinations.Settings
    )
)

// ==================== EXTENDED NAV ITEMS (TABLETS) ====================

/**
 * Extended bottom navigation for tablets or larger screens.
 */
val extendedBottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        destination = Destinations.Home
    ),
    BottomNavItem(
        title = "Statistics",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
        destination = Destinations.Statistics
    ),
    BottomNavItem(
        title = "Gesture Studio",
        icon = Icons.Outlined.Gesture,
        selectedIcon = Icons.Filled.Gesture,
        destination = Destinations.GestureStudio
    ),
    BottomNavItem(
        title = "Network",
        icon = Icons.Outlined.Wifi,
        selectedIcon = Icons.Filled.Wifi,
        destination = Destinations.NetworkDiscovery
    ),
    BottomNavItem(
        title = "Profiles",
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
        destination = Destinations.Profiles
    ),
    BottomNavItem(
        title = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        destination = Destinations.Settings
    ),
    BottomNavItem(
        title = "Help",
        icon = Icons.AutoMirrored.Filled.HelpOutline,
        selectedIcon = Icons.Filled.Help,
        destination = Destinations.Help
    )
)

// ==================== NAV ITEMS WITH BADGE SUPPORT ====================

/**
 * Default items with badge placeholders – useful for showing notifications.
 */
val bottomNavItemsWithBadges = listOf(
    BottomNavItem(
        title = "Home",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        destination = Destinations.Home,
        badgeCount = 0
    ),
    BottomNavItem(
        title = "Statistics",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
        destination = Destinations.Statistics,
        badgeCount = 0
    ),
    BottomNavItem(
        title = "Tools",
        icon = Icons.Outlined.Build,
        selectedIcon = Icons.Filled.Build,
        destination = Destinations.GestureStudio,
        badgeCount = 0
    ),
    BottomNavItem(
        title = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        destination = Destinations.Settings,
        badgeCount = 0
    )
)

// ==================== HELPER FUNCTIONS ====================

/**
 * Maps each destination to its corresponding bottom nav item for quick lookup.
 */
val destinationToBottomNavItem: Map<Destinations, BottomNavItem> =
    bottomNavItems.associateBy { it.destination }

/**
 * Retrieves the bottom nav item for a given destination.
 */
fun getBottomNavItemForDestination(destination: Destinations): BottomNavItem? =
    destinationToBottomNavItem[destination]

/**
 * Extension property to check if a destination appears in the bottom nav.
 */
fun Destinations.isInBottomNav(): Boolean =
    destinationToBottomNavItem.containsKey(this)

/**
 * Returns the index of a destination in the bottom nav list.
 */
fun getBottomNavIndex(destination: Destinations): Int =
    bottomNavItems.indexOfFirst { it.destination == destination }

/**
 * Returns the destination at a given index in the bottom nav list.
 */
fun getDestinationAtIndex(index: Int): Destinations? =
    bottomNavItems.getOrNull(index)?.destination

/**
 * Object holding constant titles for bottom nav items.
 */
object BottomNavTitles {
    const val HOME = "Home"
    const val STATISTICS = "Statistics"
    const val TOOLS = "Tools"
    const val SETTINGS = "Settings"
    const val NETWORK = "Network"
    const val PROFILES = "Profiles"
    const val HELP = "Help"
    const val GESTURE_STUDIO = "Gesture Studio"
}

/**
 * Returns the selected index based on the current route.
 * Used in the bottom bar composable.
 */
@Composable
fun getSelectedBottomNavIndex(currentRoute: String?): Int =
    when {
        currentRoute?.startsWith(Destinations.Home.route) == true -> 0
        currentRoute?.startsWith(Destinations.Statistics.route) == true -> 1
        currentRoute?.startsWith(Destinations.GestureStudio.route) == true -> 2
        currentRoute?.startsWith(Destinations.Settings.route) == true -> 3
        else -> 0
    }

/**
 * Returns the route for a given bottom nav index.
 */
fun getRouteForBottomNavIndex(index: Int): String =
    when (index) {
        0 -> Destinations.Home.route
        1 -> Destinations.Statistics.route
        2 -> Destinations.GestureStudio.route
        3 -> Destinations.Settings.route
        else -> Destinations.Home.route
    }

// ==================== STATE & CONFIGURATION ====================

/**
 * State class for the bottom navigation bar.
 */
data class BottomNavState(
    val selectedIndex: Int = 0,
    val items: List<BottomNavItem> = bottomNavItems,
    val showLabels: Boolean = true
)

/**
 * Predefined configurations for different use cases.
 */
object BottomNavConfigs {
    val default = BottomNavState()
    val noLabels = BottomNavState(showLabels = false)
    val extended = BottomNavState(items = extendedBottomNavItems)
    val withBadges = BottomNavState(items = bottomNavItemsWithBadges)
}

/**
 * Extension to update a single badge count.
 */
fun BottomNavItem.withBadgeCount(count: Int): BottomNavItem =
    this.copy(badgeCount = count)

/**
 * Extension to update badge counts for multiple items at once.
 */
fun List<BottomNavItem>.updateBadgeCounts(vararg updates: Pair<Destinations, Int>): List<BottomNavItem> {
    val updateMap = updates.toMap()
    return map { item ->
        if (updateMap.containsKey(item.destination)) {
            item.copy(badgeCount = updateMap[item.destination] ?: 0)
        } else {
            item
        }
    }
}