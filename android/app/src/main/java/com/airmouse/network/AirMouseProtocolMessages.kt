package com.airmouse.network

import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

object AirMouseProtocolMessages {
    private val seqCounter = AtomicLong(0L)

    private fun nextSeq(): Long = seqCounter.incrementAndGet()

    fun move(dx: Float, dy: Float, dt: Float? = null, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_MOVE)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject().apply {
            put("dx", dx)
            put("dy", dy)
            if (dt != null) {
                put("dt", dt)
            }
        })
    }.toString()

    fun reliableClick(type: String, id: String, button: String, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", type)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", id)
        put("payload", JSONObject().apply {
            put("button", button)
        })
    }.toString()

    fun reliableScroll(id: String, delta: Double, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_SCROLL)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", id)
        put("payload", JSONObject().apply {
            put("delta", delta)
        })
    }.toString()

    fun gesture(gesture: String, confidence: Float, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_GESTURE)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject().apply {
            put("gesture", gesture)
            put("confidence", confidence)
        })
    }.toString()

    fun proximity(deviceId: String, isNear: Boolean, distance: Float, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_PROXIMITY)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject().apply {
            put("device_id", deviceId)
            put("is_near", isNear)
            put("distance", distance)
        })
    }.toString()

    fun control(command: String, seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_CONTROL)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject().apply {
            put("command", command)
        })
    }.toString()

    fun ping(seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_PING)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject())
    }.toString()

    fun pong(seq: Long = nextSeq()): String = JSONObject().apply {
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_PONG)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", "")
        put("payload", JSONObject())
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
        authToken: String? = null,
        seq: Long = nextSeq()
    ): String = JSONObject().apply {
        val validDeviceId = if (deviceId.isNullOrBlank()) "android-device" else deviceId
        val msgId = "hello-$seq"
        put("v", 2)
        put("type", ConnectionManager.MessageTypes.TYPE_HELLO)
        put("seq", seq)
        put("ts", System.currentTimeMillis())
        put("id", msgId)
        put("payload", JSONObject().apply {
            put("device_id", validDeviceId)
            put("name", name.ifBlank { "AirMouse Android" })
            put("version", version.ifBlank { "2.0.0" })
            put("token", authToken ?: "")
            put("device", device)
            put("android_version", androidVersion)
            model?.let { put("model", it) }
            manufacturer?.let { put("manufacturer", it) }
            brand?.let { put("brand", it) }
            deviceName?.let { put("device_name", it) }
            sdkInt?.let { put("sdk_int", it) }
            put("protocol", protocol)
            put("transport", transport)
        })
    }.toString()
}
