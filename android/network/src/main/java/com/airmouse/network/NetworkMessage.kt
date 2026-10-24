package com.airmouse.network

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class NetworkMessage {
    data class Move(
        val dx: Float,
        val dy: Float,
        val dt: Float? = null
    ) : NetworkMessage()

    data class Click(
        val button: String = "left"
    ) : NetworkMessage()

    data object DoubleClick : NetworkMessage()

    data object RightClick : NetworkMessage()

    data class Scroll(
        val delta: Int
    ) : NetworkMessage()

    data class Key(
        val key: String,
        val modifiers: List<String> = emptyList()
    ) : NetworkMessage()

    data class Hello(
        val name: String,
        val version: String = "2.0.0",
        val deviceId: String? = null,
        val token: String? = null,
        val device: String? = null,
        val androidVersion: String? = null,
        val model: String? = null,
        val manufacturer: String? = null,
        val brand: String? = null,
        val deviceName: String? = null,
        val sdkInt: String? = null,
        val protocol: String = "2.0.0",
        val transport: String = "WEBSOCKET",
        val authToken: String? = null
    ) : NetworkMessage()

    data class Welcome(
        val server: String,
        val version: String,
        val clientId: String? = null,
        val approved: Boolean = true
    ) : NetworkMessage()

    data class Ack(
        val ackId: String,
        val status: String = "ok"
    ) : NetworkMessage()

    data class Nack(
        val nackId: String? = null,
        val error: String = "",
        val expectedSeq: Long? = null,
        val missingFrom: Long? = null,
        val missingTo: Long? = null
    ) : NetworkMessage()

    data class Proximity(
        val isNear: Boolean,
        val distance: Float,
        val deviceId: String? = null
    ) : NetworkMessage()

    data class Gesture(
        val gesture: String,
        val confidence: Float
    ) : NetworkMessage()

    data class Control(
        val command: String,
        val value: Int? = null
    ) : NetworkMessage()

    data object Ping : NetworkMessage()
    data object Pong : NetworkMessage()

    companion object {
        const val PROTOCOL_VERSION = 2

        fun fromJson(json: String): NetworkMessage? {
            return try {
                val jsonObject = JSONObject(json)
                val type = jsonObject.optString("type", "")
                val payload = jsonObject.optJSONObject("payload") ?: jsonObject
                when (type) {
                    "move" -> {
                        val dt = if (payload.has("dt")) payload.optDouble("dt").toFloat() else null
                        Move(
                            dx = payload.optDouble("dx").toFloat(),
                            dy = payload.optDouble("dy").toFloat(),
                            dt = dt
                        )
                    }
                    "click" -> Click(payload.optString("button", "left"))
                    "doubleclick" -> DoubleClick
                    "rightclick" -> RightClick
                    "scroll" -> Scroll(payload.optInt("delta", 0))
                    "key" -> {
                        val key = payload.optString("key", "")
                        val mods = mutableListOf<String>()
                        payload.optJSONArray("modifiers")?.let { arr ->
                            for (i in 0 until arr.length()) {
                                mods.add(arr.getString(i))
                            }
                        }
                        Key(key, mods)
                    }
                    "hello" -> {
                        val token = payload.optString("token").takeIf { it.isNotBlank() }
                            ?: payload.optString("auth_token").takeIf { it.isNotBlank() }
                        Hello(
                            name = payload.optString("name"),
                            version = payload.optString("version", "2.0.0"),
                            deviceId = payload.optString("device_id").takeIf { it.isNotBlank() },
                            token = token,
                            device = payload.optString("device").takeIf { it.isNotBlank() },
                            androidVersion = payload.optString("android_version").takeIf { it.isNotBlank() },
                            model = payload.optString("model").takeIf { it.isNotBlank() },
                            manufacturer = payload.optString("manufacturer").takeIf { it.isNotBlank() },
                            brand = payload.optString("brand").takeIf { it.isNotBlank() },
                            deviceName = payload.optString("device_name").takeIf { it.isNotBlank() },
                            sdkInt = payload.optString("sdk_int").takeIf { it.isNotBlank() },
                            protocol = payload.optString("protocol", "2.0.0"),
                            transport = payload.optString("transport", "WEBSOCKET"),
                            authToken = token
                        )
                    }
                    "welcome" -> Welcome(
                        server = payload.optString("server", ""),
                        version = payload.optString("version", "2.0.0"),
                        clientId = payload.optString("client_id").takeIf { it.isNotBlank() },
                        approved = payload.optBoolean("approved", true)
                    )
                    "ack" -> {
                        val ackId = if (payload.has("ack_id")) {
                            payload.optString("ack_id")
                        } else if (payload.has("id")) {
                            payload.optString("id")
                        } else if (jsonObject.has("id")) {
                            jsonObject.optString("id")
                        } else {
                            ""
                        }
                        Ack(ackId = ackId, status = payload.optString("status", "ok"))
                    }
                    "nack" -> {
                        Nack(
                            nackId = payload.optString("nack_id").takeIf { it.isNotBlank() },
                            error = payload.optString("error", ""),
                            expectedSeq = if (payload.has("expected_seq")) payload.optLong("expected_seq") else null,
                            missingFrom = if (payload.has("missing_from")) payload.optLong("missing_from") else null,
                            missingTo = if (payload.has("missing_to")) payload.optLong("missing_to") else null
                        )
                    }
                    "proximity" -> {
                        Proximity(
                            isNear = payload.optBoolean("is_near", false),
                            distance = payload.optDouble("distance", 0.0).toFloat(),
                            deviceId = payload.optString("device_id").takeIf { it.isNotBlank() }
                        )
                    }
                    "gesture" -> Gesture(
                        gesture = payload.optString("gesture"),
                        confidence = payload.optDouble("confidence", 0.0).toFloat()
                    )
                    "control" -> Control(
                        command = payload.optString("command"),
                        value = if (payload.has("value")) payload.optInt("value") else null
                    )
                    "ping" -> Ping
                    "pong" -> Pong
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        fun toJson(
            message: NetworkMessage,
            seq: Long = 0L,
            ts: Long = System.currentTimeMillis(),
            id: String? = null
        ): String {
            val envelope = JSONObject()
            envelope.put("v", PROTOCOL_VERSION)
            envelope.put("seq", seq)
            envelope.put("ts", ts)

            val payload = JSONObject()

            when (message) {
                is Move -> {
                    envelope.put("type", "move")
                    payload.put("dx", message.dx)
                    payload.put("dy", message.dy)
                    message.dt?.let { payload.put("dt", it) }
                }
                is Click -> {
                    envelope.put("type", "click")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("button", message.button)
                }
                DoubleClick -> {
                    envelope.put("type", "click")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("button", "double")
                }
                RightClick -> {
                    envelope.put("type", "click")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("button", "right")
                }
                is Scroll -> {
                    envelope.put("type", "scroll")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("delta", message.delta)
                }
                is Key -> {
                    envelope.put("type", "key")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("key", message.key)
                    if (message.modifiers.isNotEmpty()) {
                        val arr = JSONArray()
                        message.modifiers.forEach { arr.put(it) }
                        payload.put("modifiers", arr)
                    }
                }
                is Hello -> {
                    envelope.put("type", "hello")
                    val messageId = id ?: UUID.randomUUID().toString()
                    envelope.put("id", messageId)
                    payload.put("name", message.name)
                    payload.put("version", message.version)
                    val effectiveToken = message.token ?: message.authToken
                    effectiveToken?.takeIf { it.isNotBlank() }?.let { payload.put("token", it) }
                    message.deviceId?.takeIf { it.isNotBlank() }?.let { payload.put("device_id", it) }
                    message.device?.takeIf { it.isNotBlank() }?.let { payload.put("device", it) }
                    message.androidVersion?.takeIf { it.isNotBlank() }?.let { payload.put("android_version", it) }
                    message.model?.takeIf { it.isNotBlank() }?.let { payload.put("model", it) }
                    message.manufacturer?.takeIf { it.isNotBlank() }?.let { payload.put("manufacturer", it) }
                    message.brand?.takeIf { it.isNotBlank() }?.let { payload.put("brand", it) }
                    message.deviceName?.takeIf { it.isNotBlank() }?.let { payload.put("device_name", it) }
                    message.sdkInt?.takeIf { it.isNotBlank() }?.let { payload.put("sdk_int", it) }
                    payload.put("protocol", message.protocol)
                    payload.put("transport", message.transport)
                }
                is Welcome -> {
                    envelope.put("type", "welcome")
                    id?.let { envelope.put("id", it) }
                    payload.put("server", message.server)
                    payload.put("version", message.version)
                    message.clientId?.let { payload.put("client_id", it) }
                    payload.put("approved", message.approved)
                }
                is Ack -> {
                    envelope.put("type", "ack")
                    payload.put("ack_id", message.ackId)
                    payload.put("status", message.status)
                }
                is Nack -> {
                    envelope.put("type", "nack")
                    message.nackId?.let { payload.put("nack_id", it) }
                    payload.put("error", message.error)
                    message.expectedSeq?.let { payload.put("expected_seq", it) }
                    message.missingFrom?.let { payload.put("missing_from", it) }
                    message.missingTo?.let { payload.put("missing_to", it) }
                }
                is Proximity -> {
                    envelope.put("type", "proximity")
                    id?.let { envelope.put("id", it) }
                    payload.put("is_near", message.isNear)
                    payload.put("distance", message.distance)
                    message.deviceId?.let { payload.put("device_id", it) }
                }
                is Gesture -> {
                    envelope.put("type", "gesture")
                    id?.let { envelope.put("id", it) }
                    payload.put("gesture", message.gesture)
                    payload.put("confidence", message.confidence)
                }
                is Control -> {
                    envelope.put("type", "control")
                    id?.let { envelope.put("id", it) }
                    payload.put("command", message.command)
                    message.value?.let { payload.put("value", it) }
                }
                Ping -> {
                    envelope.put("type", "ping")
                }
                Pong -> {
                    envelope.put("type", "pong")
                }
            }

            envelope.put("payload", payload)
            return envelope.toString()
        }
    }
}
