// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsScreen.kt
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    // Handle effects
    LaunchedEffect(effect) {
        val currentEffect = effect
        when (currentEffect) {
            is SettingsEffect.ShowToast -> {
                // Show toast - handled by scaffold
                viewModel.handleEvent(SettingsEvent.ShowToast(""))
            }
            is SettingsEffect.NavigateBack -> onBack()
            is SettingsEffect.NavigateTo -> {
                // Navigate to route
                navigationActions.navigateTo(currentEffect.route)
            }
            is SettingsEffect.OpenUrl -> {
                // Open URL
                viewModel.handleEvent(SettingsEvent.OpenWebsite)
            }
            null -> { /* No effect */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (selectedSection == null) {
            SettingsMainScreen(
                uiState = uiState,
                onSectionSelected = { selectedSection = it },
                navigationActions = navigationActions,
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            SectionDetailScreen(
                section = selectedSection!!,
                uiState = uiState,
                viewModel = viewModel,
                navigationActions = navigationActions,
                onBack = { selectedSection = null },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

// ==================== MAIN SCREEN ====================

@Composable
fun SettingsMainScreen(
    uiState: SettingsUiState,
    onSectionSelected: (SettingsSection) -> Unit,
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SettingsHeader() }
        item {
            SettingsQuickActionsCard(
                navigationActions = navigationActions,
                viewModel = viewModel
            )
        }
        items(SettingsSection.entries) { section ->
            SettingsCard(
                title = section.title,
                description = section.description,
                icon = section.icon,
                onClick = {
                    if (section == SettingsSection.THEMES) {
                        navigationActions.navigateToThemes()
                    } else {
                        onSectionSelected(section)
                    }
                }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ==================== HEADER ====================

@Composable
fun SettingsHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("⚙️ Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Customize your Air Mouse experience", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsQuickActionsCard(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Jump to the most useful pages and keep the setup flow smooth.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { navigationActions.navigateToTouchpad() }, modifier = Modifier.weight(1f)) {
                    Text("Touchpad")
                }
                OutlinedButton(onClick = { navigationActions.navigateToCalibration() }, modifier = Modifier.weight(1f)) {
                    Text("Calibrate")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { viewModel.handleEvent(SettingsEvent.ResetDefaults) }, modifier = Modifier.weight(1f)) {
                    Text("Reset")
                }
                Button(onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) }, modifier = Modifier.weight(1f)) {
                    Text("Save")
                }
            }
        }
    }
}

// ==================== SETTINGS CARD ====================

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Navigate")
        }
    }
}

// ==================== SECTION DETAIL SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(
    section: SettingsSection,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(section.icon, contentDescription = section.title, tint = MaterialTheme.colorScheme.primary)
                        Text(section.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                SettingsSection.CURSOR -> item { CursorSettings(uiState, viewModel) }
                SettingsSection.GESTURE -> item { GestureSettings(uiState, viewModel) }
                SettingsSection.AI -> item { AISettings(uiState, viewModel) }
                SettingsSection.HAPTIC -> item { HapticSettings(uiState, viewModel) }
                SettingsSection.DISPLAY -> item { DisplaySettings(uiState, viewModel, navigationActions) }
                SettingsSection.THEMES -> item { ThemesShortcutCard(navigationActions) }
                SettingsSection.TOUCHPAD -> item { TouchpadSettings(uiState, viewModel, navigationActions) }
                SettingsSection.CONNECTION -> item { ConnectionSettings(uiState, viewModel) }
                SettingsSection.PRIVACY -> item { PrivacySettings(uiState, viewModel) }
                SettingsSection.PRESENTATION -> item { PresentationSettings(uiState, viewModel) }
                SettingsSection.ABOUT -> item { AboutSection(viewModel) }
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
    description: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    formatValue: (Float) -> String = { "%.2f".format(it) },
    description: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ==================== CURSOR SETTINGS ====================

@Composable
fun CursorSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Sensitivity",
            value = uiState.sensitivity,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSensitivity(it)) },
            valueRange = 0.1f..2.0f, steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Higher values make cursor faster"
        )
        SettingsSlider(
            title = "Smoothing Factor",
            value = uiState.smoothingFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateSmoothingFactor(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Smooths cursor movement"
        )
        SettingsSwitch(
            title = "Acceleration",
            description = "Cursor speed increases with faster movement",
            checked = uiState.accelerationEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAcceleration) }
        )
        SettingsSwitch(
            title = "Invert X Axis",
            description = "Swap left/right movement direction",
            checked = uiState.invertX,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertX) }
        )
        SettingsSwitch(
            title = "Invert Y Axis",
            description = "Swap up/down movement direction",
            checked = uiState.invertY,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleInvertY) }
        )
        SettingsSwitch(
            title = "Smoothing",
            description = "Smooth cursor movement for better control",
            checked = uiState.smoothingEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSmoothing) }
        )
    }
}

// ==================== GESTURE SETTINGS ====================

@Composable
fun GestureSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSlider(
            title = "Click Threshold",
            value = uiState.clickThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateClickThreshold(it)) },
            valueRange = 1f..20f, steps = 19,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for click detection"
        )
        SettingsSlider(
            title = "Double Click Interval",
            value = uiState.doubleClickInterval.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateDoubleClickInterval(it.toLong())) },
            valueRange = 100f..800f, steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Max time between clicks for double click"
        )
        SettingsSlider(
            title = "Scroll Threshold",
            value = uiState.scrollThreshold,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateScrollThreshold(it)) },
            valueRange = 1f..15f, steps = 14,
            formatValue = { "%.1f".format(it) },
            description = "Sensitivity for scroll detection"
        )
        SettingsSlider(
            title = "Right Click Tilt",
            value = uiState.rightClickTilt,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickTilt(it)) },
            valueRange = 5f..45f, steps = 8,
            formatValue = { "${it.toInt()}°" },
            description = "Tilt angle to trigger right click"
        )
        SettingsSlider(
            title = "Right Click Duration",
            value = uiState.rightClickDuration.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateRightClickDuration(it.toLong())) },
            valueRange = 100f..1000f, steps = 9,
            formatValue = { "${it.toInt()}ms" },
            description = "How long to hold tilt for right click"
        )
    }
}

// ==================== AI SETTINGS ====================

@Composable
fun AISettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "AI Smoothing",
            description = "Use AI to smooth cursor movement",
            checked = uiState.aiSmoothing,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAiSmoothing) }
        )
        SettingsSlider(
            title = "AI Blend Factor",
            value = uiState.aiBlendFactor,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateAiBlendFactor(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "Balance between raw and AI-smoothed movement"
        )
        SettingsSwitch(
            title = "Predictive Movement",
            description = "Predict future cursor position",
            checked = uiState.predictive,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.TogglePredictive) }
        )
        SettingsSlider(
            title = "Prediction Strength",
            value = uiState.predictionStrength,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdatePredictionStrength(it)) },
            valueRange = 0f..1f, steps = 10,
            formatValue = { "%.0f%%".format(it * 100) },
            description = "How much prediction to apply"
        )
        SettingsSwitch(
            title = "Kalman Filter",
            description = "Use Kalman filter for smoother tracking",
            checked = uiState.kalmanEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKalman) }
        )
    }
}

// ==================== HAPTIC SETTINGS ====================

@Composable
fun HapticSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibration on actions",
            checked = uiState.hapticEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Haptic Strength", style = MaterialTheme.typography.bodyLarge)
                Text("Intensity of vibration feedback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = uiState.hapticStrength == strength,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateHapticStrength(strength)) },
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
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSound) }
        )
        SettingsSwitch(
            title = "Visual Feedback",
            description = "Show visual indicators on actions",
            checked = uiState.visualFeedback,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleVisualFeedback) }
        )
        SettingsSwitch(
            title = "Notification on Gesture",
            description = "Show notification when gesture is detected",
            checked = uiState.notificationOnGesture,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleNotificationOnGesture) }
        )
    }
}

// ==================== DISPLAY SETTINGS ====================

@Composable
fun DisplaySettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                Text("Choose app color scheme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Quick themes", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "light", "dark", "pure_black", "high_contrast").forEach { theme ->
                        FilterChip(
                            selected = uiState.theme == theme,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateTheme(theme)) },
                            label = { Text(when (theme) {
                                "system" -> "System"; "light" -> "Light"; "dark" -> "Dark"
                                "pure_black" -> "Pure Black"; "high_contrast" -> "High Contrast"
                                else -> theme }) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
        ThemesShortcutCard(navigationActions)
        SettingsSwitch(
            title = "Dynamic Colors",
            description = "Use Material You color scheme",
            checked = uiState.useDynamicColors,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDynamicColors) }
        )
        SettingsSlider(
            title = "Font Size",
            value = uiState.fontSize,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateFontSize(it)) },
            valueRange = 12f..24f, steps = 6,
            formatValue = { "${it.toInt()}sp" },
            description = "Base font size for the app"
        )
        SettingsSwitch(
            title = "Show Debug Info",
            description = "Display debug information",
            checked = uiState.showDebugInfo,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleDebugInfo) }
        )
        SettingsSwitch(
            title = "Keep Screen On",
            description = "Prevent screen from turning off",
            checked = uiState.keepScreenOn,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleKeepScreenOn) }
        )
        SettingsSwitch(
            title = "Show FPS",
            description = "Display frames per second counter",
            checked = uiState.showFps,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleShowFps) }
        )
    }
}

@Composable
fun ThemesShortcutCard(navigationActions: NavigationActions? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Open full Themes studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Browse theme presets, previews, and accent colors in one place.", style = MaterialTheme.typography.bodySmall)
            if (navigationActions != null) {
                Button(onClick = { navigationActions.navigateToThemes() }) { Text("Open Themes") }
            }
        }
    }
}

// ==================== TOUCHPAD SETTINGS ====================

@Composable
fun TouchpadSettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Touchpad Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tune touchpad mode, click behavior, scroll behavior, and visual feedback.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navigationActions?.navigateToTouchpad() }) { Text("Open Touchpad") }
                    OutlinedButton(onClick = { navigationActions?.navigateToTouchpadSettings() }) { Text("Touchpad Page") }
                }
            }
        }

        SettingsSwitch(
            title = "Touchpad Active",
            description = "Enable touchpad mode on the phone",
            checked = uiState.touchpadActive,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadActive) }
        )
        SettingsSlider(
            title = "Touchpad Sensitivity",
            value = uiState.touchpadSensitivity,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadSensitivity(it)) },
            valueRange = 0.5f..2.0f,
            steps = 15,
            formatValue = { "%.1fx".format(it) },
            description = "Overall response of touchpad movement"
        )
        SettingsSlider(
            title = "Cursor Speed",
            value = uiState.touchpadCursorSpeed,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadCursorSpeed(it)) },
            valueRange = 0.5f..2.0f,
            steps = 15,
            formatValue = { "%.1fx".format(it) },
            description = "How fast the pointer moves"
        )
        SettingsSlider(
            title = "Pointer Precision",
            value = uiState.touchpadPointerSpeed.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadPointerSpeed(it.toInt())) },
            valueRange = 20f..100f,
            steps = 8,
            formatValue = { "${it.toInt()}%" },
            description = "Trade speed for finer control"
        )
        SettingsSwitch(
            title = "Acceleration",
            description = "Increase cursor response for fast swipes",
            checked = uiState.touchpadAccelerationEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadAcceleration) }
        )
        SettingsSwitch(
            title = "Invert Vertical",
            description = "Flip up/down movement",
            checked = uiState.touchpadInvertVertical,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadInvertVertical) }
        )
        SettingsSwitch(
            title = "Invert Horizontal",
            description = "Flip left/right movement",
            checked = uiState.touchpadInvertHorizontal,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadInvertHorizontal) }
        )
        SettingsSlider(
            title = "Scroll Speed",
            value = uiState.touchpadScrollSpeed,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadScrollSpeed(it)) },
            valueRange = 0.5f..2.0f,
            steps = 15,
            formatValue = { "%.1fx".format(it) },
            description = "Scroll distance per gesture"
        )
        SettingsSwitch(
            title = "Natural Scrolling",
            description = "Content follows finger direction",
            checked = uiState.touchpadNaturalScrolling,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadNaturalScrolling) }
        )
        SettingsSwitch(
            title = "Two-Finger Scroll",
            description = "Use two fingers to scroll on touchpad",
            checked = uiState.touchpadTwoFingerScroll,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadTwoFingerScroll) }
        )
        SettingsSwitch(
            title = "Edge Scrolling",
            description = "Scroll when dragging near the edge",
            checked = uiState.touchpadEdgeScrolling,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadEdgeScrolling) }
        )
        SettingsSwitch(
            title = "Scroll Inertia",
            description = "Smooth the end of scrolling gestures",
            checked = uiState.touchpadScrollInertia,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadScrollInertia) }
        )
        SettingsSwitch(
            title = "Tap to Click",
            description = "Single tap sends left click",
            checked = uiState.touchpadTapToClick,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadTapToClick) }
        )
        SettingsSlider(
            title = "Double Tap Delay",
            value = uiState.touchpadDoubleTapDelay.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateTouchpadDoubleTapDelay(it.toInt())) },
            valueRange = 100f..600f,
            steps = 10,
            formatValue = { "${it.toInt()}ms" },
            description = "Maximum delay between taps"
        )
        SettingsSwitch(
            title = "Three-Finger Swipe",
            description = "Use three-finger swipe shortcuts",
            checked = uiState.touchpadThreeFingerSwipe,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadThreeFingerSwipe) }
        )
        SettingsSwitch(
            title = "Pinch to Zoom",
            description = "Allow pinch-to-zoom gestures",
            checked = uiState.touchpadPinchToZoom,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadPinchToZoom) }
        )
        SettingsSwitch(
            title = "Rotate to Rotate",
            description = "Rotate gestures map to rotate actions",
            checked = uiState.touchpadRotateToRotate,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadRotateToRotate) }
        )
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibrate on touchpad actions",
            checked = uiState.touchpadHapticFeedback,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadHapticFeedback) }
        )
        SettingsSwitch(
            title = "Show Touch Points",
            description = "Display contact points on the touchpad",
            checked = uiState.touchpadShowTouchPoints,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadShowTouchPoints) }
        )
    }
}

// ==================== CONNECTION SETTINGS ====================

@Composable
fun ConnectionSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Auto Connect",
            description = "Auto-connect on app start",
            checked = uiState.autoConnect,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoConnect) }
        )
        SettingsSwitch(
            title = "Use WebSocket",
            description = "Use WebSocket protocol (fallback to TCP)",
            checked = uiState.useWebSocket,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUseWebSocket) }
        )
        SettingsSwitch(
            title = "UDP Discovery",
            description = "Auto-discover servers on network",
            checked = uiState.useUdpDiscovery,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleUdpDiscovery) }
        )
        SettingsSlider(
            title = "Reconnect Attempts",
            value = uiState.reconnectAttempts.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateReconnectAttempts(it.toInt())) },
            valueRange = 1f..20f, steps = 19,
            formatValue = { "${it.toInt()}" },
            description = "Number of reconnection attempts"
        )
        SettingsSlider(
            title = "Connection Timeout",
            value = uiState.connectionTimeout.toFloat(),
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateConnectionTimeout(it.toInt())) },
            valueRange = 1000f..15000f, steps = 14,
            formatValue = { "${it.toInt()}ms" },
            description = "Timeout for connection attempts"
        )
    }
}

// ==================== PRIVACY SETTINGS ====================

@Composable
fun PrivacySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Anonymous Statistics",
            description = "Help improve Air Mouse by sending anonymous usage data",
            checked = uiState.anonymousStats,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAnonymousStats) }
        )
        SettingsSwitch(
            title = "Crash Reporting",
            description = "Automatically report crashes to help fix issues",
            checked = uiState.crashReporting,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleCrashReporting) }
        )
        SettingsSwitch(
            title = "Clear Data on Exit",
            description = "Clear all app data when you exit",
            checked = uiState.clearDataOnExit,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleClearDataOnExit) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.handleEvent(SettingsEvent.ClearCache) }, modifier = Modifier.weight(1f)) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(onClick = { viewModel.handleEvent(SettingsEvent.ExportSettings) }, modifier = Modifier.weight(1f)) {
                        Text("Export Data")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.handleEvent(SettingsEvent.ClearAllData) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Data", color = Color.White)
                }
            }
        }
    }
}

// ==================== PRESENTATION SETTINGS ====================

@Composable
fun PresentationSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Presentation Mode",
            description = "Enable presentation controls",
            checked = uiState.presentationModeEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.TogglePresentationMode) }
        )
        SettingsSlider(
            title = "Laser Pointer Speed",
            value = uiState.laserPointerSpeed,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateLaserPointerSpeed(it)) },
            valueRange = 0.1f..2.0f, steps = 19,
            formatValue = { "%.1fx".format(it) },
            description = "Speed of laser pointer movement"
        )
        SettingsSwitch(
            title = "Show Presentation Timer",
            description = "Display timer during presentations",
            checked = uiState.showPresentationTimer,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleShowPresentationTimer) }
        )
        SettingsSwitch(
            title = "Auto-Hide Laser",
            description = "Hide laser pointer when not in use",
            checked = uiState.autoHideLaser,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoHideLaser) }
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { /* Start presentation */ }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start")
                    }
                    Button(
                        onClick = { /* Stop presentation */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
fun AboutSection(viewModel: SettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Air Mouse Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Version 3.0.0", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Turn your phone into a wireless mouse", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(
                    "🔗 github.com/airmouse",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { viewModel.handleEvent(SettingsEvent.OpenGitHub) }
                )
                Text("© 2025 Air Mouse Team", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.handleEvent(SettingsEvent.OpenLicense) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Source Licenses")
            }
        }
    }
}
