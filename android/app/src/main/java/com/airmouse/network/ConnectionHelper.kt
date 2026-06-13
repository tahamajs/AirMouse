// app/src/main/java/com/airmouse/network/ConnectionHelper.kt
package com.airmouse.network

import org.json.JSONObject

// Extension functions for easier message construction
fun JSONObject.putMove(dx: Float, dy: Float): JSONObject = apply {
    put("type", "move")
    put("dx", dx)
    put("dy", dy)
}

fun JSONObject.putClick(button: String): JSONObject = apply {
    put("type", "click")
    put("button", button)
}

fun JSONObject.putDoubleClick(): JSONObject = apply {
    put("type", "doubleclick")
}

fun JSONObject.putRightClick(): JSONObject = apply {
    put("type", "rightclick")
}

fun JSONObject.putScroll(delta: Int): JSONObject = apply {
    put("type", "scroll")
    put("delta", delta)
}

fun JSONObject.putHello(name: String, version: String): JSONObject = apply {
    put("type", "hello")
    put("payload", JSONObject().apply {
        put("name", name)
        put("version", version)
    })
}

fun JSONObject.putGesture(gesture: String, confidence: Float): JSONObject = apply {
    put("type", "gesture")
    put("payload", JSONObject().apply {
        put("gesture", gesture)
        put("confidence", confidence)
    })
}

fun JSONObject.putProximity(isNear: Boolean, distance: Float, deviceId: String): JSONObject = apply {
    put("type", "proximity")
    put("payload", JSONObject().apply {
        put("device_id", deviceId)
        put("is_near", isNear)
        put("distance", distance)
    })
}

fun JSONObject.putControl(command: String): JSONObject = apply {
    put("type", "control")
    put("payload", JSONObject().apply {
        put("command", command)
    })
}

fun JSONObject.putPing(): JSONObject = apply {
    put("type", "ping")
}

fun JSONObject.putPong(): JSONObject = apply {
    put("type", "pong")
}