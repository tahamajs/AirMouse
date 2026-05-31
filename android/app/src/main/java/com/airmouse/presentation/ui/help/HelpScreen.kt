package com.airmouse.presentation.ui.help

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
fun HelpScreen(
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Help & User Guide") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            helpSections.forEach { section ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.toggleSection(section.title) }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(section.title, style = MaterialTheme.typography.titleMedium)
                                Text(if (uiState.expandedSections.contains(section.title)) "▲" else "▼")
                            }
                            if (uiState.expandedSections.contains(section.title)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(section.content)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val helpSections = listOf(
    HelpSection("Getting Started", "1. Install the app.\n2. Run the desktop server.\n3. Enter the IP and port.\n4. Start moving your phone."),
    HelpSection("Connection", "Both devices must be on the same Wi‑Fi network. Use the QR code for quick pairing."),
    HelpSection("Calibration", "Follow the calibration wizard to align gyroscope, accelerometer, and magnetometer."),
    HelpSection("Gestures", "Quick twist → click. Fast vertical move → scroll. Two‑finger tap → right click."),
    HelpSection("Troubleshooting", "If the cursor is laggy, reduce sensitivity or enable predictive movement.")
)

data class HelpSection(val title: String, val content: String)