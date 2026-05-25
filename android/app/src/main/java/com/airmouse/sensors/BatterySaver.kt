package com.airmouse.utils

import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airmouse.sensors.SensorService

/**
 * Reduces sensor sampling rate when the phone is stationary to save battery.
 */
class BatterySaver {
    private var handler = Handler(Looper.getMainLooper())
    private var sensorService: SensorService? = null
    private var isLowPower = false
    private var lastMovementTime = System.currentTimeMillis()
    private val idleThresholdMs = 10000L   // 10 seconds of no movement
    private var lastRoll = 0f
    private var lastYaw = 0f

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (sensorService == null) return
            // This is simplified; in reality you'd need a movement magnitude callback
            // For now, we assume BatterySaver is called by MainActivity based on sensor values.
            handler.postDelayed(this, 2000)
        }
    }

    fun start(service: SensorService) {
        sensorService = service
        handler.post(checkRunnable)
    }

    fun stop() {
        handler.removeCallbacks(checkRunnable)
        if (isLowPower) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
            isLowPower = false
        }
        sensorService = null
    }

    fun isLowPowerMode(): Boolean = isLowPower

    fun onMovement() {
        lastMovementTime = System.currentTimeMillis()
        if (isLowPower) {
            // Restore normal rate
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
            isLowPower = false
        }
    }

    // This should be called regularly from SensorService or MainActivity
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