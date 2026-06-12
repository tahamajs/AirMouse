package com.airmouse.presentation.ui.navigation

import androidx.annotation.DrawableRes
import com.airmouse.R

data class BottomNavItem(val title: String, @DrawableRes val iconRes: Int, val destination: Destinations)

val bottomNavItems = listOf(
    BottomNavItem("Home", R.drawable.ic_refresh, Destinations.Home),
    BottomNavItem("Statistics", R.drawable.ic_logs, Destinations.Statistics),
    BottomNavItem("Settings", R.drawable.ic_settings, Destinations.Settings),
    BottomNavItem("Help", R.drawable.ic_help_start, Destinations.Help)
)
