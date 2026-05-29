// app/src/main/java/com/airmouse/ui/SettingsFragment.kt (or add to existing)
package com.airmouse.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SwitchCompat
import com.airmouse.R
import com.airmouse.orientation.OrientationMonitorService
import com.airmouse.utils.PreferencesHelper

class SettingsFragment : Fragment() {

    private lateinit var autoPauseSwitch: SwitchCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoPauseSwitch = view.findViewById(R.id.auto_pause_switch)
        autoPauseSwitch.isChecked = PreferencesHelper.isAutoPauseEnabled()

        autoPauseSwitch.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.setAutoPauseEnabled(isChecked)
            if (isChecked) {
                requireContext().startService(Intent(requireContext(), OrientationMonitorService::class.java))
                Toast.makeText(requireContext(), "Auto‑pause enabled", Toast.LENGTH_SHORT).show()
            } else {
                requireContext().stopService(Intent(requireContext(), OrientationMonitorService::class.java))
                Toast.makeText(requireContext(), "Auto‑pause disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}