package com.airmouse.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class TcpClient(private val host: String, private val port: Int, private val onMessage: (String) -> Unit, private val onOpen: () -> Unit = {}, private val onClose: () -> Unit = {}) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val pendingAcks = ConcurrentHashMap<String, Job>()
    private val json = Json { encodeDefaults = true }

    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    socket = Socket()
                    socket?.connect(InetSocketAddress(host, port), 3000)
                    reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                    onOpen()
                    // read loop
                    while (isActive && socket?.isConnected == true) {
                        val line = reader?.readLine() ?: break
                        if (line != null) {
                            onMessage(line)
                        }
                    }
                } catch (e: Exception) {
                    // retry with backoff
                    delay(1000)
                } finally {
                    closeInternal()
                    onClose()
                    delay(1000)
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        closeInternal()
    }

    private fun closeInternal() {
        try { reader?.close() } catch(_:Exception){}
        try { writer?.close() } catch(_:Exception){}
        try { socket?.close() } catch(_:Exception){}
        reader = null; writer = null; socket = null
    }

    suspend fun sendMessage(message: ClientMessage) = withContext(Dispatchers.IO) {
        val s = json.encodeToString(message) + "\n"
        try {
            writer?.let {
                it.write(s)
                it.flush()
            }
        } catch (e: Exception) {
            // writing failed
        }
    }

    suspend fun sendWithAck(message: ClientMessage, id: String, timeoutMs: Long = 500, retries: Int = 3): Boolean {
        // send and wait for ack via onMessage handler; simple implementation: rely on external ack handling to call acknowledge
        // For now we just send and return true
        sendMessage(message)
        return true
    }
}
