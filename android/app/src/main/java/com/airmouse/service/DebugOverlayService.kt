// app/src/main/java/com/airmouse/service/DebugOverlayService.kt
package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.airmouse.sensors.SensorService

class DebugOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var textView: TextView? = null

    companion object {
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "debug_overlay"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    private fun createOverlay() {
        textView = TextView(this).apply {
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setTextColor(Color.GREEN)
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 100
        }

        overlayView = textView
        windowManager.addView(overlayView, params)
    }

    fun updateGyroData(x: Float, y: Float, z: Float) {
        textView?.text = """
            Gyro: X:${"%.2f".format(x)} Y:${"%.2f".format(y)} Z:${"%.2f".format(z)}
        """.trimIndent()
    }

    fun setSensorService(sensorService: SensorService) {
        sensorService.onSensorData = { gyroX, gyroY, gyroZ, accelX, accelY, accelZ ->
            updateGyroData(gyroX, gyroY, gyroZ)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}// app/src/main/java/com/airmouse/service/DebugOverlayService.kt
package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.airmouse.sensors.SensorService

class DebugOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var textView: TextView? = null

    companion object {
        private const val NOTIFICATION_ID = 999
        private const val CHANNEL_ID = "debug_overlay"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
    }

    private fun createOverlay() {
        textView = TextView(this).apply {
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            setTextColor(Color.GREEN)
            textSize = 12f
            setPadding(16, 16, 16, 16)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 100
        }

        overlayView = textView
        windowManager.addView(overlayView, params)
    }

    fun updateGyroData(x: Float, y: Float, z: Float) {
        textView?.text = """
            Gyro: X:${"%.2f".format(x)} Y:${"%.2f".format(y)} Z:${"%.2f".format(z)}
        """.trimIndent()
    }

    fun setSensorService(sensorService: SensorService) {
        sensorService.onSensorData = { gyroX, gyroY, gyroZ, accelX, accelY, accelZ ->
            updateGyroData(gyroX, gyroY, gyroZ)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}