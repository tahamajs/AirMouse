package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * WebSocketManager manages the persistent WSS connection to the AirMouse Desktop Server
 * and enforces strict Protocol V2 framing for all outgoing gesture, move, and action packets.
 */
class WebSocketManager(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "WebSocketManager"
    }

    private var webSocket: WebSocket? = null
    private val sequenceCounter = AtomicLong(0)
    private var pairingToken: String = ""
    private var isConnected: Boolean = false

    /**
     * Connects to the AirMouse server via WSS / WebSocket.
     */
    fun connect(serverUrl: String, token: String, listener: WebSocketListener) {
        this.pairingToken = token
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.i(TAG, "Connected to $serverUrl")
                // Send Protocol V2 Hello
                sendHello()
                listener.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onMessage(webSocket, text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.e(TAG, "Connection failure: ${t.message}", t)
                listener.onFailure(webSocket, t, response)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.i(TAG, "Connection closed: $code / $reason")
                listener.onClosed(webSocket, code, reason)
            }
        })
    }

    /**
     * Wraps and sends an AI spatial gesture payload inside the canonical Protocol V2 envelope.
     * 
     * Schema:
     * {
     *   "v": 2,
     *   "type": "gesture",
     *   "seq": <monotonic_long>,
     *   "ts": <unix_ms>,
     *   "id": "<uuid>",
     *   "token": "<hmac_token>",
     *   "payload": {
     *     "name": "<gesture_name>",
     *     "confidence": <float>,
     *     "parameters": { ... }
     *   }
     * }
     */
    fun sendGesture(gestureName: String, confidence: Float, parameters: Map<String, Any> = emptyMap()): Boolean {
        val payload = JSONObject().apply {
            put("name", gestureName)
            put("confidence", confidence.toDouble())
            val paramsObj = JSONObject()
            parameters.forEach { (k, v) -> paramsObj.put(k, v) }
            put("parameters", paramsObj)
        }

        return sendV2Envelope(type = "gesture", payload = payload)
    }

    /**
     * Sends mouse displacement (dx, dy) inside the Protocol V2 envelope.
     */
    fun sendMove(dx: Float, dy: Float, dt: Float): Boolean {
        val payload = JSONObject().apply {
            put("dx", dx.toDouble())
            put("dy", dy.toDouble())
            put("dt", dt.toDouble())
        }
        return sendV2Envelope(type = "move", payload = payload)
    }

    /**
     * Sends mouse clicks inside the Protocol V2 envelope.
     */
    fun sendClick(button: String): Boolean {
        val payload = JSONObject().apply {
            put("button", button)
        }
        return sendV2Envelope(type = "click", payload = payload)
    }

    /**
     * Sends the V2 Hello Handshake.
     */
    private fun sendHello() {
        val payload = JSONObject().apply {
            put("device_id", UUID.randomUUID().toString())
            put("name", "Android AirMouse Client")
            put("version", "2.1.0")
            put("protocol", "WEBSOCKET")
            put("token", pairingToken)
        }
        sendV2Envelope(type = "hello", payload = payload)
    }

    /**
     * Constructs and serializes the complete Protocol V2 envelope.
     */
    private fun sendV2Envelope(type: String, payload: JSONObject): Boolean {
        val ws = webSocket ?: return false
        val seq = sequenceCounter.incrementAndGet()
        val ts = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val envelope = JSONObject().apply {
            put("v", 2)
            put("type", type)
            put("seq", seq)
            put("ts", ts)
            put("id", id)
            if (pairingToken.isNotEmpty()) {
                put("token", pairingToken)
            }
            put("payload", payload)
        }

        return ws.send(envelope.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }
}
