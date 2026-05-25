package com.airmouse.network

import android.util.Log
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

class DataSender(private val host: String, private val port: Int) : Thread() {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var input: BufferedReader? = null
    private var running = false
    private val queue = LinkedBlockingQueue<String>()
    private val pendingAcks = ConcurrentHashMap<Long, String>()
    private val ackTimeout = 500L

    override fun run() {
        try {
            socket = Socket(host, port)
            out = DataOutputStream(socket!!.getOutputStream())
            input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
            running = true

            thread {
                while (running) {
                    val line = input?.readLine() ?: break
                    if (line.contains("ack")) {
                        val id = JSONObject(line).optLong("id", -1)
                        pendingAcks.remove(id)
                    }
                }
            }

            while (running) {
                val msg = queue.take()
                out?.writeBytes("$msg\n")
                out?.flush()
                if (msg.contains("\"type\":\"click\"") || msg.contains("\"type\":\"scroll\"")) {
                    val id = JSONObject(msg).optLong("id", -1)
                    pendingAcks[id] = msg
                    Thread.sleep(ackTimeout)
                    if (pendingAcks.containsKey(id)) {
                        out?.writeBytes("$msg\n")
                        pendingAcks.remove(id)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DataSender", "Connection error", e)
        } finally {
            close()
        }
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
        close()
    }

    private fun close() {
        try {
            out?.close()
            input?.close()
            socket?.close()
        } catch (e: Exception) {}
    }
}