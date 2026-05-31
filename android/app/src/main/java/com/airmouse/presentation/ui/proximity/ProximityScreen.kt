package com.airmouse.presentation.ui.proximity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProximityScreen(
    viewModel: ProximityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Proximity Lock") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Enable Proximity Service")
                            Switch(checked = uiState.isEnabled, onCheckedChange = viewModel::toggleService)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.status)
                        uiState.currentDistance?.let {
                            Text("Distance: ${"%.2f".format(it)} m", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Near Threshold (unlock)", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.nearThreshold,
                            onValueChange = viewModel::updateNearThreshold,
                            valueRange = 0.5f..5.0f,
                            steps = 45
                        )
                        Text("${"%.1f".format(uiState.nearThreshold)} m")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Far Threshold (lock)", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.farThreshold,
                            onValueChange = viewModel::updateFarThreshold,
                            valueRange = 1.0f..10.0f,
                            steps = 90
                        )
                        Text("${"%.1f".format(uiState.farThreshold)} m")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::calibrate,
                            enabled = !uiState.isCalibrating,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (uiState.isCalibrating) "Calibrating..." else "Calibrate Distance")
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How It Works", style = MaterialTheme.typography.titleMedium)
                        Text("When your phone moves beyond the far threshold, the computer locks. When you return within the near threshold, it unlocks.")
                    }
                }
            }
        }
    }
}