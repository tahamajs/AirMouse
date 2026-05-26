package com.airmouse.network

import android.util.Log
import com.airmouse.utils.PreferencesManager
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * TCP client that sends sensor data to the PC server.
 * Supports auto‑reconnect, ACK‑based retransmission for critical packets,
 * and connection status callbacks.
 */
class DataSender(
    private var host: String,
    private val port: Int,
    private val prefs: PreferencesManager
) : Thread() {

    companion object {
        private const val TAG = "DataSender"
        private const val ACK_TIMEOUT_MS = 500L
        private const val RECONNECT_DELAY_MS = 5000L
    }

    // Connection state
    @Volatile private var running = false
    @Volatile private var connected = false
    @Volatile var isConnected = false
        private set

    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var input: BufferedReader? = null

    // Message queue and ACK tracking
    private val queue = LinkedBlockingQueue<String>()
    private val pendingAcks = ConcurrentHashMap<Long, String>()
    private lateinit var ackCheckerThread: Thread
    private lateinit var retransmitThread: Thread

    // Callbacks (optional, for UI feedback)
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null

    /**
     * Update the server host (IP). The change will take effect on next reconnect.
     */
    fun updateHost(newHost: String) {
        host = newHost
        Log.d(TAG, "Host updated to $host")
    }

    /**
     * Starts the sender thread and the ACK checker thread.
     */
    override fun start() {
        running = true
        ackCheckerThread = Thread { processAcks() }
        retransmitThread = Thread { retransmitLoop() }
        ackCheckerThread.start()
        retransmitThread.start()
        super.start()
    }

    override fun run() {
        while (running) {
            try {
                connect()
                processLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}", e)
                onError?.invoke(e)
                if (running) {
                    Log.d(TAG, "Reconnecting in ${RECONNECT_DELAY_MS}ms...")
                    Thread.sleep(RECONNECT_DELAY_MS)
                }
            }
        }
    }

    /**
     * Establishes TCP connection.
     */
    private fun connect() {
        Log.d(TAG, "Connecting to $host:$port...")
        socket = Socket(host, port)
        out = DataOutputStream(socket!!.getOutputStream())
        input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        connected = true
        isConnected = true
        onConnected?.invoke()
        Log.d(TAG, "Connected successfully")
    }

    /**
     * Main processing loop: sends queued messages, handles connection loss.
     */
    private fun processLoop() {
        while (running && connected && socket?.isConnected == true) {
            try {
                val msg = queue.poll(1, TimeUnit.SECONDS) ?: continue
                sendMessage(msg)
            } catch (e: InterruptedException) {
                break
            } catch (e: IOException) {
                Log.e(TAG, "Write error, closing connection", e)
                closeConnection()
                break
            }
        }
    }

    /**
     * Sends a single message over the socket, with retransmission if it's a critical packet.
     */
    private fun sendMessage(msg: String) {
        out?.writeBytes("$msg\n")
        out?.flush()
        Log.v(TAG, "Sent: $msg")

        // Critical packets require ACK
        if (isCriticalMessage(msg)) {
            val id = extractId(msg)
            if (id != -1L) {
                pendingAcks[id] = msg
            }
        }
    }

    /**
     * Checks if a message type requires acknowledgment.
     */
    private fun isCriticalMessage(msg: String): Boolean {
        return msg.contains("\"type\":\"click\"") ||
               msg.contains("\"type\":\"doubleclick\"") ||
               msg.contains("\"type\":\"rightclick\"") ||
               msg.contains("\"type\":\"scroll\"")
    }

    /**
     * Extracts the 'id' field from a JSON message, or -1 if not present.
     */
    private fun extractId(msg: String): Long {
        return try {
            JSONObject(msg).optLong("id", -1)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * ACK receiver thread: reads incoming lines, removes acknowledged packets from pending map.
     */
    private fun processAcks() {
        while (running) {
            try {
                if (!connected || input == null) {
                    Thread.sleep(500)
                    continue
                }
                val line = input?.readLine() ?: continue
                if (line.contains("\"type\":\"ack\"")) {
                    val id = JSONObject(line).optLong("id", -1)
                    if (id != -1L && pendingAcks.remove(id) != null) {
                        Log.v(TAG, "ACK received for id $id")
                    }
                }
            } catch (e: IOException) {
                // Ignore, reconnection will handle
            } catch (e: Exception) {
                Log.e(TAG, "ACK processing error", e)
            }
        }
    }

    /**
     * Retransmission loop: resends messages that have not been acknowledged after timeout.
     */
    private fun retransmitLoop() {
        while (running) {
            try {
                Thread.sleep(ACK_TIMEOUT_MS)
                val toRetransmit = pendingAcks.values.toList()
                for (msg in toRetransmit) {
                    val id = extractId(msg)
                    if (pendingAcks.containsKey(id)) {
                        Log.w(TAG, "Retransmitting message id $id")
                        sendMessage(msg)
                    }
                }
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    /**
     * Closes socket and streams.
     */
    private fun closeConnection() {
        connected = false
        isConnected = false
        try {
            out?.close()
            input?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            out = null
            input = null
            socket = null
            onDisconnected?.invoke()
        }
    }

    // Public API to send various messages (they are queued, non‑blocking)

    fun sendMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        queue.offer(json.toString())
    }

    fun sendClick() {
        val json = JSONObject().apply {
            put("type", "click")
            put("id", System.currentTimeMillis())
        }
        queue.offer(json.toString())
    }

    fun sendDoubleClick() {
        val json = JSONObject().apply {
            put("type", "doubleclick")
            put("id", System.currentTimeMillis())
        }
        queue.offer(json.toString())
    }

    fun sendRightClick() {
        val json = JSONObject().apply {
            put("type", "rightclick")
            put("id", System.currentTimeMillis())
        }
        queue.offer(json.toString())
    }

    fun sendScroll(delta: Int) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("delta", delta)
            put("id", System.currentTimeMillis())
        }
        queue.offer(json.toString())
    }

    /**
     * Stops the sender and all associated threads, closes connection.
     */
    fun stopSending() {
        running = false
        closeConnection()
        interrupt()
        retransmitThread.interrupt()
        ackCheckerThread.interrupt()
    }
}