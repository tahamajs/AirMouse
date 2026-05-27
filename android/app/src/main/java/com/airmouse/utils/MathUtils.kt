package com.airmouse.utils

import kotlin.math.PI

object MathUtils {
    fun radToDeg(rad: Float): Float = rad * (180f / PI.toFloat())
    fun degToRad(deg: Float): Float = deg * (PI.toFloat() / 180f)
    fun clamp(value: Float, min: Float, max: Float): Float = when {
        value < min -> min
        value > max -> max
        else -> value
    }
}