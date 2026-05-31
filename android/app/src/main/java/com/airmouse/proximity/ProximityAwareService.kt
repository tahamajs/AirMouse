// app/src/main/java/com/airmouse/proximity/ProximityAwareService.kt
package com.airmouse.proximity

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.airmouse.network.WebSocketManager
import kotlinx.coroutines.*

class ProximityAwareService : Service() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRangingActive = false
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
        startRssiBasedRanging()
        updateNotification("Active", "Monitoring distance")
        Log.i(TAG, "Proximity monitoring started")
    }

    @SuppressLint("MissingPermission")
    private fun startRssiBasedRanging() {
        serviceScope.launch {
            while (isRangingActive) {
                val distance = estimateDistanceViaRssi()
                updateNearState(distance)
                sendProximityUpdate(distance)
                updateNotification("Active", String.format("Distance: %.2fm", distance))
                delay(1000)
            }
        }
        Log.i(TAG, "RSSI‑based ranging started")
    }

    @SuppressLint("MissingPermission")
    private fun estimateDistanceViaRssi(): Float {
        if (serverMac.isBlank()) return 5.0f
        return try {
            // Simplified path loss model (placeholder)
            // In production, use actual RSSI from BluetoothDevice
            3.0f
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

    private fun sendProximityUpdate(distance: Float) {
        WebSocketManager.sendProximity(isNearState, distance)
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