package com.airmouse.domain.model

data class Macro(
    val id: String,
    val name: String,
    val actions: List<MacroAction>,
    val createdAt: Long,
    val modifiedAt: Long,
    val loopCount: Int = 1,
    val delayBetweenLoops: Long = 0,
    val enabled: Boolean = true,
    val hotkey: String? = null,
    val tags: List<String> = emptyList(),
    val category: String = "General",
    val lastUsed: Long = 0,
    val usageCount: Int = 0
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
    data class Loop(val count: Int, val actions: List<MacroAction>) : MacroAction()
    data class RunMacro(val macroId: String) : MacroAction()
}

sealed class MacroExecutionStatus {
    data object Idle : MacroExecutionStatus()
    data object Running : MacroExecutionStatus()
    data object Paused : MacroExecutionStatus()
    data object Completed : MacroExecutionStatus()
    data class Error(val message: String) : MacroExecutionStatus()
}
