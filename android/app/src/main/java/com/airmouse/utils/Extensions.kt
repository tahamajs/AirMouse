package com.airmouse.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<T>.postValueIfNotSame(value: T) {
    if (this.value != value) postValue(value)
}