package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.launch

class ProfilesFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var profileSpinner: Spinner
    private lateinit var loadProfileBtn: Button
    private lateinit var saveProfileBtn: Button
    private lateinit var deleteProfileBtn: Button
    private lateinit var profileNameInput: EditText
    private val profiles = mutableListOf<String>()
    private var currentProfile = "Default"
    private lateinit var currentProfileName: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profiles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        currentProfileName = view.findViewById(R.id.current_profile_name)
        currentProfileName.text = preferences.getLastUsedProfile() ?: "Default"

        profileSpinner = view.findViewById(R.id.profile_spinner)
        loadProfileBtn = view.findViewById(R.id.load_profile_btn)
        saveProfileBtn = view.findViewById(R.id.save_profile_btn)
        deleteProfileBtn = view.findViewById(R.id.delete_profile_btn)
        profileNameInput = view.findViewById(R.id.profile_name_input)

        refreshProfileList()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, profiles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        profileSpinner.adapter = adapter

        loadProfileBtn.setOnClickListener { loadSelectedProfile() }
        saveProfileBtn.setOnClickListener { saveCurrentProfile() }
        deleteProfileBtn.setOnClickListener { deleteSelectedProfile() }
    }

    private fun refreshProfileList() {
        val allProfiles = preferences.getAllProfileNames()
        profiles.clear()
        profiles.add("Default")
        profiles.addAll(allProfiles.filter { it != "Default" })
        (profileSpinner.adapter as? ArrayAdapter<String>)?.notifyDataSetChanged()
    }

    private fun loadSelectedProfile() {
        val selected = profileSpinner.selectedItem?.toString() ?: return
        currentProfile = selected
        lifecycleScope.launch {
            val sensitivity = preferences.getProfileSensitivity(selected)
            val clickThreshold = preferences.getProfileClickThreshold(selected)
            preferences.setSensitivity(sensitivity)
            preferences.setClickThreshold(clickThreshold)
            currentProfileName.text = selected
            Toast.makeText(requireContext(), "Loaded profile: $selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentProfile() {
        val name = profileNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter profile name", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            preferences.saveProfile(name, preferences.getSensitivity(), preferences.getClickThreshold(), preferences.getScrollThreshold())
            refreshProfileList()
            Toast.makeText(requireContext(), "Profile saved: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedProfile() {
        val selected = profileSpinner.selectedItem?.toString() ?: return
        if (selected == "Default") {
            Toast.makeText(requireContext(), "Cannot delete default profile", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Profile")
            .setMessage("Delete '$selected'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    preferences.deleteProfile(selected)
                    refreshProfileList()
                    Toast.makeText(requireContext(), "Deleted: $selected", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
