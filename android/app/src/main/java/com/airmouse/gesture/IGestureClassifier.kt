// app/src/main/java/com/airmouse/gesture/IGestureClassifier.kt
package com.airmouse.gesture

interface IGestureClassifier {
    fun start()
    fun stop()
    fun isRunning(): Boolean
    fun setOnGestureDetected(listener: (String, Float) -> Unit)
}