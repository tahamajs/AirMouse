
package com.airmouse.domain.repository

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import kotlinx.coroutines.flow.Flow

interface IVoiceCommandRepository {
    
    suspend fun getCommands(): List<VoiceCommand>
    suspend fun getCommand(id: String): VoiceCommand?
    suspend fun addCommand(command: VoiceCommand)
    suspend fun updateCommand(command: VoiceCommand)
    suspend fun deleteCommand(id: String)
    suspend fun toggleCommand(id: String, enabled: Boolean)
    fun observeCommands(): Flow<List<VoiceCommand>>

    
    suspend fun startListening()
    suspend fun stopListening()
    suspend fun isListening(): Boolean
    suspend fun setListening(enabled: Boolean)

    
    suspend fun processVoiceInput(text: String): VoiceCommand?
    suspend fun getLastCommand(): VoiceCommand?
    fun observeLastCommand(): Flow<VoiceCommand?>

    
    suspend fun getCommandHistory(): List<VoiceCommandHistory>
    suspend fun addToHistory(history: VoiceCommandHistory)
    suspend fun clearHistory()

    
    suspend fun getConfig(): VoiceCommandConfig
    suspend fun updateConfig(config: VoiceCommandConfig)
    suspend fun getSupportedCommands(): List<String>
}