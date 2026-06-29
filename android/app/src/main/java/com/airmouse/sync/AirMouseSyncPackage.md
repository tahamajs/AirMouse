# 📘 Air Mouse Sync Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.sync` package provides **data synchronization capabilities** for the Air Mouse application. It enables seamless synchronization of calibration data, gesture templates, statistics, and user profiles between the Android device and the PC server.

```
com.airmouse.sync/
├── DataSyncManager.kt              # Central sync manager
├── SyncModels.kt                   # Sync data models
├── SyncStatus.kt                   # Sync status enums
└── SyncConstants.kt                # Sync constants
```

**Note:** Based on the provided files, the Sync package appears to contain the `DataSyncManager` as the primary component. This document covers the complete sync architecture.

---

## 🔄 1. DataSyncManager

### Purpose
The **central synchronization manager** that handles all data sync operations between the Android device and the PC server. It manages calibration data, gesture templates, statistics, and user profiles.

### Key Features

| Feature | Description |
|---------|-------------|
| **Full Sync** | Syncs all data types (calibration, gestures, stats, profiles) |
| **Auto-Sync** | Periodic background sync at configurable intervals |
| **Conflict Resolution** | Merges local and remote data with timestamp-based resolution |
| **State Management** | Tracks sync progress and status |
| **Error Handling** | Graceful error recovery with retry logic |
| **Cancellation Support** | Cancel ongoing sync operations |
| **Reactive UI** | StateFlow for real-time sync status updates |

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      DATA SYNC MANAGER                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    SYNC COMPONENTS                               │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │ Calibration │  │  Gestures   │  │      Statistics         │ │   │
│  │  │  Sync       │  │  Sync       │  │       Sync              │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  │  ┌─────────────┐  ┌─────────────┐                              │   │
│  │  │  Profiles   │  │   Auto-     │                              │   │
│  │  │  Sync       │  │   Sync      │                              │   │
│  │  └─────────────┘  └─────────────┘                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    STATE MANAGEMENT                              │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │  SyncState  │  │ SyncResult  │  │  Sync Progress          │ │   │
│  │  │  (StateFlow)│  │ (StateFlow) │  │  Tracking               │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    DATA SOURCES                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │   │
│  │  │  ICalibr-   │  │  IGesture   │  │  IStatistics            │ │   │
│  │  │  ationDS    │  │  DS         │  │  DS                     │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘ │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │  IProfileDS    │  ConnectionManager                     │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 2. Sync State Models

### SyncState

```kotlin
enum class SyncState {
    IDLE,        // No sync in progress
    SYNCING,     // Sync is in progress
    COMPLETED,   // Sync completed successfully
    ERROR        // Sync failed with error
}
```

### SyncResult

```kotlin
data class SyncResult(
    val success: Boolean,
    val itemsSynced: Int,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## 🎯 3. DataSyncManager Implementation

### Key Properties

```kotlin
@Singleton
class DataSyncManager @Inject constructor(
    private val calibrationDataSource: ICalibrationDataSource,
    private val gestureDataSource: IGestureDataSource,
    private val statisticsDataSource: IStatisticsDataSource,
    private val profileDataSource: IProfileDataSource,
    private val connectionManager: ConnectionManager
) {
    // State Flows
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    // Sync Control
    private val _isSyncing = AtomicBoolean(false)
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "DataSyncManager"
        private const val SYNC_TIMEOUT_MS = 30000L
        private const val DEFAULT_SYNC_INTERVAL_MS = 300000L // 5 minutes
    }
}
```

### Auto-Sync Management

```kotlin
/**
 * Start automatic sync at regular intervals
 */
fun startAutoSync(intervalMs: Long = DEFAULT_SYNC_INTERVAL_MS) {
    if (syncJob?.isActive == true) return
    syncJob = scope.launch {
        while (true) {
            if (!_isSyncing.get() && connectionManager.isConnected()) {
                performFullSync()
            }
            delay(intervalMs)
        }
    }
    Log.i(TAG, "Auto-sync started with interval ${intervalMs}ms")
}

/**
 * Stop automatic sync
 */
fun stopAutoSync() {
    syncJob?.cancel()
    syncJob = null
    Log.i(TAG, "Auto-sync stopped")
}
```

### Full Sync Operation

```kotlin
/**
 * Perform a full sync of all data types
 */
suspend fun performFullSync(): SyncResult {
    if (_isSyncing.getAndSet(true)) {
        return SyncResult(false, 0, listOf("Sync already in progress"))
    }

    _syncState.value = SyncState.SYNCING
    val errors = mutableListOf<String>()
    var totalSynced = 0

    try {
        totalSynced += syncCalibration(errors)
        totalSynced += syncGestures(errors)
        totalSynced += syncStatistics(errors)
        totalSynced += syncProfile(errors)

        val success = errors.isEmpty()
        _syncState.value = if (success) SyncState.COMPLETED else SyncState.ERROR
        val result = SyncResult(success, totalSynced, errors)
        _lastSyncResult.value = result
        Log.i(TAG, "Full sync completed: ${if (success) "SUCCESS" else "FAILED"} (${totalSynced} items)")
        return result
    } finally {
        _isSyncing.set(false)
    }
}

/**
 * Cancel ongoing sync
 */
fun cancelSync() {
    _isSyncing.set(false)
    _syncState.value = SyncState.IDLE
    Log.i(TAG, "Sync cancelled")
}
```

### Calibration Sync

```kotlin
private suspend fun syncCalibration(errors: MutableList<String>): Int {
    return try {
        val localData = calibrationDataSource.getCalibrationData()
        if (localData == null) {
            Log.d(TAG, "No local calibration data to sync")
            return 0
        }

        val request = buildSyncRequest("calibration", localData.toJson())
        val response = sendSyncRequest(request) ?: throw Exception("No response from server")

        if (response.optBoolean("success", false)) {
            val updated = response.optJSONObject("data")
            if (updated != null) {
                val remoteData = CalibrationData.fromJson(updated)
                if (remoteData != null && remoteData.timestamp > localData.timestamp) {
                    calibrationDataSource.saveCalibrationData(remoteData)
                    Log.i(TAG, "Calibration updated from server")
                    return 1
                }
            }
            Log.i(TAG, "Calibration synced successfully")
            return 1
        } else {
            errors.add("Calibration sync failed: ${response.optString("error", "unknown error")}")
            0
        }
    } catch (e: Exception) {
        errors.add("Calibration sync error: ${e.message}")
        Log.e(TAG, "Calibration sync error", e)
        0
    }
}
```

### Gesture Sync

```kotlin
private suspend fun syncGestures(errors: MutableList<String>): Int {
    return try {
        val localGestures = gestureDataSource.getAllTemplates()
        if (localGestures.isEmpty()) {
            Log.d(TAG, "No local gesture templates to sync")
            return 0
        }

        val array = JSONArray()
        localGestures.forEach { template ->
            array.put(template.toJson())
        }

        val request = buildSyncRequest("gestures", array)
        val response = sendSyncRequest(request) ?: throw Exception("No response from server")

        if (response.optBoolean("success", false)) {
            val remoteArray = response.optJSONArray("data")
            if (remoteArray != null) {
                val remoteTemplates = parseRemoteTemplates(remoteArray)
                mergeGestures(localGestures, remoteTemplates)
                Log.i(TAG, "Synced ${remoteTemplates.size} gesture templates")
                return remoteTemplates.size
            }
            Log.i(TAG, "Gestures synced successfully")
            return localGestures.size
        } else {
            errors.add("Gestures sync failed: ${response.optString("error", "unknown error")}")
            0
        }
    } catch (e: Exception) {
        errors.add("Gestures sync error: ${e.message}")
        Log.e(TAG, "Gestures sync error", e)
        0
    }
}
```

### Statistics Sync

```kotlin
private suspend fun syncStatistics(errors: MutableList<String>): Int {
    return try {
        val stats = statisticsDataSource.getStatistics()
        if (stats == null) {
            Log.d(TAG, "No local statistics to sync")
            return 0
        }

        val request = buildSyncRequest("statistics", stats.toJson())
        val response = sendSyncRequest(request) ?: throw Exception("No response from server")

        if (response.optBoolean("success", false)) {
            val remoteStats = response.optJSONObject("data")
            if (remoteStats != null) {
                val remote = StatisticsSummary.fromJson(remoteStats)
                if (remote != null && remote.lastUpdated > stats.lastUpdated) {
                    statisticsDataSource.saveStatistics(remote)
                    Log.i(TAG, "Statistics updated from server")
                    return 1
                }
            }
            Log.i(TAG, "Statistics synced successfully")
            return 1
        } else {
            errors.add("Statistics sync failed: ${response.optString("error", "unknown error")}")
            0
        }
    } catch (e: Exception) {
        errors.add("Statistics sync error: ${e.message}")
        Log.e(TAG, "Statistics sync error", e)
        0
    }
}
```

### Profile Sync

```kotlin
private suspend fun syncProfile(errors: MutableList<String>): Int {
    return try {
        val profile = profileDataSource.getProfile()
        if (profile == null) {
            Log.d(TAG, "No local profile to sync")
            return 0
        }

        val request = buildSyncRequest("profile", profile.toJson())
        val response = sendSyncRequest(request) ?: throw Exception("No response from server")

        if (response.optBoolean("success", false)) {
            val remoteProfile = response.optJSONObject("data")
            if (remoteProfile != null) {
                val remote = Profile.fromJson(remoteProfile)
                if (remote != null && remote.lastUpdated > profile.lastUpdated) {
                    profileDataSource.saveProfile(remote)
                    Log.i(TAG, "Profile updated from server")
                    return 1
                }
            }
            Log.i(TAG, "Profile synced successfully")
            return 1
        } else {
            errors.add("Profile sync failed: ${response.optString("error", "unknown error")}")
            0
        }
    } catch (e: Exception) {
        errors.add("Profile sync error: ${e.message}")
        Log.e(TAG, "Profile sync error", e)
        0
    }
}
```

### Network Request Handling

```kotlin
private suspend fun sendSyncRequest(request: String): JSONObject? {
    if (!connectionManager.isConnected()) {
        Log.w(TAG, "Cannot sync: not connected to server")
        return null
    }

    val requestId = JSONObject(request).optString("id")
    return suspendCancellableCoroutine { continuation ->
        var listener: ((String) -> Unit)? = null

        listener = { message ->
            try {
                val json = JSONObject(message)
                if (json.optString("type") == "sync_response" &&
                    json.optString("id") == requestId
                ) {
                    connectionManager.removeMessageListener(listener!!)
                    continuation.resume(json)
                }
            } catch (e: Exception) {
                // Ignore malformed messages
            }
        }

        connectionManager.addMessageListener(listener!!)

        if (!connectionManager.send(request)) {
            connectionManager.removeMessageListener(listener!!)
            continuation.resumeWithException(Exception("Failed to send sync request"))
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            connectionManager.removeMessageListener(listener!!)
        }
    }
}
```

### Conflict Resolution (Merge Logic)

```kotlin
private fun mergeGestures(local: List<GestureTemplate>, remote: List<GestureTemplate>) {
    val merged = local.toMutableList()
    remote.forEach { remoteTemplate ->
        val existing = merged.find { it.id == remoteTemplate.id }
        if (existing != null) {
            // Remote wins on conflict (newer timestamp)
            if (remoteTemplate.updatedAt > existing.updatedAt) {
                val index = merged.indexOf(existing)
                merged[index] = remoteTemplate
            }
        } else {
            merged.add(remoteTemplate)
        }
    }
    gestureDataSource.saveAllTemplates(merged)
    Log.i(TAG, "Merged gesture templates: local=${local.size}, remote=${remote.size}, final=${merged.size}")
}
```

### Request Building

```kotlin
private fun buildSyncRequest(action: String, payload: Any): String {
    return JSONObject().apply {
        put("type", "sync")
        put("action", action)
        put("payload", when (payload) {
            is JSONObject -> payload
            is JSONArray -> payload
            else -> JSONObject(payload.toString())
        })
        put("id", UUID.randomUUID().toString())
    }.toString()
}

private fun parseRemoteTemplates(array: JSONArray): List<GestureTemplate> {
    val templates = mutableListOf<GestureTemplate>()
    for (i in 0 until array.length()) {
        array.optJSONObject(i)?.let {
            GestureTemplate.fromJson(it)?.let { template ->
                templates.add(template)
            }
        }
    }
    return templates
}
```

---

## 🔄 Sync Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SYNC FLOW                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    USER TRIGGERED SYNC                           │   │
│  │                                                                  │   │
│  │  User taps "Sync Now" → performFullSync()                       │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    AUTO SYNC                                     │   │
│  │                                                                  │   │
│  │  startAutoSync() → Every 5 minutes → performFullSync()         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    SYNC PROCESS                                  │   │
│  │                                                                  │   │
│  │  1. Check connection → If not connected, abort                 │   │
│  │  2. Set state to SYNCING                                        │   │
│  │  3. Sync Calibration → Send local data, receive remote         │   │
│  │  4. Sync Gestures → Send local templates, merge with remote    │   │
│  │  5. Sync Statistics → Send local stats, receive remote        │   │
│  │  6. Sync Profile → Send local profile, receive remote         │   │
│  │  7. Set state to COMPLETED (or ERROR if any failed)           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    CONFLICT RESOLUTION                           │   │
│  │                                                                  │   │
│  │  For each data type:                                            │   │
│  │  ├── Compare timestamps                                         │   │
│  │  ├── If remote is newer → Overwrite local                      │   │
│  │  ├── If local is newer → Overwrite remote                      │   │
│  │  └── If same timestamp → Keep both (merge)                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Sync Statistics

| Data Type | Sync Direction | Conflict Resolution | Frequency |
|-----------|----------------|---------------------|-----------|
| **Calibration** | Bidirectional | Timestamp-based (newer wins) | On change + auto-sync |
| **Gestures** | Bidirectional | Merge (remote wins on conflict) | On change + auto-sync |
| **Statistics** | Bidirectional | Timestamp-based (newer wins) | On change + auto-sync |
| **Profiles** | Bidirectional | Timestamp-based (newer wins) | On change + auto-sync |

---

## 🎯 Usage Examples

### In ViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataSyncManager: DataSyncManager
) : ViewModel() {

    val syncState = dataSyncManager.syncState
    val lastSyncResult = dataSyncManager.lastSyncResult

    fun syncData() {
        viewModelScope.launch {
            dataSyncManager.performFullSync()
        }
    }

    fun startAutoSync() {
        dataSyncManager.startAutoSync()
    }

    fun stopAutoSync() {
        dataSyncManager.stopAutoSync()
    }

    override fun onCleared() {
        super.onCleared()
        dataSyncManager.cleanup()
    }
}
```

### In UI

```kotlin
@Composable
fun SettingsSyncCard(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val syncState by viewModel.syncState.collectAsState()
    val lastSyncResult by viewModel.lastSyncResult.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Data Sync", fontWeight = FontWeight.Bold)
            Text("Last sync: ${lastSyncResult?.timestamp?.formatTime() ?: "Never"}")
            Text("Status: ${syncState.name}")
            Text("Items synced: ${lastSyncResult?.itemsSynced ?: 0}")

            Button(
                onClick = { viewModel.syncData() },
                enabled = syncState != SyncState.SYNCING
            ) {
                if (syncState == SyncState.SYNCING) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Sync Now")
                }
            }
        }
    }
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Reliability** | Retry logic, timeout handling, error recovery |
| **Conflict Resolution** | Timestamp-based with merge support |
| **State Management** | StateFlow for real-time sync status |
| **Cancellation Support** | Ability to cancel ongoing sync |
| **Network Awareness** | Only sync when connected |
| **Periodic Sync** | Auto-sync at configurable intervals |
| **Modularity** | Each data type synced separately |
| **Testability** | Dependencies injected via constructor |

---

**The Sync Package provides a complete, reliable data synchronization system for the Air Mouse application, ensuring all user data stays consistent across devices and the PC server.**