// app/src/main/java/com/airmouse/ui/ProximitySettingsFragment.kt
package com.airmouse.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.R
import com.airmouse.proximity.ProximityAwareService
import com.google.android.material.switchmaterial.SwitchMaterial
import com.airmouse.ui.UiStyleUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProximitySettingsFragment : Fragment() {

    private lateinit var proximitySwitch: SwitchMaterial
    private lateinit var nearSlider: SeekBar
    private lateinit var farSlider: SeekBar
    private lateinit var nearValue: TextView
    private lateinit var farValue: TextView
    private lateinit var calibrateBtn: Button
    private lateinit var currentDistanceText: TextView
    private lateinit var statusText: TextView

    private var serviceIntent: Intent? = null
    private var isServiceRunning = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_proximity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        proximitySwitch = view.findViewById(R.id.proximitySwitch)
        nearSlider = view.findViewById(R.id.nearThresholdSlider)
        farSlider = view.findViewById(R.id.farThresholdSlider)
        nearValue = view.findViewById(R.id.nearThresholdValue)
        farValue = view.findViewById(R.id.farThresholdValue)
        calibrateBtn = view.findViewById(R.id.calibrateBtn)
        currentDistanceText = view.findViewById(R.id.currentDistance)
        statusText = view.findViewById(R.id.statusText)

        proximitySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) startProximityService() else stopProximityService()
        }

        nearSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val meters = progress / 10f
                nearValue.text = String.format("%.1f m", meters)
                if (fromUser) updateThresholds()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        farSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val meters = progress / 10f
                farValue.text = String.format("%.1f m", meters)
                if (fromUser) updateThresholds()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        calibrateBtn.setOnClickListener { startCalibration() }

        // Light polish so the screen matches the rest of the app.
        UiStyleUtils.animateIn(proximitySwitch, 0)
        UiStyleUtils.animateIn(nearSlider, 30)
        UiStyleUtils.animateIn(farSlider, 60)
        UiStyleUtils.animateIn(calibrateBtn, 90)
        UiStyleUtils.animateIn(statusText, 120)

        // Check Bluetooth
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null || !btAdapter.isEnabled) {
            statusText.text = "Bluetooth is disabled. Please enable Bluetooth."
            proximitySwitch.isEnabled = false
        } else {
            proximitySwitch.isEnabled = true
        }
    }

    private fun startProximityService() {
        if (serviceIntent == null) {
            serviceIntent = Intent(requireContext(), ProximityAwareService::class.java)
        }
        requireContext().startForegroundService(serviceIntent)
        isServiceRunning = true
        statusText.text = "Proximity service running"
        UiStyleUtils.pulse(calibrateBtn)
        startDistanceUpdates()
    }

    private fun stopProximityService() {
        if (serviceIntent != null) {
            requireContext().stopService(serviceIntent)
        }
        isServiceRunning = false
        statusText.text = "Proximity service stopped"
        currentDistanceText.text = "Distance: --- m"
    }

    private fun startDistanceUpdates() {
        lifecycleScope.launch {
            while (isServiceRunning) {
                // In real code you would bind to service or use LiveData
                // For demo, we simulate
                currentDistanceText.text = "Distance: 2.34 m"
                delay(1000)
            }
        }
    }

    private fun updateThresholds() {
        val near = nearSlider.progress / 10f
        val far = farSlider.progress / 10f
        if (isServiceRunning) {
            // Send to service via broadcast or binder
            Toast.makeText(requireContext(), "Thresholds updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCalibration() {
        Toast.makeText(requireContext(), "Place phone exactly 1 meter away, then tap again", Toast.LENGTH_LONG).show()
        // Full calibration logic would be implemented here
    }
}