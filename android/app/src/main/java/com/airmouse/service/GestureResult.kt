package com.airmouse.service

enum class GestureConfidence {
    LOW,
    MEDIUM,
    HIGH
}

data class GestureResult(
    val gesture: String,
    val confidence: Float,
    val confidenceLevel: GestureConfidence = when {
        confidence >= 0.85f -> GestureConfidence.HIGH
        confidence >= 0.6f -> GestureConfidence.MEDIUM
        else -> GestureConfidence.LOW
    },
    val accepted: Boolean = confidence >= 0.7f,
    val timestamp: Long = System.currentTimeMillis()
)
