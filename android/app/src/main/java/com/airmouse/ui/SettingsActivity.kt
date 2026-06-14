package com.airmouse.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.airmouse.R
import com.airmouse.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var prefs: PreferencesManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            prefs = PreferencesManager(requireContext())

            // Sensitivity (read‑only display, adjust via slider in main screen)
            val sensitivityPref = findPreference<Preference>("sensitivity")
            sensitivityPref?.summary = "Current: ${prefs.getSensitivity()}"

            // Theme
            val themePref = findPreference<ListPreference>("theme")
            themePref?.summary = themePref?.entry
            themePref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val theme = newValue as String
                themePref.summary = themePref.entries[themePref.findIndexOfValue(theme)]
                prefs.setTheme(theme)
                true
            }

            // Haptic Feedback
            val hapticPref = findPreference<SwitchPreferenceCompat>("haptic_enabled")
            hapticPref?.isChecked = prefs.isHapticEnabled()
            hapticPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setHapticEnabled(newValue as Boolean)
                true
            }

            // AI Smoothing
            val aiPref = findPreference<SwitchPreferenceCompat>("ai_smoothing")
            aiPref?.isChecked = prefs.isAiSmoothingEnabled()
            aiPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setAiSmoothingEnabled(newValue as Boolean)
                true
            }

            // Predictive Movement
            val predictivePref = findPreference<SwitchPreferenceCompat>("predictive_movement")
            predictivePref?.isChecked = prefs.isPredictiveEnabled()
            predictivePref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setPredictiveEnabled(newValue as Boolean)
                true
            }

            // Invert X
            val invertXPref = findPreference<SwitchPreferenceCompat>("invert_x")
            invertXPref?.isChecked = prefs.isInvertX()
            invertXPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setInvertX(newValue as Boolean)
                true
            }

            // Invert Y
            val invertYPref = findPreference<SwitchPreferenceCompat>("invert_y")
            invertYPref?.isChecked = prefs.isInvertY()
            invertYPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setInvertY(newValue as Boolean)
                true
            }

            // Acceleration
            val accelerationPref = findPreference<SwitchPreferenceCompat>("acceleration_enabled")
            accelerationPref?.isChecked = prefs.isAccelerationEnabled()
            accelerationPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setAccelerationEnabled(newValue as Boolean)
                true
            }

            // Smoothing
            val smoothingPref = findPreference<SwitchPreferenceCompat>("smoothing_enabled")
            smoothingPref?.isChecked = prefs.isSmoothingEnabled()
            smoothingPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.setSmoothingEnabled(newValue as Boolean)
                true
            }

            // Reset Statistics
            val resetStatsPref = findPreference<Preference>("reset_statistics")
            resetStatsPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showResetStatsDialog()
                true
            }

            // Debug Overlay
            val debugOverlayPref = findPreference<SwitchPreferenceCompat>("debug_overlay")
            debugOverlayPref?.isChecked = prefs.getBoolean("debug_overlay_enabled", false)
            debugOverlayPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                prefs.putBoolean("debug_overlay_enabled", newValue as Boolean)
                true
            }

            // About
            val aboutPref = findPreference<Preference>("about")
            aboutPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }

        private fun showResetStatsDialog() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Reset Statistics")
                .setMessage("Are you sure you want to reset all usage statistics?")
                .setPositiveButton("Reset") { _, _ ->
                    prefs.resetStatistics()
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Statistics Reset")
                        .setMessage("All usage statistics have been reset.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showAboutDialog() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Air Mouse Pro")
                .setMessage("Version 3.0.0\n\nTurn your phone into a wireless mouse\n\n© 2025 Air Mouse Team")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}