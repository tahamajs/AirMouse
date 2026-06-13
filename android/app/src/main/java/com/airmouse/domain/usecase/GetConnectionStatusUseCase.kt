// app/src/main/java/com/airmouse/domain/usecase/GetConnectionStatusUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConnectionStatusUseCase @Inject constructor(
    private val connectionRepo: IConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionStatus> = connectionRepo.connectionStatus()
}

package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConnectionStatusUseCase @Inject constructor(
    private val repo: IConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionStatus> = repo.connectionStatus()
}