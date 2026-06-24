package com.airmouse.network

import org.json.JSONObject

sealed class NetworkMessage {
    data class Move(
        val dx: Float,
        val dy: Float
    ) : NetworkMessage()

    data class Click(
        val button: String = "left"
    ) : NetworkMessage()

    object DoubleClick : NetworkMessage()

    object RightClick : NetworkMessage()

    data class Scroll(
        val delta: Int
    ) : NetworkMessage()

    data class Hello(
        val name: String,
        val version: String = "3.0",
        val device: String? = null,
        val androidVersion: String? = null,
        val model: String? = null,
        val manufacturer: String? = null,
        val brand: String? = null,
        val deviceName: String? = null,
        val sdkInt: String? = null,
        val deviceId: String? = null,
        val protocol: String = "3.0",
        val transport: String = "WEBSOCKET",
        val authToken: String? = null
    ) : NetworkMessage()

    data class Ack(
        val id: String
    ) : NetworkMessage()

    data class Proximity(
        val isNear: Boolean,
        val distance: Float
    ) : NetworkMessage()

    data class Gesture(
        val gesture: String,
        val confidence: Float
    ) : NetworkMessage()

    data class Control(
        val command: String,
        val value: Int? = null
    ) : NetworkMessage()

    object Ping : NetworkMessage()
    object Pong : NetworkMessage()

    companion object {
        fun fromJson(json: String): NetworkMessage? {
            return try {
                val jsonObject = JSONObject(json)
                val type = jsonObject.optString("type", "")
                fun payload() = jsonObject.optJSONObject("payload") ?: jsonObject
                when (type) {
                    "move" -> {
                        val p = payload()
                        Move(p.optDouble("dx").toFloat(), p.optDouble("dy").toFloat())
                    }
                    "click" -> {
                        val p = payload()
                        Click(p.optString("button", "left"))
                    }
                    "doubleclick" -> DoubleClick
                    "rightclick" -> RightClick
                    "scroll" -> {
                        val p = payload()
                        Scroll(p.optInt("delta", 0))
                    }
                    "hello" -> {
                        val p = payload()
                        Hello(
                            name = p.optString("name"),
                            version = p.optString("version", "3.0"),
                            device = p.optString("device").takeIf { it.isNotBlank() },
                            androidVersion = p.optString("android_version").takeIf { it.isNotBlank() },
                            model = p.optString("model").takeIf { it.isNotBlank() },
                            manufacturer = p.optString("manufacturer").takeIf { it.isNotBlank() },
                            brand = p.optString("brand").takeIf { it.isNotBlank() },
                            deviceName = p.optString("device_name").takeIf { it.isNotBlank() },
                            sdkInt = p.optString("sdk_int").takeIf { it.isNotBlank() },
                            deviceId = p.optString("device_id").takeIf { it.isNotBlank() },
                            protocol = p.optString("protocol", "3.0"),
                            transport = p.optString("transport", "WEBSOCKET"),
                            authToken = p.optString("token").takeIf { it.isNotBlank() }
                        )
                    }
                    "ack" -> {
                        val p = payload()
                        Ack(p.optString("id"))
                    }
                    "proximity" -> {
                        val p = payload()
                        Proximity(p.optBoolean("is_near", false), p.optDouble("distance", 0.0).toFloat())
                    }
                    "gesture" -> {
                        val p = payload()
                        Gesture(p.optString("gesture"), p.optDouble("confidence", 0.0).toFloat())
                    }
                    "control" -> {
                        val p = payload()
                        Control(p.optString("command"), if (p.has("value")) p.optInt("value") else null)
                    }
                    "ping" -> Ping
                    "pong" -> Pong
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        fun toJson(message: NetworkMessage): String {
            val jsonObject = JSONObject()
            when (message) {
                is Move -> {
                    jsonObject.put("type", "move")
                    jsonObject.put("dx", message.dx)
                    jsonObject.put("dy", message.dy)
                }
                is Click -> {
                    jsonObject.put("type", "click")
                    jsonObject.put("button", message.button)
                }
                DoubleClick -> jsonObject.put("type", "doubleclick")
                RightClick -> jsonObject.put("type", "rightclick")
                is Scroll -> {
                    jsonObject.put("type", "scroll")
                    jsonObject.put("delta", message.delta)
                }
                is Hello -> {
                    jsonObject.put("type", "hello")
                    val payload = JSONObject()
                    payload.put("name", message.name)
                    payload.put("version", message.version)
                    message.device?.let { payload.put("device", it) }
                    message.androidVersion?.let { payload.put("android_version", it) }
                    message.model?.let { payload.put("model", it) }
                    message.manufacturer?.let { payload.put("manufacturer", it) }
                    message.brand?.let { payload.put("brand", it) }
                    message.deviceName?.let { payload.put("device_name", it) }
                    message.sdkInt?.let { payload.put("sdk_int", it) }
                    message.deviceId?.let { payload.put("device_id", it) }
                    payload.put("protocol", message.protocol)
                    payload.put("transport", message.transport)
                    message.authToken?.takeIf { it.isNotBlank() }?.let { payload.put("token", it) }
                    jsonObject.put("payload", payload)
                }
                is Ack -> {
                    jsonObject.put("type", "ack")
                    jsonObject.put("id", message.id)
                }
                is Proximity -> {
                    jsonObject.put("type", "proximity")
                    jsonObject.put("is_near", message.isNear)
                    jsonObject.put("distance", message.distance)
                }
                is Gesture -> {
                    jsonObject.put("type", "gesture")
                    jsonObject.put("gesture", message.gesture)
                    jsonObject.put("confidence", message.confidence)
                }
                is Control -> {
                    jsonObject.put("type", "control")
                    jsonObject.put("command", message.command)
                    message.value?.let { jsonObject.put("value", it) }
                }
                Ping -> jsonObject.put("type", "ping")
                Pong -> jsonObject.put("type", "pong")
            }
            return jsonObject.toString()
        }
    }
}
