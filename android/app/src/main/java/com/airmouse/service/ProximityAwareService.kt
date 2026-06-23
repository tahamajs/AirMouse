package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ProximityAwareService : Service() {
    private var isNear = false
    private var distance = Float.MAX_VALUE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isNear = intent?.getBooleanExtra(EXTRA_NEAR, false) ?: false
        distance = intent?.getFloatExtra(EXTRA_DISTANCE, Float.MAX_VALUE) ?: Float.MAX_VALUE
        return START_STICKY
    }

    fun updateProximity(near: Boolean, newDistance: Float) {
        isNear = near
        distance = newDistance
    }

    fun isDeviceNear(): Boolean = isNear
    fun getDistance(): Float = distance

    companion object {
        private const val EXTRA_NEAR = "near"
        private const val EXTRA_DISTANCE = "distance"
    }
}
