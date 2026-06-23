package com.airmouse.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView

object UiStyleUtils {

    private const val DEFAULT_ANIMATION_DURATION = 300L

    fun animateProgress(progressBar: android.widget.ProgressBar, targetProgress: Int, duration: Long = DEFAULT_ANIMATION_DURATION) {
        val animator = ValueAnimator.ofInt(progressBar.progress, targetProgress)
        animator.addUpdateListener {
            progressBar.progress = it.animatedValue as Int
        }
        animator.duration = duration
        animator.start()
    }

    fun animateSuccess(view: View) {
        view.alpha = 0f
        view.scaleX = 0f
        view.scaleY = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    fun shake(view: View) {
        val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 10f, -10f, 10f, -10f, 5f, -5f, 0f)
        animator.duration = 500
        animator.start()
    }

    fun styleCard(card: Any) {
        when (card) {
            is MaterialCardView -> {
                card.isUseCompatPadding = true
                card.radius = 16f
            }
            is CardView -> {
                card.useCompatPadding = true
                card.radius = 16f
            }
        }
    }
}
