package com.airmouse.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PremiumDashboardBackground(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0C10),
                        Color(0xFF040507)
                    )
                )
            )
    ) {
        // Glowing Ambient Blobs (Glassmorphism vibe)
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-100).dp, y = 150.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF5722).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-150).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD500F9).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    backgroundAlpha: Float = 0.45f,
    borderAlpha: Float = 0.12f,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF13161F).copy(alpha = backgroundAlpha)
        ),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = borderAlpha),
                    Color.White.copy(alpha = borderAlpha * 0.15f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}
