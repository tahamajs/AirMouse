// app/src/main/java/com/airmouse/network/ConnectionManager.kt
package com.airmouse.network

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
        private const val HEARTBEAT_INTERVAL_MS = 30000L
        private const val RECONNECT_DELAY_MS = 3000L
        private const val MAX_RECONNECT_ATTEMPTS = 10
    }

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
    }

    enum class ConnectionProtocol { TCP, WEBSOCKET, UDP }

    data class ConnectionQuality(
        val rssi: Int = 0,
        val ping: Int = 0,
        val jitter: Int = 0,
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
    }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentIp = MutableStateFlow("")
    val currentIp: StateFlow<String> = _currentIp.asStateFlow()

    private val _currentPort = MutableStateFlow(DEFAULT_PORT)
    val currentPort: StateFlow<Int> = _currentPort.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality())
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private var webSocket: okhttp3.WebSocket? = null
    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var tcpReader: BufferedReader? = null
    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var currentProtocol = ConnectionProtocol.WEBSOCKET
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Sound alert
    private var soundPool: SoundPool? = null
    private var disconnectSoundId = 0
    private var isSoundLoaded = false

    // Callbacks
    var onMessage: ((String) -> Unit)? = null
    var onBinaryMessage: ((ByteArray) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onConnected: (() -> Unit)? = null

    init {
        initSound()
        loadLastConnection()
        // Auto‑connect if enabled
        if (prefs.getBoolean("auto_connect", true)) {
            scope.launch { connect() }
        }
    }

    private fun initSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        soundPool?.setOnLoadCompleteListener { _, _, _ -> isSoundLoaded = true }
        // Add disconnect.mp3 to res/raw/
        // disconnectSoundId = soundPool?.load(context, R.raw.disconnect, 1) ?: 0
    }

    private fun playDisconnectSound() {
        if (isSoundLoaded && disconnectSoundId != 0) {
            soundPool?.play(disconnectSoundId, 1f, 1f, 1, 0, 1f)
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
            1 -> ConnectionProtocol.TCP
            else -> ConnectionProtocol.WEBSOCKET
        }
    }

    fun setProtocol(protocol: ConnectionProtocol) {
        currentProtocol = protocol
        prefs.putInt("last_protocol", protocol.ordinal)
    }

    suspend fun connect(ip: String = _currentIp.value, port: Int = _currentPort.value): Boolean {
        if (ip.isEmpty()) return false
        _currentIp.value = ip
        _currentPort.value = port
        prefs.putString("last_ip", ip)
        prefs.putInt("last_port", port)

        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                _connectionStatus.value = ConnectionStatus.CONNECTING
                when (currentProtocol) {
                    ConnectionProtocol.WEBSOCKET -> connectWebSocket(ip, port)
                    ConnectionProtocol.TCP -> connectTcp(ip, port)
                    ConnectionProtocol.UDP -> connectUdp(ip, port)
                }
                startHeartbeat()
                _connectionStatus.value = ConnectionStatus.CONNECTED
                reconnectAttempts = 0
                onConnected?.invoke()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionStatus.value = ConnectionStatus.ERROR
                scheduleReconnect()
                false
            }
        }
    }

    private suspend fun connectWebSocket(ip: String, port: Int) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("ws://$ip:$port/ws")
            .build()
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: okhttp3.WebSocket, response: okhttp3.Response) {
                Log.i(TAG, "WebSocket opened")
            }
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                onMessage?.invoke(text)
            }
            override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) {
                onBinaryMessage?.invoke(bytes.toByteArray())
            }
            override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
                Log.i(TAG, "WebSocket closed")
                handleDisconnect()
            }
            override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "WebSocket failure", t)
                handleDisconnect()
            }
        })
    }

    private suspend fun connectTcp(ip: String, port: Int) {
        tcpSocket = Socket().apply {
            connect(InetSocketAddress(ip, port), 5000)
            tcpNoDelay = true
        }
        tcpWriter = PrintWriter(tcpSocket?.getOutputStream(), true)
        tcpReader = BufferedReader(InputStreamReader(tcpSocket?.getInputStream()))
        startTcpReading()
    }

    private suspend fun connectUdp(ip: String, port: Int) {
        // Not implemented for heartbeat; UDP is connectionless.
    }

    private fun startTcpReading() {
        readJob?.cancel()
        readJob = scope.launch {
            while (tcpSocket?.isConnected == true) {
                try {
                    val line = tcpReader?.readLine() ?: break
                    onMessage?.invoke(line)
                } catch (e: Exception) {
                    break
                }
            }
            handleDisconnect()
        }
    }

    private fun handleDisconnect() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            playDisconnectSound()
            onDisconnected?.invoke()
            scheduleReconnect()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(HEARTBEAT_INTERVAL_MS)
                sendPing()
                measureConnectionQuality()
            }
        }
    }

    private suspend fun measureConnectionQuality() {
        val ip = _currentIp.value
        if (ip.isEmpty()) return
        val ping = measurePing(ip)
        val signal = when {
            ping < 50 -> ConnectionQuality.SignalStrength.EXCELLENT
            ping < 100 -> ConnectionQuality.SignalStrength.GOOD
            ping < 200 -> ConnectionQuality.SignalStrength.FAIR
            else -> ConnectionQuality.SignalStrength.POOR
        }
        _connectionQuality.value = ConnectionQuality(ping = ping, signalStrength = signal)
    }

    private suspend fun measurePing(ip: String): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, 80), 1000)
            socket.close()
            (System.currentTimeMillis() - start).toInt()
        } catch (e: Exception) { 999 }
    }

    private fun sendPing() {
        send("""{"type":"ping"}""")
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = RECONNECT_DELAY_MS * (reconnectAttempts + 1)
            delay(delay)
            reconnectAttempts++
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            connect()
        }
    }

    fun send(message: String): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(message) ?: false
            ConnectionProtocol.TCP -> {
                tcpWriter?.println(message)
                true
            }
            else -> false
        }
    }

    fun sendBinary(data: ByteArray): Boolean {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return false
        return when (currentProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocket?.send(ByteString.of(*data)) ?: false
            else -> false
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        tcpWriter?.close()
        tcpReader?.close()
        tcpSocket?.close()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionStatus.value == ConnectionStatus.CONNECTED

    fun cleanup() {
        disconnect()
        scope.cancel()
        soundPool?.release()
    }


    fun sendBinary(data: ByteArray) {
        // For WebSocket
        webSocket?.send(ByteString.of(*data))
        // For TCP you would need a different transport.
    }

    var onBinaryMessage: ((ByteArray) -> Unit)? = null



    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onBinaryMessage?.invoke(bytes.toByteArray())
    }override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onBinaryMessage?.invoke(bytes.toByteArray())
    }
}