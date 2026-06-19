// app/src/main/java/com/airmouse/domain/usecase/HandleVoiceCommandUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import com.airmouse.domain.repository.IVoiceCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for handling voice commands
 */
class HandleVoiceCommandUseCase @Inject constructor(
    private val voiceCommandRepository: IVoiceCommandRepository
) {

    /**
     * Process voice input
     */
    suspend operator fun invoke(text: String): Result<VoiceCommand?> {
        return try {
            val command = voiceCommandRepository.processVoiceInput(text)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start listening
     */
    suspend fun startListening(): Result<Unit> {
        return try {
            voiceCommandRepository.startListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop listening
     */
    suspend fun stopListening(): Result<Unit> {
        return try {
            voiceCommandRepository.stopListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if listening
     */
    suspend fun isListening(): Boolean {
        return voiceCommandRepository.isListening()
    }

    /**
     * Get all commands
     */
    suspend fun getCommands(): List<VoiceCommand> {
        return voiceCommandRepository.getCommands()
    }

    /**
     * Get command by ID
     */
    suspend fun getCommand(id: String): VoiceCommand? {
        return voiceCommandRepository.getCommand(id)
    }

    /**
     * Add custom command
     */
    suspend fun addCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.addCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update command
     */
    suspend fun updateCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.updateCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete command
     */
    suspend fun deleteCommand(id: String): Result<Unit> {
        return try {
            voiceCommandRepository.deleteCommand(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle command
     */
    suspend fun toggleCommand(id: String, enabled: Boolean): Result<Unit> {
        return try {
            voiceCommandRepository.toggleCommand(id, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get command history
     */
    suspend fun getCommandHistory(): List<VoiceCommandHistory> {
        return voiceCommandRepository.getCommandHistory()
    }

    /**
     * Clear command history
     */
    suspend fun clearHistory(): Result<Unit> {
        return try {
            voiceCommandRepository.clearHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get configuration
     */
    suspend fun getConfig(): VoiceCommandConfig {
        return voiceCommandRepository.getConfig()
    }

    /**
     * Update configuration
     */
    suspend fun updateConfig(config: VoiceCommandConfig): Result<Unit> {
        return try {
            voiceCommandRepository.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get supported commands
     */
    suspend fun getSupportedCommands(): List<String> {
        return voiceCommandRepository.getSupportedCommands()
    }

    /**
     * Observe commands
     */
    fun observeCommands(): Flow<List<VoiceCommand>> {
        return voiceCommandRepository.observeCommands()
    }

    /**
     * Observe last command
     */
    fun observeLastCommand(): Flow<VoiceCommand?> {
        return voiceCommandRepository.observeLastCommand()
    }
}