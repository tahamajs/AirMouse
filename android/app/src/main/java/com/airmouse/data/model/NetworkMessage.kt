// app/src/main/java/com/airmouse/data/model/NetworkMessage.kt
package com.airmouse.data.model

import com.google.gson.annotations.SerializedName

/**
 * DTO for messages sent to/received from the server.
 */
sealed class NetworkMessage {
    data class Move(
        @SerializedName("dx") val dx: Float,
        @SerializedName("dy") val dy: Float
    ) : NetworkMessage()

    data class Click(
        @SerializedName("button") val button: String = "left"
    ) : NetworkMessage()

    object DoubleClick : NetworkMessage()

    object RightClick : NetworkMessage()

    data class Scroll(
        @SerializedName("delta") val delta: Int
    ) : NetworkMessage()

    data class Hello(
        @SerializedName("name") val name: String,
        @SerializedName("version") val version: String = "3.0"
    ) : NetworkMessage()

    data class Ack(
        @SerializedName("id") val id: String
    ) : NetworkMessage()

    data class Proximity(
        @SerializedName("is_near") val isNear: Boolean,
        @SerializedName("distance") val distance: Float
    ) : NetworkMessage()

    data class Gesture(
        @SerializedName("gesture") val gesture: String,
        @SerializedName("confidence") val confidence: Float
    ) : NetworkMessage()

    object Ping : NetworkMessage()
    object Pong : NetworkMessage()

    companion object {
        fun fromJson(json: String): NetworkMessage? {
            return try {
                val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
                val type = jsonObject.get("type")?.asString ?: return null
                when (type) {
                    "move" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Move(payload.get("dx").asFloat, payload.get("dy").asFloat)
                    }
                    "click" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Click(payload.get("button").asString)
                    }
                    "doubleclick" -> DoubleClick
                    "rightclick" -> RightClick
                    "scroll" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Scroll(payload.get("delta").asInt)
                    }
                    "hello" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Hello(payload.get("name").asString, payload.get("version").asString)
                    }
                    "ack" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Ack(payload.get("id").asString)
                    }
                    "proximity" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Proximity(payload.get("is_near").asBoolean, payload.get("distance").asFloat)
                    }
                    "gesture" -> {
                        val payload = jsonObject.getAsJsonObject("payload")
                        Gesture(payload.get("gesture").asString, payload.get("confidence").asFloat)
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
            return when (message) {
                is Move -> """{"type":"move","payload":{"dx":${message.dx},"dy":${message.dy}}}"""
                is Click -> """{"type":"click","payload":{"button":"${message.button}"}}"""
                DoubleClick -> """{"type":"doubleclick"}"""
                RightClick -> """{"type":"rightclick"}"""
                is Scroll -> """{"type":"scroll","payload":{"delta":${message.delta}}}"""
                is Hello -> """{"type":"hello","payload":{"name":"${message.name}","version":"${message.version}"}}"""
                is Ack -> """{"type":"ack","payload":{"id":"${message.id}"}}"""
                is Proximity -> """{"type":"proximity","payload":{"is_near":${message.isNear},"distance":${message.distance}}}"""
                is Gesture -> """{"type":"gesture","payload":{"gesture":"${message.gesture}","confidence":${message.confidence}}}"""
                Ping -> """{"type":"ping"}"""
                Pong -> """{"type":"pong"}"""
            }
        }
    }
}