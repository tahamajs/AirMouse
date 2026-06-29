package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

/**
 * Mouse button types.
 */
enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE,
    BACK,
    FORWARD;

    /**
     * Get the display name for the button.
     */
    fun getDisplayName(): String {
        return when (this) {
            LEFT -> "Left Click"
            RIGHT -> "Right Click"
            MIDDLE -> "Middle Click"
            BACK -> "Back Button"
            FORWARD -> "Forward Button"
        }
    }
}

/**
 * Scroll direction.
 */
enum class ScrollDirection {
    UP,
    DOWN,
    NONE;

    /**
     * Get the integer delta value for this direction.
     */
    fun getDelta(): Int {
        return when (this) {
            UP -> 1
            DOWN -> -1
            NONE -> 0
        }
    }
}

/**
 * Mouse event sealed class for different mouse actions.
 */
sealed class MouseEvent {
    data class Move(val dx: Float, val dy: Float) : MouseEvent() {
        /**
         * Check if this is a zero movement.
         */
        fun isZero(): Boolean = dx == 0f && dy == 0f

        /**
         * Get the magnitude of the movement.
         */
        fun getMagnitude(): Float = kotlin.math.sqrt(dx * dx + dy * dy)
    }

    data class Click(val button: MouseButton, val repeat: Int = 1) : MouseEvent() {
        /**
         * Check if this is a single click.
         */
        fun isSingleClick(): Boolean = repeat == 1

        /**
         * Check if this is a double click.
         */
        fun isDoubleClick(): Boolean = repeat == 2

        init {
            require(repeat in 1..3) { "Repeat count must be between 1 and 3" }
        }
    }

    data class Scroll(val delta: Int, val direction: ScrollDirection) : MouseEvent() {
        /**
         * Check if this is scrolling up.
         */
        fun isUp(): Boolean = direction == ScrollDirection.UP

        /**
         * Check if this is scrolling down.
         */
        fun isDown(): Boolean = direction == ScrollDirection.DOWN

        companion object {
            /**
             * Create a scroll event from a delta value.
             */
            fun fromDelta(delta: Int): Scroll {
                val direction = when {
                    delta > 0 -> ScrollDirection.UP
                    delta < 0 -> ScrollDirection.DOWN
                    else -> ScrollDirection.NONE
                }
                return Scroll(delta, direction)
            }
        }
    }

    object DoubleClick : MouseEvent()
    object RightClick : MouseEvent()
    object MiddleClick : MouseEvent()

    /**
     * Get the event type as a string.
     */
    fun getTypeName(): String {
        return when (this) {
            is Move -> "Move"
            is Click -> "Click"
            is Scroll -> "Scroll"
            DoubleClick -> "DoubleClick"
            RightClick -> "RightClick"
            MiddleClick -> "MiddleClick"
        }
    }

    /**
     * Get a human-readable description of the event.
     */
    fun getDescription(): String {
        return when (this) {
            is Move -> "Move (dx=${"%.2f".format(dx)}, dy=${"%.2f".format(dy)})"
            is Click -> "${button.getDisplayName()} x$repeat"
            is Scroll -> "Scroll ${direction.name} (delta=$delta)"
            DoubleClick -> "Double Click"
            RightClick -> "Right Click"
            MiddleClick -> "Middle Click"
        }
    }
}

/**
 * Movement profile for cursor control.
 */
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
) : Parcelable {

    /**
     * Check if the profile is valid.
     */
    fun isValid(): Boolean {
        return sensitivity in 0.1f..3.0f &&
                accelerationFactor in 1.0f..3.0f &&
                deadband in 0f..2.0f &&
                maxSpeed > 0 &&
                minSpeed in 0f..maxSpeed &&
                predictiveBlend in 0f..1f &&
                smoothingAlpha in 0f..1f
    }

    /**
     * Apply the movement profile to raw input values.
     */
    fun applyTo(dx: Float, dy: Float): Pair<Float, Float> {
        var x = dx
        var y = dy

        // Swap axes if enabled
        if (swapAxes) {
            val temp = x
            x = y
            y = temp
        }

        // Invert axes if enabled
        if (invertX) x = -x
        if (invertY) y = -y

        // Apply sensitivity
        x *= sensitivity
        y *= sensitivity

        // Apply deadband
        if (abs(x) < deadband) x = 0f
        if (abs(y) < deadband) y = 0f

        // Apply smoothing
        if (smoothingEnabled) {
            // Smoothing is applied elsewhere with smoothingAlpha
        }

        return Pair(x, y)
    }

    companion object {
        /**
         * Create a default movement profile.
         */
        fun default(): MovementProfile {
            return MovementProfile()
        }

        /**
         * Create a gaming-optimized profile.
         */
        fun gaming(): MovementProfile {
            return MovementProfile(
                sensitivity = 1.8f,
                smoothingEnabled = false,
                accelerationEnabled = false,
                deadband = 0.2f,
                maxSpeed = 150f
            )
        }

        /**
         * Create a precision-optimized profile (e.g., for design work).
         */
        fun precision(): MovementProfile {
            return MovementProfile(
                sensitivity = 0.6f,
                smoothingEnabled = true,
                accelerationEnabled = false,
                deadband = 0.3f,
                maxSpeed = 50f,
                smoothingAlpha = 0.5f
            )
        }

        /**
         * Create a presentation-optimized profile.
         */
        fun presentation(): MovementProfile {
            return MovementProfile(
                sensitivity = 0.8f,
                smoothingEnabled = true,
                accelerationEnabled = false,
                deadband = 0.2f,
                maxSpeed = 60f,
                smoothingAlpha = 0.4f
            )
        }

        /**
         * Create a fast-responding profile.
         */
        fun responsive(): MovementProfile {
            return MovementProfile(
                sensitivity = 1.2f,
                smoothingEnabled = false,
                accelerationEnabled = true,
                accelerationFactor = 1.2f,
                deadband = 0.1f,
                maxSpeed = 120f,
                predictiveBlend = 0.8f
            )
        }
    }
}

/**
 * Mouse statistics for usage tracking.
 */
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
) : Parcelable {

    /**
     * Get the total number of clicks (left + right).
     */
    fun totalClicksCombined(): Int = totalClicks + totalRightClicks

    /**
     * Get the total number of events.
     */
    fun getTotalEvents(): Int {
        return totalClicks + totalDoubleClicks + totalRightClicks + totalScrolls + movementCount
    }

    /**
     * Record a click event.
     */
    fun recordClick(isRight: Boolean = false): MouseStatistics {
        return if (isRight) {
            copy(
                totalRightClicks = totalRightClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
        } else {
            copy(
                totalClicks = totalClicks + 1,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    /**
     * Record a double click event.
     */
    fun recordDoubleClick(): MouseStatistics {
        return copy(
            totalDoubleClicks = totalDoubleClicks + 1,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Record a scroll event.
     */
    fun recordScroll(): MouseStatistics {
        return copy(
            totalScrolls = totalScrolls + 1,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Record movement.
     */
    fun recordMovement(dx: Float, dy: Float): MouseStatistics {
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val newTotalMovement = totalMovement + distance
        val newMovementCount = movementCount + 1
        val newAverageSpeed = if (newMovementCount > 0) {
            newTotalMovement / newMovementCount
        } else 0f

        return copy(
            totalMovement = newTotalMovement,
            movementCount = newMovementCount,
            averageSpeed = newAverageSpeed,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Get the formatted average speed.
     */
    fun getFormattedAverageSpeed(): String {
        return String.format("%.1f px/s", averageSpeed)
    }

    /**
     * Get the formatted total movement.
     */
    fun getFormattedTotalMovement(): String {
        return if (totalMovement > 1000) {
            String.format("%.1fK px", totalMovement / 1000f)
        } else {
            String.format("%.0f px", totalMovement)
        }
    }

    companion object {
        /**
         * Create empty statistics.
         */
        fun empty(): MouseStatistics {
            return MouseStatistics()
        }

        /**
         * Create statistics from a list of events.
         */
        fun fromEvents(events: List<MouseEvent>): MouseStatistics {
            var stats = MouseStatistics()
            for (event in events) {
                when (event) {
                    is MouseEvent.Click -> {
                        stats = when (event.button) {
                            MouseButton.RIGHT -> stats.recordClick(true)
                            else -> stats.recordClick(false)
                        }
                    }
                    MouseEvent.DoubleClick -> stats = stats.recordDoubleClick()
                    MouseEvent.RightClick -> stats = stats.recordClick(true)
                    MouseEvent.MiddleClick -> stats = stats.recordClick(false)
                    is MouseEvent.Move -> stats = stats.recordMovement(event.dx, event.dy)
                    is MouseEvent.Scroll -> stats = stats.recordScroll()
                }
            }
            return stats
        }
    }
}
