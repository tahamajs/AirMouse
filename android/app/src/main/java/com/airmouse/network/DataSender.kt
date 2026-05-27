package com.airmouse.network

import android.util.Log
import com.airmouse.utils.LogManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * TCP client that sends sensor data to the PC server.
 * Features:
 * - JSON message format with incremental IDs for critical packets.
 * - ACK reception and automatic retransmission on timeout (500 ms).
 * - Non‑blocking coroutine I/O.
 * - Singleton instance (use getInstance()).
 * - Connection status callbacks.
 * - Integration with LogManager for debugging.
 */
class DataSender private constructor(
    private var ip: String,
    private val port: Int,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "DataSender"
        private const val ACK_TIMEOUT_MS = 500L
        private var instance: DataSender? = null

        /**
         * Returns the singleton instance. Creates it if not already existing.
         * @param ip Server IP address (required for first creation)
         * @param port Server port (default 8080)
         * @param prefs PreferencesManager for storing IP history (optional)
         */
        fun getInstance(ip: String = "", port: Int = 8080, prefs: PreferencesManager? = null): DataSender? {
            if (instance == null && ip.isNotEmpty() && prefs != null) {
                instance = DataSender(ip, port, prefs)
            }
            return instance
        }

        /**
         * Updates the instance with a new sender (used by AutoReconnect after reconnecting).
         */
        fun setInstance(sender: DataSender) {
            instance = sender
        }
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    @Volatile private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val messageId = AtomicInteger(0)
    private val pendingAcks = ConcurrentHashMap<Int, String>()   // id -> original message

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null

    /**
     * Establishes connection and starts ACK listener.
     */
    fun start() {
        if (isRunning) return
        scope.launch {
            try {
                LogManager.add("Connecting to $ip:$port...")
                socket = Socket(ip, port).apply {
                    soTimeout = 5000   // read timeout for ACK loop (5 sec)
                }
                writer = PrintWriter(socket?.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                isRunning = true
                LogManager.add("Connected to $ip:$port")
                onConnected?.invoke()
                startAckListener()
            } catch (e: Exception) {
                LogManager.add("Connection failed: ${e.message}")
                onDisconnected?.invoke()
                disconnect()
            }
        }
    }

    /**
     * Listens for ACK messages in background.
     */
    private fun startAckListener() {
        scope.launch {
            while (isRunning) {
                val line = reader?.readLine() ?: break
                if (line.contains("\"type\":\"ack\"")) {
                    val id = extractId(line)
                    if (id != null && pendingAcks.remove(id) != null) {
                        LogManager.add("ACK received for message $id")
                    }
                }
            }
            disconnect()
        }
    }

    /**
     * Extracts the 'id' integer from a JSON ACK message.
     */
    private fun extractId(json: String): Int? {
        return try {
            val regex = "\"id\":(\\d+)".toRegex()
            regex.find(json)?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // ---------- Public send methods ----------

    /**
     * Sends a movement delta (no ACK required).
     */
    fun sendMove(dx: Float, dy: Float) {
        if (!isRunning) return
        val msg = "{\"type\":\"move\",\"dx\":$dx,\"dy\":$dy}\n"
        writer?.print(msg)
        writer?.flush()
    }

    /**
     * Sends a left click (requires ACK and retransmission).
     */
    fun sendClick() {
        sendWithAck("click")
    }

    /**
     * Sends a double click (requires ACK).
     */
    fun sendDoubleClick() {
        sendWithAck("doubleclick")
    }

    /**
     * Sends a right click (requires ACK).
     */
    fun sendRightClick() {
        sendWithAck("rightclick")
    }

    /**
     * Sends a scroll event.
     * @param delta Positive = scroll up, negative = scroll down.
     */
    fun sendScroll(delta: Int) {
        sendWithAck("scroll", delta = delta)
    }

    /**
     * Sends any critical command with an ID, stores it for retransmission,
     * and schedules a timeout retry.
     */
    private fun sendWithAck(type: String, delta: Int = 0) {
        if (!isRunning) return
        val id = messageId.incrementAndGet()
        val msg = if (type == "scroll") {
            "{\"type\":\"$type\",\"delta\":$delta,\"id\":$id}\n"
        } else {
            "{\"type\":\"$type\",\"id\":$id}\n"
        }
        writer?.print(msg)
        writer?.flush()
        LogManager.add("Sent $type (id=$id)")

        pendingAcks[id] = msg

        // Schedule retransmission if no ACK received within timeout
        scope.launch {
            delay(ACK_TIMEOUT_MS)
            if (pendingAcks.containsKey(id)) {
                LogManager.add("Timeout for $type (id=$id), retransmitting...")
                writer?.print(msg)
                writer?.flush()
                // You could add a second retry here, but one retry is enough for most cases
            }
        }
    }

    /**
     * Stops all communication, closes socket, and cancels coroutines.
     */
    fun stopSending() {
        isRunning = false
        scope.cancel()
        disconnect()
    }

    /**
     * Closes socket and streams. Called internally on disconnection.
     */
    private fun disconnect() {
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            writer = null
            reader = null
            socket = null
            onDisconnected?.invoke()
            LogManager.add("Disconnected from server")
        }
    }

    /**
     * Updates the server IP address (used by AutoReconnect after reconnection).
     */
    fun updateHost(newHost: String) {
        ip = newHost
        LogManager.add("Host updated to $ip")
    }
}