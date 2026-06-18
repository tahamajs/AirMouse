package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Represents a mouse action to be sent to the server.
 */
sealed class MouseEvent : Parcelable {

    /** Movement delta (dx, dy) */
    @Parcelize
    data class Move(val dx: Float, val dy: Float, val id: Int? = null) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "move")
                put("dx", dx)
                put("dy", dy)
                id?.let { put("id", it) }
            }.toString()
        }
    }

    /** Click event */
    @Parcelize
    data class Click(val button: MouseButton = MouseButton.LEFT, val id: Int? = null) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "click")
                put("button", button.value)
                id?.let { put("id", it) }
            }.toString()
        }
    }

    /** Double click */
    @Parcelize
    object DoubleClick : MouseEvent() {
        fun toJson(): String = """{"type":"doubleclick"}"""
    }

    /** Right click */
    @Parcelize
    object RightClick : MouseEvent() {
        fun toJson(): String = """{"type":"rightclick"}"""
    }

    /** Scroll (positive delta = scroll up, negative = scroll down) */
    @Parcelize
    data class Scroll(val delta: Int, val id: Int? = null) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "scroll")
                put("delta", delta)
                id?.let { put("id", it) }
            }.toString()
        }
    }

    /** Gesture event */
    @Parcelize
    data class Gesture(val name: String, val confidence: Float, val id: Int? = null) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "gesture")
                put("payload", JSONObject().apply {
                    put("gesture", name)
                    put("confidence", confidence)
                })
                id?.let { put("id", it) }
            }.toString()
        }
    }

    /** Proximity event */
    @Parcelize
    data class Proximity(val isNear: Boolean, val distance: Float, val deviceId: String = "unknown") : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "proximity")
                put("payload", JSONObject().apply {
                    put("device_id", deviceId)
                    put("is_near", isNear)
                    put("distance", distance)
                })
            }.toString()
        }
    }

    /** Control command */
    @Parcelize
    data class Control(val command: String, val id: Int? = null) : MouseEvent() {
        companion object {
            const val PAUSE_MOVEMENT = "pause_movement"
            const val RESUME_MOVEMENT = "resume_movement"
            const val LOCK_SCREEN = "lock_screen"
            const val UNLOCK_SCREEN = "unlock_screen"
            const val CALIBRATE = "calibrate"
            const val RESET = "reset"
        }

        fun toJson(): String {
            return JSONObject().apply {
                put("type", "control")
                put("payload", JSONObject().apply {
                    put("command", command)
                })
                id?.let { put("id", it) }
            }.toString()
        }
    }

    /** Key press */
    @Parcelize
    data class KeyPress(val keyCode: Int, val keyChar: Char? = null, val modifiers: List<String> = emptyList()) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "keypress")
                put("keyCode", keyCode)
                keyChar?.let { put("keyChar", it.toString()) }
                if (modifiers.isNotEmpty()) {
                    put("modifiers", modifiers.joinToString(","))
                }
            }.toString()
        }
    }

    /** Hello message (device identification) */
    @Parcelize
    data class Hello(val deviceName: String, val version: String, val deviceModel: String? = null) : MouseEvent() {
        fun toJson(): String {
            return JSONObject().apply {
                put("type", "hello")
                put("payload", JSONObject().apply {
                    put("name", deviceName)
                    put("version", version)
                    deviceModel?.let { put("device", it) }
                    put("android_version", android.os.Build.VERSION.RELEASE)
                })
            }.toString()
        }
    }

    /** Ping (keep-alive) */
    @Parcelize
    object Ping : MouseEvent() {
        fun toJson(): String = """{"type":"ping"}"""
    }

    /** Pong (response to ping) */
    @Parcelize
    object Pong : MouseEvent() {
        fun toJson(): String = """{"type":"pong"}"""
    }

    /** Converts the event to a JSON string for sending over the network */
    abstract fun toJson(): String
}

/** Mouse button enum */
enum class MouseButton(val value: String) {
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle"),
    BACK("back"),
    FORWARD("forward");

    companion object {
        fun fromValue(value: String): MouseButton {
            return values().find { it.value == value } ?: LEFT
        }
    }
}

/** Key modifier constants */
object KeyModifiers {
    const val CTRL = "ctrl"
    const val SHIFT = "shift"
    const val ALT = "alt"
    const val META = "meta"
}