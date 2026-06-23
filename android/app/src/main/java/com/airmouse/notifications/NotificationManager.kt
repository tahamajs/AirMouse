
package com.airmouse.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.airmouse.R
import com.airmouse.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        
        const val CHANNEL_CONNECTION = "connection_channel"
        const val CHANNEL_ERROR = "error_channel"
        const val CHANNEL_GENERAL = "general_channel"

        
        const val NOTIFICATION_CONNECTED = 1001
        const val NOTIFICATION_DISCONNECTED = 1002
        const val NOTIFICATION_ERROR = 1006
        const val NOTIFICATION_INFO = 1007
        const val NOTIFICATION_DISCOVERY = 1011

        
        const val PRIORITY_LOW = NotificationCompat.PRIORITY_LOW
        const val PRIORITY_DEFAULT = NotificationCompat.PRIORITY_DEFAULT
        const val PRIORITY_HIGH = NotificationCompat.PRIORITY_HIGH
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationManagerCompat = NotificationManagerCompat.from(context)

    private val canPostNotifications: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    init {
        createNotificationChannels()
    }

    

    private fun createNotificationChannels() {
        val channels = listOf(
            createChannel(
                id = CHANNEL_CONNECTION,
                name = "Connection Status",
                importance = NotificationManager.IMPORTANCE_LOW,
                description = "Connection status updates"
            ),
            createChannel(
                id = CHANNEL_ERROR,
                name = "Errors",
                importance = NotificationManager.IMPORTANCE_HIGH,
                description = "Error notifications",
                enableVibration = true,
                enableSound = true
            ),
            createChannel(
                id = CHANNEL_GENERAL,
                name = "General",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
                description = "General notifications"
            )
        )

        channels.forEach { notificationManager.createNotificationChannel(it) }
    }

    @Suppress("SameParameterValue")
    private fun createChannel(
        id: String,
        name: String,
        importance: Int,
        description: String,
        enableVibration: Boolean = true,
        enableSound: Boolean = true
    ): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            this.description = description

            if (enableSound) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }

            if (enableVibration) {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 200)
            }

            
            if (importance == NotificationManager.IMPORTANCE_HIGH) {
                setBypassDnd(true)
            }

            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            setShowBadge(true)
        }
    }

    

    private fun createBaseNotification(
        channelId: String,
        title: String,
        message: String,
        icon: Int,
        priority: Int = PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        intent: Intent? = null
    ): NotificationCompat.Builder {
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                context, 0, it.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setPriority(priority)
            .setAutoCancel(autoCancel)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setColor(context.getColor(R.color.primary))
    }

    

    fun showConnectedNotification(serverName: String) {
        if (!canPostNotifications) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_status_center", true)
        }
        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "Connected",
            message = "Approved and connected to $serverName.",
            icon = R.drawable.ic_connected,
            priority = PRIORITY_DEFAULT,
            intent = intent
        ).build()

        try {
            notificationManagerCompat.notify(NOTIFICATION_CONNECTED, notification)
        } catch (e: SecurityException) {
            
        }
    }

    fun showDiscoveryNotification(serverName: String, ip: String, port: Int, protocol: String) {
        if (!canPostNotifications) return
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_network_discovery", true)
        }
        val notification = createBaseNotification(
            channelId = CHANNEL_GENERAL,
            title = "Nearby server found",
            message = "$serverName is available at $ip:$port using $protocol. Tap to open the discovery screen.",
            icon = R.drawable.ic_connected,
            priority = PRIORITY_DEFAULT,
            intent = intent
        ).build()

        try {
            notificationManagerCompat.notify(NOTIFICATION_DISCOVERY, notification)
        } catch (e: SecurityException) {
            
        }
    }

    fun showDisconnectedNotification(reason: String? = null) {
        if (!canPostNotifications) return
        val message = reason ?: "Waiting for approval."
        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "Waiting for approval",
            message = message,
            icon = R.drawable.ic_disconnected,
            priority = PRIORITY_DEFAULT
        ).build()

        try {
            notificationManagerCompat.notify(NOTIFICATION_DISCONNECTED, notification)
        } catch (e: SecurityException) {
            
        }
    }

    fun showConnectionPendingNotification(serverName: String? = null) {
        if (!canPostNotifications) return
        val message = if (serverName.isNullOrBlank()) {
            "Waiting for approval."
        } else {
            "Waiting for $serverName to approve the session."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_status_center", true)
            putExtra("connection_pending", true)
        }

        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "Waiting for approval",
            message = message,
            icon = R.drawable.ic_reconnecting,
            priority = PRIORITY_LOW
        ).apply {
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(
                0,
                "Open app",
                PendingIntent.getActivity(
                    context,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    2,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }.build()

        try {
            notificationManagerCompat.notify(NOTIFICATION_INFO, notification)
        } catch (e: SecurityException) {
            
        }
    }

    fun showConnectionErrorNotification(error: String) {
        if (!canPostNotifications) return
        val notification = createBaseNotification(
            channelId = CHANNEL_ERROR,
            title = "Waiting for approval",
            message = error,
            icon = R.drawable.ic_error,
            priority = PRIORITY_HIGH
        ).build()

        try {
            notificationManagerCompat.notify(NOTIFICATION_ERROR, notification)
        } catch (e: SecurityException) {
            
        }
    }
}
