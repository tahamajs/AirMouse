package com.airmouse.presentation.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.airmouse.R

/**
 * Data class representing a single onboarding page
 */
data class OnboardingItem(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String,
    val backgroundColor: Color = Color(0xFF0F172A),
    val accentColor: Color = Color(0xFF6366F1)
) {
    companion object {
        fun getDefaultItems(): List<OnboardingItem> = listOf(
            OnboardingItem(
                imageRes = R.drawable.ic_air_mouse,
                title = "Welcome to Air Mouse Pro",
                description = "Turn your phone into a wireless mouse using advanced motion sensors and AI technology",
                backgroundColor = Color(0xFF0F172A),
                accentColor = Color(0xFF6366F1)
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_gesture,
                title = "Intuitive Gestures",
                description = "Rotate, tilt, and move naturally to control your PC cursor. Quick flips for clicks, scrolls, and more",
                backgroundColor = Color(0xFF1E1B4B),
                accentColor = Color(0xFF8B5CF6)
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_wifi,
                title = "Quick Connection",
                description = "Scan QR code or enter IP address to connect instantly. Automatic reconnection remembers your last server",
                backgroundColor = Color(0xFF064E3B),
                accentColor = Color(0xFF10B981)
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_voice,
                title = "Voice Commands",
                description = "Say 'click', 'scroll up', 'next slide' and more. Hands-free control for presentations and daily use",
                backgroundColor = Color(0xFF4C1D95),
                accentColor = Color(0xFFA855F7)
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_proximity,
                title = "Proximity Lock",
                description = "Auto-locks your computer when you walk away and unlocks when you return using Bluetooth distance detection",
                backgroundColor = Color(0xFF9A3412),
                accentColor = Color(0xFFF97316)
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_about,
                title = "Ready to Go!",
                description = "You're all set to experience the future of wireless control. Let's get started!",
                backgroundColor = Color(0xFF065F46),
                accentColor = Color(0xFF34D399)
            )
        )
    }
}