package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class OrientationMonitorService : Service() {
    data class OrientationSnapshot(
        val roll: Float = 0f,
        val pitch: Float = 0f,
        val yaw: Float = 0f,
        val timestamp: Long = System.currentTimeMillis()
    )

    private var latestOrientation = OrientationSnapshot()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestOrientation = OrientationSnapshot(
            roll = intent?.getFloatExtra(EXTRA_ROLL, 0f) ?: 0f,
            pitch = intent?.getFloatExtra(EXTRA_PITCH, 0f) ?: 0f,
            yaw = intent?.getFloatExtra(EXTRA_YAW, 0f) ?: 0f
        )
        return START_STICKY
    }

    fun updateOrientation(roll: Float, pitch: Float, yaw: Float) {
        latestOrientation = OrientationSnapshot(roll, pitch, yaw)
    }

    fun getLatestOrientation(): OrientationSnapshot = latestOrientation

    companion object {
        private const val EXTRA_ROLL = "roll"
        private const val EXTRA_PITCH = "pitch"
        private const val EXTRA_YAW = "yaw"
    }
}
