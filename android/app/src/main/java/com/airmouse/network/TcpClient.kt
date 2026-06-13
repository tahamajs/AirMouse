// app/src/main/java/com/airmouse/network/TcpClient.kt
package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpClient @Inject constructor() {

    companion object {
        private const val TAG = "TcpClient"
        private const val BUFFER_SIZE = 8192
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isConnected = false
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    suspend fun connect(ip: String, port: Int, timeoutMs: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                socket = Socket().apply {
                    connect(InetSocketAddress(ip, port), timeoutMs)
                    soTimeout = timeoutMs
                    tcpNoDelay = true
                    keepAlive = true
                }

                writer = PrintWriter(socket?.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                isConnected = true

                startReading()

                Log.i(TAG, "Connected to $ip:$port")
                onConnected?.invoke()
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
                onError?.invoke(e.message ?: "Connection failed")
                return@withContext false
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            while (isConnected) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        withContext(Dispatchers.Main) {
                            onMessage?.invoke(line)
                        }
                    } else {
                        // End of stream
                        disconnect()
                        onDisconnected?.invoke()
                    }
                } catch (e: SocketException) {
                    if (isConnected) {
                        Log.e(TAG, "Socket error: ${e.message}")
                        disconnect()
                        onDisconnected?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Read error: ${e.message}")
                }
            }
        }
    }

    fun send(message: String) {
        if (isConnected && writer != null) {
            try {
                writer?.println(message)
                writer?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
                onError?.invoke(e.message ?: "Send failed")
            }
        }
    }

    fun sendMove(dx: Int, dy: Int) {
        send("""{"type":"move","dx":$dx,"dy":$dy}""")
    }

    fun sendClick(button: String) {
        send("""{"type":"click","button":"$button"}""")
    }

    fun sendDoubleClick() {
        send("""{"type":"doubleclick"}""")
    }

    fun sendRightClick() {
        send("""{"type":"rightclick"}""")
    }

    fun sendScroll(delta: Int) {
        send("""{"type":"scroll","delta":$delta}""")
    }

    fun disconnect() {
        readJob?.cancel()
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        writer = null
        reader = null
        socket = null
        isConnected = false
        Log.i(TAG, "Disconnected")
    }

    fun isConnected(): Boolean = isConnected
}// app/src/main/java/com/airmouse/network/TcpClient.kt
package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpClient @Inject constructor() {

    companion object {
        private const val TAG = "TcpClient"
        private const val BUFFER_SIZE = 8192
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var isConnected = false
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    suspend fun connect(ip: String, port: Int, timeoutMs: Int = 5000): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                socket = Socket().apply {
                    connect(InetSocketAddress(ip, port), timeoutMs)
                    soTimeout = timeoutMs
                    tcpNoDelay = true
                    keepAlive = true
                }

                writer = PrintWriter(socket?.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket?.getInputStream()))
                isConnected = true

                startReading()

                Log.i(TAG, "Connected to $ip:$port")
                onConnected?.invoke()
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}")
                onError?.invoke(e.message ?: "Connection failed")
                return@withContext false
            }
        }
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            while (isConnected) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        withContext(Dispatchers.Main) {
                            onMessage?.invoke(line)
                        }
                    } else {
                        // End of stream
                        disconnect()
                        onDisconnected?.invoke()
                    }
                } catch (e: SocketException) {
                    if (isConnected) {
                        Log.e(TAG, "Socket error: ${e.message}")
                        disconnect()
                        onDisconnected?.invoke()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Read error: ${e.message}")
                }
            }
        }
    }

    fun send(message: String) {
        if (isConnected && writer != null) {
            try {
                writer?.println(message)
                writer?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Send error: ${e.message}")
                onError?.invoke(e.message ?: "Send failed")
            }
        }
    }

    fun sendMove(dx: Int, dy: Int) {
        send("""{"type":"move","dx":$dx,"dy":$dy}""")
    }

    fun sendClick(button: String) {
        send("""{"type":"click","button":"$button"}""")
    }

    fun sendDoubleClick() {
        send("""{"type":"doubleclick"}""")
    }

    fun sendRightClick() {
        send("""{"type":"rightclick"}""")
    }

    fun sendScroll(delta: Int) {
        send("""{"type":"scroll","delta":$delta}""")
    }

    fun disconnect() {
        readJob?.cancel()
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        writer = null
        reader = null
        socket = null
        isConnected = false
        Log.i(TAG, "Disconnected")
    }

    fun isConnected(): Boolean = isConnected
}