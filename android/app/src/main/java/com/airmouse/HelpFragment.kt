package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
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

        // Populate help cards with content
        val hint = view.findViewById<TextView>(R.id.help_hint_text)
        hint?.text = getString(R.string.help_cards_hint)

        setupHelpCard(view.findViewById(R.id.help_card_controls),
            getString(R.string.help_title), getString(R.string.help_content))
        setupHelpCard(view.findViewById(R.id.help_card_calibration),
            getString(R.string.calibration_guide_title), getString(R.string.calibration_guide_content))
        setupHelpCard(view.findViewById(R.id.help_card_sensors),
            getString(R.string.sensor_info_title), getString(R.string.sensor_info_content))
        setupHelpCard(view.findViewById(R.id.help_card_connection),
            getString(R.string.connection_guide_title), getString(R.string.connection_guide_content))
    }

    private fun setupHelpCard(cardView: CardView, title: String, content: String) {
        // Inflate simple layout inside card
        val linearLayout = LinearLayout(cardView.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }
        val titleView = TextView(cardView.context).apply {
            text = title
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(androidx.core.content.ContextCompat.getColor(cardView.context, R.color.deep_orange_500))
        }
        val contentView = TextView(cardView.context).apply {
            text = android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY)
            textSize = 14f
            setPadding(0, 16, 0, 0)
        }
        // Add chevron to title to indicate expandability
        val chevron = TextView(cardView.context).apply {
            text = "›"
            textSize = 18f
            setPadding(8, 0, 0, 0)
        }
        val titleContainer = LinearLayout(cardView.context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(titleView)
            addView(chevron)
        }
        linearLayout.addView(titleContainer)
        linearLayout.addView(contentView)
        cardView.removeAllViews()
        cardView.addView(linearLayout)

        cardView.setOnClickListener {
            // Toggle visibility of content and rotate chevron
            val isVisible = contentView.visibility == View.VISIBLE
            contentView.visibility = if (isVisible) View.GONE else View.VISIBLE
            chevron.rotation = if (isVisible) 0f else 90f
        }
        // start collapsed
        contentView.visibility = View.GONE
    }
}