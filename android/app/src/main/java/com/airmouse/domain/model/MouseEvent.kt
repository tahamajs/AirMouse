// app/src/main/java/com/airmouse/domain/model/MouseEvent.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a mouse action to be sent to the server.
 */
sealed class MouseEvent : Parcelable {

    /** Movement delta (dx, dy) */
    @Parcelize
    data class Move(val dx: Float, val dy: Float) : MouseEvent()

    /** Click event */
    @Parcelize
    data class Click(val button: MouseButton = MouseButton.LEFT, val id: Int? = null) : MouseEvent()

    /** Double click */
    @Parcelize
    object DoubleClick : MouseEvent()

    /** Right click */
    @Parcelize
    object RightClick : MouseEvent()

    /** Scroll (positive delta = scroll up, negative = scroll down) */
    @Parcelize
    data class Scroll(val delta: Int, val id: Int? = null) : MouseEvent()

    /** Gesture event */
    @Parcelize
    data class Gesture(val name: String, val confidence: Float) : MouseEvent()

    /** Proximity event */
    @Parcelize
    data class Proximity(val isNear: Boolean, val distance: Float) : MouseEvent()

    /** Control command */
    @Parcelize
    data class Control(val command: String) : MouseEvent()

    /** Key press */
    @Parcelize
    data class KeyPress(val keyCode: Int, val keyChar: Char? = null) : MouseEvent()
}

enum class MouseButton(val value: String) {
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle")
}// app/src/main/java/com/airmouse/domain/model/MouseEvent.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a mouse action to be sent to the server.
 */
sealed class MouseEvent : Parcelable {

    /** Movement delta (dx, dy) */
    @Parcelize
    data class Move(val dx: Float, val dy: Float) : MouseEvent()

    /** Click event */
    @Parcelize
    data class Click(val button: MouseButton = MouseButton.LEFT, val id: Int? = null) : MouseEvent()

    /** Double click */
    @Parcelize
    object DoubleClick : MouseEvent()

    /** Right click */
    @Parcelize
    object RightClick : MouseEvent()

    /** Scroll (positive delta = scroll up, negative = scroll down) */
    @Parcelize
    data class Scroll(val delta: Int, val id: Int? = null) : MouseEvent()

    /** Gesture event */
    @Parcelize
    data class Gesture(val name: String, val confidence: Float) : MouseEvent()

    /** Proximity event */
    @Parcelize
    data class Proximity(val isNear: Boolean, val distance: Float) : MouseEvent()

    /** Control command */
    @Parcelize
    data class Control(val command: String) : MouseEvent()

    /** Key press */
    @Parcelize
    data class KeyPress(val keyCode: Int, val keyChar: Char? = null) : MouseEvent()
}

enum class MouseButton(val value: String) {
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle")
}