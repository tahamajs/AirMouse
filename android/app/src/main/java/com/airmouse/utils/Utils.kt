// app/src/main/java/com/airmouse/utils/Utils.kt
package com.airmouse.utils

import androidx.lifecycle.MutableLiveData

/**
 * Extension function to post a value only if it differs from the current one.
 * Prevents unnecessary UI updates.
 * Marked as inline to keep runtime scopes unique from identical codebase utilities.
 */
inline fun <T> MutableLiveData<T>.postValueIfNew(value: T) {
    if (this.value != value) {
        this.postValue(value)
    }
}

/**
 * Converts degrees to radians.
 */
fun Float.degToRad(): Float = this * (kotlin.math.PI.toFloat() / 180f)

/**
 * Converts radians to degrees.
 */
fun Float.radToDeg(): Float = this * (180f / kotlin.math.PI.toFloat())

