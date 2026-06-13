// app/src/main/java/com/airmouse/utils/ConversionUtils.kt
package com.airmouse.utils

/**
 * Unit conversion utilities.
 */
object ConversionUtils {

    fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()
    fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()

    fun msToSeconds(ms: Long): Float = ms / 1000f
    fun secondsToMs(seconds: Float): Long = (seconds * 1000).toLong()

    fun bytesToMB(bytes: Long): Float = bytes / (1024f * 1024f)
    fun bytesToKB(bytes: Long): Float = bytes / 1024f

    fun bpsToMbps(bps: Long): Float = bps / 1_000_000f
}// app/src/main/java/com/airmouse/utils/ConversionUtils.kt
package com.airmouse.utils

/**
 * Unit conversion utilities.
 */
object ConversionUtils {

    fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()
    fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()

    fun msToSeconds(ms: Long): Float = ms / 1000f
    fun secondsToMs(seconds: Float): Long = (seconds * 1000).toLong()

    fun bytesToMB(bytes: Long): Float = bytes / (1024f * 1024f)
    fun bytesToKB(bytes: Long): Float = bytes / 1024f

    fun bpsToMbps(bps: Long): Float = bps / 1_000_000f
}