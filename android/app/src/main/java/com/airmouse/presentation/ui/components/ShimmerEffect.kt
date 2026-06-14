package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2B3341),
                        Color(0xFF3B4353),
                        Color(0xFF2B3341)
                    ),
                    start = Offset(shimmerTranslate, 0f),
                    end = Offset(shimmerTranslate + 200f, 200f)
                ),
                shape = shape
            )
    )
}

@Composable
fun ShimmerLoadingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ShimmerEffect(modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(bottom = 8.dp))
        ShimmerEffect(modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(16.dp)
            .padding(bottom = 4.dp))
        ShimmerEffect(modifier = Modifier
            .fillMaxWidth(0.5f)
            .height(12.dp))
    }
}