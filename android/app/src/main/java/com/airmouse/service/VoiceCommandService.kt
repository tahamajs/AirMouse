package com.airmouse.service

import android.content.Context
import android.util.Log
import com.airmouse.ConnectionManager

/**
 * Lightweight voice-command helper kept for the app's optional voice surface.
 *
 * The full offline speech stack was intentionally removed from the build so the
 * assignment core can compile and ship reliably in this environment. The helper
 * still keeps the same command vocabulary and can be extended later with a real
 * recognizer without changing the UI layer.
 */
class VoiceCommandService(
    private val context: Context,
    private val connectionManager: ConnectionManager
) {

    private var isListening = false

    companion object {
        private const val TAG = "VoiceCommandService"
        private val COMMANDS = arrayOf(
            "click",
            "double click",
            "right click",
            "scroll up",
            "scroll down",
            "stop listening"
        )
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        Log.i(TAG, "Voice commands enabled in stub mode for ${context.packageName}")
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        Log.i(TAG, "Voice commands disabled")
    }

    fun isListening(): Boolean = isListening

    fun supportedCommands(): List<String> = COMMANDS.toList()

    fun handleCommand(command: String) {
        when {
            command.equals("click", ignoreCase = true) -> connectionManager.sendClick()
            command.equals("double click", ignoreCase = true) -> connectionManager.sendDoubleClick()
            command.equals("right click", ignoreCase = true) -> connectionManager.sendRightClick()
            command.equals("scroll up", ignoreCase = true) -> connectionManager.sendScroll(1)
            command.equals("scroll down", ignoreCase = true) -> connectionManager.sendScroll(-1)
            command.equals("stop listening", ignoreCase = true) -> stopListening()
            else -> Log.w(TAG, "Unsupported command: $command")
        }
    }
}
