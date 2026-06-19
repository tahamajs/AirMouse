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
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkDiscoveryUiState())
    val uiState: StateFlow<NetworkDiscoveryUiState> = _uiState.asStateFlow()

    private val discoveredServersMap = mutableMapOf<String, DiscoveredServer>()
    private var isScanning = false
    private var scanJob: Job? = null

    companion object {
        private const val DISCOVERY_PORT = 8082
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVERY"
        private const val RESPONSE_MESSAGE = "AIRMOUSE_SERVER"
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 8000L
        private const val SAVED_SERVERS_KEY = "saved_servers_v2"
        private const val CONNECTION_HISTORY_KEY = "connection_history"
    }

    init {
        loadSavedServers()
        loadConnectionHistory()
        observeConnectionStatus()
        startAutoDiscovery()
    }

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
                                connectionProgress = 100
                            )
                        }
                        ConnectionManager.ConnectionStatus.CONNECTING -> {
                            state.copy(
                                isConnecting = true,
                                connectionProgress = (state.connectionProgress + 5).coerceAtMost(90)
                            )
                        }
                        ConnectionManager.ConnectionStatus.ERROR -> {
                            state.copy(
                                isConnecting = false,
                                errorMessage = "Connection failed"
                            )
                        }
                        else -> state
                    }
                }
            }
        }
    }

    private fun loadSavedServers() {
        val savedJson = prefs.getString(SAVED_SERVERS_KEY, "")
        if (savedJson.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(savedJson)
                val servers = mutableListOf<DiscoveredServer>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    servers.add(
                        DiscoveredServer(
                            id = json.optString("id", UUID.randomUUID().toString()),
                            ip = json.getString("ip"),
                            port = json.getInt("port"),
                            name = json.optString("name", "Air Mouse Server"),
                            version = json.optString("version", "3.0.0"),
                            isFavorite = json.optBoolean("isFavorite", false),
                            notes = json.optString("notes", ""),
                            deviceType = try {
                                DeviceType.valueOf(json.optString("deviceType", "UNKNOWN"))
                            } catch (_: Exception) {
                                DeviceType.UNKNOWN
                            }
                        )
                    )
                }
                _uiState.update { it.copy(savedServers = servers) }
            } catch (_: Exception) {
                // Ignore parse errors
            }
        }
    }

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

    private fun loadConnectionHistory() {
        val historyJson = prefs.getString(CONNECTION_HISTORY_KEY, "")
        if (historyJson.isNotEmpty()) {
            try {
                val jsonArray = JSONArray(historyJson)
                val history = mutableListOf<ConnectionHistoryItem>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    history.add(
                        ConnectionHistoryItem(
                            id = json.optString("id", UUID.randomUUID().toString()),
                            serverId = json.optString("serverId", ""),
                            ip = json.getString("ip"),
                            port = json.getInt("port"),
                            timestamp = json.getLong("timestamp"),
                            serverName = json.optString("serverName", "Unknown"),
                            status = json.optString("status", "Disconnected")
                        )
                    )
                }
                _uiState.update { it.copy(connectionHistory = history.sortedByDescending { it.timestamp }) }
            } catch (_: Exception) {
                // Ignore parse errors
            }
        }
    }

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

            _uiState.update { it.copy(status = "Broadcasting UDP discovery...") }
            startUdpDiscovery()

            _uiState.update { it.copy(status = "Scanning IP range...") }
            scanIpRange(localIp)

            delay(SCAN_DURATION_MS)

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
                                discoveredServersMap[key] = DiscoveredServer(
                                    id = UUID.randomUUID().toString(),
                                    ip = serverIp,
                                    port = port,
                                    name = name,
                                    version = version,
                                    ping = ping,
                                    signalStrength = calculateSignalStrength(ping),
                                    deviceType = detectDeviceType(name)
                                )
                                updateDiscoveredServers()
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // Continue scanning
                    }
                }
            } catch (e: Exception) {
                Log.e("NetworkDiscovery", "UDP discovery error", e)
            } finally {
                socket?.close()
            }
        }
    }

    private suspend fun scanIpRange(localIp: String) {
        val ipPrefix = getNetworkPrefix(localIp)
        val ports = listOf(8080, 8081, 8082)
        val timeout = 2000

        var scanned = 0
        val totalHosts = 254

        for (i in 1..254) {
            if (!isScanning) break

            val testIp = "$ipPrefix.$i"
            if (testIp == localIp) continue

            withContext(Dispatchers.IO) {
                for (port in ports) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(testIp, port), timeout)
                        socket.close()

                        val key = "$testIp:$port"
                        if (!discoveredServersMap.containsKey(key)) {
                            val ping = calculatePing(testIp)
                            discoveredServersMap[key] = DiscoveredServer(
                                id = UUID.randomUUID().toString(),
                                ip = testIp,
                                port = port,
                                ping = ping,
                                signalStrength = calculateSignalStrength(ping),
                                isReachable = true
                            )
                            updateDiscoveredServers()
                        }
                    } catch (_: IOException) {
                        // Port not open
                    }
                }
            }

            scanned++
            updateScanProgress(scanned, totalHosts)
        }
    }

    private fun calculatePing(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(1000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

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

    private fun getBroadcastAddress(): InetAddress {
        @Suppress("DEPRECATION")
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
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
            // Ignore
        }
        return null
    }

    private fun getNetworkPrefix(ip: String): String {
        val parts = ip.split(".")
        return if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else ip
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            return info != null && info.type == ConnectivityManager.TYPE_WIFI
        }
    }

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
        _uiState.update { it.copy(isConnecting = true, selectedServerId = server.id, connectionProgress = 0) }

        val success = connectionManager.connect(server.ip, server.port)

        if (success) {
            saveConnectionHistory(server.name, server.ip, server.port, "Connected")
            addServerToSaved(server)
            _uiState.update { it.copy(isConnecting = false, connectionProgress = 100) }
        } else {
            saveConnectionHistory(server.name, server.ip, server.port, "Failed")
            _uiState.update {
                it.copy(isConnecting = false, errorMessage = "Failed to connect to ${server.ip}:${server.port}")
            }
        }
    }

    suspend fun connectManual(ip: String, port: Int) {
        val server = DiscoveredServer(
            id = UUID.randomUUID().toString(),
            ip = ip,
            port = port,
            name = "Manual Entry"
        )
        connectToServer(server)
    }

    fun disconnect() {
        connectionManager.disconnect()
        _uiState.update { it.copy(isConnecting = false, connectionProgress = 0) }
    }

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

    private fun updateDiscoveredServers() {
        val servers = discoveredServersMap.values.toList()
        val sorted = sortServers(servers, _uiState.value.sortBy)
        _uiState.update { it.copy(discoveredServers = sorted) }
    }

    private fun updateScanProgress(current: Int, total: Int) {
        val progress = (current * 100 / total).coerceIn(0, 99)
        _uiState.update { it.copy(scanProgress = progress) }
    }

    override fun onCleared() {
        super.onCleared()
        isScanning = false
        scanJob?.cancel()
    }
}