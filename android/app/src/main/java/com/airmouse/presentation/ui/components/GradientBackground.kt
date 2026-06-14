package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFF1A1D24),
        Color(0xFF0F1115),
        Color(0xFF1A1D24)
    ),
    speed: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((20000 / speed).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = colors,
                    startX = offset * 1000f,
                    endX = (offset + 0.5f) * 1000f
                )
            )
    )
}

@Composable
fun RadialGradientBackground(
    modifier: Modifier = Modifier,
    centerColor: Color = Color(0xFF00BCD4).copy(alpha = 0.1f),
    edgeColor: Color = Color.Transparent
) {
    val infiniteTransition = rememberInfiniteTransition()
    val radius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(centerColor, edgeColor),
                    radius = radius
                )
            )
    )
}