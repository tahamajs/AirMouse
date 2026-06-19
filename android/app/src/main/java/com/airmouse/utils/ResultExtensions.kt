// app/src/main/java/com/airmouse/utils/ResultExtensions.kt
package com.airmouse.utils

fun <T> Result<T>.getOrNull(): T? {
    return try {
        getOrNull()
    } catch (e: Exception) {
        null
    }
}

fun <T> Result<T>.getErrorMessage(): String {
    return try {
        exceptionOrNull()?.message ?: "Unknown error"
    } catch (e: Exception) {
        "Error occurred"
    }
}

fun <T> Result<T>.isSuccess(): Boolean {
    return isSuccess
}

fun <T> Result<T>.isFailure(): Boolean {
    return isFailure
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return try {
        val value = getOrNull()
        if (value != null) {
            Result.success(transform(value))
        } else {
            Result.failure(Exception("Value is null"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (isSuccess) {
        getOrNull()?.let { action(it) }
    }
    return this
}

inline fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (isFailure) {
        exceptionOrNull()?.let { action(it) }
    }
    return this
}