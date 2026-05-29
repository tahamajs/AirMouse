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
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    var onMessage: ((String) -> Unit)? = null

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.i("WebSocket", "Connected to $url")
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage?.invoke(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e("WebSocket", "Failure", t)
                // Auto reconnect after 5 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    connect(url)
                }, 5000)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.i("WebSocket", "Closed")
            }
        })
    }

    fun send(message: String) {
        if (isConnected) {
            webSocket?.send(message)
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

    fun disconnect() {
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        isConnected = false
    }
}