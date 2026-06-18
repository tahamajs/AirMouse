package com.airmouse.ui.onboarding

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.airmouse.R

data class OnboardingItem(
    @get:DrawableRes val imageRes: Int,
    val title: String,
    val description: String,
    @get:ColorRes val bgColor: Int = R.color.onboarding_1_bg
)