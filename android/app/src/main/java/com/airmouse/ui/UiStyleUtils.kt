package com.airmouse.ui

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

object UiStyleUtils {
    fun styleCard(cardView: MaterialCardView) {
        cardView.radius = 24f
        cardView.strokeWidth = 1
        cardView.setCardBackgroundColor(0xFF1D2430.toInt())
        cardView.strokeColor = 0xFF2B3341.toInt()
        cardView.cardElevation = 0f
    }

    fun styleCard(cardView: CardView) {
        cardView.radius = 24f
        cardView.cardElevation = 0f
        cardView.setCardBackgroundColor(0xFF1D2430.toInt())
    }

    fun animateIn(view: View, delayMs: Long = 0L) {
        view.alpha = 0f
        view.translationY = 18f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(280)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    fun pulse(view: View) {
        ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.01f, 1f).apply {
            duration = 220
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.01f, 1f).apply {
            duration = 220
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
