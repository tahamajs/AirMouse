package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun HolographicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium
) {
    val infiniteTransition = rememberInfiniteTransition()
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )
    
    val colors = listOf(
        Color.hsv(hue, 1f, 1f),
        Color.hsv((hue + 120) % 360, 1f, 1f),
        Color.hsv((hue + 240) % 360, 1f, 1f)
    )
    
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset.Infinite
            ),
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glitchOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitch"
    )
    
    Box(modifier = modifier) {
        // Red channel
        Text(
            text = text,
            modifier = Modifier.offset(x = glitchOffset.dp, y = 0.dp),
            style = style.copy(
                color = Color.Red.copy(alpha = 0.5f)
            )
        )
        
        // Blue channel
        Text(
            text = text,
            modifier = Modifier.offset(x = (-glitchOffset).dp, y = 0.dp),
            style = style.copy(
                color = Color.Blue.copy(alpha = 0.5f)
            )
        )
        
        // Main text
        Text(
            text = text,
            style = style,
            color = Color.White
        )
    }
}