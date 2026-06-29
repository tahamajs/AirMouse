package com.airmouse.presentation.ui.touchpad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.presentation.navigation.NavigationActions

@Composable
fun TouchpadSettingsScreen(
    navigationActions: NavigationActions,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touchpad Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Touchpad Control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Use the touchpad screen to control the cursor. These settings are shared with the main touchpad engine and persist automatically.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.handleEvent(TouchpadEvent.ToggleTouchpad) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.TouchApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isActive) "Disable Touchpad" else "Enable Touchpad")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Quick Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (uiState.connectionTestMessage.isNotBlank()) uiState.connectionTestMessage
                        else "Run a quick connection test to confirm the server is reachable.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { viewModel.testConnection() },
                        enabled = !uiState.isTestingConnection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isTestingConnection) "Testing..." else "Test Connection")
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Shared Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "The touchpad uses the shared preference-backed values, so changes here are reflected in the main settings screens too.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()
                    TouchpadQuickSetting(
                        label = "Tap to Click",
                        checked = uiState.tapToClick,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleTapToClick) }
                    )
                    TouchpadQuickSetting(
                        label = "Haptic Feedback",
                        checked = uiState.hapticFeedback,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleHapticFeedback) }
                    )
                    TouchpadQuickSetting(
                        label = "Show Touch Points",
                        checked = uiState.showTouchPoints,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleShowTouchPoints) }
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { navigationActions.navigateToSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Full Settings")
                    }
                }
            }

            TextButton(
                onClick = { navigationActions.navigateBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun TouchpadQuickSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
