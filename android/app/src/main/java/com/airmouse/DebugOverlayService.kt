package com.airmouse

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class DebugOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var textView: TextView

    companion object {
        private var instance: DebugOverlayService? = null
        fun updateData(roll: Float, yaw: Float, gyroY: Float, accelY: Float) {
            instance?.updateText(roll, yaw, gyroY, accelY)
        }
        fun updateConnectionState(state: String) {
            instance?.updateConnectionText(state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.debug_overlay, null)
        textView = overlayView.findViewById(R.id.debugText)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 10
        params.y = 100
        windowManager.addView(overlayView, params)
    }

    private fun updateText(roll: Float, yaw: Float, gyroY: Float, accelY: Float) {
        // Show sensor values and current connection state (if any)
        val conn = textView.getTag() as? String ?: ""
        textView.text = String.format("%s\nRoll: %.1f\nYaw: %.1f\nGyroY: %.2f\nAccelY: %.2f",
            if (conn.isNotBlank()) "Conn: $conn" else "", roll, yaw, gyroY, accelY)
    }

    private fun updateConnectionText(state: String) {
        // store into a tag so updateText can include it
        textView.setTag(state)
        // refresh visible text leaving sensor placeholders intact
        val current = textView.text.toString()
        textView.text = if (current.isNotBlank()) "$state\n$current" else state
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
