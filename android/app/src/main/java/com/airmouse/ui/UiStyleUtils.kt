// app/src/main/java/com/airmouse/ui/UiStyleUtils.kt
package com.airmouse.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.*
import androidx.core.animation.doOnEnd
import com.google.android.material.button.MaterialButton

/**
 * Utility class for UI animations and styling
 */
object UiStyleUtils {

    private const val DEFAULT_ANIMATION_DURATION = 300L
    private const val PULSE_DURATION = 200L

    /**
     * Apply pulse animation to a view
     */
    fun pulse(view: View, duration: Long = PULSE_DURATION, onEnd: (() -> Unit)? = null) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.05f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.05f, 1f)

        scaleX.duration = duration
        scaleY.duration = duration

        scaleX.start()
        scaleY.start()

        if (onEnd != null) {
            scaleX.doOnEnd { onEnd() }
        }
    }

    /**
     * Apply fade animation to a view
     */
    fun fade(view: View, fromAlpha: Float, toAlpha: Float, duration: Long = DEFAULT_ANIMATION_DURATION) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, fromAlpha, toAlpha)
        animator.duration = duration
        animator.start()
    }

    /**
     * Apply slide animation to a view
     */
    fun slide(view: View, fromX: Float, toX: Float, fromY: Float, toY: Float) {
        val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, fromX, toX)
        val animatorY = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, fromY, toY)
        animator.duration = DEFAULT_ANIMATION_DURATION
        animatorY.duration = DEFAULT_ANIMATION_DURATION
        animator.start()
        animatorY.start()
    }

    /**
     * Apply shake animation to a view
     */
    fun shake(view: View) {
        val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 10f, -10f, 10f, -10f, 5f, -5f, 0f)
        animator.duration = 500
        animator.start()
    }

    /**
     * Apply button click feedback
     */
    fun buttonClickFeedback(button: MaterialButton, onClick: () -> Unit) {
        button.isEnabled = false
        pulse(button, PULSE_DURATION) {
            button.isEnabled = true
            onClick()
        }
    }

    /**
     * Apply cross-fade transition between two views
     */
    fun crossFade(viewOut: View, viewIn: View, duration: Long = DEFAULT_ANIMATION_DURATION) {
        viewOut.animate()
            .alpha(0f)
            .setDuration(duration)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    viewOut.visibility = View.GONE
                    viewIn.visibility = View.VISIBLE
                    viewIn.alpha = 0f
                    viewIn.animate()
                        .alpha(1f)
                        .setDuration(duration)
                        .start()
                }
            })
            .start()
    }

    /**
     * Apply ripple effect to a view
     */
    fun ripple(view: View) {
        val ripple = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.2f, 1f)
        ripple.duration = 150
        ripple.start()
    }

    /**
     * Apply expand animation to a view
     */
    fun expand(view: View, targetHeight: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        val animator = ValueAnimator.ofInt(0, targetHeight)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.duration = DEFAULT_ANIMATION_DURATION
        animator.start()
    }

    /**
     * Apply collapse animation to a view
     */
    fun collapse(view: View) {
        val initialHeight = view.height
        val animator = ValueAnimator.ofInt(initialHeight, 0)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            view.layoutParams.height = value
            view.requestLayout()
        }
        animator.duration = DEFAULT_ANIMATION_DURATION
        animator.start()
    }

    /**
     * Apply rotate animation to a view
     */
    fun rotate(view: View, fromDegrees: Float, toDegrees: Float) {
        val animator = ObjectAnimator.ofFloat(view, View.ROTATION, fromDegrees, toDegrees)
        animator.duration = DEFAULT_ANIMATION_DURATION
        animator.start()
    }

    /**
     * Apply bounce animation to a view
     */
    fun bounce(view: View) {
        val animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -20f, 0f, -10f, 0f)
        animator.duration = 500
        animator.start()
    }

    /**
     * Apply fade-in animation with slide from bottom
     */
    fun fadeInSlideUp(view: View) {
        view.alpha = 0f
        view.translationY = 100f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(DEFAULT_ANIMATION_DURATION)
            .start()
    }

    /**
     * Apply fade-out animation with slide to bottom
     */
    fun fadeOutSlideDown(view: View, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(DEFAULT_ANIMATION_DURATION)
            .withEndAction {
                view.visibility = View.GONE
                onEnd?.invoke()
            }
            .start()
    }

    /**
     * Apply typing animation to a TextView
     */
    fun animateText(view: android.widget.TextView, text: String, delayBetweenChars: Long = 30) {
        var currentIndex = 0
        val timer = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (currentIndex <= text.length) {
                    view.text = text.substring(0, currentIndex)
                    currentIndex++
                    timer.postDelayed(this, delayBetweenChars)
                }
            }
        }
        timer.post(runnable)
    }

    /**
     * Apply progress animation to a ProgressBar
     */
    fun animateProgress(progressBar: android.widget.ProgressBar, targetProgress: Int, duration: Long = DEFAULT_ANIMATION_DURATION) {
        val animator = ValueAnimator.ofInt(progressBar.progress, targetProgress)
        animator.addUpdateListener {
            progressBar.progress = it.animatedValue as Int
        }
        animator.duration = duration
        animator.start()
    }

    /**
     * Apply color transition to a view
     */
    fun animateColorChange(view: View, startColor: Int, endColor: Int) {
        val animator = ValueAnimator.ofArgb(startColor, endColor)
        animator.addUpdateListener {
            view.setBackgroundColor(it.animatedValue as Int)
        }
        animator.duration = DEFAULT_ANIMATION_DURATION
        animator.start()
    }

    /**
     * Apply border animation to a view (requires custom implementation)
     */
    fun animateBorder(view: View, strokeWidth: Float) {
        // This is a placeholder – requires custom drawable or ViewOutlineProvider.
        // For simplicity, we skip actual implementation.
    }

    /**
     * Apply heartbeat animation to a view
     */
    fun heartbeat(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.3f, 1f)
        scaleX.duration = 300
        scaleY.duration = 300
        scaleX.start()
        scaleY.start()
    }

    /**
     * Apply blink animation to a view
     */
    fun blink(view: View, duration: Long = 500) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f, 1f, 0f, 1f)
        animator.duration = duration
        animator.start()
    }

    /**
     * Apply successful checkmark animation
     */
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
}