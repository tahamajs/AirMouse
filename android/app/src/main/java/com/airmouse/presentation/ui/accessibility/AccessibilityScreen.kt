// app/src/main/java/com/airmouse/presentation/ui/accessibility/AccessibilityScreen.kt
package com.airmouse.presentation.ui.accessibility

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    navigationActions: NavigationActions,
    viewModel: AccessibilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Announce Movement
            SettingsSwitchCard(
                title = "Announce Movement",
                description = "TalkBack will read cursor movement aloud",
                checked = uiState.announceMovement,
                onCheckedChange = viewModel::updateAnnounceMovement
            )

            // Announce Clicks
            SettingsSwitchCard(
                title = "Announce Clicks",
                description = "TalkBack will announce click actions",
                checked = uiState.announceClicks,
                onCheckedChange = viewModel::updateAnnounceClicks
            )

            // High Contrast Mode
            SettingsSwitchCard(
                title = "High Contrast UI",
                description = "Increases contrast for better visibility",
                checked = uiState.highContrast,
                onCheckedChange = viewModel::updateHighContrast
            )

            // Large Text
            SettingsSwitchCard(
                title = "Large Text",
                description = "Increase text size throughout the app",
                checked = uiState.largeText,
                onCheckedChange = viewModel::updateLargeText
            )

            // Reduce Motion
            SettingsSwitchCard(
                title = "Reduce Motion",
                description = "Disable animations and transitions",
                checked = uiState.reduceMotion,
                onCheckedChange = viewModel::updateReduceMotion
            )

            // Color Blind Mode
            SettingsDropdownCard(
                title = "Color Blind Mode",
                description = "Adjust colors for color vision deficiency",
                selectedValue = uiState.colorBlindMode.displayName,
                options = ColorBlindMode.values().map { it.displayName to it },
                onOptionSelected = { mode -> viewModel.updateColorBlindMode(mode) }
            )

            // Custom Font Size
            SettingsSliderCard(
                title = "Custom Font Size",
                description = "Adjust text size to your preference",
                value = uiState.customFontSize,
                onValueChange = viewModel::updateCustomFontSize,
                valueRange = 12f..24f,
                formatValue = { "${it.toInt()}sp" }
            )

            // Spoken Feedback (Experimental)
            SettingsSwitchCard(
                title = "Spoken Feedback",
                description = "Experimental: Voice feedback for actions",
                checked = uiState.spokenFeedbackEnabled,
                onCheckedChange = viewModel::updateSpokenFeedback
            )

            // Haptic Feedback
            SettingsSwitchCard(
                title = "Haptic Feedback",
                description = "Vibrate on touch and gestures",
                checked = uiState.hapticFeedbackEnabled,
                onCheckedChange = viewModel::updateHapticFeedback
            )

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Screen Reader Compatibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Air Mouse Pro works with TalkBack and other screen readers. " +
                                "Enable announcements above for full voice feedback.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsDropdownCard(
    title: String,
    description: String,
    selectedValue: String,
    options: List<Pair<String, ColorBlindMode>>,
    onOptionSelected: (ColorBlindMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedValue)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowBack, contentDescription = "Dropdown")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (label, mode) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onOptionSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSliderCard(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatValue(valueRange.start), fontSize = 12.sp)
                Text(formatValue(value), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(formatValue(valueRange.endInclusive), fontSize = 12.sp)
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}