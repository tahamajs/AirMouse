package com.airmouse.domain.model

import org.json.JSONObject

enum class GestureType {
    NONE, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN,
    CIRCLE_CW, CIRCLE_CCW, DOUBLE_TAP, LONG_PRESS, CUSTOM
}

data class GestureTemplate(
    val id: String = "",
    val name: String = "",
    val type: GestureType = GestureType.CUSTOM,
    val data: ByteArray = byteArrayOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("data", data.toString()) // In real code, use Base64 encoding
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GestureTemplate

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (!data.contentEquals(other.data)) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }

    companion object {
        fun fromJson(json: JSONObject): GestureTemplate? {
            return try {
                GestureTemplate(
                    id = json.optString("id", ""),
                    name = json.optString("name", ""),
                    type = try {
                        GestureType.valueOf(json.optString("type", "CUSTOM"))
                    } catch (_: Exception) {
                        GestureType.CUSTOM
                    },
                    data = json.optString("data", "").toByteArray(), // In real code, decode Base64
                    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}