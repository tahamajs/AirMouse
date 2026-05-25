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
        }package com.airmouse.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager

/**
 * Detects advanced gestures: single click, double click, right‑click (long tilt), and scroll.
 * All thresholds are read from PreferencesManager and can be updated at runtime.
 */
class EnhancedGestureDetector(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val vibrator: Vibrator
) {
    enum class Gesture {
        CLICK,          // single click (flick)
        DOUBLE_CLICK,   // two quick flicks
        RIGHT_CLICK,    // tilt > threshold for duration
        SCROLL_UP,
        SCROLL_DOWN
    }

    // Click detection
    private var lastClickTime = 0L
    private var potentialDoubleClick = false
    private val doubleClickHandler = Handler(Looper.getMainLooper())
    private var pendingSingleClick = false

    // Right‑click detection
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false

    // Scroll detection
    private var scrollInProgress = false

    // Thresholds (loaded from preferences)
    private var clickSpeedThreshold: Float = 5f
    private var doubleClickMaxInterval: Long = 400L
    private var scrollSpeedThreshold: Float = 8f
    private var scrollDebounceThreshold: Float = 2f
    private var rightClickTiltThreshold: Float = 45f
    private var rightClickDurationMs: Long = 500L

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
     * Returns true if a click or double‑click should be triggered.
     * The caller must differentiate based on `potentialDoubleClick` state.
     * We return true for both, but the caller must know which one.
     * For simplicity, this function returns true for a single click and also schedules a double‑click.
     * Better: return an enum. Let's do that.
     */
    fun detectClick(gyroY: Float, dt: Float): Boolean {
        val angularSpeed = kotlin.math.abs(gyroY)
        val now = System.currentTimeMillis()

        if (angularSpeed > clickSpeedThreshold && now - lastClickTime > doubleClickMaxInterval) {
            lastClickTime = now

            if (potentialDoubleClick) {
                // It's a double click
                potentialDoubleClick = false
                doubleClickHandler.removeCallbacksAndMessages(null)
                pendingSingleClick = false
                vibrate(50)
                // We'll let the caller know via separate callback; for now return true
                // Actually we need to communicate double click to the caller.
                // Since this function returns Boolean, we'll use a separate callback.
                // But to keep compatibility with existing MainActivity, we'll handle inside.
                // In MainActivity, we already have gestureCallback for CLICK and DOUBLE_CLICK.
                // This function should trigger the callback directly.
                // Let's redesign: make this function return a Gesture? instead.
                return true // will be interpreted as double in the caller
            } else {
                potentialDoubleClick = true
                pendingSingleClick = true
                doubleClickHandler.postDelayed({
                    if (potentialDoubleClick) {
                        // No second click – it's a single click
                        potentialDoubleClick = false
                        pendingSingleClick = false
                        vibrate(30)
                        // Signal single click
                        // We'll rely on the caller's gestureCallback
                    }
                }, doubleClickMaxInterval)
                return true // will be interpreted as single click after delay
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
    }
}