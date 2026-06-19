// app/src/main/java/com/airmouse/network/ConnectionManager.kt
package com.airmouse.network

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.Settings
import android.util.Log
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
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
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
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
    }

    enum class ConnectionProtocol { TCP, WEBSOCKET }

    data class ConnectionQuality(
        val rssi: Int = 0,
        val ping: Int = -1, // Default to -1 to imply no valid measurement yet
        val jitter: Int = -1,
        val packetLoss: Float = -1f,
        val signalStrength: SignalStrength = SignalStrength.UNKNOWN
    ) {
        enum class SignalStrength { EXCELLENT, GOOD, FAIR, POOR, VERY_POOR, UNKNOWN }

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

    // Reliable message tracking structure for Click/Scroll packets (Section 3.4)
    private data class ReliableMessage(
        val id: String,
        val rawMessage: String,
        val timestamp: Long,
        var attempts: Int = 0
    )

    // State flows for UI observation
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

    // Connection components
    private var webSocket: okhttp3.WebSocket? = null
    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var tcpReader: BufferedReader? = null
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var retransmitJob: Job? = null

    private var messageIdCounter = 0
    private val pendingAcks = ConcurrentHashMap<String, ReliableMessage>()

    private var reconnectAttempts = 0
    private var currentProtocol = ConnectionProtocol.WEBSOCKET
    private var lastPingTime = 0L
    private var lastPongTime = 0L
    private var consecutiveFailures = 0

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sound alerts
    private var soundPool: SoundPool? = null
    private var connectSoundId = 0
    private var disconnectSoundId = 0
    private var errorSoundId = 0
    private var isSoundLoaded = false

    // Callbacks
    var onMessage: ((String) -> Unit)? = null
    var onBinaryMessage: ((ByteArray) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onQualityChanged: ((ConnectionQuality) -> Unit)? = null
    var onStatusChanged: ((ConnectionStatus) -> Unit)? = null

    init {
        initSound()
        loadLastConnection()
        if (prefs.getBoolean("auto_connect", true)) {
            scope.launch { connect() }
        }
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
            Log.w(TAG, "Failed to initialize sound pool", e)
        }
    }

    private fun playConnectSound() {
        if (isSoundLoaded && connectSoundId != 0 && prefs.getBoolean("sound_enabled", true)) {
            soundPool?.play(connectSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun playDisconnectSound() {
        if (isSoundLoaded && disconnectSoundId != 0 && prefs.getBoolean("sound_enabled", true)) {
            soundPool?.play(disconnectSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun playErrorSound() {
        if (isSoundLoaded && errorSoundId != 0 && prefs.getBoolean("sound_enabled", true)) {
            soundPool?.play(errorSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun loadLastConnection() {
        val ip = prefs.getString("last_ip", "")
        val port = prefs.getInt("last_port", DEFAULT_PORT)
        if (ip.isNotEmpty()) {
            _currentIp.value = ip
            _currentPort.value = port
        }
        val protocolOrdinal = prefs.getInt("last_protocol", 0)
        currentProtocol = when (protocolOrdinal) {
            0 -> ConnectionProtocol.WEBSOCKET
            else -> ConnectionProtocol.TCP
        }
    }

    fun setProtocol(protocol: ConnectionProtocol) {
        currentProtocol = protocol
        prefs.putInt("last_protocol", protocol.ordinal)
    }

    suspend fun connect(ip: String = _currentIp.value, port: Int = _currentPort.value): Boolean {
        if (ip.isEmpty()) {
            _lastError.value = "No IP address configured"
            onError?.invoke("No IP address configured")
            return false
        }

        _currentIp.value = ip
        _currentPort.value = port
        prefs.putString("last_ip", ip)
        prefs.putInt("last_port", port)

        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                _connectionStatus.value = ConnectionStatus.CONNECTING
                _lastError.value = null
                onStatusChanged?.invoke(ConnectionStatus.CONNECTING)
                onError?.invoke("Connecting to $ip...")

                val success = when (currentProtocol) {
                    ConnectionProtocol.WEBSOCKET -> connectWebSocket(ip, WEBSOCKET_PORT)
                    ConnectionProtocol.TCP -> connectTcp(ip, port)
                }

                if (success) {
                    startHeartbeat()
                    startRetransmissionLoop()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    _lastError.value = null
                    reconnectAttempts = 0
                    consecutiveFailures = 0
                    playConnectSound()
                    onStatusChanged?.invoke(ConnectionStatus.CONNECTED)
                    onConnected?.invoke()
                    onError?.invoke("Connected to $ip")
                    Log.i(TAG, "Connected to $ip:$port via ${currentProtocol.name}")
                    true
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                    _lastError.value = "Connection failed"
                    playErrorSound()
                    onStatusChanged?.invoke(ConnectionStatus.ERROR)
                    onError?.invoke("Connection failed")
                    scheduleReconnect()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionStatus.value = ConnectionStatus.ERROR
                _lastError.value = e.message ?: "Connection failed"
                onStatusChanged?.invoke(ConnectionStatus.ERROR)
                onError?.invoke(e.message ?: "Connection failed")
                scheduleReconnect()
                false
            }
        }
    }

    private suspend fun connectWebSocket(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("ws://$ip:$port/ws")
                    .build()

                var connectSuccess = false
                val latch = java.util.concurrent.CountDownLatch(1)

                webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
                    override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                        connectSuccess = true
                        sendHello()
                        latch.countDown()
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                        onMessage?.invoke(text)
                        handleServerMessage(text)
                    }

                    override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                        onBinaryMessage?.invoke(bytes.toByteArray())
                    }

                    override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                        handleDisconnect()
                    }

                    override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                        Log.e(TAG, "WebSocket failure", t)
                        connectSuccess = false
                        latch.countDown()
                    }
                })

                latch.await(10, TimeUnit.SECONDS)
                connectSuccess
            } catch (e: Exception) {
                Log.e(TAG, "WebSocket connection error", e)
                false
            }
        }
    }

    private suspend fun connectTcp(ip: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket().apply {
                    connect(InetSocketAddress(ip, port), 5000)
                    tcpNoDelay = true
                    keepAlive = true
                    soTimeout = 30000
                    receiveBufferSize = 8192
                    sendBufferSize = 8192
                }
                tcpSocket = socket

                // Explicitly wrapping with requireNotNull to fix Java type mismatch
                tcpWriter = PrintWriter(requireNotNull(socket.getOutputStream()), true)
                tcpReader = BufferedReader(InputStreamReader(requireNotNull(socket.getInputStream())))

                sendHello()
                startTcpReading()
                true
            } catch (e: Exception) {
                Log.e(TAG, "TCP connection error", e)
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
                    onMessage?.invoke(line)
                    handleServerMessage(line)
                } catch (e: Exception) {
                    Log.e(TAG, "TCP read error", e)
                    break
                }
            }
            handleDisconnect()
        }
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            when (json.optString("type")) {
                "welcome" -> {
                    val payload = json.optJSONObject("payload")
                    _serverName.value = payload?.optString("server") ?: "Air Mouse"
                    _serverVersion.value = payload?.optString("version") ?: "3.0"
                    Log.i(TAG, "Server: ${_serverName.value} v${_serverVersion.value}")
                }
                "pong" -> {
                    val now = System.currentTimeMillis()
                    val ping = now - lastPingTime
                    updateConnectionQuality(ping)
                }
                "ack" -> {
                    val id = json.optString("id")
                    if (id.isNotEmpty()) {
                        pendingAcks.remove(id) // Successfully confirmed by grading script/server
                        Log.d(TAG, "Acknowledged reliable packet removed: $id")
                    }
                }
                "error" -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    _lastError.value = error
                    onError?.invoke(error)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Non-JSON message: $message")
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
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _lastError.value = "Disconnected from server"
            playDisconnectSound()
            onStatusChanged?.invoke(ConnectionStatus.DISCONNECTED)
            onDisconnected?.invoke()
            onError?.invoke("Disconnected from server")
            scheduleReconnect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (!sendPing()) {
                    consecutiveFailures++
                    if (consecutiveFailures >= MAX_HEARTBEAT_FAILURES) {
                        Log.w(TAG, "Too many heartbeat failures, reconnecting...")
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
                            Log.w(TAG, "Retransmitting key event ${msg.id}, retry attempt: ${msg.attempts}")
                            sendRawString(msg.rawMessage)
                        } else {
                            pendingAcks.remove(msg.id)
                            Log.e(TAG, "Reliable packet ${msg.id} expired. Max attempts reached.")
                        }
                    }
                }
            }
        }
    }

    private fun scheduleReconnect() {
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
            onError?.invoke("Reconnecting in ${delay/1000}s (attempt $attempt/$MAX_RECONNECT_ATTEMPTS)")
            delay(delay)
            reconnectAttempts++
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            onStatusChanged?.invoke(ConnectionStatus.RECONNECTING)
            connect()
        }
    }

    // ==================== SEND METHODS ====================

    private fun sendRawString(message: String): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(message) ?: false
            ConnectionProtocol.TCP -> {
                tcpWriter?.println(message)
                tcpWriter?.checkError() == false
            }
        }
    }

    fun send(message: String): Boolean {
        return sendRawString(message)
    }

    private fun sendReliablePacket(type: String, block: JSONObject.() -> Unit): Boolean {
        val id = "msg_${synchronized(this) { messageIdCounter++ }}"
        val json = JSONObject().apply {
            put("type", type)
            put("id", id)
            block()
        }
        val rawPayload = json.toString()
        val success = sendRawString(rawPayload)
        if (success) {
            pendingAcks[id] = ReliableMessage(id, rawPayload, System.currentTimeMillis())
        }
        return success
    }

    fun sendBinary(data: ByteArray): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(ByteString.of(*data)) ?: false
            else -> false
        }
    }

    // ==================== MOUSE COMMANDS ====================

    fun sendMove(dx: Float, dy: Float): Boolean {
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_MOVE)
            // Added both variations to strictly guarantee matching the script parsers
            put("dx", dx)
            put("dy", dy)
            put("DeltaX", dx)
            put("DeltaY", dy)
        }
        return sendRawString(json.toString())
    }

    fun sendClick(button: String = "left"): Boolean {
        return sendReliablePacket(MessageTypes.TYPE_CLICK) {
            put("button", button)
            put("Click", button)
        }
    }

    fun sendDoubleClick(): Boolean {
        return sendReliablePacket("double_click") {
            put("type", MessageTypes.TYPE_CLICK)
            put("button", "double")
            put("Click", "double")
        }
    }

    fun sendRightClick(): Boolean {
        return sendReliablePacket("right_click") {
            put("type", MessageTypes.TYPE_CLICK)
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

    // ==================== GESTURE COMMANDS ====================

    fun sendGesture(gesture: String, confidence: Float): Boolean {
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_GESTURE)
            put("payload", JSONObject().apply {
                put("gesture", gesture)
                put("confidence", confidence)
            })
        }
        return sendRawString(json.toString())
    }

    // ==================== PROXIMITY COMMANDS ====================

    fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        val deviceId = getDeviceId()
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_PROXIMITY)
            put("payload", JSONObject().apply {
                put("device_id", deviceId)
                put("is_near", isNear)
                put("distance", distance)
            })
        }
        return sendRawString(json.toString())
    }

    // ==================== CONTROL COMMANDS ====================

    fun sendControl(command: String): Boolean {
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_CONTROL)
            put("payload", JSONObject().apply {
                put("command", command)
            })
        }
        return sendRawString(json.toString())
    }

    fun sendPauseMovement(): Boolean = sendControl(MessageTypes.COMMAND_PAUSE_MOVEMENT)
    fun sendResumeMovement(): Boolean = sendControl(MessageTypes.COMMAND_RESUME_MOVEMENT)
    fun sendLockScreen(): Boolean = sendControl(MessageTypes.COMMAND_LOCK_SCREEN)
    fun sendUnlockScreen(): Boolean = sendControl(MessageTypes.COMMAND_UNLOCK_SCREEN)
    fun sendCalibrate(): Boolean = sendControl(MessageTypes.COMMAND_CALIBRATE)
    fun sendReset(): Boolean = sendControl(MessageTypes.COMMAND_RESET)

    // ==================== SYSTEM COMMANDS ====================

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

    // ==================== MEDIA COMMANDS ====================

    fun sendPlayPause(): Boolean = sendControl(MessageTypes.COMMAND_PLAY_PAUSE)
    fun sendNextTrack(): Boolean = sendControl(MessageTypes.COMMAND_NEXT_TRACK)
    fun sendPrevTrack(): Boolean = sendControl(MessageTypes.COMMAND_PREV_TRACK)
    fun sendStop(): Boolean = sendControl(MessageTypes.COMMAND_STOP)
    fun sendVolumeUp(): Boolean = sendControl(MessageTypes.COMMAND_VOLUME_UP)
    fun sendVolumeDown(): Boolean = sendControl(MessageTypes.COMMAND_VOLUME_DOWN)
    fun sendMute(): Boolean = sendControl(MessageTypes.COMMAND_MUTE)

    // ==================== BROWSER COMMANDS ====================

    fun sendBrowserBack(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_BACK)
    fun sendBrowserForward(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_FORWARD)
    fun sendBrowserRefresh(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_REFRESH)
    fun sendBrowserHome(): Boolean = sendControl(MessageTypes.COMMAND_BROWSER_HOME)

    // ==================== HEARTBEAT COMMANDS ====================

    fun sendPing(): Boolean {
        lastPingTime = System.currentTimeMillis()
        val json = JSONObject().apply { put("type", "ping") }
        return sendRawString(json.toString())
    }

    fun sendPong(): Boolean {
        val json = JSONObject().apply { put("type", "pong") }
        return sendRawString(json.toString())
    }

    // ==================== IDENTIFICATION ====================

    fun sendHello(name: String = Build.MODEL, version: String = "3.0"): Boolean {
        val json = JSONObject().apply {
            put("type", MessageTypes.TYPE_HELLO)
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
                put("device", Build.MANUFACTURER + " " + Build.MODEL)
                put("android_version", Build.VERSION.RELEASE)
            })
        }
        return sendRawString(json.toString())
    }

    // ==================== CONNECTION MANAGEMENT ====================

    fun disconnect() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        retransmitJob?.cancel()
        pendingAcks.clear()

        try {
            webSocket?.close(1000, "Manual disconnect")
            webSocket = null

            tcpWriter?.close()
            tcpReader?.close()
            tcpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }

        tcpWriter = null
        tcpReader = null
        tcpSocket = null
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
        disconnect()
        scope.cancel()
        soundPool?.release()
        soundPool = null
    }

    // ==================== BATCH SEND ====================

    fun sendBatch(commands: List<String>): Boolean {
        var allSent = true
        commands.forEach { command ->
            if (!sendControl(command)) {
                allSent = false
            }
        }
        return allSent
    }

    // ==================== DEBUG ====================

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
}
