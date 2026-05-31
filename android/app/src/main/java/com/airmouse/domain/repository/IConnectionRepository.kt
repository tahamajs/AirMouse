// app/src/main/java/com/airmouse/domain/repository/IConnectionRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.MouseEvent
import kotlinx.coroutines.flow.Flow

interface IConnectionRepository {
    suspend fun connect(config: ConnectionConfig)
    suspend fun disconnect()
    suspend fun sendEvent(event: MouseEvent)
    fun connectionStatus(): Flow<ConnectionStatus>
    suspend fun getLastConfig(): ConnectionConfig?
    suspend fun saveConfig(config: ConnectionConfig)
}