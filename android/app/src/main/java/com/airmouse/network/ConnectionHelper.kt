
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



fun ConnectionManager.sendMove(dx: Float, dy: Float): Boolean {
    return send(JSONObject().putMove(dx, dy).toString())
}

fun ConnectionManager.sendClick(button: String = MessageTypes.BUTTON_LEFT, id: String? = null): Boolean {
    return send(JSONObject().putClick(button, id).toString())
}

fun ConnectionManager.sendProximity(isNear: Boolean, distance: Float, deviceId: String): Boolean {
    return send(JSONObject().putProximity(isNear, distance, deviceId).toString())
}

fun ConnectionManager.sendControlCommand(command: String): Boolean {
    return send(JSONObject().putControl(command).toString())
}

fun ConnectionManager.sendShowDesktop(): Boolean = sendControlCommand(MessageTypes.COMMAND_SHOW_DESKTOP)

fun ConnectionManager.sendTaskView(): Boolean = sendControlCommand(MessageTypes.COMMAND_TASK_VIEW)

fun ConnectionManager.sendWindowMaximize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MAXIMIZE)

fun ConnectionManager.sendWindowMinimize(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_MINIMIZE)

fun ConnectionManager.sendWindowClose(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_CLOSE)

fun ConnectionManager.sendWindowFullscreen(): Boolean = sendControlCommand(MessageTypes.COMMAND_WINDOW_FULLSCREEN)


fun ConnectionManager.sendJson(jsonObject: JSONObject): Boolean {
    return send(jsonObject.toString())
}



fun JSONObject.getMessageType(): String? {
    return if (has("type") && !isNull("type")) getString("type") else null
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
    return if (has("id") && !isNull("id")) getString("id") else null
}

fun JSONObject.getAckStatus(): String {
    return optString("status", "ok")
}