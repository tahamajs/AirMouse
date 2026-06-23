
package com.airmouse.presentation.ui.edge

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*

data class EdgeGesturesUiState(
    val isEnabled: Boolean = false,
    val volumeUpAction: EdgeAction = EdgeAction.LEFT_CLICK,
    val volumeDownAction: EdgeAction = EdgeAction.RIGHT_CLICK,
    
    val longPressAction: EdgeAction = EdgeAction.SCROLL_UP,
    val doublePressAction: EdgeAction = EdgeAction.DOUBLE_CLICK,
    val vibrationFeedback: Boolean = true,
    val screenEdgeSensitivity: Float = 0.2f,
    val isConfiguring: Boolean = false,
    val configuringAction: EdgeAction? = null,
    val lastDetectedGesture: String? = null,
    val gestureDetectionProgress: Float = 0f
)

enum class EdgeAction(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    LEFT_CLICK("Left Click", "Simulate left mouse button click", Icons.Default.Mouse),
    RIGHT_CLICK("Right Click", "Simulate right mouse button click", Icons.Default.Mouse),
    DOUBLE_CLICK("Double Click", "Simulate double click", Icons.Default.Cached),
    SCROLL_UP("Scroll Up", "Scroll up", Icons.Default.ArrowUpward),
    SCROLL_DOWN("Scroll Down", "Scroll down", Icons.Default.ArrowDownward),
    
    VOLUME_UP("Volume Up", "Increase system volume", Icons.AutoMirrored.Filled.VolumeUp),
    VOLUME_DOWN("Volume Down", "Decrease system volume", Icons.AutoMirrored.Filled.VolumeDown),
    PREV_TRACK("Previous Track", "Previous media track", Icons.Default.SkipPrevious),
    NEXT_TRACK("Next Track", "Next media track", Icons.Default.SkipNext),
    PLAY_PAUSE("Play/Pause", "Play or pause media", Icons.Default.PlayArrow),
    LOCK_SCREEN("Lock Screen", "Lock the screen", Icons.Default.Lock),
    SHOW_DESKTOP("Show Desktop", "Minimize all windows", Icons.Default.DesktopWindows),
    TASK_VIEW("Task View", "Show task switcher", Icons.Default.Window),
    TAKE_SCREENSHOT("Take Screenshot", "Capture screenshot", Icons.Default.CameraAlt),
    TOGGLE_KEYBOARD("Toggle Keyboard", "Show/hide keyboard", Icons.Default.Keyboard),
    VOICE_COMMAND("Voice Command", "Activate voice assistant", Icons.Default.Mic),
    NONE("None", "Disabled", Icons.Default.Cancel)
}

data class EdgeGestureEvent(
    val type: GestureType,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1f
)

enum class GestureType {
    VOLUME_UP, VOLUME_DOWN, LONG_PRESS, DOUBLE_PRESS
}

data class EdgeGesturesStats(
    val totalDetections: Int = 0,
    val volumeUpCount: Int = 0,
    val volumeDownCount: Int = 0,
    val longPressCount: Int = 0,
    val doublePressCount: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0
)