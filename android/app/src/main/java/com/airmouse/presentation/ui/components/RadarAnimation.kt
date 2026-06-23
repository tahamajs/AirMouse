package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarAnimation(
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    size: Int = 120
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = if (isActive) tween(2000, easing = LinearEasing) else snap(),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size / 2f
            val centerY = size / 2f
            val radius = size / 2f - 10f
            
            
            drawCircle(
                color = Color(0xFF00BCD4).copy(alpha = 0.1f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
            
            drawCircle(
                color = Color(0xFF00BCD4).copy(alpha = 0.05f),
                radius = radius * 0.66f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            
            drawCircle(
                color = Color(0xFF00BCD4).copy(alpha = 0.03f),
                radius = radius * 0.33f,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            
            
            drawLine(
                color = Color(0xFF00BCD4).copy(alpha = 0.2f),
                start = Offset(centerX, centerY - radius),
                end = Offset(centerX, centerY + radius),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0xFF00BCD4).copy(alpha = 0.2f),
                start = Offset(centerX - radius, centerY),
                end = Offset(centerX + radius, centerY),
                strokeWidth = 1f
            )
            
            
            val angleRad = Math.toRadians(rotation.toDouble()).toFloat()
            val endX = centerX + radius * cos(angleRad)
            val endY = centerY + radius * sin(angleRad)
            
            drawLine(
                color = Color(0xFF00BCD4).copy(alpha = 0.6f),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
            
            
            drawArc(
                color = Color(0xFF00BCD4).copy(alpha = 0.15f),
                startAngle = rotation - 15f,
                sweepAngle = 30f,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            
            drawCircle(
                color = Color(0xFF00BCD4),
                radius = 4f,
                center = Offset(centerX, centerY)
            )
            
            
            if (isActive) {
                drawCircle(
                    color = Color(0xFF00BCD4).copy(alpha = 0.3f * (1 - pulse)),
                    radius = radius * (0.3f + pulse * 0.7f),
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
