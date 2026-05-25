package com.airmouse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.airmouse.data.NetworkRepository
import com.airmouse.data.PreferencesDataStore
import com.airmouse.data.SensorRepository
import com.airmouse.domain.GestureDetector
import com.airmouse.domain.MadgwickFusion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesDataStore(application)
    private val sensorRepo = SensorRepository(application)
    private val networkRepo = NetworkRepository(prefs)
    private val fusion = MadgwickFusion()
    private val gestureDetector = GestureDetector(prefs)

    private val _statusText = MutableLiveData("Not connected")
    val statusText: LiveData<String> = _statusText

    private val _orientationYaw = MutableLiveData(0f)
    val orientationYaw: LiveData<Float> = _orientationYaw

    private val _clickFlash = MutableLiveData(false)
    val clickFlashEvent: LiveData<Boolean> = _clickFlash

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    val lastIp: LiveData<String> = prefs.lastIpFlow

    private var isActive = false
    private var lastRoll = 0f
    private var lastYaw = 0f

    init {
        viewModelScope.launch {
            sensorRepo.sensorEvents.collect { (roll, yaw, gyroY, accelY) ->
                if (isActive) {
                    _orientationYaw.postValue(yaw)
                    val dx = (yaw - lastYaw) * prefs.getSensitivity() * 0.8f
                    val dy = (roll - lastRoll) * prefs.getSensitivity() * 0.8f
                    lastYaw = yaw
                    lastRoll = roll
                    networkRepo.sendMove(dx, dy)

                    when (gestureDetector.detect(gyroY, accelY, roll)) {
                        GestureDetector.Gesture.CLICK -> {
                            networkRepo.sendClick()
                            _clickFlash.postValue(true)
                            _clickFlash.postValue(false)
                            if (prefs.isHapticEnabled()) sensorRepo.vibrate(30)
                        }
                        GestureDetector.Gesture.DOUBLE_CLICK -> {
                            networkRepo.sendDoubleClick()
                            _toastMessage.postValue("Double click")
                        }
                        GestureDetector.Gesture.RIGHT_CLICK -> {
                            networkRepo.sendRightClick()
                            _toastMessage.postValue("Right click")
                        }
                        GestureDetector.Gesture.SCROLL_UP -> networkRepo.sendScroll(-1)
                        GestureDetector.Gesture.SCROLL_DOWN -> networkRepo.sendScroll(1)
                        else -> {}
                    }
                }
            }
        }
    }

    fun setSensitivity(value: Float) = prefs.setSensitivity(value)

    fun calibrate() {
        viewModelScope.launch {
            _statusText.value = "Calibrating gyro..."
            sensorRepo.calibrateGyro()
            _statusText.value = "Calibrating magnetometer (figure-8)..."
            sensorRepo.calibrateMagnetometer(30000)
            _statusText.value = "Calibration complete!"
            _toastMessage.value = "Calibration done"
        }
    }

    fun start() {
        viewModelScope.launch {
            val ip = prefs.getLastIp()
            if (ip.isBlank()) {
                _toastMessage.value = "Enter laptop IP"
                return@launch
            }
            networkRepo.connect(ip)
            sensorRepo.start()
            isActive = true
            _statusText.value = "Air Mouse Active"
        }
    }

    fun toggleDebugOverlay() {
        _toastMessage.value = "Debug overlay toggled"
        // In a real implementation you would start/stop an overlay service
    }
}