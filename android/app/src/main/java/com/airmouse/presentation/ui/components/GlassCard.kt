package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    content: @Composable () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF0F1115)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkTheme) listOf(
                        Color(0xFF1A1D24).copy(alpha = 0.85f),
                        Color(0xFF0F1115).copy(alpha = 0.95f)
                    ) else listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color(0xFFF8FAFC).copy(alpha = 0.9f)
                    )
                )
            ),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = elevation
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            content()
        }
    }
}

@Composable
fun AnimatedGlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    glowIntensity: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val animatedIntensity by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "intensity"
    )
    
    val finalGlow = if (glowIntensity > 0) glowIntensity else animatedIntensity

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        glowColor.copy(alpha = finalGlow * 0.15f),
                        glowColor.copy(alpha = finalGlow * 0.05f)
                    )
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = (finalGlow * 16).dp
        )
    ) {
        content()
    }
}

@Composable
fun HolographicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holographic")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )
    
    val colors = listOf(
        Color.hsv(hue, 0.8f, 1f).copy(alpha = 0.15f),
        Color.hsv((hue + 120) % 360, 0.8f, 1f).copy(alpha = 0.1f),
        Color.hsv((hue + 240) % 360, 0.8f, 1f).copy(alpha = 0.15f)
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = colors
                )
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        content()
    }
}