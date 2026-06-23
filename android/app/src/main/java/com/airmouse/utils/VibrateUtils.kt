package com.airmouse.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrateUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val vibrator: Vibrator? by lazy {
        ContextCompat.getSystemService(context, Vibrator::class.java)
    }

    fun vibrateShort() {
        vibrate(30)
    }

    fun vibrateMedium() {
        vibrate(50)
    }

    fun vibrateLong() {
        vibrate(100)
    }

    fun vibrateClick() {
        vibrate(20)
    }

    fun vibrateDoubleClick() {
        
        vibrate(longArrayOf(20L, 50L, 20L))
    }

    fun vibrateError() {
        
        vibrate(longArrayOf(100L, 50L, 100L))
    }

    fun vibrateSuccess() {
        
        vibrate(longArrayOf(20L, 30L, 50L))
    }

    private fun vibrate(duration: Long) {
        val safeVibrator = vibrator ?: return
        if (!safeVibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeVibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                safeVibrator.vibrate(duration)
            }
        } catch (_: SecurityException) {
            
        }
    }

    private fun vibrate(pattern: LongArray) {
        val safeVibrator = vibrator ?: return
        if (!safeVibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                safeVibrator.vibrate(pattern, -1)
            }
        } catch (_: SecurityException) {
            
        }
    }

    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (_: SecurityException) {
            
        }
    }

    fun hasVibrator(): Boolean = vibrator?.hasVibrator() == true
}
