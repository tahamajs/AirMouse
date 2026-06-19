package com.airmouse.utils

import android.graphics.Color
import java.util.Locale

object ColorUtils {

    fun alphaBlend(foreground: Int, background: Int, alpha: Float): Int {
        val fRed = Color.red(foreground)
        val fGreen = Color.green(foreground)
        val fBlue = Color.blue(foreground)

        val bRed = Color.red(background)
        val bGreen = Color.green(background)
        val bBlue = Color.blue(background)

        val a = alpha.coerceIn(0f, 1f)
        val invA = 1f - a

        val r = (fRed * a + bRed * invA).toInt()
        val g = (fGreen * a + bGreen * invA).toInt()
        val b = (fBlue * a + bBlue * invA).toInt()

        return Color.rgb(r, g, b)
    }

    fun isLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }

    fun darken(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1 - factor)).toInt()
        val g = (Color.green(color) * (1 - factor)).toInt()
        val b = (Color.blue(color) * (1 - factor)).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun intToHex(color: Int): String = String.format(Locale.US, "#%06X", 0xFFFFFF and color)

    fun hexToInt(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.BLACK
        }
    }
}
