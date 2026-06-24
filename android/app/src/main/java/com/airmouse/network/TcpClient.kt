package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @deprecated Use [ConnectionManager] instead. This class is kept for backward compatibility
 * and will be removed in a future version.
 */
@Deprecated(
    message = "Use ConnectionManager instead. This class is deprecated and will be removed.",
    replaceWith = ReplaceWith("ConnectionManager")
)
@Singleton
@Suppress("DEPRECATION", "UNUSED_PARAMETER", "unused")
class TcpClient @Inject constructor() {

    companion object {
        private const val TAG = "TcpClient"
        private const val BUFFER_SIZE = 8192
        private const val DEPRECATION_WARNING = "TcpClient is deprecated. Please use ConnectionManager instead."

        private const val DEFAULT_CONNECT_TIMEOUT = 5000
        private const val DEFAULT_READ_TIMEOUT = 30000
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val HEARTBEAT_INTERVAL_MS = 30000L
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isConnected = false
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0
    private var currentIp = ""
    private var currentPort = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    var onReconnecting: ((Int) -> Unit)? = null
    var onConnectionQuality: ((Int) -> Unit)? = null

    init {
        Log.w(TAG, DEPRECATION_WARNING)
    }

    /**
     * Connect to a TCP server.
     */
    @Deprecated("Use ConnectionManager.connect() instead", ReplaceWith("connectionManager.connect(ip, port)"))
    suspend fun connect(ip: String, port: Int, timeoutMs: Int = DEFAULT_CONNECT_TIMEOUT): Boolean {
        Log.w(TAG, DEPRECATION_WARNING)
        currentIp = ip
        currentPort = port

        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                val currentSocket = Socket().apply {
                    connect(InetSocketAddress(ip, port), timeoutMs)
                    soTimeout = timeoutMs
                    tcpNoDelay = true
                    keepAlive = true
                    receiveBufferSize = BUFFER_SIZE
                    sendBufferSize = BUFFER_SIZE
                    setPerformancePreferences(0, 2, 1) // latency, bandwidth, time
                }
                socket = currentSocket

                writer = PrintWriter(currentSocket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(currentSocket.getInputStream()))
                isConnected = true
                reconnectAttempts = 0

                startReading()
                startHeartbeat()

                Log.i(TAG, "Connected to $ip:$port")
                onConnected?.invoke()
                return@withContext true
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Connection timeout", e)
                onError?.invoke("Connection timeout")
                return@withContext false
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                onError?.invoke(e.message ?: "Connection failed")
                return@withContext false
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            while (isConnected) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        withContext(Dispatchers.Main) {
                            onMessage?.invoke(line)
                            handleServerMessage(line)
                        }
                    } else {
                        Log.d(TAG, "End of stream reached")
                        disconnect()
                        withContext(Dispatchers.Main) {
                            onDisconnected?.invoke()
                        }
                        scheduleReconnect()
                        break
                    }
                } catch (e: SocketException) {
                    if (isConnected) {
                        Log.e(TAG, "Socket error", e)
                        disconnect()
                        withContext(Dispatchers.Main) {
                            onDisconnected?.invoke()
                        }
                        scheduleReconnect()
                        break
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout is normal – continue
                    Log.d(TAG, "Read timeout, continuing...")
                } catch (e: Exception) {
                    Log.e(TAG, "Read error", e)
                    if (isConnected) {
                        disconnect()
                        withContext(Dispatchers.Main) {
                            onDisconnected?.invoke()
                        }
                        scheduleReconnect()
                        break
                    }
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isConnected) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (isConnected) {
                    sendPing()
                }
            }
        }
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "pong" -> {
                    onConnectionQuality?.invoke(10)
                }
                "welcome" -> {
                    Log.i(TAG, "Server welcome: ${json.optJSONObject("payload")?.optString("server")}")
                }
                "ack" -> {
                    Log.d(TAG, "ACK received for: ${json.optString("id")}")
                }
                "error" -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    onError?.invoke(error)
                }
                else -> {
                    Log.d(TAG, "Unhandled server message type: ${json.optString("type")}")
                }
            }
        } catch (e: Exception) {
            // Non-JSON message – ignore
            Log.d(TAG, "Non-JSON message: $message")
        }
    }

    @Deprecated("Use ConnectionManager.send() instead", ReplaceWith("connectionManager.send(message)"))
    fun send(message: String) {
        Log.w(TAG, DEPRECATION_WARNING)
        if (isConnected && writer != null) {
            try {
                writer?.println(message)
                writer?.flush()
                Log.d(TAG, "Sent: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Send error", e)
                onError?.invoke(e.message ?: "Send failed")
            }
        } else {
            Log.w(TAG, "Cannot send message: not connected")
        }
    }

    @Deprecated("Use ConnectionManager.sendMove() instead", ReplaceWith("connectionManager.sendMove(dx.toFloat(), dy.toFloat())"))
    fun sendMove(dx: Int, dy: Int) {
        send("""{"type":"move","dx":$dx,"dy":$dy}""")
    }

    @Deprecated("Use ConnectionManager.sendClick() instead", ReplaceWith("connectionManager.sendClick(button)"))
    fun sendClick(button: String) {
        send("""{"type":"click","button":"$button"}""")
    }

    @Deprecated("Use ConnectionManager.sendDoubleClick() instead", ReplaceWith("connectionManager.sendDoubleClick()"))
    fun sendDoubleClick() {
        send("""{"type":"doubleclick"}""")
    }

    @Deprecated("Use ConnectionManager.sendRightClick() instead", ReplaceWith("connectionManager.sendRightClick()"))
    fun sendRightClick() {
        send("""{"type":"rightclick"}""")
    }

    @Deprecated("Use ConnectionManager.sendScroll() instead", ReplaceWith("connectionManager.sendScroll(delta)"))
    fun sendScroll(delta: Int) {
        send("""{"type":"scroll","delta":$delta}""")
    }

    @Deprecated("Use ConnectionManager.sendPing() instead", ReplaceWith("connectionManager.sendPing()"))
    fun sendPing() {
        send("""{"type":"ping"}""")
    }

    @Deprecated("Use ConnectionManager.sendHello() instead", ReplaceWith("connectionManager.sendHello(name, version)"))
    fun sendHello(name: String = "Android", version: String = "3.0") {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
            })
        }
        send(json.toString())
    }

    @Deprecated("Use ConnectionManager.sendControl() instead", ReplaceWith("connectionManager.sendControl(command)"))
    fun sendControl(command: String) {
        val json = JSONObject().apply {
            put("type", "control")
            put("payload", JSONObject().apply {
                put("command", command)
            })
        }
        send(json.toString())
    }

    /**
     * Disconnect manually.
     */
    fun disconnect() {
        readJob?.cancel()
        heartbeatJob?.cancel()

        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error", e)
        }

        writer = null
        reader = null
        socket = null
        isConnected = false
        Log.i(TAG, "Disconnected")
    }

    /**
     * Reconnect logic with exponential backoff.
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }

        val delay = (reconnectAttempts + 1) * 2000L
        reconnectAttempts++
        Log.i(TAG, "Scheduling reconnect in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
        onReconnecting?.invoke(reconnectAttempts)

        scope.launch {
            delay(delay)
            if (!isConnected) {
                connect(currentIp, currentPort)
            }
        }
    }

    fun isConnected(): Boolean = isConnected

    fun getRemoteAddress(): String? = socket?.inetAddress?.hostAddress
    fun getRemotePort(): Int = socket?.port ?: -1
    fun getLocalAddress(): String? = socket?.localAddress?.hostAddress
    fun getLocalPort(): Int = socket?.localPort ?: -1
    fun isClosed(): Boolean = socket?.isClosed ?: true

    fun getConnectionInfo(): Map<String, Any> {
        return mapOf(
            "connected" to isConnected,
            "remote_ip" to (getRemoteAddress() ?: "N/A"),
            "remote_port" to getRemotePort(),
            "local_ip" to (getLocalAddress() ?: "N/A"),
            "local_port" to getLocalPort(),
            "closed" to isClosed()
        )
    }
}