// app/src/main/java/com/airmouse/network/UdpDiscovery.kt
package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscovery @Inject constructor() {

    companion object {
        private const val TAG = "UdpDiscovery"
        private const val DISCOVERY_PORT = 8082
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVERY"
        private const val RESPONSE_MESSAGE = "AIRMOUSE_SERVER"
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 5000L
    }

    private var socket: DatagramSocket? = null
    private var isScanning = false
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onServerFound: ((ip: String, port: Int, name: String) -> Unit)? = null
    var onScanComplete: (() -> Unit)? = null

    fun startDiscovery() {
        if (isScanning) return

        isScanning = true
        scanJob = scope.launch {
            try {
                socket = DatagramSocket()
                socket?.soTimeout = TIMEOUT_MS.toInt()
                socket?.broadcast = true

                // Send discovery broadcast
                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, DISCOVERY_PORT)
                socket?.send(sendPacket)

                Log.i(TAG, "Discovery broadcast sent")

                // Listen for responses
                val buffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()

                while (isScanning && System.currentTimeMillis() - startTime < SCAN_DURATION_MS) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket?.receive(receivePacket)

                        val message = String(receivePacket.data, 0, receivePacket.length)
                        if (message.startsWith(RESPONSE_MESSAGE)) {
                            val parts = message.split(":")
                            val ip = receivePacket.address.hostAddress
                            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8080 else 8080
                            val name = if (parts.size > 2) parts[2] else "Air Mouse Server"

                            withContext(Dispatchers.Main) {
                                onServerFound?.invoke(ip, port, name)
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout, continue scanning
                    } catch (e: Exception) {
                        Log.e(TAG, "Receive error: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    onScanComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
            } finally {
                stopDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        isScanning = false
        scanJob?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
        Log.i(TAG, "Discovery stopped")
    }

    fun isScanning(): Boolean = isScanning
}// app/src/main/java/com/airmouse/network/UdpDiscovery.kt
package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpDiscovery @Inject constructor() {

    companion object {
        private const val TAG = "UdpDiscovery"
        private const val DISCOVERY_PORT = 8082
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVERY"
        private const val RESPONSE_MESSAGE = "AIRMOUSE_SERVER"
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 5000L
    }

    private var socket: DatagramSocket? = null
    private var isScanning = false
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onServerFound: ((ip: String, port: Int, name: String) -> Unit)? = null
    var onScanComplete: (() -> Unit)? = null

    fun startDiscovery() {
        if (isScanning) return

        isScanning = true
        scanJob = scope.launch {
            try {
                socket = DatagramSocket()
                socket?.soTimeout = TIMEOUT_MS.toInt()
                socket?.broadcast = true

                // Send discovery broadcast
                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, DISCOVERY_PORT)
                socket?.send(sendPacket)

                Log.i(TAG, "Discovery broadcast sent")

                // Listen for responses
                val buffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()

                while (isScanning && System.currentTimeMillis() - startTime < SCAN_DURATION_MS) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket?.receive(receivePacket)

                        val message = String(receivePacket.data, 0, receivePacket.length)
                        if (message.startsWith(RESPONSE_MESSAGE)) {
                            val parts = message.split(":")
                            val ip = receivePacket.address.hostAddress
                            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8080 else 8080
                            val name = if (parts.size > 2) parts[2] else "Air Mouse Server"

                            withContext(Dispatchers.Main) {
                                onServerFound?.invoke(ip, port, name)
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout, continue scanning
                    } catch (e: Exception) {
                        Log.e(TAG, "Receive error: ${e.message}")
                    }
                }

                withContext(Dispatchers.Main) {
                    onScanComplete?.invoke()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
            } finally {
                stopDiscovery()
            }
        }
    }

    fun stopDiscovery() {
        isScanning = false
        scanJob?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
        Log.i(TAG, "Discovery stopped")
    }

    fun isScanning(): Boolean = isScanning
}