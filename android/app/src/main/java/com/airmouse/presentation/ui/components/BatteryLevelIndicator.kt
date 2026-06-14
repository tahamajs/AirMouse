package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BatteryLevelIndicator(
    level: Int,
    isCharging: Boolean = false,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    val isLow = level < 20
    val color = when {
        isCharging -> Color(0xFF4CAF50)
        level > 50 -> Color(0xFF4CAF50)
        level > 20 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    
    val animatedLevel by animateIntAsState(
        targetValue = level,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "battery"
    )
    
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val batteryWidth = size.toFloat() - 8f
            val batteryHeight = size.toFloat() - 4f
            val fillWidth = batteryWidth * (animatedLevel / 100f)
            
            // Battery outline
            drawRoundRect(
                color = Color.Gray,
                topLeft = Offset(2f, 2f),
                size = Size(batteryWidth, batteryHeight),
                cornerRadius = CornerRadius(4f),
                style = Stroke(width = 2f)
            )
            
            // Battery fill
            drawRoundRect(
                color = color,
                topLeft = Offset(4f, 4f),
                size = Size(fillWidth - 4f, batteryHeight - 4f),
                cornerRadius = CornerRadius(2f)
            )
            
            // Battery terminal
            drawRoundRect(
                color = Color.Gray,
                topLeft = Offset(batteryWidth + 2f, batteryHeight / 3),
                size = Size(4f, batteryHeight / 3),
                cornerRadius = CornerRadius(2f)
            )
            
            // Charging indicator
            if (isCharging) {
                drawLine(
                    color = Color.White,
                    start = Offset(batteryWidth / 3, batteryHeight / 2),
                    end = Offset(batteryWidth / 2, batteryHeight / 3),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(batteryWidth / 2, batteryHeight / 3),
                    end = Offset(batteryWidth * 2 / 3, batteryHeight / 2),
                    strokeWidth = 2f
                )
            }
        }
        
        // Percentage text for small indicator
        if (size > 50) {
            Text(
                text = "$animatedLevel%",
                fontSize = 10.sp,
                color = if (isLow) Color(0xFFF44336) else Color.White
            )
        }
    }
}