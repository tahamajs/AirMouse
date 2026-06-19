package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
    var animatedValue by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetValue) {
        animate(
            initialValue = animatedValue,
            targetValue = targetValue,
            animationSpec = tween(duration, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedValue = value
        }
    }
    
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
    var currentPercentage by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(targetPercentage) {
        animate(
            initialValue = 0,
            targetValue = targetPercentage,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        ) { value, _ ->
            currentPercentage = value
        }
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                when {
                    targetPercentage >= 80 -> androidx.compose.material.icons.Icons.Default.TrendingUp
                    targetPercentage >= 50 -> androidx.compose.material.icons.Icons.Default.TrendingFlat
                    else -> androidx.compose.material.icons.Icons.Default.TrendingDown
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
