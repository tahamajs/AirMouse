package com.airmouse.utils

import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.airmouse.sensors.SensorService

class BatterySaver {
    private var handler = Handler(Looper.getMainLooper())
    private var isLowPowerMode = false
    private var sensorService: SensorService? = null
    private val idleTimeout = 10000L // 10 seconds of no movement
    private var lastMovementTime = System.currentTimeMillis()
    private val movementThreshold = 0.5f

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (sensorService == null) return
            val now = System.currentTimeMillis()
            val currentMovement = sensorService?.getMovementMagnitude() ?: 0f
            if (currentMovement > movementThreshold) {
                lastMovementTime = now
                if (isLowPowerMode) {
                    // Return to normal
                    sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
                    isLowPowerMode = false
                }
            } else if (now - lastMovementTime > idleTimeout && !isLowPowerMode) {
                // Enter low power
                sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_NORMAL)
                isLowPowerMode = true
            }
            handler.postDelayed(this, 2000)
        }
    }

    fun start(service: SensorService) {
        sensorService = service
        handler.post(checkRunnable)
    }

    fun stop() {
        handler.removeCallbacks(checkRunnable)
        if (isLowPowerMode) {
            sensorService?.setSamplingRate(SensorManager.SENSOR_DELAY_GAME)
        }
        sensorService = null
    }
}