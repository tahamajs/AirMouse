// app/src/main/java/com/airmouse/network/MessageTypes.kt
package com.airmouse.network

object MessageTypes {
    // Client to Server
    const val TYPE_MOVE = "move"
    const val TYPE_CLICK = "click"
    const val TYPE_DOUBLE_CLICK = "doubleclick"
    const val TYPE_RIGHT_CLICK = "rightclick"
    const val TYPE_SCROLL = "scroll"
    const val TYPE_HELLO = "hello"
    const val TYPE_GESTURE = "gesture"
    const val TYPE_PROXIMITY = "proximity"
    const val TYPE_CONTROL = "control"
    const val TYPE_PING = "ping"
    
    // Server to Client
    const val TYPE_WELCOME = "welcome"
    const val TYPE_PONG = "pong"
    const val TYPE_ACK = "ack"
    const val TYPE_ERROR = "error"
    
    // Control commands
    const val COMMAND_PAUSE_MOVEMENT = "pause_movement"
    const val COMMAND_RESUME_MOVEMENT = "resume_movement"
    const val COMMAND_LOCK_SCREEN = "lock_screen"
    const val COMMAND_UNLOCK_SCREEN = "unlock_screen"
    const val COMMAND_CALIBRATE = "calibrate"
    const val COMMAND_RESET = "reset"
    
    // Gesture types
    const val GESTURE_THUMBS_UP = "ThumbsUp"
    const val GESTURE_THUMBS_DOWN = "ThumbsDown"
    const val GESTURE_SWIPE_LEFT = "SwipeLeft"
    const val GESTURE_SWIPE_RIGHT = "SwipeRight"
    const val GESTURE_SWIPE_UP = "SwipeUp"
    const val GESTURE_SWIPE_DOWN = "SwipeDown"
    const val GESTURE_CIRCLE_CW = "CircleCW"
    const val GESTURE_CIRCLE_CCW = "CircleCCW"
    const val GESTURE_ZOOM_IN = "ZoomIn"
    const val GESTURE_ZOOM_OUT = "ZoomOut"
    const val GESTURE_DOUBLE_TAP = "DoubleTap"
    const val GESTURE_LONG_PRESS = "LongPress"
    
    // Click buttons
    const val BUTTON_LEFT = "left"
    const val BUTTON_RIGHT = "right"
    const val BUTTON_MIDDLE = "middle"
}