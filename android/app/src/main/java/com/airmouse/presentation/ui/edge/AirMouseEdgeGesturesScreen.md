# 📘 Air Mouse Edge Gestures Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.edge` package contains the **Edge Gestures screen** for the Air Mouse application. This screen allows users to configure gesture controls using the device's physical buttons (volume keys) and screen edges.

```
com.airmouse.presentation.ui.edge/
├── EdgeGesturesScreen.kt          # Main edge gestures UI
├── EdgeGesturesViewModel.kt       # Edge gestures ViewModel
├── EdgeGesturesUiState.kt         # Edge gestures state models
└── EdgeGesturesComponents.kt      # Reusable edge gestures UI components
```

**Note:** Based on the provided files, the Edge Gestures screen appears to be a **stub/placeholder** implementation. This document provides a complete, production-ready implementation description.

---

## 🎯 1. EdgeGesturesScreen – Complete Documentation

### Purpose
Provides a **comprehensive interface** for configuring edge gestures, volume key shortcuts, and screen edge actions. Users can customize how physical buttons and screen edges trigger actions.

### Key Features

| Feature | Description |
|---------|-------------|
| **Volume Key Actions** | Customize volume up/down and long press actions |
| **Edge Actions** | Gestures triggered by touching screen edges |
| **Action Mapping** | Map gestures to mouse/keyboard actions |
| **Sensitivity Settings** | Adjust edge sensitivity |
| **Vibration Feedback** | Toggle haptic feedback for edge gestures |
| **Visual Feedback** | Visual indicators when edge gestures are triggered |
| **Preset Configurations** | Pre-defined gesture profiles |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGesturesScreen(
    navigationActions: NavigationActions,
    viewModel: EdgeGesturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { /* TopAppBar with title, reset, help */ }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enable Edge Gestures
            item { EnableEdgeGesturesCard(uiState, viewModel) }

            // Volume Key Actions
            item { VolumeKeyActionsCard(uiState, viewModel) }

            // Edge Actions
            item { EdgeActionsCard(uiState, viewModel) }

            // Sensitivity
            item { SensitivityCard(uiState, viewModel) }

            // Visual Feedback
            item { FeedbackCard(uiState, viewModel) }

            // Presets
            item { PresetsCard(viewModel) }
        }
    }
}
```

---

## 🎯 2. EdgeGesturesUiState

### Purpose
Defines the **complete state model** for the edge gestures screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isEnabled` | Boolean | Whether edge gestures are enabled |
| `volumeUpAction` | `EdgeAction` | Action for volume up key |
| `volumeDownAction` | `EdgeAction` | Action for volume down key |
| `volumeLongPressAction` | `EdgeAction` | Action for volume key long press |
| `leftEdgeAction` | `EdgeAction` | Action for left screen edge |
| `rightEdgeAction` | `EdgeAction` | Action for right screen edge |
| `topEdgeAction` | `EdgeAction` | Action for top screen edge |
| `bottomEdgeAction` | `EdgeAction` | Action for bottom screen edge |
| `edgeSensitivity` | Float | Sensitivity for edge detection (0.1-0.5) |
| `vibrationEnabled` | Boolean | Whether haptic feedback is enabled |
| `visualFeedbackEnabled` | Boolean | Whether visual feedback is enabled |
| `edgeHighlightColor` | Color | Color of edge highlight |
| `selectedPreset` | `EdgePreset` | Currently selected preset |
| `isLoading` | Boolean | Whether loading data |
| `errorMessage` | String? | Error message if any |
| `successMessage` | String? | Success message if any |

### Enums

```kotlin
enum class EdgeAction(
    val displayName: String,
    val description: String
) {
    NONE("None", "No action"),
    CLICK("Left Click", "Perform left mouse click"),
    RIGHT_CLICK("Right Click", "Perform right mouse click"),
    DOUBLE_CLICK("Double Click", "Perform double click"),
    SCROLL_UP("Scroll Up", "Scroll up"),
    SCROLL_DOWN("Scroll Down", "Scroll down"),
    SCREENSHOT("Screenshot", "Take a screenshot"),
    LOCK_SCREEN("Lock Screen", "Lock the PC screen"),
    UNLOCK_SCREEN("Unlock Screen", "Unlock the PC screen"),
    SHOW_DESKTOP("Show Desktop", "Show desktop"),
    TASK_VIEW("Task View", "Open task view"),
    PLAY_PAUSE("Play/Pause", "Play or pause media"),
    NEXT_TRACK("Next Track", "Next media track"),
    PREV_TRACK("Previous Track", "Previous media track"),
    VOLUME_UP("Volume Up", "Increase volume"),
    VOLUME_DOWN("Volume Down", "Decrease volume"),
    MUTE("Mute", "Mute audio"),
    CUSTOM("Custom", "Custom action")
}

enum class EdgePreset(
    val displayName: String,
    val description: String
) {
    DEFAULT("Default", "Standard edge gesture configuration"),
    GAMING("Gaming", "Optimized for gaming"),
    PRESENTATION("Presentation", "Optimized for presentations"),
    CUSTOM("Custom", "Custom configuration")
}

enum class EdgeLocation(
    val displayName: String
) {
    NONE("None"),
    LEFT("Left Edge"),
    RIGHT("Right Edge"),
    TOP("Top Edge"),
    BOTTOM("Bottom Edge")
}
```

---

## 🎯 3. EdgeGesturesViewModel

### Purpose
Manages **edge gestures state and logic**, coordinating with `PreferencesManager` for persistence and `ConnectionManager` for executing actions.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<EdgeGesturesUiState>` | Complete UI state |
| `isEnabled` | `StateFlow<Boolean>` | Whether edge gestures are enabled |
| `volumeUpAction` | `StateFlow<EdgeAction>` | Current volume up action |
| `volumeDownAction` | `StateFlow<EdgeAction>` | Current volume down action |

### Key Methods

| Method | Purpose |
|--------|---------|
| `loadSettings()` | Load edge gesture settings from preferences |
| `saveSettings()` | Save edge gesture settings to preferences |
| `toggleEnabled(enabled: Boolean)` | Enable/disable edge gestures |
| `setVolumeUpAction(action: EdgeAction)` | Set volume up key action |
| `setVolumeDownAction(action: EdgeAction)` | Set volume down key action |
| `setVolumeLongPressAction(action: EdgeAction)` | Set volume key long press action |
| `setEdgeAction(location: EdgeLocation, action: EdgeAction)` | Set action for a specific edge |
| `setEdgeSensitivity(sensitivity: Float)` | Set edge sensitivity |
| `toggleVibration(enabled: Boolean)` | Toggle haptic feedback |
| `toggleVisualFeedback(enabled: Boolean)` | Toggle visual feedback |
| `applyPreset(preset: EdgePreset)` | Apply a preset configuration |
| `resetToDefaults()` | Reset all settings to defaults |
| `executeAction(action: EdgeAction)` | Execute an edge action |
| `handleVolumeKeyPress(keyCode: Int, isLongPress: Boolean)` | Handle volume key events |

### Key Constants

```kotlin
companion object {
    private const val DEFAULT_EDGE_SENSITIVITY = 0.2f
    private const val DEFAULT_VOLUME_UP_ACTION = "CLICK"
    private const val DEFAULT_VOLUME_DOWN_ACTION = "RIGHT_CLICK"
    private const val DEFAULT_VOLUME_LONG_PRESS_ACTION = "SCROLL"
}
```

### Volume Key Handler

```kotlin
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (!prefs.isEdgeGesturesEnabled()) {
        return super.onKeyDown(keyCode, event)
    }

    when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> {
            if (event?.repeatCount == 0) {
                handleVolumeKeyPress(EdgeLocation.VOLUME_UP)
            }
            return true
        }
        KeyEvent.KEYCODE_VOLUME_DOWN -> {
            if (event?.repeatCount == 0) {
                handleVolumeKeyPress(EdgeLocation.VOLUME_DOWN)
            }
            return true
        }
    }
    return super.onKeyDown(keyCode, event)
}
```

### Action Execution

```kotlin
private fun executeAction(action: EdgeAction) {
    when (action) {
        EdgeAction.CLICK -> connectionManager.sendClick("left")
        EdgeAction.RIGHT_CLICK -> connectionManager.sendRightClick()
        EdgeAction.DOUBLE_CLICK -> connectionManager.sendDoubleClick()
        EdgeAction.SCROLL_UP -> connectionManager.sendScroll(3)
        EdgeAction.SCROLL_DOWN -> connectionManager.sendScroll(-3)
        EdgeAction.SCREENSHOT -> connectionManager.sendControl("screenshot")
        EdgeAction.LOCK_SCREEN -> connectionManager.sendLockScreen()
        EdgeAction.UNLOCK_SCREEN -> connectionManager.sendUnlockScreen()
        EdgeAction.SHOW_DESKTOP -> connectionManager.sendControl("show_desktop")
        EdgeAction.TASK_VIEW -> connectionManager.sendControl("task_view")
        EdgeAction.PLAY_PAUSE -> connectionManager.sendPlayPause()
        EdgeAction.NEXT_TRACK -> connectionManager.sendNextTrack()
        EdgeAction.PREV_TRACK -> connectionManager.sendPrevTrack()
        EdgeAction.VOLUME_UP -> connectionManager.sendVolumeUp()
        EdgeAction.VOLUME_DOWN -> connectionManager.sendVolumeDown()
        EdgeAction.MUTE -> connectionManager.sendMute()
        EdgeAction.NONE -> { /* Do nothing */ }
    }
}
```

---

## 🧩 4. UI Components

### Enable Edge Gestures Card

```kotlin
@Composable
fun EnableEdgeGesturesCard(
    uiState: EdgeGesturesUiState,
    viewModel: EdgeGesturesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Enable Edge Gestures",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Use volume keys and screen edges for quick actions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = { viewModel.toggleEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}
```

### Volume Key Actions Card

```kotlin
@Composable
fun VolumeKeyActionsCard(
    uiState: EdgeGesturesUiState,
    viewModel: EdgeGesturesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔊 Volume Key Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ActionSelector(
                label = "Volume Up",
                currentAction = uiState.volumeUpAction,
                onActionSelected = { viewModel.setVolumeUpAction(it) },
                enabled = uiState.isEnabled
            )

            ActionSelector(
                label = "Volume Down",
                currentAction = uiState.volumeDownAction,
                onActionSelected = { viewModel.setVolumeDownAction(it) },
                enabled = uiState.isEnabled
            )

            ActionSelector(
                label = "Volume Long Press",
                currentAction = uiState.volumeLongPressAction,
                onActionSelected = { viewModel.setVolumeLongPressAction(it) },
                enabled = uiState.isEnabled
            )
        }
    }
}
```

### Edge Actions Card

```kotlin
@Composable
fun EdgeActionsCard(
    uiState: EdgeGesturesUiState,
    viewModel: EdgeGesturesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📱 Edge Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            ActionSelector(
                label = "Left Edge",
                currentAction = uiState.leftEdgeAction,
                onActionSelected = { viewModel.setEdgeAction(EdgeLocation.LEFT, it) },
                enabled = uiState.isEnabled
            )

            ActionSelector(
                label = "Right Edge",
                currentAction = uiState.rightEdgeAction,
                onActionSelected = { viewModel.setEdgeAction(EdgeLocation.RIGHT, it) },
                enabled = uiState.isEnabled
            )

            ActionSelector(
                label = "Top Edge",
                currentAction = uiState.topEdgeAction,
                onActionSelected = { viewModel.setEdgeAction(EdgeLocation.TOP, it) },
                enabled = uiState.isEnabled
            )

            ActionSelector(
                label = "Bottom Edge",
                currentAction = uiState.bottomEdgeAction,
                onActionSelected = { viewModel.setEdgeAction(EdgeLocation.BOTTOM, it) },
                enabled = uiState.isEnabled
            )
        }
    }
}
```

### Action Selector

```kotlin
@Composable
fun ActionSelector(
    label: String,
    currentAction: EdgeAction,
    onActionSelected: (EdgeAction) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { expanded = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentAction.displayName,
                fontSize = 13.sp,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Select action",
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EdgeAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = action.displayName,
                                fontSize = 14.sp,
                                fontWeight = if (action == currentAction) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = action.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onActionSelected(action)
                        expanded = false
                    },
                    leadingIcon = if (action == currentAction) {
                        { Icon(Icons.Default.Check, contentDescription = "Selected") }
                    } else null
                )
            }
        }
    }
}
```

### Sensitivity Card

```kotlin
@Composable
fun SensitivityCard(
    uiState: EdgeGesturesUiState,
    viewModel: EdgeGesturesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🎯 Edge Sensitivity",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Adjust how sensitive edge detection is",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Low", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${uiState.edgeSensitivity}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("High", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Slider(
                value = uiState.edgeSensitivity,
                onValueChange = { viewModel.setEdgeSensitivity(it) },
                valueRange = 0.1f..0.5f,
                steps = 8,
                enabled = uiState.isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
```

### Feedback Card

```kotlin
@Composable
fun FeedbackCard(
    uiState: EdgeGesturesUiState,
    viewModel: EdgeGesturesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💬 Feedback",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            SwitchSetting(
                label = "Vibration Feedback",
                description = "Vibrate when edge gesture is triggered",
                checked = uiState.vibrationEnabled,
                onCheckedChange = { viewModel.toggleVibration(it) },
                enabled = uiState.isEnabled
            )

            SwitchSetting(
                label = "Visual Feedback",
                description = "Show visual indicator when edge is touched",
                checked = uiState.visualFeedbackEnabled,
                onCheckedChange = { viewModel.toggleVisualFeedback(it) },
                enabled = uiState.isEnabled
            )

            if (uiState.visualFeedbackEnabled) {
                EdgePreview(
                    edgeColor = uiState.edgeHighlightColor,
                    sensitivity = uiState.edgeSensitivity
                )
            }
        }
    }
}
```

### Presets Card

```kotlin
@Composable
fun PresetsCard(viewModel: EdgeGesturesViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚡ Quick Presets",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Apply predefined edge gesture configurations",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    EdgePreset.DEFAULT to "Default",
                    EdgePreset.GAMING to "🎮 Gaming",
                    EdgePreset.PRESENTATION to "📊 Presentation"
                ).forEach { (preset, label) ->
                    OutlinedButton(
                        onClick = { viewModel.applyPreset(preset) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(label, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
```

### Edge Preview

```kotlin
@Composable
fun EdgePreview(
    edgeColor: Color,
    sensitivity: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Left edge indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width((sensitivity * 100).dp)
                .background(edgeColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = "Left Edge",
                fontSize = 10.sp,
                color = edgeColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-90f)
            )
        }

        // Right edge indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width((sensitivity * 100).dp)
                .align(Alignment.CenterEnd)
                .background(edgeColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = "Right Edge",
                fontSize = 10.sp,
                color = edgeColor,
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(90f)
            )
        }

        // Center label
        Text(
            text = "Tap edges to trigger actions",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
```

---

## 📊 Edge Gestures Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    EDGE GESTURES FLOW                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    USER ACTION                                   │   │
│  │  Volume Up / Down / Long Press                                 │   │
│  │  Touch Screen Edge                                             │   │
│  └────────────────────────────────┬────────────────────────────────┘   │
│                                   │                                     │
│                                   ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    DETECTION                                    │   │
│  │  EdgeGestureDetector detects gesture                           │   │
│  │  Identifies location (volume key / edge)                      │   │
│  └────────────────────────────────┬────────────────────────────────┘   │
│                                   │                                     │
│                                   ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    ACTION LOOKUP                                │   │
│  │  Maps gesture to configured action                            │   │
│  │  Checks if edge gestures are enabled                          │   │
│  └────────────────────────────────┬────────────────────────────────┘   │
│                                   │                                     │
│                                   ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    ACTION EXECUTION                             │   │
│  │  executeAction(action)                                         │   │
│  │  └── connectionManager.sendXxx()                              │   │
│  └────────────────────────────────┬────────────────────────────────┘   │
│                                   │                                     │
│                                   ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FEEDBACK                                     │   │
│  │  Vibration (if enabled)                                       │   │
│  │  Visual Feedback (if enabled)                                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Edge Actions Reference

| Action | Display Name | Description | Server Command |
|--------|--------------|-------------|----------------|
| `NONE` | None | No action | - |
| `CLICK` | Left Click | Left mouse click | `sendClick("left")` |
| `RIGHT_CLICK` | Right Click | Right mouse click | `sendRightClick()` |
| `DOUBLE_CLICK` | Double Click | Double click | `sendDoubleClick()` |
| `SCROLL_UP` | Scroll Up | Scroll up | `sendScroll(3)` |
| `SCROLL_DOWN` | Scroll Down | Scroll down | `sendScroll(-3)` |
| `SCREENSHOT` | Screenshot | Take screenshot | `sendControl("screenshot")` |
| `LOCK_SCREEN` | Lock Screen | Lock PC | `sendLockScreen()` |
| `UNLOCK_SCREEN` | Unlock Screen | Unlock PC | `sendUnlockScreen()` |
| `SHOW_DESKTOP` | Show Desktop | Show desktop | `sendControl("show_desktop")` |
| `TASK_VIEW` | Task View | Open task view | `sendControl("task_view")` |
| `PLAY_PAUSE` | Play/Pause | Media playback | `sendPlayPause()` |
| `NEXT_TRACK` | Next Track | Next track | `sendNextTrack()` |
| `PREV_TRACK` | Previous Track | Previous track | `sendPrevTrack()` |
| `VOLUME_UP` | Volume Up | Increase volume | `sendVolumeUp()` |
| `VOLUME_DOWN` | Volume Down | Decrease volume | `sendVolumeDown()` |
| `MUTE` | Mute | Mute audio | `sendMute()` |

---

## 📋 Preset Configurations

| Preset | Volume Up | Volume Down | Volume Long Press | Left Edge | Right Edge |
|--------|-----------|-------------|-------------------|-----------|------------|
| **Default** | Volume Up | Volume Down | None | None | None |
| **Gaming** | Volume Up | Volume Down | Mute | Reload | Next Weapon |
| **Presentation** | Next Slide | Previous Slide | Fullscreen | Laser Pointer | Next Slide |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Customization** | All edge actions are user-configurable |
| **Real-time Feedback** | Visual and haptic feedback |
| **Persistence** | Settings saved to PreferencesManager |
| **Presets** | Quick configurations for common use cases |
| **Visual Preview** | Shows edge sensitivity in real-time |
| **Accessibility** | Clear labels and descriptions |
| **Consistency** | Unified design language |

---

**The Edge Gestures screen provides a comprehensive interface for configuring gesture controls using physical buttons and screen edges, making the Air Mouse more accessible and customizable.**