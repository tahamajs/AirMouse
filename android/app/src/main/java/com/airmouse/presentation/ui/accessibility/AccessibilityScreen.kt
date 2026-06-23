package com.airmouse.presentation.ui.accessibility

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    var selectedCategory by remember { mutableStateOf(AccessibilityCategory.DISPLAY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
                    }
                    IconButton(onClick = { viewModel.openAccessibilityHelp() }) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
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
                
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    AccessibilityCategory.entries.forEach { category ->
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

                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        when (selectedCategory) {
                            AccessibilityCategory.DISPLAY -> DisplaySettings(uiState, viewModel)
                            AccessibilityCategory.FEEDBACK -> FeedbackSettings(uiState, viewModel)
                            AccessibilityCategory.GESTURE -> GestureSettings(uiState, viewModel)
                            AccessibilityCategory.VOICE -> VoiceSettings(uiState, viewModel)
                            AccessibilityCategory.ADVANCED -> AdvancedSettings(uiState, viewModel)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(content = content)
            @Composable
            fun GlassCard(
                modifier: Modifier = Modifier,
                content: @Composable ColumnScope.() -> Unit
            ) {
                Card(
                    modifier = modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content
                    )
                }
            }

    }
}

@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}



@Composable
private fun DisplaySettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🖼️ Display Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedSwitch(
                    checked = uiState.highContrast,
                    onCheckedChange = viewModel::setHighContrast,
                    label = "High Contrast Mode",
                    description = "Increase contrast for better visibility"
                )

                AnimatedSwitch(
                    checked = uiState.largeText,
                    onCheckedChange = viewModel::setLargeText,
                    label = "Large Text",
                    description = "Increase text size throughout the app"
                )

                AnimatedSwitch(
                    checked = uiState.reduceMotion,
                    onCheckedChange = viewModel::setReduceMotion,
                    label = "Reduce Motion",
                    description = "Minimize animations and transitions"
                )

                AnimatedSwitch(
                    checked = uiState.darkMode,
                    onCheckedChange = viewModel::setDarkMode,
                    label = "Dark Mode",
                    description = "Use dark theme for reduced eye strain"
                )

                Spacer(modifier = Modifier.height(12.dp))

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

        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("🎨 Color Vision Deficiency", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                ColorBlindModeSelector(
                    selectedMode = uiState.colorBlindMode,
                    onModeSelected = viewModel::setColorBlindMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🔊 Haptic & Sound", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedSwitch(
                checked = uiState.hapticFeedback,
                onCheckedChange = viewModel::setHapticFeedback,
                label = "Haptic Feedback",
                description = "Vibration on gestures and actions"
            )

            if (uiState.hapticFeedback) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Haptic Intensity", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HapticIntensity.entries.forEach { intensity ->
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
                description = "Audio cues for actions (click, connect, etc.)"
            )

            AnimatedSwitch(
                checked = uiState.voiceFeedback,
                onCheckedChange = viewModel::setVoiceFeedback,
                label = "Voice Feedback",
                description = "Spoken announcements for actions"
            )
        }
    }
}

@Composable
private fun GestureSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("✋ Gesture Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedSwitch(
                checked = uiState.simplifiedGestures,
                onCheckedChange = viewModel::setSimplifiedGestures,
                label = "Simplified Gestures",
                description = "Easier gesture detection with lower sensitivity"
            )

            AnimatedSwitch(
                checked = uiState.screenReader,
                onCheckedChange = viewModel::setScreenReader,
                label = "Screen Reader Support",
                description = "Enhanced TalkBack compatibility"
            )

            AnimatedSwitch(
                checked = uiState.announceMovement,
                onCheckedChange = viewModel::setAnnounceMovement,
                label = "Announce Movement",
                description = "TalkBack will read cursor movement aloud"
            )

            AnimatedSwitch(
                checked = uiState.announceClicks,
                onCheckedChange = viewModel::setAnnounceClicks,
                label = "Announce Clicks",
                description = "TalkBack will announce click actions"
            )

            Spacer(modifier = Modifier.height(12.dp))

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

@Composable
private fun VoiceSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    GlassCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🎤 Voice Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedSwitch(
                checked = uiState.voiceWakeWord,
                onCheckedChange = viewModel::setVoiceWakeWord,
                label = "Wake Word Detection",
                description = "Say 'Hey Air Mouse' to activate voice commands"
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            AnimatedSwitch(
                checked = uiState.voiceConfirmation,
                onCheckedChange = viewModel::setVoiceConfirmation,
                label = "Voice Confirmation",
                description = "Get verbal confirmation when commands are executed"
            )

            AnimatedSwitch(
                checked = uiState.voiceContinuousListening,
                onCheckedChange = viewModel::setVoiceContinuousListening,
                label = "Continuous Listening",
                description = "Keep listening for commands after wake word"
            )
        }
    }
}

@Composable
private fun AdvancedSettings(uiState: AccessibilityUiState, viewModel: AccessibilityViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("⚙️ Advanced Accessibility", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedSwitch(
                    checked = uiState.switchAccess,
                    onCheckedChange = viewModel::setSwitchAccess,
                    label = "Switch Access",
                    description = "Use external switches for control"
                )

                AnimatedSwitch(
                    checked = uiState.dwellClick,
                    onCheckedChange = viewModel::setDwellClick,
                    label = "Dwell Click",
                    description = "Auto-click after cursor stops moving"
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
                        onValueChange = { viewModel.setDwellTime(it.toInt()) },
                        valueRange = 500f..3000f,
                        steps = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                AnimatedSwitch(
                    checked = uiState.audioCues,
                    onCheckedChange = viewModel::setAudioCues,
                    label = "Audio Cues",
                    description = "Sound effects for all interactions"
                )

                AnimatedSwitch(
                    checked = uiState.flashOnClick,
                    onCheckedChange = viewModel::setFlashOnClick,
                    label = "Flash on Click",
                    description = "Visual flash when clicking"
                )
            }
        }

        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ℹ️ Accessibility Info", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                        Text("Screen Reader Ready", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Learn More About Accessibility")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            ColorBlindMode.entries.forEach { mode ->
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