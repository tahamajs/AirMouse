package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.airmouse.utils.PreferencesManager

class AccessibilityFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var announceMovementSwitch: Switch
    private lateinit var announceClicksSwitch: Switch

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accessibility, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        announceMovementSwitch = view.findViewById(R.id.announce_movement_switch)
        announceClicksSwitch = view.findViewById(R.id.announce_clicks_switch)

        announceMovementSwitch.isChecked = preferences.isAnnounceMovementEnabled()
        announceClicksSwitch.isChecked = preferences.isAnnounceClicksEnabled()

        announceMovementSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.setAnnounceMovementEnabled(isChecked)
        }
        announceClicksSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.setAnnounceClicksEnabled(isChecked)
        }
    }
}