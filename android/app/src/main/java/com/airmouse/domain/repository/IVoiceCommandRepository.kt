// app/src/main/java/com/airmouse/domain/repository/IVoiceCommandRepository.kt
package com.airmouse.domain.repository

import kotlinx.coroutines.flow.Flow

interface IVoiceCommandRepository {
    suspend fun startListening()
    suspend fun stopListening()
    fun getRecognizedCommand(): Flow<VoiceCommand>
    fun getListeningStatus(): Flow<Boolean>
    suspend fun addCustomCommand(phrase: String, action: String)
    suspend fun removeCustomCommand(id: String)
    suspend fun getCustomCommands(): List<CustomVoiceCommand>
    suspend fun setWakeWord(word: String)
    suspend fun setLanguage(language: String)
}

data class VoiceCommand(
    val text: String,
    val action: String,
    val confidence: Float,
    val timestamp: Long
)

data class CustomVoiceCommand(
    val id: String,
    val phrase: String,
    val action: String,
    val isEnabled: Boolean = true
)