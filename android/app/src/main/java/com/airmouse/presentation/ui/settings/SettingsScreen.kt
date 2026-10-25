// app/src/main/java/com/airmouse/presentation/ui/settings/SettingsScreen.kt
package com.airmouse.presentation.ui.settings
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.navigation.NavigationActionsImpl
import com.airmouse.presentation.ui.components.NotificationBadge
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard

// ============================================================
// MAIN SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigationActions: NavigationActions = NavigationActionsImpl(rememberNavController()),
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = { navigationActions.navigateBack() }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    var selectedSection by remember { mutableStateOf<SettingsSection?>(null) }

    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is SettingsEffect.ShowToast -> {
                Toast.makeText(context, currentEffect.message, Toast.LENGTH_SHORT).show()
            }
            is SettingsEffect.NavigateBack -> onBack()
            is SettingsEffect.NavigateTo -> {
                navigationActions.navigateTo(currentEffect.route)
            }
            is SettingsEffect.OpenUrl -> {
                Toast.makeText(context, currentEffect.url, Toast.LENGTH_SHORT).show()
            }
            null -> { /* No effect */ }
        }
        if (effect != null) {
            viewModel.clearEffect()
        }
    }

    Scaffold(
        topBar = {
            if (selectedSection == null) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Access tuning, theme, and connection controls",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (uiState.isSaving) {
                            com.airmouse.presentation.ui.components.SweepGradientGlowRing(size = 24.dp, strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        PremiumDashboardBackground {
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
}

// ============================================================
// MAIN SCREEN COMPOSABLES
// ============================================================

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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsHeader(
                onSectionSelected = onSectionSelected,
                navigationActions = navigationActions
            )
        }
        item {
            SettingsOverviewCard(
                uiState = uiState,
                onSectionSelected = onSectionSelected,
                navigationActions = navigationActions
            )
        }
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
                onClick = { onSectionSelected(section) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun SettingsHeader(
    onSectionSelected: (SettingsSection) -> Unit,
    navigationActions: NavigationActions
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF6366F1))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Customize motion, pairing, theme, and feedback in one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha=0.6f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(
                    onClick = { onSectionSelected(SettingsSection.CURSOR) },
                    label = { Text("Motion") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { onSectionSelected(SettingsSection.CONNECTION) },
                    label = { Text("Connection") },
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = { navigationActions.navigateToThemes() },
                    label = { Text("Theme") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SettingsOverviewCard(
    uiState: SettingsUiState,
    onSectionSelected: (SettingsSection) -> Unit,
    navigationActions: NavigationActions
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Live overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Jump straight into the parts you change most, with current values visible at a glance.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha=0.6f)
            )
            HorizontalDivider(color = Color.White.copy(alpha=0.1f))
            OverviewRow(
                icon = Icons.Default.Speed,
                label = "Sensitivity",
                value = "${(uiState.sensitivity * 100).toInt()}%",
                onClick = { onSectionSelected(SettingsSection.CURSOR) }
            )
            OverviewRow(
                icon = Icons.Default.Bluetooth,
                label = "Bluetooth HID",
                value = if (uiState.connectionConfig.protocol == com.airmouse.domain.model.ConnectionProtocol.BLUETOOTH) "Enabled" else "Disabled",
                onClick = { onSectionSelected(SettingsSection.CONNECTION) }
            )
        }
    }
}

@Composable
fun OverviewRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(16.dp))
        Text(label, color = Color.White.copy(alpha=0.7f), modifier = Modifier.padding(start = 8.dp))
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun SettingsQuickActionsCard(
    navigationActions: NavigationActions,
    viewModel: SettingsViewModel
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.handleEvent(SettingsEvent.ResetDefaults) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha=0.1f), contentColor = Color.White)
                ) {
                    Text("Reset All")
                }
                Button(
                    onClick = { viewModel.handleEvent(SettingsEvent.SaveSettings) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                ) {
                    Text("Save Config")
                }
            }
        }
    }
}

// ============================================================
// SETTINGS CARD
// ============================================================

@Composable
fun SettingsCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha=0.05f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha=0.6f))
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = Color.White.copy(alpha=0.5f))
        }
    }
}

// ============================================================
// SECTION DETAIL SCREEN
// ============================================================

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
                        Text(section.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        PremiumDashboardBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(section.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Color.White)
                                Text(
                                    section.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            when (section) {
                SettingsSection.CURSOR -> item { CursorSettings(uiState, viewModel) }
                SettingsSection.GESTURE -> item { GestureSettings(uiState, viewModel) }
                SettingsSection.AI -> item { AISettings(uiState, viewModel) }
                SettingsSection.HAPTIC -> item { HapticSettings(uiState, viewModel) }
                SettingsSection.NOTIFICATIONS -> item { NotificationSettings(uiState, viewModel) }
                SettingsSection.APPEARANCE -> item { DisplaySettings(uiState, viewModel, navigationActions) }
                SettingsSection.TOUCHPAD -> item { TouchpadSettings(uiState, viewModel, navigationActions) }
                SettingsSection.CONNECTION -> item { ConnectionSettings(uiState, viewModel, navigationActions) }
                SettingsSection.PRIVACY -> item { PrivacySettings(uiState, viewModel) }
                SettingsSection.PRESENTATION -> item { PresentationSettings(uiState, viewModel) }
                SettingsSection.GAMING_MODE -> item { GamingModeSettings(uiState, viewModel) }
                SettingsSection.FILE_TRANSFER -> item { FileTransferSettings(uiState, viewModel) }
                SettingsSection.SCREEN_MIRRORING -> item { ScreenMirroringSettings(uiState, viewModel) }
                SettingsSection.ABOUT -> item { AboutSection(viewModel) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        }
    }
}

// ============================================================
// UI COMPONENTS
// ============================================================

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true
) {
    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.58f)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
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
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                Text(
                    formatValue(value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(bottom = 8.dp))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

// ============================================================
// CURSOR SETTINGS
// ============================================================

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

        // Smoothness Presets (Gaming, Office, Precision)
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Smoothness Presets",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Switch algorithm ballistics for optimal response",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf("Gaming", "Office", "Precision")
                    presets.forEach { preset ->
                        val isSelected = uiState.smoothnessPreset == preset
                        Button(
                            onClick = { viewModel.handleEvent(SettingsEvent.ApplySmoothnessPreset(preset)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                contentColor = if (isSelected) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(preset, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1€ Filter sliders
        SettingsSlider(
            title = "OneEuro Min Cutoff (fc_min)",
            value = uiState.oneEuroMinCutoff,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateOneEuroMinCutoff(it)) },
            valueRange = 0.1f..3.0f, steps = 29,
            formatValue = { "%.2f Hz".format(it) },
            description = "Lower frequencies eliminate micro-jitter when hand is still"
        )
        SettingsSlider(
            title = "OneEuro Beta Speed Coefficient",
            value = uiState.oneEuroBeta,
            onValueChange = { viewModel.handleEvent(SettingsEvent.UpdateOneEuroBeta(it)) },
            valueRange = 0.001f..0.05f, steps = 49,
            formatValue = { "%.3f".format(it) },
            description = "Higher values drastically cut lag during quick mouse sweeps"
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

// ============================================================
// GESTURE SETTINGS
// ============================================================

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

// ============================================================
// AI SETTINGS
// ============================================================

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

// ============================================================
// HAPTIC SETTINGS
// ============================================================

@Composable
fun HapticSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Haptic Feedback",
            description = "Vibration on actions",
            checked = uiState.hapticEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleHaptic) }
        )
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Haptic Strength", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Intensity of vibration feedback", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HapticStrength.entries.forEach { strength ->
                        FilterChip(
                            selected = uiState.hapticStrength == strength,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateHapticStrength(strength)) },
                            label = { Text(strength.displayName, color = if (uiState.hapticStrength == strength) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                containerColor = Color.White.copy(alpha = 0.06f)
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

// ============================================================
// NOTIFICATION SETTINGS
// ============================================================

@Composable
fun NotificationSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(28.dp)
                        )
                        NotificationBadge(
                            count = if (uiState.notificationsEnabled) 1 else 0,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Control badges, alerts, and gesture notifications from one place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = uiState.notificationsEnabled,
                        onClick = { viewModel.handleEvent(SettingsEvent.ToggleNotifications) },
                        label = { Text("Enabled", color = if (uiState.notificationsEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            containerColor = Color.White.copy(alpha = 0.06f)
                        )
                    )
                    FilterChip(
                        selected = uiState.notificationOnGesture,
                        onClick = { viewModel.handleEvent(SettingsEvent.ToggleNotificationOnGesture) },
                        label = { Text("Gesture", color = if (uiState.notificationOnGesture) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            containerColor = Color.White.copy(alpha = 0.06f)
                        )
                    )
                    FilterChip(
                        selected = uiState.soundEnabled,
                        onClick = { viewModel.handleEvent(SettingsEvent.ToggleSound) },
                        label = { Text("Sound", color = if (uiState.soundEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            containerColor = Color.White.copy(alpha = 0.06f)
                        )
                    )
                    FilterChip(
                        selected = uiState.visualFeedback,
                        onClick = { viewModel.handleEvent(SettingsEvent.ToggleVisualFeedback) },
                        label = { Text("Visual", color = if (uiState.visualFeedback) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            containerColor = Color.White.copy(alpha = 0.06f)
                        )
                    )
                }
            }
        }

        SettingsSwitch(
            title = "App Notifications",
            description = "Show in-app notification badges and alerts",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleNotifications) }
        )
        SettingsSwitch(
            title = "Gesture Notifications",
            description = "Notify when a gesture is detected",
            checked = uiState.notificationOnGesture,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleNotificationOnGesture) },
            enabled = uiState.notificationsEnabled
        )
        SettingsSwitch(
            title = "Sound Alerts",
            description = "Play a sound for notification events",
            checked = uiState.soundEnabled,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleSound) },
            enabled = uiState.notificationsEnabled
        )
        SettingsSwitch(
            title = "Visual Feedback",
            description = "Show on-screen feedback for actions",
            checked = uiState.visualFeedback,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleVisualFeedback) },
            enabled = uiState.notificationsEnabled
        )
    }
}

// ============================================================
// DISPLAY SETTINGS
// ============================================================

@Composable
fun DisplaySettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Theme", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                Text("Choose app color scheme", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Quick themes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system", "light", "dark", "pure_black", "high_contrast").forEach { theme ->
                        FilterChip(
                            selected = uiState.theme == theme,
                            onClick = { viewModel.handleEvent(SettingsEvent.UpdateTheme(theme)) },
                            label = { Text(
                                when (theme) {
                                    "system" -> "System"; "light" -> "Light"; "dark" -> "Dark"
                                    "pure_black" -> "Pure Black"; "high_contrast" -> "High Contrast"
                                    else -> theme
                                },
                                color = if (uiState.theme == theme) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)
                            ) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                containerColor = Color.White.copy(alpha = 0.06f)
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
        SettingsFontSizeCard(
            currentSize = uiState.fontSize,
            onSelected = { viewModel.handleEvent(SettingsEvent.UpdateFontSize(it)) }
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
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD500F9).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFD500F9), modifier = Modifier.size(22.dp))
                }
                Column {
                    Text("Open full Themes studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Browse theme presets, previews, and accent colors in one place.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
            if (navigationActions != null) {
                Button(
                    onClick = { navigationActions.navigateToThemes() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Themes") }
            }
        }
    }
}

@Composable
fun SettingsFontSizeCard(
    currentSize: Float,
    onSelected: (Float) -> Unit
) {
    val selectedPreset = FontSizePreset.entries.minBy { kotlin.math.abs(it.valueSp - currentSize) }

    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Font Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Pick one of four app-wide text sizes.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FontSizePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = preset == selectedPreset,
                        onClick = { onSelected(preset.valueSp) },
                        label = { Text(preset.label, color = if (preset == selectedPreset) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            containerColor = Color.White.copy(alpha = 0.06f)
                        )
                    )
                }
            }
        }
    }
}

// ============================================================
// TOUCHPAD SETTINGS
// ============================================================

@Composable
fun TouchpadSettings(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    navigationActions: NavigationActions? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        SettingsSwitch(
            title = "Show Touch Points",
            description = "Display visible touch markers while using touchpad mode",
            checked = uiState.touchpadShowTouchPoints,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleTouchpadShowTouchPoints) }
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
    }
}

// ============================================================
// CONNECTION SETTINGS
// ============================================================

@Composable
fun ConnectionSettings(uiState: SettingsUiState, viewModel: SettingsViewModel, navigationActions: NavigationActions) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Auto Connect",
            description = "Auto-connect on app start",
            checked = uiState.autoConnect,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ToggleAutoConnect) }
        )
        SettingsSwitch(
            title = "Use WebSocket",
            description = "Use WebSocket protocol for live control messages",
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

        Spacer(modifier = Modifier.height(8.dp))
        GlassmorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navigationActions.navigateTo(com.airmouse.presentation.navigation.Destinations.BluetoothMouse.route) },
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text("Bluetooth Mouse peripheral", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Configure and connect as wireless mouse", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ============================================================
// PRIVACY SETTINGS
// ============================================================

@Composable
fun PrivacySettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.handleEvent(SettingsEvent.ImportSettingsFromUri(uri.toString()))
        }
    }

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
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.handleEvent(SettingsEvent.ClearCache) }, modifier = Modifier.weight(1f)) {
                        Text("Clear Cache")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                        Text("Import Data")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.handleEvent(SettingsEvent.ExportSettings) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Settings")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.OpenSystemSettings) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("System")
                    }
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(SettingsEvent.OpenAccessibilitySettings) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Accessibility")
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

// ============================================================
// PRESENTATION SETTINGS
// ============================================================

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
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!uiState.presentationModeEnabled) {
                                viewModel.handleEvent(SettingsEvent.TogglePresentationMode)
                            }
                        },
                        enabled = !uiState.presentationModeEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Enable")
                    }
                    Button(
                        onClick = {
                            if (uiState.presentationModeEnabled) {
                                viewModel.handleEvent(SettingsEvent.TogglePresentationMode)
                            }
                        },
                        enabled = uiState.presentationModeEnabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disable")
                    }
                }
            }
        }
    }
}

// ============================================================
// ABOUT SECTION
// ============================================================

@Composable
fun AboutSection(viewModel: SettingsViewModel) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Air Mouse Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Version 1.0.0 (Release v1.0.0-final)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Turn your phone into a wireless air mouse", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏛️ University of Tehran", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                Text("📱 Built with Kotlin & Compose", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                Text(
                    "🔗 github.com/tahamajs/AirMouse",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { viewModel.handleEvent(SettingsEvent.OpenGitHub) }
                )
                Text("© 2026 Air Mouse Team • MIT License", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.handleEvent(SettingsEvent.OpenLicense) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) {
                Text("Open Source Licenses")
            }
        }
    }
}
// ============================================================
// GAMING MODE SETTINGS
// ============================================================

@Composable
fun GamingModeSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Gaming Mode",
            description = "Boost sensor polling rate to prioritize ultra-low latency.",
            checked = false,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ShowToast("Gaming mode toggled.")) }
        )
        SettingsSwitch(
            title = "Hardware Acceleration",
            description = "Force hardware rendering for the connection stack to minimize CPU overhead.",
            checked = false,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ShowToast("Hardware acceleration toggled.")) }
        )
        SettingsSlider(
            title = "Polling Rate (Hz)",
            value = 120f,
            onValueChange = {},
            valueRange = 60f..240f,
            steps = 2,
            formatValue = { "${it.toInt()} Hz" },
            description = "Higher polling rate uses more battery but decreases input lag."
        )
    }
}

// ============================================================
// FILE TRANSFER SETTINGS
// ============================================================

@Composable
fun FileTransferSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Allow File Transfer",
            description = "Allow the desktop app to transfer files to your phone's storage.",
            checked = true,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ShowToast("File transfer settings updated.")) }
        )
        SettingsSwitch(
            title = "Auto-Accept Files",
            description = "Automatically accept small files without a confirmation prompt.",
            checked = false,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ShowToast("Auto-accept updated.")) }
        )
    }
}

// ============================================================
// SCREEN MIRRORING SETTINGS
// ============================================================

@Composable
fun ScreenMirroringSettings(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSwitch(
            title = "Enable Mirroring",
            description = "Stream your phone's screen to the desktop client.",
            checked = false,
            onCheckedChange = { viewModel.handleEvent(SettingsEvent.ShowToast("Screen mirroring toggled.")) }
        )
        SettingsSlider(
            title = "Mirror Quality",
            value = 0.5f,
            onValueChange = {},
            valueRange = 0f..1f,
            steps = 2,
            formatValue = { if (it < 0.3f) "Low" else if (it < 0.7f) "Medium" else "High" },
            description = "Higher quality requires more bandwidth."
        )
    }
}
