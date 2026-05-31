package com.airmouse

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airmouse.utils.PreferencesManager
import com.google.android.material.snackbar.Snackbar
import com.airmouse.R

class EdgeGesturesFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var enableSwitch: SwitchCompat
    private lateinit var volumeUpSpinner: Spinner
    private lateinit var volumeDownSpinner: Spinner

    private val edgeGestureServiceIntent by lazy { Intent(requireContext(), EdgeGestureService::class.java) }

    // Permission launcher for accessibility service
    private val accessibilityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // After returning, check if accessibility is enabled
        if (isAccessibilityServiceEnabled()) {
            enableSwitch.isChecked = true
            preferences.setEdgeGesturesEnabled(true)
            requireContext().startService(edgeGestureServiceIntent)
        } else {
            enableSwitch.isChecked = false
            preferences.setEdgeGesturesEnabled(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edge_gestures, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        enableSwitch = view.findViewById(R.id.edge_gesture_switch)
        volumeUpSpinner = view.findViewById(R.id.volume_up_action)
        volumeDownSpinner = view.findViewById(R.id.volume_down_action)

        // Load saved states
        enableSwitch.isChecked = preferences.isEdgeGesturesEnabled()
        val actionsArray = resources.getStringArray(R.array.edge_gesture_actions)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, actionsArray)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        volumeUpSpinner.adapter = adapter
        volumeDownSpinner.adapter = adapter

        volumeUpSpinner.setSelection(getActionIndex(preferences.getEdgeGestureAction("volume_up")))
        volumeDownSpinner.setSelection(getActionIndex(preferences.getEdgeGestureAction("volume_down")))

        volumeUpSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferences.setEdgeGestureAction("volume_up", actionsArray[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        volumeDownSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                preferences.setEdgeGestureAction("volume_down", actionsArray[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (isAccessibilityServiceEnabled()) {
                    preferences.setEdgeGesturesEnabled(true)
                    requireContext().startService(edgeGestureServiceIntent)
                    Toast.makeText(requireContext(), "Edge gestures enabled", Toast.LENGTH_SHORT).show()
                } else {
                    // Request accessibility permission
                    requestAccessibilityPermission()
                    enableSwitch.isChecked = false
                }
            } else {
                preferences.setEdgeGesturesEnabled(false)
                requireContext().stopService(edgeGestureServiceIntent)
                Toast.makeText(requireContext(), "Edge gestures disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getActionIndex(action: String): Int {
        val actions = resources.getStringArray(R.array.edge_gesture_actions)
        return actions.indexOf(action).coerceAtLeast(0)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = ComponentName(requireContext(), EdgeGestureService::class.java)
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service.flattenToString())
    }
    private fun executeAction(volumeKey: String) {
        val action = preferences.getEdgeGestureAction(volumeKey)
        when (action) {
            "Click" -> sendCommand("click")
            "Double Click" -> sendCommand("doubleclick")
            "Right Click" -> sendCommand("rightclick")
            "Scroll Up" -> sendCommand("scroll", 1)
            "Scroll Down" -> sendCommand("scroll", -1)
            "Next Track" -> sendCommand("media_next")
            "Previous Track" -> sendCommand("media_prev")
            "Volume Up" -> sendCommand("volume_up")
            "Volume Down" -> sendCommand("volume_down")
        }
    }
    private fun requestAccessibilityPermission() {
        Snackbar.make(requireView(), "Edge gestures need accessibility permission to detect volume key long presses", Snackbar.LENGTH_INDEFINITE)
            .setAction("Enable") {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilityLauncher.launch(intent)
            }
            .show()
    }
}