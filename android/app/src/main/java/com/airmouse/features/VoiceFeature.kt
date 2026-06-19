// app/src/main/java/com/airmouse/features/VoiceFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import com.airmouse.domain.usecase.HandleVoiceCommandUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceFeature @Inject constructor(
    private val handleVoiceCommandUseCase: HandleVoiceCommandUseCase
) {

    data class VoiceFeatureState(
        val isListening: Boolean = false,
        val lastCommand: VoiceCommand? = null,
        val lastCommandText: String = "",
        val commandHistory: List<VoiceCommandHistory> = emptyList(),
        val commands: List<VoiceCommand> = emptyList(),
        val config: VoiceCommandConfig = VoiceCommandConfig(),
        val isProcessing: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(VoiceFeatureState())
    val state: StateFlow<VoiceFeatureState> = _state.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _wakeWordDetected = MutableStateFlow(false)
    val wakeWordDetected: StateFlow<Boolean> = _wakeWordDetected.asStateFlow()

    init {
        observeState()
    }

    private fun observeState() {
        // Observe voice command state
    }

    suspend fun processVoiceInput(text: String): Result<VoiceCommand?> {
        _state.value = _state.value.copy(isProcessing = true)

        val result = handleVoiceCommandUseCase(text)

        if (result.isSuccess) {
            val command = result.getOrNull()
            _state.value = _state.value.copy(
                lastCommand = command,
                lastCommandText = text,
                isProcessing = false
            )
        } else {
            _state.value = _state.value.copy(
                isProcessing = false,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    suspend fun startListening(): Result<Unit> {
        val result = handleVoiceCommandUseCase.startListening()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isListening = true)
        }
        return result
    }

    suspend fun stopListening(): Result<Unit> {
        val result = handleVoiceCommandUseCase.stopListening()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isListening = false)
        }
        return result
    }

    suspend fun isListening(): Boolean {
        return handleVoiceCommandUseCase.isListening()
    }

    suspend fun getCommands(): List<VoiceCommand> {
        return handleVoiceCommandUseCase.getCommands()
    }

    suspend fun addCommand(command: VoiceCommand): Result<Unit> {
        val result = handleVoiceCommandUseCase.addCommand(command)
        if (result.isSuccess) {
            refreshCommands()
        }
        return result
    }

    suspend fun updateCommand(command: VoiceCommand): Result<Unit> {
        val result = handleVoiceCommandUseCase.updateCommand(command)
        if (result.isSuccess) {
            refreshCommands()
        }
        return result
    }

    suspend fun deleteCommand(id: String): Result<Unit> {
        val result = handleVoiceCommandUseCase.deleteCommand(id)
        if (result.isSuccess) {
            refreshCommands()
        }
        return result
    }

    suspend fun toggleCommand(id: String, enabled: Boolean): Result<Unit> {
        return handleVoiceCommandUseCase.toggleCommand(id, enabled)
    }

    suspend fun getCommandHistory(): List<VoiceCommandHistory> {
        return handleVoiceCommandUseCase.getCommandHistory()
    }

    suspend fun clearHistory(): Result<Unit> {
        return handleVoiceCommandUseCase.clearHistory()
    }

    suspend fun getConfig(): VoiceCommandConfig {
        return handleVoiceCommandUseCase.getConfig()
    }

    suspend fun updateConfig(config: VoiceCommandConfig): Result<Unit> {
        val result = handleVoiceCommandUseCase.updateConfig(config)
        if (result.isSuccess) {
            _state.value = _state.value.copy(config = config)
        }
        return result
    }

    suspend fun getSupportedCommands(): List<String> {
        return handleVoiceCommandUseCase.getSupportedCommands()
    }

    fun observeCommands(): Flow<List<VoiceCommand>> {
        return handleVoiceCommandUseCase.observeCommands()
    }

    fun observeLastCommand(): Flow<VoiceCommand?> {
        return handleVoiceCommandUseCase.observeLastCommand()
    }

    suspend fun updateAudioLevel(level: Float) {
        _audioLevel.value = level
    }

    suspend fun setWakeWordDetected(detected: Boolean) {
        _wakeWordDetected.value = detected
    }

    private suspend fun refreshCommands() {
        val commands = handleVoiceCommandUseCase.getCommands()
        _state.value = _state.value.copy(commands = commands)
    }

    suspend fun isWakeWordActive(): Boolean {
        return getConfig().isEnabled && getConfig().wakeWord.isNotEmpty()
    }

    fun getVoiceFeatureState(): VoiceFeatureState = _state.value
}