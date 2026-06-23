
package com.airmouse.domain.usecase

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import com.airmouse.domain.repository.IVoiceCommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HandleVoiceCommandUseCase @Inject constructor(
    private val voiceCommandRepository: IVoiceCommandRepository
) {

    suspend operator fun invoke(text: String): Result<VoiceCommand?> {
        return try {
            val command = voiceCommandRepository.processVoiceInput(text)
            Result.success(command)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startListening(): Result<Unit> {
        return try {
            voiceCommandRepository.startListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopListening(): Result<Unit> {
        return try {
            voiceCommandRepository.stopListening()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isListening(): Boolean {
        return voiceCommandRepository.isListening()
    }

    suspend fun getCommands(): List<VoiceCommand> {
        return voiceCommandRepository.getCommands()
    }

    suspend fun getCommand(id: String): VoiceCommand? {
        return voiceCommandRepository.getCommand(id)
    }

    suspend fun addCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.addCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommand(command: VoiceCommand): Result<Unit> {
        return try {
            voiceCommandRepository.updateCommand(command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCommand(id: String): Result<Unit> {
        return try {
            voiceCommandRepository.deleteCommand(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCommand(id: String, enabled: Boolean): Result<Unit> {
        return try {
            voiceCommandRepository.toggleCommand(id, enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommandHistory(): List<VoiceCommandHistory> {
        return voiceCommandRepository.getCommandHistory()
    }

    suspend fun clearHistory(): Result<Unit> {
        return try {
            voiceCommandRepository.clearHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfig(): VoiceCommandConfig {
        return voiceCommandRepository.getConfig()
    }

    suspend fun updateConfig(config: VoiceCommandConfig): Result<Unit> {
        return try {
            voiceCommandRepository.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSupportedCommands(): List<String> {
        return voiceCommandRepository.getSupportedCommands()
    }

    fun observeCommands(): Flow<List<VoiceCommand>> {
        return voiceCommandRepository.observeCommands()
    }

    fun observeLastCommand(): Flow<VoiceCommand?> {
        return voiceCommandRepository.observeLastCommand()
    }
}