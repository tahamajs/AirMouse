package com.airmouse.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import com.airmouse.R

class AudioUtils(private val context: Context) {

    private var soundPool: SoundPool
    private var clickSoundId: Int = 0
    private var doubleClickSoundId: Int = 0
    private var rightClickSoundId: Int = 0
    private var scrollSoundId: Int = 0
    private var connectSoundId: Int = 0
    private var disconnectSoundId: Int = 0
    private var errorSoundId: Int = 0

    private var isEnabled = true
    private var volume = 0.5f

    init {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(10, AudioManager.STREAM_MUSIC, 0)
        }

        loadSounds()
    }

    private fun loadSounds() {
        
        
        
    }

    fun playClick() {
        if (!isEnabled) return
        
    }

    fun playDoubleClick() {
        if (!isEnabled) return
        
    }

    fun playRightClick() {
        if (!isEnabled) return
        
    }

    fun playScroll() {
        if (!isEnabled) return
        
    }

    fun playConnect() {
        if (!isEnabled) return
        
    }

    fun playDisconnect() {
        if (!isEnabled) return
        
    }

    fun playError() {
        if (!isEnabled) return
        
    }

    private fun playSound(soundId: Int) {
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
    }

    fun release() {
        soundPool.release()
    }
}