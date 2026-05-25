package com.airmouse.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.airmouse.sensors.SensorService

class DebugOverlay(private val context: Context) {
    private var textView: TextView? = null
    private var windowManager: WindowManager
    private var isVisible = false
    private var sensorService: SensorService? = null

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun setSensorService(service: SensorService) {
        sensorService = service
    }

    fun toggleVisibility() {
        if (isVisible) hide() else show()
    }

    fun isVisible(): Boolean = isVisible

    fun show() {
        if (isVisible) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 10
        params.y = 100

        textView = TextView(context).apply {
            setBackgroundColor(Color.parseColor("#AA000000"))
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(10, 10, 10, 10)
            text = "Roll: 0.0\nYaw: 0.0\nGyroY: 0.0\nAccelY: 0.0"
        }
        windowManager.addView(textView, params)
        isVisible = true
    }

    fun hide() {
        if (!isVisible) return
        textView?.let { windowManager.removeView(it) }
        textView = null
        isVisible = false
    }

    fun updateValues(roll: Float, yaw: Float, gyroY: Float, accelY: Float) {
        textView?.post {
            textView?.text = String.format("Roll: %.1f°\nYaw: %.1f°\nGyroY: %.2f\nAccelY: %.2f", roll, yaw, gyroY, accelY)
        }
    }
}