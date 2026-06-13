// app/src/main/java/com/airmouse/domain/model/VoiceCommandModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Voice command detected.
 */
@Parcelize
data class VoiceCommand(
    val text: String,
    val action: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false
) : Parcelable

/**
 * Custom voice command defined by user.
 */
@Parcelize
data class CustomVoiceCommand(
    val id: String,
    val phrase: String,
    val action: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Voice recognition status.
 */
enum class VoiceRecognitionStatus {
    IDLE, LISTENING, PROCESSING, EXECUTING, ERROR
}