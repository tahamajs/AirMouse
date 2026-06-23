package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GestureInferenceService : Service() {
    private var latestResult: GestureResult? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val gesture = intent?.getStringExtra(EXTRA_GESTURE)
        val confidence = intent?.getFloatExtra(EXTRA_CONFIDENCE, 0f) ?: 0f
        if (!gesture.isNullOrBlank()) {
            latestResult = GestureResult(gesture, confidence)
        }
        return START_STICKY
    }

    fun getLatestResult(): GestureResult? = latestResult

    fun submitGestureResult(result: GestureResult) {
        latestResult = result
    }

    companion object {
        private const val EXTRA_GESTURE = "gesture"
        private const val EXTRA_CONFIDENCE = "confidence"
    }
}
