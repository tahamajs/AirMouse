// app/src/main/java/com/airmouse/network/ConnectionHelper.kt
package com.airmouse.network

import org.json.JSONObject

/**
 * Extension functions for easier message construction.
 * These helpers create properly formatted JSON messages for the Go server.
 *
 * Usage:
 * ```
 * val moveMsg = JSONObject().putMove(12.5f, -3.2f)
 * connectionManager.send(moveMsg.toString())
 *
 * // Or use the extension functions directly:
 * connectionManager.sendMove(12.5f, -3.2f)
 * ```
 */

// ==================== MOUSE COMMANDS ====================

/**
 * Create a move message JSON object
 * @param dx Delta X movement (positive = right)
 * @param dy Delta Y movement (positive = down)
 */
fun JSONObject.putMove(dx: Float, dy: Float): JSONObject = apply {
    put("type", MessageTypes.TYPE_MOVE)
    put("dx", dx)
    put("dy", dy)
}

/**
 * Create a click message JSON object
 * @param button Button name (left, right, middle)
 * @param id Optional request ID for acknowledgment
 */
fun JSONObject.putClick(button: String, id: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_CLICK)
    put("button", button)
    id?.let { put("id", it) }
}

/**
 * Create a double click message JSON object
 */
fun JSONObject.putDoubleClick(): JSONObject = apply {
    put("type", MessageTypes.TYPE_DOUBLE_CLICK)
}

/**
 * Create a right click message JSON object
 */
fun JSONObject.putRightClick(): JSONObject = apply {
    put("type", MessageTypes.TYPE_RIGHT_CLICK)
}

/**
 * Create a scroll message JSON object
 * @param delta Positive = scroll up, negative = scroll down
 */
fun JSONObject.putScroll(delta: Int): JSONObject = apply {
    put("type", MessageTypes.TYPE_SCROLL)
    put("delta", delta)
}

// ==================== IDENTIFICATION ====================

/**
 * Create a hello message JSON object for device identification
 * @param name Device name (e.g., "Pixel 8 Pro")
 * @param version App version (e.g., "3.0")
 * @param device Optional device model
 * @param androidVersion Optional Android version
 */
fun JSONObject.putHello(
    name: String,
    version: String = MessageTypes.PROTOCOL_VERSION,
    device: String? = null,
    androidVersion: String? = null
): JSONObject = apply {
    put("type", MessageTypes.TYPE_HELLO)
    put("payload", JSONObject().apply {
        put("name", name)
        put("version", version)
        device?.let { put("device", it) }
        androidVersion?.let { put("android_version", it) }
    })
}

// ==================== GESTURE COMMANDS ====================

/**
 * Create a gesture message JSON object
 * @param gesture Gesture name (e.g., "ThumbsUp")
 * @param confidence Confidence score (0.0 to 1.0)
 */
fun JSONObject.putGesture(gesture: String, confidence: Float): JSONObject = apply {
    put("type", MessageTypes.TYPE_GESTURE)
    put("payload", JSONObject().apply {
        put("gesture", gesture)
        put("confidence", confidence)
    })
}

// ==================== PROXIMITY COMMANDS ====================

/**
 * Create a proximity message JSON object
 * @param isNear True if device is near (within threshold)
 * @param distance Estimated distance in meters
 * @param deviceId Unique device identifier
 */
fun JSONObject.putProximity(isNear: Boolean, distance: Float, deviceId: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_PROXIMITY)
    put("payload", JSONObject().apply {
        put("device_id", deviceId)
        put("is_near", isNear)
        put("distance", distance)
    })
}

// ==================== CONTROL COMMANDS ====================

/**
 * Create a control message JSON object
 * @param command Control command (e.g., "pause_movement")
 */
fun JSONObject.putControl(command: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_CONTROL)
    put("payload", JSONObject().apply {
        put("command", command)
    })
}

// ==================== HEARTBEAT COMMANDS ====================

/**
 * Create a ping message JSON object (keep-alive)
 */
fun JSONObject.putPing(): JSONObject = apply {
    put("type", MessageTypes.TYPE_PING)
}

/**
 * Create a pong message JSON object (response to ping)
 */
fun JSONObject.putPong(): JSONObject = apply {
    put("type", MessageTypes.TYPE_PONG)
}

// ==================== SERVER TO CLIENT ====================

/**
 * Create a welcome message JSON object (server to client)
 * @param serverName Server name
 * @param version Server version
 * @param clientId Optional client ID
 */
fun JSONObject.putWelcome(serverName: String, version: String, clientId: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_WELCOME)
    put("payload", JSONObject().apply {
        put("server", serverName)
        put("version", version)
        clientId?.let { put("id", it) }
    })
}

/**
 * Create an acknowledgment message JSON object
 * @param id Request ID being acknowledged
 * @param status Status (ok/error)
 * @param message Optional status message
 */
fun JSONObject.putAck(id: String, status: String = "ok", message: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_ACK)
    put("id", id)
    put("status", status)
    message?.let { put("message", it) }
}

/**
 * Create an error message JSON object
 * @param code Error code
 * @param message Error message
 */
fun JSONObject.putError(code: Int, message: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_ERROR)
    put("payload", JSONObject().apply {
        put("code", code)
        put("message", message)
    })
}

// ==================== CONVENIENCE EXTENSIONS FOR CONNECTION MANAGER ====================

/**
 * Extension function to send a move with dx, dy
 */
fun ConnectionManager.sendMove(dx: Float, dy: Float, id: String? = null): Boolean {
    return send(JSONObject().putMove(dx, dy).toString())
}

/**
 * Extension function to send a click with button
 */
fun ConnectionManager.sendClick(button: String = MessageTypes.BUTTON_LEFT, id: String? = null): Boolean {
    return send(JSONObject().putClick(button, id).toString())
}

/**
 * Extension function to send a double click
 */
fun ConnectionManager.sendDoubleClick(): Boolean {
    return send(JSONObject().putDoubleClick().toString())
}

/**
 * Extension function to send a right click
 */
fun ConnectionManager.sendRightClick(): Boolean {
    return send(JSONObject().putRightClick().toString())
}

/**
 * Extension function to send a scroll
 */
fun ConnectionManager.sendScroll(delta: Int): Boolean {
    return send(JSONObject().putScroll(delta).toString())
}

/**
 * Extension function to send a gesture
 */
fun ConnectionManager.sendGesture(gesture: String, confidence: Float): Boolean {
    return send(JSONObject().putGesture(gesture, confidence).toString())
}

/**
 * Extension function to send proximity update
 */
fun ConnectionManager.sendProximity(isNear: Boolean, distance: Float, deviceId: String): Boolean {
    return send(JSONObject().putProximity(isNear, distance, deviceId).toString())
}

/**
 * Extension function to send control command
 */
fun ConnectionManager.sendControlCommand(command: String): Boolean {
    return send(JSONObject().putControl(command).toString())
}

/**
 * Extension function to send pause movement command
 */
fun ConnectionManager.sendPauseMovement(): Boolean = sendControlCommand(MessageTypes.COMMAND_PAUSE_MOVEMENT)

/**
 * Extension function to send resume movement command
 */
fun ConnectionManager.sendResumeMovement(): Boolean = sendControlCommand(MessageTypes.COMMAND_RESUME_MOVEMENT)

/**
 * Extension function to send lock screen command
 */
fun ConnectionManager.sendLockScreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_LOCK_SCREEN)

/**
 * Extension function to send unlock screen command
 */
fun ConnectionManager.sendUnlockScreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_UNLOCK_SCREEN)

/**
 * Extension function to send calibrate command
 */
fun ConnectionManager.sendCalibrate(): Boolean = sendControlCommand(MessageTypes.COMMAND_CALIBRATE)

/**
 * Extension function to send reset command
 */
fun ConnectionManager.sendReset(): Boolean = sendControlCommand(MessageTypes.COMMAND_RESET)

/**
 * Extension function to send show desktop command
 */
fun ConnectionManager.sendShowDesktop(): Boolean = sendControlCommand(MessageTypes.COMMAND_SHOW_DESKTOP)

/**
 * Extension function to send task view command
 */
fun ConnectionManager.sendTaskView(): Boolean = sendControlCommand(MessageTypes.COMMAND_TASK_VIEW)

// ==================== MEDIA CONTROL EXTENSIONS ====================

/**
 * Extension function to send play/pause command
 */
fun ConnectionManager.sendPlayPause(): Boolean = sendControlCommand(MessageTypes.COMMAND_PLAY_PAUSE)

/**
 * Extension function to send next track command
 */
fun ConnectionManager.sendNextTrack(): Boolean = sendControlCommand(MessageTypes.COMMAND_NEXT_TRACK)

/**
 * Extension function to send previous track command
 */
fun ConnectionManager.sendPrevTrack(): Boolean = sendControlCommand(MessageTypes.COMMAND_PREV_TRACK)

/**
 * Extension function to send stop media command
 */
fun ConnectionManager.sendStop(): Boolean = sendControlCommand(MessageTypes.COMMAND_STOP)

/**
 * Extension function to send volume up command
 */
fun ConnectionManager.sendVolumeUp(): Boolean = sendControlCommand(MessageTypes.COMMAND_VOLUME_UP)

/**
 * Extension function to send volume down command
 */
fun ConnectionManager.sendVolumeDown(): Boolean = sendControlCommand(MessageTypes.COMMAND_VOLUME_DOWN)

/**
 * Extension function to send mute command
 */
fun ConnectionManager.sendMute(): Boolean = sendControlCommand(MessageTypes.COMMAND_MUTE)

// ==================== WINDOW CONTROL EXTENSIONS ====================

/**
 * Extension function to send window maximize command
 */
fun ConnectionManager.sendWindowMaximize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MAXIMIZE)

/**
 * Extension function to send window minimize command
 */
fun ConnectionManager.sendWindowMinimize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MINIMIZE)

/**
 * Extension function to send window close command
 */
fun ConnectionManager.sendWindowClose(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_CLOSE)

/**
 * Extension function to send window fullscreen command
 */
fun ConnectionManager.sendWindowFullscreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_FULLSCREEN)

// ==================== BROWSER CONTROL EXTENSIONS ====================

/**
 * Extension function to send browser back command
 */
fun ConnectionManager.sendBrowserBack(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_BACK)

/**
 * Extension function to send browser forward command
 */
fun ConnectionManager.sendBrowserForward(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_FORWARD)

/**
 * Extension function to send browser refresh command
 */
fun ConnectionManager.sendBrowserRefresh(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_REFRESH)

/**
 * Extension function to send browser home command
 */
fun ConnectionManager.sendBrowserHome(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_HOME)

// ==================== HEARTBEAT EXTENSIONS ====================

/**
 * Extension function to send ping
 */
fun ConnectionManager.sendPing(): Boolean {
    return send(JSONObject().putPing().toString())
}

/**
 * Extension function to send pong
 */
fun ConnectionManager.sendPong(): Boolean {
    return send(JSONObject().putPong().toString())
}

// ==================== SYSTEM KEY EXTENSIONS ====================

/**
 * Extension function to send key press via key code
 * @param keyCode Android KeyEvent code
 */
fun ConnectionManager.sendKeyPress(keyCode: Int): Boolean {
    val keyMap = mapOf(
        android.view.KeyEvent.KEYCODE_HOME to MessageTypes.COMMAND_SHOW_DESKTOP,
        android.view.KeyEvent.KEYCODE_BACK to MessageTypes.COMMAND_BROWSER_BACK,
        android.view.KeyEvent.KEYCODE_VOLUME_UP to MessageTypes.COMMAND_VOLUME_UP,
        android.view.KeyEvent.KEYCODE_VOLUME_DOWN to MessageTypes.COMMAND_VOLUME_DOWN,
        android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to MessageTypes.COMMAND_PLAY_PAUSE,
        android.view.KeyEvent.KEYCODE_MEDIA_NEXT to MessageTypes.COMMAND_NEXT_TRACK,
        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS to MessageTypes.COMMAND_PREV_TRACK,
        android.view.KeyEvent.KEYCODE_MEDIA_STOP to MessageTypes.COMMAND_STOP
    )
    val command = keyMap[keyCode] ?: return false
    return sendControlCommand(command)
}

// ==================== BATCH SEND EXTENSIONS ====================

/**
 * Extension function to send multiple commands in batch
 * @param commands List of command strings
 */
fun ConnectionManager.sendBatch(commands: List<String>): Boolean {
    var allSent = true
    commands.forEach { command ->
        if (!sendControlCommand(command)) {
            allSent = false
        }
    }
    return allSent
}

/**
 * Extension function to send a JSON object directly
 */
fun ConnectionManager.sendJson(jsonObject: JSONObject): Boolean {
    return send(jsonObject.toString())
}

// ==================== UTILITY EXTENSIONS ====================

/**
 * Extension function to get message type from JSON
 */
fun JSONObject.getMessageType(): String? {
    return optString("type", null)
}

/**
 * Extension function to check if JSON has payload
 */
fun JSONObject.hasPayload(): Boolean {
    return has("payload")
}

/**
 * Extension function to get payload as JSONObject
 */
fun JSONObject.getPayload(): JSONObject? {
    return optJSONObject("payload")
}

/**
 * Extension function to check if message is a control command
 */
fun JSONObject.isControlCommand(): Boolean {
    return optString("type") == MessageTypes.TYPE_CONTROL
}

/**
 * Extension function to get control command
 */
fun JSONObject.getControlCommand(): String? {
    return getPayload()?.optString("command")
}

/**
 * Extension function to check if message is a gesture
 */
fun JSONObject.isGesture(): Boolean {
    return optString("type") == MessageTypes.TYPE_GESTURE
}

/**
 * Extension function to get gesture name
 */
fun JSONObject.getGestureName(): String? {
    return getPayload()?.optString("gesture")
}

/**
 * Extension function to get gesture confidence
 */
fun JSONObject.getGestureConfidence(): Float {
    return getPayload()?.optFloat("confidence") ?: 0f
}

/**
 * Extension function to check if proximity update
 */
fun JSONObject.isProximity(): Boolean {
    return optString("type") == MessageTypes.TYPE_PROXIMITY
}

/**
 * Extension function to get proximity distance
 */
fun JSONObject.getProximityDistance(): Float {
    return getPayload()?.optFloat("distance") ?: 0f
}

/**
 * Extension function to get proximity near status
 */
fun JSONObject.getProximityIsNear(): Boolean {
    return getPayload()?.optBoolean("is_near") ?: false
}