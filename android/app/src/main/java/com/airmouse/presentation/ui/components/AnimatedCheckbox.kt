package com.airmouse.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String = ""
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkbox"
    )
    
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                
                drawCircle(
                    color = if (checked) Color(0xFF00BCD4) else Color(0xFF96A0AE),
                    radius = 12f,
                    center = Offset(12f, 12f)
                )
                
                
                if (animatedProgress > 0f) {
                    val startX = 6f
                    val startY = 12f
                    val midX = 10f
                    val midY = 16f
                    val endX = 18f
                    val endY = 8f
                    
                    val progress = animatedProgress
                    val currentMidX = startX + (midX - startX) * progress
                    val currentMidY = startY + (midY - startY) * progress
                    val currentEndX = midX + (endX - midX) * progress
                    val currentEndY = midY + (endY - midY) * progress
                    
                    val path = Path().apply {
                        moveTo(startX, startY)
                        lineTo(currentMidX, currentMidY)
                        lineTo(currentEndX, currentEndY)
                    }
                    
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }
        }
        
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}