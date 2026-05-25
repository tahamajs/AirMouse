package com.airmouse.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.airmouse.R
import com.airmouse.data.PreferencesDataStore

class SettingsFragment : DialogFragment() {

    private lateinit var prefs: PreferencesDataStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferencesDataStore(requireContext())

        // Click threshold
        val clickSeek = view.findViewById<SeekBar>(R.id.clickThresholdSeek)
        val clickValue = view.findViewById<TextView>(R.id.clickThresholdValue)
        clickSeek.progress = (prefs.getClickThreshold() / 0.1f).toInt()
        clickValue.text = prefs.getClickThreshold().toString()
        clickSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 10f
                clickValue.text = "$value"
                prefs.setClickThreshold(value)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })

        // Double-click interval
        val doubleSeek = view.findViewById<SeekBar>(R.id.doubleClickSeek)
        val doubleValue = view.findViewById<TextView>(R.id.doubleClickValue)
        doubleSeek.progress = ((prefs.getDoubleClickInterval() - 200) / 10).toInt()
        doubleValue.text = prefs.getDoubleClickInterval().toString()
        doubleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = 200L + progress * 10L
                doubleValue.text = "$value"
                prefs.setDoubleClickInterval(value)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })

        // Right-click tilt
        val tiltSeek = view.findViewById<SeekBar>(R.id.tiltThresholdSeek)
        val tiltValue = view.findViewById<TextView>(R.id.tiltThresholdValue)
        tiltSeek.progress = (prefs.getRightClickTilt() / 45f * 100).toInt()
        tiltValue.text = prefs.getRightClickTilt().toString()
        tiltSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress * 45 / 100f
                tiltValue.text = "$value"
                prefs.setRightClickTilt(value)
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })
    }
}