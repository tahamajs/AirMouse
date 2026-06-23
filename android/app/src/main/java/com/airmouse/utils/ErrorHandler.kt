
package com.airmouse.utils

import android.content.Context
import android.widget.Toast
import com.airmouse.domain.model.AppError
import com.airmouse.domain.model.ErrorType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val errorListeners = mutableListOf<(AppError) -> Unit>()

    fun handleError(error: AppError) {
        
        android.util.Log.e("AppError", "Error: ${error.message}", error.cause)

        
        val userMessage = getUserFriendlyMessage(error)
        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()

        
        errorListeners.forEach { it(error) }
    }

    fun handleException(throwable: Throwable, type: ErrorType = ErrorType.UNKNOWN) {
        val error = AppError(
            type = type,
            message = throwable.message ?: "Unknown error",
            cause = throwable
        )
        handleError(error)
    }

    fun addErrorListener(listener: (AppError) -> Unit) {
        errorListeners.add(listener)
    }

    fun removeErrorListener(listener: (AppError) -> Unit) {
        errorListeners.remove(listener)
    }

    private fun getUserFriendlyMessage(error: AppError): String {
        return when (error.type) {
            ErrorType.NETWORK -> "Network error. Please check your connection."
            ErrorType.CONNECTION -> "Failed to connect to server."
            ErrorType.AUTHENTICATION -> "Authentication failed. Please try again."
            ErrorType.PERMISSION -> "Permission required for this feature."
            ErrorType.SENSOR -> "Sensor error. Please restart the app."
            ErrorType.BLUETOOTH -> "Bluetooth error. Please check Bluetooth settings."
            ErrorType.USB -> "USB error. Please check USB connection."
            ErrorType.GESTURE -> "Gesture recognition error."
            ErrorType.CALIBRATION -> "Calibration failed. Please try again."
            else -> error.message ?: "An error occurred."
        }
    }
}