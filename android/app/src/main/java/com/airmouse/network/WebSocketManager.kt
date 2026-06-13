// app/src/main/java/com/airmouse/network/WebSocketManager.kt
package com.airmouse.network

import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Singleton WebSocket manager for real‑time bidirectional communication.
 * Handles text and binary messages, auto‑reconnect, and callbacks.
 */
object WebSocketManager {

    private const val TAG = "WebSocketManager"
    private const val NORMAL_CLOSURE_STATUS = 1000
    private const val RECONNECT_DELAY_MS = 3000L
    private const val MAX_RECONNECT_ATTEMPTS = 10

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private var reconnectAttempts = 0
    private var currentUrl: String? = null
    private var reconnectRunnable: Runnable? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    // Public callbacks
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    var onBinaryMessage: ((ByteArray) -> Unit)? = null

    /**
     * Connect to a WebSocket server.
     * @param ip Server IP address
     * @param port Port number (default 8081)
     * @param useSSL Whether to use WSS (SSL)
     */
    fun connect(ip: String, port: Int = 8081, useSSL: Boolean = false) {
        val scheme = if (useSSL) "wss" else "ws"
        val url = "$scheme://$ip:$port/ws"
        connectToUrl(url)
    }

    /**
     * Connect using a full URL.
     */
    fun connectToUrl(url: String) {
        if (isConnected.get() && currentUrl == url) return

        disconnect()
        currentUrl = url
        reconnectAttempts = 0

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                reconnectAttempts = 0
                Log.i(TAG, "Connected to $url")
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text: $text")
                onMessage?.invoke(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received binary: ${bytes.size()} bytes")
                onBinaryMessage?.invoke(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Closing: $reason")
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                Log.i(TAG, "Closed: $reason")
                onDisconnected?.invoke()
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                Log.e(TAG, "Error: ${t.message}")
                onError?.invoke(t.message ?: "Unknown error")
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }

        reconnectRunnable?.let {
            android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(it)
        }

        reconnectAttempts++
        val delay = (reconnectAttempts * 2000L).coerceAtMost(30000L)
        Log.i(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")

        reconnectRunnable = Runnable {
            currentUrl?.let { connectToUrl(it) }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(reconnectRunnable!!, delay)
    }

    /**
     * Disconnect manually.
     */
    fun disconnect() {
        reconnectRunnable?.let {
            android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(it)
        }
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Manual disconnect")
        webSocket = null
        isConnected.set(false)
        currentUrl = null
        reconnectAttempts = 0
    }

    // --- Sending helpers -------------------------------------------------

    private fun sendText(message: String): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot send message, not connected")
            return false
        }
        return webSocket?.send(message) ?: false
    }

    fun send(message: String): Boolean = sendText(message)

    fun sendBinary(data: ByteArray): Boolean {
        if (!isConnected.get()) return false
        return webSocket?.send(ByteString.of(*data)) ?: false
    }

    fun sendMove(dx: Float, dy: Float): Boolean {
        val json = JSONObject().apply {
            put("type", "move")
            put("payload", JSONObject().apply {
                put("dx", dx)
                put("dy", dy)
            })
        }
        return sendText(json.toString())
    }

    fun sendClick(button: String = "left"): Boolean {
        val json = JSONObject().apply {
            put("type", "click")
            put("payload", JSONObject().apply {
                put("button", button)
            })
        }
        return sendText(json.toString())
    }

    fun sendDoubleClick(): Boolean {
        return sendText("""{"type":"doubleclick","payload":{}}""")
    }

    fun sendRightClick(): Boolean {
        return sendText("""{"type":"rightclick","payload":{}}""")
    }

    fun sendScroll(delta: Int): Boolean {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("payload", JSONObject().apply {
                put("delta", delta)
            })
        }
        return sendText(json.toString())
    }

    fun sendGesture(name: String, confidence: Float): Boolean {
        val json = JSONObject().apply {
            put("type", "gesture")
            put("payload", JSONObject().apply {
                put("gesture", name)
                put("confidence", confidence)
            })
        }
        return sendText(json.toString())
    }

    fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("is_near", isNear)
                put("distance", distance)
            })
        }
        return sendText(json.toString())
    }

    fun sendControl(command: String): Boolean {
        val json = JSONObject().apply {
            put("type", "control")
            put("payload", JSONObject().apply {
                put("command", command)
            })
        }
        return sendText(json.toString())
    }

    fun sendHello(name: String, version: String): Boolean {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
            })
        }
        return sendText(json.toString())
    }

    fun sendPing(): Boolean = sendText("""{"type":"ping"}""")
    fun sendPong(): Boolean = sendText("""{"type":"pong"}""")

    fun isConnected(): Boolean = isConnected.get()
}