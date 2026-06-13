// app/src/main/java/com/airmouse/domain/repository/IGestureRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.CustomGestureTemplate
import kotlinx.coroutines.flow.Flow

interface IGestureRepository {
    suspend fun getAllCustomGestures(): List<CustomGestureTemplate>
    suspend fun getCustomGesture(id: String): CustomGestureTemplate?
    suspend fun saveCustomGesture(gesture: CustomGestureTemplate)
    suspend fun deleteCustomGesture(id: String)
    suspend fun trainGesture(gestureId: String, newSamples: List<FloatArray>? = null): Boolean
    suspend fun recognizeGesture(samples: List<FloatArray>): Pair<String?, Float> // returns (gestureName, confidence)
    suspend fun exportGesturesToCSV(): String
    suspend fun importGesturesFromCSV(filePath: String): Boolean
    fun observeGestures(): Flow<List<CustomGestureTemplate>>
}