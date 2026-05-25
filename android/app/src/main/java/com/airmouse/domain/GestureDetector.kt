package com.airmouse.domain

import com.airmouse.data.PreferencesDataStore

class GestureDetector(private val prefs: PreferencesDataStore) {

    enum class Gesture { NONE, CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN }

    private var lastClickTime = 0L
    private var potentialDoubleClick = false
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false

    fun detect(gyroY: Float, accelY: Float, roll: Float): Gesture {
        val now = System.currentTimeMillis()

        // Double-click / single click detection
        val angularSpeed = kotlin.math.abs(gyroY)
        if (angularSpeed > prefs.getClickThreshold() && now - lastClickTime > prefs.getDoubleClickInterval()) {
            lastClickTime = now
            if (potentialDoubleClick) {
                potentialDoubleClick = false
                return Gesture.DOUBLE_CLICK
            } else {
                potentialDoubleClick = true
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (potentialDoubleClick) {
                        potentialDoubleClick = false
                        // Single click will be returned on next cycle? Better to return directly.
                    }
                }, prefs.getDoubleClickInterval())
                return Gesture.CLICK
            }
        }

        // Right-click (long tilt)
        if (kotlin.math.abs(roll) > prefs.getRightClickTilt() && !rightClickTriggered) {
            if (rightClickStartTime == 0L) {
                rightClickStartTime = now
            } else if (now - rightClickStartTime > prefs.getRightClickDuration()) {
                rightClickTriggered = true
                return Gesture.RIGHT_CLICK
            }
        } else {
            rightClickStartTime = 0L
            rightClickTriggered = false
        }

        // Scroll
        val speed = kotlin.math.abs(accelY)
        if (speed > prefs.getScrollThreshold() && !scrollInProgress) {
            scrollInProgress = true
            return if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
        } else if (speed < prefs.getScrollDebounce()) {
            scrollInProgress = false
        }

        return Gesture.NONE
    }
}