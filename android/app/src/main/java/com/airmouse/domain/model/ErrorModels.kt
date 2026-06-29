package com.airmouse.domain.model

/**
 * Types of errors that can occur in the application.
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
 * Application error model with type, message, and metadata.
 */
data class AppError(
    val type: ErrorType,
    val message: String,
    val code: Int = 0,
    val cause: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Check if the error is recoverable (user can retry).
     */
    fun isRecoverable(): Boolean = when (type) {
        ErrorType.NETWORK -> true
        ErrorType.CONNECTION -> true
        ErrorType.AUTHENTICATION -> true
        ErrorType.PERMISSION -> false
        ErrorType.SENSOR -> false
        ErrorType.BLUETOOTH -> true
        ErrorType.USB -> true
        ErrorType.GESTURE -> true
        ErrorType.CALIBRATION -> true
        ErrorType.UNKNOWN -> false
    }

    /**
     * Check if the error is fatal (app should stop or show critical UI).
     */
    fun isFatal(): Boolean = when (type) {
        ErrorType.PERMISSION -> true
        ErrorType.SENSOR -> false
        else -> false
    }

    /**
     * Get a user-friendly error message.
     */
    fun getUserMessage(): String {
        return when (type) {
            ErrorType.NETWORK -> "Network connection issue. Please check your internet."
            ErrorType.CONNECTION -> "Failed to connect to server. Please try again."
            ErrorType.AUTHENTICATION -> "Authentication failed. Please check your credentials."
            ErrorType.PERMISSION -> "Permission denied. Please grant the required permissions."
            ErrorType.SENSOR -> "Sensor error. Please restart the app."
            ErrorType.BLUETOOTH -> "Bluetooth error. Please check Bluetooth connection."
            ErrorType.USB -> "USB error. Please check the USB connection."
            ErrorType.GESTURE -> "Gesture recognition error. Please try again."
            ErrorType.CALIBRATION -> "Calibration error. Please recalibrate."
            ErrorType.UNKNOWN -> "An unknown error occurred. Please try again."
        }
    }

    /**
     * Get a suggestion for the user.
     */
    fun getSuggestion(): String {
        return when (type) {
            ErrorType.NETWORK -> "Check your Wi-Fi or cellular connection."
            ErrorType.CONNECTION -> "Ensure the server is running and reachable."
            ErrorType.AUTHENTICATION -> "Verify your username and password."
            ErrorType.PERMISSION -> "Go to Settings and grant the required permissions."
            ErrorType.SENSOR -> "Restart the app or check sensor availability."
            ErrorType.BLUETOOTH -> "Ensure Bluetooth is enabled and paired."
            ErrorType.USB -> "Check USB connection and try reconnecting."
            ErrorType.GESTURE -> "Try a more distinct gesture motion."
            ErrorType.CALIBRATION -> "Recalibrate the sensors."
            else -> "Try restarting the app."
        }
    }

    /**
     * Get the error type as a display string.
     */
    fun getTypeDisplayName(): String {
        return when (type) {
            ErrorType.NETWORK -> "Network Error"
            ErrorType.CONNECTION -> "Connection Error"
            ErrorType.AUTHENTICATION -> "Authentication Error"
            ErrorType.PERMISSION -> "Permission Error"
            ErrorType.SENSOR -> "Sensor Error"
            ErrorType.BLUETOOTH -> "Bluetooth Error"
            ErrorType.USB -> "USB Error"
            ErrorType.GESTURE -> "Gesture Error"
            ErrorType.CALIBRATION -> "Calibration Error"
            ErrorType.UNKNOWN -> "Unknown Error"
        }
    }

    /**
     * Get a formatted string for logging.
     */
    fun getLogString(): String {
        return "[${getTypeDisplayName()}] $message (code=$code, recoverable=${isRecoverable()})"
    }

    /**
     * Check if the error has a cause.
     */
    fun hasCause(): Boolean = cause != null

    /**
     * Get the error code as a string (for debugging).
     */
    fun getCodeString(): String = if (code > 0) "Error $code" else "No code"

    companion object {
        /**
         * Create a network error.
         */
        fun network(message: String = "Network connection failed", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.NETWORK,
                message = message,
                cause = cause
            )
        }

        /**
         * Create a connection error.
         */
        fun connection(message: String = "Connection failed", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.CONNECTION,
                message = message,
                cause = cause
            )
        }

        /**
         * Create an authentication error.
         */
        fun authentication(message: String = "Authentication failed", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.AUTHENTICATION,
                message = message,
                cause = cause
            )
        }

        /**
         * Create a permission error.
         */
        fun permission(message: String = "Permission denied", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.PERMISSION,
                message = message,
                cause = cause
            )
        }

        /**
         * Create a sensor error.
         */
        fun sensor(message: String = "Sensor error", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.SENSOR,
                message = message,
                cause = cause
            )
        }

        /**
         * Create an unknown error.
         */
        fun unknown(message: String = "Unknown error", cause: Throwable? = null): AppError {
            return AppError(
                type = ErrorType.UNKNOWN,
                message = message,
                cause = cause
            )
        }

        /**
         * Create an error from a Throwable.
         */
        fun fromThrowable(throwable: Throwable): AppError {
            val message = throwable.message ?: "Unknown error"
            val type = when {
                message.contains("network", ignoreCase = true) -> ErrorType.NETWORK
                message.contains("connection", ignoreCase = true) -> ErrorType.CONNECTION
                message.contains("auth", ignoreCase = true) -> ErrorType.AUTHENTICATION
                message.contains("permission", ignoreCase = true) -> ErrorType.PERMISSION
                message.contains("bluetooth", ignoreCase = true) -> ErrorType.BLUETOOTH
                message.contains("usb", ignoreCase = true) -> ErrorType.USB
                message.contains("gesture", ignoreCase = true) -> ErrorType.GESTURE
                message.contains("calibration", ignoreCase = true) -> ErrorType.CALIBRATION
                message.contains("sensor", ignoreCase = true) -> ErrorType.SENSOR
                else -> ErrorType.UNKNOWN
            }
            return AppError(
                type = type,
                message = message,
                cause = throwable
            )
        }
    }
}