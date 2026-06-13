package com.airmouse.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedSection by remember { mutableStateOf(SettingsSection.CURSOR) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
                    }
                    IconButton(onClick = { viewModel.exportSettings() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success/Error messages
            if (uiState.success != null) {
                item {
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(uiState.success!!)
                    }
                }
            }

            // Cursor Settings
            item {
                SettingsSectionCard(
                    title = "Cursor Settings",
                    icon = Icons.Default.Mouse,
                    isExpanded = expandedSection == SettingsSection.CURSOR,
                    onExpand = { expandedSection = SettingsSection.CURSOR },
                    content = {
                        SliderSetting(
                            label = "Sensitivity",
                            value = uiState.sensitivity,
                            onValueChange = viewModel::updateSensitivity,
                            valueRange = 0.1f..2.0f,
                            formatValue = { String.format("%.2f", it) }
                        )
                        SwitchSetting(
                            label = "Mouse Acceleration",
                            description = "Increase cursor speed with fast movements",
                            checked = uiState.accelerationEnabled,
                            onCheckedChange = viewModel::updateAccelerationEnabled
                        )
                        if (uiState.accelerationEnabled) {
                            SliderSetting(
                                label = "Acceleration Factor",
                                value = uiState.accelerationFactor,
                                onValueChange = viewModel::updateAccelerationFactor,
                                valueRange = 1.0f..3.0f,
                                formatValue = { String.format("%.1f", it) }
                            )
                        }
                        SwitchSetting(
                            label = "Invert X-Axis",
                            description = "Reverse horizontal movement",
                            checked = uiState.invertX,
                            onCheckedChange = viewModel::updateInvertX
                        )
                        SwitchSetting(
                            label = "Invert Y-Axis",
                            description = "Reverse vertical movement",
                            checked = uiState.invertY,
                            onCheckedChange = viewModel::updateInvertY
                        )
                        SwitchSetting(
                            label = "Cursor Smoothing",
                            description = "Smoother cursor movement",
                            checked = uiState.smoothingEnabled,
                            onCheckedChange = viewModel::updateSmoothingEnabled
                        )
                        if (uiState.smoothingEnabled) {
                            SliderSetting(
                                label = "Smoothing Factor",
                                value = uiState.smoothingFactor,
                                onValueChange = viewModel::updateSmoothingFactor,
                                valueRange = 0.1f..1.0f,
                                formatValue = { String.format("%.2f", it) }
                            )
                        }
                    }
                )
            }

            // Gesture Settings
            item {
                SettingsSectionCard(
                    title = "Gesture Settings",
                    icon = Icons.Default.Gesture,
                    isExpanded = expandedSection == SettingsSection.GESTURE,
                    onExpand = { expandedSection = SettingsSection.GESTURE },
                    content = {
                        SliderSetting(
                            label = "Click Threshold",
                            value = uiState.clickThreshold,
                            onValueChange = viewModel::updateClickThreshold,
                            valueRange = 5f..20f,
                            formatValue = { String.format("%.1f", it) },
                            description = "Sensitivity for click detection"
                        )
                        SliderSetting(
                            label = "Double Click Interval",
                            value = uiState.doubleClickInterval.toFloat(),
                            onValueChange = { viewModel::updateDoubleClickInterval(it.toLong()) },
                            valueRange = 100f..600f,
                            formatValue = { String.format("%.0f ms", it) },
                            description = "Time window for double click"
                        )
                        SliderSetting(
                            label = "Scroll Threshold",
                            value = uiState.scrollThreshold,
                            onValueChange = viewModel::updateScrollThreshold,
                            valueRange = 3f..15f,
                            formatValue = { String.format("%.1f", it) },
                            description = "Sensitivity for scroll detection"
                        )
                        SliderSetting(
                            label = "Right Click Tilt",
                            value = uiState.rightClickTilt,
                            onValueChange = viewModel::updateRightClickTilt,
                            valueRange = 10f..45f,
                            formatValue = { String.format("%.0f°", it) },
                            description = "Tilt angle for right click"
                        )
                        SliderSetting(
                            label = "Right Click Duration",
                            value = uiState.rightClickDuration.toFloat(),
                            onValueChange = { viewModel::updateRightClickDuration(it.toLong()) },
                            valueRange = 200f..1000f,
                            formatValue = { String.format("%.0f ms", it) },
                            description = "Hold duration for right click"
                        )
                        SliderSetting(
                            label = "Gesture Debounce",
                            value = uiState.gestureDebounce.toFloat(),
                            onValueChange = { viewModel::updateGestureDebounce(it.toLong()) },
                            valueRange = 50f..300f,
                            formatValue = { String.format("%.0f ms", it) },
                            description = "Prevent duplicate gestures"
                        )
                    }
                )
            }

            // AI & Predictive Settings
            item {
                SettingsSectionCard(
                    title = "AI & Predictive",
                    icon = Icons.Default.Psychology,
                    isExpanded = expandedSection == SettingsSection.AI,
                    onExpand = { expandedSection = SettingsSection.AI },
                    content = {
                        SwitchSetting(
                            label = "AI Smoothing",
                            description = "Machine learning for smoother cursor",
                            checked = uiState.aiSmoothing,
                            onCheckedChange = viewModel::updateAiSmoothing
                        )
                        if (uiState.aiSmoothing) {
                            SliderSetting(
                                label = "AI Blend Factor",
                                value = uiState.aiBlendFactor,
                                onValueChange = viewModel::updateAiBlendFactor,
                                valueRange = 0f..1f,
                                formatValue = { String.format("%.2f", it) },
                                description = "Balance between raw and AI movement"
                            )
                        }
                        SwitchSetting(
                            label = "Predictive Movement",
                            description = "Kalman filter for reduced lag",
                            checked = uiState.predictive,
                            onCheckedChange = viewModel::updatePredictive
                        )
                        if (uiState.predictive) {
                            SliderSetting(
                                label = "Prediction Strength",
                                value = uiState.predictionStrength,
                                onValueChange = viewModel::updatePredictionStrength,
                                valueRange = 0f..1f,
                                formatValue = { String.format("%.2f", it) },
                                description = "How aggressive the prediction is"
                            )
                        }
                        SwitchSetting(
                            label = "Kalman Filter",
                            description = "Advanced noise reduction",
                            checked = uiState.kalmanEnabled,
                            onCheckedChange = viewModel::updateKalmanEnabled
                        )
                    }
                )
            }

            // Haptic & Feedback
            item {
                SettingsSectionCard(
                    title = "Haptic & Feedback",
                    icon = Icons.Default.Vibration,
                    isExpanded = expandedSection == SettingsSection.HAPTIC,
                    onExpand = { expandedSection = SettingsSection.HAPTIC },
                    content = {
                        SwitchSetting(
                            label = "Haptic Feedback",
                            description = "Vibration on gestures",
                            checked = uiState.hapticEnabled,
                            onCheckedChange = viewModel::updateHaptic
                        )
                        if (uiState.hapticEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Strength")
                                SegmentedButton(
                                    modifier = Modifier.weight(1f),
                                    shape = SegmentedButtonDefaults.shape()
                                ) {
                                    HapticStrength.values().forEach { strength ->
                                        SegmentedButtonItem(
                                            selected = uiState.hapticStrength == strength,
                                            onClick = { viewModel.updateHapticStrength(strength) },
                                            label = { Text(strength.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                        SwitchSetting(
                            label = "Sound Effects",
                            description = "Audio feedback for actions",
                            checked = uiState.soundEnabled,
                            onCheckedChange = viewModel::updateSoundEnabled
                        )
                        SwitchSetting(
                            label = "Visual Feedback",
                            description = "Screen indicators for gestures",
                            checked = uiState.visualFeedback,
                            onCheckedChange = viewModel::updateVisualFeedback
                        )
                        SwitchSetting(
                            label = "Gesture Notifications",
                            description = "Show notification on gesture detection",
                            checked = uiState.notificationOnGesture,
                            onCheckedChange = viewModel::updateNotificationOnGesture
                        )
                    }
                )
            }

            // Display Settings
            item {
                SettingsSectionCard(
                    title = "Display",
                    icon = Icons.Default.DisplaySettings,
                    isExpanded = expandedSection == SettingsSection.DISPLAY,
                    onExpand = { expandedSection = SettingsSection.DISPLAY },
                    content = {
                        DropdownSetting(
                            label = "Theme",
                            options = listOf("light", "dark", "pure_black", "high_contrast", "system"),
                            selected = uiState.theme,
                            onSelected = viewModel::updateTheme
                        )
                        SwitchSetting(
                            label = "Dynamic Colors",
                            description = "Use Material You colors (Android 12+)",
                            checked = uiState.useDynamicColors,
                            onCheckedChange = viewModel::updateUseDynamicColors
                        )
                        SliderSetting(
                            label = "Font Size",
                            value = uiState.fontSize,
                            onValueChange = viewModel::updateFontSize,
                            valueRange = 12f..24f,
                            formatValue = { String.format("%.0f sp", it) }
                        )
                        SwitchSetting(
                            label = "Debug Info",
                            description = "Show technical information",
                            checked = uiState.showDebugInfo,
                            onCheckedChange = viewModel::updateShowDebugInfo
                        )
                        SwitchSetting(
                            label = "Keep Screen On",
                            description = "Prevent screen timeout",
                            checked = uiState.keepScreenOn,
                            onCheckedChange = viewModel::updateKeepScreenOn
                        )
                        SwitchSetting(
                            label = "Show FPS Counter",
                            description = "Display frame rate",
                            checked = uiState.showFps,
                            onCheckedChange = viewModel::updateShowFps
                        )
                    }
                )
            }

            // Connection Settings
            item {
                SettingsSectionCard(
                    title = "Connection",
                    icon = Icons.Default.Wifi,
                    isExpanded = expandedSection == SettingsSection.CONNECTION,
                    onExpand = { expandedSection = SettingsSection.CONNECTION },
                    content = {
                        SwitchSetting(
                            label = "Auto Connect",
                            description = "Connect to last server automatically",
                            checked = uiState.autoConnect,
                            onCheckedChange = viewModel::updateAutoConnect
                        )
                        SliderSetting(
                            label = "Reconnect Attempts",
                            value = uiState.reconnectAttempts.toFloat(),
                            onValueChange = { viewModel::updateReconnectAttempts(it.toInt()) },
                            valueRange = 1f..10f,
                            formatValue = { String.format("%.0f", it) }
                        )
                        SliderSetting(
                            label = "Connection Timeout",
                            value = uiState.connectionTimeout.toFloat(),
                            onValueChange = { viewModel::updateConnectionTimeout(it.toInt()) },
                            valueRange = 1000f..10000f,
                            formatValue = { String.format("%.0f ms", it) }
                        )
                        SwitchSetting(
                            label = "Use WebSocket",
                            description = "Use WebSocket instead of TCP",
                            checked = uiState.useWebSocket,
                            onCheckedChange = viewModel::updateUseWebSocket
                        )
                        SwitchSetting(
                            label = "UDP Discovery",
                            description = "Auto-discover servers",
                            checked = uiState.useUdpDiscovery,
                            onCheckedChange = viewModel::updateUseUdpDiscovery
                        )
                    }
                )
            }

            // Privacy & Data
            item {
                SettingsSectionCard(
                    title = "Privacy & Data",
                    icon = Icons.Default.PrivacyTip,
                    isExpanded = expandedSection == SettingsSection.PRIVACY,
                    onExpand = { expandedSection = SettingsSection.PRIVACY },
                    content = {
                        SwitchSetting(
                            label = "Anonymous Usage Stats",
                            description = "Help improve the app",
                            checked = uiState.anonymousStats,
                            onCheckedChange = viewModel::updateAnonymousStats
                        )
                        SwitchSetting(
                            label = "Crash Reporting",
                            description = "Send crash reports automatically",
                            checked = uiState.crashReporting,
                            onCheckedChange = viewModel::updateCrashReporting
                        )
                        SwitchSetting(
                            label = "Clear Data on Exit",
                            description = "Remove all data when closing app",
                            checked = uiState.clearDataOnExit,
                            onCheckedChange = viewModel::updateClearDataOnExit
                        )
                    }
                )
            }
        }
    }
}

// Settings Section Types
enum class SettingsSection {
    CURSOR, GESTURE, AI, HAPTIC, DISPLAY, CONNECTION, PRIVACY
}

// Reusable Composables
@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            if (isExpanded) {
                Divider()
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    description: String = ""
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        if (description.isNotEmpty()) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DropdownSetting(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(120.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.capitalize()) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}