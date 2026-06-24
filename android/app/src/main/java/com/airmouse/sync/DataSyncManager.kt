package com.airmouse.sync

import android.util.Log
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.data.datasource.local.IProfileDataSource
import com.airmouse.data.datasource.local.IStatisticsDataSource
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.GestureTemplate
import com.airmouse.domain.model.ProfileModel
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class DataSyncManager @Inject constructor(
    @ApplicationContext private val context: Context, // Keep if needed, but we'll remove usage
    private val prefs: PreferencesManager, // Keep if needed, but we'll remove usage
    private val calibrationDataSource: ICalibrationDataSource,
    private val gestureDataSource: IGestureDataSource,
    private val statisticsDataSource: IStatisticsDataSource,
    private val profileDataSource: IProfileDataSource,
    private val connectionManager: ConnectionManager
) {
    companion object {
        private const val TAG = "DataSyncManager"
        private const val SYNC_TIMEOUT_MS = 30000L // not used but kept for potential
        private const val DEFAULT_SYNC_INTERVAL_MS = 300000L // 5 minutes
    }

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _isSyncing = AtomicBoolean(false)
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    enum class SyncState {
        IDLE, SYNCING, COMPLETED, ERROR
    }

    data class SyncResult(
        val success: Boolean,
        val itemsSynced: Int,
        val errors: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis()
    )

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

    fun stopAutoSync() {
        syncJob?.cancel()
        syncJob = null
        Log.i(TAG, "Auto-sync stopped")
    }

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

    fun cancelSync() {
        _isSyncing.set(false)
        _syncState.value = SyncState.IDLE
        Log.i(TAG, "Sync cancelled")
    }

    private suspend fun syncCalibration(errors: MutableList<String>): Int {
        return try {
            val localData = calibrationDataSource.getCalibrationData()
            if (localData == null) {
                Log.d(TAG, "No local calibration data to sync")
                return 0
            }

            val request = JSONObject().apply {
                put("type", "sync")
                put("action", "calibration")
                put("payload", localData.toJson())
                put("id", UUID.randomUUID().toString())
            }.toString()

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

            val request = JSONObject().apply {
                put("type", "sync")
                put("action", "gestures")
                put("payload", array)
                put("id", UUID.randomUUID().toString())
            }.toString()

            val response = sendSyncRequest(request) ?: throw Exception("No response from server")

            if (response.optBoolean("success", false)) {
                val remoteArray = response.optJSONArray("data")
                if (remoteArray != null) {
                    val remoteTemplates = mutableListOf<GestureTemplate>()
                    for (i in 0 until remoteArray.length()) {
                        remoteArray.optJSONObject(i)?.let {
                            GestureTemplate.fromJson(it)?.let { template ->
                                remoteTemplates.add(template)
                            }
                        }
                    }
                    // Merge: remote wins on conflict
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

    private suspend fun syncStatistics(errors: MutableList<String>): Int {
        return try {
            val stats = statisticsDataSource.getStatistics()
            if (stats == null) {
                Log.d(TAG, "No local statistics to sync")
                return 0
            }

            val request = JSONObject().apply {
                put("type", "sync")
                put("action", "statistics")
                put("payload", stats.toJson())
                put("id", UUID.randomUUID().toString())
            }.toString()

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

    private suspend fun syncProfile(errors: MutableList<String>): Int {
        return try {
            val profile = profileDataSource.getProfile()
            if (profile == null) {
                Log.d(TAG, "No local profile to sync")
                return 0
            }

            val request = JSONObject().apply {
                put("type", "sync")
                put("action", "profile")
                put("payload", profile.toJson())
                put("id", UUID.randomUUID().toString())
            }.toString()

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

    private fun mergeGestures(local: List<GestureTemplate>, remote: List<GestureTemplate>) {
        val merged = local.toMutableList()
        remote.forEach { remoteTemplate ->
            val existing = merged.find { it.id == remoteTemplate.id }
            if (existing != null) {
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

    fun cleanup() {
        stopAutoSync()
        scope.cancel()
        Log.i(TAG, "DataSyncManager cleaned up")
    }

    // These are just placeholders; we already have toJson/fromJson in the model classes.
    // No need to define them here.
}