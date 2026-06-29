package com.airmouse.sensors

import kotlin.math.abs

class MotionDetector {

    enum class Gesture {
        NONE,
        CLICK,
        DOUBLE_CLICK,
        SCROLL_UP,
        SCROLL_DOWN
    }

    
    private var lastClickTime = 0L
    private var lastSingleClickTime = 0L
    private var potentialDoubleClick = false
    private val defaultClickCooldownMs = 300L
    private val defaultDoubleClickIntervalMs = 400L
    private val defaultClickSpeedThreshold = 8.0f
    private val defaultScrollSpeedThreshold = 8.0f
    private val defaultScrollDebounceThreshold = 2.0f

    
    private var scrollInProgress = false
    private var lastScrollTime = 0L
    private val scrollCooldownMs = 100L
    private var lastUpdateTime = 0L

    
    private var isActive = true

    
    var clickSpeedThreshold: Float = defaultClickSpeedThreshold
    var clickCooldownMs: Long = defaultClickCooldownMs
    var doubleClickIntervalMs: Long = defaultDoubleClickIntervalMs
    var scrollSpeedThreshold: Float = defaultScrollSpeedThreshold
    var scrollDebounceThreshold: Float = defaultScrollDebounceThreshold

    
    var onGestureDetected: ((Gesture) -> Unit)? = null

    
    private var clickCount = 0
    private var doubleClickCount = 0
    private var scrollUpCount = 0
    private var scrollDownCount = 0

    fun reset() {
        lastClickTime = 0L
        lastSingleClickTime = 0L
        potentialDoubleClick = false
        scrollInProgress = false
        lastScrollTime = 0L
        lastUpdateTime = 0L
    }

    fun updateThresholds(
        clickSpeed: Float = clickSpeedThreshold,
        clickCooldown: Long = clickCooldownMs,
        doubleClickInterval: Long = doubleClickIntervalMs,
        scrollSpeed: Float = scrollSpeedThreshold,
        scrollDebounce: Float = scrollDebounceThreshold
    ) {
        clickSpeedThreshold = clickSpeed
        clickCooldownMs = clickCooldown
        doubleClickIntervalMs = doubleClickInterval
        scrollSpeedThreshold = scrollSpeed
        scrollDebounceThreshold = scrollDebounce
    }

    fun detect(gyroY: Float, accelY: Float): Gesture {
        if (!isActive) return Gesture.NONE

        val now = System.currentTimeMillis()

        
        val angularSpeed = abs(gyroY)

        if (angularSpeed > clickSpeedThreshold && now - lastClickTime > clickCooldownMs) {
            lastClickTime = now

            
            if (potentialDoubleClick && now - lastSingleClickTime < doubleClickIntervalMs) {
                potentialDoubleClick = false
                clickCount++
                doubleClickCount++
                onGestureDetected?.invoke(Gesture.DOUBLE_CLICK)
                return Gesture.DOUBLE_CLICK
            } else {
                potentialDoubleClick = true
                lastSingleClickTime = now
                clickCount++
                onGestureDetected?.invoke(Gesture.CLICK)
                return Gesture.CLICK
            }
        }

        
        if (potentialDoubleClick && now - lastSingleClickTime > doubleClickIntervalMs) {
            potentialDoubleClick = false
        }

        
        val speed = abs(accelY)

        if (now - lastScrollTime > scrollCooldownMs) {
            if (speed > scrollSpeedThreshold && !scrollInProgress) {
                scrollInProgress = true
                lastScrollTime = now

                val gesture = if (accelY > 0) {
                    scrollDownCount++
                    Gesture.SCROLL_DOWN
                } else {
                    scrollUpCount++
                    Gesture.SCROLL_UP
                }
                onGestureDetected?.invoke(gesture)
                return gesture
            } else if (speed < scrollDebounceThreshold) {
                scrollInProgress = false
            }
        }

        return Gesture.NONE
    }

    fun detectClick(gyroY: Float): Boolean {
        return detect(gyroY, 0f) == Gesture.CLICK
    }

    fun detectScroll(accelY: Float): Int {
        return when (detect(0f, accelY)) {
            Gesture.SCROLL_UP -> 1
            Gesture.SCROLL_DOWN -> -1
            else -> 0
        }
    }

    fun setEnabled(enabled: Boolean) {
        isActive = enabled
        if (!enabled) {
            reset()
        }
    }

    fun isEnabled(): Boolean = isActive

    fun getStatistics(): Map<String, Int> {
        return mapOf(
            "clicks" to clickCount,
            "doubleClicks" to doubleClickCount,
            "scrollUp" to scrollUpCount,
            "scrollDown" to scrollDownCount
        )
    }

    fun resetStatistics() {
        clickCount = 0
        doubleClickCount = 0
        scrollUpCount = 0
        scrollDownCount = 0
    }

    fun getConfig(): Map<String, Any> {
        return mapOf(
            "clickSpeedThreshold" to clickSpeedThreshold,
            "clickCooldownMs" to clickCooldownMs,
            "doubleClickIntervalMs" to doubleClickIntervalMs,
            "scrollSpeedThreshold" to scrollSpeedThreshold,
            "scrollDebounceThreshold" to scrollDebounceThreshold,
            "isActive" to isActive
        )
    }

    fun loadFromPreferences(preferences: com.airmouse.utils.PreferencesManager) {
        clickSpeedThreshold = preferences.getFloat("click_threshold", defaultClickSpeedThreshold)
        doubleClickIntervalMs = preferences.getLong("double_click_interval", defaultDoubleClickIntervalMs)
        scrollSpeedThreshold = preferences.getFloat("scroll_threshold", defaultScrollSpeedThreshold)
        clickCooldownMs = preferences.getLong("click_cooldown", defaultClickCooldownMs)
    }
}

class MotionDetectorBuilder {
    private var clickSpeedThreshold: Float = 8.0f
    private var clickCooldownMs: Long = 300L
    private var doubleClickIntervalMs: Long = 400L
    private var scrollSpeedThreshold: Float = 8.0f
    private var scrollDebounceThreshold: Float = 2.0f

    fun clickSpeed(threshold: Float) = apply { this.clickSpeedThreshold = threshold }
    fun clickCooldown(cooldownMs: Long) = apply { this.clickCooldownMs = cooldownMs }
    fun doubleClickInterval(intervalMs: Long) = apply { this.doubleClickIntervalMs = intervalMs }
    fun scrollSpeed(threshold: Float) = apply { this.scrollSpeedThreshold = threshold }
    fun scrollDebounce(threshold: Float) = apply { this.scrollDebounceThreshold = threshold }

    fun build(): MotionDetector {
        return MotionDetector().apply {
            clickSpeedThreshold = this@MotionDetectorBuilder.clickSpeedThreshold
            clickCooldownMs = this@MotionDetectorBuilder.clickCooldownMs
            doubleClickIntervalMs = this@MotionDetectorBuilder.doubleClickIntervalMs
            scrollSpeedThreshold = this@MotionDetectorBuilder.scrollSpeedThreshold
            scrollDebounceThreshold = this@MotionDetectorBuilder.scrollDebounceThreshold
        }
    }
}