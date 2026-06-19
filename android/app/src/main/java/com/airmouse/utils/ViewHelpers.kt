// app/src/main/java/com/airmouse/utils/ViewHelpers.kt
package com.airmouse.utils

import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.airmouse.R

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.isVisible(): Boolean = visibility == View.VISIBLE

fun View.fadeIn(duration: Long = 300) {
    val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
    animation.duration = duration
    startAnimation(animation)
    show()
}

fun View.fadeOut(duration: Long = 300) {
    val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_out)
    animation.duration = duration
    startAnimation(animation)
    hide()
}

fun ImageView.setTint(colorRes: Int) {
    setColorFilter(ContextCompat.getColor(context, colorRes), android.graphics.PorterDuff.Mode.SRC_IN)
}

fun ImageView.clearTint() {
    clearColorFilter()
}