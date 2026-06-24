
package com.airmouse.presentation.ui.touchpad

import androidx.compose.ui.graphics.vector.ImageVector
import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.presentation.navigation.Destinations





data class TouchpadUiState(
    
    val isActive: Boolean = false,

    
    val sensitivity: Float = 1.0f,
    val cursorSpeed: Float = 1.0f,
    val pointerSpeed: Int = 50,
    val accelerationEnabled: Boolean = true,
    val invertVertical: Boolean = false,
    val invertHorizontal: Boolean = false,

    
    val scrollSpeed: Float = 1.0f,
    val naturalScrolling: Boolean = true,
    val twoFingerScroll: Boolean = true,
    val edgeScrolling: Boolean = false,
    val scrollInertia: Boolean = true,

    
    val tapToClick: Boolean = true,
    val doubleTapDelay: Int = 300,
    val threeFingerSwipe: Boolean = true,
    val pinchToZoom: Boolean = true,
    val rotateToRotate: Boolean = false,

    
    val hapticFeedback: Boolean = true,
    val showTouchPoints: Boolean = false,

    
    val currentX: Float = 0f,
    val currentY: Float = 0f,
    val isDragging: Boolean = false,
    val isScrolling: Boolean = false,
    val lastGesture: String = "",
    val touchPoints: List<TouchPoint> = emptyList(),
    val gestureHistory: List<String> = emptyList(),

    
    val connectionConfig: ConnectionConfig = ConnectionConfig(),
    val mouseProfile: MovementProfile = MovementProfile(),
    val appPreferences: AppPreferences = AppPreferences(),
    val userPreferences: UserPreferences = UserPreferences(),
    val mouseStatistics: MouseStatistics = MouseStatistics()
)

data class TouchPoint(
    val id: Int,
    val x: Float,
    val y: Float,
    val pressure: Float
)





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





sealed class TouchpadEvent {
    
    object ToggleTouchpad : TouchpadEvent()
    object ResetToDefaults : TouchpadEvent()
    data class ApplyPreset(val mode: TouchpadMode) : TouchpadEvent()

    
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

    
    object NavigateBack : TouchpadEvent()
    object NavigateToSettings : TouchpadEvent()
}





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

    fun getPreset(mode: TouchpadMode): TouchpadPreset {
        return when (mode) {
            TouchpadMode.STANDARD -> STANDARD
            TouchpadMode.PRECISION -> PRECISION
            TouchpadMode.GAMING -> GAMING
            TouchpadMode.PRESENTATION -> PRESENTATION
            TouchpadMode.CUSTOM -> STANDARD
        }
    }
}
