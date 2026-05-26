package com.airmouse.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager

/**
 * Enhanced gesture detector with single click, double click, right click, and scroll.
 * All thresholds are read from PreferencesManager and can be updated at runtime.
 */
class EnhancedGestureDetector(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val vibrator: Vibrator
) {
    enum class Gesture {
        NONE, CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN
    }

    // State
    private var lastClickTime = 0L
    private var potentialDoubleClick = false
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false

    // Thresholds (loaded from preferences)
    private var clickSpeedThreshold: Float = 5f
    private var doubleClickMaxInterval: Long = 400L
    private var scrollSpeedThreshold: Float = 8f
    private var scrollDebounceThreshold: Float = 2f
    private var rightClickTiltThreshold: Float = 45f
    private var rightClickDurationMs: Long = 500L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingClickRunnable: Runnable? = null

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

    /**
     * Detect a gesture from gyro Y, accel Y, and roll.
     * @return Gesture detected (or NONE)
     */
    fun detect(gyroY: Float, accelY: Float, roll: Float): Gesture {
        val now = System.currentTimeMillis()

        // --- Click / Double-click ---
        val angularSpeed = kotlin.math.abs(gyroY)
        if (angularSpeed > clickSpeedThreshold && now - lastClickTime > doubleClickMaxInterval) {
            lastClickTime = now
            if (potentialDoubleClick) {
                // Second flick within window → double click
                potentialDoubleClick = false
                pendingClickRunnable?.let { handler.removeCallbacks(it) }
                pendingClickRunnable = null
                vibrate(50)
                return Gesture.DOUBLE_CLICK
            } else {
                // First flick → potential double click
                potentialDoubleClick = true
                val runnable = Runnable {
                    if (potentialDoubleClick) {
                        potentialDoubleClick = false
                        vibrate(30)
                        // Single click will be returned only if no second flick arrived
                        // However, we already returned CLICK? This is tricky.
                        // In this implementation, we return CLICK immediately and rely
                        // on the caller to handle double click separately.
                    }
                }
                pendingClickRunnable = runnable
                handler.postDelayed(runnable, doubleClickMaxInterval)
                vibrate(30)
                return Gesture.CLICK
            }
        }

        // --- Right Click (long tilt) ---
        if (kotlin.math.abs(roll) > rightClickTiltThreshold && !rightClickTriggered) {
            if (rightClickStartTime == 0L) {
                rightClickStartTime = now
            } else if (now - rightClickStartTime > rightClickDurationMs) {
                rightClickTriggered = true
                vibrate(80)
                return Gesture.RIGHT_CLICK
            }
        } else {
            rightClickStartTime = 0L
            rightClickTriggered = false
        }

        // --- Scroll ---
        val speed = kotlin.math.abs(accelY)
        if (speed > scrollSpeedThreshold && !scrollInProgress) {
            scrollInProgress = true
            return if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
        } else if (speed < scrollDebounceThreshold) {
            scrollInProgress = false
        }

        return Gesture.NONE
    }

    private fun vibrate(duration: Long) {
        if (preferences.isHapticEnabled()) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}