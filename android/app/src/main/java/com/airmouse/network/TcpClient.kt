// app/src/main/java/com/airmouse/network/TcpClient.kt
package com.airmouse.network

import kotlinx.coroutines.*
import org.json.JSONObject
import timber.log.Timber
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
 * @deprecated Use ConnectionManager instead. This class is kept for backward compatibility
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
        private const val BUFFER_SIZE = 8192
        private const val DEPRECATION_WARNING = "TcpClient is deprecated. Please use ConnectionManager instead."

        // Default timeouts
        private const val DEFAULT_CONNECT_TIMEOUT = 5000
        private const val DEFAULT_READ_TIMEOUT = 30000
        private const val DEFAULT_WRITE_TIMEOUT = 5000
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isConnected = false
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private var currentIp = ""
    private var currentPort = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null
    var onReconnecting: ((Int) -> Unit)? = null
    var onConnectionQuality: ((Int) -> Unit)? = null // ping in ms

    init {
        Timber.w(DEPRECATION_WARNING)
    }

    @Deprecated("Use ConnectionManager.connect() instead", ReplaceWith("connectionManager.connect(ip, port)"))
    suspend fun connect(ip: String, port: Int, timeoutMs: Int = DEFAULT_CONNECT_TIMEOUT): Boolean {
        Timber.w(DEPRECATION_WARNING)
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
                    setPerformancePreferences(0, 2, 1) // Fixed signature layout evaluation
                }
                socket = currentSocket

                writer = PrintWriter(requireNotNull(currentSocket.getOutputStream()), true) // Type-mismatch resolved safely
                reader = BufferedReader(InputStreamReader(currentSocket.getInputStream()))
                isConnected = true
                reconnectAttempts = 0

                startReading()
                startHeartbeat()

                Timber.i("Connected to %s:%d", ip, port)
                onConnected?.invoke()
                return@withContext true
            } catch (e: SocketTimeoutException) {
                Timber.e(e, "Connection timeout: %s", e.message)
                onError?.invoke("Connection timeout")
                return@withContext false
            } catch (e: Exception) {
                Timber.e(e, "Connection failed: %s", e.message)
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
                        Timber.d("End of stream reached")
                        disconnect()
                        withContext(Dispatchers.Main) {
                            onDisconnected?.invoke()
                        }
                    }
                } catch (e: SocketException) {
                    if (isConnected) {
                        Timber.e(e, "Socket error: %s", e.message)
                        disconnect()
                        withContext(Dispatchers.Main) {
                            onDisconnected?.invoke()
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Timeout is expected, continue
                    Timber.d("Read timeout, continuing...")
                } catch (e: Exception) {
                    Timber.e(e, "Read error: %s", e.message)
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isConnected) {
                delay(30000) // 30 seconds
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
                    Timber.i("Server welcome: %s", json.optJSONObject("payload")?.optString("server"))
                }
                "ack" -> {
                    Timber.d("ACK received for: %s", json.optString("id"))
                }
                "error" -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    onError?.invoke(error)
                }
            }
        } catch (e: Exception) {
            // Not JSON, ignore safely
        }
    }

    @Deprecated("Use ConnectionManager.send() instead", ReplaceWith("connectionManager.send(message)"))
    fun send(message: String) {
        Timber.w(DEPRECATION_WARNING)
        if (isConnected && writer != null) {
            try {
                writer?.println(message)
                writer?.flush()
                Timber.d("Sent: %s", message)
            } catch (e: Exception) {
                Timber.e(e, "Send error: %s", e.message)
                onError?.invoke(e.message ?: "Send failed")
            }
        } else {
            Timber.w("Cannot send message: not connected")
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

    fun disconnect() {
        readJob?.cancel()
        heartbeatJob?.cancel()

        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Timber.e(e, "Disconnect error: %s", e.message)
        }

        writer = null
        reader = null
        socket = null
        isConnected = false
        Timber.i("Disconnected")
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

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Timber.w("Max reconnect attempts reached")
            return
        }

        val delay = (reconnectAttempts + 1) * 2000L
        reconnectAttempts++

        Timber.i("Scheduling reconnect in %dms (attempt %d/%d)", delay, reconnectAttempts, maxReconnectAttempts)
        onReconnecting?.invoke(reconnectAttempts)

        scope.launch {
            delay(delay)
            if (!isConnected) {
                connect(currentIp, currentPort)
            }
        }
    }
}