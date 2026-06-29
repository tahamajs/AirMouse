package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun VoiceWaveAnimation(
    isActive: Boolean = true,
    amplitude: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val phases = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase_$index"
        )
    }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / 12
        val spacing = barWidth * 0.5f
        
        phases.forEachIndexed { index, phase ->
            val barHeight = if (isActive) {
                20f + sin(Math.toRadians(phase.value.toDouble())).toFloat() * 20f * amplitude
            } else {
                10f
            }
            
            val x = index * (barWidth + spacing) + spacing
            
            drawRoundRect(
                color = Color(0xFF00BCD4),
                topLeft = Offset(x, centerY - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight * 2),
                cornerRadius = CornerRadius(4f)
            )
        }
    }
}
