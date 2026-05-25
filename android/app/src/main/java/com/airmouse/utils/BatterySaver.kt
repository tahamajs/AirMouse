package com.airmouse.utils

import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airmouse.sensors.SensorService

/**
 * Reduces sensor sampling rate when the phone is stationary to save battery.
 * Monitors orientation changes (roll/yaw) and switches between SENSOR_DELAY_GAME (50 Hz)
 * and SENSOR_DELAY_NORMAL (20 Hz) after 10 seconds of no movement.
 */
class BatterySaver {
    private val handler = Handler(Looper.getMainLooper())
    private var sensorService: SensorService? = null
    private var isLowPower = false
    private var lastMovementTime = System.currentTimeMillis()
    private val idleThresholdMs = 10000L   // 10 seconds of no movement
    private var lastRoll = 0f
    private var lastYaw = 0f

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (sensorService == null) return
            // Periodic check (every 2 seconds) for movement
            handler.postDelayed(this, 2000)
        }
    }

    /**
     * Start battery saver monitoring.
     * @param service The SensorService instance that allows changing sampling rate.
     */
    fun start(service: SensorService) {
        sensorService = service
        handler.post(checkRunnable)
    }

    /**
     * Stop monitoring and restore normal sampling rate.
     */
    fun stop() {
        handler.removeCallbacks(checkRunnable)
        if (isLowPower) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
            isLowPower = false
        }
        sensorService = null
    }

    /**
     * @return true if currently in low‑power (reduced sampling) mode.
     */
    fun isLowPowerMode(): Boolean = isLowPower

    /**
     * Called whenever movement is detected. Resets idle timer and restores normal rate if needed.
     */
    fun onMovement() {
        lastMovementTime = System.currentTimeMillis()
        if (isLowPower) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
            isLowPower = false
        }
    }

    /**
     * Call this regularly (e.g., from SensorService on each orientation update) with current roll and yaw.
     * It computes the movement delta and triggers onMovement() if significant.
     */
    fun updateMovement(roll: Float, yaw: Float) {
        val delta = kotlin.math.abs(roll - lastRoll) + kotlin.math.abs(yaw - lastYaw)
        lastRoll = roll
        lastYaw = yaw
        if (delta > 0.05f) {
            onMovement()
        } else if (!isLowPower && System.currentTimeMillis() - lastMovementTime > idleThresholdMs) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_NORMAL)
            isLowPower = true
        }
    }
}