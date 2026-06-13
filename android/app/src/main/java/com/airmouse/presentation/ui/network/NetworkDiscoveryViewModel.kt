// app/src/main/java/com/airmouse/presentation/ui/network/NetworkDiscoveryViewModel.kt
package com.airmouse.presentation.ui.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.*
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

    companion object {
        private const val DISCOVERY_PORT = 8082
        private const val DISCOVERY_MESSAGE = "AIRMOUSE_DISCOVERY"
        private const val RESPONSE_MESSAGE = "AIRMOUSE_SERVER"
        private const val TIMEOUT_MS = 2000L
        private const val SCAN_DURATION_MS = 5000L
        private const val SAVED_SERVERS_KEY = "saved_servers"
    }

    init {
        loadSavedServers()
        observeConnectionStatus()
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                // If connected, update the selected server's reachable status
                val currentIp = connectionManager.currentIp.value
                if (currentIp.isNotEmpty() && status == ConnectionManager.ConnectionStatus.CONNECTED) {
                    val updated = _uiState.value.savedServers.map {
                        if (it.ip == currentIp) it.copy(isReachable = true) else it
                    }
                    _uiState.update { state -> state.copy(savedServers = updated) }
                    saveServersToPrefs(updated)
                }
            }
        }
    }

    private fun loadSavedServers() {
        val savedJson = prefs.getString(SAVED_SERVERS_KEY, "")
        if (savedJson.isNotEmpty()) {
            try {
                val servers = savedJson.split("|").mapNotNull { serverStr ->
                    val parts = serverStr.split(";")
                    if (parts.size >= 3) {
                        DiscoveredServer(
                            id = parts[0],
                            ip = parts[1],
                            port = parts[2].toIntOrNull() ?: 8080,
                            name = parts.getOrElse(3) { "Air Mouse Server" },
                            version = parts.getOrElse(4) { "3.0.0" },
                            isFavorite = parts.getOrElse(5) { "false" }.toBoolean(),
                            notes = parts.getOrElse(6) { "" }
                        )
                    } else null
                }
                _uiState.update { it.copy(savedServers = servers) }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun saveServersToPrefs(servers: List<DiscoveredServer>) {
        val json = servers.joinToString("|") { server ->
            "${server.id};${server.ip};${server.port};${server.name};${server.version};${server.isFavorite};${server.notes}"
        }
        prefs.putString(SAVED_SERVERS_KEY, json)
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

    fun updateServerNotes(serverId: String, notes: String) {
        val updated = _uiState.value.savedServers.map {
            if (it.id == serverId) it.copy(notes = notes) else it
        }
        _uiState.update { it.copy(savedServers = updated) }
        saveServersToPrefs(updated)
    }

    fun connectToServer(server: DiscoveredServer) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, selectedServerId = server.id) }
            connectionManager.connect(server.ip, server.port)
            // Wait a bit to see if connection succeeds
            delay(1500)
            _uiState.update { it.copy(isConnecting = false) }
            // If connection succeeded, the server will be marked reachable via the observer
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun scanNetwork() {
        if (isScanning) return

        viewModelScope.launch {
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
                        errorMessage = "Please connect to WiFi first"
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

            _uiState.update { it.copy(status = "Scanning IP range: ${getNetworkPrefix(localIp)}.*") }

            startUdpDiscovery(localIp)
            scanIpRange(localIp)

            delay(SCAN_DURATION_MS)

            val servers = discoveredServersMap.values.toList()
            val sortedServers = sortServers(servers, _uiState.value.sortBy)

            // Mark already saved servers as such (optional)
            val savedIps = _uiState.value.savedServers.map { "${it.ip}:${it.port}" }.toSet()
            val finalServers = sortedServers.map { server ->
                if (savedIps.contains("${server.ip}:${server.port}")) server else server
            }

            _uiState.update {
                it.copy(
                    isScanning = false,
                    discoveredServers = finalServers,
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

    private suspend fun startUdpDiscovery(localIp: String) {
        val socket = DatagramSocket()
        socket.soTimeout = TIMEOUT_MS.toInt()
        socket.broadcast = true

        try {
            val sendData = DISCOVERY_MESSAGE.toByteArray()
            val broadcastAddress = getBroadcastAddress()
            val packet = DatagramPacket(sendData, sendData.size, broadcastAddress, DISCOVERY_PORT)
            socket.send(packet)

            _uiState.update { it.copy(status = "Broadcasting discovery message...") }

            val receiveBuffer = ByteArray(1024)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < SCAN_DURATION_MS && isScanning) {
                try {
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)

                    val message = String(receivePacket.data, 0, receivePacket.length)
                    if (message.startsWith(RESPONSE_MESSAGE)) {
                        val parts = message.split(":")
                        val serverIp = receivePacket.address.hostAddress
                        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 8080 else 8080
                        val name = if (parts.size > 2) parts[2] else "Air Mouse Server"
                        val version = if (parts.size > 3) parts[3] else "3.0.0"

                        val key = "$serverIp:$port"
                        if (!discoveredServersMap.containsKey(key)) {
                            discoveredServersMap[key] = DiscoveredServer(
                                ip = serverIp,
                                port = port,
                                name = name,
                                version = version,
                                ping = calculatePing(serverIp),
                                isReachable = true
                            )
                            updateDiscoveredServers()
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // timeout, continue
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Discovery error: ${e.message}") }
        } finally {
            socket.close()
        }
    }

    private suspend fun scanIpRange(localIp: String) {
        val ipPrefix = getNetworkPrefix(localIp)
        val commonPorts = listOf(8080, 8081, 5000, 3000)

        for (i in 1..254) {
            if (!isScanning) break

            val testIp = "$ipPrefix.$i"
            if (testIp == localIp) continue

            for (port in commonPorts) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(testIp, port), 500)
                    socket.close()

                    val key = "$testIp:$port"
                    if (!discoveredServersMap.containsKey(key)) {
                        discoveredServersMap[key] = DiscoveredServer(
                            ip = testIp,
                            port = port,
                            ping = calculatePing(testIp),
                            isReachable = true
                        )
                        updateDiscoveredServers()
                    }
                } catch (e: IOException) {
                    // port not open
                }
            }

            updateScanProgress(i, 254)
        }
    }

    private fun calculatePing(ip: String): Int {
        return try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(1000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun getBroadcastAddress(): InetAddress {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifiManager.dhcpInfo
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        return InetAddress.getByAddress(intToByteArray(broadcast.toInt()))
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
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun updateDiscoveredServers() {
        val servers = discoveredServersMap.values.toList()
        val sorted = sortServers(servers, _uiState.value.sortBy)
        _uiState.update { it.copy(discoveredServers = sorted) }
    }

    private fun updateScanProgress(current: Int, total: Int) {
        val progress = (current * 100 / total).coerceIn(0, 99)
        _uiState.update { it.copy(scanProgress = progress) }
    }

    fun sortServers(servers: List<DiscoveredServer>, sortBy: SortBy): List<DiscoveredServer> {
        return when (sortBy) {
            SortBy.IP -> servers.sortedBy { it.ip }
            SortBy.NAME -> servers.sortedBy { it.name }
            SortBy.PING -> servers.sortedBy { if (it.ping == -1) Int.MAX_VALUE else it.ping }
            SortBy.PORT -> servers.sortedBy { it.port }
        }
    }

    fun setSortBy(sortBy: SortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        val current = _uiState.value.discoveredServers
        _uiState.update { it.copy(discoveredServers = sortServers(current, sortBy)) }
    }

    fun setFilterText(filter: String) {
        _uiState.update { it.copy(filterText = filter) }
        filterServers()
    }

    private fun filterServers() {
        val filter = _uiState.value.filterText.lowercase()
        if (filter.isEmpty()) {
            updateDiscoveredServers()
        } else {
            val filtered = discoveredServersMap.values.filter { server ->
                server.ip.contains(filter) ||
                        server.name.lowercase().contains(filter) ||
                        server.port.toString().contains(filter)
            }
            val sorted = sortServers(filtered, _uiState.value.sortBy)
            _uiState.update { it.copy(discoveredServers = sorted) }
        }
    }

    fun refreshScan() {
        if (!isScanning) scanNetwork()
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun setActiveTab(tab: DiscoveryTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    override fun onCleared() {
        super.onCleared()
        isScanning = false
    }
}