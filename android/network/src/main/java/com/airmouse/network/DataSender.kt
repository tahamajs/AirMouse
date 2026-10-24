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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class DataSender(
    private var host: String,
    val port: Int,
    private val store: ConnectionStore? = null
) {

    companion object {
        const val ACK_TIMEOUT_MS = 500L
        const val MAX_ACK_RETRIES = 3
        const val MOVE_RING_BUFFER_CAPACITY = 32

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
    private val seqCounter = AtomicLong(1)
    private val pendingAcks = ConcurrentHashMap<String, PendingCommand>()

    // 32-message ring buffer for move packets
    data class BufferedMove(val seq: Long, val ts: Long, val dx: Float, val dy: Float, val dt: Float?)
    private val moveRingBuffer = Array<BufferedMove?>(MOVE_RING_BUFFER_CAPACITY) { null }
    private var moveRingHead = 0
    private var moveRingCount = 0
    private val ringLock = Any()

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var running = false

    data class PendingCommand(val id: String, val payload: String, var retries: Int = 0, val timestamp: Long = System.currentTimeMillis())

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

            handleIncomingLine(line)
        }
        disconnectInternal()
    }

    fun handleIncomingLine(line: String) {
        val msg = NetworkMessage.fromJson(line)
        if (msg is NetworkMessage.Ack) {
            if (msg.ackId.isNotBlank()) {
                pendingAcks.remove(msg.ackId)
            }
        } else {
            // Check raw JSON for fallback ack parsing
            runCatching {
                val json = JSONObject(line)
                if (json.optString("type") == "ack") {
                    val p = json.optJSONObject("payload")
                    val id = p?.optString("ack_id")?.takeIf { it.isNotBlank() }
                        ?: p?.optString("id")?.takeIf { it.isNotBlank() }
                        ?: json.optString("id")
                    if (!id.isNullOrBlank()) {
                        pendingAcks.remove(id)
                    }
                }
            }
        }
    }

    fun sendMove(dx: Float, dy: Float, dt: Float? = null) {
        val seq = seqCounter.getAndIncrement()
        val ts = System.currentTimeMillis()
        val move = BufferedMove(seq, ts, dx, dy, dt)
        synchronized(ringLock) {
            moveRingBuffer[moveRingHead] = move
            moveRingHead = (moveRingHead + 1) % MOVE_RING_BUFFER_CAPACITY
            if (moveRingCount < MOVE_RING_BUFFER_CAPACITY) {
                moveRingCount++
            }
        }
        val wire = NetworkMessage.toJson(NetworkMessage.Move(dx, dy, dt), seq = seq, ts = ts)
        sendRaw(wire)
    }

    fun getBufferedMoves(): List<BufferedMove> {
        synchronized(ringLock) {
            val list = mutableListOf<BufferedMove>()
            val start = if (moveRingCount < MOVE_RING_BUFFER_CAPACITY) 0 else moveRingHead
            for (i in 0 until moveRingCount) {
                val idx = (start + i) % MOVE_RING_BUFFER_CAPACITY
                moveRingBuffer[idx]?.let { list.add(it) }
            }
            return list
        }
    }

    fun getPendingAcksCount(): Int = pendingAcks.size

    fun sendClick(button: String = "left"): String = sendCritical("click", button = button)
    fun sendDoubleClick(): String = sendCritical("doubleclick")
    fun sendRightClick(): String = sendCritical("rightclick")
    fun sendScroll(delta: Int): String = sendCritical("scroll", delta = delta)

    fun sendHello(deviceName: String, authToken: String? = null) {
        val seq = seqCounter.getAndIncrement()
        val ts = System.currentTimeMillis()
        val wire = NetworkMessage.toJson(
            NetworkMessage.Hello(name = deviceName, version = "2.0.0", token = authToken),
            seq = seq,
            ts = ts
        )
        sendRaw(wire)
    }

    fun sendGesture(gesture: String, confidence: Float) {
        val seq = seqCounter.getAndIncrement()
        val ts = System.currentTimeMillis()
        val wire = NetworkMessage.toJson(
            NetworkMessage.Gesture(gesture, confidence),
            seq = seq,
            ts = ts
        )
        sendRaw(wire)
    }

    fun sendProximity(isNear: Boolean, distance: Float) {
        val seq = seqCounter.getAndIncrement()
        val ts = System.currentTimeMillis()
        val wire = NetworkMessage.toJson(
            NetworkMessage.Proximity(isNear, distance),
            seq = seq,
            ts = ts
        )
        sendRaw(wire)
    }

    fun sendCommand(command: String, delta: Int = 0) {
        when (command.lowercase()) {
            "click" -> sendClick()
            "doubleclick" -> sendDoubleClick()
            "rightclick" -> sendRightClick()
            "scroll" -> sendScroll(delta)
            "gesture" -> sendGesture("custom", 1.0f)
            else -> Unit
        }
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

    fun sendCritical(type: String, button: String = "left", delta: Int? = null): String {
        val seq = seqCounter.getAndIncrement()
        val ts = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val msg = when (type.lowercase()) {
            "click" -> NetworkMessage.Click(button)
            "doubleclick" -> NetworkMessage.DoubleClick
            "rightclick" -> NetworkMessage.RightClick
            "scroll" -> NetworkMessage.Scroll(delta ?: 0)
            else -> NetworkMessage.Click(button)
        }

        val wire = NetworkMessage.toJson(msg, seq = seq, ts = ts, id = id)
        pendingAcks[id] = PendingCommand(id, wire, 0, ts)
        sendRaw(wire)
        scheduleRetry(id)
        return id
    }

    fun acknowledge(id: String) {
        pendingAcks.remove(id)
    }

    fun isPendingAck(id: String): Boolean = pendingAcks.containsKey(id)

    private fun sendRaw(payload: String) {
        if (!running || !isConnected) return
        scope.launch(Dispatchers.IO) {
            writer?.apply {
                print(payload)
                print('\n')
                flush()
            }
        }
    }

    private fun scheduleRetry(id: String) {
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
