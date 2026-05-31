// app/src/main/java/com/airmouse/domain/usecase/ConnectToServerUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.repository.IConnectionRepository
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    private val connectionRepo: IConnectionRepository
) {

    suspend operator fun invoke(ip: String, port: Int, protocol: ConnectionProtocol = ConnectionProtocol.TCP) {
        val config = ConnectionConfig(ip, port, protocol)
        connectionRepo.connect(config)
    }

    suspend fun disconnect() {
        connectionRepo.disconnect()
    }
}