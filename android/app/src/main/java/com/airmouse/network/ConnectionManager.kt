// app/src/main/java/com/airmouse/network/ConnectionManager.kt
@file:Suppress("unused")

package com.airmouse.network

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.airmouse.utils.LogManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.ByteString
import org.json.JSONObject
import java.io.BufferedReader
import java.io.EOFException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================
// ConnectionQuality – defined here (top-level)
// ============================================================

data class ConnectionQuality(
    val rssi: Int = 0,
    val ping: Int = 0,
    val jitter: Int = 0,
    val packetLoss: Float = 0f,
    val dataRate: Int = 0,
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
) {
    enum class SignalStrength {
        EXCELLENT, GOOD, FAIR, POOR, VERY_POOR, UNKNOWN
    }

    fun level(): Int = when (signalStrength) {
        SignalStrength.EXCELLENT -> 100
        SignalStrength.GOOD -> 75
        SignalStrength.FAIR -> 50
        SignalStrength.POOR -> 25
        SignalStrength.VERY_POOR -> 10
        SignalStrength.UNKNOWN -> 0
    }

    fun isHealthy(): Boolean {
        if (signalStrength == SignalStrength.UNKNOWN || ping < 0) return false
        val isPacketLossOk = packetLoss == -1f || packetLoss < 0.1f
        return ping < 200 && isPacketLossOk
    }
}

// ============================================================
// ConnectionManager
// ============================================================

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "AirMouse::ConnectionWakeLock").apply {
                    acquire()
                }
                LogManager.info("WakeLock acquired to keep connection alive in background", TAG)
            }
        } catch (e: Exception) {
            LogManager.error("Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
                wakeLock = null
                LogManager.info("WakeLock released", TAG)
            }
        } catch (e: Exception) {
            LogManager.error("Failed to release WakeLock", e)
        }
    }

    companion object {
        private const val TAG = "ConnectionManager"
        private const val DEFAULT_PORT = 8080
        private const val WEBSOCKET_PORT = 8081
        private const val HEARTBEAT_INTERVAL_MS = 30000L
        private const val RECONNECT_DELAY_MS = 3000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val PING_TIMEOUT_MS = 5000L
        private const val MAX_HEARTBEAT_FAILURES = 3
        private const val RETRANSMIT_INTERVAL_MS = 1000L
        private const val MAX_RETRANSMIT_ATTEMPTS = 5
        private const val ACK_TIMEOUT_MS = 30000L
    }

    // ============================================================
    // Message Types (matching Go server protocol)
    // ============================================================
    object MessageTypes {
        const val TYPE_MOVE = "move"
        const val TYPE_CLICK = "click"
        const val TYPE_DOUBLE_CLICK = "doubleclick"
        const val TYPE_RIGHT_CLICK = "rightclick"
        const val TYPE_SCROLL = "scroll"
        const val TYPE_HELLO = "hello"
        const val TYPE_GESTURE = "gesture"
        const val TYPE_PROXIMITY = "proximity"
        const val TYPE_CONTROL = "control"
        const val TYPE_PING = "ping"
        const val TYPE_PONG = "pong"
        const val TYPE_ACK = "ack"
        const val TYPE_ERROR = "error"
        const val TYPE_WELCOME = "welcome"

        // Control commands
        const val COMMAND_PAUSE_MOVEMENT = "pause_movement"
        const val COMMAND_RESUME_MOVEMENT = "resume_movement"
        const val COMMAND_LOCK_SCREEN = "lock_screen"
        const val COMMAND_UNLOCK_SCREEN = "unlock_screen"
        const val COMMAND_CALIBRATE = "calibrate"
        const val COMMAND_RESET = "reset"
        const val COMMAND_PLAY_PAUSE = "play_pause"
        const val COMMAND_NEXT_TRACK = "next_track"
        const val COMMAND_PREV_TRACK = "prev_track"
        const val COMMAND_STOP = "stop"
        const val COMMAND_VOLUME_UP = "volume_up"
        const val COMMAND_VOLUME_DOWN = "volume_down"
        const val COMMAND_MUTE = "mute"
        const val COMMAND_SHOW_DESKTOP = "show_desktop"
        const val COMMAND_BROWSER_BACK = "browser_back"
        const val COMMAND_BROWSER_FORWARD = "browser_forward"
        const val COMMAND_BROWSER_REFRESH = "browser_refresh"
        const val COMMAND_BROWSER_HOME = "browser_home"

        // Buttons
        const val BUTTON_LEFT = "left"
        const val BUTTON_RIGHT = "right"
        const val BUTTON_MIDDLE = "middle"
    }

    // ============================================================
    // Enums
    // ============================================================

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
    }

    enum class ConnectionProtocol { UDP, TCP, WEBSOCKET }

    // ============================================================
    // Data Classes
    // ============================================================

    private data class ReliableMessage(
        val id: String,
        val rawMessage: String,
        val timestamp: Long,
        var attempts: Int = 0
    )

    // ============================================================
    // State Flows (UI observable)
    // ============================================================

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentIp = MutableStateFlow("")
    val currentIp: StateFlow<String> = _currentIp.asStateFlow()

    private val _currentPort = MutableStateFlow(DEFAULT_PORT)
    val currentPort: StateFlow<Int> = _currentPort.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality())
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _serverName = MutableStateFlow("")
    val serverName: StateFlow<String> = _serverName.asStateFlow()

    private val _serverVersion = MutableStateFlow("")
    val serverVersion: StateFlow<String> = _serverVersion.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // ============================================================
    // Connection Components
    // ============================================================

    private var webSocket: okhttp3.WebSocket? = null
    private var udpSocket: DatagramSocket? = null
    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var tcpReader: BufferedReader? = null

    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var retransmitJob: Job? = null
    private var welcomeDeferred: CompletableDeferred<Boolean>? = null
    private var manualDisconnectRequested = false

    private var messageIdCounter = 0
    private val pendingAcks = ConcurrentHashMap<String, ReliableMessage>()

    private var reconnectAttempts = 0
    private var currentProtocol = ConnectionProtocol.WEBSOCKET
    private var lastPingTime = 0L
    private var consecutiveFailures = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sound alerts (optional)
    private var soundPool: SoundPool? = null
    private var isSoundLoaded = false

    // ============================================================
    // Callbacks
    // ============================================================

    var onMessage: ((String) -> Unit)? = null
    var onBinaryMessage: ((ByteArray) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onQualityChanged: ((ConnectionQuality) -> Unit)? = null
    var onStatusChanged: ((ConnectionStatus) -> Unit)? = null
    private val extraMessageListeners = mutableListOf<(String) -> Unit>()
    private val extraBinaryListeners = mutableListOf<(ByteArray) -> Unit>()

    // ============================================================
    // Initialisation
    // ============================================================

    init {
        initSound()
        loadLastConnection()
    }

    private fun initSound() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()
            soundPool?.setOnLoadCompleteListener { _, _, _ ->
                isSoundLoaded = true
            }
        } catch (e: Exception) {
            LogManager.warn("Failed to initialize sound pool", e)
        }
    }

    private fun loadLastConnection() {
        val ip = prefs.getString("last_ip", "")
        val savedPort = prefs.getInt("last_port", DEFAULT_PORT)
        val port = if (savedPort == 0) DEFAULT_PORT else savedPort
        if (ip.isNotEmpty()) {
            _currentIp.value = ip
            _currentPort.value = port
        }
        val protocolName = prefs.getString(
            "last_protocol",
            prefs.getString("connection_protocol", "WEBSOCKET")
        ).uppercase()
        currentProtocol = when (protocolName) {
            "UDP" -> ConnectionProtocol.UDP
            "TCP" -> ConnectionProtocol.TCP
            else -> ConnectionProtocol.WEBSOCKET
        }
    }

    fun setProtocol(protocol: ConnectionProtocol) {
        currentProtocol = protocol
        prefs.putString("last_protocol", protocol.name)
        prefs.putString("connection_protocol", protocol.name)
    }

    // ============================================================
    // Connection Management
    // ============================================================

    suspend fun connect(ip: String = _currentIp.value, port: Int = _currentPort.value): Boolean {
        LogManager.debug("connect() requested ip=$ip port=$port protocol=${currentProtocol.name} status=${_connectionStatus.value.name}")
        if (ip.isEmpty()) {
            _lastError.value = "No IP address configured"
            onError?.invoke("No IP address configured")
            return false
        }
        if (port <= 0) {
            _lastError.value = "Invalid port"
            onError?.invoke("Invalid port")
            return false
        }

        _currentIp.value = ip
        _currentPort.value = port
        prefs.putString("last_ip", ip)
        prefs.putInt("last_port", port)

        return withContext(Dispatchers.IO) {
            try {
                LogManager.debug("connect(): cleaning up previous connection state")
                cleanupConnectionState(emitDisconnected = false)
                manualDisconnectRequested = false
                _connectionStatus.value = ConnectionStatus.CONNECTING
                _lastError.value = null
                onStatusChanged?.invoke(ConnectionStatus.CONNECTING)
                onError?.invoke("Waiting for server approval...")
                val serverWelcome = CompletableDeferred<Boolean>()
                welcomeDeferred = serverWelcome
                LogManager.debug("connect(): waiting for welcome/approval")

                val success = when (currentProtocol) {
                    ConnectionProtocol.UDP -> connectUdp(ip, port)
                    ConnectionProtocol.WEBSOCKET -> connectWebSocket(ip, port)
                    ConnectionProtocol.TCP -> connectTcp(ip, port)
                }
                LogManager.debug("connect(): transport setup completed success=$success protocol=${currentProtocol.name}")

                val accepted = if (success) {
                    val waited = withTimeoutOrNull(ACK_TIMEOUT_MS) { serverWelcome.await() } == true
                    LogManager.debug("connect(): welcome wait complete accepted=$waited")
                    waited
                } else {
                    false
                }

                if (accepted) {
                    welcomeDeferred = null
                    startHeartbeat()
                    startRetransmissionLoop()
                    acquireWakeLock()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _lastError.value = null
                    reconnectAttempts = 0
                    consecutiveFailures = 0
                    onStatusChanged?.invoke(ConnectionStatus.CONNECTED)
                    onConnected?.invoke()
                    onError?.invoke("Connection approved")
                    LogManager.info("Connected to $ip:$port via ${currentProtocol.name}")
                    true
                } else {
                    LogManager.warn("connect(): approval failed or timed out; disconnecting")
                    welcomeDeferred?.complete(false)
                    welcomeDeferred = null
                    disconnect()
                    _connectionStatus.value = ConnectionStatus.ERROR
                    _lastError.value = "Connection not accepted by server"
                    onStatusChanged?.invoke(ConnectionStatus.ERROR)
                    onError?.invoke("Approval needed")
                    onError?.invoke("Connection not accepted by server")
                    false
                }
            } catch (e: ConnectException) {
                LogManager.warn("Socket refused by remote host", e)
                welcomeDeferred?.complete(false)
                welcomeDeferred = null
                disconnect()
                _connectionStatus.value = ConnectionStatus.ERROR
                val message = "Server unavailable at $ip:$port"
                _lastError.value = message
                onStatusChanged?.invoke(ConnectionStatus.ERROR)
                onError?.invoke(message)
                false
            } catch (e: UnknownHostException) {
                LogManager.warn("Unknown host", e)
                welcomeDeferred?.complete(false)
                welcomeDeferred = null
                disconnect()
                _connectionStatus.value = ConnectionStatus.ERROR
                val message = "Unknown server address"
                _lastError.value = message
                onStatusChanged?.invoke(ConnectionStatus.ERROR)
                onError?.invoke(message)
                false
            } catch (e: SocketTimeoutException) {
                LogManager.warn("Connection timed out", e)
                welcomeDeferred?.complete(false)
                welcomeDeferred = null
                disconnect()
                _connectionStatus.value = ConnectionStatus.ERROR
                val message = "Connection timed out while waiting for approval"
                _lastError.value = message
                onStatusChanged?.invoke(ConnectionStatus.ERROR)
                onError?.invoke(message)
                false
            } catch (e: CancellationException) {
                welcomeDeferred?.complete(false)
                welcomeDeferred = null
                LogManager.info("Connection attempt cancelled")
                false
            } catch (e: Exception) {
                LogManager.error("Connection failed", e)
                welcomeDeferred?.complete(false)
                welcomeDeferred = null
                _connectionStatus.value = ConnectionStatus.ERROR
                _lastError.value = e.message ?: "Connection failed"
                onStatusChanged?.invoke(ConnectionStatus.ERROR)
                onError?.invoke(e.message ?: "Connection failed")
                false
            }
        }
    }

    private suspend fun connectUdp(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                LogManager.debug("connectUdp(): opening datagram socket to $ip:$port")
                val socket = DatagramSocket()
                socket.soTimeout = 1000
                socket.connect(InetSocketAddress(ip, port))
                udpSocket = socket
                startUdpReading()
                val helloSent = sendHello()
                LogManager.debug("connectUdp(): helloSent=$helloSent")
                LogManager.info("Socket opened via UDP")
                LogManager.info("Hello sent via UDP")
                true
            } catch (e: ConnectException) {
                LogManager.warn("UDP connection refused", e)
                false
            } catch (e: SocketTimeoutException) {
                LogManager.warn("UDP connection timed out", e)
                false
            } catch (e: UnknownHostException) {
                LogManager.warn("UDP unknown host", e)
                false
            } catch (e: Exception) {
                LogManager.error("UDP connection error", e)
                false
            }
        }
    }

    private suspend fun connectWebSocket(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val authToken = prefs.getString("auth_token", "")
                val baseUrl = "ws://$ip:$port/ws"
                val url = if (authToken.isNotBlank()) {
                    "$baseUrl?token=$authToken"
                } else {
                    baseUrl
                }
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .build()

                var connectSuccess = false
                val latch = java.util.concurrent.CountDownLatch(1)

                webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        LogManager.debug("connectWebSocket(): onOpen responseCode=${response.code}")
                        LogManager.info("Socket opened via WebSocket")
                        connectSuccess = true
                        val helloSent = sendHello()
                        LogManager.debug("connectWebSocket(): helloSent=$helloSent")
                        LogManager.info("Hello sent via WebSocket")
                        latch.countDown()
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                        LogManager.debug("connectWebSocket(): received text len=${text.length}")
                        onMessage?.invoke(text)
                        handleServerMessage(text)
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                        onBinaryMessage?.invoke(bytes.toByteArray())
                        extraBinaryListeners.forEach { listener ->
                            try {
                                listener(bytes.toByteArray())
                            } catch (e: Exception) {
                                LogManager.warn("Binary listener failed", e)
                            }
                        }
                    }

                    override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                        handleDisconnect()
                    }

                    override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                        LogManager.error("WebSocket failure", t)
                        if (response != null) {
                            LogManager.warn("WebSocket failure response code=${response.code}")
                        }
                        connectSuccess = false
                        latch.countDown()
                    }
                })

                if (!latch.await(10, TimeUnit.SECONDS)) {
                    LogManager.warn("WebSocket connect timed out")
                    webSocket?.cancel()
                    webSocket = null
                    return@withContext false
                }
                connectSuccess
            } catch (e: ConnectException) {
                LogManager.warn("WebSocket connection refused", e)
                false
            } catch (e: SocketTimeoutException) {
                LogManager.warn("WebSocket connection timed out", e)
                false
            } catch (e: UnknownHostException) {
                LogManager.warn("WebSocket unknown host", e)
                false
            } catch (e: Exception) {
                LogManager.error("WebSocket connection error", e)
                false
            }
        }
    }

    private suspend fun connectTcp(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                LogManager.debug("connectTcp(): opening socket to $ip:$port")
                val socket = Socket().apply {
                    connect(InetSocketAddress(ip, port), 5000)
                    tcpNoDelay = true
                    keepAlive = true
                    soTimeout = 60000
                    receiveBufferSize = 8192
                    sendBufferSize = 8192
                }
                tcpSocket = socket
                tcpWriter = PrintWriter(socket.getOutputStream(), true)
                tcpReader = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Send hello and verify it was sent
                val helloSent = sendHello()
                LogManager.debug("connectTcp(): helloSent=$helloSent")
                if (!helloSent) {
                    LogManager.error("Failed to send hello over TCP")
                    socket.close()
                    return@withContext false
                }

                LogManager.info("Socket opened via TCP")
                LogManager.info("Hello sent via TCP")
                startTcpReading()
                true
            } catch (e: ConnectException) {
                LogManager.warn("TCP connection refused", e)
                false
            } catch (e: SocketTimeoutException) {
                LogManager.warn("TCP connection timed out", e)
                false
            } catch (e: UnknownHostException) {
                LogManager.warn("TCP unknown host", e)
                false
            } catch (e: Exception) {
                LogManager.error("TCP connection error", e)
                false
            }
        }
    }

    private fun startTcpReading() {
        readJob?.cancel()
        readJob = scope.launch {
            while (tcpSocket?.isConnected == true) {
                try {
                    val line = tcpReader?.readLine() ?: break
                    LogManager.debug("startTcpReading(): received line len=${line.length}")
                    onMessage?.invoke(line)
                    handleServerMessage(line)
                } catch (e: SocketException) {
                    if (tcpSocket?.isClosed == true || manualDisconnectRequested) {
                        LogManager.info("TCP socket closed")
                    } else {
                        LogManager.warn("TCP socket error", e)
                    }
                    break
                } catch (e: EOFException) {
                    LogManager.info("TCP stream ended")
                    break
                } catch (e: Exception) {
                    if (tcpSocket?.isClosed == true || manualDisconnectRequested) {
                        LogManager.debug("TCP read stopped after disconnect: ${e.message}")
                    } else {
                        LogManager.error("TCP read error", e)
                    }
                    break
                }
            }
            handleDisconnect()
        }
    }

    private fun startUdpReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val socket = udpSocket ?: return@launch
            val buffer = ByteArray(65507)
            while (udpSocket === socket && !socket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    LogManager.debug("startUdpReading(): received packet len=${packet.length}")
                    onMessage?.invoke(message)
                    handleServerMessage(message)
                } catch (e: PortUnreachableException) {
                    LogManager.info("UDP endpoint unreachable; closing UDP session")
                    break
                } catch (e: SocketTimeoutException) {
                    continue
                } catch (e: Exception) {
                    if (socket.isClosed || manualDisconnectRequested) {
                        LogManager.debug("UDP read stopped after disconnect: ${e.message}")
                    } else {
                        LogManager.warn("UDP read error", e)
                    }
                    break
                }
            }
            handleDisconnect()
        }
    }

    // ============================================================
    // Message Handling
    // ============================================================

    private fun handleServerMessage(message: String) {
        try {
            extraMessageListeners.forEach { listener ->
                try {
                    listener(message)
                } catch (e: Exception) {
                    LogManager.warn("Message listener failed", e)
                }
            }
            val json = JSONObject(message)
            when (json.optString("type")) {
                MessageTypes.TYPE_WELCOME -> {
                    val payload = json.optJSONObject("payload")
                    _serverName.value = payload?.optString("server") ?: "Air Mouse"
                    _serverVersion.value = payload?.optString("version") ?: "3.0"
                    welcomeDeferred?.complete(true)
                    LogManager.debug("handleServerMessage(): welcome payload=${payload?.toString()}")
                    LogManager.info("Server: ${_serverName.value} v${_serverVersion.value}")
                    LogManager.info("Welcome received and approval granted")
                }
                MessageTypes.TYPE_PONG -> {
                    val now = System.currentTimeMillis()
                    val ping = now - lastPingTime
                    updateConnectionQuality(ping)
                }
                MessageTypes.TYPE_PING -> {
                    LogManager.debug("🔄 Server ping received; sending pong")
                    sendPong()
                }
                MessageTypes.TYPE_ACK -> {
                    val id = json.optString("id")
                    if (id.isNotEmpty()) {
                        pendingAcks.remove(id)
                        LogManager.debug("Acknowledged reliable packet removed: $id")
                    }
                }
                "file" -> {
                    // File transfer service handles these messages via an extra listener.
                }
                MessageTypes.TYPE_ERROR -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    _lastError.value = error
                    onError?.invoke(error)
                }
            }
        } catch (e: Exception) {
            LogManager.debug("Non-JSON message: $message")
        }
    }

    private fun updateConnectionQuality(pingMs: Long) {
        val quality = _connectionQuality.value
        val newQuality = quality.copy(
            ping = pingMs.toInt(),
            signalStrength = when {
                pingMs < 30 -> ConnectionQuality.SignalStrength.EXCELLENT
                pingMs < 60 -> ConnectionQuality.SignalStrength.GOOD
                pingMs < 100 -> ConnectionQuality.SignalStrength.FAIR
                pingMs < 200 -> ConnectionQuality.SignalStrength.POOR
                else -> ConnectionQuality.SignalStrength.VERY_POOR
            }
        )
        _connectionQuality.value = newQuality
        onQualityChanged?.invoke(newQuality)
    }

    private fun handleDisconnect() {
        LogManager.debug("handleDisconnect(): status=${_connectionStatus.value.name} manualDisconnectRequested=$manualDisconnectRequested")
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _lastError.value = "Disconnected from server"
            onStatusChanged?.invoke(ConnectionStatus.DISCONNECTED)
            onDisconnected?.invoke()
            onError?.invoke("Disconnected from server")
            if (!manualDisconnectRequested) {
                scheduleReconnect()
            }
        }
    }

    // ============================================================
    // Heartbeat & Retransmission
    // ============================================================

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (!sendPing()) {
                    consecutiveFailures++
                    LogManager.warn("Heartbeat ping failed ($consecutiveFailures/$MAX_HEARTBEAT_FAILURES)")
                    if (consecutiveFailures >= MAX_HEARTBEAT_FAILURES) {
                        LogManager.warn("Too many heartbeat failures, reconnecting...")
                        handleDisconnect()
                    }
                } else {
                    consecutiveFailures = 0
                }
            }
        }
    }

    private fun startRetransmissionLoop() {
        retransmitJob?.cancel()
        retransmitJob = scope.launch {
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(RETRANSMIT_INTERVAL_MS)
                val now = System.currentTimeMillis()
                pendingAcks.values.forEach { msg ->
                    if (now - msg.timestamp >= RETRANSMIT_INTERVAL_MS) {
                        if (msg.attempts < MAX_RETRANSMIT_ATTEMPTS) {
                            msg.attempts++
                            LogManager.warn("Retransmitting key event ${msg.id}, retry attempt: ${msg.attempts}")
                            sendRawString(msg.rawMessage)
                        } else {
                            pendingAcks.remove(msg.id)
                            LogManager.error("Reliable packet ${msg.id} expired. Max attempts reached.")
                        }
                    }
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (manualDisconnectRequested) {
            LogManager.info("Reconnect skipped after manual disconnect")
            return
        }
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _connectionStatus.value = ConnectionStatus.ERROR
            _lastError.value = "Max reconnection attempts reached"
            onStatusChanged?.invoke(ConnectionStatus.ERROR)
            onError?.invoke("Max reconnection attempts reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = RECONNECT_DELAY_MS * (reconnectAttempts + 1)
            val attempt = reconnectAttempts + 1
            LogManager.debug("scheduleReconnect(): delay=${delay}ms attempt=$attempt")
            onError?.invoke("Reconnecting in ${delay/1000}s (attempt $attempt/$MAX_RECONNECT_ATTEMPTS)")
            delay(delay)
            reconnectAttempts++
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            onStatusChanged?.invoke(ConnectionStatus.RECONNECTING)
            connect()
        }
    }

    // ============================================================
    // Sending Methods (Public & Private)
    // ============================================================

    private fun sendRawString(message: String, allowWhileConnecting: Boolean = false): Boolean {
        LogManager.debug("📤 sendRawString: message=$message")
        val state = _connectionStatus.value
        if (state != ConnectionStatus.CONNECTED && !(allowWhileConnecting && state == ConnectionStatus.CONNECTING)) {
            LogManager.warn("📤 sendRawString: blocked state=${state.name}, allowWhileConnecting=$allowWhileConnecting")
            return false
        }

        val result = when (currentProtocol) {
            ConnectionProtocol.UDP -> sendUdpBytes(message.toByteArray())
            ConnectionProtocol.WEBSOCKET -> {
                val sent = webSocket?.send(message) ?: false
                LogManager.debug("📤 WebSocket send result: $sent")
                sent
            }
            ConnectionProtocol.TCP -> {
                val writer = tcpWriter
                if (writer == null) {
                    LogManager.error("📤 TCP writer is null")
                    return false
                }
                try {
                    writer.println(message)
                    // Do not call checkError() as it forces a flush and blocks the IO thread
                    val success = true
                    LogManager.debug("📤 TCP write result: success=$success")
                    success
                } catch (e: Exception) {
                    LogManager.error("📤 TCP write exception", e)
                    false
                }
            }
        }
        LogManager.debug("📤 sendRawString final result: $result")
        return result
    }

    fun send(message: String): Boolean = sendRawString(message)

    private fun sendReliablePacket(type: String, block: JSONObject.() -> Unit): Boolean {
        val id = "msg_${synchronized(this) { messageIdCounter++ }}"
        val rawPayload = JSONObject().apply {
            put("type", type)
            put("id", id)
            block()
        }.toString()
        val success = sendRawString(rawPayload)
        if (success) {
            pendingAcks[id] = ReliableMessage(id, rawPayload, System.currentTimeMillis())
        }
        return success
    }

    fun sendBinary(data: ByteArray): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.UDP -> sendUdpBytes(data)
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(ByteString.of(*data)) ?: false
            else -> false
        }
    }

    private fun sendUdpBytes(data: ByteArray): Boolean {
        val socket = udpSocket ?: return false
        return try {
            socket.send(DatagramPacket(data, data.size))
            true
        } catch (e: Exception) {
            LogManager.error("UDP send failed", e)
            false
        }
    }

    fun addMessageListener(listener: (String) -> Unit) {
        extraMessageListeners.add(listener)
    }

    fun removeMessageListener(listener: (String) -> Unit) {
        extraMessageListeners.remove(listener)
    }

    fun addBinaryMessageListener(listener: (ByteArray) -> Unit) {
        extraBinaryListeners.add(listener)
    }

    fun removeBinaryMessageListener(listener: (ByteArray) -> Unit) {
        extraBinaryListeners.remove(listener)
    }

    // ------------------------------------------------------------
    // Mouse Commands
    // ------------------------------------------------------------

    fun sendMove(dx: Float, dy: Float): Boolean {
        return sendRawString(AirMouseProtocolMessages.move(dx, dy))
    }

    fun sendClick(button: String = MessageTypes.BUTTON_LEFT): Boolean {
        return sendReliablePacket(MessageTypes.TYPE_CLICK) {
            put("button", button)
            put("Click", button)
        }
    }

    fun sendDoubleClick(): Boolean {
        return sendReliablePacket(MessageTypes.TYPE_DOUBLE_CLICK) {
            put("button", "double")
            put("Click", "double")
        }
    }

    fun sendRightClick(): Boolean {
        return sendReliablePacket(MessageTypes.TYPE_RIGHT_CLICK) {
            put("button", "right")
            put("Click", "right")
        }
    }

    fun sendScroll(delta: Int): Boolean {
        return sendReliablePacket(MessageTypes.TYPE_SCROLL) {
            put("delta", delta)
            put("Scroll", delta)
        }
    }

    // ------------------------------------------------------------
    // Gesture Commands
    // ------------------------------------------------------------

    fun sendGesture(gesture: String, confidence: Float): Boolean {
        return sendRawString(AirMouseProtocolMessages.gesture(gesture, confidence))
    }

    // ------------------------------------------------------------
    // Proximity Commands
    // ------------------------------------------------------------

    fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        val deviceId = getDeviceId()
        return sendRawString(AirMouseProtocolMessages.proximity(deviceId, isNear, distance))
    }

    // ------------------------------------------------------------
    // Control Commands
    // ------------------------------------------------------------

    fun sendControl(command: String): Boolean {
        return sendRawString(AirMouseProtocolMessages.control(command))
    }

    fun sendPauseMovement(): Boolean = sendControl(MessageTypes.COMMAND_PAUSE_MOVEMENT)
    fun sendResumeMovement(): Boolean = sendControl(MessageTypes.COMMAND_RESUME_MOVEMENT)
    fun sendLockScreen(): Boolean = sendControl(MessageTypes.COMMAND_LOCK_SCREEN)
    fun sendUnlockScreen(): Boolean = sendControl(MessageTypes.COMMAND_UNLOCK_SCREEN)
    fun sendCalibrate(): Boolean = sendControl(MessageTypes.COMMAND_CALIBRATE)
    fun sendReset(): Boolean = sendControl(MessageTypes.COMMAND_RESET)

    // ------------------------------------------------------------
    // System Commands
    // ------------------------------------------------------------

    fun sendKeyPress(keyCode: Int): Boolean {
        val keyMap = mapOf(
            android.view.KeyEvent.KEYCODE_HOME to MessageTypes.COMMAND_SHOW_DESKTOP,
            android.view.KeyEvent.KEYCODE_BACK to MessageTypes.COMMAND_BROWSER_BACK,
            android.view.KeyEvent.KEYCODE_VOLUME_UP to MessageTypes.COMMAND_VOLUME_UP,
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN to MessageTypes.COMMAND_VOLUME_DOWN,
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to MessageTypes.COMMAND_PLAY_PAUSE,
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT to MessageTypes.COMMAND_NEXT_TRACK,
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS to MessageTypes.COMMAND_PREV_TRACK
        )
        val command = keyMap[keyCode] ?: return false
        return sendControl(command)
    }

    fun sendWindowCommand(action: String): Boolean {
        val validActions = listOf("maximize", "minimize", "close", "fullscreen")
        return if (action in validActions) sendControl("window_$action") else false
    }

    // ------------------------------------------------------------
    // Media Commands
    // ------------------------------------------------------------

    fun sendPlayPause(): Boolean = sendControl(MessageTypes.COMMAND_PLAY_PAUSE)
    fun sendNextTrack(): Boolean = sendControl(MessageTypes.COMMAND_NEXT_TRACK)
    fun sendPrevTrack(): Boolean = sendControl(MessageTypes.COMMAND_PREV_TRACK)
    fun sendStop(): Boolean = sendControl(MessageTypes.COMMAND_STOP)
    fun sendVolumeUp(): Boolean = sendControl(MessageTypes.COMMAND_VOLUME_UP)
    fun sendVolumeDown(): Boolean = sendControl(MessageTypes.COMMAND_VOLUME_DOWN)
    fun sendMute(): Boolean = sendControl(MessageTypes.COMMAND_MUTE)

    // ------------------------------------------------------------
    // Browser Commands
    // ------------------------------------------------------------

    fun sendBrowserBack(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_BACK)
    fun sendBrowserForward(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_FORWARD)
    fun sendBrowserRefresh(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_REFRESH)
    fun sendBrowserHome(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_HOME)

    // ------------------------------------------------------------
    // Heartbeat Commands
    // ------------------------------------------------------------

    fun sendPing(): Boolean {
        lastPingTime = System.currentTimeMillis()
        return sendRawString(AirMouseProtocolMessages.ping())
    }

    fun sendPong(): Boolean {
        val sent = sendRawString(AirMouseProtocolMessages.pong(), allowWhileConnecting = true)
        LogManager.debug("📤 Pong sent: $sent")
        return sent
    }

    // ------------------------------------------------------------
    // Identification
    // ------------------------------------------------------------

    fun sendHello(name: String = Build.MODEL, version: String = "3.0"): Boolean {
        val authToken = prefs.getString("auth_token", "")
        val json = AirMouseProtocolMessages.hello(
            name = name,
            version = version,
            device = Build.MANUFACTURER + " " + Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            deviceName = Build.DEVICE,
            sdkInt = Build.VERSION.SDK_INT.toString(),
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "",
            protocol = currentProtocol.name,
            transport = when (currentProtocol) {
                ConnectionProtocol.UDP -> "udp"
                ConnectionProtocol.WEBSOCKET -> "websocket"
                ConnectionProtocol.TCP -> "tcp"
            },
            authToken = authToken
        )
        LogManager.info("📤 Sending hello: $json")
        val sent = sendRawString(json, allowWhileConnecting = true)
        LogManager.info("📤 Hello sent: $sent")
        return sent
    }

    // ============================================================
    // Connection Management (Public)
    // ============================================================

    fun disconnect() {
        LogManager.debug("disconnect() requested status=${_connectionStatus.value.name} protocol=${currentProtocol.name}")
        manualDisconnectRequested = true
        cleanupConnectionState(emitDisconnected = true)
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        onStatusChanged?.invoke(ConnectionStatus.DISCONNECTED)
    }

    fun isConnected(): Boolean = _connectionStatus.value == ConnectionStatus.CONNECTED

    fun reconnect() {
        reconnectAttempts = 0
        scope.launch { connect() }
    }

    fun getServerInfo(): Pair<String, String> = Pair(_serverName.value, _serverVersion.value)

    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    fun cleanup() {
        LogManager.debug("cleanup() requested")
        disconnect()
        scope.cancel()
        soundPool?.release()
        soundPool = null
    }

    // ============================================================
    // Batch & Debug
    // ============================================================

    fun sendBatch(commands: List<String>): Boolean {
        var allSent = true
        commands.forEach { command ->
            if (!sendControl(command)) {
                allSent = false
            }
        }
        return allSent
    }

    fun getConnectionStats(): Map<String, Any> {
        return mapOf(
            "status" to _connectionStatus.value.name,
            "ip" to _currentIp.value,
            "port" to _currentPort.value,
            "protocol" to currentProtocol.name,
            "ping" to _connectionQuality.value.ping,
            "signal_strength" to _connectionQuality.value.signalStrength.name,
            "reconnect_attempts" to reconnectAttempts,
            "server_name" to _serverName.value,
            "server_version" to _serverVersion.value,
            "pending_acks_count" to pendingAcks.size
        )
    }

    private fun cleanupConnectionState(emitDisconnected: Boolean) {
        releaseWakeLock()
        LogManager.debug("cleanupConnectionState(): emitDisconnected=$emitDisconnected status=${_connectionStatus.value.name}")
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        retransmitJob?.cancel()
        welcomeDeferred?.complete(false)
        welcomeDeferred = null
        pendingAcks.clear()

        try {
            webSocket?.close(1000, if (emitDisconnected) "Manual disconnect" else "Reconnect cleanup")
            webSocket = null

            udpSocket?.close()
            udpSocket = null

            tcpWriter?.close()
            tcpReader?.close()
            tcpSocket?.close()
        } catch (e: Exception) {
            LogManager.error("Error during disconnect cleanup", e)
        }

        tcpWriter = null
        tcpReader = null
        tcpSocket = null
    }
}
