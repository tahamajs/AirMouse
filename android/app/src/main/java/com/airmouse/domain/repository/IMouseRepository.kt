
package com.airmouse.domain.repository

import com.airmouse.domain.model.MouseButton
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import kotlinx.coroutines.flow.Flow

interface IMouseRepository {
    
    suspend fun move(dx: Float, dy: Float): Boolean
    suspend fun moveSmooth(points: List<Pair<Float, Float>>, durationMs: Int): Boolean
    suspend fun stopMovement()
    suspend fun resumeMovement()

    
    suspend fun click(button: MouseButton): Boolean
    suspend fun doubleClick(): Boolean
    suspend fun rightClick(): Boolean
    suspend fun middleClick(): Boolean

    
    suspend fun scroll(delta: Int): Boolean
    suspend fun sendGesture(gesture: String, confidence: Float): Boolean

    
    suspend fun getPosition(): Pair<Int, Int>
    suspend fun setPosition(x: Int, y: Int): Boolean

    
    suspend fun getMovementProfile(): MovementProfile
    suspend fun setMovementProfile(profile: MovementProfile)
    fun observeMovementProfile(): Flow<MovementProfile>

    
    suspend fun getStatistics(): MouseStatistics
    fun observeStatistics(): Flow<MouseStatistics>
    suspend fun resetStatistics()

    
    fun observeMouseEvents(): Flow<MouseEvent>
    suspend fun clearEvents()
}
