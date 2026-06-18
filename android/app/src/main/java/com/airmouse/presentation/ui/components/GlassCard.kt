package com.airmouse.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism Card - Frosted glass effect card
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        onClick = onClick ?: {},
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
