// app/src/main/java/com/airmouse/domain/repository/IGestureRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.Gesture
import kotlinx.coroutines.flow.Flow

interface IGestureRepository {
    suspend fun saveCustomGesture(template: CustomGestureTemplate)
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>
    suspend fun deleteCustomGesture(id: String)
    suspend fun updateGestureThreshold(id: String, threshold: Float)
    fun observeGestures(): Flow<Gesture>
}