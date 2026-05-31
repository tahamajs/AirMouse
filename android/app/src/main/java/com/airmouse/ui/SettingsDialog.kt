package com.airmouse.ui

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.airmouse.R
import com.airmouse.utils.PreferencesManager

class SettingsDialog(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val onDismiss: () -> Unit
) {
    private val dialog: AlertDialog

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_settings, null)

        // Click Threshold
        val clickSeek = view.findViewById<SeekBar>(R.id.clickThresholdSeek)
        val clickValue = view.findViewById<TextView>(R.id.clickThresholdValue)
        clickSeek.progress = (prefs.getClickThreshold() * 10).toInt().coerceIn(0, 100)
        clickValue.text = String.format("%.1f rad/s", prefs.getClickThreshold())
        clickSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10f
                clickValue.text = String.format("%.1f rad/s", value)
                if (fromUser) prefs.setClickThreshold(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Double Click Interval
        val doubleSeek = view.findViewById<SeekBar>(R.id.doubleClickSeek)
        val doubleValue = view.findViewById<TextView>(R.id.doubleClickValue)
        doubleSeek.progress = ((prefs.getDoubleClickInterval() - 200) / 10).toInt().coerceIn(0, 80)
        doubleValue.text = "${prefs.getDoubleClickInterval()} ms"
        doubleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 200L + (progress * 10L)
                doubleValue.text = "$value ms"
                if (fromUser) prefs.setDoubleClickInterval(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Scroll Threshold
        val scrollSeek = view.findViewById<SeekBar>(R.id.scrollThresholdSeek)
        val scrollValue = view.findViewById<TextView>(R.id.scrollThresholdValue)
        scrollSeek.progress = (prefs.getScrollThreshold() / 0.2f).toInt().coerceIn(0, 75)
        scrollValue.text = String.format("%.1f m/s²", prefs.getScrollThreshold())
        scrollSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress * 0.2f
                scrollValue.text = String.format("%.1f m/s²", value)
                if (fromUser) prefs.setScrollThreshold(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Right-Click Tilt Angle
        val tiltSeek = view.findViewById<SeekBar>(R.id.tiltThresholdSeek)
        val tiltValue = view.findViewById<TextView>(R.id.tiltThresholdValue)
        tiltSeek.progress = prefs.getRightClickTilt().toInt().coerceIn(0, 90)
        tiltValue.text = "${prefs.getRightClickTilt().toInt()}°"
        tiltSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tiltValue.text = "$progress°"
                if (fromUser) prefs.setRightClickTilt(progress.toFloat())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Haptic Feedback Switch
        val hapticSwitch = view.findViewById<SwitchCompat>(R.id.hapticSwitch)
        hapticSwitch.isChecked = prefs.isHapticEnabled()
        hapticSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setHapticEnabled(isChecked)
        }

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setPositiveButton(R.string.done) { _, _ -> onDismiss() }
            .create()
    }

    fun show() = dialog.show()
}