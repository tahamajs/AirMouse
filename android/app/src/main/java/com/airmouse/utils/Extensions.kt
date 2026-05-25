package com.airmouse.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.postValueIfNotSame(value: T) {
    if (this.value != value) postValue(value)
}package com.airmouse.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Posts a new value only if it differs from the current one.
 * Prevents unnecessary UI updates.
 */
fun <T> MutableLiveData<T>.postValueIfNotSame(value: T) {
    if (this.value != value) postValue(value)
}

/**
 * Converts degrees to radians.
 */
fun Float.degToRad(): Float = this * (kotlin.math.PI.toFloat() / 180f)

/**
 * Converts radians to degrees.
 */
fun Float.radToDeg(): Float = this * (180f / kotlin.math.PI.toFloat())