package com.airmouse.sensors

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager

class EnhancedGestureDetector(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val vibrator: Vibrator
) {
    enum class Gesture { CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN }

    private var lastClickTime = 0L
    private var lastRightClickTime = 0L
    private var potentialDoubleClick = false
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false

    private var clickSpeedThreshold: Float
    private var doubleClickMaxInterval: Long
    private var scrollSpeedThreshold: Float
    private var scrollDebounceThreshold: Float
    private var rightClickTiltThreshold: Float
    private var rightClickDurationMs: Long

    init {
        reloadThresholds()
    }

    fun reloadThresholds() {
        clickSpeedThreshold = preferences.getClickThreshold()
        doubleClickMaxInterval = preferences.getDoubleClickInterval()
        scrollSpeedThreshold = preferences.getScrollThreshold()
        scrollDebounceThreshold = preferences.getScrollDebounce()
        rightClickTiltThreshold = preferences.getRightClickTilt()
        rightClickDurationMs = preferences.getRightClickDuration()
    }

    fun detectClick(gyroY: Float, dt: Float): Boolean {
        val angularSpeed = kotlin.math.abs(gyroY)
        val now = System.currentTimeMillis()

        if (angularSpeed > clickSpeedThreshold && now - lastClickTime > doubleClickMaxInterval) {
            lastClickTime = now
            if (potentialDoubleClick) {
                potentialDoubleClick = false
                vibrate(50)
                return true  // This will be interpreted as double-click in the caller
            } else {
                potentialDoubleClick = true
                android.os.Handler(context.mainLooper).postDelayed({
                    if (potentialDoubleClick) {
                        potentialDoubleClick = false
                        vibrate(30)
                        // Single click will be handled by caller
                    }
                }, doubleClickMaxInterval)
            }
        }
        return false
    }

    fun detectRightClick(roll: Float, dt: Float): Boolean {
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(roll) > rightClickTiltThreshold && !rightClickTriggered) {
            if (rightClickStartTime == 0L) {
                rightClickStartTime = now
            } else if (now - rightClickStartTime > rightClickDurationMs) {
                rightClickTriggered = true
                vibrate(80)
                return true
            }
        } else {
            rightClickStartTime = 0L
            rightClickTriggered = false
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

    private fun vibrate(duration: Long) {
        if (preferences.isHapticEnabled()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}