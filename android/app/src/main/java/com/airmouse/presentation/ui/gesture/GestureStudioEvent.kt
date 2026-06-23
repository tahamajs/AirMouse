
package com.airmouse.presentation.ui.gesture

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureType

sealed class GestureStudioEvent {
    
    data class StartRecording(val name: String) : GestureStudioEvent()
    object StopRecording : GestureStudioEvent()
    object CancelRecording : GestureStudioEvent()

    
    data class AddGesture(val name: String, val action: String) : GestureStudioEvent()
    data class UpdateGesture(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    data class DeleteGesture(val id: String) : GestureStudioEvent()
    data class ToggleGesture(val id: String) : GestureStudioEvent()
    data class ToggleFavorite(val id: String) : GestureStudioEvent()

    
    object StartTraining : GestureStudioEvent()
    object StopTraining : GestureStudioEvent()

    
    data class DetectGesture(val sensorData: FloatArray) : GestureStudioEvent()

    
    data class UpdateSearchQuery(val query: String) : GestureStudioEvent()
    data class UpdateFilterType(val type: GestureType?) : GestureStudioEvent()
    data class UpdateSort(val sort: GestureSort) : GestureStudioEvent()
    data class UpdateViewMode(val mode: ViewMode) : GestureStudioEvent()
    object ToggleWaveform : GestureStudioEvent()
    data class SelectGesture(val id: String) : GestureStudioEvent()

    
    data class SetConfidenceThreshold(val threshold: Float) : GestureStudioEvent()
    data class SetCooldown(val cooldown: Long) : GestureStudioEvent()

    
    data class ExportGestures(val format: ExportFormat) : GestureStudioEvent()
    object ImportGestures : GestureStudioEvent()

    
    object ShowAddDialog : GestureStudioEvent()
    data class ShowEditDialog(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    data class ShowDeleteDialog(val id: String) : GestureStudioEvent()
    object ShowTrainDialog : GestureStudioEvent()
    object ShowExportDialog : GestureStudioEvent()
    object ShowImportDialog : GestureStudioEvent()
    data class ShowDetailsDialog(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    object CloseDialogs : GestureStudioEvent()

    
    object ResetStats : GestureStudioEvent()
    object Refresh : GestureStudioEvent()
    object ClearMessages : GestureStudioEvent()
}