package com.airmouse.presentation.ui.gesture

import com.airmouse.domain.model.CustomGestureTemplate

data class GestureStudioUiState(
    val gestureName: String = "",
    val isRecording: Boolean = false,
    val status: String = "Ready",
    val statusColor: Long = 0xFF4CAF50,
    val progress: Int = 0,
    val recordingTime: Int = 0,
    val savedGestures: List<CustomGestureTemplate> = emptyList(),
    val selectedGesture: CustomGestureTemplate? = null,
    val isTraining: Boolean = false,
    val trainingProgress: Int = 0,
    val showDeleteDialog: Boolean = false,
    val showTrainDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)