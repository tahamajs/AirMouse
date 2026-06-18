package com.airmouse.presentation.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedSection by remember { mutableStateOf<SettingsSection?>(SettingsSection.CURSOR) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Success/Error messages
            if (uiState.success != null) {
                item {
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                        }
                    ) { Text(uiState.success!!, color = Color.White) }
                }
            }

            if (uiState.error != null) {
                item {
                    Snackbar(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                        }
                    ) { Text(uiState.error!!, color = Color.White) }
                }
            }

            // Settings Sections
            items(SettingsSection.values()) { section ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    SettingsSectionCard(
                        title = section.title,
                        icon = section.icon,
                        description = section.description,
                        isExpanded = expandedSection == section,
                        onExpand = { expandedSection = if (expandedSection == section) null else section },
                        content = { SettingsContent(section = section, viewModel = viewModel, uiState = uiState) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsContent(
    section: SettingsSection,
    viewModel: SettingsViewModel,
    uiState: SettingsUiState
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (section) {
            SettingsSection.CURSOR -> CursorSettingsContent(viewModel, uiState)
            SettingsSection.GESTURE -> GestureSettingsContent(viewModel, uiState)
            SettingsSection.AI -> AISettingsContent(viewModel, uiState)
            SettingsSection.HAPTIC -> HapticSettingsContent(viewModel, uiState)
            SettingsSection.DISPLAY -> DisplaySettingsContent(viewModel, uiState)
            SettingsSection.CONNECTION -> ConnectionSettingsContent(viewModel, uiState)
            SettingsSection.PRIVACY -> PrivacySettingsContent(viewModel, uiState)
            SettingsSection.PRESENTATION -> PresentationSettingsContent(viewModel, uiState)
            SettingsSection.ABOUT -> AboutSettingsContent(viewModel, uiState)
        }
    }
}

@Composable
private fun CursorSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SliderSetting(
        label = "Sensitivity",
        value = uiState.sensitivity,
        onValueChange = viewModel::updateSensitivity,
        valueRange = 0.1f..2.0f,
        formatValue = { String.format("%.2f", it) },
        icon = Icons.Default.Speed
    )
    SwitchSetting(
        label = "Mouse Acceleration",
        description = "Increase cursor speed with fast movements",
        checked = uiState.accelerationEnabled,
        onCheckedChange = viewModel::updateAccelerationEnabled,
        icon = Icons.Default.TrendingUp
    )
    if (uiState.accelerationEnabled) {
        SliderSetting(
            label = "Acceleration Factor",
            value = uiState.accelerationFactor,
            onValueChange = viewModel::updateAccelerationFactor,
            valueRange = 1.0f..3.0f,
            formatValue = { String.format("%.1fx", it) }
        )
    }
    SwitchSetting(
        label = "Invert X-Axis",
        description = "Reverse horizontal movement",
        checked = uiState.invertX,
        onCheckedChange = viewModel::updateInvertX,
        icon = Icons.Default.SwapHoriz
    )
    SwitchSetting(
        label = "Invert Y-Axis",
        description = "Reverse vertical movement",
        checked = uiState.invertY,
        onCheckedChange = viewModel::updateInvertY,
        icon = Icons.Default.SwapVert
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

@Composable
private fun GestureSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SliderSetting(
        label = "Click Threshold",
        value = uiState.clickThreshold,
        onValueChange = viewModel::updateClickThreshold,
        valueRange = 5f..20f,
        formatValue = { String.format("%.1f", it) },
        description = "Sensitivity for click detection",
        icon = Icons.Default.TouchApp
    )
    SliderSetting(
        label = "Double Click Interval",
        value = uiState.doubleClickInterval.toFloat(),
        onValueChange = { viewModel.updateDoubleClickInterval(it.toLong()) },
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
        description = "Sensitivity for scroll detection",
        icon = Icons.Default.SwapVert
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
        onValueChange = { viewModel.updateRightClickDuration(it.toLong()) },
        valueRange = 200f..1000f,
        formatValue = { String.format("%.0f ms", it) }
    )
    SliderSetting(
        label = "Gesture Debounce",
        value = uiState.gestureDebounce.toFloat(),
        onValueChange = { viewModel.updateGestureDebounce(it.toLong()) },
        valueRange = 50f..300f,
        formatValue = { String.format("%.0f ms", it) }
    )
}

@Composable
private fun AISettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SwitchSetting(
        label = "AI Smoothing",
        description = "Machine learning for smoother cursor",
        checked = uiState.aiSmoothing,
        onCheckedChange = viewModel::updateAiSmoothing,
        icon = Icons.Default.Psychology
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
            formatValue = { String.format("%.2f", it) }
        )
    }
    SwitchSetting(
        label = "Kalman Filter",
        description = "Advanced noise reduction",
        checked = uiState.kalmanEnabled,
        onCheckedChange = viewModel::updateKalmanEnabled
    )
}

@Composable
private fun HapticSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SwitchSetting(
        label = "Haptic Feedback",
        description = "Vibration on gestures",
        checked = uiState.hapticEnabled,
        onCheckedChange = viewModel::updateHaptic,
        icon = Icons.Default.Vibration
    )
    if (uiState.hapticEnabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Strength", style = MaterialTheme.typography.bodyLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                HapticStrength.values().forEach { strength ->
                    FilterChip(
                        selected = uiState.hapticStrength == strength,
                        onClick = { viewModel.updateHapticStrength(strength) },
                        label = { Text(strength.displayName) },
                        modifier = Modifier.weight(1f)
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

@Composable
private fun DisplaySettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    DropdownSetting(
        label = "Theme",
        options = listOf("light", "dark", "pure_black", "high_contrast", "system"),
        selected = uiState.theme,
        onSelected = viewModel::updateTheme,
        icon = Icons.Default.Palette
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
        formatValue = { String.format("%.0f sp", it) },
        icon = Icons.Default.TextFields
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

@Composable
private fun ConnectionSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SwitchSetting(
        label = "Auto Connect",
        description = "Connect to last server automatically",
        checked = uiState.autoConnect,
        onCheckedChange = viewModel::updateAutoConnect,
        icon = Icons.Default.Autorenew
    )
    SliderSetting(
        label = "Reconnect Attempts",
        value = uiState.reconnectAttempts.toFloat(),
        onValueChange = { viewModel.updateReconnectAttempts(it.toInt()) },
        valueRange = 1f..10f,
        formatValue = { String.format("%.0f", it) }
    )
    SliderSetting(
        label = "Connection Timeout",
        value = uiState.connectionTimeout.toFloat(),
        onValueChange = { viewModel.updateConnectionTimeout(it.toInt()) },
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

@Composable
private fun PrivacySettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SwitchSetting(
        label = "Anonymous Usage Stats",
        description = "Help improve the app",
        checked = uiState.anonymousStats,
        onCheckedChange = viewModel::updateAnonymousStats,
        icon = Icons.Default.Visibility
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
        onCheckedChange = viewModel::updateClearDataOnExit,
        icon = Icons.Default.Delete
    )
}

@Composable
private fun PresentationSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    SwitchSetting(
        label = "Presentation Mode",
        description = "Enable presentation controls",
        checked = uiState.presentationModeEnabled,
        onCheckedChange = viewModel::updatePresentationModeEnabled,
        icon = Icons.Default.Slideshow
    )
    if (uiState.presentationModeEnabled) {
        SliderSetting(
            label = "Laser Pointer Speed",
            value = uiState.laserPointerSpeed,
            onValueChange = viewModel::updateLaserPointerSpeed,
            valueRange = 0.5f..2.0f,
            formatValue = { String.format("%.1fx", it) }
        )
        SwitchSetting(
            label = "Show Presentation Timer",
            description = "Display timer during presentation",
            checked = uiState.showPresentationTimer,
            onCheckedChange = viewModel::updateShowPresentationTimer
        )
        SwitchSetting(
            label = "Auto-hide Laser Pointer",
            description = "Hide laser after inactivity",
            checked = uiState.autoHideLaser,
            onCheckedChange = viewModel::updateAutoHideLaser
        )
    }
}

@Composable
private fun AboutSettingsContent(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Air Mouse Pro", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Version 3.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Turn your phone into a wireless mouse", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { viewModel.openWebsite() }) { Text("Website") }
                OutlinedButton(onClick = { viewModel.openPrivacyPolicy() }) { Text("Privacy") }
                OutlinedButton(onClick = { viewModel.openLicense() }) { Text("License") }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    description: String,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
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
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (description.isNotEmpty()) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
    description: String = "",
    icon: ImageVector? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        if (description.isNotEmpty()) {
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun DropdownSetting(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    icon: ImageVector? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().width(130.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replaceFirstChar { it.uppercase() }) },
                        onClick = { onSelected(option); expanded = false }
                    )
                }
            }
        }
    }
}

