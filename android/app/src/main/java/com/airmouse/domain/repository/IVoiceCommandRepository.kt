// app/src/main/java/com/airmouse/domain/repository/IVoiceCommandRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import kotlinx.coroutines.flow.Flow

interface IVoiceCommandRepository {
    // Commands
    suspend fun getCommands(): List<VoiceCommand>
    suspend fun getCommand(id: String): VoiceCommand?
    suspend fun addCommand(command: VoiceCommand)
    suspend fun updateCommand(command: VoiceCommand)
    suspend fun deleteCommand(id: String)
    suspend fun toggleCommand(id: String, enabled: Boolean)
    fun observeCommands(): Flow<List<VoiceCommand>>

    // Listening
    suspend fun startListening()
    suspend fun stopListening()
    suspend fun isListening(): Boolean
    suspend fun setListening(enabled: Boolean)

    // Processing
    suspend fun processVoiceInput(text: String): VoiceCommand?
    suspend fun getLastCommand(): VoiceCommand?
    fun observeLastCommand(): Flow<VoiceCommand?>

    // History
    suspend fun getCommandHistory(): List<VoiceCommandHistory>
    suspend fun addToHistory(history: VoiceCommandHistory)
    suspend fun clearHistory()

    // Configuration
    suspend fun getConfig(): VoiceCommandConfig
    suspend fun updateConfig(config: VoiceCommandConfig)
    suspend fun getSupportedCommands(): List<String>
}