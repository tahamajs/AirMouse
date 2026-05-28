package com.airmouse

import android.content.Context
import android.content.Intent

class DebugOverlay(private val context: Context) {
    private var isVisible = false

    fun toggleVisibility() {
        if (isVisible) {
            hide()
        } else {
            context.startService(Intent(context, DebugOverlayService::class.java))
            isVisible = true
        }
    }

    fun isVisible(): Boolean = isVisible

    fun updateValues(roll: Float, yaw: Float, gyroY: Float, accelY: Float) {
        DebugOverlayService.updateData(roll, yaw, gyroY, accelY)
    }

    fun setSensorService(service: Any) {
        // Reserved for future overlay controls that need direct sensor-service access.
    }

    fun hide() {
        if (isVisible) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
            isVisible = false
        }
    }
}
