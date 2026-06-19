// app/src/main/java/com/airmouse/utils/ResourceHelper.kt
package com.airmouse.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getString(resId: Int): String {
        return context.getString(resId)
    }

    fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }

    fun getColor(resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }

    fun getDrawable(resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)
    }

    fun getDimension(resId: Int): Float {
        return context.resources.getDimension(resId)
    }

    fun getDimensionPixelSize(resId: Int): Int {
        return context.resources.getDimensionPixelSize(resId)
    }

    fun getBoolean(resId: Int): Boolean {
        return context.resources.getBoolean(resId)
    }

    fun getInteger(resId: Int): Int {
        return context.resources.getInteger(resId)
    }

    fun getStringArray(resId: Int): Array<String> {
        return context.resources.getStringArray(resId)
    }

    fun getIntArray(resId: Int): IntArray {
        return context.resources.getIntArray(resId)
    }

    fun getTypedArray(resId: Int): Resources.Theme? {
        return context.theme
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return context.resources.displayMetrics
    }

    fun getScreenWidth(): Int {
        return getDisplayMetrics().widthPixels
    }

    fun getScreenHeight(): Int {
        return getDisplayMetrics().heightPixels
    }

    fun dpToPx(dp: Float): Float {
        return dp * getDisplayMetrics().density
    }

    fun pxToDp(px: Float): Float {
        return px / getDisplayMetrics().density
    }

    fun spToPx(sp: Float): Float {
        return sp * getDisplayMetrics().scaledDensity
    }
}
