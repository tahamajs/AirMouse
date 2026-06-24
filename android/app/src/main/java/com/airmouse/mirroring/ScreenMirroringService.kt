package com.airmouse.mirroring

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.inject.Inject

@AndroidEntryPoint
class ScreenMirroringService : Service() {

    @Inject
    lateinit var connectionManager: ConnectionManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var projectionManager: MediaProjectionManager? = null
    private var isStreaming = false
    private var displayMetrics: DisplayMetrics? = null
    private var frameRate = 15          // default
    private var quality = 50            // JPEG quality (1-100)
    private var serverUrl = ""
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var isConnected = false
    private var frameHandler: Handler? = null
    private var captureJob: Job? = null

    companion object {
        private const val TAG = "ScreenMirroring"
        private const val NOTIFICATION_ID = 5001
        private const val CHANNEL_ID = "screen_mirroring_channel"
        private const val VIRTUAL_DISPLAY_NAME = "ScreenMirroringDisplay"

        /**
         * Start the screen mirroring service.
         * @param context Application context
         * @param resultCode The result code from the screen capture permission request
         * @param data The intent data from the screen capture permission request
         * @param serverUrl The WebSocket server URL to connect to (default: empty)
         * @param quality JPEG compression quality (1-100, default 50)
         * @param frameRate Frames per second (default 15)
         */
        fun start(
            context: Context,
            resultCode: Int,
            data: Intent,
            serverUrl: String = "",
            quality: Int = 50,
            frameRate: Int = 15
        ) {
            val intent = Intent(context, ScreenMirroringService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
                putExtra("serverUrl", serverUrl)
                putExtra("quality", quality.coerceIn(1, 100))
                putExtra("frameRate", frameRate.coerceIn(1, 60))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stop the screen mirroring service. */
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
        if (intent?.action == "STOP_MIRRORING") {
            stopMirroring()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        serverUrl = intent?.getStringExtra("serverUrl") ?: ""
        quality = intent?.getIntExtra("quality", 50) ?: 50
        frameRate = intent?.getIntExtra("frameRate", 15) ?: 15

        if (resultCode != -1 && data != null) {
            startProjection(resultCode, data)
        } else {
            Log.e(TAG, "Missing screen capture permission data")
            stopSelf()
            return START_NOT_STICKY
        }

        if (serverUrl.isNotEmpty()) {
            connectToServer(serverUrl)
        } else {
            Log.e(TAG, "No server URL provided")
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    // ============================================================
    // MediaProjection Setup
    // ============================================================

    private fun startProjection(resultCode: Int, data: Intent) {
        mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(mediaProjectionCallback, null)
        setupVirtualDisplay()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "MediaProjection started")
    }

    private fun setupVirtualDisplay() {
        val width = displayMetrics?.widthPixels ?: 1080
        val height = displayMetrics?.heightPixels ?: 1920
        val density = displayMetrics?.densityDpi ?: 320

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null && isStreaming && isConnected) {
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
        Log.i(TAG, "Virtual display created: ${width}x${height} @ ${density}dpi")
    }

    // ============================================================
    // Frame Capture & Encoding
    // ============================================================

    private fun processImage(image: Image) {
        val bitmap = imageToBitmap(image) ?: return
        sendFrame(bitmap)
        bitmap.recycle()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
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
        // Throttle to target frame rate
        if (captureJob?.isActive == true) return
        captureJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val data = ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
            // Send as binary frame via ConnectionManager
            connectionManager.sendBinary(data)
            // Throttle
            val elapsed = System.currentTimeMillis() - startTime
            val delay = (1000L / frameRate) - elapsed
            if (delay > 0) {
                delay(delay)
            }
        }
    }

    // ============================================================
    // WebSocket Connection
    // ============================================================

    private fun connectToServer(url: String) {
        serviceScope.launch {
            // Use the existing ConnectionManager to connect
            // We assume the URL is like "ws://host:port/ws"
            // Parse IP and port from URL
            val uri = URI(url)
            val host = uri.host ?: return@launch
            val port = if (uri.port != -1) uri.port else 8081
            // Configure protocol (WebSocket is default)
            connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.WEBSOCKET)
            // Connect
            val success = connectionManager.connect(host, port)
            if (success) {
                isConnected = true
                reconnectAttempts = 0
                isStreaming = true
                Log.i(TAG, "Connected to server: $url")
                Toast.makeText(applicationContext, "Screen mirroring started", Toast.LENGTH_SHORT).show()
                // Update notification
                startForeground(NOTIFICATION_ID, createNotification())
            } else {
                Log.e(TAG, "Failed to connect to server")
                handleDisconnect()
            }
        }
    }

    private fun handleDisconnect() {
        isConnected = false
        isStreaming = false
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            serviceScope.launch {
                delay(2000L * reconnectAttempts) // Exponential backoff
                Log.i(TAG, "Reconnecting attempt $reconnectAttempts/$maxReconnectAttempts")
                connectToServer(serverUrl)
            }
        } else {
            Log.e(TAG, "Max reconnect attempts reached. Stopping service.")
            stopMirroring()
        }
    }

    // ============================================================
    // Lifecycle & Cleanup
    // ============================================================

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.w(TAG, "MediaProjection stopped externally")
            stopMirroring()
        }
    }

    fun stopMirroring() {
        isStreaming = false
        isConnected = false
        captureJob?.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
        virtualDisplay = null
        imageReader = null
        connectionManager.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Screen mirroring stopped")
        Toast.makeText(applicationContext, "Screen mirroring stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMirroring()
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ============================================================
    // Notification
    // ============================================================

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
        val stopIntent = Intent(this, ScreenMirroringService::class.java).apply {
            action = "STOP_MIRRORING"
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Mirroring")
            .setContentText(if (isConnected) "Streaming active" else "Connecting...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent)
            .build()

        return notification
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun getBackgroundHandler(): Handler {
        val thread = HandlerThread("ScreenMirroringThread")
        thread.start()
        return Handler(thread.looper)
    }
}
