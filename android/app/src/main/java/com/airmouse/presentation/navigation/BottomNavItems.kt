package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a bottom navigation item
 * @param title The display title of the item
 * @param icon The icon to show when not selected
 * @param selectedIcon The icon to show when selected (optional, falls back to icon)
 * @param destination The navigation destination
 * @param badgeCount Optional badge count to show on the item
 * @param enabled Whether the item is enabled
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
     * Get the appropriate icon based on selection state
     */
    fun getIcon(isSelected: Boolean): ImageVector {
        return if (isSelected && selectedIcon != null) selectedIcon else icon
    }
}

/**
 * Bottom navigation items for main screens
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

/**
 * Extended bottom navigation items for tablets/larger screens
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

/**
 * Bottom navigation items with badges for notifications
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

/**
 * Map of destination to bottom nav item for quick lookup
 */
val destinationToBottomNavItem: Map<Destinations, BottomNavItem> = bottomNavItems.associateBy { it.destination }

/**
 * Get bottom nav item for a given destination
 */
fun getBottomNavItemForDestination(destination: Destinations): BottomNavItem? {
    return destinationToBottomNavItem[destination]
}

/**
 * Check if a destination should be shown in bottom navigation
 */
fun Destinations.isInBottomNav(): Boolean {
    return this in destinationToBottomNavItem.keys
}

/**
 * Get the index of a destination in bottom navigation
 */
fun getBottomNavIndex(destination: Destinations): Int {
    return bottomNavItems.indexOfFirst { it.destination == destination }
}

/**
 * Get the destination at a specific bottom nav index
 */
fun getDestinationAtIndex(index: Int): Destinations? {
    return bottomNavItems.getOrNull(index)?.destination
}

/**
 * Screen titles for bottom navigation items
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
 * Helper function to get the current selected index based on route
 */
@Composable
fun getSelectedBottomNavIndex(currentRoute: String?): Int {
    return when {
        currentRoute?.startsWith(Destinations.Home.route) == true -> 0
        currentRoute?.startsWith(Destinations.Statistics.route) == true -> 1
        currentRoute?.startsWith(Destinations.GestureStudio.route) == true -> 2
        currentRoute?.startsWith(Destinations.Settings.route) == true -> 3
        else -> 0
    }
}

/**
 * Get the route for a given bottom nav index
 */
fun getRouteForBottomNavIndex(index: Int): String {
    return when (index) {
        0 -> Destinations.Home.route
        1 -> Destinations.Statistics.route
        2 -> Destinations.GestureStudio.route
        3 -> Destinations.Settings.route
        else -> Destinations.Home.route
    }
}

/**
 * Data class for bottom navigation state
 */
data class BottomNavState(
    val selectedIndex: Int = 0,
    val items: List<BottomNavItem> = bottomNavItems,
    val showLabels: Boolean = true
)

/**
 * Predefined bottom navigation configurations
 */
object BottomNavConfigs {
    val default = BottomNavState()
    
    val noLabels = BottomNavState(
        selectedIndex = 0,
        items = bottomNavItems,
        showLabels = false
    )
    
    val extended = BottomNavState(
        selectedIndex = 0,
        items = extendedBottomNavItems,
        showLabels = true
    )
    
    val withBadges = BottomNavState(
        selectedIndex = 0,
        items = bottomNavItemsWithBadges,
        showLabels = true
    )
}

/**
 * Extension function to update badge counts
 */
fun BottomNavItem.withBadgeCount(count: Int): BottomNavItem {
    return this.copy(badgeCount = count)
}

/**
 * Extension function to update multiple badge counts
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