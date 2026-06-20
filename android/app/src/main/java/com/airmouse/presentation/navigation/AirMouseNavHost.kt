package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.airmouse.presentation.ui.main.MainNavHost

@Composable
fun AirMouseNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    MainNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    )
}
