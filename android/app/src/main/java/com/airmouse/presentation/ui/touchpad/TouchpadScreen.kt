// app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadScreen.kt
package com.airmouse.presentation.ui.touchpad

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    navigationActions: NavigationActions,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touchpad Mode") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
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
            // Touchpad Surface
            item {
                TouchpadSurface(
                    uiState = uiState,
                    onTouchEvent = { x, y, pointerCount, pointers, pressure ->
                        viewModel.processTouchEvent(x, y, pointerCount, pointers, pressure)
                    },
                    onTap = { viewModel.processTap(it.x, it.y) },
                    onLongPress = { viewModel.processLongPress() },
                    onGestureStart = { viewModel.resetGestureState() },
                    onGestureEnd = { viewModel.resetGestureState() }
                )
            }

            // Status Card
            item {
                StatusCard(uiState, viewModel::toggleTouchpad)
            }

            // Quick Presets
            item {
                PresetsCard(viewModel::applyPreset)
            }

            // Settings Cards
            item {
                ExpandableSettingsCard("Touch Scrolling", Icons.Default.SwapVert) {
                    SwitchSetting(
                        "Two‑Finger Scroll",
                        "Use two fingers to scroll",
                        uiState.twoFingerScroll,
                        viewModel::updateTwoFingerScroll
                    )
                    SwitchSetting(
                        "Natural Scrolling",
                        "Content follows finger direction",
                        uiState.naturalScrolling,
                        viewModel::updateNaturalScrolling
                    )
                    SliderSetting(
                        "Scroll Speed",
                        uiState.scrollSpeed,
                        viewModel::updateScrollSpeed,
                        0.5f..2.0f
                    ) { "${"%.1f".format(it)}x" }
                    SwitchSetting(
                        "Edge Scrolling",
                        "Scroll by touching screen edges",
                        uiState.edgeScrolling,
                        viewModel::updateEdgeScrolling
                    )
                    SwitchSetting(
                        "Scroll Inertia",
                        "Smooth scrolling after fingers lift",
                        uiState.scrollInertia,
                        viewModel::updateScrollInertia
                    )
                }
            }

            item {
                ExpandableSettingsCard("Cursor Control", Icons.Default.Mouse) {
                    SliderSetting(
                        "Cursor Sensitivity",
                        uiState.sensitivity,
                        viewModel::updateSensitivity,
                        0.5f..2.0f
                    ) { "${"%.1f".format(it)}x" }
                    SliderSetting(
                        "Cursor Speed",
                        uiState.cursorSpeed,
                        viewModel::updateCursorSpeed,
                        0.5f..2.0f
                    ) { "${"%.1f".format(it)}x" }
                    SliderSetting(
                        "Pointer Precision",
                        uiState.pointerSpeed.toFloat(),
                        { viewModel.updatePointerSpeed(it.toInt()) },
                        20f..100f
                    ) { "${it.toInt()}%" }
                    SwitchSetting(
                        "Mouse Acceleration",
                        "Speed‑dependent cursor movement",
                        uiState.accelerationEnabled,
                        viewModel::updateAcceleration
                    )
                    SwitchSetting(
                        "Invert Vertical",
                        "",
                        uiState.invertVertical,
                        viewModel::updateInvertVertical
                    )
                    SwitchSetting(
                        "Invert Horizontal",
                        "",
                        uiState.invertHorizontal,
                        viewModel::updateInvertHorizontal
                    )
                }
            }

            item {
                ExpandableSettingsCard("Gestures & Feedback", Icons.Default.TouchApp) {
                    SwitchSetting(
                        "Tap to Click",
                        "Single tap = left click",
                        uiState.tapToClick,
                        viewModel::updateTapToClick
                    )
                    SliderSetting(
                        "Double‑Tap Delay",
                        uiState.doubleTapDelay.toFloat(),
                        { viewModel.updateDoubleTapDelay(it.toInt()) },
                        100f..600f
                    ) { "${it.toInt()}ms" }
                    SwitchSetting(
                        "Three‑Finger Swipe",
                        "Navigate back/forward, volume",
                        uiState.threeFingerSwipe,
                        viewModel::updateThreeFingerSwipe
                    )
                    SwitchSetting(
                        "Pinch to Zoom",
                        "",
                        uiState.pinchToZoom,
                        viewModel::updatePinchToZoom
                    )
                    SwitchSetting(
                        "Rotate to Rotate",
                        "",
                        uiState.rotateToRotate,
                        viewModel::updateRotateToRotate
                    )
                    SwitchSetting(
                        "Haptic Feedback",
                        "Vibrate on touch",
                        uiState.hapticFeedback,
                        viewModel::updateHapticFeedback
                    )
                    SwitchSetting(
                        "Show Touch Points",
                        "Visual feedback on touchpad",
                        uiState.showTouchPoints,
                        viewModel::updateShowTouchPoints
                    )
                }
            }

            // Last gesture display
            if (uiState.lastGesture.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Gesture:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                uiState.lastGesture,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TouchpadSurface(
    uiState: TouchpadUiState,
    onTouchEvent: (x: Float, y: Float, pointerCount: Int, pointers: List<Pair<Float, Float>>, pressure: Float) -> Unit,
    onTap: (Offset) -> Unit,
    onLongPress: () -> Unit,
    onGestureStart: () -> Unit,
    onGestureEnd: () -> Unit
) {
    var touchPoints by remember { mutableStateOf<List<TouchPoint>>(emptyList()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset -> onTap(offset) },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.isEmpty()) continue

                        val pointers = changes.mapNotNull { change ->
                            if (change.pressed) change.position else null
                        }
                        if (pointers.isNotEmpty()) {
                            val first = pointers.first()
                            val allPointers = pointers.map { it.x to it.y }
                            onTouchEvent(
                                first.x, first.y,
                                pointers.size,
                                allPointers,
                                changes.firstOrNull()?.pressure ?: 1f
                            )
                        } else {
                            onGestureEnd()
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Draw touch points
            if (uiState.showTouchPoints) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.touchPoints.forEach { point ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.6f),
                            radius = 25f,
                            center = Offset(point.x, point.y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12f,
                            center = Offset(point.x, point.y)
                        )
                    }
                }
            }

            // Center instructions
            if (!uiState.isActive) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Touchpad",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Touchpad Inactive\nPress Start to enable",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.touchPoints.isEmpty()) {
                Text(
                    text = "Touchpad Active",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusCard(uiState: TouchpadUiState, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Touchpad Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (uiState.isActive) "Active - Controlling cursor" else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (uiState.isActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isActive) "Deactivate" else "Activate")
            }
        }
    }
}

@Composable
fun PresetsCard(onApplyPreset: (TouchpadMode) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TouchpadMode.values().forEach { mode ->
                    OutlinedButton(
                        onClick = { onApplyPreset(mode) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode.displayName, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title)
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (expanded) {
                Divider()
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SwitchSetting(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
    formatValue: (Float) -> String
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
    }
}