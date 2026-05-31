// app/src/main/java/com/airmouse/domain/model/MouseEvent.kt
package com.airmouse.domain.model

/**
 * Represents a mouse action to be sent to the server.
 */
sealed class MouseEvent {
    /** Movement delta (dx, dy) */
    data class Move(val dx: Float, val dy: Float) : MouseEvent()

    /** Left click */
    object Click : MouseEvent()

    /** Double click */
    object DoubleClick : MouseEvent()

    /** Right click */
    object RightClick : MouseEvent()

    /** Scroll (positive delta = scroll up, negative = scroll down) */
    data class Scroll(val delta: Int) : MouseEvent()
}