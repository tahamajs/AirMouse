package com.airmouse.presentation.ui.touchpad

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    navigationActions: NavigationActions,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Touchpad Mode") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item {
                TouchpadSurface(
                    uiState = uiState,
                    onTouchEvent = { x, y, count, pointers, p ->
                        viewModel.processTouchEvent(x, y, count, pointers, p)
                    },
                    onTap = { viewModel.processTap(it.x, it.y) },
                    onLongPress = { viewModel.processLongPress() },
                    onGestureEnd = { viewModel.resetGestureState() }
                )
            }

            item { StatusCard(uiState, viewModel::toggleTouchpad) }
            item { PresetsCard(viewModel::applyPreset) }

            item {
                ExpandableSettingsCard("Touch Scrolling", Icons.Default.SwapVert) {
                    SwitchSetting("Two‑Finger Scroll", "Use two fingers to scroll", uiState.twoFingerScroll, viewModel::updateTwoFingerScroll)
                    SwitchSetting("Natural Scrolling", "Content follows finger direction", uiState.naturalScrolling, viewModel::updateNaturalScrolling)
                    SliderSetting("Scroll Speed", uiState.scrollSpeed, viewModel::updateScrollSpeed, 0.5f..2.0f) { "${"%.1f".format(it)}x" }
                    SwitchSetting("Edge Scrolling", "Scroll by touching screen edges", uiState.edgeScrolling, viewModel::updateEdgeScrolling)
                    SwitchSetting("Scroll Inertia", "Smooth scrolling after fingers lift", uiState.scrollInertia, viewModel::updateScrollInertia)
                }
            }

            item {
                ExpandableSettingsCard("Cursor Control", Icons.Default.Mouse) {
                    SliderSetting("Cursor Sensitivity", uiState.sensitivity, viewModel::updateSensitivity, 0.5f..2.0f) { "${"%.1f".format(it)}x" }
                    SliderSetting("Cursor Speed", uiState.cursorSpeed, viewModel::updateCursorSpeed, 0.5f..2.0f) { "${"%.1f".format(it)}x" }
                    SliderSetting("Pointer Precision", uiState.pointerSpeed.toFloat(), { viewModel.updatePointerSpeed(it.toInt()) }, 20f..100f) { "${it.toInt()}%" }
                    SwitchSetting("Mouse Acceleration", "Speed‑dependent cursor movement", uiState.accelerationEnabled, viewModel::updateAcceleration)
                    SwitchSetting("Invert Vertical", "", uiState.invertVertical, viewModel::updateInvertVertical)
                    SwitchSetting("Invert Horizontal", "", uiState.invertHorizontal, viewModel::updateInvertHorizontal)
                }
            }

            item {
                ExpandableSettingsCard("Gestures & Feedback", Icons.Default.TouchApp) {
                    SwitchSetting("Tap to Click", "Single tap = left click", uiState.tapToClick, viewModel::updateTapToClick)
                    SliderSetting("Double‑Tap Delay", uiState.doubleTapDelay.toFloat(), { viewModel.updateDoubleTapDelay(it.toInt()) }, 100f..600f) { "${it.toInt()}ms" }
                    SwitchSetting("Three‑Finger Swipe", "Navigate back/forward, volume", uiState.threeFingerSwipe, viewModel::updateThreeFingerSwipe)
                    SwitchSetting("Pinch to Zoom", "", uiState.pinchToZoom, viewModel::updatePinchToZoom)
                    SwitchSetting("Rotate to Rotate", "", uiState.rotateToRotate, viewModel::updateRotateToRotate)
                    SwitchSetting("Haptic Feedback", "Vibrate on touch", uiState.hapticFeedback, viewModel::updateHapticFeedback)
                    SwitchSetting("Show Touch Points", "Visual feedback on touchpad", uiState.showTouchPoints, viewModel::updateShowTouchPoints)
                }
            }

            if (uiState.lastGesture.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Last Gesture:", style = MaterialTheme.typography.bodyMedium)
                            Text(uiState.lastGesture, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
    onTouchEvent: (Float, Float, Int, List<Pair<Float, Float>>, Float) -> Unit,
    onTap: (Offset) -> Unit,
    onLongPress: () -> Unit,
    onGestureEnd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap(it) }, onLongPress = { onLongPress() })
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        val pointers = changes.filter { it.pressed }

                        if (pointers.isNotEmpty()) {
                            val first = pointers.first()
                            onTouchEvent(
                                first.position.x, first.position.y,
                                pointers.size,
                                pointers.map { it.position.x to it.position.y },
                                first.pressure
                            )
                            changes.forEach { it.consume() }
                        } else {
                            onGestureEnd()
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.showTouchPoints) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.touchPoints.forEach { point ->
                        drawCircle(Color.White.copy(alpha = 0.4f), radius = 30f, center = Offset(point.x, point.y))
                        drawCircle(Color.White, radius = 12f, center = Offset(point.x, point.y))
                    }
                }
            }

            if (!uiState.isActive) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Touchpad Inactive\nPress Start to enable", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (uiState.touchPoints.isEmpty()) {
                Text("Touchpad Active", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatusCard(uiState: TouchpadUiState, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Touchpad Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (uiState.isActive) "Active" else "Inactive", fontSize = 12.sp, color = if (uiState.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(onClick = onToggle, colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                Icon(if (uiState.isActive) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isActive) "Stop" else "Start")
            }
        }
    }
}

@Composable
fun PresetsCard(onApplyPreset: (TouchpadMode) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TouchpadMode.entries.forEach { mode ->
                    OutlinedButton(onClick = { onApplyPreset(mode) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text(mode.displayName, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableSettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            if (expanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
            }
        }
    }
}

@Composable
fun SwitchSetting(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SliderSetting(label: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, formatValue: (Float) -> String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatValue(value), fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.fillMaxWidth())
    }
}

