package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.airmouse.R
import com.airmouse.utils.PreferencesManager

class HelpFragment : Fragment() {

    private lateinit var preferences: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = PreferencesManager(requireContext())

        setupExpandableCard(
            view.findViewById(R.id.help_card_getting_started),
            view.findViewById(R.id.content_start),
            view.findViewById(R.id.chevron_start)
        )
        setupExpandableCard(
            view.findViewById(R.id.help_card_connection),
            view.findViewById(R.id.content_connection),
            view.findViewById(R.id.chevron_connection)
        )
        setupExpandableCard(
            view.findViewById(R.id.help_card_calibration),
            view.findViewById(R.id.content_calibration),
            view.findViewById(R.id.chevron_calibration)
        )
        setupExpandableCard(
            view.findViewById(R.id.help_card_gestures),
            view.findViewById(R.id.content_gestures),
            view.findViewById(R.id.chevron_gestures)
        )
        setupExpandableCard(
            view.findViewById(R.id.help_card_advanced),
            view.findViewById(R.id.content_advanced),
            view.findViewById(R.id.chevron_advanced)
        )
        setupExpandableCard(
            view.findViewById(R.id.help_card_troubleshooting),
            view.findViewById(R.id.content_troubleshooting),
            view.findViewById(R.id.chevron_troubleshooting)
        )

        // Set version text
        val versionText = view.findViewById<TextView>(R.id.version_text)
        val version = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) { "3.0" }
        versionText.text = "Air Mouse Pro v$version"
    }

    private fun setupExpandableCard(card: CardView, content: TextView, chevron: ImageView) {
        var isExpanded = false
        content.visibility = View.GONE
        chevron.setImageResource(R.drawable.ic_chevron_down)

        card.setOnClickListener {
            isExpanded = !isExpanded
            content.visibility = if (isExpanded) View.VISIBLE else View.GONE
            chevron.setImageResource(
                if (isExpanded) R.drawable.ic_chevron_up
                else R.drawable.ic_chevron_down
            )
        }
    }
}