package com.airmouse.utils

import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airmouse.sensors.SensorService

/**
 * Reduces sensor sampling rate when the phone is stationary to save battery.
 * Monitors orientation changes and switches to SENSOR_DELAY_NORMAL after 10s of idle.
 */
class BatterySaver {
    private val handler = try {
        Handler(Looper.getMainLooper())
    } catch (_: RuntimeException) {
        null
    }
    private var sensorService: SensorService? = null
    private var isLowPower = false
    private var lastMovementTime = System.currentTimeMillis()
    private val idleThresholdMs = 10000L   // 10 seconds of no movement
    private var lastRoll = 0f
    private var lastYaw = 0f

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (sensorService == null) return

            // If we haven't moved for a while and aren't in low power mode yet
            if (!isLowPower && System.currentTimeMillis() - lastMovementTime > idleThresholdMs) {
                sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_NORMAL)
                isLowPower = true
            }

            handler?.postDelayed(this, 2000)
        }
    }

    fun start(service: SensorService) {
        sensorService = service
        lastMovementTime = System.currentTimeMillis()
        handler?.post(checkRunnable)
    }

    fun stop() {
        handler?.removeCallbacks(checkRunnable)
        if (isLowPower) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
            isLowPower = false
        }
        sensorService = null
    }

    fun isLowPowerMode(): Boolean = isLowPower

    fun onMovement() {
        lastMovementTime = System.currentTimeMillis()
        sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
        if (isLowPower) {
            isLowPower = false
        }
    }

    fun updateMovement(roll: Float, yaw: Float) {
        // Simple delta check for movement
        val delta = kotlin.math.abs(roll - lastRoll) + kotlin.math.abs(yaw - lastYaw)
        lastRoll = roll
        lastYaw = yaw

        // Threshold for what we consider 'movement' (0.05 rad is approx 3 degrees)
        if (delta > 0.05f) {
            onMovement()
        }
    }
}
