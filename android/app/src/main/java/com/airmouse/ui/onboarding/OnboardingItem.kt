package com.airmouse.ui.onboarding

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.airmouse.R

/**
 * Data class representing a single onboarding page.
 *
 * @param imageRes The drawable resource ID for the illustration.
 * @param title The title text.
 * @param description The descriptive text.
 * @param bgColor The background color resource ID (defaults to the first page's color).
 */
data class OnboardingItem(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String,
    @ColorRes val bgColor: Int = R.color.onboarding_1_bg
)