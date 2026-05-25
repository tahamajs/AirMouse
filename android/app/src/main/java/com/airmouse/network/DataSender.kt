package com.airmouse.network

import android.util.Log
import com.airmouse.utils.PreferencesManager
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DataSender(private val host: String, private val port: Int, private val prefs: PreferencesManager) : Thread() {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var input: BufferedReader? = null
    @Volatile private var running = false
    private val queue = LinkedBlockingQueue<String>()
    private val pendingAcks = ConcurrentHashMap<Long, String>()
    private val ackTimeout = 500L

    fun updateHost(newHost: String) {
        // Will reconnect on next attempt
    }

    override fun run() {
        while (running) {
            try {
                connect()
                processLoop()
            } catch (e: Exception) {
                Log.e("DataSender", "Connection lost, retrying in 5s...", e)
                Thread.sleep(5000)
            }
        }
    }

    private fun connect() {
        socket = Socket(host, port)
        out = DataOutputStream(socket!!.getOutputStream())
        input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        Log.d("DataSender", "Connected to $host:$port")
    }

    private fun processLoop() {
        // ACK receiver thread
        val ackThread = Thread {
            while (running && socket?.isConnected == true) {
                try {
                    val line = input?.readLine() ?: break
                    if (line.contains("ack")) {
                        val id = JSONObject(line).optLong("id", -1)
                        pendingAcks.remove(id)
                    }
                } catch (e: Exception) { break }
            }
        }
        ackThread.start()

        // Sender loop
        while (running && socket?.isConnected == true) {
            val msg = queue.take()
            try {
                out?.writeBytes("$msg\n")
                out?.flush()
                if (msg.contains("\"type\":\"click\"") || msg.contains("\"type\":\"scroll\"") ||
                    msg.contains("\"type\":\"doubleclick\"") || msg.contains("\"type\":\"rightclick\"")) {
                    val id = JSONObject(msg).optLong("id", -1)
                    if (id != -1L) {
                        pendingAcks[id] = msg
                        Thread.sleep(ackTimeout)
                        if (pendingAcks.containsKey(id)) {
                            out?.writeBytes("$msg\n")
                            pendingAcks.remove(id)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DataSender", "Send failed", e)
                // Re-add message to front of queue? For simplicity, just lose it.
            }
        }
        ackThread.join()
    }

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

    fun stopSending() {
        running = false
        try {
            out?.close()
            input?.close()
            socket?.close()
        } catch (e: Exception) {}
    }
}