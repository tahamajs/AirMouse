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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IVoiceCommandRepository {

    private val _commands = MutableStateFlow(loadCommands())
    private val _listening = MutableStateFlow(false)
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    private val _history = MutableStateFlow(loadHistory())
    private val _config = MutableStateFlow(loadConfig())

    override suspend fun getCommands(): List<VoiceCommand> = _commands.value
    override suspend fun getCommand(id: String): VoiceCommand? = _commands.value.find { it.id == id }
    override fun observeCommands(): Flow<List<VoiceCommand>> = _commands.asStateFlow()

    override suspend fun addCommand(command: VoiceCommand) {
        _commands.update { it + command }
        saveCommands()
    }

    override suspend fun updateCommand(command: VoiceCommand) {
        _commands.update { list -> list.map { if (it.id == command.id) command else it } }
        saveCommands()
    }

    override suspend fun deleteCommand(id: String) {
        _commands.update { it.filterNot { cmd -> cmd.id == id } }
        saveCommands()
    }

    override suspend fun toggleCommand(id: String, enabled: Boolean) {
        _commands.update { list -> list.map { if (it.id == id) it.copy(isEnabled = enabled) else it } }
        saveCommands()
    }

    override suspend fun startListening() {
        _listening.value = true
    }

    override suspend fun stopListening() {
        _listening.value = false
    }

    override suspend fun isListening(): Boolean = _listening.value

    override suspend fun setListening(enabled: Boolean) {
        _listening.value = enabled
    }

    override suspend fun processVoiceInput(text: String): VoiceCommand? {
        val normalized = text.lowercase().trim()
        val matched = _commands.value.firstOrNull { cmd ->
            cmd.isEnabled && normalized.contains(cmd.text.lowercase())
        }
        _lastCommand.value = matched
        matched?.let {
            _history.update { history ->
                val item = VoiceCommandHistory(text = text, action = it.action, confidence = it.confidence, success = true)
                (listOf(item) + history).take(100)
            }
            saveHistory()
        }
        return matched
    }

    override suspend fun getLastCommand(): VoiceCommand? = _lastCommand.value
    override fun observeLastCommand(): Flow<VoiceCommand?> = _lastCommand.asStateFlow()

    override suspend fun getCommandHistory(): List<VoiceCommandHistory> = _history.value
    override suspend fun addToHistory(history: VoiceCommandHistory) {
        _history.update { listOf(history) + it.take(99) }
        saveHistory()
    }

    override suspend fun clearHistory() {
        _history.value = emptyList()
        saveHistory()
    }

    override suspend fun getConfig(): VoiceCommandConfig = _config.value
    override suspend fun updateConfig(config: VoiceCommandConfig) {
        _config.value = config
        saveConfig()
    }

    override suspend fun getSupportedCommands(): List<String> = _commands.value.map { it.text }

    private fun loadCommands(): List<VoiceCommand> {
        val json = prefs.getString("voice_commands", "")
        if (json.isEmpty()) {
            return defaultCommands()
        }
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VoiceCommand(
                            id = o.optString("id"),
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
        }.getOrElse { defaultCommands() }
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
    }

    private fun loadHistory(): List<VoiceCommandHistory> = emptyList()
    private fun saveHistory() {}

    private fun loadConfig(): VoiceCommandConfig = VoiceCommandConfig(
        wakeWord = prefs.getWakeWord(),
        wakeWordConfidence = prefs.getVoiceWakeWordConfidence(),
        isEnabled = prefs.isVoiceEnabled(),
        hapticFeedback = prefs.isVoiceFeedbackEnabled()
    )

    private fun saveConfig() {
        prefs.setWakeWord(_config.value.wakeWord)
        prefs.setVoiceWakeWordConfidence(_config.value.wakeWordConfidence)
        prefs.setVoiceEnabled(_config.value.isEnabled)
        prefs.setVoiceFeedbackEnabled(_config.value.hapticFeedback)
    }

    private fun defaultCommands(): List<VoiceCommand> = listOf(
        VoiceCommand(id = "click", text = "click", action = "click"),
        VoiceCommand(id = "double_click", text = "double click", action = "doubleClick"),
        VoiceCommand(id = "right_click", text = "right click", action = "rightClick"),
        VoiceCommand(id = "scroll_up", text = "scroll up", action = "scrollUp"),
        VoiceCommand(id = "scroll_down", text = "scroll down", action = "scrollDown")
    )
}
