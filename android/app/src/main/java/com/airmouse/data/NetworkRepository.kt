package com.airmouse.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * Repository that manages TCP connection to the PC server.
 * Handles sending messages, receiving ACKs, and optional retransmission.
 * This version is a simplified wrapper around the full DataSender from network package.
 * For a complete implementation, use the DataSender class directly.
 */
class NetworkRepository(private val prefs: PreferencesDataStore) {

    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var input: BufferedReader? = null
    private var isConnected = false

    private val sendQueue = LinkedBlockingQueue<String>()
    private val pendingAcks = ConcurrentHashMap<Long, String>()

    private val _connectionEvents = MutableSharedFlow<String>()
    val connectionEvents: SharedFlow<String> = _connectionEvents.asSharedFlow()

    /**
     * Connects to the PC server. This is a suspend function and should be called from a coroutine.
     */
    suspend fun connect(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            socket?.close()
            socket = Socket(ip, 8080)
            out = DataOutputStream(socket!!.getOutputStream())
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            isConnected = true
            prefs.setLastIp(ip)
            startSenderLoop()
            startAckReceiver()
            _connectionEvents.emit("Connected to $ip")
            true
        } catch (e: Exception) {
            _connectionEvents.emit("Connection failed: ${e.message}")
            false
        }
    }

    private fun startSenderLoop() {
        Thread {
            while (isConnected) {
                try {
                    val msg = sendQueue.take()
                    out?.writeBytes("$msg\n")
                    out?.flush()
                    if (isCritical(msg)) {
                        val id = extractId(msg)
                        if (id != -1L) {
                            pendingAcks[id] = msg
                            // Retransmit if no ACK after 500 ms
                            Thread.sleep(500)
                            if (pendingAcks.containsKey(id)) {
                                out?.writeBytes("$msg\n")
                                pendingAcks.remove(id)
                            }
                        }
                    }
                } catch (e: Exception) {
                    isConnected = false
                    break
                }
            }
        }.start()
    }

    private fun startAckReceiver() {
        Thread {
            while (isConnected) {
                try {
                    val line = input?.readLine() ?: break
                    if (line.contains("\"type\":\"ack\"")) {
                        val id = JSONObject(line).optLong("id", -1)
                        pendingAcks.remove(id)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }

    private fun isCritical(msg: String): Boolean {
        return msg.contains("\"type\":\"click\"") ||
                msg.contains("\"type\":\"doubleclick\"") ||
                msg.contains("\"type\":\"rightclick\"") ||
                msg.contains("\"type\":\"scroll\"")
    }

    private fun extractId(msg: String): Long {
        return try {
            JSONObject(msg).optLong("id", -1)
        } catch (e: Exception) {
            -1
        }
    }

    fun sendMove(dx: Float, dy: Float) {
        val json = JSONObject().apply {
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        sendQueue.offer(json.toString())
    }

    fun sendClick() {
        val json = JSONObject().apply {
            put("type", "click")
            put("id", System.currentTimeMillis())
        }
        sendQueue.offer(json.toString())
    }

    fun sendDoubleClick() {
        val json = JSONObject().apply {
            put("type", "doubleclick")
            put("id", System.currentTimeMillis())
        }
        sendQueue.offer(json.toString())
    }

    fun sendRightClick() {
        val json = JSONObject().apply {
            put("type", "rightclick")
            put("id", System.currentTimeMillis())
        }
        sendQueue.offer(json.toString())
    }

    fun sendScroll(delta: Int) {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("delta", delta)
            put("id", System.currentTimeMillis())
        }
        sendQueue.offer(json.toString())
    }

    fun disconnect() {
        isConnected = false
        try {
            out?.close()
            input?.close()
            socket?.close()
        } catch (e: Exception) { }
    }
}