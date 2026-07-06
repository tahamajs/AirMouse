
package com.airmouse.presentation.ui.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

@HiltViewModel
class GestureStudioViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val gestureRepository: IGestureRepository
) : ViewModel(), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val tempSamplesList = mutableListOf<Float>()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_uiState.value.isRecording) return

        val selected = _uiState.value.selectedSensor
        val isTargetSensor = when (selected) {
            SensorType.GYROSCOPE -> event.sensor.type == Sensor.TYPE_GYROSCOPE
            SensorType.ACCELEROMETER -> event.sensor.type == Sensor.TYPE_ACCELEROMETER
            SensorType.MAGNETOMETER -> event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD
            SensorType.ALL -> true
        }

        if (isTargetSensor) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            var value = sqrt(x * x + y * y + z * z)
            
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                value = abs(value - 9.81f)
            }

            synchronized(tempSamplesList) {
                tempSamplesList.add(value)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

    

    private fun updateFilteredGestures() {
        val state = _uiState.value
        var filtered = _gestures.value

        
        if (state.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { gesture ->
                gesture.name.contains(state.searchQuery, ignoreCase = true) ||
                        gesture.action.contains(state.searchQuery, ignoreCase = true)
            }
        }

        
        if (state.filterType != null) {
            filtered = filtered.filter { it.type == state.filterType }
        }

        
        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        
        filtered = when (state.sortBy) {
            GestureSort.NAME -> filtered.sortedBy { it.name }
            GestureSort.USAGE -> filtered.sortedByDescending { it.usageCount }
            GestureSort.CREATED -> filtered.sortedByDescending { it.createdAt }
            GestureSort.UPDATED -> filtered.sortedByDescending { it.updatedAt }
            GestureSort.CONFIDENCE -> filtered.sortedByDescending { it.confidence }
        }

        _uiState.update { it.copy(filteredGestures = filtered) }
    }

    

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

        synchronized(tempSamplesList) {
            tempSamplesList.clear()
        }

        val sensorToRegister = when (_uiState.value.selectedSensor) {
            SensorType.GYROSCOPE -> gyroscope
            SensorType.ACCELEROMETER -> accelerometer
            SensorType.MAGNETOMETER -> magnetometer
            SensorType.ALL -> null
        }

        if (sensorToRegister != null) {
            sensorManager.registerListener(this, sensorToRegister, SensorManager.SENSOR_DELAY_GAME)
        } else {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }

        viewModelScope.launch {
            var time = 0
            while (_uiState.value.isRecording) {
                delay(100)
                time += 100
                val progress = (time / 5000f * 100f).coerceAtMost(100f)
                val currentSamples = synchronized(tempSamplesList) { tempSamplesList.toList() }
                
                val maxVal = if (currentSamples.isNotEmpty()) currentSamples.maxOrNull() ?: 0f else 0f
                val minVal = if (currentSamples.isNotEmpty()) currentSamples.minOrNull() ?: 0f else 0f

                _uiState.update {
                    it.copy(
                        recordingTime = time,
                        progress = progress.toInt(),
                        samplesCollected = currentSamples.size,
                        waveformData = GestureWaveformData(
                            samples = currentSamples.takeLast(100),
                            maxValue = maxVal,
                            minValue = minVal,
                            samplingRate = 50
                        )
                    )
                }

                val sampleCount = currentSamples.size
                if (sampleCount > 30) {
                    _uiState.update {
                        it.copy(
                            recordingQuality = if (sampleCount > 120)
                                RecordingQuality.EXCELLENT
                            else if (sampleCount > 60)
                                RecordingQuality.GOOD
                            else
                                RecordingQuality.FAIR
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
        sensorManager.unregisterListener(this)
        _uiState.update {
            it.copy(
                isRecording = false,
                status = "Recording stopped",
                statusColor = Color(0xFF4CAF50)
            )
        }
    }

    fun cancelRecording() {
        sensorManager.unregisterListener(this)
        synchronized(tempSamplesList) {
            tempSamplesList.clear()
        }
        _uiState.update {
            it.copy(
                isRecording = false,
                status = "Cancelled",
                statusColor = Color(0xFFF44336),
                progress = 0,
                recordingTime = 0,
                waveformData = GestureWaveformData()
            )
        }
    }

    

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

    

    fun startTraining() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTraining = true,
                    trainingStatus = "Starting training...",
                    trainingProgress = 0
                )
            }

            
            for (i in 1..100) {
                delay(50)
                _uiState.update {
                    it.copy(
                        trainingProgress = i,
                        trainingStatus = "Training in progress... $i%"
                    )
                }
            }

            
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

    

    fun exportGestures(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val gestures = gestureRepository.getAllCustomGestures()
                val file = java.io.File(context.getExternalFilesDir(null), "gesture_export_${System.currentTimeMillis()}${format.extension}")
                
                when (format) {
                    ExportFormat.JSON -> {
                        val gson = com.google.gson.Gson()
                        val jsonString = gson.toJson(gestures)
                        file.writeText(jsonString)
                    }
                    ExportFormat.CSV -> {
                        val csvHeader = "id,name,type,action,confidence,isEnabled,isFavorite,createdAt,updatedAt,usageCount,description,lastUsed,isSystemGesture,version\n"
                        val csvContent = gestures.joinToString(separator = "\n") { g ->
                            "${g.id},${g.name},${g.type.name},${g.action},${g.confidence},${g.isEnabled},${g.isFavorite},${g.createdAt},${g.updatedAt},${g.usageCount},${g.description},${g.lastUsed},${g.isSystemGesture},${g.version}"
                        }
                        file.writeText(csvHeader + csvContent)
                    }
                    ExportFormat.XML -> {
                        val xml = java.lang.StringBuilder("<gestures>")
                        gestures.forEach { g ->
                            xml.append("<gesture>")
                            xml.append("<id>${g.id}</id>")
                            xml.append("<name>${g.name}</name>")
                            xml.append("<type>${g.type.name}</type>")
                            xml.append("<action>${g.action}</action>")
                            xml.append("<confidence>${g.confidence}</confidence>")
                            xml.append("<isEnabled>${g.isEnabled}</isEnabled>")
                            xml.append("<isFavorite>${g.isFavorite}</isFavorite>")
                            xml.append("<createdAt>${g.createdAt}</createdAt>")
                            xml.append("<updatedAt>${g.updatedAt}</updatedAt>")
                            xml.append("<usageCount>${g.usageCount}</usageCount>")
                            xml.append("<description>${g.description}</description>")
                            xml.append("<lastUsed>${g.lastUsed}</lastUsed>")
                            xml.append("<isSystemGesture>${g.isSystemGesture}</isSystemGesture>")
                            xml.append("<version>${g.version}</version>")
                            xml.append("</gesture>")
                        }
                        xml.append("</gestures>")
                        file.writeText(xml.toString())
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Exported ${gestures.size} gestures to ${file.name}!",
                        showExportDialog = false,
                        exportFormat = format,
                        exportPath = file.absolutePath
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
        val path = _uiState.value.exportPath
        if (path.isNullOrEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Please specify a file path to import")
            }
            return
        }
        importGestures(path)
    }

    fun importGestures(path: String) {
        viewModelScope.launch {
            val file = java.io.File(path)
            if (!file.exists()) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "File not found at: $path",
                        showImportDialog = true
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = 0,
                    errorMessage = null,
                    successMessage = null
                )
            }
            try {
                val content = file.readText()
                val importedGestures = mutableListOf<CustomGestureTemplate>()
                if (path.endsWith(".json")) {
                    val gson = com.google.gson.Gson()
                    val listType = object : com.google.gson.reflect.TypeToken<List<CustomGestureTemplate>>() {}.type
                    val parsed: List<CustomGestureTemplate> = gson.fromJson(content, listType)
                    importedGestures.addAll(parsed)
                } else if (path.endsWith(".csv")) {
                    val lines = file.readLines()
                    if (lines.size > 1) {
                        for (i in 1 until lines.size) {
                            val line = lines[i]
                            val parts = line.split(",")
                            if (parts.size >= 14) {
                                try {
                                    val g = CustomGestureTemplate(
                                        id = parts[0],
                                        name = parts[1],
                                        type = com.airmouse.domain.model.GestureType.valueOf(parts[2]),
                                        action = parts[3],
                                        confidence = parts[4].toFloatOrNull() ?: 0.7f,
                                        isEnabled = parts[5].toBoolean(),
                                        isFavorite = parts[6].toBoolean(),
                                        createdAt = parts[7].toLongOrNull() ?: System.currentTimeMillis(),
                                        updatedAt = parts[8].toLongOrNull() ?: System.currentTimeMillis(),
                                        usageCount = parts[9].toIntOrNull() ?: 0,
                                        description = parts[10],
                                        lastUsed = parts[11].toLongOrNull() ?: System.currentTimeMillis(),
                                        isSystemGesture = parts[12].toBoolean(),
                                        version = parts[13].toIntOrNull() ?: 1
                                    )
                                    importedGestures.add(g)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                } else if (path.endsWith(".xml")) {
                    val regex = Regex("<gesture>(.*?)</gesture>", RegexOption.DOT_MATCHES_ALL)
                    val matches = regex.findAll(content)
                    matches.forEach { match ->
                        val inner = match.groupValues[1]
                        fun getTag(tag: String): String {
                            val r = Regex("<$tag>(.*?)</$tag>")
                            return r.find(inner)?.groupValues?.get(1) ?: ""
                        }
                        try {
                            val g = CustomGestureTemplate(
                                id = getTag("id"),
                                name = getTag("name"),
                                type = com.airmouse.domain.model.GestureType.valueOf(getTag("type")),
                                action = getTag("action"),
                                confidence = getTag("confidence").toFloatOrNull() ?: 0.7f,
                                isEnabled = getTag("isEnabled").toBoolean(),
                                isFavorite = getTag("isFavorite").toBoolean(),
                                createdAt = getTag("createdAt").toLongOrNull() ?: System.currentTimeMillis(),
                                updatedAt = getTag("updatedAt").toLongOrNull() ?: System.currentTimeMillis(),
                                usageCount = getTag("usageCount").toIntOrNull() ?: 0,
                                description = getTag("description"),
                                lastUsed = getTag("lastUsed").toLongOrNull() ?: System.currentTimeMillis(),
                                isSystemGesture = getTag("isSystemGesture").toBoolean(),
                                version = getTag("version").toIntOrNull() ?: 1
                            )
                            importedGestures.add(g)
                        } catch (_: Exception) {}
                    }
                }

                if (importedGestures.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = "No valid gestures found to import"
                        )
                    }
                    return@launch
                }

                var count = 0
                importedGestures.forEach { gesture ->
                    gestureRepository.saveGestureTemplate(gesture)
                    count++
                    val progress = (count * 100 / importedGestures.size)
                    _uiState.update { it.copy(importProgress = progress) }
                    delay(10)
                }

                loadData()
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = 0,
                        showImportDialog = false,
                        successMessage = "Successfully imported ${importedGestures.size} gestures!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        updateFilteredGestures()
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
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

    

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}


fun GestureStudioUiState.isRecordingActive(): Boolean = isRecording
fun GestureStudioUiState.hasGestures(): Boolean = savedGestures.isNotEmpty()
fun GestureStudioUiState.hasFilteredGestures(): Boolean = filteredGestures.isNotEmpty()
fun GestureStudioUiState.getGestureCount(): Int = savedGestures.size
fun GestureStudioUiState.getFilteredCount(): Int = filteredGestures.size
fun GestureStudioUiState.getEnabledCount(): Int = savedGestures.count { it.isEnabled }
fun GestureStudioUiState.getDisabledCount(): Int = savedGestures.count { !it.isEnabled }
