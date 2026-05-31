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
    private lateinit var debugText: TextView

    companion object {
        private var instance: DebugOverlayService? = null
        private var currentText = ""

        fun updateData(roll: Float, yaw: Float, gyroY: Float, accelY: Float, text: String) {
            currentText = text
            instance?.updateText()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.debug_overlay, null)
        debugText = overlayView.findViewById(R.id.debugText)

        // Close button listener
        overlayView.findViewById<View>(R.id.closeDebugBtn).setOnClickListener {
            stopSelf()
        }

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

    private fun updateText() {
        debugText.text = currentText
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        windowManager.removeView(overlayView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}