package com.airmouse.ui

import android.animation.ArgbEvaluator
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import android.widget.TextView
import androidx.cardview.widget.CardView

object UiStyleUtils {
    fun styleCard(cardView: MaterialCardView) {
        cardView.useCompatPadding = true
        cardView.strokeWidth = 1
        cardView.strokeColor = cardView.context.getColor(com.airmouse.R.color.card_stroke)
    }

    fun styleCard(cardView: CardView) {
        cardView.useCompatPadding = true
    }

    fun animateIn(view: View, delayMs: Long = 0L) {
        view.alpha = 0f
        view.translationY = 16f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delayMs)
            .setDuration(220L)
            .start()
    }

    fun pulse(view: View) {
        view.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(90L)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(90L)
                    .start()
            }
            .start()
    }
}
