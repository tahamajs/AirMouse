package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    duration: Int = 1000
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(duration, easing = FastOutSlowInEasing),
        label = "animated_counter"
    )
    
    Text(
        text = "$prefix$animatedValue$suffix",
        modifier = modifier,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun AnimatedPercentage(
    targetPercentage: Int,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val currentPercentage by animateIntAsState(
        targetValue = targetPercentage,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "animated_percentage"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
                Icon(
                    when {
                    targetPercentage >= 80 -> Icons.Default.TrendingUp
                    targetPercentage >= 50 -> Icons.Default.TrendingFlat
                    else -> Icons.Default.TrendingDown
                },
                contentDescription = null,
                tint = when {
                    targetPercentage >= 80 -> Color(0xFF4CAF50)
                    targetPercentage >= 50 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "$currentPercentage%",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                targetPercentage >= 80 -> Color(0xFF4CAF50)
                targetPercentage >= 50 -> Color(0xFFFFC107)
                else -> Color(0xFFF44336)
            }
        )
    }
}
