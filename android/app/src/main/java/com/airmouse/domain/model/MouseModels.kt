
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    BACK,
    FORWARD
}

enum class ScrollDirection {
    UP,
    DOWN,
    NONE
}

sealed class MouseEvent {
    data class Move(val dx: Float, val dy: Float) : MouseEvent()
    data class Click(val button: MouseButton, val repeat: Int = 1) : MouseEvent()
    data class Scroll(val delta: Int, val direction: ScrollDirection) : MouseEvent()
    object DoubleClick : MouseEvent()
    object RightClick : MouseEvent()
    object MiddleClick : MouseEvent()
}

@Parcelize
data class MovementProfile(
    val sensitivity: Float = 1.0f,
    val smoothingEnabled: Boolean = true,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.5f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val swapAxes: Boolean = false,
    val deadband: Float = 0.5f,
    val maxSpeed: Float = 100f,
    val minSpeed: Float = 0.5f,
    val predictiveBlend: Float = 0.6f,
    val smoothingAlpha: Float = 0.3f
) : Parcelable

@Parcelize
data class MouseStatistics(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovement: Float = 0f,
    val movementCount: Int = 0,
    val averageSpeed: Float = 0f,
    val lastUpdated: Long = System.currentTimeMillis()
) : Parcelable