// app/src/main/java/com/airmouse/data/repository/MouseRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.repository.IMouseRepository
import com.airmouse.network.WebSocketManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MouseRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IMouseRepository {

    private val _eventHistory = MutableStateFlow<List<MouseEvent>>(emptyList())
    private val maxHistorySize = 100
    private var batchMode = false
    private val batchBuffer = mutableListOf<MouseEvent>()

    override suspend fun sendMove(dx: Float, dy: Float) {
        val sensitivity = prefs.getSensitivity()
        var finalDx = dx * sensitivity
        var finalDy = dy * sensitivity

        if (prefs.getBoolean("invert_x", false)) finalDx = -finalDx
        if (prefs.getBoolean("invert_y", false)) finalDy = -finalDy

        WebSocketManager.sendMove(finalDx, finalDy)
        addToHistory(MouseEvent("move", data = mapOf("dx" to finalDx, "dy" to finalDy)))
    }

    override suspend fun sendClick() {
        WebSocketManager.sendClick("left")
        addToHistory(MouseEvent("click", data = mapOf("button" to "left")))
    }

    override suspend fun sendDoubleClick() {
        WebSocketManager.sendDoubleClick()
        addToHistory(MouseEvent("doubleClick"))
    }

    override suspend fun sendRightClick() {
        WebSocketManager.sendClick("right")
        addToHistory(MouseEvent("rightClick"))
    }

    override suspend fun sendScroll(delta: Int) {
        WebSocketManager.sendScroll(delta)
        addToHistory(MouseEvent("scroll", data = mapOf("delta" to delta)))
    }

    override suspend fun sendGesture(gesture: String, confidence: Float) {
        WebSocketManager.sendGesture(gesture, confidence)
        addToHistory(MouseEvent("gesture", data = mapOf("gesture" to gesture, "confidence" to confidence)))
    }

    override suspend fun sendProximity(isNear: Boolean, distance: Float) {
        WebSocketManager.sendProximity(isNear, distance)
        addToHistory(MouseEvent("proximity", data = mapOf("isNear" to isNear, "distance" to distance)))
    }

    override suspend fun sendControl(command: String) {
        WebSocketManager.sendControl(command)
        addToHistory(MouseEvent("control", data = mapOf("command" to command)))
    }

    override fun getEventHistory(): Flow<List<MouseEvent>> = _eventHistory.asStateFlow()

    override suspend fun clearEventHistory() {
        _eventHistory.update { emptyList() }
    }

    override suspend fun setBatchMode(enabled: Boolean) {
        batchMode = enabled
        if (!batchMode && batchBuffer.isNotEmpty()) {
            flushBatch()
        }
    }

    override suspend fun flushBatch() {
        if (batchBuffer.isEmpty()) return

        // Process batch (in real app, would send compressed batch)
        batchBuffer.clear()
    }

    private fun addToHistory(event: MouseEvent) {
        if (batchMode) {
            batchBuffer.add(event)
            if (batchBuffer.size >= 10) {
                flushBatch()
            }
        } else {
            _eventHistory.update { history ->
                (listOf(event) + history).take(maxHistorySize)
            }
        }
    }
}