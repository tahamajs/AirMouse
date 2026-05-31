// app/src/main/java/com/airmouse/network/WebSocketManager.kt
package com.airmouse.network

import android.os.Handler
import android.os.Looper
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

    fun sendClick(button: String = "left") {
        val json = JSONObject().apply {
            put("type", "click")
            put("payload", JSONObject().apply { put("button", button) })
        }
        send(json.toString())
    }

    fun sendDoubleClick() {
        val json = JSONObject().apply { put("type", "doubleclick") }
        send(json.toString())
    }

    fun sendRightClick() { sendClick("right") }

    fun sendHello(name: String, version: String = "android-1.0") {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
            })
        }
        send(json.toString())
    }

    fun sendProximity(isNear: Boolean, distance: Float) {
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("is_near", isNear)
                put("distance", distance)
            })
        }
        send(json.toString())
    }

    // NEW: Send pause/resume command
    fun sendPauseMovement(pause: Boolean) {
        val json = JSONObject().apply {
            put("type", "control")
            put("payload", JSONObject().apply {
                put("command", if (pause) "pause_movement" else "resume_movement")
            })
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