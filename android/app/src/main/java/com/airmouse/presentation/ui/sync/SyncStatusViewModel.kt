package com.airmouse.presentation.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.sync.DataSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncStatusUiState(
    val syncState: String = "IDLE",
    val lastResult: String = "Never",
    val itemsSynced: Int = 0,
    val errors: String = "",
    val autoSyncEnabled: Boolean = false
)

@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val dataSyncManager: DataSyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SyncStatusUiState())
    val uiState: StateFlow<SyncStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataSyncManager.syncState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(syncState = state.name)
            }
        }
        viewModelScope.launch {
            dataSyncManager.lastSyncResult.collectLatest { result ->
                _uiState.value = _uiState.value.copy(
                    lastResult = result?.timestamp?.toString() ?: "Never",
                    itemsSynced = result?.itemsSynced ?: 0,
                    errors = result?.errors?.joinToString("; ") ?: ""
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            dataSyncManager.performFullSync()
        }
    }

    fun toggleAutoSync() {
        val enabled = _uiState.value.autoSyncEnabled
        if (enabled) dataSyncManager.stopAutoSync() else dataSyncManager.startAutoSync()
        _uiState.value = _uiState.value.copy(autoSyncEnabled = !enabled)
    }
}

