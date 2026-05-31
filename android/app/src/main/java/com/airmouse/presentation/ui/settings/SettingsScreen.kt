package com.airmouse.presentation.ui.settings

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
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
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
                        Text("Cursor Sensitivity", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.sensitivity,
                            onValueChange = viewModel::updateSensitivity,
                            valueRange = 0.2f..2.0f,
                            steps = 18
                        )
                        Text("${"%.2f".format(uiState.sensitivity)}x")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Click Threshold", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.clickThreshold,
                            onValueChange = viewModel::updateClickThreshold,
                            valueRange = 0f..20f,
                            steps = 40
                        )
                        Text("${"%.1f".format(uiState.clickThreshold)} rad/s")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Double Click Interval", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = uiState.doubleClickInterval.toFloat(),
                            onValueChange = { viewModel.updateDoubleClickInterval(it.toLong()) },
                            valueRange = 200f..1000f,
                            steps = 80
                        )
                        Text("${uiState.doubleClickInterval} ms")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Haptic Feedback")
                        Switch(checked = uiState.hapticEnabled, onCheckedChange = viewModel::updateHaptic)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("AI Smoothing")
                        Switch(checked = uiState.aiSmoothing, onCheckedChange = viewModel::updateAiSmoothing)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Predictive Movement")
                        Switch(checked = uiState.predictive, onCheckedChange = viewModel::updatePredictive)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            listOf("dark", "light", "pure_black", "high_contrast").forEach { theme ->
                            TextButton(
                                onClick = { viewModel.updateTheme(theme) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (uiState.theme == theme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(theme.replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }
        }
    }
}
}