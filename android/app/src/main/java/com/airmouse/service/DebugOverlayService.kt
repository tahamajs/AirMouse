//// app/src/main/java/com/airmouse/service/DebugOverlayService.kt
//package com.airmouse.service
//
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.graphics.*
//import android.os.Build
//import android.os.IBinder
//import android.view.Gravity
//import android.view.View
//import android.view.WindowManager
//import android.widget.TextView
//import androidx.core.app.NotificationCompat
//import com.airmouse.R
//import com.airmouse.sensors.SensorService
//import kotlinx.coroutines.*
//
//class DebugOverlayService : Service() {
//    private lateinit var windowManager: WindowManager
//    private var overlayView: View? = null
//    private var textView: TextView? = null
//    private var sensorDataJob: Job? = null
//    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//
//    companion object {
//        private const val NOTIFICATION_ID = 999
//        private const val CHANNEL_ID = "debug_overlay"
//        private const val TAG = "DebugOverlayService"
//
//        fun start(context: Context) {
//            val intent = Intent(context, DebugOverlayService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
//        }
//
//        fun stop(context: Context) {
//            context.stopService(Intent(context, DebugOverlayService::class.java))
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(NOTIFICATION_ID, createNotification("Debug Overlay", "Active"))
//        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        createOverlay()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Debug Overlay",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Shows debug sensor data"
//            }
//            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//        }
//    }
//
//    private fun createNotification(title: String, content: String): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle(title)
//            .setContentText(content)
//            .setSmallIcon(R.drawable.ic_debug)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//    }
//
//    private fun createOverlay() {
//        textView = TextView(this).apply {
//            setBackgroundColor(Color.argb(220, 0, 0, 0))
//            setTextColor(Color.GREEN)
//            textSize = 11f
//            setPadding(16, 16, 16, 16)
//            setTypeface(android.graphics.Typeface.MONOSPACE)
//            maxLines = 20
//        }
//
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            else
//                WindowManager.LayoutParams.TYPE_PHONE,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//            PixelFormat.TRANSLUCENT
//        ).apply {
//            gravity = Gravity.TOP or Gravity.END
//            x = 0
//            y = 100
//        }
//
//        overlayView = textView
//        windowManager.addView(overlayView, params)
//        updateDisplay("Waiting for sensor data...")
//    }
//
//    fun updateSensorData(
//        gyroX: Float, gyroY: Float, gyroZ: Float,
//        accelX: Float, accelY: Float, accelZ: Float,
//        magX: Float = 0f, magY: Float = 0f, magZ: Float = 0f,
//        roll: Float = 0f, pitch: Float = 0f, yaw: Float = 0f
//    ) {
//        val text = buildString {
//            appendLine("📊 SENSOR DATA")
//            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━")
//            appendLine("🔄 GYROSCOPE")
//            appendLine("  X: ${"%.2f".format(gyroX)} rad/s")
//            appendLine("  Y: ${"%.2f".format(gyroY)} rad/s")
//            appendLine("  Z: ${"%.2f".format(gyroZ)} rad/s")
//            appendLine()
//            appendLine("📐 ACCELEROMETER")
//            appendLine("  X: ${"%.2f".format(accelX)} m/s²")
//            appendLine("  Y: ${"%.2f".format(accelY)} m/s²")
//            appendLine("  Z: ${"%.2f".format(accelZ)} m/s²")
//            appendLine()
//            if (magX != 0f || magY != 0f || magZ != 0f) {
//                appendLine("🧲 MAGNETOMETER")
//                appendLine("  X: ${"%.2f".format(magX)} µT")
//                appendLine("  Y: ${"%.2f".format(magY)} µT")
//                appendLine("  Z: ${"%.2f".format(magZ)} µT")
//                appendLine()
//            }
//            appendLine("🧭 ORIENTATION")
//            appendLine("  Roll:  ${"%.1f".format(roll)}°")
//            appendLine("  Pitch: ${"%.1f".format(pitch)}°")
//            appendLine("  Yaw:   ${"%.1f".format(yaw)}°")
//            appendLine()
//            appendLine("⏱️ ${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())}")
//        }
//        updateDisplay(text)
//    }
//
//    private fun updateDisplay(text: String) {
//        textView?.post {
//            textView?.text = text
//        }
//    }
//
//    fun setSensorService(sensorService: SensorService) {
//        // Listen to sensor data
//        sensorDataJob?.cancel()
//        sensorDataJob = serviceScope.launch {
//            sensorService.orientationData.collect { data ->
//                updateSensorData(
//                    gyroX = data.gyroX,
//                    gyroY = data.gyroY,
//                    gyroZ = data.gyroZ,
//                    accelX = data.accelX,
//                    accelY = data.accelY,
//                    accelZ = data.accelZ,
//                    roll = data.roll,
//                    pitch = data.pitch,
//                    yaw = data.yaw
//                )
//            }
//        }
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        sensorDataJob?.cancel()
//        overlayView?.let {
//            try {
//                windowManager.removeView(it)
//            } catch (e: Exception) {
//                // View already removed
//            }
//        }
//        overlayView = null
//        textView = null
//        serviceScope.cancel()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}