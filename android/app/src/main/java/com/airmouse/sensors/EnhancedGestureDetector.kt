package com.airmouse.sensors

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.airmouse.PreferencesManager
import kotlin.math.abs
import kotlin.math.sqrt

class EnhancedGestureDetector(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val vibrator: Vibrator
) {
    enum class Gesture {
        NONE,
        CLICK,
        DOUBLE_CLICK,
        RIGHT_CLICK,
        SCROLL_UP,
        SCROLL_DOWN,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        SWIPE_UP,
        SWIPE_DOWN
    }

    
    private var lastClickTime = 0L
    private var potentialDoubleClick = false
    private var pendingClickRunnable: Runnable? = null
    private var rightClickStartTime = 0L
    private var rightClickTriggered = false
    private var scrollInProgress = false
    private var lastSwipeTime = 0L
    private var swipeCooldownMs = 300L

    
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f

    
    private var clickSpeedThreshold: Float = 8f
    private var doubleClickMaxInterval: Long = 400L
    private var scrollSpeedThreshold: Float = 6f
    private var scrollDebounceThreshold: Float = 2f
    private var rightClickTiltThreshold: Float = 45f
    private var rightClickDurationMs: Long = 500L
    private var swipeThreshold: Float = 15f
    private var hapticEnabled: Boolean = true

    private val handler = try {
        Handler(Looper.getMainLooper())
    } catch (_: RuntimeException) {
        null
    }

    
    var onGestureDetected: ((Gesture) -> Unit)? = null

    init {
        reloadThresholds()
        loadGyroOffsets()
    }

    fun reloadThresholds() {
        clickSpeedThreshold = preferences.getClickThreshold()
        doubleClickMaxInterval = preferences.getDoubleClickInterval()
        scrollSpeedThreshold = preferences.getScrollThreshold()
        scrollDebounceThreshold = preferences.getFloat("scroll_debounce", 2f)
        rightClickTiltThreshold = preferences.getRightClickTilt()
        rightClickDurationMs = preferences.getRightClickDuration()
        swipeThreshold = preferences.getFloat("swipe_threshold", 15f)
        hapticEnabled = preferences.isHapticEnabled()
    }

    fun loadGyroOffsets() {
        gyroOffsetX = preferences.getFloat("gyro_offset_x", 0f)
        gyroOffsetY = preferences.getFloat("gyro_offset_y", 0f)
        gyroOffsetZ = preferences.getFloat("gyro_offset_z", 0f)
    }

    fun setGyroOffsets(x: Float, y: Float, z: Float) {
        gyroOffsetX = x
        gyroOffsetY = y
        gyroOffsetZ = z
    }

    fun detect(
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        roll: Float
    ): Gesture {
        val now = System.currentTimeMillis()

        
        val calibratedGyroX = gyroX - gyroOffsetX
        val calibratedGyroY = gyroY - gyroOffsetY
        val calibratedGyroZ = gyroZ - gyroOffsetZ

        
        val angularSpeedY = abs(calibratedGyroY)
        val angularSpeedX = abs(calibratedGyroX)
        val angularSpeedZ = abs(calibratedGyroZ)
        val totalAngularSpeed = sqrt(calibratedGyroX * calibratedGyroX + calibratedGyroY * calibratedGyroY + calibratedGyroZ * calibratedGyroZ)

        
        if (angularSpeedY > clickSpeedThreshold && now - lastClickTime > doubleClickMaxInterval) {
            lastClickTime = now

            if (potentialDoubleClick) {
                
                potentialDoubleClick = false
                pendingClickRunnable?.let { handler?.removeCallbacks(it) }
                pendingClickRunnable = null
                vibrate(50)
                onGestureDetected?.invoke(Gesture.DOUBLE_CLICK)
                return Gesture.DOUBLE_CLICK
            } else {
                
                potentialDoubleClick = true
                val runnable = Runnable {
                    if (potentialDoubleClick) {
                        potentialDoubleClick = false
                        vibrate(30)
                        onGestureDetected?.invoke(Gesture.CLICK)
                    }
                }
                pendingClickRunnable = runnable
                handler?.postDelayed(runnable, doubleClickMaxInterval)
                vibrate(30)
                onGestureDetected?.invoke(Gesture.CLICK)
                return Gesture.CLICK
            }
        }

        
        val rollDegrees = Math.toDegrees(roll.toDouble()).toFloat()

        if (abs(rollDegrees) > rightClickTiltThreshold && !rightClickTriggered) {
            if (rightClickStartTime == 0L) {
                rightClickStartTime = now
            } else if (now - rightClickStartTime > rightClickDurationMs) {
                rightClickTriggered = true
                vibrate(80)
                onGestureDetected?.invoke(Gesture.RIGHT_CLICK)
                return Gesture.RIGHT_CLICK
            }
        } else {
            rightClickStartTime = 0L
            rightClickTriggered = false
        }

        
        val speed = abs(accelY)

        if (speed > scrollSpeedThreshold && !scrollInProgress) {
            scrollInProgress = true
            vibrate(20)
            val gesture = if (accelY > 0) Gesture.SCROLL_DOWN else Gesture.SCROLL_UP
            onGestureDetected?.invoke(gesture)
            return gesture
        } else if (speed < scrollDebounceThreshold) {
            scrollInProgress = false
        }

        
        if (now - lastSwipeTime > swipeCooldownMs) {
            when {
                angularSpeedZ > swipeThreshold -> {
                    lastSwipeTime = now
                    vibrate(25)
                    val gesture = if (calibratedGyroZ > 0) Gesture.SWIPE_RIGHT else Gesture.SWIPE_LEFT
                    onGestureDetected?.invoke(gesture)
                    return gesture
                }
                angularSpeedX > swipeThreshold -> {
                    lastSwipeTime = now
                    vibrate(25)
                    val gesture = if (calibratedGyroX > 0) Gesture.SWIPE_DOWN else Gesture.SWIPE_UP
                    onGestureDetected?.invoke(gesture)
                    return gesture
                }
            }
        }

        return Gesture.NONE
    }

    fun detect(gyroY: Float, accelY: Float, roll: Float): Gesture {
        return detect(0f, gyroY, 0f, 0f, accelY, 0f, roll)
    }

    fun reset() {
        lastClickTime = 0L
        potentialDoubleClick = false
        pendingClickRunnable?.let { handler?.removeCallbacks(it) }
        pendingClickRunnable = null
        rightClickStartTime = 0L
        rightClickTriggered = false
        scrollInProgress = false
        lastSwipeTime = 0L
    }

    fun updateThresholds() {
        reloadThresholds()
    }

    fun getConfig(): Map<String, Any> {
        return mapOf(
            "clickSpeedThreshold" to clickSpeedThreshold,
            "doubleClickMaxInterval" to doubleClickMaxInterval,
            "scrollSpeedThreshold" to scrollSpeedThreshold,
            "scrollDebounceThreshold" to scrollDebounceThreshold,
            "rightClickTiltThreshold" to rightClickTiltThreshold,
            "rightClickDurationMs" to rightClickDurationMs,
            "swipeThreshold" to swipeThreshold,
            "hapticEnabled" to hapticEnabled,
            "gyroOffsetX" to gyroOffsetX,
            "gyroOffsetY" to gyroOffsetY,
            "gyroOffsetZ" to gyroOffsetZ
        )
    }

    private fun vibrate(duration: Long) {
        if (!hapticEnabled) return

        try {
            val safeVibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
            if (!safeVibrator.hasVibrator()) return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                safeVibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                safeVibrator.vibrate(duration)
            }
        } catch (_: SecurityException) {
            
        }
    }

    private fun sqrt(value: Float): Float = kotlin.math.sqrt(value.toDouble()).toFloat()
}
