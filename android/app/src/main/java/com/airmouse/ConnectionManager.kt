package com.airmouse

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.airmouse.bluetooth.BtHidHelper
import com.airmouse.network.DataSender
import com.airmouse.network.WebSocketManager
import com.airmouse.touchpad.TcpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Central connection manager that unifies wired (DataSender / WebSocket / TcpClient)
 * and Bluetooth (HID) lifecycles and exposes LiveData for UI to observe.
 */
object ConnectionManager {
    private const val TAG = "ConnectionManager"

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

    // ----- TCP (diagnostic / touchpad) -----
    private val _tcpState = MutableLiveData(ConnectionState.DISCONNECTED)
    val tcpState: LiveData<ConnectionState> = _tcpState
    private val _tcpStatus = MutableLiveData("")
    val tcpStatus: LiveData<String> = _tcpStatus
    private var tcpClient: TcpClient? = null

    // ----- DataSender (reliable command channel used in main flow) -----
    private val _dataSenderState = MutableLiveData(ConnectionState.DISCONNECTED)
    val dataSenderState: LiveData<ConnectionState> = _dataSenderState
    private var dataSender: DataSender? = null

    // ----- WebSocket (optional alternate channel) -----
    private val _wsState = MutableLiveData(ConnectionState.DISCONNECTED)
    val webSocketState: LiveData<ConnectionState> = _wsState

    // ----- Bluetooth HID -----
    private val _bluetoothRunning = MutableLiveData(false)
    val bluetoothRunning: LiveData<Boolean> = _bluetoothRunning

    private var appContext: Context? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun init(context: Context) {
        appContext = context.applicationContext
        // wire existing WebSocket callbacks
        WebSocketManager.onConnected = { _wsState.postValue(ConnectionState.CONNECTED) }
        WebSocketManager.onDisconnected = { _wsState.postValue(ConnectionState.DISCONNECTED) }
        WebSocketManager.onMessage = { msg -> Log.d(TAG, "WS msg: $msg") }
    }

    // ----- TCP client (touchpad diagnostics) -----
    fun connectTcp(ip: String, port: Int) {
        _tcpState.postValue(ConnectionState.CONNECTING)
        if (tcpClient == null) {
            tcpClient = TcpClient { status ->
                // report human-readable status to UI
                _tcpStatus.postValue(status)
                // derive connected state heuristically
                if (status.contains("Connected", ignoreCase = true) || status.contains("Reconnected", ignoreCase = true)) {
                    _tcpState.postValue(ConnectionState.CONNECTED)
                } else if (status.contains("Disconnected", ignoreCase = true) || status.contains("Disconnected", ignoreCase = true)) {
                    _tcpState.postValue(ConnectionState.DISCONNECTED)
                }
                // also update debug overlay if running
                try { DebugOverlayService.updateConnectionState(status) } catch (_: Exception) {}
            }
        }
        scope.launch { try { tcpClient?.connect(ip, port) } catch (t: Throwable) { _tcpState.postValue(ConnectionState.DISCONNECTED); _tcpStatus.postValue("Error: ${t.message}") } }
    }

    fun disconnectTcp() {
        scope.launch {
            try {
                tcpClient?.disconnect()
            } catch (t: Throwable) {
                Log.w(TAG, "tcp disconnect failed", t)
            } finally {
                _tcpState.postValue(ConnectionState.DISCONNECTED)
                _tcpStatus.postValue("")
            }
        }
    }

    // Provide a simple accessor for the touchpad sender
    fun sendTcpMessage(msg: String) {
        scope.launch { tcpClient?.send(msg) }
    }

    // ----- DataSender (app command channel) -----
    fun startDataSender(host: String, port: Int) {
        scope.launch {
            _dataSenderState.postValue(ConnectionState.CONNECTING)
            try {
                dataSender = DataSender.getInstance(host, port) ?: DataSender(host, port).also { DataSender.getInstance(host, port) }
                dataSender?.apply {
                    onConnected = {
                        _dataSenderState.postValue(ConnectionState.CONNECTED)
                        try { DebugOverlayService.updateConnectionState("DataSender: Connected") } catch (_: Exception) {}
                    }
                    onDisconnected = {
                        _dataSenderState.postValue(ConnectionState.RECONNECTING)
                        try { DebugOverlayService.updateConnectionState("DataSender: Disconnected") } catch (_: Exception) {}
                    }
                    onError = {
                        _dataSenderState.postValue(ConnectionState.DISCONNECTED)
                        try { DebugOverlayService.updateConnectionState("DataSender: Error") } catch (_: Exception) {}
                    }
                    start()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "startDataSender failed", t)
                _dataSenderState.postValue(ConnectionState.DISCONNECTED)
            }
        }
    }

    fun stopDataSender() {
        scope.launch {
            try {
                dataSender?.stopSending()
            } catch (t: Throwable) { Log.w(TAG, "stopDataSender", t) }
            dataSender = null
            _dataSenderState.postValue(ConnectionState.DISCONNECTED)
        }
    }

    fun sendMove(dx: Float, dy: Float) { scope.launch { dataSender?.sendMove(dx, dy) } }
    fun sendClick() { scope.launch { dataSender?.sendClick() } }
    fun sendDoubleClick() { scope.launch { dataSender?.sendDoubleClick() } }
    fun sendRightClick() { scope.launch { dataSender?.sendRightClick() } }
    fun sendScroll(delta: Int) { scope.launch { dataSender?.sendScroll(delta) } }
    fun sendHello(name: String) { scope.launch { dataSender?.sendHello(name) } }

    // ----- WebSocket wrapper -----
    fun connectWebSocket(url: String) {
        _wsState.postValue(ConnectionState.CONNECTING)
        try {
            WebSocketManager.connect(url)
            // callbacks wired in init will update LiveData
        } catch (t: Throwable) {
            Log.e(TAG, "WebSocket connect failed", t)
            _wsState.postValue(ConnectionState.DISCONNECTED)
        }
    }

    fun disconnectWebSocket() {
        try {
            WebSocketManager.disconnect()
        } catch (t: Throwable) { Log.w(TAG, "ws disconnect", t) }
        _wsState.postValue(ConnectionState.DISCONNECTED)
    }

    fun sendWebSocketMessage(msg: String) { scope.launch { WebSocketManager.send(msg) } }

    // ----- Bluetooth HID helper -----
    fun startBluetooth() {
        try {
            appContext?.let { ctx -> BtHidHelper.startService(ctx); _bluetoothRunning.postValue(true) }
        } catch (t: Throwable) {
            Log.e(TAG, "startBluetooth failed", t)
            _bluetoothRunning.postValue(false)
        }
    }

    fun stopBluetooth() {
        try {
            appContext?.let { ctx -> BtHidHelper.stopService(ctx); _bluetoothRunning.postValue(false) }
        } catch (t: Throwable) {
            Log.w(TAG, "stopBluetooth failed", t)
        }
    }

    // Helper query
    fun isBluetoothRunning(): Boolean = _bluetoothRunning.value == true
}
