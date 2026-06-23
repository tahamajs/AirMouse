
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VoiceCommand(
    val id: String = "",
    val text: String = "",
    val action: String = "",
    val confidence: Float = 0f,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

@Parcelize
data class VoiceCommandHistory(
    val text: String = "",
    val action: String = "",
    val confidence: Float = 0f,
    val success: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

data class VoiceCommandConfig(
    val wakeWord: String = "hey air mouse",
    val wakeWordConfidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val silenceTimeoutMs: Long = 2000L,
    val wakeWordTimeoutMs: Long = 10000L
)