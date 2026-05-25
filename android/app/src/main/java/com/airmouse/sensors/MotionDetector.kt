package com.airmouse.sensors

class MotionDetector {
    enum class Gesture { CLICK, SCROLL_UP, SCROLL_DOWN }

    private var lastClickTime = 0L
    private val clickCooldownMs = 300L
    private val clickSpeedThreshold = 5.0f
    private val scrollSpeedThreshold = 8.0f
    private val scrollDebounceThreshold = 2.0f
    private var scrollInProgress = false

    fun detectClick(gyroY: Float, dt: Float): Boolean {
        val angularSpeed = kotlin.math.abs(gyroY)
        val now = System.currentTimeMillis()
        if (angularSpeed > clickSpeedThreshold && now - lastClickTime > clickCooldownMs) {
            lastClickTime = now
            return true
        }
        return false
    }

    fun detectScroll(accelY: Float, dt: Float): Int {
        val speed = kotlin.math.abs(accelY)
        return if (speed > scrollSpeedThreshold && !scrollInProgress) {
            scrollInProgress = true
            if (accelY > 0) 1 else -1
        } else if (speed < scrollDebounceThreshold) {
            scrollInProgress = false
            0
        } else 0
    }
}