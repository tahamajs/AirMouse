package com.airmouse.touchpad

import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

class TcpClient(private val statusCallback: (String) -> Unit) {

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    var isConnected = false
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private var reconnectJob: Job? = null
    private var lastIp: String = ""
    private var lastPort: Int = 0

    /**
     * Connect to the Air Mouse server.
     * If already connected, the previous connection is closed first.
     */
    fun connect(ip: String, port: Int) {
        lastIp = ip
        lastPort = port
        reconnectJob?.cancel()
        scope.launch {
            disconnectInternal()
            try {
                socket = Socket(ip, port)
                writer = OutputStreamWriter(socket!!.getOutputStream())
                isConnected = true
                statusCallback("Connected to $ip:$port")
                // Send any queued messages
                while (messageQueue.isNotEmpty()) {
                    messageQueue.poll()?.let { writer?.write(it + "\n") }
                }
                writer?.flush()
                startReconnectWatcher(ip, port)
            } catch (e: Exception) {
                statusCallback("Connection failed: ${e.message}")
                scheduleReconnect(ip, port)
            }
        }
    }

    /**
     * Send a JSON string. If disconnected, the message is queued and sent when connected.
     */
    fun send(message: String) {
        scope.launch {
            if (isConnected) {
                try {
                    writer?.write(message + "\n")
                    writer?.flush()
                } catch (e: Exception) {
                    isConnected = false
                    statusCallback("Send error: ${e.message}")
                    messageQueue.offer(message)
                    scheduleReconnect(lastIp, lastPort)
                }
            } else {
                messageQueue.offer(message)
            }
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        scope.launch { disconnectInternal() }
    }

    private suspend fun disconnectInternal() {
        try {
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}
        writer = null
        socket = null
        isConnected = false
        statusCallback("Disconnected")
    }

    private fun scheduleReconnect(ip: String, port: Int) {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            while (isActive) {
                delay(2000)
                try {
                    socket = Socket(ip, port)
                    writer = OutputStreamWriter(socket!!.getOutputStream())
                    isConnected = true
                    statusCallback("Reconnected to $ip:$port")
                    while (messageQueue.isNotEmpty()) {
                        messageQueue.poll()?.let { writer?.write(it + "\n") }
                    }
                    writer?.flush()
                    startReconnectWatcher(ip, port)
                    break
                } catch (_: Exception) {
                    // still trying...
                }
            }
        }
    }

    private fun startReconnectWatcher(ip: String, port: Int) {
        scope.launch {
            while (isActive) {
                delay(3000)
                if (!isConnected) {
                    scheduleReconnect(ip, port)
                    break
                }
            }
        }
    }
}