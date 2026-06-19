package com.airmouse.service

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.airmouse.R
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.pow
class ProximityAwareService : Service() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var targetDevice: BluetoothDevice? = null
    private var targetAddress = ""
    private var nearThreshold = 2.0f
    private var farThreshold = 4.0f
    private var currentRssi = -100
    private var isNear = false
    private var lastKnownDistance = 5.0f

    companion object {
        private const val NOTIFICATION_ID = 3003
        private const val CHANNEL_ID = "proximity_channel"
        const val ACTION_START = "START_PROXIMITY"
        const val ACTION_STOP = "STOP_PROXIMITY"
        const val ACTION_UPDATE_CONFIG = "UPDATE_CONFIG"
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_NEAR_THRESHOLD = "near_threshold"
        const val EXTRA_FAR_THRESHOLD = "far_threshold"
        const val BROADCAST_PROXIMITY_CHANGE = "com.airmouse.PROXIMITY_CHANGE"
        const val EXTRA_IS_NEAR = "is_near"
        const val EXTRA_DISTANCE = "distance"
        private const val TAG = "ProximityAwareService"
    }

    override fun onCreate() {
        super.onCreate()
        initializeBluetooth()
        createNotificationChannel()
        loadSettings()
        startForeground(NOTIFICATION_ID, createNotification("Initializing", "Proximity service starting"))
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth not available")
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Lock",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors distance to computer for auto lock/unlock"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("airmouse_proximity", Context.MODE_PRIVATE)
        targetAddress = prefs.getString("device_mac", "") ?: ""
        nearThreshold = prefs.getFloat("near_threshold", 2.0f)
        farThreshold = prefs.getFloat("far_threshold", 4.0f)

        if (targetAddress.isNotEmpty()) {
            targetDevice = bluetoothAdapter.getRemoteDevice(targetAddress)
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("airmouse_proximity", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("device_mac", targetAddress)
            putFloat("near_threshold", nearThreshold)
            putFloat("far_threshold", farThreshold)
            apply()
        }
    }

    @SuppressLint("MissingPermission")
    fun startProximity() {
        if (isRunning) return
        if (targetDevice == null) {
            Log.e(TAG, "No target device configured")
            stopSelf()
            return
        }

        isRunning = true
        startRssiMonitoring()
        updateNotification("Active", "Monitoring distance to ${targetDevice?.name ?: "device"}")
        Log.i(TAG, "Proximity monitoring started for ${targetDevice?.address}")
    }

    @SuppressLint("MissingPermission")
    private fun startRssiMonitoring() {
        serviceScope.launch {
            while (isRunning) {
                val distance = estimateDistance()
                val wasNear = isNear
                isNear = if (wasNear) {
                    distance < farThreshold
                } else {
                    distance < nearThreshold
                }

                lastKnownDistance = distance

                if (wasNear != isNear) {
                    onProximityChanged(isNear, distance)
                }

                updateNotification(
                    if (isNear) "Near" else "Far",
                    String.format("Distance: %.1fm", distance)
                )

                delay(1000)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun estimateDistance(): Float {
        if (targetDevice == null) return 5.0f

        return try {
            // In real implementation, read RSSI from BluetoothDevice
            // For now, use a simulation
            val rssi = getCurrentRssi()
            currentRssi = rssi
            calculateDistanceFromRssi(rssi)
        } catch (e: Exception) {
            Log.w(TAG, "RSSI estimation failed", e)
            5.0f
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentRssi(): Int {
        // This is a placeholder. Real implementation would require BluetoothGatt
        // For demo purposes, return a simulated RSSI
        return (-80..-30).random()
    }

    private fun calculateDistanceFromRssi(rssi: Int): Float {
        // Path loss model: distance = 10^((TxPower - RSSI) / (10 * n))
        val txPower = -59 // Reference RSSI at 1 meter
        val n = 2.0 // Path loss exponent
        val ratio = (txPower - rssi) / (10 * n)
        val distance = 10.0.pow(ratio)
        return distance.toFloat().coerceIn(0.5f, 10.0f)
    }

    private fun onProximityChanged(near: Boolean, distance: Float) {
        Log.i(TAG, "Proximity changed: near=$near, distance=${distance}m")

        // Broadcast intent for UI updates
        val intent = Intent(BROADCAST_PROXIMITY_CHANGE).apply {
            putExtra(EXTRA_IS_NEAR, near)
            putExtra(EXTRA_DISTANCE, distance)
        }
        sendBroadcast(intent)

        // Send command to server
        sendProximityCommand(near, distance)
    }

    private fun sendProximityCommand(isNear: Boolean, distance: Float) {
        // Send via WebSocket/TCP
        val command = if (isNear) "unlock" else "lock"
        // WebSocketManager.sendCommand(command)
    }

    fun stopProximity() {
        isRunning = false
        updateNotification("Stopped", "Proximity monitoring stopped")
        Log.i(TAG, "Proximity monitoring stopped")
    }

    fun updateConfig(deviceAddress: String, near: Float, far: Float) {
        targetAddress = deviceAddress
        nearThreshold = near
        farThreshold = far
        targetDevice = if (deviceAddress.isNotEmpty()) {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } else null
        saveSettings()

        if (isRunning) {
            stopProximity()
            startProximity()
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startProximity()
            ACTION_STOP -> stopProximity()
            ACTION_UPDATE_CONFIG -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: ""
                val near = intent.getFloatExtra(EXTRA_NEAR_THRESHOLD, nearThreshold)
                val far = intent.getFloatExtra(EXTRA_FAR_THRESHOLD, farThreshold)
                updateConfig(deviceAddress, near, far)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        Log.i(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}