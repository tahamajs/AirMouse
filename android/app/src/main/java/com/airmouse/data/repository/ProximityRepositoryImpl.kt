package com.airmouse.data.repository

import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IProximityRepository {

    private val _distance = MutableStateFlow(5f)
    override fun getDistance(): Flow<Float> = _distance

    private val _state = MutableStateFlow(ProximityState(false,5f,-100,"",""))
    override fun getProximityState(): Flow<ProximityState> = _state

    override suspend fun setThresholds(near: Float, far: Float) { /* store in prefs */ }
    override suspend fun calibrate() { }
    override suspend fun getCalibrationStatus() = ProximityCalibrationStatus(false,-59,2.5f,0.7f)
}package com.airmouse.data.repository

import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IProximityRepository {

    private val _distance = MutableStateFlow(5f)
    override fun getDistance(): Flow<Float> = _distance

    private val _state = MutableStateFlow(ProximityState(false,5f,-100,"",""))
    override fun getProximityState(): Flow<ProximityState> = _state

    override suspend fun setThresholds(near: Float, far: Float) { /* store in prefs */ }
    override suspend fun calibrate() { }
    override suspend fun getCalibrationStatus() = ProximityCalibrationStatus(false,-59,2.5f,0.7f)
}