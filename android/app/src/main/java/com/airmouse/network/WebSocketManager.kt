// app/src/main/java/com/airmouse/network/WebSocketManager.kt
package com.airmouse.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import com.airmouse.data.model.NetworkMessage
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WebSocketManager {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private var currentUrl: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    var onMessage: ((String) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    fun connect(url: String) {
        if (isConnected && currentUrl == url) {
            Log.d("WebSocket", "Already connected to $url")
            return
        }
        disconnect()
        currentUrl = url
        reconnectAttempts = 0

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                Log.i("WebSocket", "Connected to $url")
                mainHandler.post { onConnected?.invoke() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "Received: $text")
                mainHandler.post { onMessage?.invoke(text) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("WebSocket", "Connection failure: ${t.message}")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.i("WebSocket", "Closed: $reason")
                mainHandler.post { onDisconnected?.invoke() }
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.e("WebSocket", "Max reconnect attempts reached")
            return
        }
        reconnectAttempts++
        val delay = (reconnectAttempts * 2000L).coerceAtMost(30000L)
        Log.d("WebSocket", "Reconnecting in ${delay}ms (attempt $reconnectAttempts)")
        mainHandler.postDelayed({
            currentUrl?.let { connect(it) }
        }, delay)
    }

    fun send(message: String) {
        if (isConnected) {
            webSocket?.send(message)
            Log.d("WebSocket", "Sent: $message")
        } else {
            Log.w("WebSocket", "Not connected, cannot send: $message")
        }
    }

    fun sendMove(dx: Float, dy: Float) {
        send(NetworkMessage.toJson(NetworkMessage.Move(dx, dy)))
    }

    fun sendClick(button: String = "left") {
        send(NetworkMessage.toJson(NetworkMessage.Click(button)))
    }

    fun sendDoubleClick() {
        send(NetworkMessage.toJson(NetworkMessage.DoubleClick))
    }

    fun sendRightClick() {
        send(NetworkMessage.toJson(NetworkMessage.RightClick))
    }

    fun sendScroll(delta: Int) {
        send(NetworkMessage.toJson(NetworkMessage.Scroll(delta)))
    }

    fun sendHello(deviceName: String, version: String = "3.0") {
        send(NetworkMessage.toJson(NetworkMessage.Hello(deviceName, version)))
    }

    fun sendCommand(command: String, delta: Int = 0) {
        when (command.lowercase()) {
            "click" -> sendClick()
            "doubleclick" -> sendDoubleClick()
            "rightclick" -> sendRightClick()
            "scroll" -> sendScroll(delta)
            else -> Log.w("WebSocket", "Unknown command: $command")
        }
    }

    fun sendGesture(gesture: String, confidence: Float) {
        send(NetworkMessage.toJson(NetworkMessage.Gesture(gesture, confidence)))
    }

    fun sendProximity(isNear: Boolean, distance: Float) {
        send(NetworkMessage.toJson(NetworkMessage.Proximity(isNear, distance)))
    }

    fun sendPauseMovement(pause: Boolean) {
        val json = JSONObject().apply {
            put("type", "control")
            put("command", if (pause) "pause_movement" else "resume_movement")
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

    fun isConnected(): Boolean = isConnected
}