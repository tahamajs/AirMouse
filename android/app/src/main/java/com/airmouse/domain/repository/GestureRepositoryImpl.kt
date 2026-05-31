// app/src/main/java/com/airmouse/data/repository/GestureRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.data.local.GestureDao
import com.airmouse.data.local.PreferencesManager
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.Gesture
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.repository.IGestureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class GestureRepositoryImpl @Inject constructor(
    private val dao: GestureDao,
    private val prefs: PreferencesManager
) : IGestureRepository {

    private val _gestureFlow = MutableSharedFlow<Gesture>()
    override fun observeGestures(): Flow<Gesture> = _gestureFlow.asSharedFlow()

    suspend fun emitGesture(gesture: Gesture) {
        _gestureFlow.emit(gesture)
    }

    override suspend fun saveCustomGesture(template: CustomGestureTemplate) {
        dao.insertCustomGesture(template)
    }

    override suspend fun getCustomGesture(id: String): CustomGestureTemplate? {
        return dao.getCustomGesture(id)
    }

    override suspend fun getAllCustomGestures(): List<CustomGestureTemplate> {
        return dao.getAllCustomGestures()
    }

    override suspend fun deleteCustomGesture(id: String) {
        dao.deleteCustomGesture(id)
    }

    override suspend fun updateGestureThreshold(id: String, threshold: Float) {
        dao.updateGestureThreshold(id, threshold)
    }
}