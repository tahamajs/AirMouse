package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.ThemeManager
import kotlinx.coroutines.launch

class ThemesFragment : Fragment() {

    private lateinit var themeManager: ThemeManager
    private lateinit var themeGroup: RadioGroup

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_themes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        themeManager = ThemeManager(requireContext())
        themeGroup = view.findViewById(R.id.theme_radio_group)

        lifecycleScope.launch {
            val currentTheme = themeManager.getTheme()
            when (currentTheme) {
                "light" -> themeGroup.check(R.id.theme_light)
                "dark" -> themeGroup.check(R.id.theme_dark)
                "pure_black" -> themeGroup.check(R.id.theme_pure_black)
                "high_contrast" -> themeGroup.check(R.id.theme_high_contrast)
                else -> themeGroup.check(R.id.theme_system)
            }
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_light -> "light"
                R.id.theme_dark -> "dark"
                R.id.theme_pure_black -> "pure_black"
                R.id.theme_high_contrast -> "high_contrast"
                else -> "system"
            }
            lifecycleScope.launch {
                themeManager.setTheme(theme)
            }
        }
    }
}