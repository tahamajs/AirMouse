package com.airmouse

class OnboardingActivity {
    val items = listOf(
        OnboardingItem(
            imageRes = R.drawable.ic_welcome,
            title = "Welcome to Air Mouse Pro",
            description = "Turn your phone into a smart, AI‑powered remote control.",
            showStepIndicator = true,
            stepNumber = 1,
            totalSteps = 3,
            showHint = true
        ),
        OnboardingItem(
            imageRes = R.drawable.ic_gesture_recognition,
            title = "Custom Gestures",
            description = "Record and train your own gestures with AI.",
            showNewBadge = true
        ),
        OnboardingItem(
            imageRes = R.drawable.ic_proximity_lock,
            title = "Proximity Lock",
            description = "Walk away – your screen locks automatically."
        )
    )
}