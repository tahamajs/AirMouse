package com.airmouse.presentation.ui.edge

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
fun EdgeGesturesScreen(
    viewModel: EdgeGesturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = listOf("Click", "Double Click", "Right Click", "Scroll Up", "Scroll Down")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edge Gestures") }) }
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable Edge Gestures")
                        Switch(checked = uiState.isEnabled, onCheckedChange = viewModel::toggleService)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Volume Up (long press)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            actions.forEach { action ->
                                FilterChip(
                                    selected = uiState.volumeUpAction == action,
                                    onClick = { viewModel.updateVolumeUpAction(action) },
                                    label = { Text(action) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Volume Down (long press)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            actions.forEach { action ->
                                FilterChip(
                                    selected = uiState.volumeDownAction == action,
                                    onClick = { viewModel.updateVolumeDownAction(action) },
                                    label = { Text(action) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How It Works", style = MaterialTheme.typography.titleMedium)
                        Text("Long press volume keys to trigger actions even when the screen is off. Requires accessibility permission.")
                    }
                }
            }
        }
    }
}