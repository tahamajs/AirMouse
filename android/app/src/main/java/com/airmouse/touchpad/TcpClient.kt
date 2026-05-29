
package com.airmouse.touchpad

import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.net.Socket

class TcpClient(private val statusCallback: (String) -> Unit) {

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    var isConnected = false
        private set

    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(ip: String, port: Int) {
        scope.launch {
            try {
                socket = Socket(ip, port)
                writer = OutputStreamWriter(socket!!.getOutputStream())
                isConnected = true
                statusCallback("Connected to $ip:$port")
            } catch (e: Exception) {
                statusCallback("Connection failed: ${e.message}")
            }
        }
    }

    fun send(message: String) {
        scope.launch {
            try {
                writer?.write(message + "\n")
                writer?.flush()
            } catch (e: Exception) {
                isConnected = false
                statusCallback("Send error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                writer?.close()
                socket?.close()
            } catch (_: Exception) {}
            isConnected = false
            statusCallback("Disconnected")
        }
    }
}