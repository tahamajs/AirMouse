// app/src/main/java/com/airmouse/domain/model/CustomGestureTemplate.kt
package com.airmouse.domain.model

data class CustomGestureTemplate(
    val id: String,
    val name: String,
    val threshold: Float = 10f,
    val samples: List<FloatArray> = emptyList(), // each float array of 6 values: gyroX,gyroY,gyroZ,accelX,accelY,accelZ
    val features: FloatArray = floatArrayOf(), // extracted features for classification
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isTrained: Boolean = false,
    val confidence: Float = 0f
)