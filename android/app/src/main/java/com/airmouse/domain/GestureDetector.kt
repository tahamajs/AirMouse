package com.airmouse.domain

import android.os.Handler
import android.os.Looper

/**
 * GestureDetector handles spatial taps, delayed single clicks, and double-click disambiguation.
 * 
 * CRITICAL LOGIC:
 * A tap should not fire a single click immediately, because a subsequent tap within
 * DOUBLE_CLICK_WINDOW_MS would produce an accidental single-click followed by a double-click.
 * Instead, single-click is delayed until the window expires. If a second tap arrives,
 * the pending single-click is cancelled and a double-click is emitted.
 */
class GestureDetector(
    private val onSingleClick: () -> Unit,
    private val onDoubleClick: () -> Unit,
    private val onGestureDetected: (String) -> Unit
) {
    companion object {
        const val DOUBLE_CLICK_WINDOW_MS = 280L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pendingSingleClickRunnable: Runnable? = null
    private var lastTapTimestamp = 0L

    /**
     * Invoked when a tap event is registered from touchpad or spatial gesture.
     */
    fun onRawTap() {
        val now = System.currentTimeMillis()
        val timeSinceLastTap = now - lastTapTimestamp

        if (timeSinceLastTap < DOUBLE_CLICK_WINDOW_MS && pendingSingleClickRunnable != null) {
            // Second tap arrived within window -> Cancel delayed single click & trigger Double Click!
            pendingSingleClickRunnable?.let { handler.removeCallbacks(it) }
            pendingSingleClickRunnable = null
            lastTapTimestamp = 0L
            onDoubleClick()
        } else {
            // First tap -> Schedule delayed single click
            lastTapTimestamp = now
            val runnable = Runnable {
                onSingleClick()
                pendingSingleClickRunnable = null
            }
            pendingSingleClickRunnable = runnable
            handler.postDelayed(runnable, DOUBLE_CLICK_WINDOW_MS)
        }
    }

    /**
     * Resets any pending click state.
     */
    fun reset() {
        pendingSingleClickRunnable?.let { handler.removeCallbacks(it) }
        pendingSingleClickRunnable = null
        lastTapTimestamp = 0L
    }
}
