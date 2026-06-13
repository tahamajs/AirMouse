// app/src/main/java/com/airmouse/mirroring/ScreenMirroringService.kt
package com.airmouse.mirroring

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.WebSocketManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.inject.Inject

@AndroidEntryPoint
class ScreenMirroringService : Service() {

    @Inject
    lateinit var webSocketManager: WebSocketManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionManager: MediaProjectionManager? = null
    private var isStreaming = false
    private var displayMetrics: DisplayMetrics? = null
    private var frameRate = 15 // frames per second
    private var quality = 50 // JPEG quality (0-100)

    companion object {
        private const val TAG = "ScreenMirroring"
        private const val NOTIFICATION_ID = 5001
        private const val CHANNEL_ID = "screen_mirroring_channel"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenMirroringDisplay"
        private const val REQUEST_CODE = 1001

        // Used to start the service with projection permission
        fun start(context: Context, resultCode: Int, data: Intent, serverUrl: String = "") {
            val intent = Intent(context, ScreenMirroringService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
                putExtra("serverUrl", serverUrl)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenMirroringService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        displayMetrics = resources.displayMetrics
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        val serverUrl = intent?.getStringExtra("serverUrl") ?: ""

        if (resultCode != -1 && data != null) {
            startProjection(resultCode, data)
        }

        if (serverUrl.isNotEmpty()) {
            connectToServer(serverUrl)
        }

        return START_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(mediaProjectionCallback, null)
        setupVirtualDisplay()
    }

    private fun setupVirtualDisplay() {
        val width = displayMetrics?.widthPixels ?: 1080
        val height = displayMetrics?.heightPixels ?: 1920
        val density = displayMetrics?.densityDpi ?: 320

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null && isStreaming) {
                processImage(image)
                image.close()
            }
        }, getBackgroundHandler())

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun processImage(image: android.media.Image) {
        val bitmap = imageToBitmap(image)
        if (bitmap != null) {
            sendFrame(bitmap)
            bitmap.recycle()
        }
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap? {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun sendFrame(bitmap: Bitmap) {
        serviceScope.launch {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val data = stream.toByteArray()
            stream.close()

            // Send over WebSocket (or TCP)
            val frameMessage = """{"type":"frame","data":"${android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)}"}"""
            webSocketManager.send(frameMessage)
        }
    }

    private fun connectToServer(url: String) {
        webSocketManager.connect(url)
        isStreaming = true
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "Screen mirroring started")
    }

    private fun getBackgroundHandler(): Handler {
        val thread = HandlerThread("ScreenMirroringThread")
        thread.start()
        return Handler(thread.looper)
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopMirroring()
        }
    }

    fun stopMirroring() {
        isStreaming = false
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
        virtualDisplay = null
        imageReader = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Screen mirroring stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Mirroring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen mirroring service is active"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirroring")
            .setContentText("Streaming your screen to the connected device")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMirroring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}