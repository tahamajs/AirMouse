package com.airmouse.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.MainActivity
import com.airmouse.utils.LogManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service to maintain AirMouse connection and sensor polling in background.
 * Manages WakeLock with a strict maximum timeout to prevent battery drain.
 */
@AndroidEntryPoint
class AirMouseService : Service() {

    @Inject
    lateinit var connectionManager: ConnectionManager

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceInternal()
        initWakeLock()
        observeConnectionLifecycle()
    }

    private fun startForegroundServiceInternal() {
        val notification = createNotification("AirMouse service active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AirMouse::ServiceWakeLock"
            ).apply {
                setReferenceCounted(false)
            }
            acquireWakeLock()
        } catch (e: Exception) {
            LogManager.error("Failed to initialize WakeLock in AirMouseService", e)
        }
    }

    /**
     * Acquires wake lock with strict maximum timeout (2 hours) to avoid battery drain.
     */
    private fun acquireWakeLock() {
        try {
            wakeLock?.let { wl ->
                if (!wl.isHeld) {
                    wl.acquire(MAX_WAKELOCK_TIMEOUT_MS)
                    LogManager.info("AirMouseService: WakeLock acquired with strict 2-hour timeout ($MAX_WAKELOCK_TIMEOUT_MS ms)", TAG)
                }
            }
        } catch (e: Exception) {
            LogManager.error("AirMouseService: Failed to acquire WakeLock", e)
        }
    }

    /**
     * Safely releases wake lock if held.
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let { wl ->
                if (wl.isHeld) {
                    wl.release()
                    LogManager.info("AirMouseService: WakeLock safely released", TAG)
                }
            }
        } catch (e: Exception) {
            LogManager.error("AirMouseService: Failed to release WakeLock", e)
        }
    }

    private fun observeConnectionLifecycle() {
        serviceScope.launch {
            connectionManager.connectionStatus.collectLatest { status ->
                when (status) {
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        acquireWakeLock()
                        updateNotification("Connected to PC")
                    }
                    ConnectionManager.ConnectionStatus.DISCONNECTED,
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        releaseWakeLock()
                        updateNotification("Disconnected")
                    }
                    else -> {
                        // Connecting or Reconnecting
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                releaseWakeLock()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                acquireWakeLock()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        wakeLock = null
        serviceScope.cancel()
        LogManager.info("AirMouseService destroyed and WakeLock released", TAG)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AirMouse Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps connection alive while AirMouse is running"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunchIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AirMouseService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AirMouse")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mouse)
            .setContentIntent(pendingLaunchIntent)
            .addAction(R.drawable.ic_mouse, "Stop", pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    companion object {
        private const val TAG = "AirMouseService"
        private const val CHANNEL_ID = "airmouse_background_channel"
        private const val NOTIFICATION_ID = 2001

        /** Strict maximum timeout for WakeLock (2 hours = 7,200,000 ms) */
        const val MAX_WAKELOCK_TIMEOUT_MS = 2 * 60 * 60 * 1000L

        const val ACTION_START = "com.airmouse.service.ACTION_START"
        const val ACTION_STOP = "com.airmouse.service.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AirMouseService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AirMouseService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }
}
