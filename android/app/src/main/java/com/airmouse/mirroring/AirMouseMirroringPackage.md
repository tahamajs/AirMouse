# 📘 Air Mouse Mirroring Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.mirroring` package provides **screen mirroring functionality**, allowing the Android device to capture and stream its screen to the PC server in real-time. This enables presentation mode, remote screen viewing, and screen sharing capabilities.

```
com.airmouse.mirroring/
└── ScreenMirroringService.kt       # Screen capture and streaming service
```

---

## 📺 ScreenMirroringService

### Purpose
Captures the device screen in real-time and streams it to the PC server via WebSocket. The service handles screen capture, encoding, transmission, and reconnection logic.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Screen Capture** | Uses MediaProjection API to capture device screen |
| **Encoding** | Compresses frames to JPEG format |
| **Streaming** | Sends encoded frames via WebSocket |
| **Reconnection** | Automatic reconnection with exponential backoff |
| **Foreground Service** | Runs as a foreground service with notification |
| **Configuration** | Configurable frame rate and quality |
| **Battery Optimization** | Throttles frame rate for battery efficiency |

---

## 📦 Service Architecture

### Service Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SERVICE LIFECYCLE                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  onCreate()                                                            │
│     │                                                                   │
│     ▼                                                                   │
│  onStartCommand()                                                      │
│     │                                                                   │
│     ├── Start MediaProjection                                          │
│     │     │                                                             │
│     │     ▼                                                             │
│     │   startProjection()                                              │
│     │     │                                                             │
│     │     ▼                                                             │
│     │   setupVirtualDisplay()                                          │
│     │     │                                                             │
│     │     ▼                                                             │
│     │   imageReader.setOnImageAvailableListener()                     │
│     │     │                                                             │
│     │     └── processImage() → sendFrame()                           │
│     │                                                                   │
│     ├── Connect to server                                              │
│     │     │                                                             │
│     │     ▼                                                             │
│     │   connectToServer()                                              │
│     │     │                                                             │
│     │     └── ConnectionManager.connect()                             │
│     │                                                                   │
│     └── Start foreground service                                       │
│                                                                         │
│  onDestroy()                                                           │
│     │                                                                   │
│     └── stopMirroring()                                                │
│           │                                                             │
│           ├── virtualDisplay.release()                                 │
│           ├── imageReader.close()                                     │
│           ├── mediaProjection.stop()                                  │
│           ├── connectionManager.disconnect()                         │
│           └── stopForeground()                                        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Key Components

### 1. MediaProjection Setup

```kotlin
/**
 * Start the screen projection
 */
private fun startProjection(resultCode: Int, data: Intent) {
    mediaProjection = projectionManager?.getMediaProjection(resultCode, data)
    mediaProjection?.registerCallback(mediaProjectionCallback, null)
    setupVirtualDisplay()
    startForeground(NOTIFICATION_ID, createNotification())
    LogManager.info("MediaProjection started", TAG)
}

/**
 * Set up the virtual display for screen capture
 */
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
    LogManager.info("Virtual display created: ${width}x${height} @ ${density}dpi", TAG)
}
```

### 2. Frame Capture & Encoding

```kotlin
/**
 * Process a captured image frame
 */
private fun processImage(image: Image) {
    val bitmap = imageToBitmap(image) ?: return
    sendFrame(bitmap)
    bitmap.recycle()
}

/**
 * Convert Image to Bitmap
 */
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

/**
 * Send a frame to the server
 */
private fun sendFrame(bitmap: Bitmap) {
    if (captureJob?.isActive == true) return
    captureJob = serviceScope.launch {
        val startTime = System.currentTimeMillis()
        val data = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }
        // Send as binary frame via ConnectionManager
        connectionManager.sendBinary(data)
        // Throttle to target frame rate
        val elapsed = System.currentTimeMillis() - startTime
        val delay = (1000L / frameRate) - elapsed
        if (delay > 0) {
            delay(delay)
        }
    }
}
```

### 3. WebSocket Streaming

```kotlin
/**
 * Connect to the WebSocket server
 */
private fun connectToServer(url: String) {
    serviceScope.launch {
        val uri = URI(url)
        val host = uri.host ?: return@launch
        val port = if (uri.port != -1) uri.port else 8081
        
        connectionManager.setProtocol(ConnectionManager.ConnectionProtocol.WEBSOCKET)
        val success = connectionManager.connect(host, port)
        
        if (success) {
            isConnected = true
            reconnectAttempts = 0
            isStreaming = true
            LogManager.info("Connected to server: $url", TAG)
            Toast.makeText(applicationContext, "Screen mirroring started", Toast.LENGTH_SHORT).show()
            startForeground(NOTIFICATION_ID, createNotification())
        } else {
            LogManager.error("Failed to connect to server", TAG)
            handleDisconnect()
        }
    }
}

/**
 * Handle disconnection with reconnection logic
 */
private fun handleDisconnect() {
    isConnected = false
    isStreaming = false
    if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        serviceScope.launch {
            delay(2000L * reconnectAttempts) // Exponential backoff
            LogManager.info("Reconnecting attempt $reconnectAttempts/$maxReconnectAttempts", TAG)
            connectToServer(serverUrl)
        }
    } else {
        LogManager.error("Max reconnect attempts reached. Stopping service.", TAG)
        stopMirroring()
    }
}
```

### 4. Foreground Service Notification

```kotlin
/**
 * Create notification for foreground service
 */
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
```

---

## 📊 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      SCREEN MIRRORING DATA FLOW                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Screen Capture                                │   │
│  │                                                                  │   │
│  │  Display → MediaProjection → VirtualDisplay → ImageReader      │   │
│  │                                                                    │   │
│  └────────────────────────────────────┬────────────────────────────┘   │
│                                       │                                 │
│                                       ▼                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Image Processing                             │   │
│  │                                                                  │   │
│  │  Image → Bitmap → JPEG Compression (quality configurable)      │   │
│  │                                                                    │   │
│  └────────────────────────────────────┬────────────────────────────┘   │
│                                       │                                 │
│                                       ▼                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Frame Throttling                             │   │
│  │                                                                  │   │
│  │  Calculate elapsed time → Apply delay → Target FPS             │   │
│  │  (Configurable: 15-60 FPS)                                     │   │
│  │                                                                    │   │
│  └────────────────────────────────────┬────────────────────────────┘   │
│                                       │                                 │
│                                       ▼                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Network Transmission                        │   │
│  │                                                                  │   │
│  │  ByteArray → WebSocket (Binary) → PC Server                    │   │
│  │                                                                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Reconnection Logic                          │   │
│  │                                                                  │   │
│  │  Connection Lost → Exponential Backoff → Reconnect Attempt     │   │
│  │  (Max 5 attempts, backoff: 2s, 4s, 8s, 16s, 32s)             │   │
│  │                                                                    │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `frameRate` | Int | 15 | Frames per second (1-60) |
| `quality` | Int | 50 | JPEG compression quality (1-100) |
| `serverUrl` | String | "" | WebSocket server URL |
| `maxReconnectAttempts` | Int | 5 | Maximum reconnection attempts |
| `VIRTUAL_DISPLAY_NAME` | String | "ScreenMirroringDisplay" | Virtual display name |
| `NOTIFICATION_ID` | Int | 5001 | Foreground service notification ID |
| `CHANNEL_ID` | String | "screen_mirroring_channel" | Notification channel ID |

---

## 🔧 Performance Optimizations

### 1. Frame Throttling
```kotlin
val elapsed = System.currentTimeMillis() - startTime
val delay = (1000L / frameRate) - elapsed
if (delay > 0) {
    delay(delay)
}
```

### 2. JPEG Compression
```kotlin
bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
```
- Higher quality = larger file size = more bandwidth
- Lower quality = smaller file size = less bandwidth

### 3. Binary Frame Transmission
```kotlin
connectionManager.sendBinary(data)
```
Binary frames are more efficient than text frames for image data.

### 4. Image Reuse
```kotlin
image.close()
bitmap.recycle()
```
Prevents memory leaks by releasing image resources.

---

## 📋 Public API Summary

### Starting the Service
```kotlin
ScreenMirroringService.start(
    context = context,
    resultCode = resultCode,      // From MediaProjection permission request
    data = data,                  // From MediaProjection permission request
    serverUrl = "ws://192.168.1.100:8081",
    quality = 50,                 // JPEG quality (1-100)
    frameRate = 15                // Frames per second (1-60)
)
```

### Stopping the Service
```kotlin
ScreenMirroringService.stop(context)
```

### Service Intent Extras
| Extra | Type | Description |
|-------|------|-------------|
| `resultCode` | Int | MediaProjection permission result |
| `data` | Intent | MediaProjection permission intent |
| `serverUrl` | String | WebSocket server URL |
| `quality` | Int | JPEG compression quality |
| `frameRate` | Int | Frames per second |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Real-Time Streaming** | Continuous screen capture and transmission |
| **Configurable Quality** | Adjustable frame rate and JPEG quality |
| **Auto-Reconnection** | Exponential backoff reconnection logic |
| **Foreground Service** | Runs in foreground with user notification |
| **Memory Management** | Proper image and bitmap recycling |
| **Thread Safety** | Background threads for capture and transmission |
| **Battery Efficiency** | Throttling to reduce battery consumption |

---

**The ScreenMirroringService enables real-time screen sharing, making it ideal for presentations, remote support, and screen sharing scenarios.**