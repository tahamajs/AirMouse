// app/src/main/java/com/airmouse/domain/model/ErrorModels.kt
package com.airmouse.domain.model

/**
 * Error type
 */
enum class ErrorType {
    NETWORK,
    CONNECTION,
    AUTHENTICATION,
    PERMISSION,
    SENSOR,
    BLUETOOTH,
    USB,
    GESTURE,
    CALIBRATION,
    UNKNOWN
}

/**
 * Application error
 */
data class AppError(
    val type: ErrorType,
    val message: String,
    val code: Int = 0,
    val cause: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isRecoverable(): Boolean = when (type) {
        ErrorType.NETWORK -> true
        ErrorType.CONNECTION -> true
        ErrorType.PERMISSION -> false
        ErrorType.AUTHENTICATION -> true
        else -> false
    }
}