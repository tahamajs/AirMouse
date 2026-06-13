// app/src/main/java/com/airmouse/utils/MathUtils.kt
package com.airmouse.utils

import kotlin.math.*

object MathUtils {

    fun radToDeg(rad: Float): Float = rad * (180f / PI.toFloat())
    fun degToRad(deg: Float): Float = deg * (PI.toFloat() / 180f)

    fun clamp(value: Float, min: Float, max: Float): Float = when {
        value < min -> min
        value > max -> max
        else -> value
    }

    fun clamp(value: Int, min: Int, max: Int): Int = when {
        value < min -> min
        value > max -> max
        else -> value
    }

    fun clamp(value: Long, min: Long, max: Long): Long = when {
        value < min -> min
        value > max -> max
        else -> value
    }

    fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

    fun mapRange(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        return toLow + (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow)
    }

    fun magnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

    fun normalize(value: Float, min: Float, max: Float): Float = (value - min) / (max - min)

    fun denormalize(value: Float, min: Float, max: Float): Float = min + value * (max - min)

    fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun exponentialDecay(value: Float, target: Float, decay: Float): Float {
        return value + (target - value) * decay
    }
}