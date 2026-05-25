package com.airmouse.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.*
import com.airmouse.R
import com.airmouse.utils.PreferencesManager

class SettingsDialog(context: Context, private val prefs: PreferencesManager, private val onDismiss: () -> Unit) {
    private val dialog: AlertDialog

    init {
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // Click threshold
        val clickLabel = TextView(context).apply { text = "Click speed threshold (rad/s)" }
        val clickSeek = SeekBar(context).apply {
            progress = (prefs.getClickThreshold() / 0.1f).toInt()
            max = 100
        }
        val clickValue = TextView(context).apply { text = "${prefs.getClickThreshold()}" }
        clickSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10f
                clickValue.text = "$value"
                prefs.setClickThreshold(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        view.addView(clickLabel)
        view.addView(clickSeek)
        view.addView(clickValue)

        // Double-click interval
        val doubleLabel = TextView(context).apply { text = "Double-click max interval (ms)" }
        val doubleSeek = SeekBar(context).apply {
            progress = (prefs.getDoubleClickInterval().toInt() - 200) / 10
            max = 80 // 200..1000ms
        }
        val doubleValue = TextView(context).apply { text = "${prefs.getDoubleClickInterval()}" }
        doubleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 200 + progress * 10L
                doubleValue.text = "$value"
                prefs.setDoubleClickInterval(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        view.addView(doubleLabel)
        view.addView(doubleSeek)
        view.addView(doubleValue)

        // Scroll threshold
        val scrollLabel = TextView(context).apply { text = "Scroll speed threshold (m/s²)" }
        val scrollSeek = SeekBar(context).apply {
            progress = (prefs.getScrollThreshold() / 0.2f).toInt()
            max = 75 // up to 15 m/s²
        }
        val scrollValue = TextView(context).apply { text = "${prefs.getScrollThreshold()}" }
        scrollSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress * 0.2f
                scrollValue.text = "$value"
                prefs.setScrollThreshold(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        view.addView(scrollLabel)
        view.addView(scrollSeek)
        view.addView(scrollValue)

        // Right-click tilt
        val tiltLabel = TextView(context).apply { text = "Right-click tilt angle (degrees)" }
        val tiltSeek = SeekBar(context).apply {
            progress = (prefs.getRightClickTilt() / 45f * 100).toInt()
            max = 100
        }
        val tiltValue = TextView(context).apply { text = "${prefs.getRightClickTilt()}" }
        tiltSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress * 45 / 100f
                tiltValue.text = "$value"
                prefs.setRightClickTilt(value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        view.addView(tiltLabel)
        view.addView(tiltSeek)
        view.addView(tiltValue)

        // Haptic toggle
        val hapticCheck = CheckBox(context).apply {
            text = "Enable haptic feedback"
            isChecked = prefs.isHapticEnabled()
            setOnCheckedChangeListener { _, isChecked -> prefs.setHapticEnabled(isChecked) }
        }
        view.addView(hapticCheck)

        dialog = AlertDialog.Builder(context)
            .setTitle("Air Mouse Settings")
            .setView(view)
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .create()
    }

    fun show() = dialog.show()
}