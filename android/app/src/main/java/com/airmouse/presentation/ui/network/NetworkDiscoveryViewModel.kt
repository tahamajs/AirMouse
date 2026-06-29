package com.airmouse.presentation.ui.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.notifications.NotificationManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.*
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for discovering Air Mouse servers on the local network.
 * Uses UDP broadcasts and TCP port scanning to find servers.
 */
@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager,
    private val notificationManager: NotificationManager
) : ViewModel() {

    // ============================================================
    // UI State
    // ============================================================

    private val _uiState = MutableStateFlow(NetworkDiscoveryUiState())
    val uiState: StateFlow<NetworkDiscoveryUiState> = _uiState.asStateFlow()

    private val discoveredServersMap = mutableMapOf<String, DiscoveredServer>()
    private var isScanning = false
    private var scanJob: Job? = null

    // ============================================================
    // Constants
    // ============================================================

    companion object {
        private const val DISCOVERY_PORT = 8082
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVERY"
        private const val RESPONSE_MESSAGE = "AIRMOUSE_SERVER"
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 8000L
        private const val SAVED_SERVERS_KEY = "saved_servers_v2"
        private const val CONNECTION_HISTORY_KEY = "connection_history"
    }

    // ============================================================
    // Initialisation
    // ============================================================

    init {
        loadSavedServers()
        loadConnectionHistory()
        observeConnectionStatus()
        startAutoDiscovery()
    }

    // ============================================================
    // Observers
    // ============================================================

    /**
     * Observes the connection status from [ConnectionManager] and updates the UI state.
     */
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                _uiState.update { state ->
                    when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> {
                            val currentIp = connectionManager.currentIp.value
                            val updatedServers = state.savedServers.map {
                                if (it.ip == currentIp) it.copy(isReachable = true) else it
                            }
                            saveServersToPrefs(updatedServers)
                            state.copy(
                                savedServers = updatedServers,
                                isConnecting = false,
                                connectionProgress = 100,
                                errorMessage = null
                            )
                        }
                        ConnectionManager.ConnectionStatus.CONNECTING -> {
                            state.copy(
                                isConnecting = true,
                                connectionProgress = (state.connectionProgress + 5).coerceAtMost(90),
                                errorMessage = null
                            )
                        }
                        ConnectionManager.ConnectionStatus.ERROR -> {
                            val lastError = connectionManager.lastError.value ?: "Connection failed"
                            state.copy(
                                isConnecting = false,
                                errorMessage = lastError
                            )
                        }
                        else -> state
                    }
                }
            }
        }
    }

    // ============================================================
    // Persistence
    // ============================================================

    /**
     * Loads saved servers from SharedPreferences.
     */
    private fun loadSavedServers() {
        val savedJson = prefs.getString(SAVED_SERVERS_KEY, "")
        if (savedJson.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(savedJson)
                val servers = mutableListOf<DiscoveredServer>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val ip = json.optString("ip", "")
                    val port = json.optInt("port", 0)
                    if (ip.isEmpty() || port == 0) continue // skip invalid entries

                    servers.add(
                        DiscoveredServer(
                            id = json.optString("id", UUID.randomUUID().toString()),
                            ip = ip,
                            port = port,
                            name = json.optString("name", "Air Mouse Server"),
                            version = json.optString("version", "3.0.0"),
                            isFavorite = json.optBoolean("isFavorite", false),
                            notes = json.optString("notes", ""),
                            deviceType = DeviceType.fromString(json.optString("deviceType", "UNKNOWN"))
                        )
                    )
                }
                _uiState.update { it.copy(savedServers = servers) }
            } catch (_: Exception) {
                // If parsing fails, ignore and start empty
            }
        }
    }

    /**
     * Saves the list of servers to SharedPreferences.
     */
    private fun saveServersToPrefs(servers: List<DiscoveredServer>) {
        val jsonArray = JSONArray()
        servers.forEach { server ->
            val json = JSONObject().apply {
                put("id", server.id)
                put("ip", server.ip)
                put("port", server.port)
                put("name", server.name)
                put("version", server.version)
                put("isFavorite", server.isFavorite)
                put("notes", server.notes)
                put("deviceType", server.deviceType.name)
            }
            jsonArray.put(json)
        }
        prefs.putString(SAVED_SERVERS_KEY, jsonArray.toString())
    }

    /**
     * Loads connection history from SharedPreferences.
     */
    private fun loadConnectionHistory() {
        val historyJson = prefs.getString(CONNECTION_HISTORY_KEY, "")
        if (historyJson.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(historyJson)
                val history = mutableListOf<ConnectionHistoryItem>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    val ip = json.optString("ip", "")
                    val port = json.optInt("port", 0)
                    if (ip.isEmpty() || port == 0) continue

                    history.add(
                        ConnectionHistoryItem(
                            id = json.optString("id", UUID.randomUUID().toString()),
                            serverId = json.optString("serverId", ""),
                            ip = ip,
                            port = port,
                            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                            serverName = json.optString("serverName", "Unknown"),
                            status = json.optString("status", "Disconnected")
                        )
                    )
                }
                _uiState.update { it.copy(connectionHistory = history.sortedByDescending { it.timestamp }) }
            } catch (_: Exception) {
                // Ignore on parse error
            }
        }
    }

    /**
     * Saves a connection history entry.
     */
    private fun saveConnectionHistory(serverName: String, ip: String, port: Int, status: String) {
        val history = ConnectionHistoryItem(
            id = UUID.randomUUID().toString(),
            serverId = UUID.randomUUID().toString(),
            serverName = serverName,
            ip = ip,
            port = port,
            timestamp = System.currentTimeMillis(),
            status = status
        )

        val currentHistory = _uiState.value.connectionHistory.toMutableList()
        currentHistory.add(0, history)
        if (currentHistory.size > 50) currentHistory.removeAt(currentHistory.lastIndex)

        val jsonArray = JSONArray()
        currentHistory.forEach { record ->
            val json = JSONObject().apply {
                put("id", record.id)
                put("serverId", record.serverId)
                put("serverName", record.serverName)
                put("ip", record.ip)
                put("port", record.port)
                put("timestamp", record.timestamp)
                put("status", record.status)
            }
            jsonArray.put(json)
        }
        prefs.putString(CONNECTION_HISTORY_KEY, jsonArray.toString())
        _uiState.update { it.copy(connectionHistory = currentHistory) }
    }

    // ============================================================
    // Auto Discovery
    // ============================================================

    /**
     * Starts automatic discovery after a delay if the preference is enabled.
     */
    private fun startAutoDiscovery() {
        if (prefs.getBoolean("auto_discovery_enabled", true)) {
            viewModelScope.launch {
                delay(2000)
                if (_uiState.value.discoveredServers.isEmpty()) {
                    scanNetwork()
                }
            }
        }
    }

    /**
     * Toggles the auto‑discovery preference.
     */
    fun toggleAutoDiscovery() {
        val current = prefs.getBoolean("auto_discovery_enabled", true)
        prefs.putBoolean("auto_discovery_enabled", !current)
        // Optionally restart discovery if turned on
        if (!current) {
            scanNetwork()
        }
    }

    // ============================================================
    // Scanning
    // ============================================================

    /**
     * Starts a full network scan.
     */
    fun scanNetwork() {
        if (isScanning) return

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            isScanning = true
            discoveredServersMap.clear()

            _uiState.update {
                it.copy(
                    isScanning = true,
                    status = "Scanning network...",
                    scanProgress = 0,
                    errorMessage = null,
                    discoveredServers = emptyList()
                )
            }

            if (!isWifiConnected()) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        status = "WiFi not connected",
                        errorMessage = "Please connect to WiFi network first"
                    )
                }
                isScanning = false
                return@launch
            }

            val localIp = getLocalIpAddress()
            if (localIp == null) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        status = "Could not get local IP",
                        errorMessage = "Network configuration error"
                    )
                }
                isScanning = false
                return@launch
            }

            _uiState.update { it.copy(status = "Scanning nearby servers...") }

            // Run UDP discovery and IP scanning concurrently
            coroutineScope {
                launch { startUdpDiscovery() }
                launch { scanIpRange(localIp) }
            }

            val servers = discoveredServersMap.values.toList()
            val sortedServers = sortServers(servers, _uiState.value.sortBy)

            _uiState.update {
                it.copy(
                    isScanning = false,
                    discoveredServers = sortedServers,
                    status = if (servers.isEmpty())
                        "No servers found. Make sure the Air Mouse server is running on your PC."
                    else
                        "Found ${servers.size} server(s)",
                    scanProgress = 100,
                    lastScanTime = System.currentTimeMillis()
                )
            }

            isScanning = false
        }
    }

    /**
     * Performs UDP broadcast discovery.
     */
    private suspend fun startUdpDiscovery() {
        withContext(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.soTimeout = TIMEOUT_MS.toInt()
                socket.broadcast = true

                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val broadcastAddress = getBroadcastAddress()
                val packet = DatagramPacket(sendData, sendData.size, broadcastAddress, DISCOVERY_PORT)
                socket.send(packet)

                val receiveBuffer = ByteArray(1024)
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < SCAN_DURATION_MS && isScanning) {
                    try {
                        val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                        socket.receive(receivePacket)

                        val message = String(receivePacket.data, 0, receivePacket.length)
                        if (message.startsWith(RESPONSE_MESSAGE)) {
                            val parts = message.split(":")
                            val serverIp = receivePacket.address.hostAddress ?: continue
                            val port = parts.getOrNull(1)?.toIntOrNull() ?: 8080
                            val name = parts.getOrNull(2) ?: "Air Mouse Server"
                            val version = parts.getOrNull(3) ?: "3.0.0"

                            val key = "$serverIp:$port"
                            if (!discoveredServersMap.containsKey(key)) {
                                val ping = calculatePing(serverIp)
                                val discovered = DiscoveredServer(
                                    id = UUID.randomUUID().toString(),
                                    ip = serverIp,
                                    port = port,
                                    name = name,
                                    version = version,
                                    ping = ping,
                                    signalStrength = calculateSignalStrength(ping),
                                    deviceType = detectDeviceType(name),
                                    protocol = when (port) {
                                        8081 -> Protocol.WEBSOCKET
                                        8082 -> Protocol.UDP
                                        else -> Protocol.TCP
                                    }
                                )
                                discoveredServersMap[key] = discovered
                                _uiState.update {
                                    it.copy(
                                        status = "Nearby server found: ${discovered.name} at ${discovered.ip}:${discovered.port}"
                                    )
                                }
                                notificationManager.showDiscoveryNotification(
                                    serverName = discovered.name,
                                    ip = discovered.ip,
                                    port = discovered.port,
                                    protocol = discovered.protocol.displayName
                                )
                                updateDiscoveredServers()
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // Ignore, continue
                    }
                }
            } catch (e: Exception) {
                Log.e("NetworkDiscovery", "UDP discovery error", e)
            } finally {
                socket?.close()
            }
        }
    }

    /**
     * Scans the local IP range for open ports (TCP).
     */
    private suspend fun scanIpRange(localIp: String) = withContext(Dispatchers.IO) {
        val ipPrefix = getNetworkPrefix(localIp)
        val ports = listOf(8080, 8081, 8082)
        val timeout = 1000

        var scanned = 0
        val totalHosts = 254
        val semaphore = Semaphore(50)

        val jobs = (1..254).map { i ->
            launch {
                semaphore.withPermit {
                    if (!isScanning) return@launch
                    val testIp = "$ipPrefix.$i"
                    if (testIp == localIp) {
                        synchronized(this@NetworkDiscoveryViewModel) {
                            scanned++
                            updateScanProgress(scanned, totalHosts)
                        }
                        return@launch
                    }

                    for (port in ports) {
                        if (!isScanning) break
                        try {
                            val socket = Socket()
                            socket.connect(InetSocketAddress(testIp, port), timeout)
                            socket.close()

                            val key = "$testIp:$port"
                            synchronized(discoveredServersMap) {
                                if (!discoveredServersMap.containsKey(key)) {
                                    val ping = calculatePing(testIp)
                                    val discovered = DiscoveredServer(
                                        id = UUID.randomUUID().toString(),
                                        ip = testIp,
                                        port = port,
                                        name = "Air Mouse Server",
                                        ping = ping,
                                        signalStrength = calculateSignalStrength(ping),
                                        isReachable = true,
                                        protocol = when (port) {
                                            8081 -> Protocol.WEBSOCKET
                                            8082 -> Protocol.UDP
                                            else -> Protocol.TCP
                                        }
                                    )
                                    discoveredServersMap[key] = discovered
                                    _uiState.update {
                                        it.copy(
                                            status = "Nearby server found: ${discovered.ip}:${discovered.port}"
                                        )
                                    }
                                    notificationManager.showDiscoveryNotification(
                                        serverName = discovered.name,
                                        ip = discovered.ip,
                                        port = discovered.port,
                                        protocol = discovered.protocol.displayName
                                    )
                                    updateDiscoveredServers()
                                }
                            }
                        } catch (_: IOException) {
                            // Ignore
                        }
                    }

                    synchronized(this@NetworkDiscoveryViewModel) {
                        scanned++
                        updateScanProgress(scanned, totalHosts)
                    }
                }
            }
        }
        jobs.joinAll()
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    /**
     * Calculates ping time to an IP address.
     */
    private fun calculatePing(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(1000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    /**
     * Converts ping to a signal strength indicator (0‑100).
     */
    private fun calculateSignalStrength(ping: Int): Int {
        return when {
            ping < 0 -> 0
            ping < 30 -> 100
            ping < 60 -> 80
            ping < 100 -> 60
            ping < 200 -> 40
            else -> 20
        }
    }

    /**
     * Detects device type from the server name.
     */
    private fun detectDeviceType(name: String): DeviceType {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("raspberry") || lowerName.contains("pi") -> DeviceType.RASPBERRY_PI
            lowerName.contains("mac") || lowerName.contains("apple") -> DeviceType.MAC
            lowerName.contains("laptop") -> DeviceType.LAPTOP
            lowerName.contains("server") -> DeviceType.SERVER
            else -> DeviceType.UNKNOWN
        }
    }

    /**
     * Gets the broadcast address for the current WiFi network.
     */
    @Suppress("DEPRECATION")
    private fun getBroadcastAddress(): InetAddress {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcpInfo = wifiManager.dhcpInfo
        val broadcast = (dhcpInfo.ipAddress.toInt() and dhcpInfo.netmask.toInt()) or dhcpInfo.netmask.toInt().inv()
        return InetAddress.getByAddress(intToByteArray(broadcast))
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }

    /**
     * Gets the local IPv4 address.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun getNetworkPrefix(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else ip
    }

    /**
     * Checks if WiFi is connected.
     */
    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            return info != null && info.typeName.equals("WIFI", ignoreCase = true)
        }
    }

    // ============================================================
    // User Actions
    // ============================================================

    fun addServerToSaved(server: DiscoveredServer) {
        val current = _uiState.value.savedServers.toMutableList()
        if (!current.any { it.ip == server.ip && it.port == server.port }) {
            current.add(server)
            _uiState.update { it.copy(savedServers = current) }
            saveServersToPrefs(current)
        }
    }

    fun removeSavedServer(serverId: String) {
        val current = _uiState.value.savedServers.filter { it.id != serverId }
        _uiState.update { it.copy(savedServers = current) }
        saveServersToPrefs(current)
    }

    fun toggleFavorite(serverId: String) {
        val updated = _uiState.value.savedServers.map {
            if (it.id == serverId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _uiState.update { it.copy(savedServers = updated) }
        saveServersToPrefs(updated)
    }

    suspend fun connectToServer(server: DiscoveredServer) {
        if (!prefs.isCalibrated()) {
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    errorMessage = "Calibration required before connecting. Please calibrate the device first."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                isConnecting = true,
                selectedServerId = server.id,
                connectionProgress = 0,
                errorMessage = null
            )
        }

        val targetProtocol = when (server.protocol) {
            Protocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
            Protocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
            else -> ConnectionManager.ConnectionProtocol.WEBSOCKET
        }
        val targetPort = when {
            targetProtocol == ConnectionManager.ConnectionProtocol.WEBSOCKET && (server.port <= 0 || server.port == 8080 || server.port == 8082) -> 8081
            targetProtocol == ConnectionManager.ConnectionProtocol.TCP && (server.port <= 0 || server.port == 8081 || server.port == 8082) -> 8080
            targetProtocol == ConnectionManager.ConnectionProtocol.UDP && (server.port <= 0 || server.port == 8080 || server.port == 8081) -> 8082
            server.port > 0 -> server.port
            else -> 8080
        }

        prefs.setLastIp(server.ip)
        prefs.setLastPort(targetPort)
        prefs.setLastProtocol(targetProtocol.name)
        connectionManager.setProtocol(targetProtocol)

        val success = connectionManager.connect(server.ip, targetPort)

        if (success) {
            saveConnectionHistory(server.name, server.ip, targetPort, "Connected")
            addServerToSaved(server.copy(port = targetPort))
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    connectionProgress = 100,
                    errorMessage = null
                )
            }
        } else {
            val lastError = connectionManager.lastError.value ?: "Failed to connect"
            saveConnectionHistory(server.name, server.ip, targetPort, "Failed")
            _uiState.update {
                it.copy(
                    isConnecting = false,
                    errorMessage = "Failed to connect to ${server.ip}:$targetPort – $lastError"
                )
            }
        }
    }

    suspend fun connectManual(ip: String, port: Int) {
        val server = DiscoveredServer(
            id = UUID.randomUUID().toString(),
            ip = ip,
            port = port,
            name = "Manual Entry",
            protocol = when (port) {
                8082 -> Protocol.UDP
                8080 -> Protocol.TCP
                else -> Protocol.WEBSOCKET
            }
        )
        connectToServer(server)
    }

    fun disconnect() {
        connectionManager.disconnect()
        _uiState.update { it.copy(isConnecting = false, connectionProgress = 0) }
    }

    // ============================================================
    // Sorting & Filtering
    // ============================================================

    fun sortServers(servers: List<DiscoveredServer>, sortBy: SortBy): List<DiscoveredServer> {
        return when (sortBy) {
            SortBy.IP -> servers.sortedBy { it.ip }
            SortBy.NAME -> servers.sortedBy { it.name }
            SortBy.PING -> servers.sortedBy { if (it.ping < 0) Int.MAX_VALUE else it.ping }
            SortBy.PORT -> servers.sortedBy { it.port }
            SortBy.SIGNAL -> servers.sortedByDescending { it.signalStrength }
            SortBy.LAST_SEEN -> servers.sortedByDescending { it.lastSeen }
        }
    }

    fun setSortBy(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        updateDiscoveredServers()
    }

    fun setFilterText(filter: String) {
        _uiState.update { it.copy(filterText = filter) }
    }

    fun refreshScan() {
        if (!isScanning) scanNetwork()
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConnectionHistory() {
        _uiState.update { it.copy(connectionHistory = emptyList()) }
        prefs.putString(CONNECTION_HISTORY_KEY, "")
    }

    fun setActiveTab(tab: DiscoveryTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // ============================================================
    // Internal UI Updates
    // ============================================================

    private fun updateDiscoveredServers() {
        val servers = discoveredServersMap.values.toList()
        val sorted = sortServers(servers, _uiState.value.sortBy)
        _uiState.update { it.copy(discoveredServers = sorted) }
    }

    private fun updateScanProgress(current: Int, total: Int) {
        val progress = (current * 100 / total).coerceIn(0, 99)
        _uiState.update { it.copy(scanProgress = progress) }
    }

    // ============================================================
    // Cleanup
    // ============================================================

    override fun onCleared() {
        super.onCleared()
        isScanning = false
        scanJob?.cancel()
    }
}
