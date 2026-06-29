package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * A single voice command mapping a spoken phrase to an action.
 */
@Parcelize
data class VoiceCommand(
    val id: String = "",
    val text: String = "",
    val action: String = "",
    val confidence: Float = 0f,
    val isEnabled: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Check if this command is valid for execution.
     */
    fun isValid(): Boolean {
        return text.isNotBlank() && action.isNotBlank() && confidence >= 0.5f
    }

    /**
     * Get a display name for the action.
     */
    fun getActionDisplayName(): String {
        return when (action) {
            "play_pause" -> "Play/Pause"
            "next_track" -> "Next Track"
            "prev_track" -> "Previous Track"
            "volume_up" -> "Volume Up"
            "volume_down" -> "Volume Down"
            "mute" -> "Mute"
            "stop" -> "Stop"
            "lock_screen" -> "Lock Screen"
            "pause_movement" -> "Pause Movement"
            "resume_movement" -> "Resume Movement"
            else -> action.replace("_", " ").split(" ").joinToString(" ") {
                it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
            }
        }
    }

    companion object {
        /**
         * Create a custom voice command.
         */
        fun custom(text: String, action: String): VoiceCommand {
            return VoiceCommand(
                id = UUID.randomUUID().toString(),
                text = text,
                action = action,
                isCustom = true,
                confidence = 0.8f
            )
        }

        /**
         * Create a system voice command.
         */
        fun system(text: String, action: String): VoiceCommand {
            return VoiceCommand(
                id = UUID.randomUUID().toString(),
                text = text,
                action = action,
                isCustom = false,
                confidence = 0.85f
            )
        }

        /**
         * Get default system voice commands.
         */
        fun defaultCommands(): List<VoiceCommand> {
            return listOf(
                VoiceCommand.system("play", "play_pause"),
                VoiceCommand.system("pause", "play_pause"),
                VoiceCommand.system("next", "next_track"),
                VoiceCommand.system("previous", "prev_track"),
                VoiceCommand.system("volume up", "volume_up"),
                VoiceCommand.system("volume down", "volume_down"),
                VoiceCommand.system("mute", "mute"),
                VoiceCommand.system("stop", "stop"),
                VoiceCommand.system("lock", "lock_screen"),
                VoiceCommand.system("resume movement", "resume_movement"),
                VoiceCommand.system("pause movement", "pause_movement")
            )
        }
    }
}

/**
 * History entry for a voice command execution.
 */
@Parcelize
data class VoiceCommandHistory(
    val text: String = "",
    val action: String = "",
    val confidence: Float = 0f,
    val success: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Get a formatted timestamp string.
     */
    fun getFormattedTime(): String {
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(timestamp)
    }

    /**
     * Get a formatted date string.
     */
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(timestamp)
    }

    /**
     * Check if this was a successful command execution.
     */
    fun isSuccessful(): Boolean = success

    companion object {
        /**
         * Create a success history entry.
         */
        fun success(text: String, action: String, confidence: Float): VoiceCommandHistory {
            return VoiceCommandHistory(
                text = text,
                action = action,
                confidence = confidence,
                success = true
            )
        }

        /**
         * Create a failure history entry.
         */
        fun failure(text: String, confidence: Float): VoiceCommandHistory {
            return VoiceCommandHistory(
                text = text,
                action = "",
                confidence = confidence,
                success = false
            )
        }
    }
}

/**
 * Configuration for voice command recognition.
 */
data class VoiceCommandConfig(
    val wakeWord: String = "hey air mouse",
    val wakeWordConfidence: Float = 0.7f,
    val isEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val silenceTimeoutMs: Long = 2000L,
    val wakeWordTimeoutMs: Long = 10000L
) {

    /**
     * Check if the configuration is valid.
     */
    fun isValid(): Boolean {
        return wakeWord.isNotBlank() &&
                wakeWordConfidence in 0.5f..0.95f &&
                silenceTimeoutMs > 0 &&
                wakeWordTimeoutMs > 0
    }

    /**
     * Create a copy with a specific wake word.
     */
    fun withWakeWord(wakeWord: String): VoiceCommandConfig {
        return copy(wakeWord = wakeWord)
    }

    /**
     * Create a copy enabling/disabling voice commands.
     */
    fun withEnabled(enabled: Boolean): VoiceCommandConfig {
        return copy(isEnabled = enabled)
    }

    /**
     * Create a copy with a different confidence threshold.
     */
    fun withConfidence(confidence: Float): VoiceCommandConfig {
        return copy(wakeWordConfidence = confidence.coerceIn(0.5f, 0.95f))
    }

    companion object {
        /**
         * Default configuration for voice commands.
         */
        fun default(): VoiceCommandConfig {
            return VoiceCommandConfig()
        }

        /**
         * Configuration optimized for quiet environments.
         */
        fun quiet(): VoiceCommandConfig {
            return VoiceCommandConfig(
                wakeWordConfidence = 0.6f,
                silenceTimeoutMs = 1500L
            )
        }

        /**
         * Configuration optimized for noisy environments.
         */
        fun noisy(): VoiceCommandConfig {
            return VoiceCommandConfig(
                wakeWordConfidence = 0.8f,
                wakeWordTimeoutMs = 15000L
            )
        }
    }
}