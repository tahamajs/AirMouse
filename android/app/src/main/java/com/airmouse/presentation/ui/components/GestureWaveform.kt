package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.sin

@Composable
fun GestureWaveform(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    animated: Boolean = true,
    showPeaks: Boolean = true,
    peakColor: Color = Color(0xFFFF5722)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind { drawRect(backgroundColor) }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val maxAmplitude = height / 2 - 10f
            
            if (dataPoints.isEmpty()) {
                // Draw placeholder when no data
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2f
                )
                return@Canvas
            }
            
            val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)
            
            // Draw background grid
            val gridColor = Color.White.copy(alpha = 0.05f)
            for (i in 0..4) {
                val y = centerY - maxAmplitude + (maxAmplitude * 2 * i / 4)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }
            
            // Draw zero line
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            
            // Draw main waveform
            val path = Path().apply {
                moveTo(0f, centerY - dataPoints.first().coerceIn(-maxAmplitude, maxAmplitude))
                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = centerY - value.coerceIn(-maxAmplitude, maxAmplitude)
                    lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f)
            )
            
            // Fill area under curve
            val fillPath = Path().apply {
                moveTo(0f, centerY)
                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = centerY - value.coerceIn(-maxAmplitude, maxAmplitude)
                    lineTo(x, y)
                }
                lineTo(width, centerY)
                close()
            }
            
            drawPath(
                path = fillPath,
                color = color.copy(alpha = 0.15f),
                style = Stroke(width = 0f)
            )
            
            // Draw peak markers
            if (showPeaks && dataPoints.isNotEmpty()) {
                val maxValue = dataPoints.maxOrNull() ?: 0f
                val minValue = dataPoints.minOrNull() ?: 0f
                val maxIndex = dataPoints.indexOf(maxValue)
                val minIndex = dataPoints.indexOf(minValue)
                
                drawCircle(
                    color = peakColor,
                    radius = 6f,
                    center = Offset(maxIndex * stepX, centerY - maxValue.coerceIn(-maxAmplitude, maxAmplitude))
                )
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = 6f,
                    center = Offset(minIndex * stepX, centerY - minValue.coerceIn(-maxAmplitude, maxAmplitude))
                )
            }
        }
        
        // Animated glow overlay
        if (animated && dataPoints.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.1f * glowIntensity),
                                    Color.Transparent,
                                    color.copy(alpha = 0.1f * glowIntensity)
                                )
                            )
                        )
                    }
            )
        }
        
        // Peak labels
        if (showPeaks && dataPoints.isNotEmpty()) {
            val maxValue = dataPoints.maxOrNull() ?: 0f
            val minValue = dataPoints.minOrNull() ?: 0f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peak: ${"%.1f".format(maxValue)}",
                    fontSize = 10.sp,
                    color = Color(0xFFFF5722)
                )
                Text(
                    text = "Trough: ${"%.1f".format(minValue)}",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun RealTimeGestureWaveform(
    gestureData: MutableStateFlow<List<Float>>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    windowSize: Int = 100
) {
    val data by gestureData.collectAsState()
    
    GestureWaveform(
        dataPoints = data.takeLast(windowSize),
        modifier = modifier,
        color = color,
        animated = true,
        showPeaks = true
    )
}
