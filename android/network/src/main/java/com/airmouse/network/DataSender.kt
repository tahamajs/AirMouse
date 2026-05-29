package com.airmouse.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DataSender(
    private var host: String,
    val port: Int,
    private val store: ConnectionStore? = null
) {

    companion object {
        private const val ACK_TIMEOUT_MS = 500L
        private const val MAX_ACK_RETRIES = 3

        @Volatile
        private var instance: DataSender? = null

        fun getInstance(
            host: String = "",
            port: Int = 8080,
            store: ConnectionStore? = null
        ): DataSender? {
            val current = instance
            if (current != null) {
                if (host.isNotBlank() && store != null) {
                    val needsReplacement = current.host != host || current.port != port
                    if (needsReplacement) {
                        current.stopSending()
                        return DataSender(host, port, store).also { instance = it }
                    }
                }
                return current
            }

            return if (host.isNotBlank() && store != null) {
                DataSender(host, port, store).also { instance = it }
            } else {
                null
            }
        }

        fun setInstance(sender: DataSender) {
            instance = sender
        }
    }

    @Volatile
    var isConnected: Boolean = false
        private set

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val messageId = AtomicLong(0)
    private val pendingAcks = ConcurrentHashMap<Long, PendingCommand>()

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var running = false

    private data class PendingCommand(val payload: String, var retries: Int = 0)

    fun start() {
        if (running) return
        running = true
        scope.launch {
            try {
                connect()
                readLoop()
            } catch (t: Throwable) {
                onError?.invoke(t)
                disconnectInternal()
            }
        }
    }

    private fun connect() {
        val newSocket = Socket(host, port).apply { soTimeout = 5_000 }
        socket = newSocket
        writer = PrintWriter(newSocket.getOutputStream(), true)
        reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))
        isConnected = true
        store?.setLastIp(host)
        store?.setLastPort(port)
        onConnected?.invoke()
    }

    private suspend fun readLoop() {
        while (running) {
            val job = scope.coroutineContext[Job]
            if (job == null || !job.isActive) break

            val line = try {
                reader?.readLine()
            } catch (_: java.net.SocketTimeoutException) {
                null
            }

            if (line.isNullOrBlank()) {
                delay(50)
                continue
            }

            val type = runCatching { JSONObject(line).optString("type") }.getOrDefault("")
            if (type == "ack") {
                val id = runCatching { JSONObject(line).optLong("id", -1L) }.getOrDefault(-1L)
                if (id >= 0) {
                    pendingAcks.remove(id)
                }
            }
        }
        disconnectInternal()
    }

    fun sendMove(dx: Float, dy: Float) {
        sendRaw(MoveMessage(dx = dx, dy = dy).let { JSONObject().apply {
            put("type", it.type)
            put("dx", it.dx)
            put("dy", it.dy)
        }.toString() })
    }

    fun sendClick() = sendCritical("click")
    fun sendDoubleClick() = sendCritical("doubleclick")
    fun sendRightClick() = sendCritical("rightclick")
    fun sendScroll(delta: Int) = sendCritical("scroll", delta = delta)

    fun sendHello(deviceName: String) {
        sendRaw(HelloMessage(name = deviceName).let { JSONObject().apply {
            put("type", it.type)
            put("name", it.name)
        }.toString() })
    }

    fun updateHost(newHost: String) {
        host = newHost
        store?.setLastIp(newHost)
    }

    fun stopSending() {
        running = false
        if (instance === this) {
            instance = null
        }
        scope.cancel()
        disconnectInternal()
    }

    private fun sendCritical(type: String, delta: Int? = null) {
        val id = messageId.incrementAndGet()
        val payload = JSONObject().apply {
            put("type", type)
            put("id", id)
            if (delta != null) put("delta", delta)
        }.toString()
        pendingAcks[id] = PendingCommand(payload)
        sendRaw(payload)
        scheduleRetry(id)
    }

    private fun sendRaw(payload: String) {
        if (!running || !isConnected) return
        writer?.apply {
            print(payload)
            print('\n')
            flush()
        }
    }

    private fun scheduleRetry(id: Long) {
        scope.launch {
            delay(ACK_TIMEOUT_MS)
            val pending = pendingAcks[id] ?: return@launch
            if (!running || !isConnected) return@launch
            if (pending.retries >= MAX_ACK_RETRIES) {
                pendingAcks.remove(id)
                return@launch
            }
            pending.retries++
            sendRaw(pending.payload)
            scheduleRetry(id)
        }
    }

    private fun disconnectInternal() {
        try { writer?.close() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        writer = null
        reader = null
        pendingAcks.clear()
        isConnected = false
        running = false
        onDisconnected?.invoke()
    }
}