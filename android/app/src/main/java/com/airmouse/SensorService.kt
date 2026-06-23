package com.airmouse

interface SensorService {
    fun start()
    fun stop()
    fun setSamplingRate(delay: Int)
}
