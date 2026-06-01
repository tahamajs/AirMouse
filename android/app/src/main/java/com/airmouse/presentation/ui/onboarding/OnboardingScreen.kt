package com.airmouse.presentation.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions

@Composable
fun OnboardingScreen(
    navigationActions: NavigationActions,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = listOf(
        OnboardingItem(R.drawable.ic_air_mouse, "Welcome", "Turn your phone into a smart remote"),
        OnboardingItem(R.drawable.ic_gesture, "Custom Gestures", "Train your own gestures with AI"),
        OnboardingItem(R.drawable.ic_proximity, "Proximity Lock", "Auto-lock when you walk away")
    )

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { navigationActions.navigateToHome() }) {
                    Text("Skip")
                }
                Button(
                    onClick = {
                        if (uiState.currentPage < items.size - 1) viewModel.nextPage()
                        else navigationActions.navigateToHome()
                    }
                ) {
                    Text(if (uiState.currentPage == items.size - 1) "Get Started" else "Next")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = items[uiState.currentPage].imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(items[uiState.currentPage].title, style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(items[uiState.currentPage].description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

data class OnboardingItem(val imageRes: Int, val title: String, val description: String)