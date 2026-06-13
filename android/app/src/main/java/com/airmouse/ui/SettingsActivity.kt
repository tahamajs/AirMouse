// app/src/main/java/com/airmouse/ui/SettingsActivity.kt
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

            // Additional preferences can be added here (scroll speed, deadzone, etc.)
        }
    }
}