package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.airmouse.network.DataSender
import com.airmouse.utils.PreferencesManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class EdgeGesturesFragment : Fragment() {

    private lateinit var preferences: PreferencesManager
    private lateinit var enableSwitch: Switch
    private var floatingButton: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edge_gestures, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())
        enableSwitch = view.findViewById(R.id.edge_gesture_switch)

        enableSwitch.isChecked = preferences.isEdgeGesturesEnabled()
        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.setEdgeGesturesEnabled(isChecked)
            if (isChecked) {
                showFloatingButton()
            } else {
                hideFloatingButton()
            }
        }

        if (preferences.isEdgeGesturesEnabled()) {
            showFloatingButton()
        }
    }

    private fun showFloatingButton() {
        val activity = requireActivity()
        val rootView = activity.window.decorView.rootView
        if (floatingButton == null) {
            floatingButton = FloatingActionButton(activity).apply {
                setImageResource(android.R.drawable.ic_menu_edit)
                setSize(FloatingActionButton.SIZE_MINI)
                val params = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                (rootView as? ViewGroup)?.addView(this, params)
                setOnClickListener {
                    DataSender.getInstance()?.sendClick()
                    Snackbar.make(requireView(), "Edge gesture: Click", Snackbar.LENGTH_SHORT).show()
                }
                setOnLongClickListener {
                    DataSender.getInstance()?.sendRightClick()
                    Snackbar.make(requireView(), "Edge gesture: Right Click", Snackbar.LENGTH_SHORT).show()
                    true
                }
            }
        }
        floatingButton?.visibility = View.VISIBLE
    }

    private fun hideFloatingButton() {
        floatingButton?.visibility = View.GONE
    }

    override fun onDestroyView() {
        hideFloatingButton()
        super.onDestroyView()
    }
}