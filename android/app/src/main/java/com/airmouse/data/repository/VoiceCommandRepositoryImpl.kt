package com.airmouse.data.repository

import com.airmouse.domain.model.VoiceCommand
import com.airmouse.domain.repository.IVoiceCommandRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _commands = MutableStateFlow<List<VoiceCommand>>(emptyList())
    override val commands: StateFlow<List<VoiceCommand>> = _commands.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastCommand = MutableStateFlow<String?>(null)
    override val lastCommand: StateFlow<String?> = _lastCommand.asStateFlow()

    init {
        loadCommands()
    }

    private fun loadCommands() {
        val json = prefs.getString("voice_commands", "")
        if (json.isNotEmpty()) {
            try {
                val array = JSONArray(json)
                val list = mutableListOf<VoiceCommand>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(VoiceCommand(
                        id = obj.getString("id"),
                        phrase = obj.getString("phrase"),
                        action = obj.getString("action"),
                        isCustom = obj.optBoolean("isCustom", false),
                        enabled = obj.optBoolean("enabled", true),
                        confidenceThreshold = obj.optDouble("confidenceThreshold", 0.7).toFloat()
                    ))
                }
                _commands.value = list
            } catch (e: Exception) {
                _commands.value = getDefaultCommands()
            }
        } else {
            _commands.value = getDefaultCommands()
        }
    }

    private fun getDefaultCommands(): List<VoiceCommand> {
        return listOf(
            VoiceCommand("click", "click", "click"),
            VoiceCommand("double_click", "double click", "doubleClick"),
            VoiceCommand("right_click", "right click", "rightClick"),
            VoiceCommand("scroll_up", "scroll up", "scrollUp", delta = 3),
            VoiceCommand("scroll_down", "scroll down", "scrollDown", delta = -3),
            VoiceCommand("play_pause", "play pause", "playPause"),
            VoiceCommand("next_track", "next", "nextTrack"),
            VoiceCommand("prev_track", "previous", "prevTrack"),
            VoiceCommand("volume_up", "volume up", "volumeUp"),
            VoiceCommand("volume_down", "volume down", "volumeDown"),
            VoiceCommand("mute", "mute", "mute"),
            VoiceCommand("lock_screen", "lock screen", "lockScreen"),
            VoiceCommand("calibrate", "calibrate", "calibrate"),
            VoiceCommand("show_stats", "show stats", "showStats"),
            VoiceCommand("help", "help", "help")
        )
    }

    private fun saveCommands() {
        val array = JSONArray()
        _commands.value.forEach { cmd ->
            val obj = JSONObject().apply {
                put("id", cmd.id)
                put("phrase", cmd.phrase)
                put("action", cmd.action)
                put("isCustom", cmd.isCustom)
                put("enabled", cmd.enabled)
                put("confidenceThreshold", cmd.confidenceThreshold)
            }
            array.put(obj)
        }
        prefs.putString("voice_commands", array.toString())
    }

    override suspend fun addCommand(command: VoiceCommand) {
        _commands.update { it + command.copy(isCustom = true) }
        saveCommands()
    }

    override suspend fun removeCommand(id: String) {
        _commands.update { it.filter { it.id != id } }
        saveCommands()
    }

    override suspend fun toggleCommand(id: String) {
        _commands.update { list ->
            list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        }
        saveCommands()
    }

    override suspend fun setListening(enabled: Boolean) {
        _isListening.value = enabled
    }

    override suspend fun processVoiceInput(text: String): VoiceCommand? {
        val lowerText = text.lowercase().trim()
        val matched = _commands.value.find { cmd ->
            cmd.enabled && (lowerText.contains(cmd.phrase.lowercase()) ||
                    lowerText == cmd.phrase.lowercase())
        }
        _lastCommand.value = matched?.action
        matched?.let { addToHistory(it.phrase) }
        return matched
    }

    override suspend fun getCommandHistory(): List<String> {
        val history = prefs.getString("voice_history", "")
        return if (history.isNotEmpty()) history.split("|") else emptyList()
    }

    override suspend fun addToHistory(command: String) {
        val history = prefs.getString("voice_history", "")
        val newHistory = (listOf(command) + history.split("|").take(49)).joinToString("|")
        prefs.putString("voice_history", newHistory)
    }

    override suspend fun clearHistory() {
        prefs.putString("voice_history", "")
    }

    override suspend fun getSupportedCommands(): List<VoiceCommand> = _commands.value

    override suspend fun updateCommand(command: VoiceCommand) {
        _commands.update { list ->
            list.map { if (it.id == command.id) command else it }
        }
        saveCommands()
    }
}