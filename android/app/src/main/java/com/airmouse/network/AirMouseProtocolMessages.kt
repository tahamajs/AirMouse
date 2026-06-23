package com.airmouse.network

import org.json.JSONObject

object AirMouseProtocolMessages {
    fun move(dx: Float, dy: Float): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_MOVE)
        put("dx", dx)
        put("dy", dy)
        put("DeltaX", dx)
        put("DeltaY", dy)
    }.toString()

    fun reliableClick(type: String, id: String, button: String): String = JSONObject().apply {
        put("type", type)
        put("id", id)
        put("button", button)
        put("Click", button)
    }.toString()

    fun reliableScroll(id: String, delta: Int): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_SCROLL)
        put("id", id)
        put("delta", delta)
        put("Scroll", delta)
    }.toString()

    fun gesture(gesture: String, confidence: Float): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_GESTURE)
        put("payload", JSONObject().apply {
            put("gesture", gesture)
            put("confidence", confidence)
        })
    }.toString()

    fun proximity(deviceId: String, isNear: Boolean, distance: Float): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_PROXIMITY)
        put("payload", JSONObject().apply {
            put("device_id", deviceId)
            put("is_near", isNear)
            put("distance", distance)
        })
    }.toString()

    fun control(command: String): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_CONTROL)
        put("payload", JSONObject().apply {
            put("command", command)
        })
    }.toString()

    fun ping(): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_PING)
    }.toString()

    fun pong(): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_PONG)
    }.toString()

    fun hello(
        name: String,
        version: String,
        device: String,
        androidVersion: String,
        model: String? = null,
        manufacturer: String? = null,
        brand: String? = null,
        deviceName: String? = null,
        sdkInt: String? = null,
        deviceId: String? = null,
        protocol: String,
        transport: String,
        authToken: String? = null
    ): String = JSONObject().apply {
        put("type", ConnectionManager.MessageTypes.TYPE_HELLO)
        put("payload", JSONObject().apply {
            put("name", name)
            put("version", version)
            put("device", device)
            put("android_version", androidVersion)
            model?.let { put("model", it) }
            manufacturer?.let { put("manufacturer", it) }
            brand?.let { put("brand", it) }
            deviceName?.let { put("device_name", it) }
            sdkInt?.let { put("sdk_int", it) }
            deviceId?.let { put("device_id", it) }
            put("protocol", protocol)
            put("transport", transport)
            authToken?.takeIf { it.isNotBlank() }?.let { put("token", it) }
        })
    }.toString()
}
