package com.airmouse.presentation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.saveAllSettings() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (selectedSection == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SettingsHeader()
                }

                items(SettingsSection.entries) { section ->
                    SettingsCard(
                        title = section.title,
                        description = section.description,
                        icon = section.icon,
                        onClick = { selectedSection = section }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            SectionDetailScreen(
                section = selectedSection!!,
                uiState = uiState,
                onUpdate = { viewModel.updateUiState(it) },
                onBack = { selectedSection = null }
            )
        }
    }
}

@Composable
fun SettingsHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚙️ Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Customize your Air Mouse experience",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== SECTION DETAIL SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    section: SettingsSection,
    uiState: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            section.icon,
                            contentDescription = section.title,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            section.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                SettingsSection.CURSOR -> item { CursorSettings(uiState, onUpdate) }
                SettingsSection.GESTURE -> item { GestureSettings(uiState, onUpdate) }
                SettingsSection.AI -> item { AISettings(uiState, onUpdate) }
                SettingsSection.HAPTIC -> item { HapticSettings(uiState, onUpdate) }
                SettingsSection.DISPLAY -> item { DisplaySettings(uiState, onUpdate) }
                SettingsSection.CONNECTION -> item { ConnectionSettings(uiState, onUpdate) }
                SettingsSection.PRIVACY -> item { PrivacySettings(uiState, onUpdate) }
                SettingsSection.PRESENTATION -> item { PresentationSettings(uiState, onUpdate) }
                SettingsSection.ABOUT -> item { AboutSection() }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ==================== UI COMPONENTS ====================

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    formatValue: (Float) -> String = { "%.2f".format(it) },
    description: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    formatValue(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ==================== CURSOR SETTINGS ====================

@Composable
fun CursorSettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Sensitivity",
            value = uiState.sensitivity,
            onValueChange = { onUpdate(uiState.copy(sensitivity = it)) },
            valueRange = 0.1f..2.0f,
            steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Higher values make cursor faster"
        )

        SettingsSlider(
            title = "Smoothing Factor",
            value = uiState.smoothingFactor,
            onValueChange = { onUpdate(uiState.copy(smoothingFactor = it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Smooths cursor movement"
        )

        SettingsSwitch(
            title = "Acceleration",
            description = "Cursor speed increases with faster movement",
            checked = uiState.accelerationEnabled,
            onCheckedChange = { onUpdate(uiState.copy(accelerationEnabled = it)) }
        )

        SettingsSwitch(
            title = "Invert X Axis",
            description = "Swap left/right movement direction",
            checked = uiState.invertX,
            onCheckedChange = { onUpdate(uiState.copy(invertX = it)) }
        )

        SettingsSwitch(
            title = "Invert Y Axis",
            description = "Swap up/down movement direction",
            checked = uiState.invertY,
            onCheckedChange = { onUpdate(uiState.copy(invertY = it)) }
        )

        SettingsSwitch(
            title = "Smoothing",
            description = "Smooth cursor movement for better control",
            checked = uiState.smoothingEnabled,
            onCheckedChange = { onUpdate(uiState.copy(smoothingEnabled = it)) }
        )
    }
}

// ==================== GESTURE SETTINGS ====================

@Composable
fun GestureSettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Click Threshold",
            value = uiState.clickThreshold,
            onValueChange = { onUpdate(uiState.copy(clickThreshold = it)) },
            valueRange = 1f..20f,
            steps = 19,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for click detection"
        )

        SettingsSlider(
            title = "Double Click Interval",
            value = uiState.doubleClickInterval.toFloat(),
            onValueChange = { onUpdate(uiState.copy(doubleClickInterval = it.toLong())) },
            valueRange = 100f..800f,
            steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Max time between clicks for double click"
        )

        SettingsSlider(
            title = "Scroll Threshold",
            value = uiState.scrollThreshold,
            onValueChange = { onUpdate(uiState.copy(scrollThreshold = it)) },
            valueRange = 1f..15f,
            steps = 14,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for scroll detection"
        )

        SettingsSlider(
            title = "Right Click Tilt",
            value = uiState.rightClickTilt,
            onValueChange = { onUpdate(uiState.copy(rightClickTilt = it)) },
            valueRange = 5f..45f,
            steps = 8,
            formatValue = { "${it.toInt()}°" },
            description = "Tilt angle to trigger right click"
        )

        SettingsSlider(
            title = "Right Click Duration",
            value = uiState.rightClickDuration.toFloat(),
            onValueChange = { onUpdate(uiState.copy(rightClickDuration = it.toLong())) },
            valueRange = 100f..1000f,
            steps = 9,
            formatValue = { "${it.toInt()}ms" },
            description = "How long to hold tilt for right click"
        )
    }
}

// ==================== AI SETTINGS ====================

@Composable
fun AISettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "AI Smoothing",
            description = "Use AI to smooth cursor movement",
            checked = uiState.aiSmoothing,
            onCheckedChange = { onUpdate(uiState.copy(aiSmoothing = it)) }
        )

        SettingsSlider(
            title = "AI Blend Factor",
            value = uiState.aiBlendFactor,
            onValueChange = { onUpdate(uiState.copy(aiBlendFactor = it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Balance between raw and AI-smoothed movement"
        )

        SettingsSwitch(
            title = "Predictive Movement",
            description = "Predict future cursor position",
            checked = uiState.predictive,
            onCheckedChange = { onUpdate(uiState.copy(predictive = it)) }
        )

        SettingsSlider(
            title = "Prediction Strength",
            value = uiState.predictionStrength,
            onValueChange = { onUpdate(uiState.copy(predictionStrength = it)) },
            valueRange = 0f..1f,
            steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "How much prediction to apply"
        )

        SettingsSwitch(
            title = "Kalman Filter",
            description = "Use Kalman filter for smoother tracking",
            checked = uiState.kalmanEnabled,
            onCheckedChange = { onUpdate(uiState.copy(kalmanEnabled = it)) }
        )
    }
}

// ==================== HAPTIC SETTINGS ====================

@Composable
fun HapticSettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibration on actions",
            checked = uiState.hapticEnabled,
            onCheckedChange = { onUpdate(uiState.copy(hapticEnabled = it)) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Haptic Strength",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Intensity of vibration feedback",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = uiState.hapticStrength == strength,
                            onClick = { onUpdate(uiState.copy(hapticStrength = strength)) },
                            label = { Text(strength.displayName) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        SettingsSwitch(
            title = "Sound Feedback",
            description = "Play sounds on actions",
            checked = uiState.soundEnabled,
            onCheckedChange = { onUpdate(uiState.copy(soundEnabled = it)) }
        )

        SettingsSwitch(
            title = "Visual Feedback",
            description = "Show visual indicators on actions",
            checked = uiState.visualFeedback,
            onCheckedChange = { onUpdate(uiState.copy(visualFeedback = it)) }
        )

        SettingsSwitch(
            title = "Notification on Gesture",
            description = "Show notification when gesture is detected",
            checked = uiState.notificationOnGesture,
            onCheckedChange = { onUpdate(uiState.copy(notificationOnGesture = it)) }
        )
    }
}

// ==================== DISPLAY SETTINGS ====================

@Composable
fun DisplaySettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Choose app color scheme",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("system", "light", "dark", "pure_black").forEach { theme ->
                        FilterChip(
                            selected = uiState.theme == theme,
                            onClick = { onUpdate(uiState.copy(theme = theme)) },
                            label = {
                                Text(
                                    when (theme) {
                                        "system" -> "System"
                                        "light" -> "Light"
                                        "dark" -> "Dark"
                                        "pure_black" -> "Pure Black"
                                        else -> theme
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }

        SettingsSwitch(
            title = "Dynamic Colors",
            description = "Use Material You color scheme",
            checked = uiState.useDynamicColors,
            onCheckedChange = { onUpdate(uiState.copy(useDynamicColors = it)) }
        )

        SettingsSlider(
            title = "Font Size",
            value = uiState.fontSize,
            onValueChange = { onUpdate(uiState.copy(fontSize = it)) },
            valueRange = 12f..24f,
            steps = 6,
            formatValue = { "${it.toInt()}sp" },
            description = "Base font size for the app"
        )

        SettingsSwitch(
            title = "Show Debug Info",
            description = "Display debug information",
            checked = uiState.showDebugInfo,
            onCheckedChange = { onUpdate(uiState.copy(showDebugInfo = it)) }
        )

        SettingsSwitch(
            title = "Keep Screen On",
            description = "Prevent screen from turning off",
            checked = uiState.keepScreenOn,
            onCheckedChange = { onUpdate(uiState.copy(keepScreenOn = it)) }
        )

        SettingsSwitch(
            title = "Show FPS",
            description = "Display frames per second counter",
            checked = uiState.showFps,
            onCheckedChange = { onUpdate(uiState.copy(showFps = it)) }
        )
    }
}

// ==================== CONNECTION SETTINGS ====================

@Composable
fun ConnectionSettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Auto Connect",
            description = "Auto-connect on app start",
            checked = uiState.autoConnect,
            onCheckedChange = { onUpdate(uiState.copy(autoConnect = it)) }
        )

        SettingsSwitch(
            title = "Use WebSocket",
            description = "Use WebSocket protocol (fallback to TCP)",
            checked = uiState.useWebSocket,
            onCheckedChange = { onUpdate(uiState.copy(useWebSocket = it)) }
        )

        SettingsSwitch(
            title = "UDP Discovery",
            description = "Auto-discover servers on network",
            checked = uiState.useUdpDiscovery,
            onCheckedChange = { onUpdate(uiState.copy(useUdpDiscovery = it)) }
        )

        SettingsSlider(
            title = "Reconnect Attempts",
            value = uiState.reconnectAttempts.toFloat(),
            onValueChange = { onUpdate(uiState.copy(reconnectAttempts = it.toInt())) },
            valueRange = 1f..20f,
            steps = 19,
            formatValue = { "${it.toInt()}" },
            description = "Number of reconnection attempts"
        )

        SettingsSlider(
            title = "Connection Timeout",
            value = uiState.connectionTimeout.toFloat(),
            onValueChange = { onUpdate(uiState.copy(connectionTimeout = it.toInt())) },
            valueRange = 1000f..15000f,
            steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Timeout for connection attempts"
        )
    }
}

// ==================== PRIVACY SETTINGS ====================

@Composable
fun PrivacySettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Anonymous Statistics",
            description = "Help improve Air Mouse by sending anonymous usage data",
            checked = uiState.anonymousStats,
            onCheckedChange = { onUpdate(uiState.copy(anonymousStats = it)) }
        )

        SettingsSwitch(
            title = "Crash Reporting",
            description = "Automatically report crashes to help fix issues",
            checked = uiState.crashReporting,
            onCheckedChange = { onUpdate(uiState.copy(crashReporting = it)) }
        )

        SettingsSwitch(
            title = "Clear Data on Exit",
            description = "Clear all app data when you exit",
            checked = uiState.clearDataOnExit,
            onCheckedChange = { onUpdate(uiState.copy(clearDataOnExit = it)) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Clear cache */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(
                        onClick = { /* Export data */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export Data")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Delete all data */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All Data", color = Color.White)
                }
            }
        }
    }
}

// ==================== PRESENTATION SETTINGS ====================

@Composable
fun PresentationSettings(uiState: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Presentation Mode",
            description = "Enable presentation controls",
            checked = uiState.presentationModeEnabled,
            onCheckedChange = { onUpdate(uiState.copy(presentationModeEnabled = it)) }
        )

        SettingsSlider(
            title = "Laser Pointer Speed",
            value = uiState.laserPointerSpeed,
            onValueChange = { onUpdate(uiState.copy(laserPointerSpeed = it)) },
            valueRange = 0.1f..2.0f,
            steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Speed of laser pointer movement"
        )

        SettingsSwitch(
            title = "Show Presentation Timer",
            description = "Display timer during presentations",
            checked = uiState.showPresentationTimer,
            onCheckedChange = { onUpdate(uiState.copy(showPresentationTimer = it)) }
        )

        SettingsSwitch(
            title = "Auto-Hide Laser",
            description = "Hide laser pointer when not in use",
            checked = uiState.autoHideLaser,
            onCheckedChange = { onUpdate(uiState.copy(autoHideLaser = it)) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Start presentation */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                    Button(
                        onClick = { /* Stop presentation */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }
        }
    }
}

// ==================== ABOUT SECTION ====================

@Composable
fun AboutSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Air Mouse Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Version 3.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Turn your phone into a wireless mouse",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏛️ University of Tehran", style = MaterialTheme.typography.bodyMedium)
                Text("📱 Built with Kotlin & Compose", style = MaterialTheme.typography.bodySmall)
                Text("🔗 github.com/airmouse", style = MaterialTheme.typography.bodySmall)
                Text("© 2025 Air Mouse Team", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Open license */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Source Licenses")
            }
        }
    }
}