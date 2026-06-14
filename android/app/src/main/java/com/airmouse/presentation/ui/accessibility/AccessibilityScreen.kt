package com.airmouse.presentation.ui.accessibility

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    navigationActions: NavigationActions,
    viewModel: AccessibilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(AccessibilityCategory.DISPLAY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Accessibility",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
                    }
                    IconButton(onClick = { viewModel.openAccessibilityHelp() }) {
                        Icon(Icons.Default.Help, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Category Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    AccessibilityCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        LeadingIconTab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = { Text(category.title, fontSize = 13.sp) },
                            icon = {
                                Icon(
                                    category.icon,
                                    contentDescription = category.title,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected category
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedCategory) {
                        AccessibilityCategory.DISPLAY -> displaySettings(uiState, viewModel)
                        AccessibilityCategory.FEEDBACK -> feedbackSettings(uiState, viewModel)
                        AccessibilityCategory.GESTURE -> gestureSettings(uiState, viewModel)
                        AccessibilityCategory.VOICE -> voiceSettings(uiState, viewModel)
                        AccessibilityCategory.ADVANCED -> advancedSettings(uiState, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyListScope.displaySettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🖼️ Display Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedSwitch(
                    checked = uiState.highContrast,
                    onCheckedChange = viewModel::setHighContrast,
                    label = "High Contrast Mode",
                    description = "Increase contrast for better visibility",
                    icon = Icons.Default.Contrast
                )
                
                AnimatedSwitch(
                    checked = uiState.largeText,
                    onCheckedChange = viewModel::setLargeText,
                    label = "Large Text",
                    description = "Increase text size throughout the app",
                    icon = Icons.Default.TextFields
                )
                
                AnimatedSwitch(
                    checked = uiState.reduceMotion,
                    onCheckedChange = viewModel::setReduceMotion,
                    label = "Reduce Motion",
                    description = "Minimize animations and transitions",
                    icon = Icons.Default.Animation
                )
                
                AnimatedSwitch(
                    checked = uiState.darkMode,
                    onCheckedChange = viewModel::setDarkMode,
                    label = "Dark Mode",
                    description = "Use dark theme for reduced eye strain",
                    icon = Icons.Default.DarkMode
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Font Size Slider
                Text("Custom Font Size", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Adjust text size to your preference", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("A", fontSize = 12.sp)
                    Text("${uiState.customFontSize.toInt()}sp", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("A", fontSize = 20.sp)
                }
                
                Slider(
                    value = uiState.customFontSize,
                    onValueChange = viewModel::setCustomFontSize,
                    valueRange = 12f..24f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
    
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🎨 Color Vision Deficiency",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                ColorBlindModeSelector(
                    selectedMode = uiState.colorBlindMode,
                    onModeSelected = viewModel::setColorBlindMode
                )
            }
        }
    }
}

@Composable
private fun LazyListScope.feedbackSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🔊 Haptic & Sound",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedSwitch(
                    checked = uiState.hapticFeedback,
                    onCheckedChange = viewModel::setHapticFeedback,
                    label = "Haptic Feedback",
                    description = "Vibration on gestures and actions",
                    icon = Icons.Default.Vibration
                )
                
                if (uiState.hapticFeedback) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Haptic Intensity", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HapticIntensity.values().forEach { intensity ->
                                FilterChip(
                                    selected = uiState.hapticIntensity == intensity,
                                    onClick = { viewModel.setHapticIntensity(intensity) },
                                    label = { Text(intensity.name.lowercase(), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                AnimatedSwitch(
                    checked = uiState.soundFeedback,
                    onCheckedChange = viewModel::setSoundFeedback,
                    label = "Sound Feedback",
                    description = "Audio cues for actions (click, connect, etc.)",
                    icon = Icons.Default.VolumeUp
                )
                
                AnimatedSwitch(
                    checked = uiState.voiceFeedback,
                    onCheckedChange = viewModel::setVoiceFeedback,
                    label = "Voice Feedback",
                    description = "Spoken announcements for actions",
                    icon = Icons.Default.RecordVoiceOver
                )
            }
        }
    }
}

@Composable
private fun LazyListScope.gestureSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "✋ Gesture Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedSwitch(
                    checked = uiState.simplifiedGestures,
                    onCheckedChange = viewModel::setSimplifiedGestures,
                    label = "Simplified Gestures",
                    description = "Easier gesture detection with lower sensitivity",
                    icon = Icons.Default.Gesture
                )
                
                AnimatedSwitch(
                    checked = uiState.screenReader,
                    onCheckedChange = viewModel::setScreenReader,
                    label = "Screen Reader Support",
                    description = "Enhanced TalkBack compatibility",
                    icon = Icons.Default.Accessibility
                )
                
                AnimatedSwitch(
                    checked = uiState.announceMovement,
                    onCheckedChange = viewModel::setAnnounceMovement,
                    label = "Announce Movement",
                    description = "TalkBack will read cursor movement aloud",
                    icon = Icons.Default.Mouse
                )
                
                AnimatedSwitch(
                    checked = uiState.announceClicks,
                    onCheckedChange = viewModel::setAnnounceClicks,
                    label = "Announce Clicks",
                    description = "TalkBack will announce click actions",
                    icon = Icons.Default.TouchApp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Gesture Sensitivity Slider
                Text("Gesture Sensitivity", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Adjust how sensitive gesture detection is", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Low", fontSize = 12.sp)
                    Text("Normal", fontSize = 12.sp)
                    Text("High", fontSize = 12.sp)
                }
                
                Slider(
                    value = uiState.gestureSensitivity,
                    onValueChange = viewModel::setGestureSensitivity,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun LazyListScope.voiceSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "🎤 Voice Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedSwitch(
                    checked = uiState.voiceWakeWord,
                    onCheckedChange = viewModel::setVoiceWakeWord,
                    label = "Wake Word Detection",
                    description = "Say 'Hey Air Mouse' to activate voice commands",
                    icon = Icons.Default.Mic
                )
                
                if (uiState.voiceWakeWord) {
                    OutlinedTextField(
                        value = uiState.wakeWord,
                        onValueChange = viewModel::setWakeWord,
                        label = { Text("Custom Wake Word") },
                        placeholder = { Text("Hey Air Mouse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                AnimatedSwitch(
                    checked = uiState.voiceConfirmation,
                    onCheckedChange = viewModel::setVoiceConfirmation,
                    label = "Voice Confirmation",
                    description = "Get verbal confirmation when commands are executed",
                    icon = Icons.Default.CheckCircle
                )
                
                AnimatedSwitch(
                    checked = uiState.voiceContinuousListening,
                    onCheckedChange = viewModel::setVoiceContinuousListening,
                    label = "Continuous Listening",
                    description = "Keep listening for commands after wake word",
                    icon = Icons.Default.Repeat
                )
            }
        }
    }
}

@Composable
private fun LazyListScope.advancedSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "⚙️ Advanced Accessibility",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedSwitch(
                    checked = uiState.switchAccess,
                    onCheckedChange = viewModel::setSwitchAccess,
                    label = "Switch Access",
                    description = "Use external switches for control",
                    icon = Icons.Default.SettingsRemote
                )
                
                AnimatedSwitch(
                    checked = uiState.dwellClick,
                    onCheckedChange = viewModel::setDwellClick,
                    label = "Dwell Click",
                    description = "Auto-click after cursor stops moving",
                    icon = Icons.Default.Timer
                )
                
                if (uiState.dwellClick) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dwell Time", fontSize = 13.sp)
                        Text("${uiState.dwellTime}ms", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = uiState.dwellTime.toFloat(),
                        onValueChange = { viewModel::setDwellTime(it.toInt()) },
                        valueRange = 500f..3000f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                AnimatedSwitch(
                    checked = uiState.audioCues,
                    onCheckedChange = viewModel::setAudioCues,
                    label = "Audio Cues",
                    description = "Sound effects for all interactions",
                    icon = Icons.Default.MusicNote
                )
                
                AnimatedSwitch(
                    checked = uiState.flashOnClick,
                    onCheckedChange = viewModel::setFlashOnClick,
                    label = "Flash on Click",
                    description = "Visual flash when clicking",
                    icon = Icons.Default.FlashOn
                )
            }
        }
    }
    
    item {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ℹ️ Accessibility Info",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Screen Reader Ready",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Air Mouse Pro is fully compatible with TalkBack and other screen readers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.openAccessibilityHelp() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Learn More About Accessibility")
                }
            }
        }
    }
}

@Composable
fun ColorBlindModeSelector(
    selectedMode: ColorBlindMode,
    onModeSelected: (ColorBlindMode) -> Unit
) {
    Column {
        Text("Color Blind Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text("Adjust colors for color vision deficiency", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorBlindMode.values().forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.displayName, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )
            }
        }
    }
}

enum class AccessibilityCategory(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DISPLAY("Display", Icons.Default.DisplaySettings),
    FEEDBACK("Feedback", Icons.Default.VolumeUp),
    GESTURE("Gesture", Icons.Default.Gesture),
    VOICE("Voice", Icons.Default.Mic),
    ADVANCED("Advanced", Icons.Default.Settings)
}

enum class ColorBlindMode(val displayName: String) {
    NONE("None"),
    PROTANOPIA("Protanopia (Red-Blind)"),
    DEUTERANOPIA("Deuteranopia (Green-Blind)"),
    TRITANOPIA("Tritanopia (Blue-Blind)")
}

enum class HapticIntensity {
    LIGHT, MEDIUM, STRONG
}