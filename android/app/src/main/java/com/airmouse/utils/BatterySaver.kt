
package com.airmouse.utils

import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airmouse.SensorService
import kotlin.math.abs

class BatterySaver {
    private val handler = try {
        Handler(Looper.getMainLooper())
    } catch (_: RuntimeException) {
        null
    }
    private var sensorService: SensorService? = null
    private var isLowPower = false
    private var lastMovementTime = System.currentTimeMillis()
    private val idleThresholdMs = 10000L   
    private var lastRoll = 0f
    private var lastYaw = 0f
    private var lastPitch = 0f

    private var isEnabled = true
    private var onPowerStateChange: ((Boolean) -> Unit)? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (sensorService == null) return

            
            if (!isLowPower && System.currentTimeMillis() - lastMovementTime > idleThresholdMs) {
                enterLowPowerMode()
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
            exitLowPowerMode()
        }
        sensorService = null
    }

    fun isLowPowerMode(): Boolean = isLowPower

    fun onMovement() {
        lastMovementTime = System.currentTimeMillis()
        if (isLowPower) {
            exitLowPowerMode()
        }
    }

    fun updateMovement(roll: Float, pitch: Float, yaw: Float) {
        
        val deltaRoll = abs(roll - lastRoll)
        val deltaPitch = abs(pitch - lastPitch)
        val deltaYaw = abs(yaw - lastYaw)
        val totalDelta = deltaRoll + deltaPitch + deltaYaw

        lastRoll = roll
        lastPitch = pitch
        lastYaw = yaw

        
        if (totalDelta > 0.05f) {
            onMovement()
        }
    }

    private fun enterLowPowerMode() {
        if (!isEnabled) return
        sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_NORMAL)
        isLowPower = true
        onPowerStateChange?.invoke(true)
    }

    private fun exitLowPowerMode() {
        sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
        isLowPower = false
        onPowerStateChange?.invoke(false)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled && isLowPower) {
            exitLowPowerMode()
        }
    }

    fun setOnPowerStateChange(callback: (Boolean) -> Unit) {
        onPowerStateChange = callback
    }
}
