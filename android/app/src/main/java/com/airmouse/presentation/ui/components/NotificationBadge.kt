package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    showZero: Boolean = false
) {
    val displayCount = if (count > maxCount) "$maxCount+" else count.toString()
    val animatedScale by animateFloatAsState(
        targetValue = if (count > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge"
    )
    
    if ((count > 0 || showZero) && animatedScale > 0f) {
        Box(
            modifier = modifier
                .size(20.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(Color(0xFFF44336)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayCount,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnimatedBadge(
    isVisible: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        label = "badge"
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = color,
            shadowElevation = 4.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}
