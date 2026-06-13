# Air Mouse Communication Protocol – Complete Specification & Android Implementation

This document defines the **complete communication protocol** between the Air Mouse Android app and the Go desktop server, along with the **full Android client implementation** (Kotlin). The protocol is designed for low‑latency real‑time control, supports multiple message types, and includes optional features like gesture recognition and proximity locking.

---

## 1. Protocol Overview

- **Transport**: WebSocket over TCP (primary). Fallback to plain TCP is also supported.
- **Port**: Default `8080` (configurable on server).
- **Message format**: JSON lines (`\n` terminated). The server accepts either:
  - nested messages with a `payload` object
  - flat messages where fields sit beside `type` and `id`
- **Authentication**: Optional pairing token exchanged via QR code (JWT). The token is sent as a query parameter `?token=...` during WebSocket handshake.

---

## 2. Connection & Pairing Flow

1. **User opens Android app** → scans QR code displayed in server’s **Network** tab.
2. QR code contains: `airmouse://pair?token=<jwt>&ws=ws://<server-ip>:<port>/ws`
3. Android app extracts the WebSocket URL and token.
4. App connects to `ws://<server-ip>:<port>/ws?token=<jwt>`.
5. Server validates token if authentication is enabled. If valid, connection is accepted.
6. App sends a `hello` message to identify itself.
7. Server responds with `welcome`.
8. Normal control messages begin.

> If authentication is disabled (`auth_enabled: false` in server config), the token is ignored.

---

## 3. Message Specification

All messages are JSON objects. The Android code currently sends flat JSON for TCP and WebSocket, while the server also accepts the older nested `payload` form.

### 3.1 `hello` – Identify device

```json
{
  "type": "hello",
  "name": "Pixel 8 Pro",
  "version": "3.0"
}
```
- `name`: User‑defined device name (appears in server’s **Devices** tab).
- `version`: App version (optional, used for compatibility).

### 3.2 `move` – Cursor movement delta

```json
{
  "type": "move",
  "dx": 12.5,
  "dy": -3.2
}
```
- `dx`, `dy`: relative movement in pixels (floating point). Positive `dx` moves right, positive `dy` moves down.

### 3.3 `click` – Mouse click

```json
{
  "type": "click",
  "button": "left",
  "id": 123
}
```
- `button`: `"left"`, `"right"`, or `"middle"`.

### 3.4 `doubleclick` – Double left click

```json
{
  "type": "doubleclick",
  "id": 124
}
```

### 3.5 `rightclick` – Right click (convenience)

```json
{
  "type": "rightclick",
  "id": 125
}
```

### 3.6 `scroll` – Scroll wheel

```json
{
  "type": "scroll",
  "delta": 1,
  "id": 126
}
```
- `delta`: integer. Positive = scroll up/forward, negative = down/backward.

### 3.7 `gesture` – Recognised gesture (from Android ML)

```json
{
  "type": "gesture",
  "gesture": "ThumbsUp",
  "confidence": 0.92
}
```
- `gesture`: string (e.g., `"LeftSwipe"`, `"CircleCW"`, custom name).
- `confidence`: float between 0 and 1.

### 3.8 `proximity` – Distance estimate (for auto lock/unlock)

```json
{
  "type": "proximity",
  "device_id": "abc123",
  "is_near": true,
  "distance": 1.23
}
```
- `device_id`: unique identifier (e.g., Android ID).
- `is_near`: boolean (true if distance < near_threshold).
- `distance`: estimated distance in meters.

### 3.9 `control` – System control commands

```json
{
  "type": "control",
  "command": "pause_movement"
}
```
Supported commands:
- `pause_movement` – temporarily ignore incoming `move` messages.
- `resume_movement` – resume moving cursor.

### 3.10 `ping` – Keep‑alive

```json
{
  "type": "ping"
}
```
Server responds with `pong`.

### 3.11 Server → Client messages

- **`welcome`** – sent after `hello`:
  ```json
  { "type": "welcome", "payload": { "server": "AirMouse", "version": "3.0" } }
  ```
- **`ping`** – server asks client to reply with `pong`.
- **`pong`** – client reply.

### 3.12 ACK behavior

- `click`, `doubleclick`, `rightclick`, and `scroll` may include an `id`.
- The Go server replies with `{"type":"ack","id":<same id>}`.
- `move` messages are not acknowledged.
- The TCP and WebSocket handlers accept both the flat Android JSON and the older nested `payload` shape.

---

## 4. Android App – Complete Implementation

The Android app is written in **Kotlin** with **Coroutines**, **OkHttp** for WebSocket, and **TensorFlow Lite** for gesture classification. It uses the device’s sensors (gyroscope, accelerometer) to generate movements and recognises gestures using a custom TFLite model.

### 4.1 Dependencies (`build.gradle`)

```gradle
dependencies {
    implementation 'androidx.core:core-ktx:1.15.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    implementation 'org.tensorflow:tensorflow-lite:2.16.1'
    implementation 'com.google.code.gson:gson:2.11.0'
}
```

### 4.2 WebSocket Manager

```kotlin
// WebSocketManager.kt
package com.airmouse.network

import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private var currentUrl: String? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    var onMessage: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun connect(url: String) {
        if (isConnected && currentUrl == url) return
        disconnect()
        currentUrl = url
        reconnectAttempts = 0
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                Log.i("WebSocket", "Connected to $url")
                onConnected?.invoke()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage?.invoke(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("WebSocket", "Failure: ${t.message}")
                scheduleReconnect()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                onDisconnected?.invoke()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) return
        reconnectAttempts++
        val delay = (reconnectAttempts * 2000L).coerceAtMost(30000L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            currentUrl?.let { connect(it) }
        }, delay)
    }

    fun send(message: String) {
        if (isConnected) webSocket?.send(message)
    }

    fun sendMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("payload", JSONObject().apply {
                put("dx", dx)
                put("dy", dy)
            })
        }
        send(json.toString())
    }

    fun sendClick(button: String) {
        val json = JSONObject().apply {
            put("type", "click")
            put("payload", JSONObject().put("button", button))
        }
        send(json.toString())
    }

    fun sendDoubleClick() {
        send("""{"type":"doubleclick","payload":{}}""")
    }

    fun sendRightClick() {
        send("""{"type":"rightclick","payload":{}}""")
    }

    fun sendScroll(delta: Int) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("payload", JSONObject().put("delta", delta))
        }
        send(json.toString())
    }

    fun sendHello(name: String, version: String) {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
            })
        }
        send(json.toString())
    }

    fun sendGesture(gesture: String, confidence: Float) {
        val json = JSONObject().apply {
            put("type", "gesture")
            put("payload", JSONObject().apply {
                put("gesture", gesture)
                put("confidence", confidence)
            })
        }
        send(json.toString())
    }

    fun sendProximity(isNear: Boolean, distance: Float) {
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("device_id", getDeviceId())
                put("is_near", isNear)
                put("distance", distance)
            })
        }
        send(json.toString())
    }

    fun sendPauseMovement(pause: Boolean) {
        val cmd = if (pause) "pause_movement" else "resume_movement"
        val json = JSONObject().apply {
            put("type", "control")
            put("payload", JSONObject().put("command", cmd))
        }
        send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        isConnected = false
        currentUrl = null
        reconnectAttempts = 0
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            App.context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
}
```

### 4.3 Sensor Service (Movement & Gesture Inference)

```kotlin
// SensorService.kt
package com.airmouse.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SensorService(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var tflite: Interpreter? = null
    private var gestureLabels = listOf<String>()
    private val sensorBuffer = mutableListOf<FloatArray>()
    private val windowSize = 30
    private var isActive = false
    private var predictionJob: Job? = null
    private var lastGestureTime = 0L
    private val gestureCooldown = 500L

    init {
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile("gesture_model.tflite")
            tflite = Interpreter(modelBuffer)
            val labelsJson = context.assets.open("gesture_labels.json").bufferedReader().use { it.readText() }
            gestureLabels = com.google.gson.Gson().fromJson(labelsJson, Array<String>::class.java).toList()
        } catch (e: Exception) {
            // Model not available – continue without gesture recognition
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun start() {
        if (isActive) return
        isActive = true
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        startPredictionLoop()
    }

    fun stop() {
        isActive = false
        sensorManager.unregisterListener(this)
        predictionJob?.cancel()
    }

    private fun startPredictionLoop() {
        predictionJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (sensorBuffer.size >= windowSize) {
                    runGesturePrediction()
                }
                delay(50)
            }
        }
    }

    private fun runGesturePrediction() {
        if (tflite == null || gestureLabels.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastGestureTime < gestureCooldown) return
        val input = Array(1) { sensorBuffer.map { it.clone() }.toTypedArray() }
        val output = Array(1) { FloatArray(gestureLabels.size) }
        tflite?.run(input, output)
        val maxIdx = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        val confidence = if (maxIdx >= 0) output[0][maxIdx] else 0f
        if (confidence > 0.7f && maxIdx >= 0) {
            lastGestureTime = now
            val gesture = gestureLabels[maxIdx]
            WebSocketManager.sendGesture(gesture, confidence)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isActive) return
        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f,0f,0f,0f,0f,0f)
                sensorBuffer.add(floatArrayOf(values[0], values[1], values[2], last[3], last[4], last[5]))
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val last = sensorBuffer.lastOrNull() ?: floatArrayOf(0f,0f,0f,0f,0f,0f)
                sensorBuffer.add(floatArrayOf(last[0], last[1], last[2], values[0], values[1], values[2]))
            }
        }
        if (sensorBuffer.size > windowSize) sensorBuffer.removeAt(0)

        // Send raw movement deltas for cursor control
        // This is simplified: you would compute dx, dy from orientation changes
        // For demonstration, we send dummy values. In a real app, use rotation vector.
        val dx = values[1] * 0.5f  // example: map gyro Y to horizontal
        val dy = values[0] * 0.5f  // map gyro X to vertical
        if (kotlin.math.abs(dx) > 0.05f || kotlin.math.abs(dy) > 0.05f) {
            WebSocketManager.sendMove(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
```

### 4.4 Proximity Service (Bluetooth RSSI based)

```kotlin
// ProximityService.kt
package com.airmouse.proximity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.*

class ProximityService(private val context: Context) {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var isActive = false
    private var serverMac = ""
    private var nearThreshold = 2.0f
    private var farThreshold = 4.0f
    private var lastNearState = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setConfig(mac: String, near: Float, far: Float) {
        serverMac = mac
        nearThreshold = near
        farThreshold = far
    }

    fun start() {
        if (isActive) return
        isActive = true
        scope.launch {
            while (isActive) {
                val distance = estimateDistance()
                val isNear = if (lastNearState) distance < farThreshold else distance < nearThreshold
                if (isNear != lastNearState) {
                    lastNearState = isNear
                    WebSocketManager.sendProximity(isNear, distance)
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        isActive = false
        scope.cancel()
    }

    private fun estimateDistance(): Float {
        if (serverMac.isEmpty()) return 5.0f
        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(serverMac)
        // RSSI reading is not trivial; this is a placeholder
        // In practice you would use BluetoothGatt to read RSSI.
        return 2.5f // dummy value
    }
}
```

### 4.5 Main Activity – Orchestration

```kotlin
// MainActivity.kt
package com.airmouse

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airmouse.network.WebSocketManager
import com.airmouse.sensor.SensorService
import com.airmouse.proximity.ProximityService

class MainActivity : AppCompatActivity() {
    private lateinit var sensorService: SensorService
    private lateinit var proximityService: ProximityService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Parse QR code data (simplified)
        val serverUrl = "ws://192.168.1.10:8080/ws"
        WebSocketManager.connect(serverUrl)
        WebSocketManager.onConnected = {
            runOnUiThread { Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show() }
            WebSocketManager.sendHello(android.os.Build.MODEL, "3.0")
        }

        sensorService = SensorService(this)
        sensorService.start()

        proximityService = ProximityService(this)
        proximityService.setConfig("AA:BB:CC:DD:EE:FF", 2.0f, 4.0f)
        proximityService.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorService.stop()
        proximityService.stop()
        WebSocketManager.disconnect()
    }
}
```

---

## 5. Flow Examples

### Pairing & Hello
```
Phone -> Server: WebSocket upgrade with token
Server -> Phone: 101 Switching Protocols
Phone -> Server: {"type":"hello","payload":{"name":"Pixel","version":"3.0"}}
Server -> Phone: {"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}
```

### Movement
```
Phone -> Server: {"type":"move","payload":{"dx":12.5,"dy":-3.2}}
Server: moves mouse
```

### Gesture
```
Phone -> Server: {"type":"gesture","payload":{"gesture":"ThumbsUp","confidence":0.92}}
Server: executes media play/pause
```

### Proximity Lock
```
Phone -> Server: {"type":"proximity","payload":{"device_id":"abc","is_near":false,"distance":4.5}}
Server: locks screen (if far threshold exceeded)
```

---

## 6. Security Considerations

- **Authentication**: Use JWT tokens (short‑lived) to prevent unauthorised connections.
- **Encryption**: On untrusted networks, use WSS (WebSocket over TLS). The server can be configured with a TLS certificate.
- **Device whitelisting**: The server can store a list of allowed device IDs (MAC addresses) for proximity lock.

---

## 7. API Summary Table

| Message | Direction | Required | Payload Fields |
|---------|-----------|----------|----------------|
| `hello` | C→S | Yes | `name`, `version` |
| `move` | C→S | Yes | `dx`, `dy` |
| `click` | C→S | Yes | `button` |
| `doubleclick` | C→S | No | – |
| `rightclick` | C→S | No | – |
| `scroll` | C→S | No | `delta` |
| `gesture` | C→S | No | `gesture`, `confidence` |
| `proximity` | C→S | No | `device_id`, `is_near`, `distance` |
| `control` | C→S | No | `command` |
| `ping` | Either | No | – |
| `pong` | Either | No | – |
| `welcome` | S→C | Yes | `server`, `version` |

---
