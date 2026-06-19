// app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioEvent.kt
package com.airmouse.presentation.ui.gesture

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureType

sealed class GestureStudioEvent {
    // Recording events
    data class StartRecording(val name: String) : GestureStudioEvent()
    object StopRecording : GestureStudioEvent()
    object CancelRecording : GestureStudioEvent()

    // CRUD events
    data class AddGesture(val name: String, val action: String) : GestureStudioEvent()
    data class UpdateGesture(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    data class DeleteGesture(val id: String) : GestureStudioEvent()
    data class ToggleGesture(val id: String) : GestureStudioEvent()
    data class ToggleFavorite(val id: String) : GestureStudioEvent()

    // Training events
    object StartTraining : GestureStudioEvent()
    object StopTraining : GestureStudioEvent()

    // Detection events
    data class DetectGesture(val sensorData: FloatArray) : GestureStudioEvent()

    // Filter & Sort events
    data class UpdateSearchQuery(val query: String) : GestureStudioEvent()
    data class UpdateFilterType(val type: GestureType?) : GestureStudioEvent()
    data class UpdateSort(val sort: GestureSort) : GestureStudioEvent()
    data class UpdateViewMode(val mode: ViewMode) : GestureStudioEvent()
    object ToggleWaveform : GestureStudioEvent()
    data class SelectGesture(val id: String) : GestureStudioEvent()

    // Settings events
    data class SetConfidenceThreshold(val threshold: Float) : GestureStudioEvent()
    data class SetCooldown(val cooldown: Long) : GestureStudioEvent()

    // Export/Import events
    data class ExportGestures(val format: ExportFormat) : GestureStudioEvent()
    object ImportGestures : GestureStudioEvent()

    // Dialog events
    object ShowAddDialog : GestureStudioEvent()
    data class ShowEditDialog(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    data class ShowDeleteDialog(val id: String) : GestureStudioEvent()
    object ShowTrainDialog : GestureStudioEvent()
    object ShowExportDialog : GestureStudioEvent()
    object ShowImportDialog : GestureStudioEvent()
    data class ShowDetailsDialog(val gesture: CustomGestureTemplate) : GestureStudioEvent()
    object CloseDialogs : GestureStudioEvent()

    // Reset events
    object ResetStats : GestureStudioEvent()
    object Refresh : GestureStudioEvent()
    object ClearMessages : GestureStudioEvent()
}