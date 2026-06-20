// app/src/main/java/com/airmouse/network/UdpDiscovery.kt
package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UDP Discovery Service – Discovers Air Mouse servers on the local network.
 *
 * This class sends a broadcast message and listens for responses from
 * Air Mouse servers. It can be used to find available servers without
 * manual IP configuration.
 *
 * **Compatibility with Go Server:**
 * - The Go server (internal/protocol/udp/server.go) expects the discovery
 *   message to be exactly "AIRMOUSE_DISCOVER" (no trailing 'Y').
 * - The Go server responds with a JSON object:
 *   {"type":"discovery_response", "port":8080, "ip":"192.168.1.x"}
 *
 * Response formats supported (for compatibility):
 * - JSON: {"type":"discovery_response","port":8080,"ip":"192.168.1.100"}
 * - Legacy: "AIRMOUSE_SERVER:port:name:version"
 * - Simple: "ip:port:name:version"
 *
 * Usage:
 * ```
 * udpDiscovery.onServerFound = { ip, port, name, version ->
 *     // Connect to server
 * }
 * udpDiscovery.startDiscovery()
 * ```
 */
@Singleton
class UdpDiscovery @Inject constructor() {

    companion object {
        private const val TAG = "UdpDiscovery"

        /**
         * IMPORTANT: Must match the Go server's expected discovery message.
         * The Go server in internal/protocol/udp/server.go checks for
         * "AIRMOUSE_DISCOVER" (note: no trailing 'Y').
         */
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVER"

        private const val DISCOVERY_PORT = 8082
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 5000L
        private const val MAX_PACKET_SIZE = 2048
        private const val MAX_SERVERS = 10

        // Different broadcast addresses to try for wider network discovery
        private val BROADCAST_ADDRESSES = listOf(
            "255.255.255.255",
            "192.168.1.255",
            "192.168.0.255",
            "10.0.0.255",
            "10.0.1.255",
            "172.16.0.255",
            "172.16.1.255"
        )
    }

    private var socket: DatagramSocket? = null
    private var isScanning = false
    private var scanJob: Job? = null
    private var foundServers = mutableSetOf<String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    var onServerFound: ((ip: String, port: Int, name: String, version: String) -> Unit)? = null
    var onScanComplete: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onScanStart: (() -> Unit)? = null
    var onScanProgress: ((progress: Int, found: Int) -> Unit)? = null

    /**
     * Start discovering Air Mouse servers on the network.
     * Results will be delivered via onServerFound callback.
     * Scan completes after SCAN_DURATION_MS or when stopped.
     */
    fun startDiscovery() {
        if (isScanning) {
            Log.d(TAG, "Discovery already in progress")
            return
        }

        Log.i(TAG, "Starting UDP discovery on port $DISCOVERY_PORT")
        isScanning = true
        foundServers.clear()
        onScanStart?.invoke()

        scanJob = scope.launch {
            var discoveredServers = 0
            try {
                socket = DatagramSocket().apply {
                    soTimeout = TIMEOUT_MS.toInt()
                    broadcast = true
                    reuseAddress = true
                }

                // Send discovery broadcast to all network interfaces
                sendDiscoveryBroadcasts()

                // Listen for responses
                val buffer = ByteArray(MAX_PACKET_SIZE)
                val startTime = System.currentTimeMillis()

                while (isScanning && System.currentTimeMillis() - startTime < SCAN_DURATION_MS) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket?.receive(receivePacket)

                        val message = String(receivePacket.data, 0, receivePacket.length)
                        val ip = receivePacket.address.hostAddress

                        // Parse the response (supports JSON and legacy formats)
                        val (port, name, version) = parseResponse(message, ip)

                        val serverKey = "$ip:$port"
                        if (!foundServers.contains(serverKey) && foundServers.size < MAX_SERVERS) {
                            foundServers.add(serverKey)
                            discoveredServers++
                            val progress = ((System.currentTimeMillis() - startTime) * 100 / SCAN_DURATION_MS).toInt()

                            withContext(Dispatchers.Main) {
                                onServerFound?.invoke(ip, port, name, version)
                                onScanProgress?.invoke(progress, discoveredServers)
                            }

                            Log.i(TAG, "Found server: $ip:$port ($name) v$version")
                        }
                    } catch (e: SocketTimeoutException) {
                        // Timeout is expected, continue scanning
                        val progress = ((System.currentTimeMillis() - startTime) * 100 / SCAN_DURATION_MS).toInt()
                        withContext(Dispatchers.Main) {
                            onScanProgress?.invoke(progress, discoveredServers)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Receive error: ${e.message}")
                        if (isScanning) {
                            withContext(Dispatchers.Main) {
                                onError?.invoke("Receive error: ${e.message}")
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onScanComplete?.invoke()
                    onScanProgress?.invoke(100, discoveredServers)
                }

                Log.i(TAG, "Discovery scan complete, found $discoveredServers server(s)")

            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError?.invoke(e.message ?: "Discovery failed")
                }
            } finally {
                stopDiscovery()
            }
        }
    }

    /**
     * Send discovery broadcasts to multiple addresses
     */
    private fun sendDiscoveryBroadcasts() {
        val sendData = DISCOVERY_MESSAGE.toByteArray()

        for (address in BROADCAST_ADDRESSES) {
            try {
                val broadcastAddr = InetAddress.getByName(address)
                val sendPacket = DatagramPacket(sendData, sendData.size, broadcastAddr, DISCOVERY_PORT)
                socket?.send(sendPacket)
                Log.d(TAG, "Discovery broadcast sent to $address")
            } catch (e: Exception) {
                Log.d(TAG, "Failed to send broadcast to $address: ${e.message}")
            }
        }
    }

    /**
     * Parse response from server.
     *
     * Supports multiple formats:
     * 1. JSON (Go server format):
     *    {"type":"discovery_response","port":8080,"ip":"192.168.1.100"}
     *
     * 2. Legacy (Air Mouse v2):
     *    "AIRMOUSE_SERVER:8080:Air Mouse:3.0"
     *
     * 3. Simple format:
     *    "192.168.1.100:8080:Air Mouse:3.0"
     */
    private fun parseResponse(message: String, defaultIp: String): Triple<Int, String, String> {
        // Format 1: JSON response (Go server format)
        if (message.startsWith("{") && message.endsWith("}")) {
            try {
                val json = JSONObject(message)
                val type = json.optString("type", "")

                // Only parse if it's a discovery response or if we don't care about type
                if (type.isEmpty() || type == "discovery_response") {
                    val port = json.optInt("port", 8080)
                    // The Go server's JSON does NOT include name or version, so use defaults
                    val name = json.optString("name", "Air Mouse Server")
                    val version = json.optString("version", "3.0")
                    return Triple(port, name, version)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON response: ${e.message}")
            }
        }

        // Format 2: Legacy "AIRMOUSE_SERVER:port:name:version"
        if (message.startsWith("AIRMOUSE_SERVER")) {
            val parts = message.split(":")
            val port = when {
                parts.size > 1 -> parts[1].toIntOrNull() ?: 8080
                else -> 8080
            }
            val name = when {
                parts.size > 2 -> parts[2]
                else -> "Air Mouse Server"
            }
            val version = when {
                parts.size > 3 -> parts[3]
                else -> "3.0"
            }
            return Triple(port, name, version)
        }

        // Format 3: Simple "ip:port:name:version"
        if (message.contains(":")) {
            val parts = message.split(":")
            when (parts.size) {
                2 -> return Triple(parts[1].toIntOrNull() ?: 8080, parts[0], "3.0")
                3 -> return Triple(parts[1].toIntOrNull() ?: 8080, parts[2], "3.0")
                4 -> return Triple(parts[1].toIntOrNull() ?: 8080, parts[2], parts[3])
                else -> return Triple(8080, "Air Mouse Server", "3.0")
            }
        }

        // Default fallback
        return Triple(8080, "Air Mouse Server", "3.0")
    }

    /**
     * Stop the current discovery scan.
     */
    fun stopDiscovery() {
        if (!isScanning) {
            Log.d(TAG, "Discovery not active")
            return
        }

        Log.i(TAG, "Stopping UDP discovery")
        isScanning = false
        scanJob?.cancel()

        try {
            socket?.close()
        } catch (e: Exception) {
            Log.d(TAG, "Error closing socket: ${e.message}")
        }

        socket = null
        Log.d(TAG, "Discovery stopped")
    }

    /**
     * Check if discovery is currently running.
     */
    fun isScanning(): Boolean = isScanning

    /**
     * Perform a quick scan (shorter duration).
     */
    fun quickScan() {
        if (isScanning) {
            stopDiscovery()
        }
        startDiscovery()
    }

    /**
     * Send a discovery request to a specific IP (unicast).
     * @param ip Target IP address
     * @param port Target port (default DISCOVERY_PORT)
     */
    suspend fun probeServer(ip: String, port: Int = DISCOVERY_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tempSocket = DatagramSocket()
                tempSocket.soTimeout = TIMEOUT_MS.toInt()

                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val address = InetAddress.getByName(ip)
                val sendPacket = DatagramPacket(sendData, sendData.size, address, port)
                tempSocket.send(sendPacket)

                val buffer = ByteArray(MAX_PACKET_SIZE)
                val receivePacket = DatagramPacket(buffer, buffer.size)

                try {
                    tempSocket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val (responsePort, name, version) = parseResponse(response, ip)
                    tempSocket.close()

                    withContext(Dispatchers.Main) {
                        onServerFound?.invoke(ip, responsePort, name, version)
                    }
                    return@withContext true
                } catch (e: SocketTimeoutException) {
                    tempSocket.close()
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Probe failed for $ip: ${e.message}")
                return@withContext false
            }
        }
    }

    /**
     * Get all discovered servers
     */
    fun getDiscoveredServers(): List<Map<String, Any>> {
        // Convert found servers to a list
        return foundServers.map { key ->
            val parts = key.split(":")
            mutableMapOf<String, Any>().apply {
                put("ip", parts.getOrElse(0) { "" })
                put("port", (parts.getOrElse(1) { "8080" }).toIntOrNull() ?: 8080)
            }
        }
    }

    /**
     * Clear discovered servers list
     */
    fun clearDiscoveredServers() {
        foundServers.clear()
    }

    /**
     * Get the current socket status
     */
    fun getSocketStatus(): Map<String, Any> {
        return mapOf(
            "is_scanning" to isScanning,
            "socket_open" to (socket?.isConnected ?: false),
            "found_servers" to foundServers.size,
            "max_servers" to MAX_SERVERS,
            "timeout_ms" to TIMEOUT_MS,
            "scan_duration_ms" to SCAN_DURATION_MS
        )
    }

    /**
     * Get the number of found servers
     */
    fun getFoundServerCount(): Int = foundServers.size

    /**
     * Check if a server was found at the given IP and port
     */
    fun isServerFound(ip: String, port: Int = 8080): Boolean {
        return foundServers.contains("$ip:$port")
    }

    /**
     * Get the list of found server IPs
     */
    fun getFoundServerIPs(): List<String> {
        return foundServers.map { key ->
            key.substringBefore(":")
        }
    }
}
