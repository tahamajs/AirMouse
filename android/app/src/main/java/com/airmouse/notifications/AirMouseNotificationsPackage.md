# 📘 Air Mouse Notifications Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.notifications` package provides a **centralized notification management system** for the Air Mouse application. It handles all notification channels, alerts, and user-facing messages with proper Android integration.

```
com.airmouse.notifications/
└── NotificationManager.kt          # Centralized notification management
```

---

## 🔔 NotificationManager

### Purpose
Manages all system notifications for the Air Mouse application, including connection status, errors, gestures, proximity alerts, and calibration progress.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Channel Creation** | Creates and manages notification channels for Android O+ |
| **Notification Display** | Shows various types of notifications (connection, error, gesture, etc.) |
| **Permission Handling** | Handles POST_NOTIFICATIONS permission (Android 13+) |
| **Channel Management** | Creates channels with appropriate importance levels |
| **Sound & Vibration** | Configurable sound and vibration for different notification types |
| **Notification Actions** | Supports action buttons for user interaction |

---

## 📦 Notification Channels

### Channel Configuration

| Channel ID | Name | Importance | Description |
|------------|------|------------|-------------|
| `connection_channel` | Connection Status | LOW | Connection status updates |
| `error_channel` | Errors | HIGH | Error notifications with sound/vibration |
| `general_channel` | General | DEFAULT | General notifications |
| `gesture_channel` | Gestures | DEFAULT | Gesture detection notifications |
| `proximity_channel` | Proximity | HIGH | Proximity lock/unlock events |
| `calibration_channel` | Calibration | DEFAULT | Calibration progress and status |

### Channel Creation

```kotlin
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
        ),
        createChannel(
            id = CHANNEL_GESTURE,
            name = "Gestures",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            description = "Gesture detection notifications",
            enableVibration = true
        ),
        createChannel(
            id = CHANNEL_PROXIMITY,
            name = "Proximity",
            importance = NotificationManager.IMPORTANCE_HIGH,
            description = "Proximity lock/unlock events",
            enableVibration = true,
            enableSound = true
        ),
        createChannel(
            id = CHANNEL_CALIBRATION,
            name = "Calibration",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            description = "Calibration progress and status"
        )
    )
}
```

---

## 📋 Notification IDs

| ID | Constant | Purpose |
|----|----------|---------|
| 1001 | `NOTIFICATION_CONNECTED` | Connection established |
| 1002 | `NOTIFICATION_DISCONNECTED` | Connection lost |
| 1006 | `NOTIFICATION_ERROR` | Error occurred |
| 1007 | `NOTIFICATION_INFO` | General info |
| 1011 | `NOTIFICATION_DISCOVERY` | Server discovered |
| 1012 | `NOTIFICATION_GESTURE` | Gesture detected |
| 1013 | `NOTIFICATION_PROXIMITY` | Proximity changed |
| 1014 | `NOTIFICATION_CALIBRATION` | Calibration status |
| 1015 | `NOTIFICATION_GENERIC` | Generic notification |

---

## 📱 Notification Types

### 1. Connection Notifications

#### Connected Notification
```kotlin
fun showConnectedNotification(serverName: String) {
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
    
    notificationManagerCompat.notify(NOTIFICATION_CONNECTED, notification)
}
```

#### Disconnected Notification
```kotlin
fun showDisconnectedNotification(reason: String? = null) {
    val message = reason ?: "Waiting for approval."
    val notification = createBaseNotification(
        channelId = CHANNEL_CONNECTION,
        title = "Waiting for approval",
        message = message,
        icon = R.drawable.ic_disconnected,
        priority = PRIORITY_DEFAULT
    ).build()
    
    notificationManagerCompat.notify(NOTIFICATION_DISCONNECTED, notification)
}
```

#### Connection Pending Notification
```kotlin
fun showConnectionPendingNotification(serverName: String? = null) {
    val message = if (serverName.isNullOrBlank()) {
        "Waiting for approval."
    } else {
        "Waiting for $serverName to approve the session."
    }
    
    val notification = createBaseNotification(...)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(...)
        .build()
    
    notificationManagerCompat.notify(NOTIFICATION_INFO, notification)
}
```

### 2. Discovery Notifications

```kotlin
fun showDiscoveryNotification(serverName: String, ip: String, port: Int, protocol: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("open_network_discovery", true)
    }
    val notification = createBaseNotification(
        channelId = CHANNEL_GENERAL,
        title = "Nearby server found",
        message = "$serverName is available at $ip:$port using $protocol. Tap to open the discovery screen.",
        icon = android.R.drawable.ic_menu_search,
        priority = PRIORITY_DEFAULT,
        intent = intent
    ).build()
    
    notificationManagerCompat.notify(NOTIFICATION_DISCOVERY, notification)
}
```

### 3. Gesture Notifications

```kotlin
fun showGestureNotification(gesture: String, confidence: Float) {
    val message = String.format("Gesture '%s' detected (confidence: %.0f%%)", gesture, confidence * 100)
    val notification = createBaseNotification(
        channelId = CHANNEL_GESTURE,
        title = "Gesture Detected",
        message = message,
        icon = R.drawable.ic_gesture,
        priority = PRIORITY_DEFAULT
    ).build()
    
    notificationManagerCompat.notify(NOTIFICATION_GESTURE, notification)
}
```

### 4. Proximity Notifications

```kotlin
fun showProximityNotification(isNear: Boolean, distance: Float) {
    val title = if (isNear) "Device Near" else "Device Far"
    val message = String.format("Distance: %.2f meters", distance)
    val notification = createBaseNotification(
        channelId = CHANNEL_PROXIMITY,
        title = title,
        message = message,
        icon = R.drawable.ic_proximity,
        priority = PRIORITY_HIGH
    ).build()
    
    notificationManagerCompat.notify(NOTIFICATION_PROXIMITY, notification)
}
```

### 5. Calibration Notifications

```kotlin
fun showCalibrationNotification(status: String, progress: Int) {
    val message = String.format("Progress: %d%% – %s", progress, status)
    val notification = createBaseNotification(
        channelId = CHANNEL_CALIBRATION,
        title = "Calibration Status",
        message = message,
        icon = R.drawable.ic_calibration,
        priority = PRIORITY_DEFAULT
    ).apply {
        setProgress(100, progress, progress < 0)
    }.build()
    
    notificationManagerCompat.notify(NOTIFICATION_CALIBRATION, notification)
}
```

### 6. Info Notifications

```kotlin
fun showInfoNotification(title: String, message: String, id: Int = NOTIFICATION_GENERIC) {
    val notification = createBaseNotification(
        channelId = CHANNEL_GENERAL,
        title = title,
        message = message,
        icon = R.drawable.ic_info,
        priority = PRIORITY_DEFAULT
    ).build()
    
    notificationManagerCompat.notify(id, notification)
}
```

---

## 🎨 Notification Builder

### Base Notification Builder

```kotlin
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
```

### Channel Builder

```kotlin
private fun createChannel(
    id: String,
    name: String,
    importance: Int,
    description: String,
    enableVibration: Boolean = true,
    enableSound: Boolean = true,
    enableLights: Boolean = false,
    notificationLightColor: Int = 0
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

        if (enableLights) {
            enableLights(true)
            lightColor = notificationLightColor
        }

        if (importance == NotificationManager.IMPORTANCE_HIGH) {
            setBypassDnd(true)
        }

        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        setShowBadge(true)
    }
}
```

---

## 🔧 Permission Handling

### Android 13+ Permission

```kotlin
private val canPostNotifications: Boolean
    get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
```

### Safe Notification Display

```kotlin
try {
    notificationManagerCompat.notify(id, notification)
} catch (e: SecurityException) {
    // Permission not granted – ignore
    LogManager.warn("Cannot post notification: ${e.message}", TAG)
}
```

---

## 📊 Notification Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      NOTIFICATION FLOW                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. Event occurs (connection, gesture, proximity, etc.)               │
│         │                                                               │
│         ▼                                                               │
│  2. NotificationManager.showXxxNotification() called                  │
│         │                                                               │
│         ▼                                                               │
│  3. Check notification permission                                      │
│         │                                                               │
│         ├─── DENIED ───> Silently fail                                │
│         │                                                               │
│         └─── GRANTED ───> Continue                                    │
│                                 │                                       │
│                                 ▼                                       │
│  4. Create PendingIntent (if action required)                         │
│         │                                                               │
│         ▼                                                               │
│  5. Build notification with appropriate channel                       │
│         │                                                               │
│         ▼                                                               │
│  6. Show notification with NotificationManagerCompat                 │
│         │                                                               │
│         ▼                                                               │
│  7. User taps notification → Intent → Activity                       │
│         │                                                               │
│         ▼                                                               │
│  8. User dismisses → Notification removed                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Notification Priority Levels

| Priority | Value | Use Case |
|----------|-------|----------|
| `PRIORITY_LOW` | -1 | Connection status, general info |
| `PRIORITY_DEFAULT` | 0 | Gesture notifications, calibration |
| `PRIORITY_HIGH` | 1 | Errors, proximity alerts |

---

## 🎯 Usage Examples

### In HomeViewModel

```kotlin
@Inject
lateinit var notificationManager: NotificationManager

fun onConnectionEstablished(serverName: String) {
    notificationManager.showConnectedNotification(serverName)
}

fun onConnectionFailed(error: String) {
    notificationManager.showConnectionErrorNotification(error)
}

fun onGestureDetected(gesture: String, confidence: Float) {
    notificationManager.showGestureNotification(gesture, confidence)
}
```

### In ProximityViewModel

```kotlin
fun onProximityChanged(isNear: Boolean, distance: Float) {
    notificationManager.showProximityNotification(isNear, distance)
}
```

### In CalibrationViewModel

```kotlin
fun onCalibrationProgress(status: String, progress: Int) {
    notificationManager.showCalibrationNotification(status, progress)
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Centralized Management** | Single point of control for all notifications |
| **Channel Separation** | Different channels for different notification types |
| **Permission Handling** | Graceful handling of missing permissions |
| **Sound & Vibration** | Configurable per notification type |
| **Action Support** | Intent-based actions for user interaction |
| **Rich Content** | BigText style for detailed messages |
| **DND Bypass** | High priority notifications bypass Do Not Disturb |
| **Badge Support** | Notification badges for launcher icons |

---

**The NotificationManager provides a complete, centralized system for managing all notifications in the Air Mouse application, ensuring a consistent and professional user experience.**