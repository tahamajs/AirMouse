package com.airmouse.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class TcpClient(
    private var host: String = "",
    private var port: Int = 0,
    private var onLine: (String) -> Unit = {},
    private val onConnected: () -> Unit = {},
    private val onDisconnected: () -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    @Volatile
    var isConnected: Boolean = false
        private set

    fun start() {
        scope.launch {
            try {
                socket = Socket().apply {
                    connect(InetSocketAddress(host, port), 3000)
                }
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream()))
                isConnected = true
                onConnected()

                while (isActive && socket?.isConnected == true) {
                    val line = reader?.readLine() ?: break
                    onLine(line)
                }
            } catch (_: Exception) {
                delay(250)
            } finally {
                closeInternal()
                onDisconnected()
            }
        }
    }

    fun connect(host: String, port: Int) {
        this.host = host
        this.port = port
        start()
    }

    suspend fun send(message: String) {
        sendLine(message)
    }

    fun disconnect() {
        stop()
    }

    suspend fun sendLine(line: String) = withContext(Dispatchers.IO) {
        writer?.apply {
            write(line)
            write("\n")
            flush()
        }
    }

    fun stop() {
        scope.cancel()
        closeInternal()
    }

    private fun closeInternal() {
        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        reader = null
        writer = null
        socket = null
        isConnected = false
    }
}
