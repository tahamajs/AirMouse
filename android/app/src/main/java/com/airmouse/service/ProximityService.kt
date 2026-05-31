// app/src/main/java/com/airmouse/service/ProximityService.kt
package com.airmouse.service

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.pow

@AndroidEntryPoint
class ProximityService : Service() {

    @Inject lateinit var prefs: PreferencesManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false
    private var lastDistance = 3.0f
    private var isNear = false

    companion object {
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "proximity_service_channel"
        private const val TX_POWER = -59
        private const val ENV_FACTOR = 2.5

        fun start(context: Context) {
            val intent = Intent(context, ProximityService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProximityService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proximity Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Proximity Lock")
            .setContentText("Monitoring distance to PC...")
            .setSmallIcon(R.drawable.ic_proximity)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startMonitoring() {
        if (isRunning) return
        isRunning = true
        serviceScope.launch {
            while (isRunning) {
                val distance = estimateDistance()
                updateNearState(distance)
                sendProximityUpdate(distance, isNear)
                delay(1000)
            }
        }
    }

    fun stopMonitoring() {
        isRunning = false
    }

    private suspend fun estimateDistance(): Float = withContext(Dispatchers.IO) {
        val serverMac = prefs.getServerMac()
        if (serverMac.isBlank()) return@withContext 5.0f
        try {
            val device = bluetoothAdapter.getRemoteDevice(serverMac)
            // In real implementation, read RSSI from device
            val rssi = -60 // placeholder
            val ratio = (TX_POWER - rssi) / (10.0 * ENV_FACTOR)
            (10.0.pow(ratio)).toFloat().coerceIn(0.5f, 10.0f)
        } catch (e: Exception) {
            5.0f
        }
    }

    private fun updateNearState(distance: Float) {
        val nearThreshold = prefs.getNearThreshold()
        val farThreshold = prefs.getFarThreshold()
        val wasNear = isNear
        isNear = if (wasNear) distance < farThreshold else distance < nearThreshold
        if (wasNear != isNear) {
            // Notify lock/unlock
        }
    }

    private fun sendProximityUpdate(distance: Float, near: Boolean) {
        // Send via WebSocket/TCP to server
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_MONITORING") startMonitoring()
        else if (intent?.action == "STOP_MONITORING") stopMonitoring()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}