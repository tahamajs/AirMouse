package com.airmouse.presentation.ui.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.files.FileTransferService
import com.airmouse.files.Transfer
import com.airmouse.files.TransferState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileTransferViewModel @Inject constructor(
    private val fileTransferService: FileTransferService
) : ViewModel() {

    // Now this works because FileTransferService.TransferState is defined
    val transferState: StateFlow<TransferState> = fileTransferService.state

    private val _uiState = MutableStateFlow(FileTransferUiState())
    val uiState: StateFlow<FileTransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fileTransferService.state.collect { state ->
                _uiState.update {
                    it.copy(
                        currentTransfer = state.currentTransfer,
                        completedTransfers = state.completedTransfers,
                        queueSize = state.queueSize,
                        isActive = state.isActive
                    )
                }
            }
        }
    }

    fun uploadFile(uri: Uri, fileName: String) {
        fileTransferService.queueFileForUpload(uri, fileName)
    }

    fun downloadFile(fileName: String, destinationPath: String) {
        fileTransferService.downloadFile(fileName, destinationPath)
    }

    fun cancelActiveTransfer() {
        val transfer = _uiState.value.currentTransfer
        if (transfer != null) {
            fileTransferService.cancelTransfer(transfer.id)
        }
    }

    fun clearHistory() {
        fileTransferService.clearCompletedTransfers()
    }

    // Returns the transfer folder path
    fun getTransferFolderPath(): String {
        return fileTransferService.getTransferFolderPath()
    }

    // Opens the folder
    fun openTransferFolder() {
        fileTransferService.openTransferFolder()
    }

    data class FileTransferUiState(
        val currentTransfer: Transfer? = null,
        val completedTransfers: List<Transfer> = emptyList(),
        val queueSize: Int = 0,
        val isActive: Boolean = false
    )
}
