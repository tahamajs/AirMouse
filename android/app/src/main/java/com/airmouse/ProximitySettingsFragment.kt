// ProximitySettingsFragment.kt
package com.airmouse.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.airmouse.R
import com.airmouse.proximity.ProximityAwareService
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class ProximitySettingsFragment : Fragment() {

    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var nearSeekBar: SeekBar
    private lateinit var farSeekBar: SeekBar
    private lateinit var nearValue: TextView
    private lateinit var farValue: TextView
    private lateinit var macInput: TextView
    private lateinit var saveBtn: MaterialButton

    private var proximityIntent: Intent? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_proximity_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableSwitch = view.findViewById(R.id.proximity_enable_switch)
        nearSeekBar = view.findViewById(R.id.near_threshold_seek)
        farSeekBar = view.findViewById(R.id.far_threshold_seek)
        nearValue = view.findViewById(R.id.near_threshold_value)
        farValue = view.findViewById(R.id.far_threshold_value)
        macInput = view.findViewById(R.id.server_mac_input)
        saveBtn = view.findViewById(R.id.save_proximity_btn)

        // Load saved preferences (using SharedPreferences)
        val prefs = requireContext().getSharedPreferences(ProximityAwareService.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val near = prefs.getFloat(ProximityAwareService.KEY_NEAR_THRESHOLD, 2.0f)
        val far = prefs.getFloat(ProximityAwareService.KEY_FAR_THRESHOLD, 4.0f)
        val mac = prefs.getString(ProximityAwareService.KEY_SERVER_MAC, "") ?: ""

        nearSeekBar.progress = (near * 10).toInt()
        farSeekBar.progress = (far * 10).toInt()
        nearValue.text = String.format("%.1f m", near)
        farValue.text = String.format("%.1f m", far)
        macInput.text = mac

        nearSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val meters = progress / 10f
                nearValue.text = String.format("%.1f m", meters)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        farSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val meters = progress / 10f
                farValue.text = String.format("%.1f m", meters)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        proximityIntent = Intent(requireContext(), ProximityAwareService::class.java)

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireContext().startService(proximityIntent)
            } else {
                requireContext().stopService(proximityIntent)
            }
        }

        saveBtn.setOnClickListener {
            val nearMeters = nearSeekBar.progress / 10f
            val farMeters = farSeekBar.progress / 10f
            val mac = macInput.text.toString()
            prefs.edit()
                .putFloat(ProximityAwareService.KEY_NEAR_THRESHOLD, nearMeters)
                .putFloat(ProximityAwareService.KEY_FAR_THRESHOLD, farMeters)
                .putString(ProximityAwareService.KEY_SERVER_MAC, mac)
                .apply()
            Toast.makeText(requireContext(), "Proximity settings saved", Toast.LENGTH_SHORT).show()
        }
    }
}