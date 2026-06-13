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
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 5000L
    }

    private var socket: DatagramSocket? = null
    private var isScanning = false
    private var scanJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var onServerFound: ((ip: String, port: Int, name: String) -> Unit)? = null
    var onScanComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    fun startDiscovery() {
        if (isScanning) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        isScanning = true
        scanJob = scope.launch {
            try {
                socket = DatagramSocket().apply {
                    soTimeout = TIMEOUT_MS.toInt()
                    broadcast = true
                    reuseAddress = true
                }

                // Send discovery broadcast
                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, DISCOVERY_PORT)
                socket?.send(sendPacket)

                Log.i(TAG, "Discovery broadcast sent")

                // Listen for responses
                val buffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()
                val foundServers = mutableSetOf<String>()

                while (isScanning && System.currentTimeMillis() - startTime < SCAN_DURATION_MS) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket?.receive(receivePacket)

                        val message = String(receivePacket.data, 0, receivePacket.length)
                        val ip = receivePacket.address.hostAddress
                        
                        // Parse response (format: AIRMOUSE_SERVER:port:name or JSON)
                        val (port, name) = parseResponse(message)
                        
                        val serverKey = "$ip:$port"
                        if (!foundServers.contains(serverKey)) {
                            foundServers.add(serverKey)
                            withContext(Dispatchers.Main) {
                                onServerFound?.invoke(ip, port, name)
                            }
                            Log.d(TAG, "Found server: $ip:$port ($name)")
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout, continue scanning
                        Log.d(TAG, "Discovery timeout, continuing...")
                    } catch (e: Exception) {
                        Log.e(TAG, "Receive error: ${e.message}")
                        onError?.invoke(e.message ?: "Discovery error")
                    }
                }

                withContext(Dispatchers.Main) {
                    onScanComplete?.invoke()
                }
                Log.i(TAG, "Discovery scan complete, found ${foundServers.size} servers")

            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
                onError?.invoke(e.message ?: "Discovery failed")
            } finally {
                stopDiscovery()
            }
        }
    }

    private fun parseResponse(message: String): Pair<Int, String> {
        return when {
            message.startsWith("AIRMOUSE_SERVER") -> {
                val parts = message.split(":")
                val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8080 else 8080
                val name = if (parts.size > 2) parts[2] else "Air Mouse Server"
                Pair(port, name)
            }
            message.startsWith("{") -> {
                try {
                    val json = org.json.JSONObject(message)
                    val port = json.optInt("port", 8080)
                    val name = json.optString("name", "Air Mouse Server")
                    Pair(port, name)
                } catch (e: Exception) {
                    Pair(8080, "Air Mouse Server")
                }
            }
            else -> Pair(8080, "Air Mouse Server")
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