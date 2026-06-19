// app/src/main/java/com/airmouse/notifications/NotificationManager.kt
package com.airmouse.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.airmouse.R
import com.airmouse.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Channel IDs
        const val CHANNEL_CONNECTION = "connection_channel"
        const val CHANNEL_GESTURE = "gesture_channel"
        const val CHANNEL_PROXIMITY = "proximity_channel"
        const val CHANNEL_UPDATE = "update_channel"
        const val CHANNEL_FOREGROUND = "foreground_channel"
        const val CHANNEL_ERROR = "error_channel"
        const val CHANNEL_GENERAL = "general_channel"

        // Notification IDs
        const val NOTIFICATION_CONNECTED = 1001
        const val NOTIFICATION_DISCONNECTED = 1002
        const val NOTIFICATION_GESTURE = 1003
        const val NOTIFICATION_PROXIMITY = 1004
        const val NOTIFICATION_UPDATE = 1005
        const val NOTIFICATION_ERROR = 1006
        const val NOTIFICATION_INFO = 1007
        const val NOTIFICATION_FOREGROUND = 1008
        const val NOTIFICATION_WARNING = 1009
        const val NOTIFICATION_SUCCESS = 1010

        // Priority levels
        const val PRIORITY_LOW = NotificationCompat.PRIORITY_LOW
        const val PRIORITY_DEFAULT = NotificationCompat.PRIORITY_DEFAULT
        const val PRIORITY_HIGH = NotificationCompat.PRIORITY_HIGH
        const val PRIORITY_MAX = NotificationCompat.PRIORITY_MAX
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationManagerCompat = NotificationManagerCompat.from(context)

    private var lastNotificationId = NOTIFICATION_CONNECTED

    init {
        createNotificationChannels()
    }

    // ==================== Channel Creation ====================

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                createChannel(
                    CHANNEL_CONNECTION,
                    "Connection Status",
                    NotificationManager.IMPORTANCE_LOW,
                    "Connection status updates"
                ),
                createChannel(
                    CHANNEL_GESTURE,
                    "Gestures",
                    NotificationManager.IMPORTANCE_DEFAULT,
                    "Gesture recognition notifications"
                ),
                createChannel(
                    CHANNEL_PROXIMITY,
                    "Proximity",
                    NotificationManager.IMPORTANCE_HIGH,
                    "Proximity lock/unlock notifications",
                    true,
                    true
                ),
                createChannel(
                    CHANNEL_UPDATE,
                    "Updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                    "App update notifications"
                ),
                createChannel(
                    CHANNEL_FOREGROUND,
                    "Foreground Service",
                    NotificationManager.IMPORTANCE_LOW,
                    "Foreground service notifications",
                    false,
                    false
                ),
                createChannel(
                    CHANNEL_ERROR,
                    "Errors",
                    NotificationManager.IMPORTANCE_HIGH,
                    "Error notifications",
                    true,
                    true
                ),
                createChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT,
                    "General notifications"
                )
            )

            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Allow notification to bypass DND for high priority
                if (importance == NotificationManager.IMPORTANCE_HIGH) {
                    setBypassDnd(true)
                }
            }

            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            setShowBadge(true)
        }
    }

    // ==================== Base Notification Builder ====================

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
                context, 0, it,
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

    // ==================== Connection Notifications ====================

    fun showConnectedNotification(serverName: String) {
        val intent = Intent(context, MainActivity::class.java)
        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "✅ Connected to $serverName",
            message = "Air Mouse is ready for use",
            icon = R.drawable.ic_connected,
            priority = PRIORITY_LOW,
            intent = intent
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_CONNECTED, notification)
    }

    fun showDisconnectedNotification(reason: String? = null) {
        val message = reason ?: "Air Mouse connection lost"
        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "❌ Disconnected",
            message = message,
            icon = R.drawable.ic_disconnected,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_DISCONNECTED, notification)
    }

    fun showReconnectingNotification(attempt: Int, maxAttempts: Int) {
        val notification = createBaseNotification(
            channelId = CHANNEL_CONNECTION,
            title = "🔄 Reconnecting...",
            message = "Attempt $attempt of $maxAttempts",
            icon = R.drawable.ic_reconnecting,
            priority = PRIORITY_LOW
        ).apply {
            setProgress(maxAttempts, attempt, false)
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_DISCONNECTED, notification)
    }

    fun showConnectionErrorNotification(error: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_ERROR,
            title = "⚠️ Connection Error",
            message = error,
            icon = R.drawable.ic_error,
            priority = PRIORITY_HIGH
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_ERROR, notification)
    }

    // ==================== Gesture Notifications ====================

    fun showGestureNotification(gesture: String, confidence: Float) {
        val confidenceText = "Confidence: ${(confidence * 100).toInt()}%"
        val notification = createBaseNotification(
            channelId = CHANNEL_GESTURE,
            title = "✋ Gesture Detected",
            message = "$gesture ($confidenceText)",
            icon = R.drawable.ic_gesture,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_GESTURE, notification)
    }

    fun showGestureTrainingNotification(gestureName: String, progress: Int) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GESTURE,
            title = "🎯 Training Gesture",
            message = "Recording $gestureName",
            icon = R.drawable.ic_gesture_training,
            priority = PRIORITY_LOW
        ).apply {
            setProgress(100, progress, false)
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_GESTURE, notification)
    }

    fun showGestureTrainingComplete(gestureName: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GESTURE,
            title = "✅ Gesture Training Complete",
            message = "$gestureName trained successfully",
            icon = R.drawable.ic_gesture_trained,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_GESTURE, notification)
    }

    // ==================== Proximity Notifications ====================

    fun showProximityNotification(isNear: Boolean, distance: Float) {
        val (title, message) = if (isNear) {
            "🔓 Unlocked" to "Device unlocked - you're near (${distance}m)"
        } else {
            "🔒 Locked" to "Device locked - you walked away (${distance}m)"
        }

        val notification = createBaseNotification(
            channelId = CHANNEL_PROXIMITY,
            title = title,
            message = message,
            icon = if (isNear) R.drawable.ic_unlock else R.drawable.ic_lock,
            priority = PRIORITY_HIGH
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_PROXIMITY, notification)

        // Vibrate for proximity changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun showProximityCalibratingNotification() {
        val notification = createBaseNotification(
            channelId = CHANNEL_PROXIMITY,
            title = "📡 Calibrating Proximity",
            message = "Please follow the on-screen instructions",
            icon = R.drawable.ic_calibrate,
            priority = PRIORITY_LOW
        ).apply {
            setProgress(0, 0, true)
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_PROXIMITY, notification)
    }

    fun showProximityCalibrationComplete() {
        val notification = createBaseNotification(
            channelId = CHANNEL_PROXIMITY,
            title = "✅ Proximity Calibrated",
            message = "Proximity detection is now optimized",
            icon = R.drawable.ic_calibrated,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_PROXIMITY, notification)
    }

    // ==================== Update Notifications ====================

    fun showUpdateNotification(version: String, releaseNotes: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_update", true)
        }

        val message = releaseNotes?.take(100) ?: "Version $version is ready to install"
        val notification = createBaseNotification(
            channelId = CHANNEL_UPDATE,
            title = "📦 Update Available",
            message = "Version $version",
            icon = R.drawable.ic_update,
            priority = PRIORITY_DEFAULT,
            intent = intent
        ).apply {
            setStyle(NotificationCompat.BigTextStyle().bigText(message))
            addAction(
                R.drawable.ic_download,
                "Download",
                getUpdatePendingIntent("download")
            )
            addAction(
                R.drawable.ic_install,
                "Install Now",
                getUpdatePendingIntent("install")
            )
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_UPDATE, notification)
    }

    fun showDownloadProgressNotification(version: String, progress: Int, total: Long, downloaded: Long) {
        val notification = createBaseNotification(
            channelId = CHANNEL_UPDATE,
            title = "📥 Downloading Update",
            message = "Version $version - ${progress}%",
            icon = R.drawable.ic_downloading,
            priority = PRIORITY_LOW
        ).apply {
            setProgress(100, progress, false)
            setSubText("${formatFileSize(downloaded)} / ${formatFileSize(total)}")
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_UPDATE, notification)
    }

    fun showUpdateInstalledNotification(version: String) {
        val intent = Intent(context, MainActivity::class.java)
        val notification = createBaseNotification(
            channelId = CHANNEL_UPDATE,
            title = "✅ Update Installed",
            message = "Version $version is ready to use",
            icon = R.drawable.ic_installed,
            priority = PRIORITY_DEFAULT,
            intent = intent
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_UPDATE, notification)
    }

    // ==================== Foreground Service Notifications ====================

    fun showForegroundNotification(title: String = "Air Mouse Active", message: String = "Controlling cursor...") {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_service)
            .setPriority(PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManagerCompat.notify(NOTIFICATION_FOREGROUND, notification)
    }

    fun updateForegroundNotification(connectionStatus: String, deviceCount: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setContentTitle("Air Mouse Active")
            .setContentText("$connectionStatus | $deviceCount device(s)")
            .setSmallIcon(R.drawable.ic_service)
            .setPriority(PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManagerCompat.notify(NOTIFICATION_FOREGROUND, notification)
    }

    // ==================== Error & Info Notifications ====================

    fun showErrorNotification(title: String, message: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_ERROR,
            title = "❌ $title",
            message = message,
            icon = R.drawable.ic_error,
            priority = PRIORITY_HIGH
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_ERROR, notification)
    }

    fun showWarningNotification(title: String, message: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GENERAL,
            title = "⚠️ $title",
            message = message,
            icon = R.drawable.ic_warning,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_WARNING, notification)
    }

    fun showSuccessNotification(title: String, message: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GENERAL,
            title = "✅ $title",
            message = message,
            icon = R.drawable.ic_success,
            priority = PRIORITY_DEFAULT
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_SUCCESS, notification)
    }

    fun showInfoNotification(title: String, message: String) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GENERAL,
            title = "ℹ️ $title",
            message = message,
            icon = R.drawable.ic_info,
            priority = PRIORITY_LOW
        ).build()

        notificationManagerCompat.notify(NOTIFICATION_INFO, notification)
    }

    // ==================== Utility Methods ====================

    private fun getUpdatePendingIntent(action: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("update_action", action)
        }
        return PendingIntent.getActivity(
            context,
            if (action == "download") 1 else 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    fun cancelAllNotifications() {
        notificationManagerCompat.cancelAll()
    }

    fun cancelNotification(id: Int) {
        notificationManagerCompat.cancel(id)
    }

    fun areNotificationsEnabled(): Boolean {
        return notificationManagerCompat.areNotificationsEnabled()
    }

    fun getActiveNotifications(): Array<StatusBarNotification>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
        } else {
            null
        }
    }

    fun createNotificationGroup(groupKey: String, title: String, messages: List<Pair<String, String>>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setContentTitle(title)
                .setContentText("${messages.size} new messages")
                .setSmallIcon(R.drawable.ic_notification)

            val summaryNotification = builder.build()

            // Create individual notifications for each message
            messages.forEachIndexed { index, (title, message) ->
                val childBuilder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
                    .setGroup(groupKey)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setAutoCancel(true)

                notificationManagerCompat.notify(NOTIFICATION_INFO + index, childBuilder.build())
            }

            notificationManagerCompat.notify(NOTIFICATION_INFO + 100, summaryNotification)
        }
    }

    fun clearNotificationGroup(groupKey: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Find and cancel all notifications in the group
            val activeNotifications = getActiveNotifications()
            activeNotifications?.forEach { notification ->
                val extras = notification.notification.extras
                val notificationGroupKey = extras.getString(NotificationCompat.EXTRA_GROUP_KEY)
                if (notificationGroupKey == groupKey) {
                    notificationManagerCompat.cancel(notification.id)
                }
            }
        }
    }

    fun createProgressNotification(
        title: String,
        message: String,
        progress: Int,
        max: Int,
        indeterminate: Boolean = false
    ) {
        val notification = createBaseNotification(
            channelId = CHANNEL_GENERAL,
            title = title,
            message = message,
            icon = R.drawable.ic_progress,
            priority = PRIORITY_LOW
        ).apply {
            setProgress(max, progress, indeterminate)
        }.build()

        notificationManagerCompat.notify(NOTIFICATION_INFO, notification)
    }

    fun createCustomNotification(
        channelId: String,
        title: String,
        message: String,
        icon: Int,
        priority: Int = PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        actions: List<Pair<String, PendingIntent>> = emptyList(),
        largeIcon: Boolean = false,
        bigText: Boolean = false,
        progress: Pair<Int, Int>? = null
    ): Notification {
        val builder = createBaseNotification(
            channelId = channelId,
            title = title,
            message = message,
            icon = icon,
            priority = priority,
            autoCancel = autoCancel
        )

        if (bigText) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
        }

        if (largeIcon) {
            builder.setLargeIcon(android.graphics.BitmapFactory.decodeResource(
                context.resources, R.drawable.ic_launcher
            ))
        }

        progress?.let { (current, max) ->
            builder.setProgress(max, current, false)
        }

        actions.forEach { (label, pendingIntent) ->
            builder.addAction(0, label, pendingIntent)
        }

        return builder.build()
    }

    fun isNotificationIdle(id: Int): Boolean {
        return try {
            notificationManagerCompat.activeNotifications
                ?.none { it.id == id } == true
        } catch (e: Exception) {
            true
        }
    }

    fun getNextNotificationId(): Int {
        lastNotificationId++
        return lastNotificationId
    }
}