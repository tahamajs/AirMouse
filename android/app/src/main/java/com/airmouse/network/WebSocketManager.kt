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
 * @deprecated Use ConnectionManager instead. This class is kept for backward compatibility
 * and will be removed in a future version.
 *
 * All functionality has been migrated to ConnectionManager which provides:
 * - Unified WebSocket and TCP support
 * - Automatic reconnection
 * - Connection quality monitoring
 * - StateFlow for UI observation
 * - Better error handling and recovery
 */
@Deprecated(
    message = "Use ConnectionManager instead. This class is deprecated and will be removed.",
    replaceWith = ReplaceWith("ConnectionManager")
)
object WebSocketManager {

    private const val TAG = "WebSocketManager"
    private const val NORMAL_CLOSURE_STATUS = 1000
    private const val MAX_RECONNECT_ATTEMPTS = 10
    private const val DEPRECATION_WARNING = "WebSocketManager is deprecated. Please use ConnectionManager instead."

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
    var onReconnecting: ((Int) -> Unit)? = null

    init {
        Log.w(TAG, DEPRECATION_WARNING)
    }

    /**
     * Connect to a WebSocket server.
     * @param ip Server IP address
     * @param port Port number (default 8081)
     * @param useSSL Whether to use WSS (SSL)
     */
    @Deprecated("Use ConnectionManager.connect() instead", ReplaceWith("connectionManager.connect(ip, port)"))
    fun connect(ip: String, port: Int = MessageTypes.DEFAULT_WEBSOCKET_PORT, useSSL: Boolean = false) {
        Log.w(TAG, DEPRECATION_WARNING)
        val scheme = if (useSSL) "wss" else "ws"
        val url = "$scheme://$ip:$port/ws"
        connectToUrl(url)
    }

    /**
     * Connect using a full URL.
     */
    @Deprecated("Use ConnectionManager.connect() instead", ReplaceWith("connectionManager.connectToUrl(url)"))
    fun connectToUrl(url: String) {
        Log.w(TAG, DEPRECATION_WARNING)
        if (isConnected.get() && currentUrl == url) return

        disconnect()
        currentUrl = url
        reconnectAttempts = 0

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "AirMouse-Android/3.0")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                reconnectAttempts = 0
                Log.i(TAG, "Connected to $url")
                sendHello()
                onConnected?.invoke()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text: ${text.length} chars")
                onMessage?.invoke(text)
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received binary: ${bytes.size} bytes")
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

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                MessageTypes.TYPE_WELCOME -> {
                    val payload = json.optJSONObject("payload")
                    Log.i(TAG, "Server: ${payload?.optString("server")} v${payload?.optString("version")}")
                }
                MessageTypes.TYPE_PONG -> {
                    // Heartbeat received
                    Log.d(TAG, "Pong received")
                }
                MessageTypes.TYPE_ACK -> {
                    Log.d(TAG, "ACK received for: ${json.optString("id")}")
                }
                MessageTypes.TYPE_ERROR -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    onError?.invoke(error)
                }
            }
        } catch (e: Exception) {
            // Not JSON, ignore
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            onError?.invoke("Max reconnection attempts reached")
            return
        }

        reconnectRunnable?.let {
            android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(it)
        }

        reconnectAttempts++
        val delay = (reconnectAttempts * 2000L).coerceAtMost(30000L)
        Log.i(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
        onReconnecting?.invoke(reconnectAttempts)

        reconnectRunnable = Runnable {
            currentUrl?.let { connectToUrl(it) }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(reconnectRunnable!!, delay)
    }

    /**
     * Disconnect manually.
     */
    @Deprecated("Use ConnectionManager.disconnect() instead", ReplaceWith("connectionManager.disconnect()"))
    fun disconnect() {
        Log.w(TAG, DEPRECATION_WARNING)
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

    @Deprecated("Use ConnectionManager.send() instead", ReplaceWith("connectionManager.send(message)"))
    fun send(message: String): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(message)
    }

    @Deprecated("Use ConnectionManager.sendBinary() instead", ReplaceWith("connectionManager.sendBinary(data)"))
    fun sendBinary(data: ByteArray): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        if (!isConnected.get()) return false
        return webSocket?.send(ByteString.of(*data)) ?: false
    }

    @Deprecated("Use ConnectionManager.sendMove() instead", ReplaceWith("connectionManager.sendMove(dx, dy)"))
    fun sendMove(dx: Float, dy: Float): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_MOVE)
            put("dx", dx)
            put("dy", dy)
        }
        return sendText(json.toString())
    }

    @Deprecated("Use ConnectionManager.sendClick() instead", ReplaceWith("connectionManager.sendClick(button)"))
    fun sendClick(button: String = MessageTypes.BUTTON_LEFT): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_CLICK)
            put("button", button)
        }
        return sendText(json.toString())
    }

    @Deprecated("Use ConnectionManager.sendDoubleClick() instead", ReplaceWith("connectionManager.sendDoubleClick()"))
    fun sendDoubleClick(): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putDoubleClick().toString())
    }

    @Deprecated("Use ConnectionManager.sendRightClick() instead", ReplaceWith("connectionManager.sendRightClick()"))
    fun sendRightClick(): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putRightClick().toString())
    }

    @Deprecated("Use ConnectionManager.sendScroll() instead", ReplaceWith("connectionManager.sendScroll(delta)"))
    fun sendScroll(delta: Int): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_SCROLL)
            put("delta", delta)
        }
        return sendText(json.toString())
    }

    @Deprecated("Use ConnectionManager.sendGesture() instead", ReplaceWith("connectionManager.sendGesture(name, confidence)"))
    fun sendGesture(name: String, confidence: Float): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putGesture(name, confidence).toString())
    }

    @Deprecated("Use ConnectionManager.sendProximity() instead", ReplaceWith("connectionManager.sendProximity(isNear, distance)"))
    fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        val deviceId = "unknown"
        return sendText(JSONObject().putProximity(isNear, distance, deviceId).toString())
    }

    @Deprecated("Use ConnectionManager.sendControl() instead", ReplaceWith("connectionManager.sendControl(command)"))
    fun sendControl(command: String): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putControl(command).toString())
    }

    @Deprecated("Use ConnectionManager.sendHello() instead", ReplaceWith("connectionManager.sendHello(name, version)"))
    fun sendHello(name: String = android.os.Build.MODEL, version: String = MessageTypes.PROTOCOL_VERSION): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putHello(name, version).toString())
    }

    @Deprecated("Use ConnectionManager.sendPing() instead", ReplaceWith("connectionManager.sendPing()"))
    fun sendPing(): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putPing().toString())
    }

    @Deprecated("Use ConnectionManager.sendPong() instead", ReplaceWith("connectionManager.sendPong()"))
    fun sendPong(): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        return sendText(JSONObject().putPong().toString())
    }

    @Deprecated("Use ConnectionManager.isConnected() instead", ReplaceWith("connectionManager.isConnected()"))
    fun isConnected(): Boolean {
        return isConnected.get()
    }

    /**
     * Get current connection URL
     */
    fun getCurrentUrl(): String? = currentUrl

    /**
     * Get reconnect attempts count
     */
    fun getReconnectAttempts(): Int = reconnectAttempts

    /**
     * Reset reconnect attempts
     */
    fun resetReconnectAttempts() {
        reconnectAttempts = 0
    }
}
