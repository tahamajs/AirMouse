# 📘 Air Mouse Macros Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.macros` package provides **macro recording and playback functionality** for automating mouse actions, keyboard inputs, and complex sequences of commands. Users can record their actions, save them as macros, and play them back on demand.

```
com.airmouse.macros/
└── MacroRecorder.kt          # Macro recording, playback, and management
```

---

## 🎯 MacroRecorder

### Purpose
Provides **macro recording and playback** functionality for automating repetitive tasks. Users can record mouse movements, clicks, scrolls, key presses, and delays, then save them as macros for later playback.

### Key Responsibilities

| Responsibility | Description |
|----------------|-------------|
| **Recording** | Captures user actions (clicks, movements, scrolls, key presses) in real-time |
| **Playback** | Executes recorded macros with accurate timing |
| **Macro Management** | CRUD operations for macros (save, load, delete, export, import) |
| **Action Types** | Supports click, double-click, right-click, move, scroll, key press, type, delay, loops, conditions |
| **Smart Recording** | Removes redundant actions, optimizes sequences |
| **Export/Import** | Share macros via JSON files |
| **Reactive UI** | StateFlow for real-time macro list updates |

---

## 📦 Data Models

### Macro

```kotlin
data class Macro(
    val id: String,                     // Unique identifier
    val name: String,                   // Display name
    val actions: List<MacroAction>,     // Sequence of actions
    val createdAt: Long,                // Creation timestamp
    val modifiedAt: Long,               // Last modified timestamp
    val loopCount: Int = 1,             // Number of times to loop
    val delayBetweenLoops: Long = 0,    // Delay between loops (ms)
    val enabled: Boolean = true,        // Whether macro is enabled
    val hotkey: String? = null          // Optional hotkey trigger
)
```

### MacroAction (Sealed Class)

```kotlin
sealed class MacroAction {
    
    /** Mouse click at current or specific position */
    data class Click(val button: String, val x: Int? = null, val y: Int? = null) : MacroAction()
    
    /** Double click at current or specific position */
    data class DoubleClick(val x: Int? = null, val y: Int? = null) : MacroAction()
    
    /** Right click at current or specific position */
    data class RightClick(val x: Int? = null, val y: Int? = null) : MacroAction()
    
    /** Mouse movement delta */
    data class Move(val dx: Int, val dy: Int) : MacroAction()
    
    /** Scroll wheel delta */
    data class Scroll(val delta: Int) : MacroAction()
    
    /** Key press with optional modifiers */
    data class KeyPress(val keyCode: Int, val modifiers: List<String> = emptyList()) : MacroAction()
    
    /** Key down (hold) */
    data class KeyDown(val keyCode: Int) : MacroAction()
    
    /** Key up (release) */
    data class KeyUp(val keyCode: Int) : MacroAction()
    
    /** Type text (simulates keyboard typing) */
    data class Type(val text: String) : MacroAction()
    
    /** Delay in milliseconds */
    data class Delay(val milliseconds: Long) : MacroAction()
    
    /** Wait for a click event (user interaction) */
    data class WaitForClick(val timeout: Long = 30000) : MacroAction()
    
    /** Loop over a set of actions */
    data class Loop(val count: Int, val actions: List<MacroAction>) : MacroAction()
    
    /** Conditional execution based on pixel color */
    data class IfPixel(val x: Int, val y: Int, val color: Int, val then: List<MacroAction>) : MacroAction()
    
    /** Run another macro */
    data class RunMacro(val macroId: String) : MacroAction()
}
```

### Recording Mode

```kotlin
enum class RecordingMode {
    STANDARD,        // Records all actions with timing
    SMART,           // Removes redundant movements, optimizes
    GESTURE_ONLY     // Records only gesture actions
}
```

---

## 🔧 Key Methods

### 1. Recording

```kotlin
/**
 * Start recording a macro
 * @param mode The recording mode (STANDARD, SMART, GESTURE_ONLY)
 */
fun startRecording(mode: RecordingMode = RecordingMode.STANDARD) {
    recordedActions.clear()
    recordingStartTime = System.currentTimeMillis()
    lastActionTime = 0
    recordingMode = mode
    isRecording = true
    onRecordingStarted?.invoke()
    LogManager.info("Recording started", TAG)
}

/**
 * Record a single action
 * @param action The action to record
 */
fun recordAction(action: MacroAction) {
    if (!isRecording) return
    
    val now = System.currentTimeMillis()
    if (lastActionTime > 0) {
        // Insert delay if there was a gap between actions
        val delay = now - lastActionTime
        if (delay > 100) {
            recordedActions.add(MacroAction.Delay(delay))
        }
    }
    recordedActions.add(action)
    lastActionTime = now
    LogManager.debug("Recorded: $action", TAG)
}

/**
 * Stop recording and save the macro
 * @param name The name of the macro
 * @return The saved Macro object
 */
fun stopRecording(name: String): Macro {
    isRecording = false
    
    // Apply SMART optimization if enabled
    val optimizedActions = if (recordingMode == RecordingMode.SMART) {
        optimizeActions(recordedActions.toList())
    } else {
        recordedActions.toList()
    }
    
    val macro = Macro(
        id = UUID.randomUUID().toString(),
        name = name,
        actions = optimizedActions,
        createdAt = System.currentTimeMillis(),
        modifiedAt = System.currentTimeMillis()
    )
    
    saveMacro(macro)
    onRecordingStopped?.invoke(macro)
    LogManager.info("Recording stopped: ${macro.name}", TAG)
    
    return macro
}
```

### 2. Playback

```kotlin
/**
 * Play a macro
 * @param macro The macro to play
 */
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
        LogManager.info("Playback finished", TAG)
    }
}

/**
 * Execute a list of actions
 */
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
                    val cmd = buildKeyCommand(action)
                    connectionManager.send(cmd)
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
            LogManager.error("Error executing action $action: ${e.message}", TAG)
        }
    }
}
```

### 3. Macro Management (CRUD)

```kotlin
/**
 * Save a macro to storage
 */
fun saveMacro(macro: Macro) {
    val json = serializeMacro(macro)
    val file = File(context.filesDir, "$MACROS_DIR/${macro.id}.json")
    file.writeText(json)
    
    val updatedMacros = _macros.value.toMutableList()
    val index = updatedMacros.indexOfFirst { it.id == macro.id }
    if (index >= 0) {
        updatedMacros[index] = macro
    } else {
        updatedMacros.add(macro)
    }
    _macros.value = updatedMacros
}

/**
 * Delete a macro
 */
fun deleteMacro(macroId: String) {
    val file = File(context.filesDir, "$MACROS_DIR/$macroId.json")
    file.delete()
    _macros.value = _macros.value.filter { it.id != macroId }
}

/**
 * Load all macros from storage
 */
private fun loadMacros() {
    val macrosDir = File(context.filesDir, MACROS_DIR)
    if (!macrosDir.exists()) {
        macrosDir.mkdirs()
        createExampleMacros()
        return
    }
    
    val macroFiles = macrosDir.listFiles { file -> file.extension == "json" }
    val loadedMacros = macroFiles?.mapNotNull { file ->
        try {
            val json = JSONObject(file.readText())
            parseMacro(json)
        } catch (e: Exception) {
            LogManager.error("Failed to load macro: ${file.name}", TAG)
            null
        }
    } ?: emptyList()
    
    _macros.value = loadedMacros
}
```

### 4. Smart Optimization

```kotlin
/**
 * Optimize recorded actions (SMART mode)
 */
private fun optimizeActions(actions: List<MacroAction>): List<MacroAction> {
    val optimized = mutableListOf<MacroAction>()
    
    var i = 0
    while (i < actions.size) {
        val current = actions[i]
        
        // Remove consecutive delays
        if (current is MacroAction.Delay) {
            var totalDelay = current.milliseconds
            while (i + 1 < actions.size && actions[i + 1] is MacroAction.Delay) {
                totalDelay += (actions[i + 1] as MacroAction.Delay).milliseconds
                i++
            }
            optimized.add(MacroAction.Delay(totalDelay))
            i++
            continue
        }
        
        // Remove redundant moves
        if (current is MacroAction.Move && i + 1 < actions.size && actions[i + 1] is MacroAction.Move) {
            val moves = mutableListOf(current)
            while (i + 1 < actions.size && actions[i + 1] is MacroAction.Move) {
                moves.add(actions[i + 1] as MacroAction.Move)
                i++
            }
            // Combine moves
            var totalDx = moves.sumOf { it.dx }
            var totalDy = moves.sumOf { it.dy }
            if (totalDx != 0 || totalDy != 0) {
                optimized.add(MacroAction.Move(totalDx, totalDy))
            }
            i++
            continue
        }
        
        optimized.add(current)
        i++
    }
    
    return optimized
}
```

### 5. Export/Import

```kotlin
/**
 * Export a macro to a file
 */
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

/**
 * Import a macro from a file
 */
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
        LogManager.error("Failed to import macro", TAG)
        null
    }
}
```

---

## 🔄 Macro Execution Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MACRO EXECUTION FLOW                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User triggers macro (button click, hotkey, voice command)          │
│         │                                                               │
│         ▼                                                               │
│  2. playMacro(macro) is called                                         │
│         │                                                               │
│         ▼                                                               │
│  3. For each loop iteration:                                           │
│         │                                                               │
│         ├── For each action in actions:                               │
│         │   ├── Click → connectionManager.sendClick()                 │
│         │   ├── Move → connectionManager.sendMove()                   │
│         │   ├── Scroll → connectionManager.sendScroll()               │
│         │   ├── KeyPress → connectionManager.send()                   │
│         │   ├── Type → connectionManager.send() per character         │
│         │   ├── Delay → coroutine delay()                             │
│         │   ├── Loop → recursive execution                           │
│         │   ├── WaitForClick → suspend until user clicks             │
│         │   ├── IfPixel → conditional branching                       │
│         │   └── RunMacro → execute another macro                     │
│         │                                                               │
│         └── After loop, check if more iterations are needed           │
│                                                                         │
│  4. onPlaybackStopped callback is invoked                              │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Example Macros

### Auto Clicker Macro
```kotlin
Macro(
    name = "Auto Clicker",
    actions = listOf(
        MacroAction.Click("left"),
        MacroAction.Delay(100),
        MacroAction.Loop(10, listOf(
            MacroAction.Click("left"),
            MacroAction.Delay(50)
        ))
    ),
    loopCount = 1
)
```

### Login Sequence Macro
```kotlin
Macro(
    name = "Login Sequence",
    actions = listOf(
        MacroAction.Type("username"),
        MacroAction.Delay(500),
        MacroAction.KeyPress(KeyEvent.KEYCODE_TAB),
        MacroAction.Delay(500),
        MacroAction.Type("password"),
        MacroAction.Delay(500),
        MacroAction.KeyPress(KeyEvent.KEYCODE_ENTER)
    )
)
```

### Scrolling Test Macro
```kotlin
Macro(
    name = "Scrolling Test",
    actions = listOf(
        MacroAction.Scroll(5),
        MacroAction.Delay(100),
        MacroAction.Scroll(-5)
    )
)
```

---

## 📋 Public API Summary

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `startRecording(mode)` | Start recording a macro | `fun: Unit` |
| `recordAction(action)` | Record a single action | `fun: Unit` |
| `stopRecording(name)` | Stop recording and save macro | `fun: Macro` |
| `playMacro(macro)` | Play a macro | `fun: Unit` |
| `stopPlayback()` | Stop macro playback | `fun: Unit` |
| `saveMacro(macro)` | Save a macro | `fun: Unit` |
| `deleteMacro(macroId)` | Delete a macro | `fun: Unit` |
| `exportMacro(macroId)` | Export macro to file | `fun: File` |
| `importMacro(file)` | Import macro from file | `fun: Macro?` |
| `isRecording()` | Check if recording | `fun: Boolean` |
| `isPlayingBack()` | Check if playing | `fun: Boolean` |
| `macros` (StateFlow) | All saved macros | `StateFlow<List<Macro>>` |

### Callbacks

```kotlin
var onRecordingStarted: (() -> Unit)? = null
var onRecordingStopped: ((Macro) -> Unit)? = null
var onPlaybackStarted: (() -> Unit)? = null
var onPlaybackStopped: (() -> Unit)? = null
var onActionExecuted: ((MacroAction) -> Unit)? = null
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Action Recording** | Real-time recording of all user actions |
| **Smart Optimization** | Removes redundant actions in SMART mode |
| **Sequential Playback** | Accurate execution with timing |
| **Nested Actions** | Supports loops and conditional execution |
| **Macro Management** | Full CRUD with persistence |
| **Export/Import** | Share macros as JSON files |
| **Reactive Updates** | StateFlow for UI updates |
| **Error Handling** | Graceful error recovery |

---

**The MacroRecorder provides powerful automation capabilities, allowing users to record, save, and play back complex sequences of actions, making repetitive tasks effortless.**