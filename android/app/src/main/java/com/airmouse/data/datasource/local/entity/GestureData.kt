package com.airmouse.data.datasource.local

data class GestureData(
    val samples: List<FloatArray>,
    val labels: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
