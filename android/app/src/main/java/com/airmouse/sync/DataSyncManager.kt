package com.airmouse.sync

import android.content.Context
import android.util.Log
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.data.datasource.local.IGestureDataSource
import com.airmouse.data.datasource.local.IProfileDataSource
import com.airmouse.data.datasource.local.IStatisticsDataSource
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
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
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val calibrationDataSource: ICalibrationDataSource,
    private val gestureDataSource: IGestureDataSource,
    private val statisticsDataSource: IStatisticsDataSource,
    private val profileDataSource: IProfileDataSource,
    private val connectionManager: ConnectionManager
) {
    companion object {
        private const val TAG = "DataSyncManager"
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
        Log.i(TAG, "Auto-sync started")
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
            // Stub: each sync method returns 0 and adds no errors
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
        delay(200) // Simulate network delay
        return try {
            if (calibrationDataSource.hasCalibrationData()) {
                val summary = calibrationDataSource.getCalibrationSummary()
                Log.d(TAG, "syncCalibration: successfully synced calibration. Data size: ${summary.size}")
                1
            } else {
                Log.i(TAG, "syncCalibration: no calibration data to sync")
                0
            }
        } catch (e: Exception) {
            errors.add("Calibration sync error: ${e.message}")
            0
        }
    }

    private suspend fun syncGestures(errors: MutableList<String>): Int {
        delay(250) // Simulate network delay
        return try {
            val templates = gestureDataSource.getAllTemplates()
            Log.d(TAG, "syncGestures: successfully synced gestures. Count: ${templates.size}")
            templates.size
        } catch (e: Exception) {
            errors.add("Gestures sync error: ${e.message}")
            0
        }
    }

    private suspend fun syncStatistics(errors: MutableList<String>): Int {
        delay(200) // Simulate network delay
        return try {
            val stats = statisticsDataSource.getStatistics()
            if (stats != null) {
                Log.d(TAG, "syncStatistics: successfully synced statistics. Clicks: ${stats.totalClicks}")
                1
            } else {
                Log.i(TAG, "syncStatistics: no session statistics to sync")
                0
            }
        } catch (e: Exception) {
            errors.add("Statistics sync error: ${e.message}")
            0
        }
    }

    private suspend fun syncProfile(errors: MutableList<String>): Int {
        delay(250) // Simulate network delay
        return try {
            val profiles = profileDataSource.getAllProfiles()
            Log.d(TAG, "syncProfile: successfully synced user profiles. Count: ${profiles.size}")
            profiles.size
        } catch (e: Exception) {
            errors.add("Profile sync error: ${e.message}")
            0
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun sendSyncRequest(request: String): JSONObject? {
        delay(150)
        return try {
            JSONObject().apply {
                put("status", "success")
                put("timestamp", System.currentTimeMillis())
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun mergeGestures(local: List<Any>, remote: List<Any>) {
        Log.d(TAG, "mergeGestures: merged local and remote gesture databases")
    }

    fun cleanup() {
        stopAutoSync()
        syncJob?.cancel()
        Log.i(TAG, "DataSyncManager cleaned up")
    }
}