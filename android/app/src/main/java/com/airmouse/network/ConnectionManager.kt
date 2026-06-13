// app/src/main/java/com/airmouse/network/ConnectionManager.kt
package com.airmouse.network

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
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
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
    }

    enum class ConnectionProtocol { TCP, WEBSOCKET }

    data class ConnectionQuality(
        val rssi: Int = 0,
        val ping: Int = 0,
        val jitter: Int = 0,
        val packetLoss: Float = 0f,
        val signalStrength: SignalStrength = SignalStrength.UNKNOWN
    ) {
        enum class SignalStrength { EXCELLENT, GOOD, FAIR, POOR, UNKNOWN }
        
        fun level(): Int = when (signalStrength) {
            SignalStrength.EXCELLENT -> 100
            SignalStrength.GOOD -> 75
            SignalStrength.FAIR -> 50
            SignalStrength.POOR -> 25
            SignalStrength.UNKNOWN -> 0
        }
        
        fun isHealthy(): Boolean = ping < 200 && packetLoss < 0.1f
    }

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

    // Connection components
    private var webSocket: okhttp3.WebSocket? = null
    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var tcpReader: BufferedReader? = null
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
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
            // Note: Add sound files to res/raw/ directory
            // connectSoundId = soundPool?.load(context, R.raw.connect, 1) ?: 0
            // disconnectSoundId = soundPool?.load(context, R.raw.disconnect, 1) ?: 0
            // errorSoundId = soundPool?.load(context, R.raw.error, 1) ?: 0
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
                onError?.invoke("Connecting to $ip...")
                
                val success = when (currentProtocol) {
                    ConnectionProtocol.WEBSOCKET -> connectWebSocket(ip, WEBSOCKET_PORT)
                    ConnectionProtocol.TCP -> connectTcp(ip, port)
                }
                
                if (success) {
                    startHeartbeat()
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    reconnectAttempts = 0
                    consecutiveFailures = 0
                    playConnectSound()
                    onConnected?.invoke()
                    onError?.invoke("Connected to $ip")
                    Log.i(TAG, "Connected to $ip:$port via ${currentProtocol.name}")
                    true
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                    playErrorSound()
                    onError?.invoke("Connection failed")
                    scheduleReconnect()
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionStatus.value = ConnectionStatus.ERROR
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
                
                latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
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
                tcpSocket = Socket().apply {
                    connect(InetSocketAddress(ip, port), 5000)
                    tcpNoDelay = true
                    keepAlive = true
                    soTimeout = 30000
                }
                tcpWriter = PrintWriter(tcpSocket?.getOutputStream(), true)
                tcpReader = BufferedReader(InputStreamReader(tcpSocket?.getInputStream()))
                
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
                    // Handle acknowledgment if needed
                    val id = json.optString("id")
                    Log.d(TAG, "Received ACK for: $id")
                }
                "error" -> {
                    val error = json.optJSONObject("payload")?.optString("message") ?: "Unknown error"
                    onError?.invoke(error)
                }
            }
        } catch (e: Exception) {
            // Not JSON or malformed, ignore
            Log.d(TAG, "Non-JSON message: $message")
        }
    }

    private fun updateConnectionQuality(pingMs: Long) {
        val quality = _connectionQuality.value
        val newQuality = quality.copy(
            ping = pingMs.toInt(),
            signalStrength = when {
                pingMs < 50 -> ConnectionQuality.SignalStrength.EXCELLENT
                pingMs < 100 -> ConnectionQuality.SignalStrength.GOOD
                pingMs < 200 -> ConnectionQuality.SignalStrength.FAIR
                else -> ConnectionQuality.SignalStrength.POOR
            }
        )
        _connectionQuality.value = newQuality
        onQualityChanged?.invoke(newQuality)
    }

    private fun handleDisconnect() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            playDisconnectSound()
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
                    if (consecutiveFailures >= 3) {
                        Log.w(TAG, "Too many heartbeat failures, reconnecting...")
                        handleDisconnect()
                    }
                } else {
                    consecutiveFailures = 0
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _connectionStatus.value = ConnectionStatus.ERROR
            onError?.invoke("Max reconnection attempts reached")
            return
        }
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = RECONNECT_DELAY_MS * (reconnectAttempts + 1)
            onError?.invoke("Reconnecting in ${delay/1000}s (attempt ${reconnectAttempts + 1}/$MAX_RECONNECT_ATTEMPTS)")
            delay(delay)
            reconnectAttempts++
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            connect()
        }
    }

    // ==================== SEND METHODS ====================

    fun send(message: String): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(message) ?: false
            ConnectionProtocol.TCP -> {
                tcpWriter?.println(message)
                tcpWriter?.checkError()
                true
            }
        }
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
            put("type", "move")
            put("dx", dx)
            put("dy", dy)
        }
        return send(json.toString())
    }

    fun sendClick(button: String = "left"): Boolean {
        val json = JSONObject().apply {
            put("type", "click")
            put("button", button)
        }
        return send(json.toString())
    }

    fun sendDoubleClick(): Boolean {
        return send("""{"type":"doubleclick"}""")
    }

    fun sendRightClick(): Boolean {
        return send("""{"type":"rightclick"}""")
    }

    fun sendScroll(delta: Int): Boolean {
        val json = JSONObject().apply {
            put("type", "scroll")
            put("delta", delta)
        }
        return send(json.toString())
    }

    // ==================== GESTURE COMMANDS ====================

    fun sendGesture(gesture: String, confidence: Float): Boolean {
        val json = JSONObject().apply {
            put("type", "gesture")
            put("payload", JSONObject().apply {
                put("gesture", gesture)
                put("confidence", confidence)
            })
        }
        return send(json.toString())
    }

    // ==================== PROXIMITY COMMANDS ====================

    fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("device_id", deviceId)
                put("is_near", isNear)
                put("distance", distance)
            })
        }
        return send(json.toString())
    }

    // ==================== CONTROL COMMANDS ====================

    fun sendControl(command: String): Boolean {
        val json = JSONObject().apply {
            put("type", "control")
            put("payload", JSONObject().apply {
                put("command", command)
            })
        }
        return send(json.toString())
    }

    fun sendPauseMovement(): Boolean = sendControl("pause_movement")
    fun sendResumeMovement(): Boolean = sendControl("resume_movement")
    fun sendLockScreen(): Boolean = sendControl("lock_screen")
    fun sendUnlockScreen(): Boolean = sendControl("unlock_screen")
    fun sendCalibrate(): Boolean = sendControl("calibrate")
    fun sendReset(): Boolean = sendControl("reset")

    // ==================== SYSTEM COMMANDS ====================

    fun sendKeyPress(keyCode: Int): Boolean {
        val keyMap = mapOf(
            android.view.KeyEvent.KEYCODE_HOME to "home",
            android.view.KeyEvent.KEYCODE_BACK to "back",
            android.view.KeyEvent.KEYCODE_VOLUME_UP to "volume_up",
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN to "volume_down",
            android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "play_pause",
            android.view.KeyEvent.KEYCODE_MEDIA_NEXT to "next_track",
            android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS to "prev_track"
        )
        val command = keyMap[keyCode] ?: return false
        return sendControl(command)
    }

    fun sendWindowCommand(action: String): Boolean {
        val validActions = listOf("maximize", "minimize", "close", "fullscreen")
        return if (action in validActions) sendControl("window_$action") else false
    }

    // ==================== HEARTBEAT COMMANDS ====================

    fun sendPing(): Boolean {
        lastPingTime = System.currentTimeMillis()
        return send("""{"type":"ping"}""")
    }

    fun sendPong(): Boolean {
        return send("""{"type":"pong"}""")
    }

    // ==================== IDENTIFICATION ====================

    fun sendHello(name: String = android.os.Build.MODEL, version: String = "3.0"): Boolean {
        val json = JSONObject().apply {
            put("type", "hello")
            put("payload", JSONObject().apply {
                put("name", name)
                put("version", version)
                put("device", Build.MANUFACTURER + " " + Build.MODEL)
                put("android_version", Build.VERSION.RELEASE)
            })
        }
        return send(json.toString())
    }

    // ==================== UTILITY METHODS ====================

    fun disconnect() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        
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
    }

    fun isConnected(): Boolean = _connectionStatus.value == ConnectionStatus.CONNECTED
    
    fun reconnect() {
        reconnectAttempts = 0
        scope.launch { connect() }
    }

    fun getServerInfo(): Pair<String, String> = Pair(_serverName.value, _serverVersion.value)

    fun cleanup() {
        disconnect()
        scope.cancel()
        soundPool?.release()
        soundPool = null
    }
}