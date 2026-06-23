
package com.airmouse.utils

import androidx.lifecycle.MutableLiveData

inline fun <T> MutableLiveData<T>.postValueIfNew(value: T) {
    if (this.value != value) {
        this.postValue(value)
    }
}

fun Float.degToRad(): Float = this * (kotlin.math.PI.toFloat() / 180f)

fun Float.radToDeg(): Float = this * (180f / kotlin.math.PI.toFloat())

