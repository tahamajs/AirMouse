// app/src/main/java/com/airmouse/presentation/navigation/BottomNavItems.kt
package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val destination: Destinations
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "Home",
        icon = Icons.Default.Home,
        destination = Destinations.Home
    ),
    BottomNavItem(
        title = "Statistics",
        icon = Icons.Default.BarChart,
        destination = Destinations.Statistics
    ),
    BottomNavItem(
        title = "Settings",
        icon = Icons.Default.Settings,
        destination = Destinations.Settings
    ),
    BottomNavItem(
        title = "Help",
        icon = Icons.Default.Info,
        destination = Destinations.Help
    )
)