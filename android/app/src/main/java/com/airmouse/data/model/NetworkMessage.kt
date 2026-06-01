// app/src/main/java/com/airmouse/data/model/NetworkMessage.kt
package com.airmouse.data.model

import org.json.JSONObject

/**
 * DTO for messages sent to/received from the server.
 */
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
        val version: String = "3.0"
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
                        Hello(p.optString("name"), p.optString("version", "3.0"))
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
                    jsonObject.put("name", message.name)
                    jsonObject.put("version", message.version)
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
                Ping -> jsonObject.put("type", "ping")
                Pong -> jsonObject.put("type", "pong")
            }
            return jsonObject.toString()
        }
    }
}