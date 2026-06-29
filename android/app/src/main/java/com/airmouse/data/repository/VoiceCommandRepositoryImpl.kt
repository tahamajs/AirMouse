package com.airmouse.data.repository

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.model.VoiceCommandConfig
import com.airmouse.domain.model.VoiceCommandHistory
import com.airmouse.domain.repository.IVoiceCommandRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IVoiceCommandRepository {

    private val MAX_HISTORY_SIZE = 100

    // ============================================================
    // State Flows
    // ============================================================

    private val _commands = MutableStateFlow(loadCommands())
    private val _listening = MutableStateFlow(false)
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    private val _history = MutableStateFlow(loadHistory())
    private val _config = MutableStateFlow(loadConfig())

    // ============================================================
    // Command CRUD
    // ============================================================

    override suspend fun getCommands(): List<VoiceCommand> = _commands.value

    override suspend fun getCommand(id: String): VoiceCommand? {
        return _commands.value.find { it.id == id }
    }

    override fun observeCommands(): Flow<List<VoiceCommand>> = _commands.asStateFlow()

    override suspend fun addCommand(command: VoiceCommand) {
        val newCommand = command.copy(id = UUID.randomUUID().toString())
        _commands.update { it + newCommand }
        saveCommands()
        Timber.d("Voice command added: ${newCommand.text} -> ${newCommand.action}")
    }

    override suspend fun updateCommand(command: VoiceCommand) {
        _commands.update { list ->
            list.map { if (it.id == command.id) command else it }
        }
        saveCommands()
        Timber.d("Voice command updated: ${command.text} -> ${command.action}")
    }

    override suspend fun deleteCommand(id: String) {
        val deleted = _commands.value.find { it.id == id }
        _commands.update { it.filterNot { cmd -> cmd.id == id } }
        saveCommands()
        Timber.d("Voice command deleted: ${deleted?.text}")
    }

    override suspend fun toggleCommand(id: String, enabled: Boolean) {
        _commands.update { list ->
            list.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
        }
        saveCommands()
        Timber.d("Voice command toggled: $id -> enabled=$enabled")
    }

    // ============================================================
    // Listening State
    // ============================================================

    override suspend fun startListening() {
        _listening.value = true
        Timber.d("Voice listening started")
    }

    override suspend fun stopListening() {
        _listening.value = false
        Timber.d("Voice listening stopped")
    }

    override suspend fun isListening(): Boolean = _listening.value

    override suspend fun setListening(enabled: Boolean) {
        _listening.value = enabled
        Timber.d("Voice listening set to: $enabled")
    }

    // ============================================================
    // Voice Processing
    // ============================================================

    override suspend fun processVoiceInput(text: String): VoiceCommand? {
        val normalized = text.lowercase().trim()
        Timber.d("Processing voice input: '$text'")

        // Find matching command
        val matched = _commands.value.firstOrNull { cmd ->
            cmd.isEnabled && normalized.contains(cmd.text.lowercase())
        }

        _lastCommand.value = matched

        if (matched != null) {
            val history = VoiceCommandHistory(
                text = text,
                action = matched.action,
                confidence = matched.confidence,
                success = true,
                timestamp = System.currentTimeMillis()
            )
            addToHistory(history)
            Timber.d("Voice command matched: ${matched.text} -> ${matched.action}")
        } else {
            val history = VoiceCommandHistory(
                text = text,
                action = "unknown",
                confidence = 0f,
                success = false,
                timestamp = System.currentTimeMillis()
            )
            addToHistory(history)
            Timber.d("Voice command not recognized: '$text'")
        }

        return matched
    }

    override suspend fun getLastCommand(): VoiceCommand? = _lastCommand.value

    override fun observeLastCommand(): Flow<VoiceCommand?> = _lastCommand.asStateFlow()

    // ============================================================
    // History Management
    // ============================================================

    override suspend fun getCommandHistory(): List<VoiceCommandHistory> = _history.value

    override suspend fun addToHistory(history: VoiceCommandHistory) {
        _history.update { list ->
            (listOf(history) + list).take(MAX_HISTORY_SIZE)
        }
        saveHistory()
    }

    override suspend fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
        Timber.d("Voice command history cleared")
    }

    // ============================================================
    // Configuration
    // ============================================================

    override suspend fun getConfig(): VoiceCommandConfig = _config.value

    override suspend fun updateConfig(config: VoiceCommandConfig) {
        _config.value = config
        saveConfig()
        Timber.d("Voice config updated: wakeWord=${config.wakeWord}, enabled=${config.isEnabled}")
    }

    override suspend fun getSupportedCommands(): List<String> {
        return _commands.value.map { it.text }
    }

    // ============================================================
    // Private Helpers - Commands
    // ============================================================

    private fun loadCommands(): List<VoiceCommand> {
        val json = prefs.getString("voice_commands", "")
        if (json.isEmpty()) {
            Timber.d("No voice commands found, loading defaults")
            return defaultCommands()
        }

        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VoiceCommand(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            text = o.optString("text"),
                            action = o.optString("action"),
                            confidence = o.optDouble("confidence", 0.7).toFloat(),
                            isEnabled = o.optBoolean("isEnabled", true),
                            isCustom = o.optBoolean("isCustom", false),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrElse { e ->
            Timber.e(e, "Failed to load voice commands, using defaults")
            defaultCommands()
        }
    }

    private fun saveCommands() {
        val arr = JSONArray()
        _commands.value.forEach { cmd ->
            arr.put(JSONObject().apply {
                put("id", cmd.id)
                put("text", cmd.text)
                put("action", cmd.action)
                put("confidence", cmd.confidence)
                put("isEnabled", cmd.isEnabled)
                put("isCustom", cmd.isCustom)
                put("createdAt", cmd.createdAt)
            })
        }
        prefs.putString("voice_commands", arr.toString())
        Timber.d("Saved ${_commands.value.size} voice commands")
    }

    private fun defaultCommands(): List<VoiceCommand> {
        return listOf(
            VoiceCommand(id = UUID.randomUUID().toString(), text = "click", action = "click", confidence = 0.9f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "double click", action = "doubleClick", confidence = 0.85f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "right click", action = "rightClick", confidence = 0.85f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "scroll up", action = "scrollUp", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "scroll down", action = "scrollDown", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "left click", action = "click", confidence = 0.9f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "select", action = "click", confidence = 0.85f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "back", action = "back", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "home", action = "home", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "calibrate", action = "calibrate", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "connect", action = "connect", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "disconnect", action = "disconnect", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "stop", action = "stop", confidence = 0.8f),
            VoiceCommand(id = UUID.randomUUID().toString(), text = "start", action = "start", confidence = 0.8f)
        )
    }

    // ============================================================
    // Private Helpers - History
    // ============================================================

    private fun loadHistory(): List<VoiceCommandHistory> {
        val json = prefs.getString("voice_command_history", "")
        if (json.isEmpty()) return emptyList()

        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VoiceCommandHistory(
                            text = o.optString("text"),
                            action = o.optString("action"),
                            confidence = o.optDouble("confidence", 0.0).toFloat(),
                            success = o.optBoolean("success", false),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveHistory() {
        val arr = JSONArray()
        _history.value.forEach { history ->
            arr.put(JSONObject().apply {
                put("text", history.text)
                put("action", history.action)
                put("confidence", history.confidence)
                put("success", history.success)
                put("timestamp", history.timestamp)
            })
        }
        prefs.putString("voice_command_history", arr.toString())
        Timber.d("Saved ${_history.value.size} voice history entries")
    }

    // ============================================================
    // Private Helpers - Configuration
    // ============================================================

    private fun loadConfig(): VoiceCommandConfig {
        return VoiceCommandConfig(
            wakeWord = prefs.getString("wake_word", "Hey Air Mouse"),
            wakeWordConfidence = prefs.getFloat("voice_wake_word_confidence", 0.7f),
            isEnabled = prefs.getBoolean("voice_enabled", false),
            hapticFeedback = prefs.getBoolean("voice_feedback", true),
            silenceTimeoutMs = prefs.getLong("voice_silence_timeout", 2000L),
            wakeWordTimeoutMs = prefs.getLong("voice_wake_word_timeout", 10000L)
        )
    }

    private fun saveConfig() {
        val config = _config.value
        prefs.putString("wake_word", config.wakeWord)
        prefs.putFloat("voice_wake_word_confidence", config.wakeWordConfidence)
        prefs.putBoolean("voice_enabled", config.isEnabled)
        prefs.putBoolean("voice_feedback", config.hapticFeedback)
        prefs.putLong("voice_silence_timeout", config.silenceTimeoutMs)
        prefs.putLong("voice_wake_word_timeout", config.wakeWordTimeoutMs)
        Timber.d("Voice config saved")
    }
}
