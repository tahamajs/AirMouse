// app/src/main/java/com/airmouse/utils/AnimationUtils.kt
package com.airmouse.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

object AnimationUtils {

    enum class AnimationType {
        FADE_IN, FADE_OUT, SLIDE_UP, SLIDE_DOWN, SLIDE_LEFT, SLIDE_RIGHT, BOUNCE, PULSE
    }

    fun fadeIn(view: View, duration: Long = 300) {
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun fadeOut(view: View, duration: Long = 300, onComplete: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onComplete?.invoke() }
            .start()
    }

    fun slideUp(view: View, duration: Long = 300) {
        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun slideDown(view: View, duration: Long = 300, startY: Float) {
        view.translationY = startY
        view.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun pulse(view: View, duration: Long = 200, scale: Float = 1.05f) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .start()
            }
            .start()
    }

    fun shake(view: View, duration: Long = 500, intensity: Float = 10f) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            val shakeX = sin(progress.toDouble() * PI * 4).toFloat() * intensity * (1 - progress)
            view.translationX = shakeX
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.translationX = 0f
            }
        })
        animator.start()
    }

    @Composable
    fun infinitePulseAnimation(): InfiniteRepeatableSpec<Float> {
        return infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0 with LinearEasing
                1.05f at 500 with FastOutSlowInEasing
                1f at 1000 with FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Reverse
        )
    }

    @Composable
    fun fadeInAnimation(targetState: Boolean): Float {
        val alpha by animateFloatAsState(
            targetValue = if (targetState) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "fadeIn"
        )
        return alpha
    }

    @Composable
    fun slideInAnimation(targetState: Boolean): Dp {
        val offset by animateDpAsState(
            targetValue = if (targetState) 0.dp else 100.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "slideIn"
        )
        return offset
    }
}
