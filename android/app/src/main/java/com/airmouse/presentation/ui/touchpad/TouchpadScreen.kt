// app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadScreen.kt
package com.airmouse.presentation.ui.touchpad

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    navigationActions: NavigationActions,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is TouchpadEffect.NavigateBack -> navigationActions.navigateBack()
            is TouchpadEffect.NavigateToSettings -> navigationActions.navigateToSettings()
            is TouchpadEffect.ShowToast -> {
                Toast.makeText(context, currentEffect.message, Toast.LENGTH_SHORT).show()
            }
            else -> { /* Other effects handled by connection manager */ }
        }
        if (effect != null) {
            viewModel.clearEffect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Touchpad Mode", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(TouchpadEvent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleEvent(TouchpadEvent.ResetToDefaults) }) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset")
                    }
                    IconButton(onClick = { viewModel.handleEvent(TouchpadEvent.NavigateToSettings) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                TouchpadHeroCard(uiState)
            }
            item {
                TouchpadDomainSummaryCard(uiState)
            }
            item {
                TouchpadSurface(
                    uiState = uiState,
                    onTouchEvent = { x, y, count, pointers, p ->
                        viewModel.handleEvent(TouchpadEvent.TouchEvent(x, y, count, pointers, p))
                    },
                    onTap = { viewModel.handleEvent(TouchpadEvent.TapEvent(it.x, it.y)) },
                    onLongPress = { viewModel.handleEvent(TouchpadEvent.LongPressEvent) },
                    onGestureEnd = { viewModel.handleEvent(TouchpadEvent.GestureEnd) }
                )
            }

            item { StatusCard(uiState, viewModel) }
            item { TouchpadTestCard(uiState, viewModel) }
            item { PresetsCard(viewModel) }

            item {
                ExpandableSettingsCard(
                    title = "Touch Scrolling",
                    icon = Icons.Default.SwapVert
                ) {
                    SwitchSetting(
                        label = "Two‑Finger Scroll",
                        description = "Use two fingers to scroll",
                        checked = uiState.twoFingerScroll,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleTwoFingerScroll) }
                    )
                    SwitchSetting(
                        label = "Natural Scrolling",
                        description = "Content follows finger direction",
                        checked = uiState.naturalScrolling,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleNaturalScrolling) }
                    )
                    SliderSetting(
                        label = "Scroll Speed",
                        value = uiState.scrollSpeed,
                        onValueChange = { viewModel.handleEvent(TouchpadEvent.UpdateScrollSpeed(it)) },
                        valueRange = 0.5f..2.0f,
                        formatValue = { "${"%.1f".format(it)}x" }
                    )
                    SwitchSetting(
                        label = "Edge Scrolling",
                        description = "Scroll by touching screen edges",
                        checked = uiState.edgeScrolling,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleEdgeScrolling) }
                    )
                    SwitchSetting(
                        label = "Scroll Inertia",
                        description = "Smooth scrolling after fingers lift",
                        checked = uiState.scrollInertia,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleScrollInertia) }
                    )
                }
            }

            item {
                ExpandableSettingsCard(
                    title = "Cursor Control",
                    icon = Icons.Default.Mouse
                ) {
                    SliderSetting(
                        label = "Cursor Sensitivity",
                        value = uiState.sensitivity,
                        onValueChange = { viewModel.handleEvent(TouchpadEvent.UpdateSensitivity(it)) },
                        valueRange = 0.5f..2.0f,
                        formatValue = { "${"%.1f".format(it)}x" }
                    )
                    SliderSetting(
                        label = "Cursor Speed",
                        value = uiState.cursorSpeed,
                        onValueChange = { viewModel.handleEvent(TouchpadEvent.UpdateCursorSpeed(it)) },
                        valueRange = 0.5f..2.0f,
                        formatValue = { "${"%.1f".format(it)}x" }
                    )
                    SliderSetting(
                        label = "Pointer Precision",
                        value = uiState.pointerSpeed.toFloat(),
                        onValueChange = { viewModel.handleEvent(TouchpadEvent.UpdatePointerSpeed(it.toInt())) },
                        valueRange = 20f..100f,
                        formatValue = { "${it.toInt()}%" }
                    )
                    SwitchSetting(
                        label = "Mouse Acceleration",
                        description = "Speed‑dependent cursor movement",
                        checked = uiState.accelerationEnabled,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleAcceleration) }
                    )
                    SwitchSetting(
                        label = "Invert Vertical",
                        description = "",
                        checked = uiState.invertVertical,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleInvertVertical) }
                    )
                    SwitchSetting(
                        label = "Invert Horizontal",
                        description = "",
                        checked = uiState.invertHorizontal,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleInvertHorizontal) }
                    )
                }
            }

            item {
                ExpandableSettingsCard(
                    title = "Gestures & Feedback",
                    icon = Icons.Default.TouchApp
                ) {
                    SwitchSetting(
                        label = "Tap to Click",
                        description = "Single tap = left click",
                        checked = uiState.tapToClick,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleTapToClick) }
                    )
                    SliderSetting(
                        label = "Double‑Tap Delay",
                        value = uiState.doubleTapDelay.toFloat(),
                        onValueChange = { viewModel.handleEvent(TouchpadEvent.UpdateDoubleTapDelay(it.toInt())) },
                        valueRange = 100f..600f,
                        formatValue = { "${it.toInt()}ms" }
                    )
                    SwitchSetting(
                        label = "Three‑Finger Swipe",
                        description = "Navigate back/forward, volume",
                        checked = uiState.threeFingerSwipe,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleThreeFingerSwipe) }
                    )
                    SwitchSetting(
                        label = "Pinch to Zoom",
                        description = "",
                        checked = uiState.pinchToZoom,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.TogglePinchToZoom) }
                    )
                    SwitchSetting(
                        label = "Rotate to Rotate",
                        description = "",
                        checked = uiState.rotateToRotate,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleRotateToRotate) }
                    )
                    SwitchSetting(
                        label = "Haptic Feedback",
                        description = "Vibrate on touch",
                        checked = uiState.hapticFeedback,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleHapticFeedback) }
                    )
                    SwitchSetting(
                        label = "Show Touch Points",
                        description = "Visual feedback on touchpad",
                        checked = uiState.showTouchPoints,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleShowTouchPoints) }
                    )
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
                            Text(
                                uiState.lastGesture,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (uiState.gestureHistory.isNotEmpty()) {
                item {
                    ExpandableSettingsCard(
                        title = "Gesture History",
                        icon = Icons.Default.History
                    ) {
                        uiState.gestureHistory.takeLast(10).forEach { gesture ->
                            Text(
                                "• $gesture",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ============================================================
// UI COMPONENTS
// ============================================================

@Composable
private fun TouchpadDomainSummaryCard(uiState: TouchpadUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Domain snapshot", fontWeight = FontWeight.Bold)
            Text(
                "Profile, preferences, and session stats now come from shared models.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { }, label = { Text("Sensitivity ${"%.1f".format(uiState.mouseProfile.sensitivity)}") })
                AssistChip(onClick = { }, label = { Text("Clicks ${uiState.mouseStatistics.totalClicks}") })
                AssistChip(onClick = { }, label = { Text("Auto connect ${if (uiState.userPreferences.autoConnect) "On" else "Off"}") })
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
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    val surfaceFill by animateColorAsState(
        targetValue = if (uiState.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500),
        label = "surfaceFill"
    )
    
    val gridAccent by animateColorAsState(
        targetValue = if (uiState.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        animationSpec = tween(500),
        label = "gridAccent"
    )
    
    val gridLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var isLongPress = false
                    var longPressJob: Job? = null

                    // ✅ FIXED: Use the coroutine scope from rememberCoroutineScope
                    longPressJob = scope.launch {
                        delay(500)
                        if (!isLongPress) {
                            isLongPress = true
                            onLongPress()
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            longPressJob?.cancel()
                            if (!isLongPress && event.changes.size == 1) {
                                onTap(down.position)
                            }
                            onGestureEnd()
                            break
                        } else {
                            val first = pressed.first()
                            onTouchEvent(
                                first.position.x,
                                first.position.y,
                                pressed.size,
                                pressed.map { it.position.x to it.position.y },
                                first.pressure
                            )
                            pressed.forEach { it.consume() }
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = surfaceFill
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Grid background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = gridAccent,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                )
                val centerY = size.height / 2f
                val centerX = size.width / 2f
                drawLine(
                    color = gridLine,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = gridLine,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2f
                )
            }

            // Touch point visualisation
            if (uiState.showTouchPoints) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    uiState.touchPoints.forEach { point ->
                        drawCircle(
                            Color.White.copy(alpha = 0.4f),
                            radius = 30f,
                            center = Offset(point.x, point.y)
                        )
                        drawCircle(
                            Color.White,
                            radius = 12f,
                            center = Offset(point.x, point.y)
                        )
                        drawCircle(
                            Color.White.copy(alpha = 0.3f),
                            radius = 20f + point.pressure * 20f,
                            center = Offset(point.x, point.y)
                        )
                    }
                }
            }

            // Placeholder text when inactive
            if (!uiState.isActive) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = labelColor
                    )
                    Text(
                        "Touchpad Inactive\nPress Start to enable",
                        textAlign = TextAlign.Center,
                        color = labelColor
                    )
                }
            } else if (uiState.touchPoints.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Touchpad Active", color = labelColor)
                    Text(
                        "Swipe, tap, two-finger scroll, and long press for click actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TouchpadHeroCard(uiState: TouchpadUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Touchpad mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (uiState.isActive) "Ready for cursor, click, and scroll control."
                        else "Press Start to turn the phone into a touchpad.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                AssistChip(
                    onClick = { },
                    enabled = false,
                    label = { Text(if (uiState.isActive) "Active" else "Idle") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Tap to click", "Two-finger scroll", "Gestures").forEach { label ->
                    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(uiState: TouchpadUiState, viewModel: TouchpadViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Touchpad Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (uiState.isActive) "Active" else "Inactive",
                    fontSize = 12.sp,
                    color = if (uiState.isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = { viewModel.handleEvent(TouchpadEvent.ToggleTouchpad) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (uiState.isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isActive) "Stop" else "Start")
            }
        }
    }
}

@Composable
fun TouchpadTestCard(uiState: TouchpadUiState, viewModel: TouchpadViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Connection Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                if (uiState.connectionTestMessage.isNotBlank()) uiState.connectionTestMessage
                else "Run a quick server test before using the touchpad.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.testConnection() },
                enabled = !uiState.isTestingConnection,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.isTestingConnection) "Testing..." else "Test Server")
            }
            uiState.connectionTestResult?.let {
                Text(
                    "Last result: ${it.message}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PresetsCard(viewModel: TouchpadViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TouchpadMode.entries.forEach { mode ->
                    OutlinedButton(
                        onClick = { viewModel.handleEvent(TouchpadEvent.ApplyPreset(mode)) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(mode.displayName, fontSize = 11.sp)
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    description: String,
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
                Text(
                    description,
                    fontSize = 12.sp,
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

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatValue(value),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
