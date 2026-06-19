package com.airmouse.sensors

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import com.airmouse.utils.PreferencesManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Gesture detector for click, double click, right click, and scroll gestures.
 * Uses gyroscope and accelerometer data to detect user gestures.
 */
class GestureDetector(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    enum class Gesture {
        NONE, CLICK, DOUBLE_CLICK, RIGHT_CLICK, SCROLL_UP, SCROLL_DOWN
    }

    // State
    private var lastClickTime = 0L
    private var potentialDoubleClick = false
    private var pendingClickRunnable: Runnable? = null
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false

    // Gyro offsets
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f

    // Thresholds (will be loaded from preferences)
    private var clickSpeedThreshold: Float = 8f
    private var doubleClickMaxInterval: Long = 400L
    private var scrollSpeedThreshold: Float = 6f
    private var scrollDebounceThreshold: Float = 2f
    private var rightClickTiltThreshold: Float = 45f
    private var rightClickDurationMs: Long = 500L

    private val handler = Handler(Looper.getMainLooper())

    init {
        reloadThresholds()
        loadGyroOffsets()
    }

    fun reloadThresholds() {
        clickSpeedThreshold = prefs.getFloat("click_threshold", 8f)
        doubleClickMaxInterval = prefs.getLong("double_click_interval", 400L)
        scrollSpeedThreshold = prefs.getFloat("scroll_threshold", 6f)
        scrollDebounceThreshold = prefs.getFloat("scroll_debounce", 2f)
        rightClickTiltThreshold = prefs.getFloat("right_click_tilt", 45f)
        rightClickDurationMs = prefs.getLong("right_click_duration", 500L)
    }

    fun loadGyroOffsets() {
        gyroOffsetX = prefs.getFloat("gyro_offset_x", 0f)
        gyroOffsetY = prefs.getFloat("gyro_offset_y", 0f)
        gyroOffsetZ = prefs.getFloat("gyro_offset_z", 0f)
    }

    fun setGyroOffsets(x: Float, y: Float, z: Float) {
        gyroOffsetX = x
        gyroOffsetY = y
        gyroOffsetZ = z
    }

    /**
     * Detect a gesture from gyro Y, accel Y, and roll.
     * @return Gesture detected (or NONE)
     */
    fun detect(gyroX: Float, gyroY: Float, gyroZ: Float, accelX: Float, accelY: Float, accelZ: Float): Gesture {
        val now = System.currentTimeMillis()

        // Apply calibration offsets
        val calibratedGyroX = gyroX - gyroOffsetX
        val calibratedGyroY = gyroY - gyroOffsetY
        val calibratedGyroZ = gyroZ - gyroOffsetZ

        // Calculate angular speed (magnitude of gyro)
        val angularSpeed = sqrt(calibratedGyroX * calibratedGyroX + calibratedGyroY * calibratedGyroY + calibratedGyroZ * calibratedGyroZ)

        // --- Click / Double-click detection ---
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
                    }
                }
                pendingClickRunnable = runnable
                handler.postDelayed(runnable, doubleClickMaxInterval)
                vibrate(30)
                return Gesture.CLICK
            }
        }

        // --- Right Click (long tilt) detection ---
        val roll = calibratedGyroX // Use gyro X as roll indicator
        if (abs(roll) > rightClickTiltThreshold && !rightClickTriggered) {
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

        // --- Scroll detection ---
        val speed = abs(accelY)
        if (speed > scrollSpeedThreshold && !scrollInProgress) {
            scrollInProgress = true
            vibrate(20)
            return if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
        } else if (speed < scrollDebounceThreshold) {
            scrollInProgress = false
        }

        return Gesture.NONE
    }

    private fun vibrate(duration: Long) {
        if (prefs.getBoolean("haptic_enabled", true)) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }
}