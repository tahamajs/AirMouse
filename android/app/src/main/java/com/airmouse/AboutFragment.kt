package com.airmouse

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.airmouse.ui.UiStyleUtils
import com.google.android.material.card.MaterialCardView

class AboutFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val versionText = view.findViewById<TextView>(R.id.version_text)
        val version = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "?"
        }
        versionText?.text = getString(R.string.version_format, version)

        // Harmonize the about screen with the dashboard visual system.
        val cards = mutableListOf<MaterialCardView>()
        fun collectCards(node: View) {
            when (node) {
                is MaterialCardView -> cards.add(node)
                is ViewGroup -> for (i in 0 until node.childCount) collectCards(node.getChildAt(i))
            }
        }
        collectCards(view)
        cards.forEachIndexed { index, card ->
            UiStyleUtils.styleCard(card)
            UiStyleUtils.animateIn(card, index * 60L)
        }
    }
}