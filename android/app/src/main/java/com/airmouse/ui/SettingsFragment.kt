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
import com.airmouse.ui.UiStyleUtils
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class SettingsFragment : Fragment() {

    private lateinit var autoPauseSwitch: SwitchCompat

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoPauseSwitch = view.findViewById(R.id.auto_pause_switch)
        autoPauseSwitch.isChecked = PreferencesHelper.isAutoPauseEnabled()

        // Make the screen feel like part of the same dashboard system.
        val root = view as? ViewGroup
        root?.let {
            for (i in 0 until it.childCount) {
                UiStyleUtils.animateIn(it.getChildAt(i), i * 35L)
            }
        }

        val labels = view.findViewById<TextView?>(android.R.id.title)
        labels?.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        val infoBlocks = mutableListOf<View>()
        fun styleNode(node: View) {
            when (node) {
                is com.google.android.material.card.MaterialCardView -> UiStyleUtils.styleCard(node)
                is LinearLayout -> if (node.id != R.id.auto_pause_switch) node.setBackgroundColor(0x00000000)
            }
            if (node is ViewGroup) for (i in 0 until node.childCount) styleNode(node.getChildAt(i))
        }
        styleNode(view)

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

        autoPauseSwitch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) UiStyleUtils.pulse(autoPauseSwitch)
        }
    }
}