package com.airmouse.presentation.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(val title: String, val icon: ImageVector, val destination: Destinations)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Default.Home, Destinations.Home),
    BottomNavItem("Statistics", Icons.Default.BarChart, Destinations.Statistics),
    BottomNavItem("Settings", Icons.Default.Settings, Destinations.Settings),
    BottomNavItem("Help", Icons.Default.Info, Destinations.Help)
)