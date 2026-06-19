// app/src/main/java/com/airmouse/domain/usecase/TestConnectionUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.TestResult
import com.airmouse.domain.repository.IConnectionRepository
import javax.inject.Inject

/**
 * Use case for testing connection
 */
class TestConnectionUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Test connection to server
     */
    suspend operator fun invoke(ip: String, port: Int = 8080): Result<TestResult> {
        return try {
            val result = connectionRepository.testConnection(ip, port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Test current connection
     */
    suspend fun testCurrentConnection(): Result<TestResult> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.testConnection(config.ip, config.port)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Ping server
     */
    suspend fun ping(): Result<Long> {
        return try {
            val latency = connectionRepository.ping()
            Result.success(latency)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}