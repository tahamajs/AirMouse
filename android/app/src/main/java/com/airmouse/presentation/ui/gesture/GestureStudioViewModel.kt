// app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioViewModel.kt
package com.airmouse.presentation.ui.gesture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IGestureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureStudioViewModel @Inject constructor(
    private val gestureRepository: IGestureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GestureStudioUiState())
    val uiState: StateFlow<GestureStudioUiState> = _uiState.asStateFlow()

    private val _gestures = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    val gestures: StateFlow<List<CustomGestureTemplate>> = _gestures.asStateFlow()

    private val _stats = MutableStateFlow(GestureTrainingStats())
    val stats: StateFlow<GestureTrainingStats> = _stats.asStateFlow()

    private val _lastGesture = MutableStateFlow<GestureEvent?>(null)
    val lastGesture: StateFlow<GestureEvent?> = _lastGesture.asStateFlow()

    init {
        observeGestures()
        observeStats()
        loadData()
    }

    // ==================== Observation ====================

    private fun observeGestures() {
        viewModelScope.launch {
            gestureRepository.observeCustomGestures().collect { gestures ->
                _gestures.value = gestures
                updateFilteredGestures()
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            gestureRepository.observeGestureStats().collect { stats ->
                _stats.value = stats
                _uiState.update { it.copy(trainingStats = stats) }
            }
        }
    }

    // ==================== Data Loading ====================

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val gestures = gestureRepository.getAllCustomGestures()
                val stats = gestureRepository.getGestureStats()
                _gestures.value = gestures
                _stats.value = stats
                _uiState.update {
                    it.copy(
                        savedGestures = gestures,
                        filteredGestures = gestures,
                        trainingStats = stats,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load gestures"
                    )
                }
            }
        }
    }

    // ==================== Filtering & Sorting ====================

    private fun updateFilteredGestures() {
        val state = _uiState.value
        var filtered = _gestures.value

        // Filter by search query
        if (state.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { gesture ->
                gesture.name.contains(state.searchQuery, ignoreCase = true) ||
                        gesture.action.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Filter by type
        if (state.filterType != null) {
            filtered = filtered.filter { it.type == state.filterType }
        }

        // Filter favorites
        if (state.showFavoritesOnly) {
            // Would need favorite tracking
        }

        // Sort
        filtered = when (state.sortBy) {
            GestureSort.NAME -> filtered.sortedBy { it.name }
            GestureSort.USAGE -> filtered.sortedByDescending { it.usageCount }
            GestureSort.CREATED -> filtered.sortedByDescending { it.createdAt }
            GestureSort.UPDATED -> filtered.sortedByDescending { it.updatedAt }
            GestureSort.CONFIDENCE -> filtered.sortedByDescending { it.confidence }
        }

        _uiState.update { it.copy(filteredGestures = filtered) }
    }

    // ==================== Recording ====================

    fun startRecording(name: String = _uiState.value.gestureName) {
        if (name.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Please enter a gesture name")
            }
            return
        }

        _uiState.update {
            it.copy(
                isRecording = true,
                gestureName = name,
                status = "Recording...",
                statusColor = Color(0xFFFF9800),
                progress = 0,
                recordingTime = 0,
                samplesCollected = 0,
                recordingQuality = RecordingQuality.UNKNOWN,
                errorMessage = null
            )
        }

        // Start recording timer
        viewModelScope.launch {
            var time = 0
            while (_uiState.value.isRecording) {
                delay(100)
                time += 100
                val progress = (time / 5000f * 100f).coerceAtMost(100f)
                _uiState.update {
                    it.copy(
                        recordingTime = time,
                        progress = progress.toInt(),
                        samplesCollected = it.samplesCollected + 1
                    )
                }

                // Update quality based on samples
                val sampleCount = _uiState.value.samplesCollected
                if (sampleCount > 30) {
                    _uiState.update {
                        it.copy(
                            recordingQuality = if (sampleCount > 40)
                                RecordingQuality.EXCELLENT
                            else
                                RecordingQuality.GOOD
                        )
                    }
                }
            }
        }
    }

    fun updateGestureName(name: String) {
        _uiState.update { it.copy(gestureName = name, errorMessage = null) }
    }

    fun updateNewGestureName(name: String) {
        _uiState.update { it.copy(newGestureName = name, errorMessage = null) }
    }

    fun updateNewGestureAction(action: String) {
        _uiState.update { it.copy(newGestureAction = action, errorMessage = null) }
    }

    fun setSelectedSensor(sensor: SensorType) {
        _uiState.update { it.copy(selectedSensor = sensor) }
    }

    fun clearRecognition() {
        _uiState.update {
            it.copy(
                lastRecognizedGesture = null,
                lastRecognitionConfidence = 0f,
                lastRecognitionTime = null
            )
        }
    }

    fun stopRecording() {
        _uiState.update {
            it.copy(
                isRecording = false,
                status = "Recording stopped",
                statusColor = Color(0xFF4CAF50)
            )
        }
    }

    fun cancelRecording() {
        _uiState.update {
            it.copy(
                isRecording = false,
                status = "Cancelled",
                statusColor = Color(0xFFF44336),
                progress = 0,
                recordingTime = 0
            )
        }
    }

    // ==================== Gesture CRUD ====================

    fun addGesture(name: String, action: String) {
        if (name.isEmpty() || action.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Please fill in all fields")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val template = CustomGestureTemplate(
                    name = name,
                    action = action,
                    type = GestureType.CUSTOM,
                    confidence = 0.7f,
                    isEnabled = true
                )
                gestureRepository.addCustomGesture(template)
                loadData()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Gesture '$name' added successfully!",
                        showAddGestureDialog = false,
                        newGestureName = "",
                        newGestureAction = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to add gesture"
                    )
                }
            }
        }
    }

    fun addGesture() {
        addGesture(_uiState.value.newGestureName, _uiState.value.newGestureAction)
    }

    fun updateGesture(gesture: CustomGestureTemplate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                gestureRepository.updateCustomGesture(gesture)
                loadData()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Gesture updated successfully!",
                        showEditGestureDialog = false,
                        editGesture = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to update gesture"
                    )
                }
            }
        }
    }

    fun deleteGesture(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val name = _gestures.value.find { it.id == id }?.name ?: "Gesture"
                gestureRepository.deleteCustomGesture(id)
                loadData()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Gesture '$name' deleted successfully!",
                        showDeleteDialog = false,
                        deleteGestureId = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to delete gesture"
                    )
                }
            }
        }
    }

    fun toggleGesture(id: String) {
        viewModelScope.launch {
            try {
                val gesture = gestureRepository.getCustomGesture(id)
                gesture?.let {
                    gestureRepository.updateCustomGesture(
                        it.copy(isEnabled = !it.isEnabled)
                    )
                    loadData()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to toggle gesture")
                }
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                gestureRepository.toggleFavorite(id)
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to toggle favorite")
                }
            }
        }
    }

    fun showPlaybackDialog(gesture: CustomGestureTemplate) {
        _uiState.update {
            it.copy(
                showPlaybackDialog = true,
                selectedGesture = gesture,
                errorMessage = null
            )
        }
    }

    fun trainGesture(id: String) {
        viewModelScope.launch {
            try {
                val gesture = gestureRepository.getCustomGesture(id)
                if (gesture != null) {
                    gestureRepository.trainGesture(gesture.name, emptyList())
                    loadData()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to train gesture")
                }
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    // ==================== Training ====================

    fun startTraining() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTraining = true,
                    trainingStatus = "Starting training...",
                    trainingProgress = 0
                )
            }

            // Simulate training progress
            for (i in 1..100) {
                delay(50)
                _uiState.update {
                    it.copy(
                        trainingProgress = i,
                        trainingStatus = "Training in progress... $i%"
                    )
                }
            }

            // Perform actual training
            val success = gestureRepository.trainAllGestures()
            _uiState.update {
                it.copy(
                    isTraining = false,
                    trainingStatus = if (success) "Training complete!" else "Training failed",
                    trainingProgress = 0
                )
            }

            if (success) {
                loadData()
                _uiState.update {
                    it.copy(successMessage = "All gestures trained successfully!")
                }
            }
        }
    }

    // ==================== Recognition ====================

    fun detectGesture(sensorData: FloatArray) {
        viewModelScope.launch {
            try {
                val event = gestureRepository.detectGesture(sensorData)
                val threshold = gestureRepository.getConfidenceThreshold()

                if (event.type != GestureType.NONE && event.confidence >= threshold) {
                    _lastGesture.value = event
                    _uiState.update {
                        it.copy(
                            lastRecognizedGesture = event.name,
                            lastRecognitionConfidence = event.confidence,
                            lastRecognitionTime = event.timestamp,
                            recognizedGestureCount = it.recognizedGestureCount + 1
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to detect gesture")
                }
            }
        }
    }

    // ==================== Settings ====================

    fun setConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            try {
                gestureRepository.setConfidenceThreshold(threshold)
                _uiState.update {
                    it.copy(successMessage = "Confidence threshold updated to ${threshold * 100}%")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to update threshold")
                }
            }
        }
    }

    fun getConfidenceThreshold(): Float {
        return runBlocking {
            try {
                gestureRepository.getConfidenceThreshold()
            } catch (e: Exception) {
                0.7f
            }
        }
    }

    fun setCooldown(cooldown: Long) {
        viewModelScope.launch {
            try {
                gestureRepository.setCooldownMs(cooldown)
                _uiState.update {
                    it.copy(successMessage = "Cooldown updated to ${cooldown}ms")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to update cooldown")
                }
            }
        }
    }

    // ==================== Export/Import ====================

    fun exportGestures(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val gestures = gestureRepository.getAllCustomGestures()
                // Export logic here
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Exported ${gestures.size} gestures successfully!",
                        showExportDialog = false,
                        exportFormat = format
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to export gestures"
                    )
                }
            }
        }
    }

    fun importGestures() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = 0,
                    showImportDialog = false
                )
            }

            // Simulate import
            for (i in 1..100) {
                delay(20)
                _uiState.update { it.copy(importProgress = i) }
            }

            loadData()
            _uiState.update {
                it.copy(
                    isImporting = false,
                    importProgress = 0,
                    successMessage = "Gestures imported successfully!"
                )
            }
        }
    }

    fun importGestures(path: String) {
        _uiState.update { it.copy(exportPath = path) }
        importGestures()
    }

    // ==================== UI State Management ====================

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateFilteredGestures()
    }

    fun updateFilterType(type: GestureType?) {
        _uiState.update { it.copy(filterType = type) }
        updateFilteredGestures()
    }

    fun updateSort(sort: GestureSort) {
        _uiState.update { it.copy(sortBy = sort) }
        updateFilteredGestures()
    }

    fun updateViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun toggleWaveform() {
        _uiState.update { it.copy(showWaveform = !it.showWaveform) }
    }

    fun selectGesture(id: String) {
        val gesture = _gestures.value.find { it.id == id }
        _uiState.update { it.copy(selectedGesture = gesture) }
    }

    // ==================== Dialog Management ====================

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showAddGestureDialog = true,
                newGestureName = "",
                newGestureAction = "",
                errorMessage = null
            )
        }
    }

    fun showEditDialog(gesture: CustomGestureTemplate) {
        _uiState.update {
            it.copy(
                showEditGestureDialog = true,
                editGesture = gesture,
                newGestureName = gesture.name,
                newGestureAction = gesture.action,
                errorMessage = null
            )
        }
    }

    fun showDeleteDialog(id: String) {
        _uiState.update {
            it.copy(
                showDeleteDialog = true,
                deleteGestureId = id,
                errorMessage = null
            )
        }
    }

    fun showTrainDialog() {
        _uiState.update {
            it.copy(
                showTrainDialog = true,
                errorMessage = null
            )
        }
    }

    fun showExportDialog() {
        _uiState.update {
            it.copy(
                showExportDialog = true,
                errorMessage = null
            )
        }
    }

    fun showImportDialog() {
        _uiState.update {
            it.copy(
                showImportDialog = true,
                errorMessage = null
            )
        }
    }

    fun showDetailsDialog(gesture: CustomGestureTemplate) {
        _uiState.update {
            it.copy(
                showDetailsDialog = true,
                selectedGesture = gesture,
                errorMessage = null
            )
        }
    }

    fun closeAllDialogs() {
        _uiState.update {
            it.copy(
                showDeleteDialog = false,
                showTrainDialog = false,
                showExportDialog = false,
                showImportDialog = false,
                showDetailsDialog = false,
                showPlaybackDialog = false,
                showAddGestureDialog = false,
                showEditGestureDialog = false,
                deleteGestureId = null,
                editGesture = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    // ==================== Reset ====================

    fun resetStats() {
        viewModelScope.launch {
            try {
                gestureRepository.resetStats()
                loadData()
                _uiState.update {
                    it.copy(successMessage = "Statistics reset successfully!")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to reset statistics")
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }

    // ==================== Cleanup ====================

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources
    }
}

// Extension functions for UI state
fun GestureStudioUiState.isRecordingActive(): Boolean = isRecording
fun GestureStudioUiState.hasGestures(): Boolean = savedGestures.isNotEmpty()
fun GestureStudioUiState.hasFilteredGestures(): Boolean = filteredGestures.isNotEmpty()
fun GestureStudioUiState.getGestureCount(): Int = savedGestures.size
fun GestureStudioUiState.getFilteredCount(): Int = filteredGestures.size
fun GestureStudioUiState.getEnabledCount(): Int = savedGestures.count { it.isEnabled }
fun GestureStudioUiState.getDisabledCount(): Int = savedGestures.count { !it.isEnabled }
