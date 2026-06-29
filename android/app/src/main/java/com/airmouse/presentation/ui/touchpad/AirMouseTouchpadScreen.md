# 📘 Air Mouse Touchpad Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.touchpad` package contains the **Touchpad screen** for the Air Mouse application. This screen provides a full touchpad simulation with gesture recognition, haptic feedback, visual feedback, and comprehensive settings for touchpad behavior.

```
com.airmouse.presentation.ui.touchpad/
├── TouchpadScreen.kt              # Main touchpad UI
├── TouchpadViewModel.kt           # Touchpad ViewModel
├── TouchpadState.kt               # Touchpad state models
├── TouchpadComponents.kt          # Reusable touchpad UI components
└── TouchpadConstants.kt           # Touchpad constants
```

---

## 🎯 1. TouchpadScreen

### Purpose
Provides a **full touchpad simulation** with gesture recognition, visual feedback, haptic feedback, and comprehensive settings.

### Key Features

| Feature | Description |
|---------|-------------|
| **Touch Surface** | Large touch area for cursor control |
| **Gesture Detection** | Drag, scroll, swipe, pinch, tap, long-press |
| **Multi-Touch** | 1-4 finger gesture support |
| **Visual Feedback** | Touch points display |
| **Haptic Feedback** | Vibration on touch actions |
| **Quick Presets** | Standard, Precision, Gaming, Presentation |
| **Comprehensive Settings** | Sensitivity, scroll speed, gestures, feedback |
| **Gesture History** | Recent gesture log |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadScreen(
    navigationActions: NavigationActions,
    viewModel: TouchpadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()

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
                }
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
            // Hero Card
            item {
                TouchpadHeroCard(uiState)
            }

            // Domain Summary
            item {
                TouchpadDomainSummaryCard(uiState)
            }

            // Touch Surface
            item {
                TouchpadSurface(
                    uiState = uiState,
                    onTouchEvent = { x, y, count, pointers, pressure ->
                        viewModel.handleEvent(TouchpadEvent.TouchEvent(x, y, count, pointers, pressure))
                    },
                    onTap = { viewModel.handleEvent(TouchpadEvent.TapEvent(it.x, it.y)) },
                    onLongPress = { viewModel.handleEvent(TouchpadEvent.LongPressEvent) },
                    onGestureEnd = { viewModel.handleEvent(TouchpadEvent.GestureEnd) }
                )
            }

            // Status Card
            item {
                StatusCard(uiState, viewModel)
            }

            // Presets Card
            item {
                PresetsCard(viewModel)
            }

            // Touch Scrolling Settings
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

            // Cursor Control Settings
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
                        description = "Flip up/down movement",
                        checked = uiState.invertVertical,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleInvertVertical) }
                    )
                    SwitchSetting(
                        label = "Invert Horizontal",
                        description = "Flip left/right movement",
                        checked = uiState.invertHorizontal,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.ToggleInvertHorizontal) }
                    )
                }
            }

            // Gestures & Feedback Settings
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
                        description = "Pinch gestures for zoom",
                        checked = uiState.pinchToZoom,
                        onCheckedChange = { viewModel.handleEvent(TouchpadEvent.TogglePinchToZoom) }
                    )
                    SwitchSetting(
                        label = "Rotate to Rotate",
                        description = "Rotate gestures for rotation",
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

            // Last Gesture
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

            // Gesture History
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
```

---

## 🎯 2. TouchpadState

### Purpose
Defines the **complete state model** for the touchpad screen, including all settings, enums, events, effects, and presets.

### TouchpadUiState

| Category | Properties | Count |
|----------|------------|-------|
| **Status** | isActive | 1 |
| **Cursor Settings** | sensitivity, cursorSpeed, pointerSpeed, accelerationEnabled, invertVertical, invertHorizontal | 6 |
| **Scroll Settings** | scrollSpeed, naturalScrolling, twoFingerScroll, edgeScrolling, scrollInertia | 5 |
| **Gesture Settings** | tapToClick, doubleTapDelay, threeFingerSwipe, pinchToZoom, rotateToRotate | 5 |
| **Feedback Settings** | hapticFeedback, showTouchPoints | 2 |
| **Touch State** | currentX, currentY, isDragging, isScrolling, lastGesture, touchPoints, gestureHistory | 7 |
| **Domain Models** | connectionConfig, mouseProfile, appPreferences, userPreferences, mouseStatistics | 5 |
| **Total** | | **31** |

### Enums

```kotlin
enum class TouchpadMode(val displayName: String) {
    STANDARD("Standard"),
    PRECISION("Precision"),
    GAMING("Gaming"),
    PRESENTATION("Presentation"),
    CUSTOM("Custom")
}

enum class TouchpadGesture(val displayName: String) {
    TAP("Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right"),
    SWIPE_UP("Swipe Up"),
    SWIPE_DOWN("Swipe Down"),
    PINCH_IN("Pinch In"),
    PINCH_OUT("Pinch Out"),
    ROTATE_CW("Rotate CW"),
    ROTATE_CCW("Rotate CCW"),
    THREE_FINGER_SWIPE_LEFT("3-Finger Swipe Left"),
    THREE_FINGER_SWIPE_RIGHT("3-Finger Swipe Right"),
    THREE_FINGER_SWIPE_UP("3-Finger Swipe Up"),
    THREE_FINGER_SWIPE_DOWN("3-Finger Swipe Down"),
    FOUR_FINGER_SWIPE_LEFT("4-Finger Swipe Left"),
    FOUR_FINGER_SWIPE_RIGHT("4-Finger Swipe Right"),
    FOUR_FINGER_SWIPE_UP("4-Finger Swipe Up"),
    FOUR_FINGER_SWIPE_DOWN("4-Finger Swipe Down"),
    EDGE_SCROLL("Edge Scroll"),
    TWO_FINGER_SCROLL("Two-Finger Scroll"),
    DRAG("Drag"),
    NONE("None")
}
```

### Events

```kotlin
sealed class TouchpadEvent {
    // Control Events
    object ToggleTouchpad : TouchpadEvent()
    object ResetToDefaults : TouchpadEvent()
    data class ApplyPreset(val mode: TouchpadMode) : TouchpadEvent()

    // Touch Events
    data class TouchEvent(
        val x: Float,
        val y: Float,
        val pointerCount: Int,
        val pointers: List<Pair<Float, Float>>,
        val pressure: Float
    ) : TouchpadEvent()
    data class TapEvent(val x: Float, val y: Float) : TouchpadEvent()
    object LongPressEvent : TouchpadEvent()
    object GestureEnd : TouchpadEvent()

    // Settings Events (30+ events)
    data class UpdateSensitivity(val value: Float) : TouchpadEvent()
    data class UpdateCursorSpeed(val value: Float) : TouchpadEvent()
    data class UpdatePointerSpeed(val value: Int) : TouchpadEvent()
    object ToggleAcceleration : TouchpadEvent()
    object ToggleInvertVertical : TouchpadEvent()
    object ToggleInvertHorizontal : TouchpadEvent()
    data class UpdateScrollSpeed(val value: Float) : TouchpadEvent()
    object ToggleNaturalScrolling : TouchpadEvent()
    object ToggleTwoFingerScroll : TouchpadEvent()
    object ToggleEdgeScrolling : TouchpadEvent()
    object ToggleScrollInertia : TouchpadEvent()
    object ToggleTapToClick : TouchpadEvent()
    data class UpdateDoubleTapDelay(val value: Int) : TouchpadEvent()
    object ToggleThreeFingerSwipe : TouchpadEvent()
    object TogglePinchToZoom : TouchpadEvent()
    object ToggleRotateToRotate : TouchpadEvent()
    object ToggleHapticFeedback : TouchpadEvent()
    object ToggleShowTouchPoints : TouchpadEvent()

    // Navigation Events
    object NavigateBack : TouchpadEvent()
    object NavigateToSettings : TouchpadEvent()
}
```

### Effects

```kotlin
sealed class TouchpadEffect {
    data class ShowToast(val message: String) : TouchpadEffect()
    data class SendMessage(val message: String) : TouchpadEffect()
    data class SendMove(val dx: Float, val dy: Float) : TouchpadEffect()
    data class SendScroll(val delta: Int) : TouchpadEffect()
    data class SendClick(val button: String) : TouchpadEffect()
    data class SendGesture(val gesture: String) : TouchpadEffect()
    data class SendCommand(val command: String) : TouchpadEffect()
    object NavigateBack : TouchpadEffect()
    object NavigateToSettings : TouchpadEffect()
    data class Vibrate(val duration: Long) : TouchpadEffect()
}
```

### Presets

```kotlin
data class TouchpadPreset(
    val mode: TouchpadMode,
    val sensitivity: Float,
    val cursorSpeed: Float,
    val scrollSpeed: Float,
    val accelerationEnabled: Boolean,
    val tapToClick: Boolean,
    val twoFingerScroll: Boolean,
    val threeFingerSwipe: Boolean,
    val naturalScrolling: Boolean
)

object TouchpadPresets {
    val STANDARD = TouchpadPreset(
        mode = TouchpadMode.STANDARD,
        sensitivity = 1.0f,
        cursorSpeed = 1.0f,
        scrollSpeed = 1.0f,
        accelerationEnabled = true,
        tapToClick = true,
        twoFingerScroll = true,
        threeFingerSwipe = true,
        naturalScrolling = true
    )

    val PRECISION = TouchpadPreset(
        mode = TouchpadMode.PRECISION,
        sensitivity = 0.7f,
        cursorSpeed = 0.6f,
        scrollSpeed = 0.8f,
        accelerationEnabled = false,
        tapToClick = true,
        twoFingerScroll = true,
        threeFingerSwipe = false,
        naturalScrolling = true
    )

    val GAMING = TouchpadPreset(
        mode = TouchpadMode.GAMING,
        sensitivity = 1.5f,
        cursorSpeed = 1.5f,
        scrollSpeed = 0.5f,
        accelerationEnabled = true,
        tapToClick = false,
        twoFingerScroll = false,
        threeFingerSwipe = false,
        naturalScrolling = false
    )

    val PRESENTATION = TouchpadPreset(
        mode = TouchpadMode.PRESENTATION,
        sensitivity = 0.5f,
        cursorSpeed = 1.0f,
        scrollSpeed = 1.5f,
        accelerationEnabled = false,
        tapToClick = true,
        twoFingerScroll = true,
        threeFingerSwipe = true,
        naturalScrolling = false
    )
}
```

---

## 🧩 3. UI Components

### TouchpadHeroCard

```kotlin
@Composable
fun TouchpadHeroCard(uiState: TouchpadUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Touchpad mode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Tap to click", "Two-finger scroll", "Gestures").forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
```

### TouchpadSurface

```kotlin
@Composable
fun TouchpadSurface(
    uiState: TouchpadUiState,
    onTouchEvent: (Float, Float, Int, List<Pair<Float, Float>>, Float) -> Unit,
    onTap: (Offset) -> Unit,
    onLongPress: () -> Unit,
    onGestureEnd: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val surfaceFill = if (uiState.isActive) 
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f) 
    else 
        MaterialTheme.colorScheme.surfaceVariant
    val gridAccent = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
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
        colors = CardDefaults.cardColors(containerColor = surfaceFill),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Grid Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = gridAccent,
                    cornerRadius = CornerRadius(24f, 24f)
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

            // Touch Points
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

            // Placeholder Text
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
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
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
```

### StatusCard

```kotlin
@Composable
fun StatusCard(
    uiState: TouchpadUiState,
    viewModel: TouchpadViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
```

### PresetsCard

```kotlin
@Composable
fun PresetsCard(viewModel: TouchpadViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
```

### ExpandableSettingsCard

```kotlin
@Composable
fun ExpandableSettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
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
```

### SwitchSetting

```kotlin
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
```

### SliderSetting

```kotlin
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
```

---

## 🎯 4. TouchpadViewModel

### Key Methods

| Method | Purpose |
|--------|---------|
| `handleEvent(event: TouchpadEvent)` | Handle user events |
| `toggleTouchpad()` | Start/stop touchpad mode |
| `processTouchEvent()` | Process raw touch events |
| `handleSingleFingerMove()` | Handle 1-finger drag |
| `handleTwoFingerGesture()` | Handle 2-finger scroll/pinch |
| `handleThreeFingerGesture()` | Handle 3-finger swipe |
| `handleFourFingerGesture()` | Handle 4-finger volume control |
| `processTap()` | Handle tap gestures |
| `processLongPress()` | Handle long press |
| `resetGestureState()` | Reset gesture tracking |
| `applyPreset()` | Apply quick preset |
| `sendMove()` | Send movement to server |
| `sendScroll()` | Send scroll to server |
| `sendClick()` | Send click to server |
| `vibrate()` | Trigger haptic feedback |

### Gesture Handling

| Pointer Count | Gesture | Action |
|---------------|---------|--------|
| 1 | Drag | Cursor movement |
| 1 | Tap | Left click |
| 1 | Long Press | Right click |
| 2 | Scroll | Scroll up/down |
| 2 | Pinch | Zoom in/out |
| 3 | Swipe | Task view/Desktop |
| 4 | Swipe | Volume control |

---

## 📋 Touchpad Gesture Reference

| Gesture | Fingers | Action | Description |
|---------|---------|--------|-------------|
| **Tap** | 1 | Left Click | Single tap on touchpad |
| **Double Tap** | 1 | Left Click | Two quick taps |
| **Long Press** | 1 | Right Click | Hold finger down |
| **Drag** | 1 | Move | Move finger to move cursor |
| **Two-Finger Scroll** | 2 | Scroll | Move two fingers up/down |
| **Pinch In** | 2 | Zoom Out | Pinch fingers together |
| **Pinch Out** | 2 | Zoom In | Spread fingers apart |
| **Rotate CW** | 2 | Rotate | Rotate fingers clockwise |
| **Rotate CCW** | 2 | Rotate | Rotate fingers counter-clockwise |
| **3-Finger Swipe Up** | 3 | Task View | Swipe up with 3 fingers |
| **3-Finger Swipe Down** | 3 | Show Desktop | Swipe down with 3 fingers |
| **3-Finger Swipe Left** | 3 | Switch Window | Swipe left with 3 fingers |
| **3-Finger Swipe Right** | 3 | Switch Window | Swipe right with 3 fingers |
| **4-Finger Swipe Up** | 4 | Volume Up | Swipe up with 4 fingers |
| **4-Finger Swipe Down** | 4 | Volume Down | Swipe down with 4 fingers |
| **4-Finger Swipe Left** | 4 | Switch Window | Swipe left with 4 fingers |
| **4-Finger Swipe Right** | 4 | Switch Window | Swipe right with 4 fingers |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Multi-Touch Support** | 1-4 finger gesture recognition |
| **Visual Feedback** | Touch points and animations |
| **Haptic Feedback** | Vibration on touch actions |
| **Real-time Processing** | Instant gesture detection |
| **Presets** | Quick configuration |
| **Comprehensive Settings** | 30+ configurable options |
| **Gesture History** | Recent gesture log |
| **Reactive UI** | StateFlow with automatic updates |

---

**The Touchpad Screen provides a complete touchpad simulation with comprehensive gesture recognition, visual feedback, haptic feedback, and extensive customization options.**