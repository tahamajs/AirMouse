package com.airmouse.ui

import android.content.Context
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.airmouse.R
import com.airmouse.utils.PreferencesManager

class SettingsDialog(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val onApply: () -> Unit = {}
) {
    fun show() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }

        val sensitivityLabel = TextView(context).apply {
            text = context.getString(R.string.gesture_sensitivity)
        }
        val sensitivitySeek = SeekBar(context).apply {
            max = 100
            progress = (preferences.getSensitivity().coerceIn(0.2f, 2.0f) / 2.0f * 100).toInt()
        }
        val thresholdLabel = TextView(context).apply {
            text = context.getString(R.string.click_speed_threshold)
        }
        val thresholdSeek = SeekBar(context).apply {
            max = 100
            progress = preferences.getClickThreshold().toInt().coerceIn(0, 100)
        }

        root.addView(sensitivityLabel)
        root.addView(sensitivitySeek)
        root.addView(thresholdLabel)
        root.addView(thresholdSeek)

        AlertDialog.Builder(context)
            .setTitle(R.string.settings_title)
            .setView(root)
            .setPositiveButton(R.string.ok) { _, _ ->
                val sensitivity = 0.2f + (sensitivitySeek.progress / 100f) * 1.8f
                preferences.setSensitivity(sensitivity)
                preferences.setClickThreshold(thresholdSeek.progress.toFloat())
                onApply()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
