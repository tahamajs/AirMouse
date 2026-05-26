// file: app/src/main/java/com/airmouse/ui/DebugOverlay.kt
package com.airmouse.ui

import android.content.Context
import android.content.Intent

class DebugOverlay(private val context: Context) {
    private var isVisible = false

    fun toggleVisibility() {
        if (isVisible) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
            isVisible = false
        } else {
            context.startService(Intent(context, DebugOverlayService::class.java))
            isVisible = true
        }
    }

    fun isVisible(): Boolean = isVisible

    fun updateValues(roll: Float, yaw: Float, gyroY: Float, accelY: Float) {
        DebugOverlayService.updateData(roll, yaw, gyroY, accelY)
    }

    fun setSensorService(service: Any) { /* optional stub */ }

    fun hide() {
        if (isVisible) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
            isVisible = false
        }
    }
}