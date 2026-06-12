package com.airmouse.presentation.ui.battery

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    navigationActions: NavigationActions,
    viewModel: BatteryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Battery Monitor") }, navigationIcon = { BackButton(navigationActions) }) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Battery Level", style = MaterialTheme.typography.titleMedium)
                    Text("${uiState.level}%", style = MaterialTheme.typography.displaySmall)
                    LinearProgressIndicator(progress = uiState.level / 100f, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Temperature", style = MaterialTheme.typography.titleMedium)
                    Text("${"%.1f".format(uiState.temperature)}°C", style = MaterialTheme.typography.displaySmall)
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Battery Usage History", style = MaterialTheme.typography.titleMedium)
                    Text("History points: ${uiState.history.size}")
                }
            }
        }
    }
}

@Composable
private fun BackButton(navigationActions: NavigationActions) {
    IconButton(onClick = { navigationActions.navigateBack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
