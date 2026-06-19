// app/src/main/java/com/airmouse/domain/repository/IGestureRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import kotlinx.coroutines.flow.Flow

interface IGestureRepository {
    // Gesture detection
    suspend fun detectGesture(sensorData: FloatArray): GestureEvent
    suspend fun detectGestureFromMotion(dx: Float, dy: Float): GestureType

    // Custom gestures
    suspend fun addCustomGesture(gesture: CustomGestureTemplate): String
    suspend fun updateCustomGesture(gesture: CustomGestureTemplate)
    suspend fun deleteCustomGesture(id: String)
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>
    fun observeCustomGestures(): Flow<List<CustomGestureTemplate>>

    // Training
    suspend fun trainGesture(gestureName: String, samples: List<FloatArray>): Boolean
    suspend fun trainAllGestures(): Boolean

    // Statistics
    suspend fun getGestureStats(): GestureTrainingStats
    fun observeGestureStats(): Flow<GestureTrainingStats>

    // Templates
    suspend fun loadGestureTemplates(): List<CustomGestureTemplate>
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun deleteGestureTemplate(id: String)

    // Recognition configuration
    suspend fun setConfidenceThreshold(threshold: Float)
    suspend fun getConfidenceThreshold(): Float
    suspend fun setCooldownMs(cooldown: Long)
    suspend fun getCooldownMs(): Long
}