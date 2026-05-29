package com.airmouse.utils

/**
 * Application‑wide constants. Change these values to adjust default behaviour.
 */
object Constants {
    /** Default TCP port for the PC server. */
    const val DEFAULT_PORT = 8080

    /** Sensor sampling delay in microseconds (20 ms = 50 Hz). */
    const val SENSOR_DELAY_US = 20000

    /** Default cursor sensitivity multiplier (0.2–2.0). */
    const val DEFAULT_SENSITIVITY = 0.5f

    /** Default gyro angular speed threshold for a click (rad/s). */
    const val CLICK_SPEED_THRESHOLD = 5.0f

    /** Default linear acceleration threshold for scroll (m/s²). */
    const val SCROLL_SPEED_THRESHOLD = 8.0f

    /** Default debounce value for scroll (m/s²). */
    const val SCROLL_DEBOUNCE = 2.0f

    /** Default double‑click interval (ms). */
    const val DOUBLE_CLICK_INTERVAL_MS = 400L

    /** Default tilt angle for right‑click (degrees). */
    const val RIGHT_CLICK_TILT_DEG = 45f

    /** Default hold duration for right‑click (ms). */
    const val RIGHT_CLICK_DURATION_MS = 500L

    /** Time to wait for ACK before retransmission (ms). */
    const val ACK_TIMEOUT_MS = 500L

    /** Reconnect delay after connection loss (ms). */
    const val RECONNECT_DELAY_MS = 5000L

}
