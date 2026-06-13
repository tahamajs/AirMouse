// app/src/main/java/com/airmouse/utils/VibrateUtils.kt (Update)
package com.airmouse.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrateUtils @Inject constructor(
    private val context: Context
) {

    private val vibrator: Vibrator by lazy {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun getVibrator(): Vibrator = vibrator

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
        vibrate(arrayOf(20L, 50L, 20L))
    }

    fun vibrateError() {
        vibrate(arrayOf(100L, 50L, 100L))
    }

    fun vibrateSuccess() {
        vibrate(arrayOf(20L, 30L, 50L))
    }

    private fun vibrate(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    fun cancel() {
        vibrator.cancel()
    }

    fun hasVibrator(): Boolean = vibrator.hasVibrator()
}