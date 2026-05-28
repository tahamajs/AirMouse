package com.airmouse.domain

import android.os.Handler
import android.os.Looper
import com.airmouse.utils.PreferencesManager

/**
 * Detects gestures from sensor values using configurable thresholds.
 * Uses a callback to report gestures asynchronously (supports double‑click delay).
 */
class GestureDetector(private val prefs: PreferencesManager) {

    enum class Gesture {
        NONE,
        CLICK,
        DOUBLE_CLICK,
        RIGHT_CLICK,
        SCROLL_UP,
        SCROLL_DOWN
    }

    interface Listener {
        fun onGestureDetected(gesture: Gesture)
    }

    private var listener: Listener? = null

    // State
    private var lastClickTime = 0L
    private var potentialClick = false
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false
    private val handler = Handler(Looper.getMainLooper())
    private var pendingSingleClickRunnable: Runnable? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun reloadThresholds() {
        reset()
    }

    /**
     * Main detection method. Call on every sensor update (e.g., 50 Hz).
     * @param gyroY Angular velocity around Y‑axis (rad/s)
     * @param accelY Linear acceleration along Y‑axis (m/s²)
     * @param roll Roll angle (radians) from sensor fusion
     */
    fun detect(gyroY: Float, accelY: Float, roll: Float) {
        val now = System.currentTimeMillis()
        val angularSpeed = kotlin.math.abs(gyroY)
        val clickThreshold = prefs.getClickThreshold()
        val doubleClickInterval = prefs.getDoubleClickInterval()

        // --- Click / Double‑Click ---
        if (angularSpeed > clickThreshold) {
            if (!potentialClick) {
                // First click: start timer
                potentialClick = true
                pendingSingleClickRunnable = Runnable {
                    if (potentialClick) {
                        potentialClick = false
                        listener?.onGestureDetected(Gesture.CLICK)
                    }
                }
                handler.postDelayed(pendingSingleClickRunnable!!, doubleClickInterval)
                lastClickTime = now
            } else if (now - lastClickTime <= doubleClickInterval) {
                // Second click within window → double click
                potentialClick = false
                pendingSingleClickRunnable?.let { handler.removeCallbacks(it) }
                pendingSingleClickRunnable = null
                listener?.onGestureDetected(Gesture.DOUBLE_CLICK)
            }
        }

        // --- Right‑Click (Long Tilt) ---
        val rightClickTilt = prefs.getRightClickTilt()
        val rightClickDuration = prefs.getRightClickDuration()
        if (kotlin.math.abs(roll) > rightClickTilt && !rightClickTriggered) {
            if (rightClickStartTime == 0L) {
                rightClickStartTime = now
            } else if (now - rightClickStartTime > rightClickDuration) {
                rightClickTriggered = true
                listener?.onGestureDetected(Gesture.RIGHT_CLICK)
            }
        } else {
            rightClickStartTime = 0L
            rightClickTriggered = false
        }

        // --- Scroll ---
        val scrollThreshold = prefs.getScrollThreshold()
        val scrollDebounce = prefs.getScrollDebounce()
        val speed = kotlin.math.abs(accelY)
        if (speed > scrollThreshold && !scrollInProgress) {
            scrollInProgress = true
            val gesture = if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
            listener?.onGestureDetected(gesture)
        } else if (speed < scrollDebounce) {
            scrollInProgress = false
        }
    }

    fun reset() {
        potentialClick = false
        pendingSingleClickRunnable?.let { handler.removeCallbacks(it) }
        pendingSingleClickRunnable = null
        rightClickStartTime = 0L
        rightClickTriggered = false
        scrollInProgress = false
    }
}