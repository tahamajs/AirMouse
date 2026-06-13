// app/src/main/java/com/airmouse/ui/components/GestureWaveform.kt
package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun GestureWaveform(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path().apply {
            moveTo(0f, height / 2)
            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height / 2 - (value * height / 2)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )

        // Animated waveform tail
        val tailPath = Path().apply {
            moveTo(0f, height / 2)
            dataPoints.takeLast(20).forEachIndexed { index, value ->
                val x = index * stepX + waveOffset % stepX
                val y = height / 2 - (value * height / 2)
                lineTo(x, y)
            }
        }

        drawPath(
            path = tailPath,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = 2f)
        )
    }
}// app/src/main/java/com/airmouse/ui/components/GestureWaveform.kt
package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun GestureWaveform(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path().apply {
            moveTo(0f, height / 2)
            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height / 2 - (value * height / 2)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )

        // Animated waveform tail
        val tailPath = Path().apply {
            moveTo(0f, height / 2)
            dataPoints.takeLast(20).forEachIndexed { index, value ->
                val x = index * stepX + waveOffset % stepX
                val y = height / 2 - (value * height / 2)
                lineTo(x, y)
            }
        }

        drawPath(
            path = tailPath,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = 2f)
        )
    }
}