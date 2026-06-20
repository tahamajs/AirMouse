// app/src/main/java/com/airmouse/network/MessageTypes.kt
package com.airmouse.network

/**
 * Central repository for all message types, commands, and constants used in the
 * Air Mouse communication protocol between Android client and Go server.
 *
 * This ensures consistency across the entire app and prevents string typos.
 *
 * Protocol Version: 3.0
 */
object MessageTypes {

    // ==================== CLIENT TO SERVER ====================

    /** Mouse movement delta */
    const val TYPE_MOVE = "move"

    /** Mouse click (left, right, middle) */
    const val TYPE_CLICK = "click"

    /** Double click (left button) */
    const val TYPE_DOUBLE_CLICK = "doubleclick"

    /** Right click (convenience) */
    const val TYPE_RIGHT_CLICK = "rightclick"

    /** Scroll wheel movement */
    const val TYPE_SCROLL = "scroll"

    /** Device identification */
    const val TYPE_HELLO = "hello"

    /** Recognised gesture from ML model */
    const val TYPE_GESTURE = "gesture"

    /** Proximity distance update for auto lock/unlock */
    const val TYPE_PROXIMITY = "proximity"

    /** Control command (pause, resume, lock, etc.) */
    const val TYPE_CONTROL = "control"

    /** Keep-alive heartbeat */
    const val TYPE_PING = "ping"

    /** Orientation data from device sensors */
    const val TYPE_ORIENTATION = "orientation"

    /** Battery status update */
    const val TYPE_BATTERY = "battery"

    /** Device status update */
    const val TYPE_STATUS = "status"

    /** Log message from client */
    const val TYPE_LOG = "log"

    /** Custom data message */
    const val TYPE_CUSTOM = "custom"

    // ==================== SERVER TO CLIENT ====================

    /** Welcome message after successful hello */
    const val TYPE_WELCOME = "welcome"

    /** Response to ping */
    const val TYPE_PONG = "pong"

    /** Acknowledgment of a command with ID */
    const val TYPE_ACK = "ack"

    /** Error message from server */
    const val TYPE_ERROR = "error"

    /** Server statistics */
    const val TYPE_STATS = "stats"

    /** Server notification */
    const val TYPE_NOTIFICATION = "notification"

    /** Server configuration update */
    const val TYPE_CONFIG = "config"

    // ==================== CONTROL COMMANDS ====================

    /** Temporarily ignore move messages */
    const val COMMAND_PAUSE_MOVEMENT = "pause_movement"

    /** Resume processing move messages */
    const val COMMAND_RESUME_MOVEMENT = "resume_movement"

    /** Reset movement state */
    const val COMMAND_RESET = "reset"

    /** Calibrate sensors or movement */
    const val COMMAND_CALIBRATE = "calibrate"

    /** Lock the computer screen */
    const val COMMAND_LOCK_SCREEN = "lock_screen"

    /** Unlock the computer screen */
    const val COMMAND_UNLOCK_SCREEN = "unlock_screen"

    /** Show desktop / minimize all windows */
    const val COMMAND_SHOW_DESKTOP = "show_desktop"

    /** Open task view / mission control */
    const val COMMAND_TASK_VIEW = "task_view"

    /** Switch between windows */
    const val COMMAND_SWITCH_WINDOW = "switch_window"

    /** Start screen recording */
    const val COMMAND_START_RECORDING = "start_recording"

    /** Stop screen recording */
    const val COMMAND_STOP_RECORDING = "stop_recording"

    /** Take screenshot */
    const val COMMAND_SCREENSHOT = "screenshot"

    /** Lock the device */
    const val COMMAND_DEVICE_LOCK = "device_lock"

    /** Sleep the device */
    const val COMMAND_SLEEP = "sleep"

    /** Restart the device */
    const val COMMAND_RESTART = "restart"

    /** Shutdown the device */
    const val COMMAND_SHUTDOWN = "shutdown"

    // ==================== MEDIA CONTROL COMMANDS ====================

    /** Play/Pause media */
    const val COMMAND_PLAY_PAUSE = "play_pause"

    /** Next track */
    const val COMMAND_NEXT_TRACK = "next_track"

    /** Previous track */
    const val COMMAND_PREV_TRACK = "prev_track"

    /** Stop media */
    const val COMMAND_STOP = "stop"

    /** Volume up */
    const val COMMAND_VOLUME_UP = "volume_up"

    /** Volume down */
    const val COMMAND_VOLUME_DOWN = "volume_down"

    /** Mute audio */
    const val COMMAND_MUTE = "mute"

    /** Seek forward in media */
    const val COMMAND_SEEK_FORWARD = "seek_forward"

    /** Seek backward in media */
    const val COMMAND_SEEK_BACKWARD = "seek_backward"

    /** Repeat mode toggle */
    const val COMMAND_REPEAT = "repeat"

    /** Shuffle mode toggle */
    const val COMMAND_SHUFFLE = "shuffle"

    // ==================== WINDOW COMMANDS ====================

    /** Maximize current window */
    const val COMMAND_WINDOW_MAXIMIZE = "window_maximize"

    /** Minimize current window */
    const val COMMAND_WINDOW_MINIMIZE = "window_minimize"

    /** Close current window */
    const val COMMAND_WINDOW_CLOSE = "window_close"

    /** Enter/exit fullscreen */
    const val COMMAND_WINDOW_FULLSCREEN = "window_fullscreen"

    /** Snap window to left half */
    const val COMMAND_WINDOW_SNAP_LEFT = "window_snap_left"

    /** Snap window to right half */
    const val COMMAND_WINDOW_SNAP_RIGHT = "window_snap_right"

    /** Snap window to top half */
    const val COMMAND_WINDOW_SNAP_TOP = "window_snap_top"

    /** Snap window to bottom half */
    const val COMMAND_WINDOW_SNAP_BOTTOM = "window_snap_bottom"

    /** Move window to next monitor */
    const val COMMAND_WINDOW_NEXT_MONITOR = "window_next_monitor"

    // ==================== BROWSER COMMANDS ====================

    /** Browser back navigation */
    const val COMMAND_BROWSER_BACK = "browser_back"

    /** Browser forward navigation */
    const val COMMAND_BROWSER_FORWARD = "browser_forward"

    /** Refresh current page */
    const val COMMAND_BROWSER_REFRESH = "browser_refresh"

    /** Go to home page */
    const val COMMAND_BROWSER_HOME = "browser_home"

    /** Open new tab */
    const val COMMAND_BROWSER_NEW_TAB = "browser_new_tab"

    /** Close current tab */
    const val COMMAND_BROWSER_CLOSE_TAB = "browser_close_tab"

    /** Switch to next tab */
    const val COMMAND_BROWSER_NEXT_TAB = "browser_next_tab"

    /** Switch to previous tab */
    const val COMMAND_BROWSER_PREV_TAB = "browser_prev_tab"

    /** Open incognito/private tab */
    const val COMMAND_BROWSER_INCOGNITO = "browser_incognito"

    // ==================== TEXT EDITING COMMANDS ====================

    /** Copy selected text */
    const val COMMAND_COPY = "copy"

    /** Cut selected text */
    const val COMMAND_CUT = "cut"

    /** Paste from clipboard */
    const val COMMAND_PASTE = "paste"

    /** Undo last action */
    const val COMMAND_UNDO = "undo"

    /** Redo last action */
    const val COMMAND_REDO = "redo"

    /** Select all text */
    const val COMMAND_SELECT_ALL = "select_all"

    /** Find text */
    const val COMMAND_FIND = "find"

    /** Replace text */
    const val COMMAND_REPLACE = "replace"

    // ==================== ZOOM COMMANDS ====================

    /** Zoom in */
    const val COMMAND_ZOOM_IN = "zoom_in"

    /** Zoom out */
    const val COMMAND_ZOOM_OUT = "zoom_out"

    /** Reset zoom to default */
    const val COMMAND_ZOOM_RESET = "zoom_reset"

    /** Fit to screen */
    const val COMMAND_ZOOM_FIT = "zoom_fit"

    // ==================== GESTURE TYPES ====================

    /** Thumbs up gesture - typically Play/Pause */
    const val GESTURE_THUMBS_UP = "ThumbsUp"

    /** Thumbs down gesture - typically Stop */
    const val GESTURE_THUMBS_DOWN = "ThumbsDown"

    /** Left swipe gesture - typically Previous Track */
    const val GESTURE_SWIPE_LEFT = "SwipeLeft"

    /** Right swipe gesture - typically Next Track */
    const val GESTURE_SWIPE_RIGHT = "SwipeRight"

    /** Up swipe gesture - typically Volume Up */
    const val GESTURE_SWIPE_UP = "SwipeUp"

    /** Down swipe gesture - typically Volume Down */
    const val GESTURE_SWIPE_DOWN = "SwipeDown"

    /** Clockwise circle gesture - typically Volume Up */
    const val GESTURE_CIRCLE_CW = "CircleCW"

    /** Counter-clockwise circle gesture - typically Volume Down */
    const val GESTURE_CIRCLE_CCW = "CircleCCW"

    /** Pinch in gesture - typically Zoom Out */
    const val GESTURE_PINCH_IN = "PinchIn"

    /** Pinch out gesture - typically Zoom In */
    const val GESTURE_PINCH_OUT = "PinchOut"

    /** Double tap gesture - typically Play/Pause */
    const val GESTURE_DOUBLE_TAP = "DoubleTap"

    /** Long press gesture - typically Right Click */
    const val GESTURE_LONG_PRESS = "LongPress"

    /** Shake gesture - typically Undo */
    const val GESTURE_SHAKE = "Shake"

    /** Peace sign gesture - typically Lock Screen */
    const val GESTURE_PEACE = "Peace"

    /** Fist gesture - typically Mute */
    const val GESTURE_FIST = "Fist"

    /** Zoom in gesture */
    const val GESTURE_ZOOM_IN = "ZoomIn"

    /** Zoom out gesture */
    const val GESTURE_ZOOM_OUT = "ZoomOut"

    /** Wave gesture - typically Hello */
    const val GESTURE_WAVE = "Wave"

    /** OK sign gesture - typically Confirm */
    const val GESTURE_OK = "Ok"

    /** Point gesture - typically Select */
    const val GESTURE_POINT = "Point"

    /** Heart gesture - typically Favorite */
    const val GESTURE_HEART = "Heart"

    /** V sign gesture - typically Victory */
    const val GESTURE_V = "V"

    // ==================== BUTTON TYPES ====================

    /** Left mouse button */
    const val BUTTON_LEFT = "left"

    /** Right mouse button */
    const val BUTTON_RIGHT = "right"

    /** Middle mouse button */
    const val BUTTON_MIDDLE = "middle"

    /** Back mouse button */
    const val BUTTON_BACK = "back"

    /** Forward mouse button */
    const val BUTTON_FORWARD = "forward"

    // ==================== PROTOCOL VERSIONS ====================

    /** Current protocol version */
    const val PROTOCOL_VERSION = "3.0"

    /** Minimum supported protocol version */
    const val MIN_PROTOCOL_VERSION = "3.0"

    /** Maximum supported protocol version */
    const val MAX_PROTOCOL_VERSION = "3.0"

    // ==================== CONNECTION PARAMETERS ====================

    /** Default TCP port */
    const val DEFAULT_TCP_PORT = 8080

    /** Default WebSocket port */
    const val DEFAULT_WEBSOCKET_PORT = 8081

    /** Default UDP discovery port */
    const val DEFAULT_UDP_PORT = 8082

    /** Default heartbeat interval in milliseconds */
    const val DEFAULT_HEARTBEAT_INTERVAL_MS = 30000L

    /** Default connection timeout in milliseconds */
    const val DEFAULT_CONNECTION_TIMEOUT_MS = 10000L

    /** Default read timeout in milliseconds */
    const val DEFAULT_READ_TIMEOUT_MS = 30000L

    /** Default write timeout in milliseconds */
    const val DEFAULT_WRITE_TIMEOUT_MS = 5000L

    /** Default max reconnection attempts */
    const val DEFAULT_MAX_RECONNECT_ATTEMPTS = 10

    /** Default reconnection delay in milliseconds */
    const val DEFAULT_RECONNECT_DELAY_MS = 3000L

    // ==================== MESSAGE SIZE LIMITS ====================

    /** Maximum size of a move message in bytes */
    const val MAX_MOVE_SIZE = 128

    /** Maximum size of a click message in bytes */
    const val MAX_CLICK_SIZE = 64

    /** Maximum size of a gesture message in bytes */
    const val MAX_GESTURE_SIZE = 256

    /** Maximum size of a proximity message in bytes */
    const val MAX_PROXIMITY_SIZE = 128

    /** Maximum size of a control message in bytes */
    const val MAX_CONTROL_SIZE = 64

    /** Maximum size of a hello message in bytes */
    const val MAX_HELLO_SIZE = 256

    // ==================== RATE LIMITS ====================

    /** Maximum move messages per second */
    const val MAX_MOVE_RATE = 120

    /** Maximum click messages per second */
    const val MAX_CLICK_RATE = 20

    /** Maximum scroll messages per second */
    const val MAX_SCROLL_RATE = 20

    /** Maximum gesture messages per second */
    const val MAX_GESTURE_RATE = 10

    /** Maximum proximity messages per second */
    const val MAX_PROXIMITY_RATE = 5

    /** Maximum control messages per second */
    const val MAX_CONTROL_RATE = 10

    // ==================== ERROR CODES ====================

    /** Invalid JSON format */
    const val ERROR_INVALID_JSON = 400

    /** Missing required field */
    const val ERROR_MISSING_FIELD = 401

    /** Invalid message type */
    const val ERROR_INVALID_TYPE = 402

    /** Authentication failed */
    const val ERROR_AUTH_FAILED = 403

    /** Permission denied */
    const val ERROR_PERMISSION_DENIED = 404

    /** Rate limit exceeded */
    const val ERROR_RATE_LIMIT = 429

    /** Internal server error */
    const val ERROR_INTERNAL = 500

    /** Service unavailable */
    const val ERROR_SERVICE_UNAVAILABLE = 503

    // ==================== MESSAGE PRIORITIES ====================

    /** High priority - critical actions (click, lock, etc.) */
    const val PRIORITY_HIGH = "high"

    /** Medium priority - standard actions (move, scroll) */
    const val PRIORITY_MEDIUM = "medium"

    /** Low priority - analytics, logs, etc. */
    const val PRIORITY_LOW = "low"
}

/**
 * Helper object for gesture-to-action mapping
 */
object GestureActionMap {

    /**
     * Map gesture names to their default system actions
     */
    val defaultActions: Map<String, String> = mapOf(
        MessageTypes.GESTURE_THUMBS_UP to MessageTypes.COMMAND_PLAY_PAUSE,
        MessageTypes.GESTURE_THUMBS_DOWN to MessageTypes.COMMAND_STOP,
        MessageTypes.GESTURE_SWIPE_LEFT to MessageTypes.COMMAND_PREV_TRACK,
        MessageTypes.GESTURE_SWIPE_RIGHT to MessageTypes.COMMAND_NEXT_TRACK,
        MessageTypes.GESTURE_SWIPE_UP to MessageTypes.COMMAND_VOLUME_UP,
        MessageTypes.GESTURE_SWIPE_DOWN to MessageTypes.COMMAND_VOLUME_DOWN,
        MessageTypes.GESTURE_CIRCLE_CW to MessageTypes.COMMAND_VOLUME_UP,
        MessageTypes.GESTURE_CIRCLE_CCW to MessageTypes.COMMAND_VOLUME_DOWN,
        MessageTypes.GESTURE_PEACE to MessageTypes.COMMAND_LOCK_SCREEN,
        MessageTypes.GESTURE_FIST to MessageTypes.COMMAND_MUTE,
        MessageTypes.GESTURE_DOUBLE_TAP to MessageTypes.COMMAND_PLAY_PAUSE,
        MessageTypes.GESTURE_LONG_PRESS to MessageTypes.COMMAND_WINDOW_CLOSE,
        MessageTypes.GESTURE_SHAKE to MessageTypes.COMMAND_RESET,
        MessageTypes.GESTURE_ZOOM_IN to MessageTypes.COMMAND_ZOOM_IN,
        MessageTypes.GESTURE_ZOOM_OUT to MessageTypes.COMMAND_ZOOM_OUT,
        MessageTypes.GESTURE_WAVE to "hello",
        MessageTypes.GESTURE_OK to "confirm",
        MessageTypes.GESTURE_POINT to "select",
        MessageTypes.GESTURE_HEART to "favorite",
        MessageTypes.GESTURE_V to "victory"
    )

    /**
     * Get action for a gesture, with fallback to "none"
     */
    fun getActionForGesture(gesture: String): String {
        return defaultActions[gesture] ?: "none"
    }

    /**
     * Get confidence threshold for a gesture (different gestures may need different thresholds)
     */
    fun getConfidenceThreshold(gesture: String): Float {
        return when (gesture) {
            MessageTypes.GESTURE_DOUBLE_TAP -> 0.65f
            MessageTypes.GESTURE_LONG_PRESS -> 0.7f
            MessageTypes.GESTURE_SHAKE -> 0.75f
            MessageTypes.GESTURE_PEACE -> 0.7f
            MessageTypes.GESTURE_FIST -> 0.7f
            MessageTypes.GESTURE_WAVE -> 0.7f
            MessageTypes.GESTURE_OK -> 0.7f
            MessageTypes.GESTURE_POINT -> 0.7f
            MessageTypes.GESTURE_HEART -> 0.75f
            MessageTypes.GESTURE_V -> 0.7f
            else -> 0.6f
        }
    }

    /**
     * Get priority for a gesture
     */
    fun getPriorityForGesture(gesture: String): String {
        return when (gesture) {
            MessageTypes.GESTURE_THUMBS_UP,
            MessageTypes.GESTURE_THUMBS_DOWN,
            MessageTypes.GESTURE_DOUBLE_TAP,
            MessageTypes.GESTURE_LONG_PRESS,
            MessageTypes.GESTURE_PEACE,
            MessageTypes.GESTURE_FIST -> MessageTypes.PRIORITY_HIGH

            MessageTypes.GESTURE_SWIPE_LEFT,
            MessageTypes.GESTURE_SWIPE_RIGHT,
            MessageTypes.GESTURE_SWIPE_UP,
            MessageTypes.GESTURE_SWIPE_DOWN,
            MessageTypes.GESTURE_CIRCLE_CW,
            MessageTypes.GESTURE_CIRCLE_CCW -> MessageTypes.PRIORITY_MEDIUM

            else -> MessageTypes.PRIORITY_LOW
        }
    }
}// app/src/main/java/com/airmouse/network/MessageTypes.kt
package com.airmouse.network

object MessageTypes {
    // Protocol version
    const val PROTOCOL_VERSION = "3.0"

    // Default ports
    const val DEFAULT_TCP_PORT = 8080
    const val DEFAULT_WEBSOCKET_PORT = 8081
    const val DEFAULT_UDP_PORT = 8082

    // Message types (client → server)
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
    const val TYPE_PONG = "pong"
    const val TYPE_ACK = "ack"
    const val TYPE_ERROR = "error"
    const val TYPE_WELCOME = "welcome"
    const val TYPE_CALIBRATION_DATA = "calibration_data"

    // Button types
    const val BUTTON_LEFT = "left"
    const val BUTTON_RIGHT = "right"
    const val BUTTON_MIDDLE = "middle"
    const val BUTTON_BACK = "back"
    const val BUTTON_FORWARD = "forward"

    // Control commands
    const val COMMAND_PAUSE_MOVEMENT = "pause_movement"
    const val COMMAND_RESUME_MOVEMENT = "resume_movement"
    const val COMMAND_LOCK_SCREEN = "lock_screen"
    const val COMMAND_UNLOCK_SCREEN = "unlock_screen"
    const val COMMAND_CALIBRATE = "calibrate"
    const val COMMAND_RESET = "reset"
    const val COMMAND_PLAY_PAUSE = "play_pause"
    const val COMMAND_NEXT_TRACK = "next_track"
    const val COMMAND_PREV_TRACK = "prev_track"
    const val COMMAND_STOP = "stop"
    const val COMMAND_VOLUME_UP = "volume_up"
    const val COMMAND_VOLUME_DOWN = "volume_down"
    const val COMMAND_MUTE = "mute"
    const val COMMAND_BROWSER_BACK = "browser_back"
    const val COMMAND_BROWSER_FORWARD = "browser_forward"
    const val COMMAND_BROWSER_REFRESH = "browser_refresh"
    const val COMMAND_BROWSER_HOME = "browser_home"
    const val COMMAND_SHOW_DESKTOP = "show_desktop"
    const val COMMAND_TASK_VIEW = "task_view"
    const val COMMAND_SWITCH_WINDOW = "switch_window"
    const val COMMAND_ZOOM_IN = "zoom_in"
    const val COMMAND_ZOOM_OUT = "zoom_out"
    const val COMMAND_ZOOM_RESET = "zoom_reset"
}