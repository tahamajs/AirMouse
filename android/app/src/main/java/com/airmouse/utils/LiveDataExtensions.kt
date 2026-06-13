// app/src/main/java/com/airmouse/utils/LiveDataExtensions.kt
package com.airmouse.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

/**
 * Extension function to post a value only if it differs from the current one.
 * Prevents unnecessary UI updates.
 */
fun <T> MutableLiveData<T>.postValueIfNotSame(value: T) {
    if (this.value != value) {
        postValue(value)
    }
}

/**
 * Extension function to set value only if it differs from the current one.
 */
fun <T> MutableLiveData<T>.setValueIfNotSame(value: T) {
    if (this.value != value) {
        this.value = value
    }
}

/**
 * Combine two LiveData into one.
 */
fun <T1, T2, R> LiveData<T1>.combineWith(
    other: LiveData<T2>,
    combiner: (T1?, T2?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) { result.value = combiner(this.value, other.value) }
    result.addSource(other) { result.value = combiner(this.value, other.value) }
    return result
}

/**
 * Map LiveData to another type.
 */
fun <T, R> LiveData<T>.map(mapper: (T) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) { result.value = mapper(it) }
    return result
}

/**
 * Filter LiveData values.
 */
fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        if (predicate(it)) {
            result.value = it
        }
    }
    return result
}

/**
 * Distinct until changed.
 */
fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> {
    val result = MediatorLiveData<T>()
    var lastValue: T? = null
    result.addSource(this) {
        if (lastValue != it) {
            lastValue = it
            result.value = it
        }
    }
    return result
}// app/src/main/java/com/airmouse/utils/LiveDataExtensions.kt
package com.airmouse.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

/**
 * Extension function to post a value only if it differs from the current one.
 * Prevents unnecessary UI updates.
 */
fun <T> MutableLiveData<T>.postValueIfNotSame(value: T) {
    if (this.value != value) {
        postValue(value)
    }
}

/**
 * Extension function to set value only if it differs from the current one.
 */
fun <T> MutableLiveData<T>.setValueIfNotSame(value: T) {
    if (this.value != value) {
        this.value = value
    }
}

/**
 * Combine two LiveData into one.
 */
fun <T1, T2, R> LiveData<T1>.combineWith(
    other: LiveData<T2>,
    combiner: (T1?, T2?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) { result.value = combiner(this.value, other.value) }
    result.addSource(other) { result.value = combiner(this.value, other.value) }
    return result
}

/**
 * Map LiveData to another type.
 */
fun <T, R> LiveData<T>.map(mapper: (T) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) { result.value = mapper(it) }
    return result
}

/**
 * Filter LiveData values.
 */
fun <T> LiveData<T>.filter(predicate: (T) -> Boolean): LiveData<T> {
    val result = MediatorLiveData<T>()
    result.addSource(this) {
        if (predicate(it)) {
            result.value = it
        }
    }
    return result
}

/**
 * Distinct until changed.
 */
fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> {
    val result = MediatorLiveData<T>()
    var lastValue: T? = null
    result.addSource(this) {
        if (lastValue != it) {
            lastValue = it
            result.value = it
        }
    }
    return result
}