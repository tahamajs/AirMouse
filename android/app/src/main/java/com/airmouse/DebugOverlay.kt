package com.airmouse

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.airmouse.R

class DebugOverlay(private val context: Context) {
    private var isVisible = false
    private var overlayView: View? = null

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
        val text = String.format(
            "Roll: %.1f°\nYaw: %.1f°\nGyro Y: %.2f rad/s\nAccel Y: %.2f m/s²\n\nFPS: %.0f\nUptime: %s",
            roll, yaw, gyroY, accelY, getFPS(), getUptime()
        )
        DebugOverlayService.updateData(roll, yaw, gyroY, accelY, text)
    }

    private fun getFPS(): Float = 60.0f // placeholder – implement real FPS meter
    private fun getUptime(): String = android.text.format.DateUtils.formatElapsedTime(android.os.SystemClock.elapsedRealtime() / 1000)

    fun setSensorService(service: Any) { /* Reserved for future overlay controls */ }

    fun hide() {
        if (isVisible) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
            isVisible = false
        }
    }
}