package com.airmouse.macros

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MacroRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) {

    companion object {
        private const val TAG = "MacroRecorder"
        private const val MACROS_DIR = "macros"
    }

    data class Macro(
        val id: String,
        val name: String,
        val actions: List<MacroAction>,
        val createdAt: Long,
        val modifiedAt: Long,
        val loopCount: Int = 1,
        val delayBetweenLoops: Long = 0,
        val enabled: Boolean = true,
        val hotkey: String? = null
    )

    sealed class MacroAction {
        data class Click(val button: String, val x: Int? = null, val y: Int? = null) : MacroAction()
        data class DoubleClick(val x: Int? = null, val y: Int? = null) : MacroAction()
        data class RightClick(val x: Int? = null, val y: Int? = null) : MacroAction()
        data class Move(val dx: Int, val dy: Int) : MacroAction()
        data class Scroll(val delta: Int) : MacroAction()
        data class KeyPress(val keyCode: Int, val modifiers: List<String> = emptyList()) : MacroAction()
        data class KeyDown(val keyCode: Int) : MacroAction()
        data class KeyUp(val keyCode: Int) : MacroAction()
        data class Type(val text: String) : MacroAction()
        data class Delay(val milliseconds: Long) : MacroAction()
        data class WaitForClick(val timeout: Long = 30000) : MacroAction()
        data class Loop(val count: Int, val actions: List<MacroAction>) : MacroAction()
        data class IfPixel(val x: Int, val y: Int, val color: Int, val then: List<MacroAction>) : MacroAction()
        data class RunMacro(val macroId: String) : MacroAction()
    }

    enum class RecordingMode {
        STANDARD,    
        SMART,       
        GESTURE_ONLY 
    }

    private val _macros = MutableStateFlow<List<Macro>>(emptyList())
    val macros: StateFlow<List<Macro>> = _macros.asStateFlow()

    private var isRecording = false
    private var recordedActions = mutableListOf<MacroAction>()
    private var recordingStartTime = 0L
    private var recordingMode = RecordingMode.STANDARD
    private var lastActionTime = 0L

    private var isPlaying = false
    private var playJob: Job? = null
    private var currentMacroJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: ((Macro) -> Unit)? = null
    var onPlaybackStarted: (() -> Unit)? = null
    var onPlaybackStopped: (() -> Unit)? = null
    var onActionExecuted: ((MacroAction) -> Unit)? = null

    init {
        loadMacros()
    }

    private fun loadMacros() {
        val macrosDir = File(context.filesDir, MACROS_DIR)
        if (!macrosDir.exists()) {
            macrosDir.mkdirs()
            createExampleMacros()
            return
        }

        val macroFiles = macrosDir.listFiles { file -> file.extension == "json" }
        val loadedMacros = mutableListOf<Macro>()

        macroFiles?.forEach { file ->
            try {
                val json = JSONObject(file.readText())
                loadedMacros.add(parseMacro(json))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to load macro: ${file.name}", e)
            }
        }

        _macros.value = loadedMacros
    }

    private fun createExampleMacros() {
        val exampleMacros = listOf(
            Macro(
                id = UUID.randomUUID().toString(),
                name = "Auto Clicker",
                actions = listOf(
                    MacroAction.Click("left"),
                    MacroAction.Delay(100),
                    MacroAction.Loop(10, listOf(MacroAction.Click("left"), MacroAction.Delay(50)))
                ),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                loopCount = 1
            ),
            Macro(
                id = UUID.randomUUID().toString(),
                name = "Login Sequence",
                actions = listOf(
                    MacroAction.Type("username"),
                    MacroAction.Delay(500),
                    MacroAction.KeyPress(android.view.KeyEvent.KEYCODE_TAB),
                    MacroAction.Delay(500),
                    MacroAction.Type("password"),
                    MacroAction.Delay(500),
                    MacroAction.KeyPress(android.view.KeyEvent.KEYCODE_ENTER)
                ),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            ),
            Macro(
                id = UUID.randomUUID().toString(),
                name = "Scrolling Test",
                actions = listOf(
                    MacroAction.Scroll(5),
                    MacroAction.Delay(100),
                    MacroAction.Scroll(-5)
                ),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
        )

        exampleMacros.forEach { saveMacro(it) }
        _macros.value = exampleMacros
    }

    private fun parseMacro(json: JSONObject): Macro {
        val actionsArray = json.getJSONArray("actions")
        val actions = parseActions(actionsArray)

        return Macro(
            id = json.getString("id"),
            name = json.getString("name"),
            actions = actions,
            createdAt = json.getLong("createdAt"),
            modifiedAt = json.getLong("modifiedAt"),
            loopCount = json.optInt("loopCount", 1),
            delayBetweenLoops = json.optLong("delayBetweenLoops", 0),
            enabled = json.optBoolean("enabled", true),
            hotkey = json.optString("hotkey", null)
        )
    }

    private fun parseActions(array: JSONArray): List<MacroAction> {
        val actions = mutableListOf<MacroAction>()
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            val type = json.getString("type")

            when (type) {
                "click" -> {
                    actions.add(
                        MacroAction.Click(
                            button = json.getString("button"),
                            x = if (json.has("x")) json.getInt("x") else null,
                            y = if (json.has("y")) json.getInt("y") else null
                        )
                    )
                }
                "doubleclick" -> {
                    actions.add(
                        MacroAction.DoubleClick(
                            x = if (json.has("x")) json.getInt("x") else null,
                            y = if (json.has("y")) json.getInt("y") else null
                        )
                    )
                }
                "rightclick" -> {
                    actions.add(
                        MacroAction.RightClick(
                            x = if (json.has("x")) json.getInt("x") else null,
                            y = if (json.has("y")) json.getInt("y") else null
                        )
                    )
                }
                "move" -> {
                    actions.add(
                        MacroAction.Move(
                            dx = json.getInt("dx"),
                            dy = json.getInt("dy")
                        )
                    )
                }
                "scroll" -> {
                    actions.add(MacroAction.Scroll(delta = json.getInt("delta")))
                }
                "keypress" -> {
                    actions.add(
                        MacroAction.KeyPress(
                            keyCode = json.getInt("keyCode"),
                            modifiers = json.optJSONArray("modifiers")?.let { arr ->
                                (0 until arr.length()).map { arr.getString(it) }
                            } ?: emptyList()
                        )
                    )
                }
                "keydown" -> {
                    actions.add(MacroAction.KeyDown(keyCode = json.getInt("keyCode")))
                }
                "keyup" -> {
                    actions.add(MacroAction.KeyUp(keyCode = json.getInt("keyCode")))
                }
                "type" -> {
                    actions.add(MacroAction.Type(text = json.getString("text")))
                }
                "delay" -> {
                    actions.add(MacroAction.Delay(milliseconds = json.getLong("milliseconds")))
                }
                "waitclick" -> {
                    actions.add(MacroAction.WaitForClick(timeout = json.optLong("timeout", 30000)))
                }
                "loop" -> {
                    actions.add(
                        MacroAction.Loop(
                            count = json.getInt("count"),
                            actions = parseActions(json.getJSONArray("actions"))
                        )
                    )
                }
                "ifpixel" -> {
                    actions.add(
                        MacroAction.IfPixel(
                            x = json.getInt("x"),
                            y = json.getInt("y"),
                            color = json.getInt("color"),
                            then = parseActions(json.getJSONArray("then"))
                        )
                    )
                }
                "runmacro" -> {
                    actions.add(MacroAction.RunMacro(macroId = json.getString("macroId")))
                }
            }
        }
        return actions
    }

    fun saveMacro(macro: Macro) {
        val json = JSONObject().apply {
            put("id", macro.id)
            put("name", macro.name)
            put("actions", serializeActions(macro.actions))
            put("createdAt", macro.createdAt)
            put("modifiedAt", System.currentTimeMillis())
            put("loopCount", macro.loopCount)
            put("delayBetweenLoops", macro.delayBetweenLoops)
            put("enabled", macro.enabled)
            macro.hotkey?.let { put("hotkey", it) }
        }

        val file = File(context.filesDir, "$MACROS_DIR/${macro.id}.json")
        file.writeText(json.toString())

        val updatedMacros = _macros.value.toMutableList()
        val index = updatedMacros.indexOfFirst { it.id == macro.id }
        if (index >= 0) {
            updatedMacros[index] = macro
        } else {
            updatedMacros.add(macro)
        }
        _macros.value = updatedMacros
    }

    private fun serializeActions(actions: List<MacroAction>): JSONArray {
        val array = JSONArray()
        actions.forEach { action ->
            when (action) {
                is MacroAction.Click -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "click")
                            put("button", action.button)
                            action.x?.let { put("x", it) }
                            action.y?.let { put("y", it) }
                        }
                    )
                }
                is MacroAction.DoubleClick -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "doubleclick")
                            action.x?.let { put("x", it) }
                            action.y?.let { put("y", it) }
                        }
                    )
                }
                is MacroAction.RightClick -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "rightclick")
                            action.x?.let { put("x", it) }
                            action.y?.let { put("y", it) }
                        }
                    )
                }
                is MacroAction.Move -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "move")
                            put("dx", action.dx)
                            put("dy", action.dy)
                        }
                    )
                }
                is MacroAction.Scroll -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "scroll")
                            put("delta", action.delta)
                        }
                    )
                }
                is MacroAction.KeyPress -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "keypress")
                            put("keyCode", action.keyCode)
                            if (action.modifiers.isNotEmpty()) {
                                put("modifiers", JSONArray(action.modifiers))
                            }
                        }
                    )
                }
                is MacroAction.KeyDown -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "keydown")
                            put("keyCode", action.keyCode)
                        }
                    )
                }
                is MacroAction.KeyUp -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "keyup")
                            put("keyCode", action.keyCode)
                        }
                    )
                }
                is MacroAction.Type -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "type")
                            put("text", action.text)
                        }
                    )
                }
                is MacroAction.Delay -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "delay")
                            put("milliseconds", action.milliseconds)
                        }
                    )
                }
                is MacroAction.WaitForClick -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "waitclick")
                            put("timeout", action.timeout)
                        }
                    )
                }
                is MacroAction.Loop -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "loop")
                            put("count", action.count)
                            put("actions", serializeActions(action.actions))
                        }
                    )
                }
                is MacroAction.IfPixel -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "ifpixel")
                            put("x", action.x)
                            put("y", action.y)
                            put("color", action.color)
                            put("then", serializeActions(action.then))
                        }
                    )
                }
                is MacroAction.RunMacro -> {
                    array.put(
                        JSONObject().apply {
                            put("type", "runmacro")
                            put("macroId", action.macroId)
                        }
                    )
                }
            }
        }
        return array
    }

    fun deleteMacro(macroId: String) {
        val file = File(context.filesDir, "$MACROS_DIR/$macroId.json")
        file.delete()
        _macros.value = _macros.value.filter { it.id != macroId }
    }

    fun startRecording(mode: RecordingMode = RecordingMode.STANDARD) {
        recordedActions.clear()
        recordingStartTime = System.currentTimeMillis()
        lastActionTime = 0
        recordingMode = mode
        isRecording = true
        onRecordingStarted?.invoke()
        android.util.Log.i(TAG, "Recording started")
    }

    fun recordAction(action: MacroAction) {
        if (!isRecording) return

        val now = System.currentTimeMillis()
        if (lastActionTime > 0) {
            val delay = now - lastActionTime
            if (delay > 100) {
                recordedActions.add(MacroAction.Delay(delay))
            }
        }
        recordedActions.add(action)
        lastActionTime = now
        android.util.Log.d(TAG, "Recorded: $action")
    }

    fun stopRecording(name: String): Macro {
        isRecording = false

        val macro = Macro(
            id = UUID.randomUUID().toString(),
            name = name,
            actions = recordedActions.toList(),
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )

        saveMacro(macro)
        onRecordingStopped?.invoke(macro)
        android.util.Log.i(TAG, "Recording stopped: ${macro.name}")

        return macro
    }

    fun playMacro(macro: Macro) {
        if (isPlaying) {
            stopPlayback()
        }

        isPlaying = true
        onPlaybackStarted?.invoke()

        currentMacroJob = scope.launch {
            for (loop in 0 until macro.loopCount) {
                if (!isPlaying) break
                executeActions(macro.actions)
                if (loop < macro.loopCount - 1 && macro.delayBetweenLoops > 0) {
                    delay(macro.delayBetweenLoops)
                }
            }
            isPlaying = false
            onPlaybackStopped?.invoke()
            android.util.Log.i(TAG, "Playback finished")
        }
    }

    private suspend fun executeActions(actions: List<MacroAction>) {
        actions.forEach { action ->
            if (!isPlaying) return@forEach

            try {
                when (action) {
                    is MacroAction.Click -> {
                        connectionManager.sendClick(action.button)
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.DoubleClick -> {
                        connectionManager.sendDoubleClick()
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.RightClick -> {
                        connectionManager.sendRightClick()
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.Move -> {
                        connectionManager.sendMove(action.dx.toFloat(), action.dy.toFloat())
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.Scroll -> {
                        connectionManager.sendScroll(action.delta)
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.KeyPress -> {
                        
                        val modifiersStr = action.modifiers.joinToString("+")
                        val cmd = if (modifiersStr.isNotEmpty()) "$modifiersStr+${action.keyCode}" else action.keyCode.toString()
                        connectionManager.send("""{"type":"keypress","keycode":${action.keyCode},"modifiers":${JSONArray(action.modifiers)}}""")
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.KeyDown -> {
                        connectionManager.send("""{"type":"keydown","keycode":${action.keyCode}}""")
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.KeyUp -> {
                        connectionManager.send("""{"type":"keyup","keycode":${action.keyCode}}""")
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.Type -> {
                        
                        action.text.forEach { ch ->
                            connectionManager.send("""{"type":"type","char":"$ch"}""")
                            delay(50)
                        }
                        onActionExecuted?.invoke(action)
                    }
                    is MacroAction.Delay -> {
                        delay(action.milliseconds)
                    }
                    is MacroAction.WaitForClick -> {
                        waitForClick(action.timeout)
                    }
                    is MacroAction.Loop -> {
                        repeat(action.count) {
                            if (!isPlaying) return@repeat
                            executeActions(action.actions)
                        }
                    }
                    is MacroAction.IfPixel -> {
                        if (checkPixelColor(action.x, action.y, action.color)) {
                            executeActions(action.then)
                        }
                    }
                    is MacroAction.RunMacro -> {
                        val targetMacro = _macros.value.find { it.id == action.macroId }
                        targetMacro?.let { executeActions(it.actions) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error executing action $action", e)
                
            }
        }
    }

    fun stopPlayback() {
        isPlaying = false
        currentMacroJob?.cancel()
        playJob?.cancel()
        onPlaybackStopped?.invoke()
        android.util.Log.i(TAG, "Playback stopped")
    }

    
    private suspend fun waitForClick(timeout: Long) {
        suspendCoroutine<Unit> { continuation ->
            val timeoutRunnable = Runnable {
                continuation.resume(Unit)
            }
            val clickListener = object {
                fun onClick() {
                    handler.removeCallbacks(timeoutRunnable)
                    continuation.resume(Unit)
                }
            }
            handler.postDelayed(timeoutRunnable, timeout)
            
            
        }
    }

    private suspend fun checkPixelColor(x: Int, y: Int, color: Int): Boolean {
        
        return false
    }

    fun isRecording(): Boolean = isRecording
    fun isPlayingBack(): Boolean = isPlaying

    fun exportMacro(macroId: String): File {
        val macro = _macros.value.find { it.id == macroId }
        val exportFile = File(context.getExternalFilesDir(null), "macro_${macro?.name ?: "export"}.json")
        macro?.let {
            val json = JSONObject().apply {
                put("name", it.name)
                put("actions", serializeActions(it.actions))
                put("loopCount", it.loopCount)
            }
            exportFile.writeText(json.toString())
        }
        return exportFile
    }

    fun importMacro(file: File): Macro? {
        return try {
            val json = JSONObject(file.readText())
            val macro = Macro(
                id = UUID.randomUUID().toString(),
                name = json.getString("name"),
                actions = parseActions(json.getJSONArray("actions")),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                loopCount = json.optInt("loopCount", 1)
            )
            saveMacro(macro)
            macro
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to import macro", e)
            null
        }
    }

    fun cleanup() {
        stopPlayback()
        scope.cancel()
    }
}
