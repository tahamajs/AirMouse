package com.airmouse.domain.repository

import com.airmouse.domain.model.MouseEvent
import kotlinx.coroutines.flow.Flow

interface IMouseRepository {
    suspend fun sendMove(dx: Float, dy: Float)
    suspend fun sendClick()
    suspend fun sendDoubleClick()
    suspend fun sendRightClick()
    suspend fun sendScroll(delta: Int)
    suspend fun sendGesture(gesture: String, confidence: Float)
    suspend fun sendProximity(isNear: Boolean, distance: Float)
    suspend fun sendControl(command: String)
    fun getEventHistory(): Flow<List<MouseEvent>>
    suspend fun clearEventHistory()
    suspend fun setBatchMode(enabled: Boolean)
    suspend fun flushBatch()
}
