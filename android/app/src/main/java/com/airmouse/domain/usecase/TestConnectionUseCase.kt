
package com.airmouse.domain.usecase

import com.airmouse.domain.model.TestResult
import com.airmouse.domain.repository.IConnectionRepository
import javax.inject.Inject

class TestConnectionUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    suspend operator fun invoke(ip: String, port: Int = 8080): Result<TestResult> {
        return try {
            val result = connectionRepository.testConnection(ip, port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testCurrentConnection(): Result<TestResult> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.testConnection(config.ip, config.port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun ping(): Result<Long> {
        return try {
            val latency = connectionRepository.ping()
            Result.success(latency)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}