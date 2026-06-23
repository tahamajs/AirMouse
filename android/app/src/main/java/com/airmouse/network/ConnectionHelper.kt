
package com.airmouse.network

import android.view.KeyEvent
import org.json.JSONObject



fun JSONObject.putMove(dx: Float, dy: Float): JSONObject = apply {
    put("type", MessageTypes.TYPE_MOVE)
    put("dx", dx)
    put("dy", dy)
    
    put("DeltaX", dx)
    put("DeltaY", dy)
}

fun JSONObject.putClick(button: String, id: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_CLICK)
    put("button", button)
    id?.let { put("id", it) }
}

fun JSONObject.putDoubleClick(id: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_DOUBLE_CLICK)
    id?.let { put("id", it) }
}

fun JSONObject.putRightClick(id: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_RIGHT_CLICK)
    id?.let { put("id", it) }
}

fun JSONObject.putScroll(delta: Int, id: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_SCROLL)
    put("delta", delta)
    put("Scroll", delta)
    id?.let { put("id", it) }
}



fun JSONObject.putHello(
    name: String,
    version: String = MessageTypes.PROTOCOL_VERSION,
    device: String? = null,
    androidVersion: String? = null
): JSONObject = apply {
    put("type", MessageTypes.TYPE_HELLO)
    val payload = JSONObject().apply {
        put("name", name)
        put("version", version)
        device?.let { put("device", it) }
        androidVersion?.let { put("android_version", it) }
    }
    put("payload", payload)
}



fun JSONObject.putGesture(gesture: String, confidence: Float): JSONObject = apply {
    put("type", MessageTypes.TYPE_GESTURE)
    val payload = JSONObject().apply {
        put("gesture", gesture)
        put("confidence", confidence)
    }
    put("payload", payload)
}



fun JSONObject.putProximity(isNear: Boolean, distance: Float, deviceId: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_PROXIMITY)
    val payload = JSONObject().apply {
        put("device_id", deviceId)
        put("is_near", isNear)
        put("distance", distance)
    }
    put("payload", payload)
}



fun JSONObject.putControl(command: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_CONTROL)
    val payload = JSONObject().apply {
        put("command", command)
    }
    put("payload", payload)
}



fun JSONObject.putPing(): JSONObject = apply {
    put("type", MessageTypes.TYPE_PING)
}

fun JSONObject.putPong(): JSONObject = apply {
    put("type", MessageTypes.TYPE_PONG)
}



fun JSONObject.putWelcome(serverName: String, version: String, clientId: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_WELCOME)
    val payload = JSONObject().apply {
        put("server", serverName)
        put("version", version)
        clientId?.let { put("id", it) }
    }
    put("payload", payload)
}

fun JSONObject.putAck(id: String, status: String = "ok", message: String? = null): JSONObject = apply {
    put("type", MessageTypes.TYPE_ACK)
    put("id", id)
    put("status", status)
    message?.let { put("message", it) }
}

fun JSONObject.putError(code: Int, message: String): JSONObject = apply {
    put("type", MessageTypes.TYPE_ERROR)
    val payload = JSONObject().apply {
        put("code", code)
        put("message", message)
    }
    put("payload", payload)
}



fun ConnectionManager.sendMove(dx: Float, dy: Float, id: String? = null): Boolean {
    return send(JSONObject().putMove(dx, dy).toString())
}

fun ConnectionManager.sendClick(button: String = MessageTypes.BUTTON_LEFT, id: String? = null): Boolean {
    return send(JSONObject().putClick(button, id).toString())
}

fun ConnectionManager.sendDoubleClick(): Boolean {
    return send(JSONObject().putDoubleClick().toString())
}

fun ConnectionManager.sendRightClick(): Boolean {
    return send(JSONObject().putRightClick().toString())
}

fun ConnectionManager.sendScroll(delta: Int): Boolean {
    return send(JSONObject().putScroll(delta).toString())
}

fun ConnectionManager.sendGesture(gesture: String, confidence: Float): Boolean {
    return send(JSONObject().putGesture(gesture, confidence).toString())
}

fun ConnectionManager.sendProximity(isNear: Boolean, distance: Float, deviceId: String): Boolean {
    return send(JSONObject().putProximity(isNear, distance, deviceId).toString())
}

fun ConnectionManager.sendControlCommand(command: String): Boolean {
    return send(JSONObject().putControl(command).toString())
}

fun ConnectionManager.sendPauseMovement(): Boolean = sendControlCommand(MessageTypes.COMMAND_PAUSE_MOVEMENT)

fun ConnectionManager.sendResumeMovement(): Boolean = sendControlCommand(MessageTypes.COMMAND_RESUME_MOVEMENT)

fun ConnectionManager.sendLockScreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_LOCK_SCREEN)

fun ConnectionManager.sendUnlockScreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_UNLOCK_SCREEN)

fun ConnectionManager.sendCalibrate(): Boolean = sendControlCommand(MessageTypes.COMMAND_CALIBRATE)

fun ConnectionManager.sendReset(): Boolean = sendControlCommand(MessageTypes.COMMAND_RESET)

fun ConnectionManager.sendShowDesktop(): Boolean = sendControlCommand(MessageTypes.COMMAND_SHOW_DESKTOP)

fun ConnectionManager.sendTaskView(): Boolean = sendControlCommand(MessageTypes.COMMAND_TASK_VIEW)



fun ConnectionManager.sendPlayPause(): Boolean = sendControlCommand(MessageTypes.COMMAND_PLAY_PAUSE)

fun ConnectionManager.sendNextTrack(): Boolean = sendControlCommand(MessageTypes.COMMAND_NEXT_TRACK)

fun ConnectionManager.sendPrevTrack(): Boolean = sendControlCommand(MessageTypes.COMMAND_PREV_TRACK)

fun ConnectionManager.sendStop(): Boolean = sendControlCommand(MessageTypes.COMMAND_STOP)

fun ConnectionManager.sendVolumeUp(): Boolean = sendControlCommand(MessageTypes.COMMAND_VOLUME_UP)

fun ConnectionManager.sendVolumeDown(): Boolean = sendControlCommand(MessageTypes.COMMAND_VOLUME_DOWN)

fun ConnectionManager.sendMute(): Boolean = sendControlCommand(MessageTypes.COMMAND_MUTE)



fun ConnectionManager.sendWindowMaximize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MAXIMIZE)

fun ConnectionManager.sendWindowMinimize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MINIMIZE)

fun ConnectionManager.sendWindowClose(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_CLOSE)

fun ConnectionManager.sendWindowFullscreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_FULLSCREEN)



fun ConnectionManager.sendBrowserBack(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_BACK)

fun ConnectionManager.sendBrowserForward(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_FORWARD)

fun ConnectionManager.sendBrowserRefresh(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_REFRESH)

fun ConnectionManager.sendBrowserHome(): Boolean = sendControlCommand(MessageTypes.COMMAND_BROWSER_HOME)



fun ConnectionManager.sendPing(): Boolean {
    return send(JSONObject().putPing().toString())
}

fun ConnectionManager.sendPong(): Boolean {
    return send(JSONObject().putPong().toString())
}



fun ConnectionManager.sendKeyPress(keyCode: Int): Boolean {
    val command = when (keyCode) {
        KeyEvent.KEYCODE_HOME -> MessageTypes.COMMAND_SHOW_DESKTOP
        KeyEvent.KEYCODE_BACK -> MessageTypes.COMMAND_BROWSER_BACK
        KeyEvent.KEYCODE_VOLUME_UP -> MessageTypes.COMMAND_VOLUME_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> MessageTypes.COMMAND_VOLUME_DOWN
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MessageTypes.COMMAND_PLAY_PAUSE
        KeyEvent.KEYCODE_MEDIA_NEXT -> MessageTypes.COMMAND_NEXT_TRACK
        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MessageTypes.COMMAND_PREV_TRACK
        KeyEvent.KEYCODE_MEDIA_STOP -> MessageTypes.COMMAND_STOP
        KeyEvent.KEYCODE_MUTE -> MessageTypes.COMMAND_MUTE
        else -> return false
    }
    return sendControlCommand(command)
}



fun ConnectionManager.sendBatch(commands: List<String>): Boolean {
    var allSent = true
    commands.forEach { command ->
        if (!sendControlCommand(command)) {
            allSent = false
        }
    }
    return allSent
}

fun ConnectionManager.sendJson(jsonObject: JSONObject): Boolean {
    return send(jsonObject.toString())
}



fun JSONObject.getMessageType(): String? {
    return optString("type", null)
}

fun JSONObject.hasPayload(): Boolean {
    return has("payload")
}

fun JSONObject.getPayload(): JSONObject? {
    return optJSONObject("payload")
}

fun JSONObject.isControlCommand(): Boolean {
    return optString("type") == MessageTypes.TYPE_CONTROL
}

fun JSONObject.getControlCommand(): String? {
    return getPayload()?.optString("command")
}

fun JSONObject.isGesture(): Boolean {
    return optString("type") == MessageTypes.TYPE_GESTURE
}

fun JSONObject.getGestureName(): String? {
    return getPayload()?.optString("gesture")
}

fun JSONObject.getGestureConfidence(): Float {
    return getPayload()?.optDouble("confidence")?.toFloat() ?: 0f
}

fun JSONObject.isProximity(): Boolean {
    return optString("type") == MessageTypes.TYPE_PROXIMITY
}

fun JSONObject.getProximityDistance(): Float {
    return getPayload()?.optDouble("distance")?.toFloat() ?: 0f
}

fun JSONObject.getProximityIsNear(): Boolean {
    return getPayload()?.optBoolean("is_near") ?: false
}

fun JSONObject.isPing(): Boolean {
    return optString("type") == MessageTypes.TYPE_PING
}

fun JSONObject.isPong(): Boolean {
    return optString("type") == MessageTypes.TYPE_PONG
}

fun JSONObject.isWelcome(): Boolean {
    return optString("type") == MessageTypes.TYPE_WELCOME
}

fun JSONObject.getServerName(): String? {
    return getPayload()?.optString("server")
}

fun JSONObject.getServerVersion(): String? {
    return getPayload()?.optString("version")
}

fun JSONObject.isAck(): Boolean {
    return optString("type") == MessageTypes.TYPE_ACK
}

fun JSONObject.getAckId(): String? {
    return optString("id", null)
}

fun JSONObject.getAckStatus(): String {
    return optString("status", "ok")
}