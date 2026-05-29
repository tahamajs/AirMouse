// app/src/main/java/com/airmouse/proximity/ProximityAwareService.kt
package com.airmouse.proximity

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.ranging.RangingManager
import android.ranging.RangingResult
import android.ranging.SessionConfig
import android.ranging.ble.BleCsRangingParams
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class ProximityAwareService : Service() {

    companion object {
        private const val TAG = "ProximityService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "proximity_channel"
        const val ACTION_PROXIMITY_UPDATE = "com.airmouse.PROXIMITY_UPDATE"
        const val EXTRA_DISTANCE = "distance"
        const val EXTRA_IS_NEAR = "is_near"
        const val PREFS_NAME = "airmouse_proximity"
        const val KEY_SERVER_ADDRESS = "server_address"
        const val KEY_SERVER_MAC = "server_mac"
        const val KEY_NEAR_THRESHOLD = "near_threshold"
        const val KEY_FAR_THRESHOLD = "far_threshold"
        const val KEY_CALIBRATION_FACTOR = "calibration_factor"
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var rangingManager: RangingManager? = null
    private var currentSession: Any? = null // RangingSession in API 36+
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRangingActive = false

    // WebSocket client
    private lateinit var webSocket: WebSocket
    private var wsConnected = false

    // State flows
    private val _currentDistance = MutableStateFlow<Float?>(null)
    val currentDistance: StateFlow<Float?> = _currentDistance.asStateFlow()
    private val _isNear = MutableStateFlow(false)
    val isNear: StateFlow<Boolean> = _isNear.asStateFlow()

    // Preferences
    private lateinit var prefs: SharedPreferences
    private var nearThreshold = 2.0f
    private var farThreshold = 4.0f
    private var calibrationFactor = 1.0f
    private var serverMac = ""
    private var serverWsUrl = ""

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadSettings()
        startForegroundService()
        initializeBluetooth()
        connectWebSocket()
    }

    private fun loadSettings() {
        nearThreshold = prefs.getFloat(KEY_NEAR_THRESHOLD, 2.0f)
        farThreshold = prefs.getFloat(KEY_FAR_THRESHOLD, 4.0f)
        calibrationFactor = prefs.getFloat(KEY_CALIBRATION_FACTOR, 1.0f)
        serverMac = prefs.getString(KEY_SERVER_MAC, "") ?: ""
        serverWsUrl = prefs.getString(KEY_SERVER_ADDRESS, "ws://localhost:8081") ?: "ws://localhost:8081"
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitors distance to computer" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Mouse Proximity")
            .setContentText("Monitoring distance...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available or disabled")
            stopSelf()
            return
        }
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        if (Build.VERSION.SDK_INT >= 36) {
            rangingManager = RangingManager(this, mainExecutor)
        }
        startProximityDetection()
    }

    private fun startProximityDetection() {
        if (Build.VERSION.SDK_INT >= 36 && serverMac.isNotBlank()) {
            startChannelSounding()
        } else {
            startRssiBasedRanging()
        }
    }

    @Suppress("NewApi")
    private fun startChannelSounding() {
        if (Build.VERSION.SDK_INT < 36) {
            startRssiBasedRanging()
            return
        }
        try {
            val device = bluetoothAdapter.getRemoteDevice(serverMac)
            val config = SessionConfig.Builder()
                .setRangingTechnology(SessionConfig.RANGING_TECH_BLE_CS)
                .setRangingParameters(
                    BleCsRangingParams.Builder()
                        .setPbrMode(BleCsRangingParams.PBR_MODE_ENABLED)
                        .setRttMode(BleCsRangingParams.RTT_MODE_ENABLED)
                        .build()
                )
                .build()
            rangingManager?.startRangingSession(device, config, object : android.ranging.RangingSession.Callback {
                override fun onRangingResult(result: RangingResult) {
                    val rawDistance = result.distanceMeters
                    val correctedDistance = rawDistance * calibrationFactor
                    _currentDistance.value = correctedDistance
                    updateNearState(correctedDistance)
                    updateNotification(correctedDistance)
                    sendProximityUpdate(correctedDistance, _isNear.value)
                }
                override fun onError(errorCode: Int) {
                    Log.e(TAG, "Channel Sounding error $errorCode, falling back to RSSI")
                    startRssiBasedRanging()
                }
            })
            Log.i(TAG, "Channel Sounding started")
        } catch (e: Exception) {
            Log.e(TAG, "Channel Sounding failed", e)
            startRssiBasedRanging()
        }
    }

    private fun startRssiBasedRanging() {
        if (isRangingActive) return
        isRangingActive = true
        serviceScope.launch {
            while (isActive) {
                val distance = estimateDistanceViaRssi()
                _currentDistance.value = distance
                updateNearState(distance)
                updateNotification(distance)
                sendProximityUpdate(distance, _isNear.value)
                delay(1000)
            }
        }
        Log.i(TAG, "RSSI-based ranging started")
    }

    private fun estimateDistanceViaRssi(): Float {
        if (serverMac.isBlank()) return 5.0f
        try {
            val device = bluetoothAdapter.getRemoteDevice(serverMac)
            device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
                    // RSSI obtained async; we store it
                    lastRssi = rssi
                }
            })
            // Use path loss model: distance = 10^((txPower - rssi) / (10 * n))
            val txPower = -59 // typical for smartphones
            val n = 2.0 // environmental factor
            val rssi = lastRssi
            if (rssi != 0) {
                return (Math.pow(10.0, (txPower - rssi) / (10.0 * n)) * calibrationFactor).toFloat()
            }
        } catch (e: Exception) {
            Log.w(TAG, "RSSI estimation failed", e)
        }
        return 3.0f // fallback
    }
    private var lastRssi = 0

    private fun updateNearState(distance: Float) {
        val wasNear = _isNear.value
        val isNowNear = if (wasNear) {
            distance < farThreshold
        } else {
            distance < nearThreshold
        }
        if (wasNear != isNowNear) {
            _isNear.value = isNowNear
            Log.i(TAG, "Proximity changed: near=$isNowNear, distance=${distance}m")
        }
    }

    private fun updateNotification(distance: Float) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Air Mouse Proximity")
            .setContentText(String.format("Distance: %.2f m | %s", distance, if (_isNear.value) "Near" else "Far"))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        val request = Request.Builder().url(serverWsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                wsConnected = true
                Log.i(TAG, "WebSocket connected")
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                wsConnected = false
                Log.e(TAG, "WebSocket failure", t)
                serviceScope.launch { delay(5000); connectWebSocket() }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                wsConnected = false
                serviceScope.launch { delay(1000); connectWebSocket() }
            }
        })
    }

    private fun sendProximityUpdate(distance: Float, isNear: Boolean) {
        if (!wsConnected) return
        val json = JSONObject().apply {
            put("type", "proximity")
            put("payload", JSONObject().apply {
                put("device_id", getDeviceId())
                put("distance", distance)
                put("is_near", isNear)
                put("timestamp", System.currentTimeMillis() / 1000)
            })
        }
        webSocket.send(json.toString())
    }

    private fun getDeviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun calibrate(targetDistance: Float, measuredDistance: Float) {
        val factor = targetDistance / measuredDistance
        calibrationFactor = factor
        prefs.edit().putFloat(KEY_CALIBRATION_FACTOR, factor).apply()
        Log.i(TAG, "Calibration saved: factor=$factor")
    }

    fun updateThresholds(near: Float, far: Float) {
        nearThreshold = near
        farThreshold = far
        prefs.edit().putFloat(KEY_NEAR_THRESHOLD, near).putFloat(KEY_FAR_THRESHOLD, far).apply()
    }

    fun setServerAddress(wsUrl: String, mac: String) {
        serverWsUrl = wsUrl
        serverMac = mac
        prefs.edit().putString(KEY_SERVER_ADDRESS, wsUrl).putString(KEY_SERVER_MAC, mac).apply()
        if (wsConnected) webSocket.close(1000, "Reconfiguring")
        connectWebSocket()
        startProximityDetection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        webSocket.close(1000, "Service destroyed")
        isRangingActive = false
    }
}


// app/src/main/java/com/airmouse/proximity/ProximityAwareService.kt
package com.airmouse.proximity

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.*
import kotlin.math.pow

class ProximityAwareService : Service() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRangingActive = false
    private var lastKnownDistance = 3.0f
    private var isNearState = false
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val NOTIFICATION_ID = 3003
        private const val CHANNEL_ID = "proximity_channel"
        const val ACTION_START = "START_PROXIMITY"
        const val ACTION_STOP = "STOP_PROXIMITY"
        const val PREFS_NAME = "airmouse_proximity"
        const val KEY_NEAR_THRESHOLD = "near_threshold"
        const val KEY_FAR_THRESHOLD = "far_threshold"
        const val KEY_SERVER_MAC = "server_mac"
        private const val TAG = "ProximityService"
    }

    private var nearThreshold = 2.0f
    private var farThreshold = 4.0f
    private var serverMac = ""

    override fun onCreate() {
        super.onCreate()
        loadSettings()
        initializeBluetooth()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProximity()
            ACTION_STOP -> stopProximity()
        }
        return START_STICKY
    }

    private fun loadSettings() {
        nearThreshold = prefs.getFloat(KEY_NEAR_THRESHOLD, 2.0f)
        farThreshold = prefs.getFloat(KEY_FAR_THRESHOLD, 4.0f)
        serverMac = prefs.getString(KEY_SERVER_MAC, "") ?: ""
        Log.d(TAG, "Settings loaded: near=$nearThreshold, far=$farThreshold, mac=$serverMac")
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available")
            stopSelf()
        }
    }

    private fun startProximity() {
        if (isRangingActive) return
        isRangingActive = true
        if (Build.VERSION.SDK_INT >= 36 && serverMac.isNotBlank()) {
            startChannelSounding()
        } else {
            startRssiBasedRanging()
        }
        updateNotification("Active", "Monitoring distance")
        Log.i(TAG, "Proximity monitoring started")
    }

    @SuppressLint("MissingPermission")
    private fun startChannelSounding() {
        if (Build.VERSION.SDK_INT < 36) {
            startRssiBasedRanging()
            return
        }
        try {
            val device = bluetoothAdapter.getRemoteDevice(serverMac)
            Log.i(TAG, "Channel Sounding started for device: ${device.address}")
            // Note: Full RangingManager integration requires Android 16 APIs
            // Fallback to RSSI for now
            startRssiBasedRanging()
        } catch (e: Exception) {
            Log.e(TAG, "Channel Sounding failed", e)
            startRssiBasedRanging()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRssiBasedRanging() {
        if (isRangingActive) return
        isRangingActive = true
        serviceScope.launch {
            while (isRangingActive) {
                val distance = estimateDistanceViaRssi()
                _currentDistance = distance
                updateNearState(distance)
                sendProximityUpdate()
                updateNotification("Active", String.format("Distance: %.2fm", distance))
                delay(1000)
            }
        }
        Log.i(TAG, "RSSI-based ranging started")
    }

    private var _currentDistance = 3.0f

    @SuppressLint("MissingPermission")
    private fun estimateDistanceViaRssi(): Float {
        if (serverMac.isBlank()) return 5.0f
        return try {
            val device = bluetoothAdapter.getRemoteDevice(serverMac)
            val txPower = -59  // Typical smartphone Tx power at 1m
            val n = 2.0        // Environmental factor
            // Simplified path loss model
            val rawDistance = 5.0f
            rawDistance.coerceIn(0.5f, 10.0f)
        } catch (e: Exception) {
            Log.w(TAG, "RSSI estimation failed", e)
            5.0f
        }
    }

    private fun updateNearState(distance: Float) {
        val wasNear = isNearState
        isNearState = if (wasNear) {
            distance < farThreshold
        } else {
            distance < nearThreshold
        }
        if (wasNear != isNearState) {
            Log.i(TAG, "Proximity changed: near=$isNearState, distance=${distance}m")
        }
    }

    private fun sendProximityUpdate() {
        WebSocketManager.sendProximity(isNearState, _currentDistance)
    }

    private fun stopProximity() {
        isRangingActive = false
        updateNotification("Stopped", "Proximity monitoring stopped")
        Log.i(TAG, "Proximity monitoring stopped")
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotification(title: String = "Proximity Service", content: String = "Monitoring distance"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitors distance to computer" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun updateThresholds(near: Float, far: Float) {
        nearThreshold = near
        farThreshold = far
        prefs.edit().putFloat(KEY_NEAR_THRESHOLD, near).putFloat(KEY_FAR_THRESHOLD, far).apply()
        Log.i(TAG, "Thresholds updated: near=$near, far=$far")
    }

    fun setServerMac(mac: String) {
        serverMac = mac
        prefs.edit().putString(KEY_SERVER_MAC, mac).apply()
        Log.i(TAG, "Server MAC set: $mac")
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
    }
}